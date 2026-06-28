# ADHD 项目启动说明

当前仓库已经生成两个可继续开发的子项目:

- `adhd-app-server`: Spring Boot 后端
- `adhd-app-miniprogram`: 微信原生小程序前端

## 目录结构

```text
adhd/
├── adhd-app-server/
├── adhd-app-miniprogram/
├── docker-compose.yml
└── *.md
```

## 先启动 MySQL

```bash
docker compose -f "/Users/huangbo/Documents/projects/adhd/docker-compose.yml" up -d
```

## 启动后端

```bash
cd "/Users/huangbo/Documents/projects/adhd/adhd-app-server"
WECHAT_APP_ID="你的小程序AppID" \
WECHAT_APP_SECRET="你的小程序AppSecret" \
MODEL_BASE_URL="http://43.142.79.65:8080/v1" \
MODEL_API_KEY="EMPTY" \
MODEL_NAME="gpt-4o-mini" \
mvn spring-boot:run
```

## 打开前端

1. 打开微信开发者工具
2. 导入目录 `adhd-app-miniprogram`
3. 本地调试时关闭合法域名校验

## 现在已有的接口

- `GET /api/health`
- `POST /api/auth/login`
- `GET /api/users/me`
- `GET /api/users/me/dashboard`
- `GET /api/tasks`
- `POST /api/tasks`
- `POST /api/checkins`
- `GET /api/reports/weekly`
- `POST /api/ai/chat`

## 小程序侧说明

- 当前小程序会在请求前自动触发 `wx.login`
- 后端会用微信 `code` 换 `openid`
- 本地调试时，要在微信开发者工具里用你自己的小程序 `AppID` 打开项目，不能长期用 `touristappid`

## 下一步建议

1. 增加孩子资料编辑页
2. 增加任务详情、月历打卡和奖励兑换页
3. 让周报基于真实统计口径生成
4. 接入生产域名和微信后台服务器域名白名单
