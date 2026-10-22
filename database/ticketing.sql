/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80040 (8.0.40)
 Source Host           : localhost:3306
 Source Schema         : ticketing

 Target Server Type    : MySQL
 Target Server Version : 80040 (8.0.40)
 File Encoding         : 65001

 Date: 22/10/2026 09:38:44
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for attachments
-- ----------------------------
DROP TABLE IF EXISTS `attachments`;
CREATE TABLE `attachments`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `upload_token` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `uploader_id` bigint UNSIGNED NOT NULL,
  `ticket_id` bigint UNSIGNED NULL DEFAULT NULL,
  `ticket_event_id` bigint UNSIGNED NULL DEFAULT NULL,
  `bucket_name` varchar(63) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `object_key` varchar(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `original_filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `media_type` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `size_bytes` bigint UNSIGNED NOT NULL,
  `sha256` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `image_width` int UNSIGNED NOT NULL,
  `image_height` int UNSIGNED NOT NULL,
  `storage_state` varchar(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'UPLOADING',
  `expires_at` datetime(6) NULL DEFAULT NULL COMMENT '仅未绑定临时对象使用',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `bound_at` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_attachment_token`(`upload_token` ASC) USING BTREE,
  UNIQUE INDEX `uk_attachment_object`(`bucket_name` ASC, `object_key` ASC) USING BTREE,
  INDEX `ix_attachment_ticket_created`(`ticket_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `ix_attachment_expiration`(`storage_state` ASC, `expires_at` ASC) USING BTREE,
  INDEX `ix_attachment_uploader`(`uploader_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `fk_attachment_event`(`ticket_event_id` ASC, `ticket_id` ASC) USING BTREE,
  CONSTRAINT `fk_attachment_event` FOREIGN KEY (`ticket_event_id`, `ticket_id`) REFERENCES `ticket_events` (`id`, `ticket_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_attachment_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_attachment_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_attachment_binding` CHECK (((`storage_state` = _utf8mb4'BOUND') and (`ticket_id` is not null) and (`ticket_event_id` is not null) and (`bound_at` is not null) and (`expires_at` is null)) or ((`storage_state` <> _utf8mb4'BOUND') and (`ticket_id` is null) and (`ticket_event_id` is null) and (`bound_at` is null) and (`expires_at` is not null))),
  CONSTRAINT `ck_attachment_dimensions` CHECK ((`image_width` > 0) and (`image_height` > 0)),
  CONSTRAINT `ck_attachment_mime` CHECK (`media_type` in (_utf8mb4'image/png',_utf8mb4'image/jpeg',_utf8mb4'image/webp')),
  CONSTRAINT `ck_attachment_size` CHECK (`size_bytes` between 1 and 5242880),
  CONSTRAINT `ck_attachment_state` CHECK (`storage_state` in (_utf8mb4'UPLOADING',_utf8mb4'READY',_utf8mb4'BOUND',_utf8mb4'DELETE_PENDING'))
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of attachments
-- ----------------------------
INSERT INTO `attachments` VALUES (8, '73655f51-ee02-448b-956f-7cdb4f0b93d0', 1, 3, 17, 'ticketing-images', 'uploads/73655f51-ee02-448b-956f-7cdb4f0b93d0.png', '测试文件.jpg', 'image/png', 31808, '3d8a79e50831bbb4bc244052022d9756165e005ba9d3f9dc1d64b97834e58ad3', 2280, 1163, 'BOUND', NULL, '2026-10-22 01:05:26.463021', '2026-10-22 01:05:37.932521');

-- ----------------------------
-- Table structure for notifications
-- ----------------------------
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `recipient_id` bigint UNSIGNED NOT NULL,
  `ticket_id` bigint UNSIGNED NOT NULL,
  `event_id` bigint UNSIGNED NOT NULL,
  `type` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `read_at` datetime(6) NULL DEFAULT NULL,
  `invalidated_at` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_notification_delivery`(`recipient_id` ASC, `event_id` ASC, `type` ASC) USING BTREE,
  INDEX `ix_notification_unread`(`recipient_id` ASC, `invalidated_at` ASC, `read_at` ASC, `id` ASC) USING BTREE,
  INDEX `ix_notification_list`(`recipient_id` ASC, `invalidated_at` ASC, `id` ASC) USING BTREE,
  INDEX `ix_notification_ticket_recipient`(`ticket_id` ASC, `recipient_id` ASC) USING BTREE,
  INDEX `fk_notification_event`(`event_id` ASC, `ticket_id` ASC) USING BTREE,
  CONSTRAINT `fk_notification_event` FOREIGN KEY (`event_id`, `ticket_id`) REFERENCES `ticket_events` (`id`, `ticket_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_notification_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_notification_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_notification_type` CHECK (`type` in (_utf8mb4'TICKET_CREATED',_utf8mb4'TICKET_ASSIGNED',_utf8mb4'TICKET_REASSIGNED',_utf8mb4'NEW_MESSAGE',_utf8mb4'RESULT_SUBMITTED',_utf8mb4'REWORK_REQUESTED',_utf8mb4'TICKET_COMPLETED'))
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notifications
-- ----------------------------
INSERT INTO `notifications` VALUES (1, 3, 1, 1, 'TICKET_CREATED', '2026-10-21 13:19:48.375074', '2026-10-21 13:20:10.752589', NULL);
INSERT INTO `notifications` VALUES (2, 4, 1, 2, 'TICKET_ASSIGNED', '2026-10-21 13:20:33.648485', NULL, NULL);
INSERT INTO `notifications` VALUES (3, 1, 1, 2, 'TICKET_ASSIGNED', '2026-10-21 13:20:33.649279', '2026-10-21 13:23:28.896233', NULL);
INSERT INTO `notifications` VALUES (4, 1, 1, 4, 'RESULT_SUBMITTED', '2026-10-21 13:21:04.744852', '2026-10-21 13:23:30.629168', NULL);
INSERT INTO `notifications` VALUES (5, 3, 1, 5, 'REWORK_REQUESTED', '2026-10-21 13:21:32.775889', NULL, NULL);
INSERT INTO `notifications` VALUES (6, 4, 1, 5, 'REWORK_REQUESTED', '2026-10-21 13:21:32.776448', '2026-10-21 13:42:14.778759', NULL);
INSERT INTO `notifications` VALUES (7, 3, 2, 6, 'TICKET_CREATED', '2026-10-21 13:40:29.718616', '2026-10-21 13:42:28.369777', NULL);
INSERT INTO `notifications` VALUES (8, 5, 2, 7, 'TICKET_ASSIGNED', '2026-10-21 13:41:06.619770', NULL, '2026-10-21 13:45:04.146172');
INSERT INTO `notifications` VALUES (9, 1, 2, 7, 'TICKET_ASSIGNED', '2026-10-21 13:41:06.621115', NULL, NULL);
INSERT INTO `notifications` VALUES (10, 1, 2, 8, 'NEW_MESSAGE', '2026-10-21 13:41:20.872560', NULL, NULL);
INSERT INTO `notifications` VALUES (11, 5, 2, 8, 'NEW_MESSAGE', '2026-10-21 13:41:20.873847', NULL, '2026-10-21 13:45:04.146172');
INSERT INTO `notifications` VALUES (12, 1, 1, 10, 'RESULT_SUBMITTED', '2026-10-21 13:42:20.037229', NULL, NULL);
INSERT INTO `notifications` VALUES (13, 3, 1, 11, 'TICKET_COMPLETED', '2026-10-21 13:42:43.511528', NULL, NULL);
INSERT INTO `notifications` VALUES (14, 4, 1, 11, 'TICKET_COMPLETED', '2026-10-21 13:42:43.512150', NULL, NULL);
INSERT INTO `notifications` VALUES (15, 1, 2, 12, 'NEW_MESSAGE', '2026-10-21 13:44:28.963392', NULL, NULL);
INSERT INTO `notifications` VALUES (16, 3, 2, 12, 'NEW_MESSAGE', '2026-10-21 13:44:28.963888', '2026-10-21 13:44:46.821889', NULL);
INSERT INTO `notifications` VALUES (17, 1, 2, 14, 'RESULT_SUBMITTED', '2026-10-21 13:44:38.208146', NULL, NULL);
INSERT INTO `notifications` VALUES (18, 4, 2, 15, 'TICKET_REASSIGNED', '2026-10-21 13:45:04.149499', NULL, NULL);
INSERT INTO `notifications` VALUES (19, 1, 2, 15, 'TICKET_REASSIGNED', '2026-10-21 13:45:04.150672', '2026-10-22 01:00:02.503947', NULL);
INSERT INTO `notifications` VALUES (20, 3, 2, 16, 'TICKET_COMPLETED', '2026-10-21 13:46:03.462311', NULL, NULL);
INSERT INTO `notifications` VALUES (21, 4, 2, 16, 'TICKET_COMPLETED', '2026-10-21 13:46:03.462787', NULL, NULL);
INSERT INTO `notifications` VALUES (22, 3, 3, 17, 'TICKET_CREATED', '2026-10-22 01:05:37.937526', NULL, NULL);
INSERT INTO `notifications` VALUES (23, 1, 3, 18, 'NEW_MESSAGE', '2026-10-22 01:06:42.309124', NULL, NULL);
INSERT INTO `notifications` VALUES (24, 5, 3, 19, 'TICKET_ASSIGNED', '2026-10-22 01:06:47.137322', NULL, '2026-10-22 01:07:56.177874');
INSERT INTO `notifications` VALUES (25, 1, 3, 19, 'TICKET_ASSIGNED', '2026-10-22 01:06:47.137889', NULL, NULL);
INSERT INTO `notifications` VALUES (26, 4, 3, 20, 'TICKET_REASSIGNED', '2026-10-22 01:07:56.179920', '2026-10-22 01:08:01.134874', NULL);
INSERT INTO `notifications` VALUES (27, 1, 3, 20, 'TICKET_REASSIGNED', '2026-10-22 01:07:56.181612', NULL, NULL);
INSERT INTO `notifications` VALUES (28, 3, 4, 22, 'TICKET_CREATED', '2026-10-22 01:09:04.622893', NULL, NULL);
INSERT INTO `notifications` VALUES (29, 1, 3, 23, 'RESULT_SUBMITTED', '2026-10-22 01:12:21.206606', NULL, NULL);
INSERT INTO `notifications` VALUES (30, 3, 6, 24, 'TICKET_CREATED', '2026-10-22 01:22:12.614894', NULL, NULL);
INSERT INTO `notifications` VALUES (31, 1, 6, 25, 'NEW_MESSAGE', '2026-10-22 01:22:34.114778', NULL, NULL);
INSERT INTO `notifications` VALUES (48, 1, 6, 36, 'TICKET_COMPLETED', '2026-10-22 01:31:20.036024', NULL, NULL);

-- ----------------------------
-- Table structure for permissions
-- ----------------------------
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_permissions_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of permissions
-- ----------------------------
INSERT INTO `permissions` VALUES (1, 'ticket:create', '创建工单');
INSERT INTO `permissions` VALUES (2, 'ticket:read:own', '读取本人工单');
INSERT INTO `permissions` VALUES (3, 'ticket:read:all', '读取全部工单');
INSERT INTO `permissions` VALUES (4, 'ticket:read:assigned', '读取负责工单');
INSERT INTO `permissions` VALUES (5, 'ticket:comment', '公开沟通');
INSERT INTO `permissions` VALUES (6, 'ticket:category:update', '纠正分类');
INSERT INTO `permissions` VALUES (7, 'ticket:assign', '分派工单');
INSERT INTO `permissions` VALUES (8, 'ticket:reassign', '转派工单');
INSERT INTO `permissions` VALUES (9, 'ticket:start', '开始处理');
INSERT INTO `permissions` VALUES (10, 'ticket:result:submit', '提交结果');
INSERT INTO `permissions` VALUES (11, 'ticket:confirm', '确认完成');
INSERT INTO `permissions` VALUES (12, 'ticket:rework', '要求返工');
INSERT INTO `permissions` VALUES (13, 'ticket:history:read', '读取历史');
INSERT INTO `permissions` VALUES (14, 'ticket:related:create', '关联旧工单');
INSERT INTO `permissions` VALUES (15, 'attachment:upload', '上传图片');
INSERT INTO `permissions` VALUES (16, 'attachment:bind', '绑定图片');
INSERT INTO `permissions` VALUES (17, 'attachment:read', '读取图片');
INSERT INTO `permissions` VALUES (18, 'notification:read', '读取本人通知');
INSERT INTO `permissions` VALUES (19, 'notification:mark-read', '本人通知已读');
INSERT INTO `permissions` VALUES (20, 'ticket:access:approve', '审批权限申请');

-- ----------------------------
-- Table structure for role_permissions
-- ----------------------------
DROP TABLE IF EXISTS `role_permissions`;
CREATE TABLE `role_permissions`  (
  `role_id` bigint UNSIGNED NOT NULL,
  `permission_id` bigint UNSIGNED NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`) USING BTREE,
  INDEX `fk_rp_permission`(`permission_id` ASC) USING BTREE,
  CONSTRAINT `fk_rp_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role_permissions
-- ----------------------------
INSERT INTO `role_permissions` VALUES (1, 1);
INSERT INTO `role_permissions` VALUES (1, 2);
INSERT INTO `role_permissions` VALUES (2, 3);
INSERT INTO `role_permissions` VALUES (3, 4);
INSERT INTO `role_permissions` VALUES (1, 5);
INSERT INTO `role_permissions` VALUES (2, 5);
INSERT INTO `role_permissions` VALUES (3, 5);
INSERT INTO `role_permissions` VALUES (2, 6);
INSERT INTO `role_permissions` VALUES (2, 7);
INSERT INTO `role_permissions` VALUES (2, 8);
INSERT INTO `role_permissions` VALUES (2, 9);
INSERT INTO `role_permissions` VALUES (3, 9);
INSERT INTO `role_permissions` VALUES (2, 10);
INSERT INTO `role_permissions` VALUES (3, 10);
INSERT INTO `role_permissions` VALUES (1, 11);
INSERT INTO `role_permissions` VALUES (1, 12);
INSERT INTO `role_permissions` VALUES (1, 13);
INSERT INTO `role_permissions` VALUES (2, 13);
INSERT INTO `role_permissions` VALUES (3, 13);
INSERT INTO `role_permissions` VALUES (1, 14);
INSERT INTO `role_permissions` VALUES (1, 15);
INSERT INTO `role_permissions` VALUES (1, 16);
INSERT INTO `role_permissions` VALUES (1, 17);
INSERT INTO `role_permissions` VALUES (2, 17);
INSERT INTO `role_permissions` VALUES (3, 17);
INSERT INTO `role_permissions` VALUES (1, 18);
INSERT INTO `role_permissions` VALUES (2, 18);
INSERT INTO `role_permissions` VALUES (3, 18);
INSERT INTO `role_permissions` VALUES (1, 19);
INSERT INTO `role_permissions` VALUES (2, 19);
INSERT INTO `role_permissions` VALUES (3, 19);
INSERT INTO `role_permissions` VALUES (2, 20);

-- ----------------------------
-- Table structure for roles
-- ----------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_roles_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of roles
-- ----------------------------
INSERT INTO `roles` VALUES (1, 'REQUESTER', '提交者');
INSERT INTO `roles` VALUES (2, 'COORDINATOR', '工单协调员');
INSERT INTO `roles` VALUES (3, 'SUPPORT', '技术支持');

-- ----------------------------
-- Table structure for ticket_events
-- ----------------------------
DROP TABLE IF EXISTS `ticket_events`;
CREATE TABLE `ticket_events`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `ticket_id` bigint UNSIGNED NOT NULL,
  `actor_id` bigint UNSIGNED NOT NULL,
  `event_type` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '公开补充、回复、处理结果或变更原因',
  `changes` json NULL COMMENT '变更前后字段；不存密码、会话或MinIO凭据',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_event_id_ticket`(`id` ASC, `ticket_id` ASC) USING BTREE,
  INDEX `ix_events_ticket_created`(`ticket_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `fk_event_actor`(`actor_id` ASC) USING BTREE,
  CONSTRAINT `fk_event_actor` FOREIGN KEY (`actor_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_event_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_event_content_length` CHECK ((`content` is null) or (char_length(`content`) <= 10000)),
  CONSTRAINT `ck_event_type` CHECK (`event_type` in (_utf8mb4'CREATED',_utf8mb4'CATEGORY_CHANGED',_utf8mb4'ASSIGNED',_utf8mb4'REASSIGNED',_utf8mb4'STARTED',_utf8mb4'COMMENTED',_utf8mb4'SUPPLEMENTED',_utf8mb4'RESULT_SUBMITTED',_utf8mb4'REWORK_REQUESTED',_utf8mb4'COMPLETED'))
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_events
-- ----------------------------
INSERT INTO `ticket_events` VALUES (1, 1, 1, 'CREATED', NULL, '{\"category\": \"SYSTEM_FAILURE\"}', '2026-10-21 13:19:48.367614');
INSERT INTO `ticket_events` VALUES (2, 1, 3, 'ASSIGNED', 'f ', '{\"toAssignee\": \"4\", \"fromAssignee\": \"\"}', '2026-10-21 13:20:33.646554');
INSERT INTO `ticket_events` VALUES (3, 1, 4, 'STARTED', NULL, '{\"to\": \"IN_PROGRESS\", \"from\": \"PENDING\"}', '2026-10-21 13:20:59.911574');
INSERT INTO `ticket_events` VALUES (4, 1, 4, 'RESULT_SUBMITTED', 's ', '{\"to\": \"AWAITING_CONFIRMATION\", \"from\": \"IN_PROGRESS\", \"resolution\": \"RESOLVED\"}', '2026-10-21 13:21:04.743780');
INSERT INTO `ticket_events` VALUES (5, 1, 1, 'REWORK_REQUESTED', '但是', '{\"to\": \"REWORK\", \"from\": \"AWAITING_CONFIRMATION\"}', '2026-10-21 13:21:32.774509');
INSERT INTO `ticket_events` VALUES (6, 2, 1, 'CREATED', NULL, '{\"category\": \"SYSTEM_FAILURE\"}', '2026-10-21 13:40:29.711821');
INSERT INTO `ticket_events` VALUES (7, 2, 3, 'ASSIGNED', 'ada a', '{\"toAssignee\": \"5\", \"fromAssignee\": \"\"}', '2026-10-21 13:41:06.617707');
INSERT INTO `ticket_events` VALUES (8, 2, 3, 'COMMENTED', 'dss d', '{}', '2026-10-21 13:41:20.869336');
INSERT INTO `ticket_events` VALUES (9, 1, 4, 'STARTED', NULL, '{\"to\": \"IN_PROGRESS\", \"from\": \"REWORK\"}', '2026-10-21 13:42:16.481948');
INSERT INTO `ticket_events` VALUES (10, 1, 4, 'RESULT_SUBMITTED', 'dad ad a', '{\"to\": \"AWAITING_CONFIRMATION\", \"from\": \"IN_PROGRESS\", \"resolution\": \"RESOLVED\"}', '2026-10-21 13:42:20.036230');
INSERT INTO `ticket_events` VALUES (11, 1, 1, 'COMPLETED', NULL, '{\"to\": \"COMPLETED\", \"from\": \"AWAITING_CONFIRMATION\"}', '2026-10-21 13:42:43.509859');
INSERT INTO `ticket_events` VALUES (12, 2, 5, 'COMMENTED', 'fs ', '{}', '2026-10-21 13:44:28.962228');
INSERT INTO `ticket_events` VALUES (13, 2, 5, 'STARTED', NULL, '{\"to\": \"IN_PROGRESS\", \"from\": \"PENDING\"}', '2026-10-21 13:44:35.548811');
INSERT INTO `ticket_events` VALUES (14, 2, 5, 'RESULT_SUBMITTED', 'da d', '{\"to\": \"AWAITING_CONFIRMATION\", \"from\": \"IN_PROGRESS\", \"resolution\": \"RESOLVED\"}', '2026-10-21 13:44:38.207365');
INSERT INTO `ticket_events` VALUES (15, 2, 3, 'REASSIGNED', 'd', '{\"toAssignee\": \"4\", \"fromAssignee\": \"5\"}', '2026-10-21 13:45:04.144097');
INSERT INTO `ticket_events` VALUES (16, 2, 1, 'COMPLETED', NULL, '{\"to\": \"COMPLETED\", \"from\": \"AWAITING_CONFIRMATION\"}', '2026-10-21 13:46:03.460850');
INSERT INTO `ticket_events` VALUES (17, 3, 1, 'CREATED', NULL, '{\"category\": \"SYSTEM_FAILURE\"}', '2026-10-22 01:05:37.898985');
INSERT INTO `ticket_events` VALUES (18, 3, 3, 'COMMENTED', 'f gf g', '{}', '2026-10-22 01:06:42.306721');
INSERT INTO `ticket_events` VALUES (19, 3, 3, 'ASSIGNED', 'f gf g', '{\"toAssignee\": \"5\", \"fromAssignee\": \"\"}', '2026-10-22 01:06:47.136162');
INSERT INTO `ticket_events` VALUES (20, 3, 3, 'REASSIGNED', 'dd', '{\"toAssignee\": \"4\", \"fromAssignee\": \"5\"}', '2026-10-22 01:07:56.175272');
INSERT INTO `ticket_events` VALUES (21, 3, 4, 'STARTED', NULL, '{\"to\": \"IN_PROGRESS\", \"from\": \"PENDING\"}', '2026-10-22 01:08:05.883881');
INSERT INTO `ticket_events` VALUES (22, 4, 1, 'CREATED', NULL, '{\"category\": \"ACCESS_REQUEST\"}', '2026-10-22 01:09:04.620968');
INSERT INTO `ticket_events` VALUES (23, 3, 4, 'RESULT_SUBMITTED', 'u', '{\"to\": \"AWAITING_CONFIRMATION\", \"from\": \"IN_PROGRESS\", \"resolution\": \"RESOLVED\"}', '2026-10-22 01:12:21.205394');
INSERT INTO `ticket_events` VALUES (24, 6, 1, 'CREATED', NULL, '{\"category\": \"ACCESS_REQUEST\"}', '2026-10-22 01:22:12.609435');
INSERT INTO `ticket_events` VALUES (25, 6, 3, 'COMMENTED', 'd sd ', '{}', '2026-10-22 01:22:34.113083');
INSERT INTO `ticket_events` VALUES (36, 6, 3, 'RESULT_SUBMITTED', 's', '{\"to\": \"COMPLETED\", \"from\": \"PENDING\", \"resolution\": \"APPROVED\"}', '2026-10-22 01:31:20.031856');

-- ----------------------------
-- Table structure for tickets
-- ----------------------------
DROP TABLE IF EXISTS `tickets`;
CREATE TABLE `tickets`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `request_id` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `requester_id` bigint UNSIGNED NOT NULL,
  `original_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `creation_digest` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '服务端规范序列化原文、提交分类、关联ID、初始附件ID后计算SHA-256',
  `initial_category` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '提交者最初选择的分类，不随纠正覆盖',
  `category` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前分类，协调员可纠正',
  `status` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDING',
  `assignee_id` bigint UNSIGNED NULL DEFAULT NULL,
  `related_ticket_id` bigint UNSIGNED NULL DEFAULT NULL,
  `version` bigint UNSIGNED NOT NULL DEFAULT 0,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `completed_at` datetime(6) NULL DEFAULT NULL,
  `resolution_code` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tickets_request_id`(`request_id` ASC) USING BTREE,
  INDEX `ix_tickets_requester_created`(`requester_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `ix_tickets_assignee_status_created`(`assignee_id` ASC, `status` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `ix_tickets_status_created`(`status` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `ix_tickets_category_created`(`category` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `fk_ticket_related`(`related_ticket_id` ASC) USING BTREE,
  CONSTRAINT `fk_ticket_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ticket_related` FOREIGN KEY (`related_ticket_id`) REFERENCES `tickets` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ticket_requester` FOREIGN KEY (`requester_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_ticket_assigned` CHECK ((`status` = _ascii'PENDING') or (`assignee_id` is not null)),
  CONSTRAINT `ck_ticket_category` CHECK (`category` in (_ascii'SYSTEM_FAILURE',_ascii'ACCESS_REQUEST',_ascii'FEATURE_SUGGESTION',_ascii'USAGE_QUESTION',_ascii'UNCONFIRMED')),
  CONSTRAINT `ck_ticket_completion` CHECK (((`status` = _ascii'COMPLETED') and (`completed_at` is not null)) or ((`status` <> _ascii'COMPLETED') and (`completed_at` is null))),
  CONSTRAINT `ck_ticket_initial_category` CHECK (`initial_category` in (_ascii'SYSTEM_FAILURE',_ascii'ACCESS_REQUEST',_ascii'FEATURE_SUGGESTION',_ascii'USAGE_QUESTION',_ascii'UNCONFIRMED')),
  CONSTRAINT `ck_ticket_resolution` CHECK ((`resolution_code` is null) or (`resolution_code` in (_utf8mb4'RESOLVED',_utf8mb4'ANSWERED',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'ACCEPTED',_utf8mb4'DEFERRED'))),
  CONSTRAINT `ck_ticket_status` CHECK (`status` in (_ascii'PENDING',_ascii'IN_PROGRESS',_ascii'AWAITING_CONFIRMATION',_ascii'REWORK',_ascii'COMPLETED')),
  CONSTRAINT `ck_ticket_text_length` CHECK (char_length(`original_text`) between 1 and 10000)
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tickets
-- ----------------------------
INSERT INTO `tickets` VALUES (1, '6f7fce66-c60d-4521-86d6-e0e876b14e15', 1, '相关bug', '1dee7e9b730847326fd202bdb2281afa5548b85bd508e466a612d073f538bbd5', 'SYSTEM_FAILURE', 'SYSTEM_FAILURE', 'COMPLETED', 4, NULL, 7, '2026-10-21 13:19:48.365256', '2026-10-21 13:42:43.507921', '2026-10-21 13:42:43.506529', 'RESOLVED');
INSERT INTO `tickets` VALUES (2, '050a2400-f03c-4202-9d78-7f3c50c04c5f', 1, 'aa', '00a301701b5db4d2987293826a71abc73520e49604a1c13539eb6e00ba7497ec', 'SYSTEM_FAILURE', 'SYSTEM_FAILURE', 'COMPLETED', 4, NULL, 7, '2026-10-21 13:40:29.706811', '2026-10-21 13:46:03.460016', '2026-10-21 13:46:03.459454', 'RESOLVED');
INSERT INTO `tickets` VALUES (3, '55b17aa8-52d8-43b9-af23-5d8f4c42a62d', 1, 'dfd ', '032030f3e597b7d8ddcb681deb4e2255da66e5bb7538ba1d309f9eb1676ec1ef', 'SYSTEM_FAILURE', 'SYSTEM_FAILURE', 'AWAITING_CONFIRMATION', 4, NULL, 5, '2026-10-22 01:05:37.894600', '2026-10-22 01:12:21.203820', NULL, 'RESOLVED');
INSERT INTO `tickets` VALUES (4, 'de5e9d11-8e10-4a8f-86db-a6e80f740129', 1, 'fdfaddd', '24d99a788af83406a723ab405dc6c274dcf4264305871b4538a2842c0f8eb9e5', 'ACCESS_REQUEST', 'ACCESS_REQUEST', 'PENDING', NULL, NULL, 0, '2026-10-22 01:09:04.619872', '2026-10-22 01:09:04.619872', NULL, NULL);
INSERT INTO `tickets` VALUES (6, '31c32b72-2702-4550-abfe-6565c7720a91', 1, 'das ', '8fce5a525a5259afb8351120bdc61aebe0bed4b73893939f853c328557180687', 'ACCESS_REQUEST', 'ACCESS_REQUEST', 'COMPLETED', 3, NULL, 2, '2026-10-22 01:22:12.602553', '2026-10-22 01:31:20.028498', '2026-10-22 01:31:20.025671', 'APPROVED');

-- ----------------------------
-- Table structure for user_permission
-- ----------------------------
DROP TABLE IF EXISTS `user_permission`;
CREATE TABLE `user_permission`  (
  `ticket_id` bigint UNSIGNED NOT NULL,
  `user_id` bigint UNSIGNED NOT NULL,
  `permission_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDING',
  `reviewed_by` bigint UNSIGNED NULL DEFAULT NULL,
  `reviewed_at` datetime(6) NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `permission_code` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL,
  PRIMARY KEY (`ticket_id`) USING BTREE,
  INDEX `ix_user_permission_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `fk_up_reviewer`(`reviewed_by` ASC) USING BTREE,
  INDEX `fk_up_permission`(`permission_code` ASC) USING BTREE,
  CONSTRAINT `fk_up_permission` FOREIGN KEY (`permission_code`) REFERENCES `permissions` (`code`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_up_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_up_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_up_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_up_review` CHECK (((`status` = _ascii'PENDING') and (`reviewed_by` is null) and (`reviewed_at` is null)) or ((`status` <> _ascii'PENDING') and (`reviewed_by` is not null) and (`reviewed_at` is not null))),
  CONSTRAINT `ck_up_status` CHECK (`status` in (_ascii'PENDING',_ascii'APPROVED',_ascii'REJECTED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_permission
-- ----------------------------
INSERT INTO `user_permission` VALUES (6, 1, '读取历史', 'APPROVED', 3, '2026-10-22 01:31:20.025325', '2026-10-22 01:22:12.605122', 'ticket:history:read');

-- ----------------------------
-- Table structure for user_roles
-- ----------------------------
DROP TABLE IF EXISTS `user_roles`;
CREATE TABLE `user_roles`  (
  `user_id` bigint UNSIGNED NOT NULL,
  `role_id` bigint UNSIGNED NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE,
  UNIQUE INDEX `uk_user_roles_single`(`user_id` ASC) USING BTREE,
  INDEX `fk_ur_role`(`role_id` ASC) USING BTREE,
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_roles
-- ----------------------------
INSERT INTO `user_roles` VALUES (1, 1);
INSERT INTO `user_roles` VALUES (2, 1);
INSERT INTO `user_roles` VALUES (3, 2);
INSERT INTO `user_roles` VALUES (4, 3);
INSERT INTO `user_roles` VALUES (5, 3);

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `display_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password_hash` varchar(255) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_users_username`(`username` ASC) USING BTREE,
  CONSTRAINT `ck_users_enabled` CHECK (`enabled` in (0,1))
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'requester1', '小李', '$2a$10$bKPUkchD/TSkowZJte7EfuSlbMXX2P0Fz1M5noKt26T7YhF0VGne.', 1, '2026-10-21 13:17:05.819169', '2026-10-21 13:17:05.819169');
INSERT INTO `users` VALUES (2, 'requester2', '小周', '$2a$10$tRFVbphgAuDpsyI275pWg.cegRenKuiqfLLS8M2Go.5Qtfg0Zc37C', 1, '2026-10-21 13:17:05.884160', '2026-10-21 13:17:05.884160');
INSERT INTO `users` VALUES (3, 'coordinator1', '协调员', '$2a$10$yq/jdfKX2jzvJ5O4MnLluOR3.LdKrcTdhrOENgEA2Ef2jf.qZ7Mc2', 1, '2026-10-21 13:17:05.940132', '2026-10-21 13:17:05.940132');
INSERT INTO `users` VALUES (4, 'support1', '技术支持一', '$2a$10$vO/FyhlUKFPppfsFa9AbV.nEEetpF.K8zrV3dHrHpvqkekRIYIjhK', 1, '2026-10-21 13:17:05.995619', '2026-10-21 13:17:05.995619');
INSERT INTO `users` VALUES (5, 'support2', '技术支持二', '$2a$10$DW40q2GcpDxBwRLmmol1duzm54yhBWnJ28YDuWfH37luAjUfuF.xO', 1, '2026-10-21 13:17:06.051891', '2026-10-21 13:17:06.051891');

SET FOREIGN_KEY_CHECKS = 1;
