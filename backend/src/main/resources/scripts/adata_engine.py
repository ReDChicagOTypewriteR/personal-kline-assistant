#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Local adata market-data engine.

The Spring Boot backend starts this process and calls it over localhost HTTP.
It keeps Python/adta dependencies out of the JVM while avoiding one Python
process per market-data request.
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import signal
import sys
import traceback
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, Dict, List, Optional


ADATA_HOME: Optional[Path] = None


def configure_runtime(adata_home: str) -> None:
    global ADATA_HOME
    ADATA_HOME = Path(adata_home).expanduser().resolve()
    if not (ADATA_HOME / "adata").is_dir():
        raise RuntimeError(f"adata-main not found: {ADATA_HOME}")
    if str(ADATA_HOME) not in sys.path:
        sys.path.insert(0, str(ADATA_HOME))

    for key in ("HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "http_proxy", "https_proxy", "all_proxy"):
        os.environ.pop(key, None)
    os.environ["NO_PROXY"] = "*"

    # adata imports both py_mini_racer.MiniRacer and py_mini_racer.py_mini_racer.
    # Newer py_mini_racer packages expose MiniRacer at package level only.
    try:
        import py_mini_racer as pmr
        if not hasattr(pmr, "py_mini_racer"):
            pmr.py_mini_racer = pmr
    except Exception:
        pass


def k_type(period: str) -> int:
    value = (period or "D").upper()
    if value == "D":
        return 1
    if value == "W":
        return 2
    if value == "M":
        return 3
    if value == "5M":
        return 5
    if value == "15M":
        return 15
    if value == "30M":
        return 30
    if value in {"1H", "60M"}:
        return 60
    raise RuntimeError(f"Unsupported adata period: {period}")


def normalize_df(df) -> List[Dict[str, Any]]:
    if df is None or getattr(df, "empty", True):
        return []
    rows = []
    for record in df.to_dict(orient="records"):
        rows.append({
            "tradeDate": record.get("trade_date") or str(record.get("trade_time", ""))[:10],
            "tradeTime": record.get("trade_time"),
            "open": clean_number(record.get("open")),
            "high": clean_number(record.get("high")),
            "low": clean_number(record.get("low")),
            "close": clean_number(record.get("close") or record.get("price")),
            "volume": clean_number(record.get("volume")),
            "amount": clean_number(record.get("amount")),
        })
    return rows


def clean_number(value: Any) -> Optional[float]:
    if value is None:
        return None
    text = str(value).strip()
    if not text or text.lower() in {"none", "nan", "--"}:
        return None
    return float(text)


def fetch_rows(payload: Dict[str, Any]) -> Dict[str, Any]:
    source = str(payload.get("source") or "").upper()
    code = str(payload.get("symbolCode") or payload.get("code") or "").strip()
    start = payload.get("start") or "1990-01-01"
    end = payload.get("end") or ""
    period = payload.get("period") or "D"
    adjust = int(payload.get("adjustType") or 1)

    if not code:
        raise RuntimeError("symbolCode is required")

    try:
        if source == "STOCK_EAST":
            from adata.stock.market.stock_market.stock_market_east import StockMarketEast
            df = StockMarketEast().get_market(
                stock_code=code,
                start_date=start,
                end_date=end or None,
                k_type=k_type(period),
                adjust_type=adjust,
            )
        elif source == "STOCK_BAIDU":
            from adata.stock.market.stock_market.stock_market_baidu import StockMarketBaiDu
            df = StockMarketBaiDu().get_market(
                stock_code=code,
                start_date=start,
                end_date=end or None,
                k_type=k_type(period),
                adjust_type=adjust,
            )
        elif source == "ETF_THS":
            from adata.fund.market.etf_market import ETFMarket
            df = ETFMarket().get_market_etf(
                fund_code=code,
                k_type=k_type(period),
                start_date=start,
                end_date=end or "",
            )
        else:
            raise RuntimeError(f"Unsupported source: {source}")
    except Exception as exc:
        return {"source": source, "rows": [], "error": compact_error(exc)}

    return {"source": source, "rows": normalize_df(df)}


def compact_error(exc: Exception) -> str:
    text = str(exc).strip()
    if not text:
        text = exc.__class__.__name__
    if "substring not found" in text:
        return "数据源返回内容不是有效 K 线 JSON，可能是代码无数据、同花顺限制或网络异常"
    if len(text) > 240:
        return text[:240] + "..."
    return text


def resolve_symbol(payload: Dict[str, Any]) -> Dict[str, Any]:
    code = str(payload.get("symbolCode") or payload.get("code") or "").strip()
    if not code:
        raise RuntimeError("symbolCode is required")
    name = lookup_name_from_cache(code)
    asset_type = "ETF" if code.startswith(("15", "16", "50", "51", "56", "58")) else "STOCK"
    return {
        "symbolCode": code,
        "symbolName": name or code,
        "market": "CN",
        "assetType": asset_type,
        "currency": "CNY",
    }


def lookup_name_from_cache(code: str) -> Optional[str]:
    if ADATA_HOME is None:
        return None
    stock_cache = ADATA_HOME / "adata" / "stock" / "cache" / "code.csv"
    if stock_cache.exists():
        with stock_cache.open("r", encoding="utf-8") as handle:
            for row in csv.reader(handle):
                if row and row[0] == code and len(row) > 1:
                    return row[1]
    for path in (ADATA_HOME / "adata" / "fund").rglob("*.csv"):
        try:
            with path.open("r", encoding="utf-8") as handle:
                reader = csv.DictReader(handle)
                for row in reader:
                    values = {str(k).lower(): v for k, v in row.items()}
                    row_code = values.get("fund_code") or values.get("code") or values.get("symbol") or values.get("基金代码")
                    if row_code == code:
                        return values.get("fund_name") or values.get("name") or values.get("基金简称") or values.get("基金名称")
        except Exception:
            continue
    return None


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self) -> None:
        if self.path == "/health":
            self.write_json({"status": "ok"})
            return
        self.write_json({"error": "not_found"}, status=404)

    def do_POST(self) -> None:
        try:
            payload = self.read_json()
            if self.path in {"/klines/daily", "/klines/chart"}:
                self.write_json(fetch_rows(payload))
            elif self.path == "/symbols/resolve":
                self.write_json(resolve_symbol(payload))
            else:
                self.write_json({"error": "not_found"}, status=404)
        except Exception as exc:
            self.write_json(
                {"error": str(exc), "trace": traceback.format_exc(limit=4)},
                status=500,
            )

    def read_json(self) -> Dict[str, Any]:
        if self.headers.get("Transfer-Encoding", "").lower() == "chunked":
            body = self.read_chunked_body().decode("utf-8")
            return json.loads(body) if body else {}
        length = int(self.headers.get("Content-Length") or "0")
        if length <= 0:
            return {}
        body = self.rfile.read(length).decode("utf-8")
        return json.loads(body) if body else {}

    def read_chunked_body(self) -> bytes:
        chunks = []
        while True:
            size_line = self.rfile.readline().strip()
            if not size_line:
                continue
            size_text = size_line.split(b";", 1)[0]
            size = int(size_text, 16)
            if size == 0:
                self.rfile.readline()
                break
            chunks.append(self.rfile.read(size))
            self.rfile.read(2)
        return b"".join(chunks)

    def write_json(self, payload: Dict[str, Any], status: int = 200) -> None:
        body = json.dumps(payload, ensure_ascii=False, default=str).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt: str, *args: Any) -> None:
        sys.stderr.write("[adata-engine] " + fmt % args + "\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=19090)
    parser.add_argument("--adata-home", required=True)
    args = parser.parse_args()

    configure_runtime(args.adata_home)
    server = ThreadingHTTPServer((args.host, args.port), Handler)

    def stop_server(signum, frame) -> None:
        raise KeyboardInterrupt

    signal.signal(signal.SIGTERM, stop_server)
    signal.signal(signal.SIGINT, stop_server)
    print(json.dumps({"status": "started", "host": args.host, "port": args.port}, ensure_ascii=False), flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
