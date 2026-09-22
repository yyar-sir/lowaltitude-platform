-- ============================================================
-- 天枢 · 低空智能运营管控平台 —— H2 建表脚本（MODE=MySQL）
-- 该文件由数据模型元数据生成，请勿手改；改结构请改元数据后重新生成。
-- 每次启动重建（DROP + CREATE），便于反复调试。
-- ============================================================

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录名',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码散列',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `nickname` VARCHAR(50) COMMENT '昵称',
  `avatar` VARCHAR(10) COMMENT '头像 emoji',
  `role` TINYINT NOT NULL DEFAULT 2 COMMENT '0管理员 1物流运营商家 2终端客户',
  `phone` VARCHAR(20) UNIQUE COMMENT '联系电话',
  `company_name` VARCHAR(100) COMMENT '企业名称（商家）',
  `license_no` VARCHAR(64) COMMENT '运营许可证号（商家）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `notify_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1接收站内消息 0不接收',
  `password_changed_at` DATETIME COMMENT '密码最后修改时间（作废此前 token）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` DATETIME COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='用户表（终端客户/物流运营商家/平台管理员）';

DROP TABLE IF EXISTS `pricing_rule`;
CREATE TABLE `pricing_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` BIGINT NOT NULL COMMENT '所属商家 user.id',
  `name` VARCHAR(50) NOT NULL COMMENT '规则名称',
  `base_fee` DECIMAL(8,2) NOT NULL COMMENT '基础配送费（元）',
  `per_km_fee` DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '每公里费用（元）',
  `airspace_fee` DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '空域使用费（元）',
  `urgent_fee` DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '加急费（元）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) COMMENT='定价规则表（商家自定义收费标准）';

DROP TABLE IF EXISTS `aircraft`;
CREATE TABLE `aircraft` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` BIGINT NOT NULL COMMENT '所属商家 user.id',
  `aircraft_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '飞行器编号',
  `model` VARCHAR(50) NOT NULL COMMENT '型号',
  `max_payload` DECIMAL(6,2) NOT NULL COMMENT '最大载重（kg）',
  `endurance_minutes` INT NOT NULL DEFAULT 0 COMMENT '续航时间（分钟）',
  `battery` TINYINT DEFAULT 100 COMMENT '当前电量百分比',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1可用 2任务中 3维保中 4停用',
  `station_no` VARCHAR(32) COMMENT '停放场站编号',
  `maintain_at` DATETIME COMMENT '下次维保时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (`id`)
) COMMENT='飞行器台账表';

DROP TABLE IF EXISTS `station`;
CREATE TABLE `station` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` BIGINT NOT NULL COMMENT '所属商家 user.id',
  `station_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '场站编号',
  `name` VARCHAR(50) NOT NULL COMMENT '场站名称',
  `address` VARCHAR(128) NOT NULL COMMENT '场站地址',
  `longitude` DECIMAL(10,6) COMMENT '经度',
  `latitude` DECIMAL(10,6) COMMENT '纬度',
  `capacity` INT NOT NULL DEFAULT 1 COMMENT '可容纳飞行器数量',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) COMMENT='起降场站表';

DROP TABLE IF EXISTS `pilot`;
CREATE TABLE `pilot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` BIGINT NOT NULL COMMENT '所属商家 user.id',
  `name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `phone` VARCHAR(20) COMMENT '联系电话',
  `license_no` VARCHAR(64) NOT NULL COMMENT '驾驶证编号',
  `license_expire` DATE COMMENT '驾驶证到期日',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1在岗 2任务中 0休息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) COMMENT='作业人员（机长）表';

DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号',
  `customer_id` BIGINT NOT NULL COMMENT '客户 user.id（外键）',
  `merchant_id` BIGINT COMMENT '商家 user.id（外键，接单后写入）',
  `send_address` VARCHAR(128) NOT NULL COMMENT '寄件地址',
  `receive_address` VARCHAR(128) NOT NULL COMMENT '收件地址',
  `goods_type` VARCHAR(32) NOT NULL COMMENT '物品类型',
  `goods_weight` DECIMAL(6,2) NOT NULL COMMENT '物品重量（kg）',
  `delivery_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1普通配送 2加急配送',
  `priority` TINYINT NOT NULL DEFAULT 1 COMMENT '优先级 1普通 2较高 3紧急',
  `distance_km` DECIMAL(6,2) COMMENT '预估航程（km）',
  `estimate_fee` DECIMAL(8,2) NOT NULL COMMENT '预估费用（元）',
  `actual_fee` DECIMAL(8,2) COMMENT '实际费用（元）',
  `order_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1待接单 2待调度 3飞行中 4已送达 5已完成 6已取消',
  `reject_reason` VARCHAR(255) COMMENT '拒单原因',
  `remark` VARCHAR(255) COMMENT '订单备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `accept_time` DATETIME COMMENT '接单时间',
  `finish_time` DATETIME COMMENT '送达时间',
  PRIMARY KEY (`id`)
) COMMENT='订单信息表（订单全流程核心主表）';

DROP TABLE IF EXISTS `dispatch_record`;
CREATE TABLE `dispatch_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT NOT NULL COMMENT '订单 id（外键）',
  `aircraft_id` BIGINT COMMENT '飞行器 id',
  `aircraft_no` VARCHAR(32) NOT NULL COMMENT '飞行器编号',
  `station_id` BIGINT COMMENT '起降场站 id',
  `pilot_id` BIGINT COMMENT '执行机长 id',
  `pilot_name` VARCHAR(50) COMMENT '执行机长姓名',
  `flight_path` VARCHAR(512) NOT NULL COMMENT '规划飞行路径',
  `plan_takeoff_time` DATETIME COMMENT '计划起飞时间',
  `plan_arrive_time` DATETIME COMMENT '计划送达时间',
  `dispatch_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1待执行 2执行中 3已完成 4已取消',
  `dispatch_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调度时间',
  PRIMARY KEY (`id`)
) COMMENT='调度记录表（一条订单可多次调度）';

DROP TABLE IF EXISTS `order_track`;
CREATE TABLE `order_track` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT NOT NULL COMMENT '订单 id（外键）',
  `aircraft_no` VARCHAR(32) COMMENT '飞行器编号',
  `longitude` DECIMAL(10,6) NOT NULL COMMENT '当前位置经度',
  `latitude` DECIMAL(10,6) NOT NULL COMMENT '当前位置纬度',
  `battery` TINYINT COMMENT '剩余电量百分比',
  `flight_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1起飞 2巡航 3悬停 4降落 5已结束',
  `abnormal_type` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1设备故障 2空域管制',
  `abnormal_desc` VARCHAR(255) COMMENT '异常描述',
  `collect_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
  PRIMARY KEY (`id`)
) COMMENT='订单跟踪表（飞行位置与状态）';

DROP TABLE IF EXISTS `order_settlement`;
CREATE TABLE `order_settlement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT NOT NULL UNIQUE COMMENT '订单 id（外键，唯一）',
  `settle_amount` DECIMAL(8,2) NOT NULL COMMENT '结算金额（元）',
  `discount_amount` DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '优惠金额（元）',
  `pay_type` TINYINT COMMENT '1微信 2支付宝 3对公转账',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0未支付 1已支付 2支付失败',
  `pay_no` VARCHAR(64) COMMENT '支付流水号',
  `score` TINYINT COMMENT '客户评分 1-5',
  `comment` VARCHAR(255) COMMENT '评价内容',
  `settle_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结算时间',
  PRIMARY KEY (`id`)
) COMMENT='订单结算表（一对一订单）';

DROP TABLE IF EXISTS `airspace_zone`;
CREATE TABLE `airspace_zone` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(50) NOT NULL COMMENT '空域名称',
  `zone_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1可用空域 2禁飞区 3临时管制区',
  `boundary` VARCHAR(500) COMMENT '空域边界（坐标点串）',
  `center_lng` DECIMAL(10,6) COMMENT '中心经度',
  `center_lat` DECIMAL(10,6) COMMENT '中心纬度',
  `max_altitude` INT NOT NULL DEFAULT 120 COMMENT '最大允许高度（米）',
  `effective_from` DATETIME COMMENT '生效时间',
  `effective_to` DATETIME COMMENT '失效时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1生效中 0已失效',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) COMMENT='空域信息表（禁飞区/临时管制区）';

DROP TABLE IF EXISTS `flight_plan`;
CREATE TABLE `flight_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT COMMENT '关联订单 id',
  `merchant_id` BIGINT NOT NULL COMMENT '申报商家 user.id',
  `plan_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '计划编号',
  `route` VARCHAR(512) NOT NULL COMMENT '计划航线',
  `altitude` INT NOT NULL DEFAULT 100 COMMENT '计划飞行高度（米）',
  `plan_start` DATETIME COMMENT '计划开始时间',
  `plan_end` DATETIME COMMENT '计划结束时间',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待审批 1已通过 2已驳回 3已执行',
  `reject_reason` VARCHAR(255) COMMENT '驳回原因',
  `reviewer_id` BIGINT COMMENT '审批人 user.id',
  `review_time` DATETIME COMMENT '审批时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申报时间',
  PRIMARY KEY (`id`)
) COMMENT='飞行计划表（申报与审批）';

DROP TABLE IF EXISTS `violation_record`;
CREATE TABLE `violation_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT COMMENT '关联订单 id',
  `merchant_id` BIGINT COMMENT '违规商家 user.id',
  `aircraft_id` BIGINT COMMENT '涉及飞行器 id',
  `violation_type` VARCHAR(32) NOT NULL COMMENT '违规类型',
  `detail` VARCHAR(255) COMMENT '违规说明',
  `level` TINYINT NOT NULL DEFAULT 1 COMMENT '1轻微 2一般 3严重',
  `handle_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理',
  `handle_result` VARCHAR(255) COMMENT '处理结果',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`)
) COMMENT='违规记录表';

DROP TABLE IF EXISTS `incident`;
CREATE TABLE `incident` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT COMMENT '关联订单 id',
  `reporter_id` BIGINT COMMENT '上报人 user.id',
  `title` VARCHAR(100) NOT NULL COMMENT '事件标题',
  `content` VARCHAR(500) COMMENT '事件描述',
  `incident_type` TINYINT NOT NULL DEFAULT 4 COMMENT '1设备故障 2空域冲突 3恶劣天气 4其他',
  `level` TINYINT NOT NULL DEFAULT 1 COMMENT '1一般 2较大 3重大',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2已解决',
  `handle_result` VARCHAR(500) COMMENT '处置结果',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
  `resolved_at` DATETIME COMMENT '解决时间',
  PRIMARY KEY (`id`)
) COMMENT='应急事件表';

DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '接收人 user.id',
  `type` VARCHAR(20) DEFAULT 'info' COMMENT 'order订单/plan审批/violation违规/announce公告/info',
  `title` VARCHAR(100) COMMENT '标题',
  `content` VARCHAR(255) COMMENT '内容',
  `link` VARCHAR(120) COMMENT '点击跳转的前端路由',
  `is_read` TINYINT DEFAULT 0 COMMENT '0未读 1已读（列名不能用 read，是保留字）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) COMMENT='站内消息表';

DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator_id` BIGINT COMMENT '操作人 user.id',
  `operator_role` TINYINT COMMENT '操作人角色',
  `op_type` VARCHAR(30) NOT NULL COMMENT '操作类型',
  `detail` VARCHAR(300) COMMENT '详情',
  `ip` VARCHAR(50) COMMENT 'IP 地址',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`)
) COMMENT='操作日志表';

DROP TABLE IF EXISTS `ai_config`;
CREATE TABLE `ai_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provider` VARCHAR(30) NOT NULL DEFAULT 'deepseek' COMMENT '服务商',
  `api_key` VARCHAR(200) NOT NULL DEFAULT '' COMMENT '密钥（仅后端持有）',
  `base_url` VARCHAR(200) DEFAULT 'https://api.deepseek.com/v1' COMMENT 'Base URL',
  `model` VARCHAR(50) DEFAULT 'deepseek-chat' COMMENT '模型名',
  `timeout_sec` INT NOT NULL DEFAULT 60 COMMENT '超时（秒）',
  `max_concurrency` INT NOT NULL DEFAULT 3 COMMENT '最大并发',
  `rate_limit_per_min` INT NOT NULL DEFAULT 20 COMMENT '每人每分钟限流',
  `content_filter` TINYINT NOT NULL DEFAULT 1 COMMENT '内容安全开关',
  `updated_at` DATETIME COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='AI 接口配置表（单行）';

