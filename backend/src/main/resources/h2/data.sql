-- ============================================================
-- 天枢 · 低空智能运营管控平台 —— 演示数据（H2 版）
-- 启动时由 spring.sql.init 自动执行，依赖 schema.sql 先执行
-- 演示账号密码统一为 123456（BCrypt 散列）
-- ============================================================

-- ---------- 用户：1 管理员 + 2 商家 + 2 客户 ----------
INSERT INTO `user` (username, password, real_name, nickname, avatar, role, phone, company_name, license_no, status) VALUES
('admin',     '$2b$10$aXKY2uk.rmhxeNTvlygNzerTk7RbQPv7h.SQBauehgoVmtTxW3B22', '平台管理员', '管理员', '🛠️', 0, '13800000000', NULL, NULL, 1),
('merchant01','$2b$10$aXKY2uk.rmhxeNTvlygNzerTk7RbQPv7h.SQBauehgoVmtTxW3B22', '顺空物流', '顺空物流', '🚁', 1, '13800000001', '顺空低空物流有限公司', 'LAC-2026-0001', 1),
('merchant02','$2b$10$aXKY2uk.rmhxeNTvlygNzerTk7RbQPv7h.SQBauehgoVmtTxW3B22', '云翼科技', '云翼科技', '🛰️', 1, '13800000002', '云翼低空科技有限公司', 'LAC-2026-0002', 1),
('customer01','$2b$10$aXKY2uk.rmhxeNTvlygNzerTk7RbQPv7h.SQBauehgoVmtTxW3B22', '明创科技', '明创科技', '🏢', 2, '13900000001', '明创科技有限公司', NULL, 1),
('customer02','$2b$10$aXKY2uk.rmhxeNTvlygNzerTk7RbQPv7h.SQBauehgoVmtTxW3B22', '张伟', '张伟', '👨', 2, '13900000002', NULL, NULL, 1);

-- ---------- 定价规则（商家自定义收费标准） ----------
INSERT INTO `pricing_rule` (merchant_id, name, base_fee, per_km_fee, airspace_fee, urgent_fee, status) VALUES
(2, '城区普通配送标准', 20.00, 3.00, 5.00, 8.00, 1),
(2, '跨区配送标准',     35.00, 4.50, 8.00, 12.00, 1),
(3, '轻小件优惠标准',   15.00, 2.50, 4.00, 6.00, 1);

-- ---------- 飞行器台账 ----------
INSERT INTO `aircraft` (merchant_id, aircraft_no, model, max_payload, endurance_minutes, battery, status, station_no, maintain_at) VALUES
(2, 'TS-FC30-001', '天枢 FC30', 5.00, 45, 92, 1, 'ST-001', '2026-12-01 09:00:00'),
(2, 'TS-FC30-002', '天枢 FC30', 5.00, 45, 78, 3, 'ST-001', '2026-11-15 09:00:00'),
(2, 'TS-FC60-003', '天枢 FC60', 15.00, 70, 88, 1, 'ST-002', '2026-12-10 09:00:00'),
(3, 'YY-M200-001', '云翼 M200', 8.00, 55, 95, 1, 'ST-003', '2026-12-05 09:00:00'),
(3, 'YY-M200-002', '云翼 M200', 8.00, 55, 64, 1, 'ST-003', '2026-11-28 09:00:00');

-- ---------- 起降场站 ----------
INSERT INTO `station` (merchant_id, station_no, name, address, longitude, latitude, capacity, status) VALUES
(2, 'ST-001', '高新区一号起降场', '高新区科技园A座楼顶', 113.952000, 22.541000, 3, 1),
(2, 'ST-002', '经开区中心场站',   '经开区创新大道8号',   113.901000, 22.562000, 5, 1),
(3, 'ST-003', '南山低空枢纽站',   '南山区智慧园3号楼顶', 113.930000, 22.528000, 4, 1);

-- ---------- 作业人员（机长） ----------
INSERT INTO `pilot` (merchant_id, name, phone, license_no, license_expire, status) VALUES
(2, '李机长', '13700000001', 'PL-2024-1001', '2027-06-30', 1),
(2, '王机长', '13700000002', 'PL-2024-1002', '2027-03-31', 1),
(3, '赵机长', '13700000003', 'PL-2025-2001', '2028-01-31', 1);

-- ---------- 空域信息 ----------
INSERT INTO `airspace_zone` (name, zone_type, boundary, center_lng, center_lat, max_altitude, effective_from, effective_to, status) VALUES
('高新区可用空域',   1, '113.94,22.53;113.97,22.53;113.97,22.56;113.94,22.56', 113.955000, 22.545000, 120, NULL, NULL, 1),
('南山核心禁飞区',   2, '113.92,22.52;113.94,22.52;113.94,22.54;113.92,22.54', 113.930000, 22.530000, 0,   NULL, NULL, 1),
('宝安机场临时管制区', 3, '113.80,22.60;113.85,22.60;113.85,22.65;113.80,22.65', 113.825000, 22.625000, 0,   '2026-09-20 00:00:00', '2026-10-20 23:59:59', 1);

-- ---------- 示例订单（覆盖各状态，便于演示订单全流程） ----------
INSERT INTO `order_info` (order_no, customer_id, merchant_id, send_address, receive_address, goods_type, goods_weight, delivery_type, priority, distance_km, estimate_fee, actual_fee, order_status, remark, create_time, accept_time, finish_time) VALUES
('TS20260922001', 4, 2, '高新区科技园A座', '经开区创新大厦B座', '文件类', 2.50, 2, 3, 8.60, 49.30, NULL, 1, '加急件，尽快送达', '2026-09-22 09:02:00', NULL, NULL),
('TS20260922002', 4, 2, '高新区软件园2栋',  '南山区智慧园3号楼', '生鲜冷链', 6.20, 1, 2, 12.40, 72.20, NULL, 2, NULL, '2026-09-22 09:10:00', '2026-09-22 09:15:00', NULL),
('TS20260922003', 5, 2, '福田区金融中心',   '罗湖区万象城',     '小型配件', 1.20, 2, 1, 6.80, 40.40, 40.40, 5, NULL, '2026-09-21 14:20:00', '2026-09-21 14:25:00', '2026-09-21 15:02:00'),
('TS20260922004', 5, 3, '南山区科技园',     '宝安区中心区',     '医疗物资', 3.80, 1, 3, 15.20, 87.80, NULL, 6, '客户临时取消', '2026-09-21 16:40:00', NULL, NULL);

-- ---------- 调度记录（订单 3 已完成、订单 2 执行中） ----------
INSERT INTO `dispatch_record` (order_id, aircraft_id, aircraft_no, station_id, pilot_id, pilot_name, flight_path, plan_takeoff_time, plan_arrive_time, dispatch_status, dispatch_time) VALUES
(2, 1, 'TS-FC30-001', 1, 1, '李机长', 'ST-001→高新区软件园2栋→南山区智慧园3号楼', '2026-09-22 09:20:00', '2026-09-22 09:45:00', 2, '2026-09-22 09:16:00'),
(3, 1, 'TS-FC30-001', 1, 1, '李机长', 'ST-001→福田区金融中心→罗湖区万象城',       '2026-09-21 14:30:00', '2026-09-21 15:00:00', 3, '2026-09-21 14:26:00');

-- ---------- 订单跟踪（订单 2 飞行中的轨迹点） ----------
INSERT INTO `order_track` (order_id, aircraft_no, longitude, latitude, battery, flight_status, abnormal_type, abnormal_desc, collect_time) VALUES
(2, 'TS-FC30-001', 113.952000, 22.541000, 95, 1, 0, NULL, '2026-09-22 09:20:30'),
(2, 'TS-FC30-001', 113.945000, 22.538000, 88, 2, 0, NULL, '2026-09-22 09:25:00'),
(2, 'TS-FC30-001', 113.938000, 22.534000, 79, 2, 2, '接近临时管制区，已重新规划航线', '2026-09-22 09:30:00'),
(2, 'TS-FC30-001', 113.933000, 22.530000, 68, 2, 0, NULL, '2026-09-22 09:35:00');

-- ---------- 结算记录（订单 3 已支付并评价） ----------
INSERT INTO `order_settlement` (order_id, settle_amount, discount_amount, pay_type, pay_status, pay_no, score, comment, settle_time) VALUES
(3, 40.40, 0.00, 1, 1, 'PAY20260921150200', 5, '送得很快，包装完好', '2026-09-21 15:05:00');

-- ---------- 飞行计划申报 ----------
INSERT INTO `flight_plan` (order_id, merchant_id, plan_no, route, altitude, plan_start, plan_end, status, reviewer_id, review_time) VALUES
(2, 2, 'FP20260922001', 'ST-001→高新区软件园2栋→南山区智慧园3号楼', 100, '2026-09-22 09:20:00', '2026-09-22 09:45:00', 1, 1, '2026-09-22 09:18:00'),
(3, 2, 'FP20260921001', 'ST-001→福田区金融中心→罗湖区万象城',       100, '2026-09-21 14:30:00', '2026-09-21 15:00:00', 3, 1, '2026-09-21 14:28:00'),
(NULL, 3, 'FP20260922002', 'ST-003→南山区科技园→宝安区中心区',      120, '2026-09-23 10:00:00', '2026-09-23 10:40:00', 0, NULL, NULL);

-- ---------- 违规记录 ----------
INSERT INTO `violation_record` (order_id, merchant_id, aircraft_id, violation_type, detail, level, handle_status, handle_result) VALUES
(2, 2, 1, '偏离计划航线', '因临时管制区调整航线，未及时重新申报', 1, 1, '已提醒商家补报飞行计划'),
(4, 3, 4, '超范围飞行',   '目的地超出商家备案服务空域',           2, 0, NULL);

-- ---------- 应急事件 ----------
INSERT INTO `incident` (order_id, reporter_id, title, content, incident_type, level, status, handle_result, resolved_at) VALUES
(2, 2, '航段遇临时空域管制', '飞行中收到空域管制通知，已按预案绕行', 2, 2, 2, '已绕行并安全送达，无损失', '2026-09-22 09:40:00'),
(4, 4, '订单取消后设备未起飞', '客户取消订单，飞行器未起飞，无需处置', 4, 1, 2, '已确认设备未起飞', '2026-09-21 17:00:00');

-- ---------- 站内消息 ----------
INSERT INTO `notification` (user_id, type, title, content, link, is_read) VALUES
(2, 'order', '新订单待接单', '订单 TS20260922001 等待接单', '/merchant/orders', 0),
(4, 'order', '订单已接单',   '订单 TS20260922002 已被顺空物流接单', '/customer/orders', 0),
(4, 'order', '订单已完成',   '订单 TS20260922003 已送达，请查看结算', '/customer/settlement', 1),
(2, 'plan',  '飞行计划已通过', '计划 FP20260922001 已通过审批', '/merchant/plans', 0);

-- ---------- 操作日志 ----------
INSERT INTO `operation_log` (operator_id, operator_role, op_type, detail, ip) VALUES
(1, 0, '审批飞行计划', '通过计划 FP20260922001', '127.0.0.1'),
(1, 0, '空域规则维护', '新增临时管制区：宝安机场临时管制区', '127.0.0.1');

-- ---------- AI 接口配置（单行；apiKey 留空 → AI 接口返回 1101，其余功能不受影响） ----------
INSERT INTO `ai_config` (id, provider, api_key, base_url, model, timeout_sec, max_concurrency, rate_limit_per_min, content_filter) VALUES
(1, 'deepseek', '', 'https://api.deepseek.com/v1', 'deepseek-chat', 60, 3, 20, 1);
