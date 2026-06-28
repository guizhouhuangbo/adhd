# adhd-app-miniprogram

微信原生小程序 MVP，围绕文档里的 5 个核心模块先实现首页、任务、AI 陪伴、周报、我的五个页面骨架。

## 本地调试

1. 用微信开发者工具打开目录 `adhd-app-miniprogram`
2. 在开发者工具里勾选“不校验合法域名”
3. 确保后端已启动在 `http://127.0.0.1:8080`

当前接口前缀在 `app.js` 中:

```js
baseUrl: 'http://127.0.0.1:8080/api'
```

## 说明

- 当前是 MVP 骨架，未接入真实微信登录
- AI 对话和任务拆分目前先走本地 stub，方便你先联调界面和后端流程
- 后面接入真实 DeepSeek / 微信登录时，优先替换 `utils/api.js` 和后端 `integration/` 目录
