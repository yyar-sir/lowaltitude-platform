# 天枢 · 低空智能运营管控平台

面向低空物流的数字化运营管控系统：实现低空飞行 **「申请 — 审批 — 调度 — 监控 — 结算」** 全流程线上管理。

**三端**：终端客户 / 物流运营商家 / 平台管理员 · **七大模块**：账号与权限 · 订单全流程 · 低空资源 · 空域合规 · 数据统计 · 应急与客服 · 系统配置

- **后端**：Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + JWT（jjwt 0.12.6）+ AOP 两级权限
- **前端**：Vue 3 + Vite 6 + Element Plus + ECharts
- **数据库**：H2 单文件（`MODE=MySQL`）—— 零安装零密码，重启不丢数据，可一键切回 MySQL
- **大模型**：OpenAI 兼容协议（默认 DeepSeek），限流 + 内容安全 + 三级降级 + 密钥脱敏

> **本人负责模块**：订单全流程管理（创建 → 承接 → 调度 → 跟踪 → 结算），是本平台的核心业务载体。

---

## 一、快速启动

### 1. 后端（端口 8080）

```powershell
cd backend
mvn spring-boot:run
```

首次启动自动完成：创建 `backend/data/lowaltitude.mv.db` → 执行 `h2/schema.sql` 建 16 张表 → 执行 `h2/data.sql` 灌入演示数据。
每次启动重建表结构（`DROP TABLE IF EXISTS` + `CREATE TABLE`），便于反复调试。

### 2. 前端（端口 5173）

```powershell
cd frontend
npm install
npm run dev
```

打开 <http://127.0.0.1:5173>。`vite.config.js` 已把 `/api` 代理到 `127.0.0.1:8080`。

### 3. 演示账号（密码均为 `123456`）

| 角色 | 账号 | 登录后入口 | 说明 |
|---|---|---|---|
| 平台管理员 | `admin` | `/admin/dashboard` | 全平台监管、空域规则、计划审批 |
| 物流运营商家 | `merchant01` | `/merchant/workbench` | 接单、调度、资源与定价管理 |
| 终端客户 | `customer01` | `/customer/home` | 下单、跟踪、结算评价 |

另有 `merchant02`（云翼科技）、`customer02`（张伟）可用于交叉验证数据范围隔离。

> H2 控制台（可选）：<http://127.0.0.1:8080/h2-console>，JDBC URL 填 `jdbc:h2:file:./data/lowaltitude`，用户名 `sa`，密码留空。

---

## 二、订单全流程（核心模块）

状态机集中维护在 `OrderService`，**每个变更方法都先校验当前状态**，非法跳转直接拒绝——即使前端被绕过也造不出脏数据。

```
1 待接单 --接单--> 2 待调度 --调度--> 3 飞行中 --送达--> 4 已送达 --结算--> 5 已完成
   |                    |
   +---拒单/取消--------> 6 已取消
```

| 阶段 | 关键约束 |
|---|---|
| 创建 | 寄收地址不能相同；收件地址命中禁飞区/临时管制区直接拒单；按商家定价规则预估费用 |
| 承接 | 用**状态条件更新**（`where order_status=1`）抢单，并发下只有一个商家能接到 |
| 调度 | 三重复核：飞行器可用 + 载重足够 + 电量 ≥30%；机长在岗；场站归属校验 |
| 跟踪 | 按时间累积轨迹点；异常类型非 0 时自动推送给客户与商家 |
| 结算 | 订单与结算一对一（唯一约束）；重复结算被拒；评价随结算记录保存 |

**数据范围收敛**：客户只看自己的订单、商家只看自己商家的订单、管理员看全平台——范围在**服务端**按登录态收敛，不依赖前端隐藏菜单。

---

## 三、目录结构

```
lowaltitude-platform/
├── backend/
│   ├── sql/                       # MySQL 版建表与演示数据（保留备查）
│   └── src/main/
│       ├── java/com/example/eduplatform/
│       │   ├── common/            # Result / BizException / 全局异常处理
│       │   ├── config/            # WebConfig(CORS+拦截器) / Jackson 时间格式 / MyBatis-Plus
│       │   ├── controller/        # 11 个 Controller，仅做参数校验与调用
│       │   ├── dto/ vo/           # 入参与出参
│       │   ├── entity/ mapper/    # 16 张表实体与 Mapper
│       │   ├── security/          # JWT / 拦截器 / @RequireRole / 切面 / ThreadLocal
│       │   └── service/           # 业务逻辑与事务边界
│       └── resources/
│           ├── application.yml          # H2 数据源、SQL 初始化、JWT 占位
│           ├── application-local.yml    # 本地配置（已 gitignore）
│           └── h2/{schema,data}.sql     # H2 版建表与演示数据
└── frontend/src/
    ├── api/                       # 唯一请求出口（request.js）+ 10 个业务模块
    ├── router/index.js            # 三端路由 + 角色守卫
    ├── styles/main.css            # 设计系统 Token（全站唯一来源）
    └── views/{customer,merchant,admin,shared}/   # 22 个页面
```

---

## 四、功能矩阵

**终端客户（8 页）**：首页 · 创建订单 · 我的订单 · 订单跟踪 · 订单结算 · 反馈与应急 · 消息中心 · 个人中心
**物流运营商家（10 页）**：工作台 · 订单调度 · 订单跟踪 · 低空资源 · 定价规则 · 飞行计划 · 违规记录 · 运营数据 · 消息中心 · 账号资料
**平台管理员（6 页）**：数据看板 · 用户与商家 · 订单监管 · 空域与合规 · 应急与客服 · 系统配置

---

## 五、关键设计

| 主题 | 做法 | 为什么 |
|---|---|---|
| 统一返回体 | `{code, msg, data}`，`code=0` 成功 | 前端只在一处解包，错误码语义稳定 |
| 权限两层 | `AuthInterceptor` 管「有没有登录」，`@RequireRole` 管「这个角色能不能调」 | 混在一起会让 401/403 语义混乱，把用户反复踢出登录 |
| 401 / 403 | 401 清 token 跳登录，403 只提示 | 403 当登录失效处理会误伤有权限问题的用户 |
| token 失效判定 | 除验签与过期外，还校验「账号未禁用」+「密码未在签发后被修改」 | JWT 无法撤销；这样改密 = 下线所有旧设备 |
| 订单状态机 | 变更前校验前置状态，条件更新抢单 | 防越权跳转与并发抢单 |
| 资源启停管控 | 停用的飞行器无法被调度（后端强制），前端下拉同步标记 | 只做前端隐藏，直接调接口就绕过去了 |
| 大模型 | 当成不可靠外部依赖：按用户限流 + 内容安全 + 三级降级 + 密钥脱敏 | 大模型不可用时平台其他功能照常运行 |
| 数据库 | H2 单文件（`MODE=MySQL`） | 克隆即跑；表结构与 MySQL 版一致，切回只需改 2 行配置 |
| 时间格式 | 输出统一 `yyyy-MM-dd HH:mm:ss`，输入兼容带秒/不带秒 | `spring.jackson.date-format` 对 `LocalDateTime` 无效 |

### 大模型三级降级

| 层级 | 失败场景 | 处理 |
|---|---|---|
| 一级 | 网络异常 / 超时 / 非 200 | 返回**场景化**预设文案（问答、调度建议、合规检查各不同） |
| 二级 | 调度建议生成失败 | 回退为**不依赖模型的规则建议**（按载重与航程计算），功能不中断 |
| 三级 | 未配置 apiKey | 返回业务码 `1101`，前端提示「AI 功能未配置，请联系管理员」 |

---

## 六、数据模型（16 张表）

| 业务域 | 表 |
|---|---|
| 用户与权限 | `user`（管理员/商家/客户，含资质字段） |
| 低空资源 | `aircraft` 飞行器台账、`station` 起降场站、`pilot` 作业人员、`pricing_rule` 定价规则 |
| **订单全流程** | `order_info` 订单主表、`dispatch_record` 调度记录、`order_track` 订单跟踪、`order_settlement` 订单结算 |
| 空域合规 | `airspace_zone` 空域规则、`flight_plan` 飞行计划、`violation_record` 违规记录 |
| 应急与系统 | `incident` 应急事件、`notification` 站内消息、`operation_log` 操作日志、`ai_config` AI 配置 |

建模要点：
- `order_info` 是核心主表，`dispatch_record` / `order_track` / `order_settlement` 通过 `order_id` 关联；
- 订单与结算**一对一**（`order_settlement.order_id` 唯一约束）；
- 一条订单可多次调度（飞行器故障、空域管制导致重排），故调度记录为一对多；
- `notification.is_read` 而非 `read` —— `read` 是 MySQL 保留字。

---

## 七、验收

### 技能验收脚本（凭据 / gitignore / mock / 构建 / 越权）

```powershell
powershell -ExecutionPolicy Bypass -File <skill>/scripts/verify.ps1 `
  -ProjectDir . -Build -BaseUrl http://127.0.0.1:8080
```

### 业务闭环验收（45 项）

```powershell
node e2e.mjs http://127.0.0.1:8080
```

覆盖：三端登录 → 订单创建（地址/空域校验）→ 承接（状态机 + 并发保护）→
调度（载重与资源校验）→ 跟踪（轨迹 + 异常预警）→ 送达结算（一对一约束）→
越权拦截（403/401）→ 资源管理 → 空域合规（计划申报审批 + 违规处置）→
应急闭环（禁止跳步）→ AI 三级降级 → 管理端看板与操作日志。

---

## 八、切换到 MySQL（可选）

1. `pom.xml`：放开 `mysql-connector-j`，移除 `h2`；
2. `application.yml`：替换 `datasource` 的 url / username / password / driver-class-name 四行；
3. 用 `backend/sql/init.sql` + `demo-data.sql` 建库灌数据。

表结构与字段完全一致，**无需改动任何 Java 代码**。

---

## 九、常见问题

**Q：登录接口返回 9999「系统繁忙」？**
先确认请求体是合法 JSON。用 PowerShell 内联写 JSON 时引号常被吞掉，建议写成 UTF-8 无 BOM 文件后用 `curl.exe --data-binary "@file"` 发送。

**Q：提交时间字段报格式错误？**
时间输出统一 `yyyy-MM-dd HH:mm:ss`，输入兼容带秒与不带秒（`LenientLocalDateTimeDeserializer`）。

**Q：数据库想重置？**
删掉 `backend/data/lowaltitude.mv.db` 后重启后端，会自动重建并灌入演示数据。

**Q：`npm run build` 报 `spawn EPERM`？**
这是受限沙箱禁止子进程管道导致的，在普通终端里执行不受影响。
