# Personal Kline Assistant v0.1

个人 K 线分析系统，当前版本聚焦日 K 数据、技术指标、技术评分、技术信号和前端展示。第一阶段已支持通过外部 `daily_stock_analysis` HTTP 服务做 BUY_CANDIDATE 的 AI 风险过滤；系统仍不包含登录、多用户、自动交易、券商 API 或持仓同步。

## 第一阶段：接入 daily_stock_analysis 做 AI 风险过滤

当前阶段把 `personal-kline-assistant` 定位为主系统，负责 K 线数据、技术指标、技术信号、AI 结果保存和最终交易辅助决策；`daily_stock_analysis` 定位为外部 LLM 分析服务，只通过 HTTP API 提供股票情绪、风险、市场背景和完整报告。

`daily_stock_analysis` 是独立开源项目，建议保持源码尽量不修改，以便后续继续从 GitHub 原仓库拉取更新。第一阶段主要整合逻辑都在 `personal-kline-assistant` 中，两个系统不共享数据库，也不会让 `daily_stock_analysis` 写入本系统 MySQL。

### 整合链路

1. 本系统生成 `technical_signal`。
2. 只有 `signal_type = BUY_CANDIDATE` 时才允许触发 AI 风险分析。
3. 本系统调用 `daily_stock_analysis` 的 HTTP API。
4. 本系统把返回结果保存到 `ai_analysis_snapshot`。
5. 本系统根据 K 线信号和 AI 风险分生成 `final_trade_decision`。
6. 前端 Dashboard 和 K 线详情页展示 AI 情绪分、AI 风险分、风险等级、最终动作和完整报告入口。

### 为什么本系统是主系统

- K 线信号和回测规则来自本系统，AI 只做风险过滤。
- 最终交易辅助决策由本系统生成并保存。
- LLM 再看好也不能绕过 K 线信号；非 `BUY_CANDIDATE` 一律不进入交易候选。
- LLM 调用失败时，本系统保守降级为 `WATCH_ONLY`。

### 启动 daily_stock_analysis

在另一个终端启动外部 LLM 服务：

```bash
cd ../daily_stock_analysis
python main.py --serve-only
```

服务默认地址按本项目配置为：

```text
http://localhost:8000
```

第一阶段默认调用：

```http
POST /api/external/kline-risk-analysis
```

如果你希望直接复用 `daily_stock_analysis` 后续新增或已有的其他分析接口，可以只改本项目的 `risk-analysis-path` 配置。

### 配置 LLM API 地址

`backend/src/main/resources/application.yml`：

```yaml
llm:
  daily-stock-analysis:
    enabled: true
    base-url: http://localhost:8000
    risk-analysis-path: /api/external/kline-risk-analysis
    connect-timeout-seconds: 5
    read-timeout-seconds: 90
    cache-enabled: true
```

如果 `enabled=false`，本系统不会调用 LLM 服务，AI 分析请求会按保守规则生成观察类决策。

### 触发 AI 风险分析

手动分析单个标的：

```http
POST /api/ai-analysis/analyze?symbolCode=510300&tradeDate=2026-05-04
```

批量分析指定日期所有 `BUY_CANDIDATE`：

```http
POST /api/ai-analysis/analyze-buy-candidates?tradeDate=2026-05-04
```

查询 AI 快照：

```http
GET /api/ai-analysis/snapshot?symbolCode=510300&analysisDate=2026-05-04
```

查询最终决策：

```http
GET /api/final-decisions/latest
GET /api/final-decisions/history?symbolCode=510300
```

### 缓存规则

`ai_analysis_snapshot` 对 `symbol_code + analysis_date` 做唯一约束。默认开启缓存，同一天同一标的重复点击 AI 分析时，优先读取已有快照，不重复调用 `daily_stock_analysis`。

### 失败降级规则

- `daily_stock_analysis` 未启动、超时或返回异常时，本系统不崩溃。
- 失败信息会写入 `ai_analysis_snapshot.call_status = FAILED` 和 `error_message`。
- 对应最终决策为 `final_action = WATCH_ONLY`、`position_policy = WATCH`。
- AI 返回字段缺失时使用 `UNKNOWN` 或空值，不影响 K 线系统继续使用。

### 当前阶段不支持

第一阶段不支持自动交易、券商 API、持仓同步、消息队列、共享数据库，也不会把 AI 分析强行塞进历史每一天的 K 线回测。历史回测仍由本系统独立完成，AI 风险过滤只用于当前或近期指定日期的 `BUY_CANDIDATE`。

### ETF 信号阈值

ETF 的 `BUY_CANDIDATE` 默认比个股略宽，便于宽基 ETF 在趋势偏强但均线排列不完全理想时进入 AI 风险过滤：

```yaml
signal:
  rule:
    stock-buy-score-threshold: 75
    etf-buy-score-threshold: 70
    etf-rsi-min: 42
    etf-rsi-max: 72
    etf-allow-partial-ma-confirmation: true
```

ETF 仍然必须满足：收盘价在 MA20 上方、RSI 不过热/不过弱，并且 MA5>MA10 或 MA10>MA20 至少成立一个。非 `BUY_CANDIDATE` 仍不会触发 AI 风险分析。

## 技术栈

- 后端：Java 21、Spring Boot 3、Maven、MySQL、MyBatis Plus、Lombok、Validation
- 前端：Vue 3、Vite、TypeScript、Axios、Element Plus、KLineCharts
- 数据库：MySQL，默认库名 `quant_kline`

## MySQL 配置

后端默认连接：

- Host：`localhost`
- Port：`3306`
- Database：`quant_kline`
- Username：`root`
- Password：空

可通过环境变量覆盖：

```bash
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=quant_kline
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your_password
```

JDBC URL 已开启 `createDatabaseIfNotExist=true`。只要当前 MySQL 用户有建库权限，首次启动会自动创建 `quant_kline` 并执行 `backend/src/main/resources/db/schema.sql` 初始化表。

### MySQL 在 Docker 中

如果 MySQL 跑在 Docker 容器里，后端仍然可以直接连接宿主机暴露出来的端口。

常见启动方式：

```bash
docker run --name quant-kline-mysql \
  -e MYSQL_ROOT_PASSWORD=your_password \
  -e MYSQL_DATABASE=quant_kline \
  -p 3306:3306 \
  -d mysql:8.4 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

后端启动前设置：

```bash
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=quant_kline
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your_password
```

如果你的容器端口不是 `3306:3306`，例如 `3307:3306`，则设置：

```bash
export MYSQL_PORT=3307
```

可以用下面命令查看容器端口映射：

```bash
docker ps
```

## 后端启动

```bash
cd personal-kline-assistant/backend
mvn spring-boot:run
```

后端默认监听 `http://localhost:8080`。首次启动会自动执行 `src/main/resources/db/schema.sql` 初始化表。

## IDEA 导入后端

如果 IDEA 只把后端代码当普通 `.java` 文件，没有识别 Spring Boot：

1. 在 IDEA 里打开 `personal-kline-assistant` 目录，不要只打开外层 `kline` 目录。
2. 右键 `personal-kline-assistant/pom.xml`，选择 `Add as Maven Project`。
3. 如果没有这个菜单，打开右侧 Maven 面板，点击 `+`，选择 `personal-kline-assistant/pom.xml` 或 `backend/pom.xml`。
4. 等 Maven 同步完成后，打开 `backend/src/main/java/com/alexjoker/quant/QuantApplication.java`，运行 `main` 方法。
5. 如果 Lombok 标红，确认 IDEA 已安装 Lombok 插件，并在 `Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors` 勾选 `Enable annotation processing`。

IDEA Community 版不会显示完整 Spring Boot 工具窗口，但仍然可以直接运行 `QuantApplication.main()`。

## 前端启动

```bash
cd personal-kline-assistant/frontend
npm install
npm run dev
```

前端默认监听 `http://localhost:5173`，Vite 会把 `/api` 请求代理到后端。

## CSV 导入格式

接口：`POST /api/klines/daily/import?symbolCode=QQQ`

字段名：`file`

```csv
date,open,high,low,close,volume,amount
2025-01-02,100.12,102.50,99.80,101.30,12345678,1234567890
2025-01-03,101.30,103.20,100.50,102.80,13456789,1345678900
```

`open/high/low/close` 必填，`volume/amount` 可为空。重复导入会按 `symbol_code + trade_date` 更新已有数据。

## 东方财富日 K 数据源

当前已接入 A 股日 K 数据源，默认来源为东方财富历史 K 线接口。这个实现参考了当前目录下 `adata-main` 项目的调用方式：

- `adata.stock.market.get_market(stock_code='000001', k_type=1, start_date='2024-01-01')`
- 底层接口：`https://push2his.eastmoney.com/api/qt/stock/kline/get`
- 返回字段可映射为：`trade_date/open/high/low/close/volume/amount`

后端接口：

```http
POST /api/klines/daily/fetch/eastmoney?symbolCode=000001&stockCode=000001&start=2024-01-01&end=2026-12-31&adjustType=1
```

参数说明：

- `symbolCode`：系统内标的代码，必须先在“自选池”页面创建，例如 `000001`
- `stockCode`：东方财富 A 股 6 位代码，例如平安银行 `000001`
- `start/end`：日期，格式 `yyyy-MM-dd`
- `adjustType`：`1` 前复权，`0` 不复权，`2` 后复权

导入逻辑和 CSV 一样，重复数据会按 `symbol_code + trade_date` 更新。

当前主流程已经支持按 `symbol` 自动获取数据：进入 K 线详情页或调用明细接口时，后端会先检查 `daily_kline`。如果该 CN 标的没有日 K，或最新日 K 距今天超过 5 天，会自动调用东方财富拉取并写入 MySQL，再返回给前端展示。CSV 导入仍保留为备用方式。

## adata-main 行情引擎

如果项目目录下存在 `adata-main`，后端会在 Spring Boot 启动时自动托管一个本地 Python 行情引擎。你仍然只需要启动 `personal-kline-assistant` 后端，行情引擎会跟随后端一起启动和停止。

架构：

```text
Spring Boot 后端 :8080
  └── 本地 adata 行情引擎 :19090
        └── 调用 adata-main SDK
```

默认配置：

```yaml
market-data:
  engine:
    enabled: true
    host: 127.0.0.1
    port: 19090
    python-command: python3
    adata-home: ./adata-main
    startup-timeout-seconds: 20
    request-timeout-seconds: 60
```

默认数据源顺序：

- ETF 日/周/月：东方财富 -> adata 同花顺 ETF -> adata 东方财富股票行情 -> adata 百度股市通
- A 股日/周/月：东方财富 -> adata 东方财富股票行情 -> adata 百度股市通
- 分时周期：仍优先使用东方财富图表接口

这样设计的原因是：东方财富当前对日 K 和图表数据最稳定；adata 的同花顺 ETF 源可作为补充，但可能受 Python 依赖、同花顺 IP 限制或本机网络代理影响。只要某个数据源失败，后端会自动尝试下一个数据源；如果本地 MySQL 已有日 K，页面也会优先显示本地数据，不会因为远端临时断连阻断 K 线详情页。

如果你的 adata 运行环境不是系统 `python3`，例如使用虚拟环境：

```bash
export ADATA_HOME=/Users/alexjoker/Desktop/coding/Quantify/personal-kline-assistant/adata-main
```

并把 `market-data.engine.python-command` 改成虚拟环境中的 Python 路径。

行情引擎本地接口：

```http
GET  http://127.0.0.1:19090/health
POST http://127.0.0.1:19090/klines/daily
POST http://127.0.0.1:19090/klines/chart
POST http://127.0.0.1:19090/symbols/resolve
```

这些接口只供后端内部调用。正常使用时不需要手动启动行情引擎。

前端 K 线图使用 `klinecharts`，组件接收的数据会转换为：

```ts
{
  timestamp: number,
  open: number,
  close: number,
  high: number,
  low: number,
  volume: number,
  turnover: number
}
```

## 推荐操作流程

1. 启动后端
2. 启动前端
3. 确认 Docker 中的 MySQL 容器已启动，并且端口已映射到宿主机
4. 在“自选池”添加 symbol，例如 `QQQ`
5. 添加到自选池
6. 在“数据导入”导入 `backend/sample/QQQ.csv`
7. 点击计算指标
8. 点击生成信号
9. 到 Dashboard 查看结果
10. 到 K线详情查看图表

## 样例数据

项目内置：

- `backend/sample/QQQ.csv`
- `backend/sample/SPY.csv`
- `backend/sample/000001.csv`
- `backend/sample/000001.kline-data.json`

`QQQ.csv` 和 `SPY.csv` 是模拟数据。`000001.csv` 是通过 `adata-main` 东方财富链路拉取的平安银行日 K 测试数据；`000001.kline-data.json` 是同一批数据转换后的 KLineCharts `KLineData` 示例。
