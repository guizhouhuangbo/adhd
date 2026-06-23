# ADHD 助手小程序 - 完整产品规划

> 基于「Java 后端工程师 + ADHD 家长」双重身份的个人创业项目

---

## 一、项目背景

### 创始人画像

```
身份: Java 后端工程师 + ADHD 孩子的父亲
优势: 后端技术强 + 真实用户身份 + AI 应用经验
约束: 个人开发 + 业余时间(下班 6:30 后 2 小时) + 没 UI 设计经验
现有资源:
  ✅ 腾讯云 CVM(已有,跑着 Jenkins)
  ✅ 腾讯云 TKE 集群(已有)
  ✅ 腾讯云 Prometheus 监控(已用)
  ✅ Hertzbeat 监控(已部署)
  ✅ 注册了个人公司(刚办)
```

### 市场背景

```
中国 ADHD 儿童: 约 2000-2300 万
确诊率: < 10%
家庭付费意愿: 极高(平均年消费 5000+)
竞品: 国内基本空白,几个国外产品(Mightier、Brili、Inflow)
窗口期: AI 时代第一次能做「个性化+24h陪伴」
```

### 核心定位

**「家长侧的 ADHD 儿童行为管理 + AI 辅助训练工具」**

- ❌ 不是医疗工具(避开监管)
- ❌ 不是教育工具(避开 K12 监管)
- ✅ 是**家长助手**(规避监管,精准定位)

---

## 二、产品功能规划

### 核心模块(5 大模块)

#### 1. 🎯 行为打卡 + 代币奖励系统(核心)

**理论基础**: 代币奖励法 (Token Economy) 是 ADHD 行为干预的金标准

```
家长设定任务(写作业、收拾玩具、不打人等)
  ↓
孩子完成 → 拍照/确认 → 家长 review → 获得星星
  ↓
攒够星星兑换实物奖励(玩具、活动、零花钱)
```

**功能点**:
- 任务模板库(分年龄段、分场景)
- 自动化提醒(到点提醒)
- 数据可视化(每周/月进步曲线)
- 多人协作(爸爸妈妈、爷爷奶奶共同打卡)

#### 2. 🧠 专注力训练小游戏

基于循证的训练方法:
- 持续注意力(划消任务)
- 选择性注意(不被干扰找目标)
- 工作记忆(数字/图形记忆)
- 执行功能(计划、分类)

**关键**: 每日 10-15 分钟,有进度系统、有奖励、有家长报告

#### 3. 📅 时间管理可视化

ADHD 孩子最大特征是「时间感知差」:

- 倒计时番茄钟(可视化沙漏/进度条)
- 每日时间盒子(图形化日程表)
- 任务拆分工具(把大任务拆成小步骤)

#### 4. 📊 家长报告 + 行为模式分析

每周生成报告:
- 哪些行为改善了
- 哪些时间段表现差(发现规律)
- 建议家长下周重点关注什么
- AI 分析家长记录,给个性化建议

#### 5. 📚 家长学习中心

- ADHD 知识科普
- 视频课程
- 家长社区
- 专家答疑(付费咨询)

---

## 三、AI Agent 集成规划

### 5 个 AI Agent 角色

#### Agent 1: 「家长辅导员」(最核心)
- 家长焦虑时立即得到专业支持
- 共情 + 给出可操作方案
- 像「心理咨询师 + 闺蜜」

#### Agent 2: 「孩子的小伙伴」
- 孩子写作业时陪伴
- 不是教师,是同伴(让孩子不孤独)

#### Agent 3: 「数据分析师」
- 后台默默分析孩子数据
- 每周主动推送洞察
- 发现规律 + 给建议

#### Agent 4: 「任务拆分助手」
- 家长说「整理书包」
- Agent 自动拆成 5-7 个可执行步骤

#### Agent 5: 「专家咨询机器人」(付费功能)
- 家长付费的高端服务
- ¥9.9/次咨询(对比真人专家 200-500 元)

---

## 四、技术架构

### 技术栈选型(后端工程师友好版)

```
前端: 微信小程序原生 + Vant Weapp + 微信开发者工具
后端: Spring Boot 3 + MyBatis-Plus + MySQL 8 + Sa-Token + WxJava + Hutool
AI:   DeepSeek API(直接 HTTP 调,5 行 Java 代码)
部署: 你 TKE 集群(自动注入腾讯云 APM)
监控: Hertzbeat(已有) + 腾讯云 APM
```

### 关键决策

| 决策 | 选择 | 理由 |
|---|---|---|
| 前端框架 | **微信原生** | 后端工程师友好,学习成本最低 |
| ~~不用 Taro/React~~ | 排除 | 个人开发过度工程 |
| ~~不用 uniapp~~ | 排除 | 只做微信小程序,跨端用不上 |
| UI 组件库 | **Vant Weapp** | 现成组件,UI 70 分起步 |
| 后端框架 | **Spring Boot 3** | 你强项 |
| 数据库 | **MySQL 8** | 你熟悉 |
| AI 接入 | **DeepSeek HTTP 调用** | 不用 LangChain,5 行代码 |
| 部署 | **TKE + annotation 注入 APM** | 利用现有 TKE 集群 |

### 完整架构图

```
┌─────────────────────────────────────────────┐
│  微信小程序(原生 + Vant Weapp)               │
│  ├─ pages/home/   首页(任务+进度)            │
│  ├─ pages/task/   任务详情                    │
│  ├─ pages/chat/   AI 对话                     │
│  ├─ pages/report/ 周报                        │
│  └─ pages/profile/ 我的                       │
└────────────────┬────────────────────────────┘
                 │ HTTPS API
                 ↓
┌─────────────────────────────────────────────┐
│  TKE 集群(已有)                              │
│  ┌────────────────────────────────────────┐ │
│  │  Java 后端(Spring Boot)               │ │
│  │  + 自动注入腾讯云 APM agent             │ │
│  │  ├─ Controller    REST API             │ │
│  │  ├─ Service       业务逻辑              │ │
│  │  ├─ Mapper        数据访问              │ │
│  │  └─ Integration   微信 / DeepSeek      │ │
│  └────────────────────────────────────────┘ │
└──────────┬──────────────────────────────────┘
           │
    ┌──────┴──────┐
    ↓             ↓
┌────────┐  ┌───────────────────┐
│ MySQL  │  │ 外部服务调用       │
│        │  │ - 微信 API         │
└────────┘  │ - DeepSeek         │
            │ - 微信支付          │
            └───────────────────┘
```

---

## 五、数据库设计

### 核心表结构(4 张表起步)

```sql
-- 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    openid VARCHAR(64) UNIQUE NOT NULL,
    nickname VARCHAR(64),
    avatar VARCHAR(255),
    child_name VARCHAR(32),
    child_age INT,
    child_gender TINYINT,
    total_stars INT DEFAULT 0,
    membership_type VARCHAR(32),
    membership_expire DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 任务表
CREATE TABLE task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    sub_steps JSON,
    reward_stars INT DEFAULT 1,
    schedule VARCHAR(64),
    is_active TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
);

-- 打卡记录
CREATE TABLE check_in (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    completed TINYINT DEFAULT 0,
    stars_earned INT DEFAULT 0,
    note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, created_at)
);

-- AI 对话
CREATE TABLE chat_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    agent_type VARCHAR(32),
    role VARCHAR(16),
    content TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
);

-- 订单表(收钱后加)
CREATE TABLE `order` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    plan_id INT NOT NULL,
    amount INT NOT NULL,  -- 单位:分
    status TINYINT DEFAULT 0,  -- 0待支付 1已支付 2已退款
    transaction_id VARCHAR(64),
    pay_time DATETIME,
    expire_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 六、商业模式

### 定价策略

| 套餐 | 价格 | 适合 |
|---|---|---|
| **体验版** | 免费 | 拉新 |
| **月度会员** | ¥39/月 | 试用心理 |
| **年度会员** | **¥299/年** ⭐ | 主推 |
| **家庭年卡** | ¥499/年 | 多孩家庭 |
| **专家咨询** | ¥99/次 | 高客单价 |
| **课程包** | ¥199/套 | 一次性 |

### 收入预估(保守版)

| 月份 | 总用户 | 付费用户 | 月收入 | 累计收入 |
|---|---|---|---|---|
| 第 6 月 | 100 | 5 | ¥195 | ¥500 |
| 第 12 月 | 1000 | 100 | ¥3900 | ¥1.5w |
| 第 18 月 | 5000 | 750 | ¥2.9w | ¥15w |
| 第 24 月 | 15000 | 2250 | ¥8.8w | ¥80w |
| 第 36 月 | 50000 | 7500 | ¥29w | ¥350w |

⚠️ **复利的恐怖在最后半年**,大部分人死在第 12 个月。

### 二次变现(后期)

- 专家在线咨询(抽佣 30%)
- ADHD 夏令营/线下营(流量分成)
- 奖励兑换电商(玩具/课程,抽佣)
- 测评(韦氏智商、Conners 量表,单次付费)

---

## 七、12 周开发路线图(下班 2 小时节奏)

### Phase 1: 沉浸 + 备案(Week 1-4)

**不写代码,做这些**:

```
✅ 加 5 个 ADHD 家长群,潜伏听
✅ 跟 20 个家长聊 30 分钟
✅ 读《如何养育多动症孩子》(罗素·巴克利)
✅ 写「创始人日记」(每天 1 条)
✅ 域名采购 + ICP 备案(critical path,15-25 天)
✅ 注册小程序企业账号
✅ 申请 DeepSeek API Key
✅ 用 Excel 给自家孩子做 2 周代币打卡(验证产品逻辑)
```

### Phase 2: 后端开发(Week 5-8)

```
Week 5: Spring Boot 项目骨架 + MyBatis-Plus + MySQL
Week 6: 微信登录(WxJava + Sa-Token)+ 用户/任务表
Week 7: 任务 CRUD + 打卡 + 周报接口
Week 8: AI 接入(DeepSeek HTTP)
```

### Phase 3: 小程序开发(Week 9-12)

```
Week 9:  学小程序基础 + Vant Weapp + 5 个页面骨架
Week 10: 首页 + 任务列表 + 任务详情
Week 11: AI 对话 + 周报 + 我的
Week 12: 联调 + 调试 + 部署到 TKE
```

### Phase 4: 内测 + 公测(Week 13-20)

```
Week 13-14: 自家内测 2 周
Week 15-17: 种子用户 10 人内测(从 ADHD 家长群来)
Week 18-20: 微信审核 + 上线公测
```

### Phase 5: 收钱验证(Week 21-24)

```
Week 21: 申请微信支付商户号
Week 22: 接入支付代码
Week 23: 上线月度会员 ¥39
Week 24: 收第一笔钱 🎉
```

---

## 八、关键决策记录

### 已做出的决策

| 决策 | 选择 | 理由 |
|---|---|---|
| 创业方向 | ADHD 行为辅助小程序 | 创始人自己是用户 |
| 平台选择 | 微信小程序 | 国内最大入口 |
| 前端技术 | 微信原生 + Vant | 学习成本最低 |
| 后端技术 | Spring Boot + MySQL | 利用 Java 强项 |
| AI 接入 | DeepSeek HTTP 直调 | 简单,不用框架 |
| 部署 | TKE 集群 + APM 自动注入 | 复用现有资源 |
| 主体类型 | 个人公司(已注册) | 长期发展 |
| 收钱时机 | MVP 后 Month 4 起 | 先验证再收钱 |

### 不要做的事(避坑)

| ❌ 不要 | 原因 |
|---|---|
| 一上来就用 Taro/React | 个人开发过度工程 |
| MVP 阶段堆 AI 功能 | 先验证用户再加 |
| 视觉识别 / YOLO | MVP 不需要 |
| 自动续费 | 合规风险高 |
| 在小程序端跑 AI 推理 | 模型大、慢、耗电 |
| 用医疗词汇(ADHD/治疗) | 审核风险 |
| 上来就做 90 分 UI | Vant 70 分够用 |
| 上班时间做副业 | 法律 + 道德风险 |

---

## 九、风险与对策

### 风险 1: 政策风险(医疗 / 教育监管)

**对策**: 严格定位「家庭辅助工具」,避开医疗词汇

### 风险 2: 微信审核被拒

**对策**: 内容合规审查 + 小心起名(参考支付指南)

### 风险 3: 个人精力不足

**对策**: 节奏控制(下班 2h + 周末 4h),不熬夜不影响主业

### 风险 4: 没有用户

**对策**: 小红书 + ADHD 家长群运营,自家试用先

### 风险 5: 技术债务

**对策**: 用最简方案(Vant + Spring Boot),不过度设计

---

## 十、监控与 SLA

### MVP 阶段(月活 < 1 万)

**目标 SLA: 99%**(全年允许停机 87 小时)

```
✅ Hertzbeat 接口探针 + 钉钉告警(已配)
✅ Spring Boot Actuator
✅ MySQL 每日备份
✅ K8s readinessProbe + 优雅停机
```

详见: `SLA.md`

---

## 十一、下一步行动(Today)

### 今晚下班后 2 小时

```
🔴 P0(关键路径,5 分钟操作 + 25 天等待):
   1. 注册腾讯云 + 实名(若没注册)
   2. 买域名(¥40-70,推荐你拼音.com)
   3. 启动 ICP 备案(企业主体,因为已有公司)

🟡 P1(并行):
   4. 申请 DeepSeek API Key(免费 500w token)
   5. 注册小程序企业账号(用公司主体)

🟢 P2(明天起):
   6. 加 1 个 ADHD 家长群
   7. 写「创始人日记」第 1 条
   8. 下单买《如何养育多动症孩子》
```

### 本周必做

```
□ ICP 备案提交
□ 公司公户开户(去银行)
□ 微信小程序企业主体认证(¥300)
□ 申请微信支付商户号
□ 加 3 个 ADHD 家长群,潜伏听
□ 读完《如何养育多动症孩子》前 3 章
```

---

## 一句信念

> **「我做的不是一个 App,是给 2300 万孤立无援的家庭一个出口。」**
>
> 你是用户 + 你是工程师,**这是最强护城河**。
>
> 6 个月后,告诉我第一个付费用户的故事 ❤️

---

*整理于 2026-06-23*

*相关文档:*
- *SLA.md - 可用性保障 25 招*
- *miniprogram-payment-guide.md - 支付接入指南*
- *adhd-tech-architecture.md - 技术架构详解(待创建)*
- *adhd-ideas-backlog.md - 灵感库(待创建)*
