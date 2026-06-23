# ADHD 助手 - 技术架构详解

> Java 工程师 + 个人开发 + TKE 集群 的最优技术方案

---

## 一、技术栈总览

```
前端:
  ├─ 微信小程序原生(WXML + WXSS + JS)
  ├─ Vant Weapp 4.x(UI 组件库)
  └─ 微信开发者工具(IDE)

后端:
  ├─ JDK 17 + Spring Boot 3.x
  ├─ MyBatis-Plus 3.5(ORM)
  ├─ Sa-Token 1.37(认证)
  ├─ WxJava 4.6(微信 SDK)
  ├─ Hutool 5.8(工具库)
  └─ SpringDoc OpenAPI 2.3(API 文档)

数据存储:
  ├─ MySQL 8(主数据)
  ├─ Redis 7(缓存,可选,后期加)
  └─ 腾讯云 COS(图片/文件存储)

AI 服务:
  ├─ DeepSeek API(主用,便宜)
  ├─ Claude API(高级场景,后期)
  └─ 直接 HTTP 调用(不用 LangChain)

基础设施:
  ├─ 腾讯云 TKE(容器编排)
  ├─ 腾讯云 CLB(负载均衡 + HTTPS)
  ├─ 腾讯云 APM(性能监控,自动注入)
  ├─ 腾讯云 CLS(日志服务)
  ├─ 腾讯云 Prometheus(指标监控)
  └─ Hertzbeat(接口探针 + 钉钉告警)

CI/CD:
  ├─ Jenkins(已有)
  ├─ 腾讯云容器镜像仓库 CCR
  └─ kubectl apply 自动部署
```

---

## 二、为什么这么选(决策依据)

### 前端: 微信原生(不用 Taro/uniapp)

| 备选 | 选择 | 理由 |
|---|---|---|
| **微信原生** | ✅ | 学习 3-5 天,后端工程师友好 |
| ~~Taro + React~~ | ❌ | 要学 React + JSX + Hooks,2-3 周 |
| ~~uniapp + Vue~~ | ❌ | 要学 Vue,跨端用不上 |

### 后端: Spring Boot(你的强项)

| 备选 | 选择 | 理由 |
|---|---|---|
| **Spring Boot 3** | ✅ | 你最熟,生态最好 |
| ~~Spring Cloud~~ | ❌ | 微服务过度工程 |
| ~~Node.js~~ | ❌ | 不是你强项 |

### ORM: MyBatis-Plus(不用 JPA)

| 备选 | 选择 | 理由 |
|---|---|---|
| **MyBatis-Plus** | ✅ | 中文文档好,灵活,CRUD 自动 |
| ~~JPA/Hibernate~~ | ❌ | 复杂 SQL 难写 |

### 认证: Sa-Token(不用 Spring Security)

| 备选 | 选择 | 理由 |
|---|---|---|
| **Sa-Token** | ✅ | 国产,中文文档,5 分钟接入微信登录 |
| ~~Spring Security~~ | ❌ | 学习曲线陡,过度工程 |

### AI: 直接 HTTP(不用 LangChain)

| 备选 | 选择 | 理由 |
|---|---|---|
| **直接 HTTP 调** | ✅ | 5 行代码,你完全掌控 |
| ~~LangChain~~ | ❌ | 框架太重,过度抽象 |
| ~~Spring AI~~ | ❌ | 新框架不稳定 |

---

## 三、完整架构图

```
┌──────────────────────────────────────────────────────────┐
│  用户层                                                    │
│  📱 微信小程序(用户手机)                                  │
└─────────────────────┬────────────────────────────────────┘
                      │ HTTPS (TLS 1.3)
                      ↓
┌──────────────────────────────────────────────────────────┐
│  接入层(腾讯云)                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  腾讯云 CLB(负载均衡 + HTTPS 证书)                │  │
│  │  域名: api.你的域名.com                             │  │
│  └─────────────────────┬──────────────────────────────┘  │
└────────────────────────┼─────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────┐
│  应用层(TKE 集群)                                        │
│  ┌────────────────────────────────────────────────────┐  │
│  │  Ingress Controller(K8s 自带)                     │  │
│  └─────────────────────┬──────────────────────────────┘  │
│                        ↓                                  │
│  ┌────────────────────────────────────────────────────┐  │
│  │  Service: adhd-app (ClusterIP)                     │  │
│  └─────────────────────┬──────────────────────────────┘  │
│                        ↓                                  │
│  ┌────────────────────────────────────────────────────┐  │
│  │  Deployment: adhd-app (2 副本)                     │  │
│  │  ┌──────────────────────────────────────────────┐  │  │
│  │  │  Pod 1                                       │  │  │
│  │  │  ├─ Init Container: APM Agent 注入            │  │  │
│  │  │  └─ App Container:                           │  │  │
│  │  │     Spring Boot + 自动注入 javaagent         │  │  │
│  │  │     ├─ Controller (REST API)                 │  │  │
│  │  │     ├─ Service (业务逻辑)                    │  │  │
│  │  │     ├─ Mapper (数据访问)                     │  │  │
│  │  │     └─ Integration (外部调用)                │  │  │
│  │  └──────────────────────────────────────────────┘  │  │
│  │  ┌──────────────────────────────────────────────┐  │  │
│  │  │  Pod 2 (同上)                                │  │  │
│  │  └──────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
          │                    │                    │
          ↓                    ↓                    ↓
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  数据层           │  │  AI 层            │  │  第三方           │
│                  │  │                  │  │                  │
│  MySQL 8         │  │  DeepSeek API    │  │  微信开放平台      │
│  (TKE 集群内     │  │  (HTTP 调用)     │  │  - 登录           │
│   或云数据库)    │  │                  │  │  - 支付           │
│                  │  │                  │  │  - 模板消息       │
│  Redis(可选)    │  │                  │  │                  │
└──────────────────┘  └──────────────────┘  └──────────────────┘

观测层:
┌──────────────────────────────────────────────────────────┐
│  ├─ 腾讯云 APM(调用链追踪,自动注入)                       │
│  ├─ 腾讯云 CLS(日志聚合)                                  │
│  ├─ 腾讯云 Prometheus(指标监控)                           │
│  ├─ Hertzbeat(接口探针)                                   │
│  └─ 钉钉告警(异常通知)                                    │
└──────────────────────────────────────────────────────────┘
```

---

## 四、项目结构

### 后端项目: `adhd-app-server`

```
adhd-app-server/
├── pom.xml
├── Dockerfile
├── docker-compose.yml(本地开发用)
├── k8s/                          # K8s 部署文件
│   ├── deployment.yaml
│   ├── service.yaml
│   └── ingress.yaml
├── src/main/java/com/yourname/adhd/
│   ├── AdhdApplication.java     # 启动类
│   │
│   ├── config/                   # 配置
│   │   ├── SaTokenConfig.java
│   │   ├── WxConfig.java
│   │   ├── CorsConfig.java
│   │   ├── MybatisPlusConfig.java
│   │   └── SwaggerConfig.java
│   │
│   ├── controller/               # 接口层
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   ├── TaskController.java
│   │   ├── CheckInController.java
│   │   ├── ReportController.java
│   │   ├── AIController.java
│   │   └── PaymentController.java(后期加)
│   │
│   ├── service/                  # 业务层
│   │   ├── UserService.java
│   │   ├── TaskService.java
│   │   ├── CheckInService.java
│   │   ├── ReportService.java
│   │   ├── AIService.java
│   │   └── WeChatService.java
│   │
│   ├── mapper/                   # 数据层
│   │   ├── UserMapper.java
│   │   ├── TaskMapper.java
│   │   └── CheckInMapper.java
│   │
│   ├── entity/                   # 实体
│   │   ├── User.java
│   │   ├── Task.java
│   │   └── CheckIn.java
│   │
│   ├── dto/                      # DTO
│   │   ├── LoginRequest.java
│   │   ├── TaskCreateRequest.java
│   │   └── ReportResponse.java
│   │
│   ├── integration/              # 外部集成
│   │   ├── DeepSeekClient.java
│   │   └── WeChatClient.java
│   │
│   └── common/                   # 通用
│       ├── Result.java
│       ├── GlobalExceptionHandler.java
│       └── exception/
│
└── src/main/resources/
    ├── application.yml
    ├── application-prod.yml
    └── mapper/                   # XML SQL(复杂查询用)
```

### 前端项目: `adhd-app-miniprogram`

```
adhd-app-miniprogram/
├── app.js                   # 入口逻辑
├── app.json                 # 全局配置(页面、tab)
├── app.wxss                 # 全局样式
├── project.config.json      # 项目配置
├── sitemap.json
│
├── pages/
│   ├── home/                # 首页
│   │   ├── home.wxml
│   │   ├── home.wxss
│   │   ├── home.js
│   │   └── home.json
│   ├── task/                # 任务
│   │   ├── list/            # 任务列表
│   │   ├── detail/          # 任务详情
│   │   └── create/          # 创建任务
│   ├── checkin/             # 打卡
│   ├── chat/                # AI 对话
│   ├── report/              # 周报
│   └── profile/             # 我的
│
├── components/              # 自定义组件
│   ├── task-card/
│   ├── star-counter/
│   └── ai-chat-bubble/
│
├── utils/
│   ├── api.js               # API 封装
│   ├── auth.js              # 登录管理
│   └── format.js            # 工具函数
│
├── images/                  # 图片资源
└── miniprogram_npm/         # Vant Weapp
```

---

## 五、核心 API 设计

### RESTful 接口列表

| 接口 | 方法 | 鉴权 | 说明 |
|---|---|---|---|
| `/api/auth/login` | POST | ❌ | 微信登录,返回 token |
| `/api/auth/logout` | POST | ✅ | 退出 |
| `/api/users/me` | GET | ✅ | 当前用户信息 |
| `/api/users/me` | PUT | ✅ | 更新用户信息 |
| `/api/users/me/child` | PUT | ✅ | 更新孩子信息 |
| `/api/tasks` | GET | ✅ | 任务列表 |
| `/api/tasks` | POST | ✅ | 创建任务(AI 拆分) |
| `/api/tasks/{id}` | GET | ✅ | 任务详情 |
| `/api/tasks/{id}` | PUT | ✅ | 更新任务 |
| `/api/tasks/{id}` | DELETE | ✅ | 删除任务 |
| `/api/tasks/templates` | GET | ✅ | 任务模板库 |
| `/api/checkins` | POST | ✅ | 完成打卡 |
| `/api/checkins/today` | GET | ✅ | 今日打卡情况 |
| `/api/checkins/calendar` | GET | ✅ | 月历视图 |
| `/api/reports/weekly` | GET | ✅ | 周报 |
| `/api/reports/monthly` | GET | ✅ | 月报 |
| `/api/ai/chat` | POST | ✅ | AI 对话 |
| `/api/ai/task-split` | POST | ✅ | AI 任务拆分 |
| `/api/games` | GET | ✅ | 训练游戏列表 |
| `/api/games/{id}/play` | POST | ✅ | 游戏结果上报 |
| `/api/health` | GET | ❌ | 健康检查(Hertzbeat 用) |
| `/actuator/health` | GET | ❌ | K8s probe |
| `/actuator/prometheus` | GET | ❌ | Prometheus 指标 |

### 统一返回结构

```java
public class Result<T> {
    private int code;        // 0 成功,其他失败
    private String msg;
    private T data;
    private long timestamp;
    private String traceId;  // 全链路追踪 ID
}
```

---

## 六、关键代码模板

### 1. 微信登录

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private WeChatService weChatService;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest req) {
        // 1. 微信 code 换 openid
        String openid = weChatService.code2Openid(req.getCode());

        // 2. 查询 / 创建用户
        User user = userService.findOrCreateByOpenid(openid);

        // 3. Sa-Token 登录
        StpUtil.login(user.getId());

        // 4. 返回
        return Result.ok(Map.of(
            "token", StpUtil.getTokenValue(),
            "user", user
        ));
    }
}
```

### 2. AI 任务拆分

```java
@Service
public class AIService {

    @Value("${deepseek.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SYSTEM_PROMPT_TASK_SPLIT = """
        你是 ADHD 任务拆分助手。
        把家长输入的任务,拆成 5-7 个具体可执行的小步骤。
        每个步骤要:
        - 简短(< 15 字)
        - 具体(能立即执行)
        - 有时间预估
        - 配合孩子年龄
        
        直接返回 JSON 数组,如:
        ["步骤1(2分钟)", "步骤2(5分钟)"]
        """;

    public List<String> splitTask(String taskName, int childAge) {
        String userPrompt = String.format(
            "给 %d 岁的 ADHD 孩子,拆分任务:「%s」",
            childAge, taskName
        );

        String response = callDeepSeek(SYSTEM_PROMPT_TASK_SPLIT, userPrompt);
        return JSON.parseArray(response, String.class);
    }

    private String callDeepSeek(String systemPrompt, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
            "model", "deepseek-chat",
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://api.deepseek.com/v1/chat/completions",
            entity,
            Map.class
        );

        Map<String, Object> choice = ((List<Map<String, Object>>) response.getBody().get("choices")).get(0);
        Map<String, String> message = (Map<String, String>) choice.get("message");
        return message.get("content");
    }
}
```

### 3. 小程序 API 封装

```javascript
// utils/api.js
const BASE_URL = 'https://api.你的域名.com'

function request(options) {
  const token = wx.getStorageSync('token')
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success(res) {
        if (res.data.code === 0) {
          resolve(res.data.data)
        } else if (res.data.code === 401) {
          // token 过期,跳登录
          wx.removeStorageSync('token')
          wx.reLaunch({ url: '/pages/login/login' })
        } else {
          wx.showToast({ title: res.data.msg, icon: 'none' })
          reject(res.data.msg)
        }
      },
      fail: reject
    })
  })
}

export const api = {
  // 认证
  login: (code) => request({ url: '/api/auth/login', method: 'POST', data: { code } }),
  
  // 用户
  getMe: () => request({ url: '/api/users/me' }),
  updateChild: (data) => request({ url: '/api/users/me/child', method: 'PUT', data }),
  
  // 任务
  getTasks: () => request({ url: '/api/tasks' }),
  createTask: (data) => request({ url: '/api/tasks', method: 'POST', data }),
  
  // 打卡
  checkin: (taskId) => request({ url: '/api/checkins', method: 'POST', data: { taskId } }),
  
  // 周报
  getWeeklyReport: () => request({ url: '/api/reports/weekly' }),
  
  // AI
  chat: (message) => request({ url: '/api/ai/chat', method: 'POST', data: { message } })
}
```

---

## 七、TKE 部署清单

### Deployment YAML

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: adhd-app
  namespace: prod
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: adhd-app
  template:
    metadata:
      labels:
        app: adhd-app
        env: prod
      annotations:
        # 自动注入腾讯云 APM
        instrumentation.opentelemetry.io/inject-java: "true"
    spec:
      terminationGracePeriodSeconds: 60
      containers:
      - name: app
        image: ccr.ccs.tencentyun.com/your-namespace/adhd-app:v1
        ports:
        - containerPort: 8080
        env:
        - name: OTEL_SERVICE_NAME
          value: "adhd-app"
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: adhd-app-secrets
              key: db-url
        - name: DEEPSEEK_API_KEY
          valueFrom:
            secretKeyRef:
              name: adhd-app-secrets
              key: deepseek-key
        resources:
          requests:
            cpu: "500m"
            memory: "1Gi"
          limits:
            cpu: "2"
            memory: "2Gi"
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
          failureThreshold: 2
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          failureThreshold: 3
        lifecycle:
          preStop:
            exec:
              command: ["sh", "-c", "sleep 15"]

---
apiVersion: v1
kind: Service
metadata:
  name: adhd-app
  namespace: prod
spec:
  type: ClusterIP
  selector:
    app: adhd-app
  ports:
  - port: 80
    targetPort: 8080

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: adhd-app
  namespace: prod
  annotations:
    kubernetes.io/ingress.class: qcloud
spec:
  tls:
  - hosts:
    - api.你的域名.com
    secretName: adhd-app-tls
  rules:
  - host: api.你的域名.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: adhd-app
            port:
              number: 80
```

---

## 八、CI/CD Pipeline(Jenkins)

```groovy
pipeline {
    agent any
    
    environment {
        IMAGE = 'ccr.ccs.tencentyun.com/your-namespace/adhd-app'
        VERSION = "v${BUILD_NUMBER}"
    }
    
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
        
        stage('Docker Build') {
            steps {
                sh "docker build -t ${IMAGE}:${VERSION} ."
                sh "docker tag ${IMAGE}:${VERSION} ${IMAGE}:latest"
            }
        }
        
        stage('Docker Push') {
            steps {
                sh "docker push ${IMAGE}:${VERSION}"
                sh "docker push ${IMAGE}:latest"
            }
        }
        
        stage('Deploy to TKE') {
            steps {
                sh "kubectl -n prod set image deployment/adhd-app app=${IMAGE}:${VERSION}"
                sh "kubectl -n prod rollout status deployment/adhd-app"
            }
        }
        
        stage('Notify') {
            steps {
                sh """
                curl -X POST 'https://oapi.dingtalk.com/robot/send?access_token=xxx' \\
                  -H 'Content-Type: application/json' \\
                  -d '{"msgtype":"text","text":{"content":"✅ adhd-app ${VERSION} 部署成功"}}'
                """
            }
        }
    }
}
```

---

## 九、监控告警(SLA 99%)

### 必配监控项

```yaml
- API 接口可用性(Hertzbeat 探针)
- API 响应时间(Hertzbeat)
- JVM 堆内存(腾讯云 APM)
- 数据库连接池(腾讯云 APM)
- DeepSeek API 调用成功率(自定义指标)
- 微信支付回调成功率(后期)
```

### 告警阈值

```
🔴 严重(钉钉 @所有人):
  - 接口完全不可用 > 3 分钟
  - 5xx 错误率 > 5%
  - 数据库连接失败

🟡 警告(钉钉群):
  - 响应时间 > 3 秒持续 5 次
  - DeepSeek 失败率 > 10%
  - JVM 内存 > 80%
```

详见: `SLA.md`

---

## 十、性能优化检查点

### 上线前必做

```
□ 接口加分页(列表接口必须有 limit)
□ N+1 查询检查(用 BatchMapper)
□ 索引检查(user_id, created_at 等高频查询字段)
□ Redis 缓存热点数据(任务模板、AI prompt)
□ DeepSeek 响应缓存(相同问题用 hash 缓存)
□ 图片走 CDN
□ 日志异步写
□ 慢 SQL 监控(> 1 秒告警)
```

---

## 十一、安全 Checklist

```
□ 所有接口加鉴权(Sa-Token)
□ 用户数据隔离(查 user_id = current_user.id)
□ 敏感字段加密(child_name 等)
□ HTTPS 全站
□ SQL 注入防护(MyBatis 参数化查询,不要拼字符串)
□ XSS 防护(返回时转义)
□ 接口限流(用户级 + IP 级)
□ 微信 API 签名验证
□ 异常信息不暴露(GlobalExceptionHandler)
□ 日志脱敏(openid、手机号)
```

---

## 一句话总结

> **「微信原生 + Spring Boot + TKE + APM」= 个人开发的最优组合**
>
> 学习成本最低,运维负担最小,扩展性最强。

---

*整理于 2026-06-23*
