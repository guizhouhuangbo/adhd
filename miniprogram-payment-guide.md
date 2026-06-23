# 小程序微信支付接入指南

> 个人 / 个体工商户 / 公司 - 小程序支付从 0 到 1 完整路径

---

## 目录

- [一、能不能收钱?主体资质要求](#一能不能收钱主体资质要求)
- [二、个人 vs 个体 vs 公司 对比](#二个人-vs-个体-vs-公司-对比)
- [三、完整准备清单(上线前必做)](#三完整准备清单上线前必做)
- [四、微信支付接入流程](#四微信支付接入流程)
- [五、技术对接(代码层)](#五技术对接代码层)
- [六、必须避开的 10 个坑](#六必须避开的-10-个坑)
- [七、合规与法律](#七合规与法律)
- [八、定价策略建议](#八定价策略建议)
- [九、ADHD 小程序专用方案](#九adhd-小程序专用方案)
- [十、上线后运营要点](#十上线后运营要点)

---

## 一、能不能收钱?主体资质要求

### 关键事实

| 主体类型 | 能开通微信支付? | 备注 |
|---|---|---|
| **个人主体小程序** | ❌ **绝对不能** | 这是硬限制 |
| **个体工商户** | ✅ 可以 | 最低门槛,推荐 |
| **个人独资企业** | ✅ 可以 | 类似个体户 |
| **有限公司** | ✅ 可以 | 最稳 |

### 你的情况:已注册个人公司

**✅ 完全 OK,可以接微信支付**

但还要确认:
- 营业执照已下来
- 经营范围**包含**:软件开发 / 信息技术服务 / 互联网信息服务
- 已开**对公账户**(公户)

---

## 二、个人 vs 个体 vs 公司 对比

| 维度 | 个体工商户 | 有限公司 |
|---|---|---|
| **注册成本** | ¥0-200 | ¥500-3000 |
| **注册时间** | 1-3 天(网上) | 7-15 天 |
| **年费** | 极低(几乎 0) | 几千(代账等) |
| **税率** | 经营所得税(超额累进) | 企业所得税 25% + 个税 |
| **股权融资** | ❌ 不能 | ✅ 可以 |
| **责任** | 无限连带 | 有限责任 |
| **微信支付费率** | 0.6% | 0.6% |
| **适合阶段** | MVP / 早期 / 自营 | 长期 / 融资 / 团队 |

**你的选择**:
- **MVP 验证期**:个体工商户够用
- **打算融资 / 团队化 / 长期做大**:有限公司

---

## 三、完整准备清单(上线前必做)

### 法律 / 资质类

```
□ 1. 营业执照(已有 ✅)
□ 2. 对公账户(公户)
□ 3. 法人身份证
□ 4. 经营范围包含"软件开发""信息技术服务"
□ 5. 域名(已有/已购)
□ 6. ICP 备案(15-25 天 critical path,先启动)
□ 7. 网站名称备案通过
□ 8. (虚拟商品)文化经营许可证(部分品类需要,先咨询)
```

### 微信生态类

```
□ 9. 小程序账号(企业主体)
□ 10. 小程序认证(¥300/年,企业必做)
□ 11. 微信支付商户号(申请 1-3 天)
□ 12. 商户号关联小程序 AppID
□ 13. 微信支付 API 密钥(V3)
□ 14. 微信支付证书(API 调用需要)
```

### 技术类

```
□ 15. HTTPS 域名(微信支付硬要求)
□ 16. 后端服务上线(HTTPS)
□ 17. 微信支付回调接口(/api/pay/notify)
□ 18. 订单表 + 支付记录表
□ 19. 异步回调验签代码
□ 20. 幂等处理逻辑
□ 21. 金额校验逻辑
□ 22. 退款流程
□ 23. 对账机制(每日)
□ 24. 监控告警(支付异常)
```

### 业务类

```
□ 25. 用户服务协议
□ 26. 隐私政策
□ 27. (涉未成年人)儿童隐私政策
□ 28. 退款政策(明确告知用户)
□ 29. 客服渠道(微信原生客服 / 电话)
□ 30. 发票申请功能(可选)
```

---

## 四、微信支付接入流程

### Step 1: 注册微信支付商户号

1. 访问 https://pay.weixin.qq.com/
2. 注册商户号(用你公司主体)
3. 提交资料:
   - 营业执照
   - 法人身份证
   - 对公账户信息
   - 经营场景(选小程序)
4. 等审核(1-3 天)
5. 拿到 **商户号 mch_id**(类似 1234567890)

### Step 2: 关联小程序

1. 登录微信支付商户平台
2. **产品中心 → AppID 账号管理**
3. **关联小程序 AppID**
4. 小程序后台 → **微信支付** → 同意关联

### Step 3: 拿到 API 密钥和证书

**两种 API 版本,推荐 V3**:

#### V3(推荐)
- **API V3 密钥**(32 位字符串)
- **商户私钥**(apiclient_key.pem)
- **商户证书**(apiclient_cert.pem)
- **微信支付平台证书**(微信下发)

#### V2(旧版,不推荐)
- **API V2 密钥**(32 位字符串)
- 即将废弃,新接入别选

### Step 4: 配置回调地址

商户平台 → **API 安全 → 设置回调 URL**:
```
https://api.你的域名.com/api/pay/notify
```

(必须 HTTPS!)

---

## 五、技术对接(代码层)

### 推荐: Java + WxJava SDK

```xml
<dependency>
    <groupId>com.github.binarywang</groupId>
    <artifactId>weixin-java-pay</artifactId>
    <version>4.6.0</version>
</dependency>
```

### 支付流程

```
小程序                    你的后端                  微信支付
   │                         │                          │
   │ 1. 用户点击购买          │                          │
   ├────────────────────────>│                          │
   │                         │ 2. 创建订单 (JSAPI)      │
   │                         ├─────────────────────────>│
   │                         │                          │
   │                         │ 3. 返回 prepay_id        │
   │                         │<─────────────────────────┤
   │                         │                          │
   │ 4. 返回支付参数          │                          │
   │<────────────────────────┤                          │
   │                         │                          │
   │ 5. 调起微信支付 wx.requestPayment()                 │
   ├────────────────────────────────────────────────────>
   │                         │                          │
   │ 6. 用户输密码 → 微信扣款 │                          │
   │                         │                          │
   │                         │ 7. 异步通知 (回调)        │
   │                         │<─────────────────────────┤
   │                         │                          │
   │                         │ 8. 返回 SUCCESS,更新订单 │
   │                         ├─────────────────────────>│
```

### 核心代码骨架

#### 1. 创建支付订单(后端)

```java
@RestController
@RequestMapping("/api/pay")
public class PaymentController {

    @Autowired
    private WxPayService wxPayService;

    @PostMapping("/order")
    @SaCheckLogin
    public Result createOrder(@RequestBody PayRequest req) {
        // 1. 创建本地订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setOpenid(req.getOpenid());
        order.setAmount(getPlanPrice(req.getPlanId()));  // 单位:分
        order.setStatus(0);
        orderMapper.insert(order);

        // 2. 调微信下单
        WxPayUnifiedOrderV3Request payReq = new WxPayUnifiedOrderV3Request();
        payReq.setOutTradeNo(order.getOrderNo());
        payReq.setDescription("ADHD助手会员-" + req.getPlanName());
        payReq.setNotifyUrl("https://api.你的域名.com/api/pay/notify");
        payReq.setAmount(new WxPayUnifiedOrderV3Request.Amount().setTotal(order.getAmount()));
        payReq.setPayer(new WxPayUnifiedOrderV3Request.Payer().setOpenid(req.getOpenid()));

        WxPayUnifiedOrderV3Result.JsapiResult payResult =
            wxPayService.createOrderV3(TradeTypeEnum.JSAPI, payReq);

        // 3. 返回给小程序
        return Result.ok(payResult);
    }
}
```

#### 2. 处理回调(后端)

```java
@PostMapping("/notify")
public Map<String, String> notify(@RequestBody String body, HttpServletRequest request) {
    String signature = request.getHeader("Wechatpay-Signature");
    String timestamp = request.getHeader("Wechatpay-Timestamp");
    String nonce = request.getHeader("Wechatpay-Nonce");
    String serial = request.getHeader("Wechatpay-Serial");

    try {
        // 1. 验签
        SignatureHeader signatureHeader = new SignatureHeader();
        signatureHeader.setSignature(signature);
        signatureHeader.setTimeStamp(timestamp);
        signatureHeader.setNonce(nonce);
        signatureHeader.setSerial(serial);

        WxPayOrderNotifyV3Result result =
            wxPayService.parseOrderNotifyV3Result(body, signatureHeader);

        WxPayOrderNotifyV3Result.DecryptNotifyResult decrypt = result.getResult();
        String outTradeNo = decrypt.getOutTradeNo();

        // 2. 幂等处理(关键!)
        Order order = orderMapper.selectByOrderNo(outTradeNo);
        if (order.getStatus() == 1) {
            return Map.of("code", "SUCCESS", "message", "OK");
        }

        // 3. 校验金额(关键!防止恶意篡改)
        if (!decrypt.getAmount().getTotal().equals(order.getAmount())) {
            throw new RuntimeException("金额不匹配");
        }

        // 4. 更新订单 + 开通会员
        if ("SUCCESS".equals(decrypt.getTradeState())) {
            order.setStatus(1);
            order.setPayTime(LocalDateTime.now());
            order.setTransactionId(decrypt.getTransactionId());
            orderMapper.updateById(order);

            // 5. 业务处理
            userService.upgradeMembership(order.getOpenid(), order.getPlanId());
        }

        return Map.of("code", "SUCCESS", "message", "OK");

    } catch (Exception e) {
        log.error("支付回调处理失败", e);
        return Map.of("code", "FAIL", "message", e.getMessage());
    }
}
```

#### 3. 小程序前端

```javascript
async function pay() {
  // 1. 调后端下单
  const { data } = await wx.request({
    url: 'https://api.你的域名.com/api/pay/order',
    method: 'POST',
    data: { planId: 1, openid: wx.getStorageSync('openid') }
  })

  // 2. 调起微信支付
  wx.requestPayment({
    timeStamp: data.timeStamp,
    nonceStr: data.nonceStr,
    package: data.package,
    signType: data.signType,
    paySign: data.paySign,

    success(res) {
      wx.showToast({ title: '支付成功' })
      // 3. 轮询订单状态确认
      pollOrderStatus(data.orderNo)
    },
    fail(err) {
      if (err.errMsg.includes('cancel')) {
        wx.showToast({ title: '已取消', icon: 'none' })
      } else {
        wx.showToast({ title: '支付失败', icon: 'error' })
      }
    }
  })
}
```

---

## 六、必须避开的 10 个坑

### 🚨 坑 1: 没做幂等,会员被开通多次

**症状**: 微信回调可能重发,业务处理多次 → 用户会员开通了 3 个月

**解决**: 处理前查订单状态,已处理直接返回 SUCCESS

### 🚨 坑 2: 不校验金额,被篡改

**症状**: 用户改小程序代码,把 ¥99 改成 ¥1 调起支付

**解决**: **金额永远由后端决定**,回调验金额一致性

### 🚨 坑 3: 用前端传的订单号

**症状**: 同一订单号支付多次

**解决**: **订单号永远后端生成**(雪花算法 / UUID)

### 🚨 坑 4: 回调没回 SUCCESS,微信反复重试

**症状**: 业务处理报错,抛 500 → 微信认为没收到 → 8 次重试后放弃 → 你永远丢失通知

**解决**:
```java
// 务必返回:
return Map.of("code", "SUCCESS", "message", "OK");
```
业务失败 try-catch,**只要确认收到了就回 SUCCESS**

### 🚨 坑 5: 用户支付后前端断网

**症状**: 用户付完关掉小程序,前端没收到 success 回调,但后端收到了

**解决**:
- 用户重新打开主动查未完成订单
- 「我的订单」页能看到已支付
- **不依赖前端 success 回调**

### 🚨 坑 6: 退款不闭环

**症状**: 微信退款成功,后端没收到通知,用户会员没收回

**解决**: 单独做退款通知接口 + 定时任务轮询补漏

### 🚨 坑 7: 没做支付有效期

**症状**: 用户下单 1 周后才付,价格已变

**解决**: 下单时记录 `expire_time`(15 分钟),过期订单拒绝

### 🚨 坑 8: 测试环境用生产商户号

**症状**: 测试时扣了真钱

**解决**: 用自己微信小额(0.01-1 元)测试,严格 review

### 🚨 坑 9: 没记录交易凭证

**症状**: 用户来投诉「没收到会员」,你没数据查

**解决**: 存全 `transaction_id`,关键字段持久化

### 🚨 坑 10: 客服功能没做

**症状**: 用户有问题,没地方反馈,直接微信投诉 → 商户号被冻结

**解决**: 小程序必须有「联系客服」入口(微信原生客服 + 电话)

---

## 七、合规与法律

### ⚠️ 风险 1: 内容合规(尤其 ADHD 项目)

**ADHD 是医疗相关词汇**,小程序审核可能拒:

| ❌ 不要用 | ✅ 推荐用 |
|---|---|
| ADHD 治疗 | 专注力训练 |
| 多动症康复 | 儿童行为习惯养成 |
| 诊断 | 辅助工具 |
| 治愈 | 改善 |
| 替代医生 | 家庭辅助 |

### ⚠️ 风险 2: 退款政策

**虚拟商品也必须有退款渠道**(国家法律要求):

- 自动续费明确告知
- 提供「申请退款」入口
- 处理时效写清楚(建议 7 天)

### ⚠️ 风险 3: 用户协议 + 隐私政策

**必须有**(微信审核硬要求):
- 用户服务协议
- 隐私政策
- 涉及儿童的:儿童隐私保护政策

**模板**: 用 AI 起草 → 律师 review 1 小时(¥500 左右)

### ⚠️ 风险 4: 自动续费违规

**新手强烈建议**:**第一版不要做自动续费**,只做单次购买。

如果做:
- 明显展示自动续费规则
- 容易取消(不能藏)
- 续费 **48 小时前发提醒**(法律要求)
- 苹果系统额外要求

### ⚠️ 风险 5: 费率成本

| 商户类型 | 微信支付费率 |
|---|---|
| 普通商户 | **0.6%** |
| 部分行业(餐饮等) | 0.38% |

例:用户付 ¥100,你实收 **¥99.4**

### ⚠️ 风险 6: 提现限制

- T+1 可以提现到公户
- 第一次提现需要银行卡验证
- 个体户每天提现限额(几十万,够用)

---

## 八、定价策略建议

### ADHD 项目推荐价格

| 套餐 | 价格 | 适合 |
|---|---|---|
| **体验版** | 免费 | 拉新 |
| **月度会员** | ¥39/月 | 试用心理 |
| **年度会员** | **¥299/年** ⭐ | 主推(月均 25,比月度便宜 36%) |
| **家庭年卡** | ¥499/年 | 多孩家庭 |
| **专家咨询** | ¥99/次 | 高客单价 |
| **课程包** | ¥199/套 | 一次性 |

### 定价心理学

- **永远展示「年付划算」**(锚定对比)
- **设置「锚点价」**(原价 ¥599,现价 ¥299)
- **限时优惠制造紧迫感**

---

## 九、ADHD 小程序专用方案

### 商业模式建议

```
免费版:
  ✅ 基础打卡(3 个任务)
  ✅ 简单训练游戏(3 个)
  ✅ 周报(基础数据)
  ❌ AI 拆任务
  ❌ 家长咨询
  ❌ 进阶训练
  ❌ 数据导出

基础版 (¥39/月 或 ¥299/年):
  ✅ 全部打卡功能
  ✅ AI 任务拆分
  ✅ 全部训练游戏
  ✅ 详细周报 + 月报
  ✅ AI 家长助手对话

专业版 (¥99/月 或 ¥899/年):
  ✅ 基础版全部
  ✅ 1 次/月专家在线咨询
  ✅ 个性化训练方案
  ✅ 数据导出 PDF
  ✅ 优先客服

家庭版 (¥149/月 或 ¥1499/年):
  ✅ 专业版全部
  ✅ 多孩子账户
  ✅ 家长论坛
  ✅ 月度线上沙龙
```

### 收钱时间表

```
Month 1-3: 不收钱,验证产品 + 积累 100 用户
Month 4:   推出月度版 ¥39,看转化率
Month 5:   推出年度版 ¥299(主推)
Month 6:   推出专家咨询 ¥99/次
Month 7+:  加增值服务(课程、社群、家庭版)
```

### 第一笔收入路径

```
本周:
  ✅ 营业执照 + 公户(已有)
  ✅ ICP 备案启动(15-25 天)

下月:
  ✅ 小程序企业主体注册
  ✅ 微信认证 ¥300
  ✅ 申请微信支付商户号
  ✅ 等审核 1-3 天

3 个月后:
  ✅ 接入支付代码
  ✅ 上线月度版
  ✅ 收到第一笔 ¥9.9(优惠价)
```

---

## 十、上线后运营要点

### 必做的日常

```
每日:
  □ 查微信支付账单
  □ 对账(微信账单 vs 你数据库)
  □ 异常告警(订单异常 / 回调失败)
  □ 客服回复

每周:
  □ 转化漏斗分析(浏览 → 试用 → 付费)
  □ 退款率统计
  □ 用户反馈整理

每月:
  □ 提现到公户
  □ 报税(个体户季度报)
  □ SLA 报告(支付成功率)
  □ 用户增长复盘
```

### 监控告警(必配)

```yaml
告警类:
  - 支付回调失败 → 钉钉告警
  - 订单状态异常 → 钉钉
  - 支付成功率 < 95% → 钉钉
  - 退款异常 → 钉钉
  - 商户余额异常 → 钉钉
```

### 客服建设

```
入口:
  - 小程序「我的 → 联系客服」
  - 微信原生客服(免费)
  - 电话(对外公示)

模板回复(写好备用):
  - 如何申请退款
  - 如何查看订单
  - 如何续费
  - 投诉处理
```

---

## 上线 Checklist(打印贴墙)

```
□ 营业执照(经营范围正确)
□ 对公账户已开
□ 小程序企业主体认证
□ 微信支付商户号开通
□ 商户号关联小程序
□ 域名 + ICP 备案完成
□ HTTPS 证书已配
□ API 密钥 + 证书已配
□ 回调接口已上线返回 SUCCESS
□ 验签代码已写
□ 幂等处理已写
□ 金额校验已写
□ 订单超时清理任务
□ 退款流程已测试
□ 已用真钱小额测试(0.01-1 元)
□ 客服入口已加
□ 用户协议 + 隐私政策
□ 监控告警已配
□ 对账机制已建
```

---

## 一句忠告

> **「不要为了做支付而做支付」**
>
> 很多创业者一上来就琢磨怎么收钱,结果产品没人用。
>
> **正确顺序**:做出能用的产品 → 100 个真实用户 → 验证愿意付钱 → 才接支付
>
> 但 **ICP 备案 15 天就先启动**,可以并行准备。

---

## 我的最终建议(给你的)

### 已有: 公司主体 ✅

剩下要做的:

```
本周(并行):
  🔴 启动 ICP 备案
  🟡 注册企业主体小程序
  🟢 申请微信支付商户号

下月:
  ✅ 等审核通过
  ✅ 准备技术接入
  ✅ 写测试用例

3 个月后:
  ✅ 真正上线收钱
```

**别现在就堆支付代码**,先确保「**产品有人用**」。

---

*整理于 2026-06-23*

*相关文档: SLA.md(可用性保障)、adhd-product-roadmap.md(产品路线图,待创建)*
