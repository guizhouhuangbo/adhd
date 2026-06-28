# adhd-app-server

Spring Boot 3 + MyBatis-Plus 的本地 MVP 后端。

## 本地运行

1. 确保 MySQL 已启动

```bash
docker compose up -d
```

2. 启动后端

```bash
mvn spring-boot:run
```

如果你要接真实微信登录和模型服务，建议这样启动:

```bash
WECHAT_APP_ID="你的小程序AppID" \
WECHAT_APP_SECRET="你的小程序AppSecret" \
MODEL_BASE_URL="http://43.142.79.65:8080/v1" \
MODEL_API_KEY="EMPTY" \
MODEL_NAME="gpt-4o-mini" \
mvn spring-boot:run
```

服务默认地址:

- API: `http://127.0.0.1:8080/api`
- 健康检查: `http://127.0.0.1:8080/api/health`
- Swagger: `http://127.0.0.1:8080/swagger-ui.html`

## 默认数据库配置

- Host: `127.0.0.1`
- Port: `3306`
- Database: `adhd`
- Username: `adhd`
- Password: `123456`

首次启动会自动执行 `src/main/resources/schema.sql` 创建表。

## 当前认证与模型配置

- 登录: 微信小程序 `wx.login` + 后端 `jscode2session`
- 鉴权: Sa-Token，前端通过 `Authorization: Bearer <token>` 传递
- 模型接口: OpenAI 兼容 `/chat/completions`

注意:

- 如果 `WECHAT_APP_ID` / `WECHAT_APP_SECRET` 没配，真实小程序登录会失败
- `MODEL_BASE_URL` 和 `MODEL_API_KEY` 已支持通过环境变量覆盖
- `MODEL_NAME` 默认是 `gpt-4o-mini`，如果你的服务需要别的模型名，改这个环境变量即可
