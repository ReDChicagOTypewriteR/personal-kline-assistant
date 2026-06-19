# Backend

Personal Kline Assistant 后端服务。

## 启动

```bash
mvn spring-boot:run
```

服务地址：`http://localhost:8080`

## IDEA 启动

在 IDEA 中导入 Maven 工程：

1. 打开 `personal-kline-assistant` 目录。
2. 右键根目录 `pom.xml` 或 `backend/pom.xml`，选择 `Add as Maven Project`。
3. Maven 同步完成后，运行 `com.alexjoker.quant.QuantApplication`。
4. 启动前在 Run Configuration 里设置环境变量：

```bash
MYSQL_HOST=localhost;MYSQL_PORT=3306;MYSQL_DATABASE=quant_kline;MYSQL_USERNAME=root;MYSQL_PASSWORD=你的密码
```

MySQL 默认连接：

- `MYSQL_HOST=localhost`
- `MYSQL_PORT=3306`
- `MYSQL_DATABASE=quant_kline`
- `MYSQL_USERNAME=root`
- `MYSQL_PASSWORD=` 空

如需修改连接信息，可在启动前设置对应环境变量。

## Docker 中的 MySQL

后端默认从宿主机访问 MySQL。如果容器端口映射是 `3306:3306`，使用默认 `MYSQL_HOST=localhost`、`MYSQL_PORT=3306` 即可。

示例：

```bash
docker run --name quant-kline-mysql \
  -e MYSQL_ROOT_PASSWORD=your_password \
  -e MYSQL_DATABASE=quant_kline \
  -p 3306:3306 \
  -d mysql:8.4 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

启动后端：

```bash
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=quant_kline
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your_password
mvn spring-boot:run
```

如果容器映射为 `3307:3306`，把 `MYSQL_PORT` 改成 `3307`。

## 主要接口

- `GET /api/symbols`
- `POST /api/symbols`
- `POST /api/klines/daily/import?symbolCode=QQQ`
- `POST /api/klines/daily/fetch/eastmoney?symbolCode=000001&stockCode=000001&start=2024-01-01&end=2026-12-31&adjustType=1`
- `POST /api/indicators/calculate?symbolCode=QQQ`
- `POST /api/signals/generate?symbolCode=QQQ`
- `GET /api/dashboard`

CSV 样例在 `sample/QQQ.csv`、`sample/SPY.csv` 和 `sample/000001.csv`。

`sample/000001.csv` 是通过当前目录外的 `adata-main` 项目分析出的东方财富日 K 链路拉取的平安银行数据。前端 KLineCharts 数据格式示例在 `sample/000001.kline-data.json`。
