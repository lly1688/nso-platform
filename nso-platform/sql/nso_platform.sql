/*
 Navicat Premium Data Transfer

 Source Server         : 本机
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : nso_platform

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 04/08/2026 16:44:19
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for nso_action_item
-- ----------------------------
DROP TABLE IF EXISTS `nso_action_item`;
CREATE TABLE `nso_action_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `source_id` bigint NOT NULL,
  `action_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `project_id` bigint NULL DEFAULT NULL,
  `assignee_user_id` bigint NULL DEFAULT NULL,
  `assignee_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `priority` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NORMAL',
  `sla_due_at` datetime NULL DEFAULT NULL,
  `route` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `source_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `action_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'OPEN',
  `source_version` int NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `closed_at` datetime NULL DEFAULT NULL,
  `version` int NOT NULL DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_action_projection`(`tenant_id` ASC, `source_type` ASC, `source_id` ASC, `action_code` ASC) USING BTREE,
  INDEX `idx_action_assignee_state_due`(`tenant_id` ASC, `assignee_user_id` ASC, `action_status` ASC, `sla_due_at` ASC) USING BTREE,
  INDEX `idx_action_project_state`(`tenant_id` ASC, `project_id` ASC, `action_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '可重建的统一行动项投影' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_action_item
-- ----------------------------

-- ----------------------------
-- Table structure for nso_audit_log
-- ----------------------------
DROP TABLE IF EXISTS `nso_audit_log`;
CREATE TABLE `nso_audit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `client_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `module_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `operation_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `business_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `business_id` bigint NULL DEFAULT NULL,
  `before_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `after_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `result` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `failure_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `operated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `user_id` bigint NULL DEFAULT NULL,
  `request_id` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_audit_business`(`business_type` ASC, `business_id` ASC) USING BTREE,
  INDEX `idx_audit_operated_at`(`operated_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审计日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_audit_log
-- ----------------------------

-- ----------------------------
-- Table structure for nso_authorization_audit
-- ----------------------------
DROP TABLE IF EXISTS `nso_authorization_audit`;
CREATE TABLE `nso_authorization_audit`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `subject_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `subject_id` bigint NOT NULL,
  `change_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `before_json` json NULL,
  `after_json` json NULL,
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `operator_id` bigint NULL DEFAULT NULL,
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_authorization_audit_subject`(`tenant_id` ASC, `subject_type` ASC, `subject_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账号、组织、授权和职责变更审计' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_authorization_audit
-- ----------------------------

-- ----------------------------
-- Table structure for nso_business_sequence
-- ----------------------------
DROP TABLE IF EXISTS `nso_business_sequence`;
CREATE TABLE `nso_business_sequence`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `business_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `period_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `next_value` bigint NOT NULL DEFAULT 1,
  `version` int NOT NULL DEFAULT 0,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_business_sequence`(`tenant_id` ASC, `business_type` ASC, `period_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '业务编号流水号' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_business_sequence
-- ----------------------------

-- ----------------------------
-- Table structure for nso_capa_transition
-- ----------------------------
DROP TABLE IF EXISTS `nso_capa_transition`;
CREATE TABLE `nso_capa_transition`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `exception_case_id` bigint NOT NULL,
  `from_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `to_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `idempotency_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `comment` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `operator_id` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_capa_transition_idempotency`(`tenant_id` ASC, `exception_case_id` ASC, `idempotency_key` ASC) USING BTREE,
  INDEX `idx_capa_transition_case`(`tenant_id` ASC, `exception_case_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '异常 CAPA 状态迁移与幂等记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_capa_transition
-- ----------------------------

-- ----------------------------
-- Table structure for nso_change_impact
-- ----------------------------
DROP TABLE IF EXISTS `nso_change_impact`;
CREATE TABLE `nso_change_impact`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `change_id` bigint NOT NULL,
  `object_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `object_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `department_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `suggested_action` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING_FEEDBACK',
  `feedback_result` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `responsible_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `version` int NOT NULL DEFAULT 0,
  `object_id` bigint NULL DEFAULT NULL,
  `object_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_impact_change`(`change_id` ASC) USING BTREE,
  INDEX `idx_impact_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '变更影响项' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_change_impact
-- ----------------------------

-- ----------------------------
-- Table structure for nso_change_order
-- ----------------------------
DROP TABLE IF EXISTS `nso_change_order`;
CREATE TABLE `nso_change_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `change_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `change_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `urgency` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NORMAL',
  `before_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `after_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `delay_days` int NOT NULL DEFAULT 0,
  `rework_qty` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `applicant_user_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `change_no`(`change_no` ASC) USING BTREE,
  INDEX `idx_change_project`(`project_id` ASC) USING BTREE,
  INDEX `idx_change_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '变更单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_change_order
-- ----------------------------

-- ----------------------------
-- Table structure for nso_customer
-- ----------------------------
DROP TABLE IF EXISTS `nso_customer`;
CREATE TABLE `nso_customer`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `industry` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `customer_code`(`customer_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客户档案' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_customer
-- ----------------------------

-- ----------------------------
-- Table structure for nso_document_compare
-- ----------------------------
DROP TABLE IF EXISTS `nso_document_compare`;
CREATE TABLE `nso_document_compare`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `before_version_id` bigint NOT NULL,
  `after_version_id` bigint NOT NULL,
  `comparison_file_id` bigint NULL DEFAULT NULL,
  `summary` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_document_compare`(`tenant_id` ASC, `before_version_id` ASC, `after_version_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '技术版本对比记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_document_compare
-- ----------------------------

-- ----------------------------
-- Table structure for nso_document_version
-- ----------------------------
DROP TABLE IF EXISTS `nso_document_version`;
CREATE TABLE `nso_document_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `file_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `version_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT',
  `effective_date` date NULL DEFAULT NULL,
  `change_summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `current_version` tinyint NOT NULL DEFAULT 0,
  `sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `storage_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `uploaded_by` bigint NULL DEFAULT NULL,
  `published_by` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_doc_project_type_version`(`project_id` ASC, `file_type` ASC, `version_no` ASC) USING BTREE,
  INDEX `idx_doc_current`(`project_id` ASC, `file_type` ASC, `current_version` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '技术文件版本' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_document_version
-- ----------------------------

-- ----------------------------
-- Table structure for nso_exception_case
-- ----------------------------
DROP TABLE IF EXISTS `nso_exception_case`;
CREATE TABLE `nso_exception_case`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `case_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `project_id` bigint NOT NULL,
  `task_id` bigint NULL DEFAULT NULL,
  `risk_id` bigint NULL DEFAULT NULL,
  `exception_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `reporter_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `owner_user_id` bigint NOT NULL,
  `owner_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `due_at` datetime NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'OPEN',
  `containment_plan` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `root_cause` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `corrective_plan` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `verification_summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `close_conclusion` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `idempotency_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `escalated_at` datetime NULL DEFAULT NULL,
  `closed_at` datetime NULL DEFAULT NULL,
  `version` int NOT NULL DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_exception_case_no`(`tenant_id` ASC, `case_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_exception_case_idempotency`(`tenant_id` ASC, `idempotency_key` ASC) USING BTREE,
  INDEX `idx_exception_project_state_due`(`tenant_id` ASC, `project_id` ASC, `status` ASC, `due_at` ASC) USING BTREE,
  INDEX `idx_exception_owner_state_due`(`tenant_id` ASC, `owner_user_id` ASC, `status` ASC, `due_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '异常 CAPA 闭环案例' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_exception_case
-- ----------------------------

-- ----------------------------
-- Table structure for nso_exception_evidence
-- ----------------------------
DROP TABLE IF EXISTS `nso_exception_evidence`;
CREATE TABLE `nso_exception_evidence`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `exception_case_id` bigint NOT NULL,
  `evidence_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'REFERENCE',
  `evidence_ref` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `submitted_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_exception_evidence_case`(`tenant_id` ASC, `exception_case_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '异常 CAPA 证据索引' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_exception_evidence
-- ----------------------------

-- ----------------------------
-- Table structure for nso_file_cleanup_task
-- ----------------------------
DROP TABLE IF EXISTS `nso_file_cleanup_task`;
CREATE TABLE `nso_file_cleanup_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `file_object_id` bigint NOT NULL,
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `retry_count` int NOT NULL DEFAULT 0,
  `last_error` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_file_cleanup`(`tenant_id` ASC, `file_object_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '孤立文件清理队列' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_file_cleanup_task
-- ----------------------------

-- ----------------------------
-- Table structure for nso_file_object
-- ----------------------------
DROP TABLE IF EXISTS `nso_file_object`;
CREATE TABLE `nso_file_object`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `file_size` bigint NOT NULL,
  `sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `storage_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT 1,
  `project_id` bigint NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_sha256`(`sha256` ASC) USING BTREE,
  INDEX `idx_file_object_tenant`(`tenant_id` ASC, `id` ASC) USING BTREE,
  INDEX `idx_file_project`(`tenant_id` ASC, `project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件对象' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_file_object
-- ----------------------------

-- ----------------------------
-- Table structure for nso_message
-- ----------------------------
DROP TABLE IF EXISTS `nso_message`;
CREATE TABLE `nso_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'UNREAD',
  `business_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `business_id` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_message_status`(`status` ASC) USING BTREE,
  INDEX `idx_message_business`(`business_type` ASC, `business_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息中心' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_message
-- ----------------------------

-- ----------------------------
-- Table structure for nso_operation_confirmation
-- ----------------------------
DROP TABLE IF EXISTS `nso_operation_confirmation`;
CREATE TABLE `nso_operation_confirmation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `operation_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `business_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `business_id` bigint NOT NULL,
  `requester_id` bigint NULL DEFAULT NULL,
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `challenge_id` bigint NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `expires_at` datetime NOT NULL,
  `confirmed_at` datetime NULL DEFAULT NULL,
  `consumed_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation_confirmation_lookup`(`tenant_id` ASC, `operation_code` ASC, `business_type` ASC, `business_id` ASC, `status` ASC, `expires_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '高风险操作二次确认' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_operation_confirmation
-- ----------------------------

-- ----------------------------
-- Table structure for nso_password_recovery_request
-- ----------------------------
DROP TABLE IF EXISTS `nso_password_recovery_request`;
CREATE TABLE `nso_password_recovery_request`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `contact_value` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `requester_note` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `active_key` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `handler_id` bigint NULL DEFAULT NULL,
  `handling_note` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reviewed_at` datetime NULL DEFAULT NULL,
  `reset_at` datetime NULL DEFAULT NULL,
  `version` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_password_recovery_active`(`active_key` ASC) USING BTREE,
  INDEX `idx_password_recovery_queue`(`tenant_id` ASC, `status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_password_recovery_user`(`tenant_id` ASC, `user_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '内部账号密码恢复申请' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_password_recovery_request
-- ----------------------------

-- ----------------------------
-- Table structure for nso_project
-- ----------------------------
DROP TABLE IF EXISTS `nso_project`;
CREATE TABLE `nso_project`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `customer_id` bigint NULL DEFAULT NULL,
  `customer_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `product_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `project_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `quantity` int NOT NULL DEFAULT 1,
  `target_date` date NULL DEFAULT NULL,
  `owner_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `stage` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `priority` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MEDIUM',
  `risk_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'LOW',
  `sample_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NONE',
  `archived_at` datetime NULL DEFAULT NULL,
  `archived_by` bigint NULL DEFAULT NULL,
  `archive_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `suspended_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `owner_user_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `project_no`(`project_no` ASC) USING BTREE,
  INDEX `idx_project_customer`(`customer_id` ASC) USING BTREE,
  INDEX `idx_project_status`(`status` ASC) USING BTREE,
  INDEX `idx_project_risk`(`risk_level` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '非标订单项目' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_project
-- ----------------------------

-- ----------------------------
-- Table structure for nso_project_archive
-- ----------------------------
DROP TABLE IF EXISTS `nso_project_archive`;
CREATE TABLE `nso_project_archive`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `project_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `customer_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `archived_by` bigint NULL DEFAULT NULL,
  `archive_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `archived_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `restored_by` bigint NULL DEFAULT NULL,
  `restored_at` datetime NULL DEFAULT NULL,
  `restore_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ARCHIVED',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_project_archive`(`tenant_id` ASC, `project_id` ASC) USING BTREE,
  INDEX `idx_project_archive_search`(`tenant_id` ASC, `project_no` ASC, `customer_name` ASC, `archived_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '项目归档与恢复记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_project_archive
-- ----------------------------

-- ----------------------------
-- Table structure for nso_project_manager_history
-- ----------------------------
DROP TABLE IF EXISTS `nso_project_manager_history`;
CREATE TABLE `nso_project_manager_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `previous_manager_user_id` bigint NULL DEFAULT NULL,
  `next_manager_user_id` bigint NOT NULL,
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `operator_id` bigint NULL DEFAULT NULL,
  `transferred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_manager_history`(`tenant_id` ASC, `project_id` ASC, `transferred_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '项目经理交接历史' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_project_manager_history
-- ----------------------------

-- ----------------------------
-- Table structure for nso_project_member_responsibility
-- ----------------------------
DROP TABLE IF EXISTS `nso_project_member_responsibility`;
CREATE TABLE `nso_project_member_responsibility`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `project_member_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `responsibility_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `primary_flag` tinyint NOT NULL DEFAULT 0,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE',
  `version` int NOT NULL DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_project_member_responsibility`(`tenant_id` ASC, `project_member_id` ASC, `responsibility_code` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_project_responsibility_scope`(`tenant_id` ASC, `project_id` ASC, `user_id` ASC, `responsibility_code` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '项目成员职责' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_project_member_responsibility
-- ----------------------------

-- ----------------------------
-- Table structure for nso_project_requirement
-- ----------------------------
DROP TABLE IF EXISTS `nso_project_requirement`;
CREATE TABLE `nso_project_requirement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `confirm_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'UNCONFIRMED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_requirement_project`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '项目需求项' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_project_requirement
-- ----------------------------

-- ----------------------------
-- Table structure for nso_risk
-- ----------------------------
DROP TABLE IF EXISTS `nso_risk`;
CREATE TABLE `nso_risk`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `score` int NOT NULL,
  `reasons` json NULL,
  `suggestion` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'OPEN',
  `calculated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_risk_project`(`project_id` ASC) USING BTREE,
  INDEX `idx_risk_level`(`level` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '交期风险' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_risk
-- ----------------------------

-- ----------------------------
-- Table structure for nso_sample
-- ----------------------------
DROP TABLE IF EXISTS `nso_sample`;
CREATE TABLE `nso_sample`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `sample_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `purpose` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `quantity` int NOT NULL DEFAULT 1,
  `plan_finish_date` date NULL DEFAULT NULL,
  `referenced_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `confirm_conclusion` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `responsible_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `issue_summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint NULL DEFAULT NULL,
  `quality_confirmed_by` bigint NULL DEFAULT NULL,
  `proxy_confirmed_by` bigint NULL DEFAULT NULL,
  `proxy_confirmation` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `sample_no`(`sample_no` ASC) USING BTREE,
  INDEX `idx_sample_project`(`project_id` ASC) USING BTREE,
  INDEX `idx_sample_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '样品单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_sample
-- ----------------------------

-- ----------------------------
-- Table structure for nso_sensitive_field_policy
-- ----------------------------
DROP TABLE IF EXISTS `nso_sensitive_field_policy`;
CREATE TABLE `nso_sensitive_field_policy`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `field_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `read_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MASKED',
  `write_allowed` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sensitive_field_policy`(`tenant_id` ASC, `role_code` ASC, `field_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '敏感字段读取与编辑策略' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_sensitive_field_policy
-- ----------------------------
INSERT INTO `nso_sensitive_field_policy` VALUES (1, 1, 'admin', 'CUSTOMER_CONTACT', 'FULL', 1, '2026-07-28 21:08:26', '2026-07-28 21:08:26');
INSERT INTO `nso_sensitive_field_policy` VALUES (2, 1, 'project_manager', 'CUSTOMER_CONTACT', 'FULL', 1, '2026-07-28 21:08:26', '2026-07-28 21:08:26');
INSERT INTO `nso_sensitive_field_policy` VALUES (3, 1, 'technical', 'CUSTOMER_CONTACT', 'MASKED', 0, '2026-07-28 21:08:26', '2026-07-28 21:08:26');
INSERT INTO `nso_sensitive_field_policy` VALUES (4, 1, 'process', 'CUSTOMER_CONTACT', 'MASKED', 0, '2026-07-28 21:08:26', '2026-07-28 21:08:26');
INSERT INTO `nso_sensitive_field_policy` VALUES (5, 1, 'purchaser', 'CUSTOMER_CONTACT', 'MASKED', 0, '2026-07-28 21:08:26', '2026-07-28 21:08:26');
INSERT INTO `nso_sensitive_field_policy` VALUES (6, 1, 'production', 'CUSTOMER_CONTACT', 'MASKED', 0, '2026-07-28 21:08:26', '2026-07-28 21:08:26');
INSERT INTO `nso_sensitive_field_policy` VALUES (7, 1, 'quality', 'CUSTOMER_CONTACT', 'MASKED', 0, '2026-07-28 21:08:26', '2026-07-28 21:08:26');
INSERT INTO `nso_sensitive_field_policy` VALUES (8, 1, 'executive', 'CUSTOMER_CONTACT', 'MASKED', 0, '2026-07-28 21:08:26', '2026-07-28 21:08:26');

-- ----------------------------
-- Table structure for nso_special_release
-- ----------------------------
DROP TABLE IF EXISTS `nso_special_release`;
CREATE TABLE `nso_special_release`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `sample_id` bigint NULL DEFAULT NULL,
  `applicant_user_id` bigint NOT NULL,
  `approver_user_id` bigint NULL DEFAULT NULL,
  `release_scope` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `valid_until` datetime NOT NULL,
  `risk_statement` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `approved_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_special_release_project`(`tenant_id` ASC, `project_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_special_release_sample`(`tenant_id` ASC, `sample_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '样品与投产特殊放行记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_special_release
-- ----------------------------

-- ----------------------------
-- Table structure for nso_support_ticket
-- ----------------------------
DROP TABLE IF EXISTS `nso_support_ticket`;
CREATE TABLE `nso_support_ticket`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `ticket_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `requester_user_id` bigint NULL DEFAULT NULL,
  `requester_username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `contact_value` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `source` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `priority` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NORMAL',
  `page_context` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'OPEN',
  `handler_id` bigint NULL DEFAULT NULL,
  `handling_note` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `resolved_at` datetime NULL DEFAULT NULL,
  `version` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_support_ticket_no`(`tenant_id` ASC, `ticket_no` ASC) USING BTREE,
  INDEX `idx_support_ticket_queue`(`tenant_id` ASC, `status` ASC, `priority` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_support_ticket_requester`(`tenant_id` ASC, `requester_user_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台技术支持工单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_support_ticket
-- ----------------------------

-- ----------------------------
-- Table structure for nso_support_ticket_attachment
-- ----------------------------
DROP TABLE IF EXISTS `nso_support_ticket_attachment`;
CREATE TABLE `nso_support_ticket_attachment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `ticket_id` bigint NOT NULL,
  `file_object_id` bigint NOT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_support_ticket_attachment`(`tenant_id` ASC, `ticket_id` ASC, `file_object_id` ASC) USING BTREE,
  INDEX `idx_support_attachment_ticket`(`tenant_id` ASC, `ticket_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '技术支持工单图片附件' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_support_ticket_attachment
-- ----------------------------

-- ----------------------------
-- Table structure for nso_task
-- ----------------------------
DROP TABLE IF EXISTS `nso_task`;
CREATE TABLE `nso_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `capa_case_id` bigint NULL DEFAULT NULL,
  `capa_idempotency_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `task_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `task_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `referenced_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `responsible_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `plan_start` date NULL DEFAULT NULL,
  `plan_finish` date NULL DEFAULT NULL,
  `block_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `task_no`(`task_no` ASC) USING BTREE,
  INDEX `idx_task_project`(`project_id` ASC) USING BTREE,
  INDEX `idx_task_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '协同任务' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_task
-- ----------------------------

-- ----------------------------
-- Table structure for nso_tenant
-- ----------------------------
DROP TABLE IF EXISTS `nso_tenant`;
CREATE TABLE `nso_tenant`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tenant_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_nso_tenant_code`(`tenant_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '企业租户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_tenant
-- ----------------------------
INSERT INTO `nso_tenant` VALUES (1, 'DEFAULT', '默认企业', 'ENABLED', '2026-07-27 08:21:00', '2026-07-27 08:21:00');

-- ----------------------------
-- Table structure for nso_timeline_event
-- ----------------------------
DROP TABLE IF EXISTS `nso_timeline_event`;
CREATE TABLE `nso_timeline_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT 1,
  `business_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `business_id` bigint NULL DEFAULT NULL,
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_timeline_project`(`project_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_timeline_tenant_project`(`tenant_id` ASC, `project_id` ASC, `occurred_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '项目时间轴' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_timeline_event
-- ----------------------------

-- ----------------------------
-- Table structure for nso_user_handover
-- ----------------------------
DROP TABLE IF EXISTS `nso_user_handover`;
CREATE TABLE `nso_user_handover`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `from_user_id` bigint NOT NULL,
  `to_user_id` bigint NULL DEFAULT NULL,
  `handover_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `scope_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `scope_id` bigint NULL DEFAULT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `requested_by` bigint NULL DEFAULT NULL,
  `completed_by` bigint NULL DEFAULT NULL,
  `completed_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_handover_from_status`(`tenant_id` ASC, `from_user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_user_handover_to_status`(`tenant_id` ASC, `to_user_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '调岗、停用与离职交接单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_user_handover
-- ----------------------------

-- ----------------------------
-- Table structure for nso_verification_challenge
-- ----------------------------
DROP TABLE IF EXISTS `nso_verification_challenge`;
CREATE TABLE `nso_verification_challenge`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `receiver_id` bigint NULL DEFAULT NULL,
  `channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `purpose` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `code_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `attempt_count` int NOT NULL DEFAULT 0,
  `max_attempts` int NOT NULL DEFAULT 5,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `expires_at` datetime NOT NULL,
  `verified_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_verification_challenge`(`tenant_id` ASC, `receiver_id` ASC, `purpose` ASC, `status` ASC, `expires_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '一次性验证码挑战' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of nso_verification_challenge
-- ----------------------------

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 1,
  `parent_id` bigint NULL DEFAULT NULL,
  `ancestors` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `dept_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `leader_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `leader_user_id` bigint NULL DEFAULT NULL,
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sort_no` int NOT NULL DEFAULT 0,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED',
  `version` int NOT NULL DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_dept_name`(`tenant_id` ASC, `dept_name` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_sys_dept_parent`(`tenant_id` ASC, `parent_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '组织部门' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (1, 1, NULL, '', '默认部门', '系统管理员', NULL, NULL, 1, 'ENABLED', 0, 0, '2026-07-27 08:20:47', '2026-07-27 08:20:47');
INSERT INTO `sys_dept` VALUES (2, 1, NULL, '', '平台运营部', '系统管理员', NULL, NULL, 10, 'ENABLED', 0, 0, '2026-07-28 15:17:42', '2026-08-02 08:44:12');
INSERT INTO `sys_dept` VALUES (3, 1, NULL, '', '项目管理部', '陈晓明', NULL, NULL, 20, 'ENABLED', 0, 0, '2026-07-28 15:17:42', '2026-08-02 08:44:12');
INSERT INTO `sys_dept` VALUES (4, 1, NULL, '', '技术设计部', '技术设计工程师', NULL, NULL, 30, 'ENABLED', 0, 0, '2026-07-28 15:17:42', '2026-08-02 08:44:12');
INSERT INTO `sys_dept` VALUES (5, 1, NULL, '', '工艺工程部', '工艺工程师', NULL, NULL, 40, 'ENABLED', 0, 0, '2026-07-28 15:17:42', '2026-08-02 08:44:12');
INSERT INTO `sys_dept` VALUES (6, 1, NULL, '', '采购供应部', '采购专员', NULL, NULL, 50, 'ENABLED', 0, 0, '2026-07-28 15:17:42', '2026-08-02 08:44:12');
INSERT INTO `sys_dept` VALUES (7, 1, NULL, '', '生产计划部', '生产计划员', NULL, NULL, 60, 'ENABLED', 0, 0, '2026-07-28 15:17:42', '2026-08-02 08:44:12');
INSERT INTO `sys_dept` VALUES (8, 1, NULL, '', '质量管理部', '质量工程师', NULL, NULL, 70, 'ENABLED', 0, 0, '2026-07-28 15:17:42', '2026-08-02 08:44:12');
INSERT INTO `sys_dept` VALUES (9, 1, NULL, '', '客户协同组', '客户接口人', NULL, NULL, 80, 'ENABLED', 0, 0, '2026-07-28 15:17:42', '2026-08-02 08:44:12');
INSERT INTO `sys_dept` VALUES (10, 1, NULL, '', '经营管理部', '经营负责人', NULL, NULL, 90, 'ENABLED', 0, 0, '2026-07-28 15:17:42', '2026-08-02 08:44:12');

-- ----------------------------
-- Table structure for sys_dept_manager
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept_manager`;
CREATE TABLE `sys_dept_manager`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `dept_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `manager_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `effective_from` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `effective_to` datetime NULL DEFAULT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE',
  `created_by` bigint NULL DEFAULT NULL,
  `reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dept_manager_current`(`tenant_id` ASC, `dept_id` ASC, `status` ASC, `manager_type` ASC) USING BTREE,
  INDEX `idx_dept_manager_user`(`tenant_id` ASC, `user_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门负责人及副负责人任职历史' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept_manager
-- ----------------------------

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `dict_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `dict_label` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_no` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dict_type_code`(`dict_type` ASC, `dict_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '业务字典' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES (1, 'project_status', 'DRAFT', '订单草稿', 1);
INSERT INTO `sys_dict_data` VALUES (2, 'project_status', 'TECH_PUBLISHED', '技术包已发布', 2);
INSERT INTO `sys_dict_data` VALUES (3, 'sample_status', 'WAIT_CUSTOMER_CONFIRM', '待客户确认', 1);
INSERT INTO `sys_dict_data` VALUES (4, 'sample_status', 'CONFIRMED', '已确认', 2);
INSERT INTO `sys_dict_data` VALUES (5, 'change_status', 'WAIT_IMPACT', '待影响分析', 1);
INSERT INTO `sys_dict_data` VALUES (6, 'change_status', 'EXECUTING', '执行中', 2);
INSERT INTO `sys_dict_data` VALUES (7, 'risk_level', 'LOW', '低风险', 1);
INSERT INTO `sys_dict_data` VALUES (8, 'risk_level', 'MEDIUM', '中风险', 2);
INSERT INTO `sys_dict_data` VALUES (9, 'risk_level', 'HIGH', '高风险', 3);
INSERT INTO `sys_dict_data` VALUES (10, 'risk_level', 'SERIOUS', '严重风险', 4);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NULL DEFAULT NULL,
  `menu_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `route_path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `permission_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `component_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `visible` tinyint NOT NULL DEFAULT 1,
  `sort_no` int NOT NULL DEFAULT 0,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED',
  `version` int NOT NULL DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_menu_permission`(`permission_code` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统菜单与功能权限' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, NULL, '工作台', '/dashboard', 'dashboard:view', 'Dashboard', 1, 1, 'ENABLED', 0, 0, '2026-07-27 08:20:47', '2026-07-27 08:20:47');
INSERT INTO `sys_menu` VALUES (2, NULL, '客户项目', '/projects', 'project:view', 'Projects', 1, 2, 'ENABLED', 0, 0, '2026-07-27 08:20:47', '2026-07-27 08:20:47');
INSERT INTO `sys_menu` VALUES (3, NULL, '技术文件', '/documents', 'document:view', 'Documents', 1, 3, 'ENABLED', 0, 0, '2026-07-27 08:20:47', '2026-07-27 08:20:47');
INSERT INTO `sys_menu` VALUES (4, NULL, '样品管理', '/samples', 'sample:view', 'Samples', 1, 4, 'ENABLED', 0, 0, '2026-07-27 08:20:47', '2026-07-27 08:20:47');
INSERT INTO `sys_menu` VALUES (5, NULL, '变更中心', '/changes', 'change:view', 'Changes', 1, 5, 'ENABLED', 0, 0, '2026-07-27 08:20:47', '2026-07-27 08:20:47');
INSERT INTO `sys_menu` VALUES (6, NULL, '任务风险', '/tasks', 'task:view', 'Tasks', 1, 6, 'ENABLED', 0, 0, '2026-07-27 08:20:47', '2026-07-27 08:20:47');
INSERT INTO `sys_menu` VALUES (7, NULL, '统计分析', '/reports', 'report:view', 'Reports', 1, 7, 'ENABLED', 0, 0, '2026-07-27 08:20:47', '2026-07-27 08:20:47');
INSERT INTO `sys_menu` VALUES (8, NULL, '系统管理', '/system', 'system:manage', 'System', 1, 8, 'ENABLED', 0, 0, '2026-07-27 08:20:47', '2026-07-27 08:20:47');
INSERT INTO `sys_menu` VALUES (9, NULL, '客户管理', '/projects', 'customer:view', 'Projects', 1, 11, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (10, NULL, '客户维护', NULL, 'customer:manage', NULL, 0, 12, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (11, NULL, '项目新建', NULL, 'project:create', NULL, 0, 21, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (12, NULL, '项目维护', NULL, 'project:manage', NULL, 0, 22, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (13, NULL, '项目成员维护', NULL, 'project:member:manage', NULL, 0, 23, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (14, NULL, '客户邀请', NULL, 'customer:invite', NULL, 0, 24, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (15, NULL, '需求维护', NULL, 'project:requirement:manage', NULL, 0, 25, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (16, NULL, '技术资料上传', NULL, 'document:upload', NULL, 0, 31, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (17, NULL, '技术版本发布', NULL, 'document:publish', NULL, 0, 32, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (18, NULL, 'BOM维护', NULL, 'bom:manage', NULL, 0, 33, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (19, NULL, '工艺维护', NULL, 'process:manage', NULL, 0, 34, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (20, NULL, '检验规范维护', NULL, 'inspection:manage', NULL, 0, 35, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (21, NULL, '样品创建', NULL, 'sample:create', NULL, 0, 41, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (22, NULL, '样品提交客户确认', NULL, 'sample:submit', NULL, 0, 42, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (23, NULL, '样品质量检验', NULL, 'sample:inspect', NULL, 0, 43, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (24, NULL, '销售代录客户结论', NULL, 'sample:proxy-confirm', NULL, 0, 44, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (25, NULL, '特殊放行申请', NULL, 'sample:release:apply', NULL, 0, 45, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (26, NULL, '特殊放行批准', NULL, 'sample:release:approve', NULL, 0, 46, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (27, NULL, '变更发起', NULL, 'change:create', NULL, 0, 51, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (28, NULL, '变更影响分析', NULL, 'change:analyze', NULL, 0, 52, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (29, NULL, '变更审批', NULL, 'change:approve', NULL, 0, 53, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (30, NULL, '变更执行反馈', NULL, 'change:feedback', NULL, 0, 54, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (31, NULL, '变更关闭', NULL, 'change:close', NULL, 0, 55, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (32, NULL, '任务排程', NULL, 'task:plan', NULL, 0, 61, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (33, NULL, '任务执行', NULL, 'task:execute', NULL, 0, 62, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (34, NULL, '任务反馈', NULL, 'task:feedback', NULL, 0, 63, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (35, NULL, '风险查看', NULL, 'risk:view', NULL, 0, 71, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (36, NULL, '消息查看', NULL, 'message:view', NULL, 0, 72, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (37, NULL, '客户确认门户', NULL, 'customer:portal', NULL, 0, 81, 'ENABLED', 0, 0, '2026-07-27 10:18:05', '2026-07-27 10:18:05');
INSERT INTO `sys_menu` VALUES (38, NULL, '账户支持', '/system/account-support', 'sys:account-support:manage', 'SystemAccountSupport', 0, 920, 'ENABLED', 0, 0, '2026-08-02 08:44:26', '2026-08-02 08:44:26');

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `post_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_no` int NOT NULL DEFAULT 0,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_post_tenant_code`(`tenant_id` ASC, `post_code` ASC) USING BTREE,
  INDEX `idx_sys_post_tenant_status`(`tenant_id` ASC, `status` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '组织岗位，不等同于系统角色' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_post
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `role_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT 1,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED',
  `data_scope` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SELF',
  `version` int NOT NULL DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统角色' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'admin', '系统管理员', '2026-07-26 19:58:50', 1, 'ENABLED', 'ALL', 0, 0);
INSERT INTO `sys_role` VALUES (2, 'project_manager', '销售/项目经理', '2026-07-26 19:58:50', 1, 'ENABLED', 'DEPT_AND_CHILD', 0, 0);
INSERT INTO `sys_role` VALUES (3, 'field_user', '现场人员', '2026-07-26 19:58:50', 1, 'ENABLED', 'SELF', 0, 0);
INSERT INTO `sys_role` VALUES (4, 'technical', '技术/设计人员', '2026-07-27 10:18:05', 1, 'ENABLED', 'SELF', 0, 0);
INSERT INTO `sys_role` VALUES (5, 'process', '工艺人员', '2026-07-27 10:18:05', 1, 'ENABLED', 'SELF', 0, 0);
INSERT INTO `sys_role` VALUES (6, 'purchaser', '采购人员', '2026-07-27 10:18:05', 1, 'ENABLED', 'SELF', 0, 0);
INSERT INTO `sys_role` VALUES (7, 'production', '计划/生产人员', '2026-07-27 10:18:05', 1, 'ENABLED', 'SELF', 0, 0);
INSERT INTO `sys_role` VALUES (8, 'quality', '质量人员', '2026-07-27 10:18:05', 1, 'ENABLED', 'SELF', 0, 0);
INSERT INTO `sys_role` VALUES (9, 'customer_confirm', '客户确认人', '2026-07-27 10:18:05', 1, 'ENABLED', 'SELF', 0, 0);
INSERT INTO `sys_role` VALUES (10, 'executive', '管理层', '2026-07-27 10:18:05', 1, 'ENABLED', 'ALL', 0, 0);
INSERT INTO `sys_role` VALUES (11, 'sales', '销售人员', '2026-07-29 01:57:06', 1, 'ENABLED', 'SELF', 0, 0);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单权限关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 1);
INSERT INTO `sys_role_menu` VALUES (1, 2);
INSERT INTO `sys_role_menu` VALUES (1, 3);
INSERT INTO `sys_role_menu` VALUES (1, 4);
INSERT INTO `sys_role_menu` VALUES (1, 5);
INSERT INTO `sys_role_menu` VALUES (1, 6);
INSERT INTO `sys_role_menu` VALUES (1, 7);
INSERT INTO `sys_role_menu` VALUES (1, 8);
INSERT INTO `sys_role_menu` VALUES (1, 9);
INSERT INTO `sys_role_menu` VALUES (1, 10);
INSERT INTO `sys_role_menu` VALUES (1, 11);
INSERT INTO `sys_role_menu` VALUES (1, 12);
INSERT INTO `sys_role_menu` VALUES (1, 13);
INSERT INTO `sys_role_menu` VALUES (1, 14);
INSERT INTO `sys_role_menu` VALUES (1, 15);
INSERT INTO `sys_role_menu` VALUES (1, 16);
INSERT INTO `sys_role_menu` VALUES (1, 17);
INSERT INTO `sys_role_menu` VALUES (1, 18);
INSERT INTO `sys_role_menu` VALUES (1, 19);
INSERT INTO `sys_role_menu` VALUES (1, 20);
INSERT INTO `sys_role_menu` VALUES (1, 21);
INSERT INTO `sys_role_menu` VALUES (1, 22);
INSERT INTO `sys_role_menu` VALUES (1, 23);
INSERT INTO `sys_role_menu` VALUES (1, 24);
INSERT INTO `sys_role_menu` VALUES (1, 25);
INSERT INTO `sys_role_menu` VALUES (1, 26);
INSERT INTO `sys_role_menu` VALUES (1, 27);
INSERT INTO `sys_role_menu` VALUES (1, 28);
INSERT INTO `sys_role_menu` VALUES (1, 29);
INSERT INTO `sys_role_menu` VALUES (1, 30);
INSERT INTO `sys_role_menu` VALUES (1, 31);
INSERT INTO `sys_role_menu` VALUES (1, 32);
INSERT INTO `sys_role_menu` VALUES (1, 33);
INSERT INTO `sys_role_menu` VALUES (1, 34);
INSERT INTO `sys_role_menu` VALUES (1, 35);
INSERT INTO `sys_role_menu` VALUES (1, 36);
INSERT INTO `sys_role_menu` VALUES (1, 37);
INSERT INTO `sys_role_menu` VALUES (1, 38);
INSERT INTO `sys_role_menu` VALUES (2, 1);
INSERT INTO `sys_role_menu` VALUES (2, 2);
INSERT INTO `sys_role_menu` VALUES (2, 3);
INSERT INTO `sys_role_menu` VALUES (2, 4);
INSERT INTO `sys_role_menu` VALUES (2, 5);
INSERT INTO `sys_role_menu` VALUES (2, 6);
INSERT INTO `sys_role_menu` VALUES (2, 7);
INSERT INTO `sys_role_menu` VALUES (2, 9);
INSERT INTO `sys_role_menu` VALUES (2, 10);
INSERT INTO `sys_role_menu` VALUES (2, 11);
INSERT INTO `sys_role_menu` VALUES (2, 12);
INSERT INTO `sys_role_menu` VALUES (2, 13);
INSERT INTO `sys_role_menu` VALUES (2, 14);
INSERT INTO `sys_role_menu` VALUES (2, 15);
INSERT INTO `sys_role_menu` VALUES (2, 21);
INSERT INTO `sys_role_menu` VALUES (2, 22);
INSERT INTO `sys_role_menu` VALUES (2, 24);
INSERT INTO `sys_role_menu` VALUES (2, 27);
INSERT INTO `sys_role_menu` VALUES (2, 29);
INSERT INTO `sys_role_menu` VALUES (2, 30);
INSERT INTO `sys_role_menu` VALUES (2, 31);
INSERT INTO `sys_role_menu` VALUES (2, 32);
INSERT INTO `sys_role_menu` VALUES (2, 33);
INSERT INTO `sys_role_menu` VALUES (2, 34);
INSERT INTO `sys_role_menu` VALUES (2, 35);
INSERT INTO `sys_role_menu` VALUES (2, 36);
INSERT INTO `sys_role_menu` VALUES (4, 1);
INSERT INTO `sys_role_menu` VALUES (4, 2);
INSERT INTO `sys_role_menu` VALUES (4, 3);
INSERT INTO `sys_role_menu` VALUES (4, 4);
INSERT INTO `sys_role_menu` VALUES (4, 5);
INSERT INTO `sys_role_menu` VALUES (4, 6);
INSERT INTO `sys_role_menu` VALUES (4, 16);
INSERT INTO `sys_role_menu` VALUES (4, 17);
INSERT INTO `sys_role_menu` VALUES (4, 18);
INSERT INTO `sys_role_menu` VALUES (4, 19);
INSERT INTO `sys_role_menu` VALUES (4, 20);
INSERT INTO `sys_role_menu` VALUES (4, 27);
INSERT INTO `sys_role_menu` VALUES (4, 28);
INSERT INTO `sys_role_menu` VALUES (4, 29);
INSERT INTO `sys_role_menu` VALUES (4, 35);
INSERT INTO `sys_role_menu` VALUES (4, 36);
INSERT INTO `sys_role_menu` VALUES (5, 1);
INSERT INTO `sys_role_menu` VALUES (5, 2);
INSERT INTO `sys_role_menu` VALUES (5, 3);
INSERT INTO `sys_role_menu` VALUES (5, 4);
INSERT INTO `sys_role_menu` VALUES (5, 5);
INSERT INTO `sys_role_menu` VALUES (5, 6);
INSERT INTO `sys_role_menu` VALUES (5, 18);
INSERT INTO `sys_role_menu` VALUES (5, 19);
INSERT INTO `sys_role_menu` VALUES (5, 20);
INSERT INTO `sys_role_menu` VALUES (5, 28);
INSERT INTO `sys_role_menu` VALUES (5, 30);
INSERT INTO `sys_role_menu` VALUES (5, 36);
INSERT INTO `sys_role_menu` VALUES (6, 1);
INSERT INTO `sys_role_menu` VALUES (6, 2);
INSERT INTO `sys_role_menu` VALUES (6, 3);
INSERT INTO `sys_role_menu` VALUES (6, 4);
INSERT INTO `sys_role_menu` VALUES (6, 5);
INSERT INTO `sys_role_menu` VALUES (6, 6);
INSERT INTO `sys_role_menu` VALUES (6, 30);
INSERT INTO `sys_role_menu` VALUES (6, 33);
INSERT INTO `sys_role_menu` VALUES (6, 34);
INSERT INTO `sys_role_menu` VALUES (6, 35);
INSERT INTO `sys_role_menu` VALUES (6, 36);
INSERT INTO `sys_role_menu` VALUES (7, 1);
INSERT INTO `sys_role_menu` VALUES (7, 2);
INSERT INTO `sys_role_menu` VALUES (7, 3);
INSERT INTO `sys_role_menu` VALUES (7, 4);
INSERT INTO `sys_role_menu` VALUES (7, 5);
INSERT INTO `sys_role_menu` VALUES (7, 6);
INSERT INTO `sys_role_menu` VALUES (7, 25);
INSERT INTO `sys_role_menu` VALUES (7, 29);
INSERT INTO `sys_role_menu` VALUES (7, 30);
INSERT INTO `sys_role_menu` VALUES (7, 32);
INSERT INTO `sys_role_menu` VALUES (7, 33);
INSERT INTO `sys_role_menu` VALUES (7, 34);
INSERT INTO `sys_role_menu` VALUES (7, 35);
INSERT INTO `sys_role_menu` VALUES (7, 36);
INSERT INTO `sys_role_menu` VALUES (8, 1);
INSERT INTO `sys_role_menu` VALUES (8, 2);
INSERT INTO `sys_role_menu` VALUES (8, 3);
INSERT INTO `sys_role_menu` VALUES (8, 4);
INSERT INTO `sys_role_menu` VALUES (8, 5);
INSERT INTO `sys_role_menu` VALUES (8, 6);
INSERT INTO `sys_role_menu` VALUES (8, 23);
INSERT INTO `sys_role_menu` VALUES (8, 26);
INSERT INTO `sys_role_menu` VALUES (8, 29);
INSERT INTO `sys_role_menu` VALUES (8, 30);
INSERT INTO `sys_role_menu` VALUES (8, 33);
INSERT INTO `sys_role_menu` VALUES (8, 34);
INSERT INTO `sys_role_menu` VALUES (8, 35);
INSERT INTO `sys_role_menu` VALUES (8, 36);
INSERT INTO `sys_role_menu` VALUES (9, 37);
INSERT INTO `sys_role_menu` VALUES (10, 1);
INSERT INTO `sys_role_menu` VALUES (10, 2);
INSERT INTO `sys_role_menu` VALUES (10, 7);
INSERT INTO `sys_role_menu` VALUES (10, 35);
INSERT INTO `sys_role_menu` VALUES (11, 1);
INSERT INTO `sys_role_menu` VALUES (11, 2);
INSERT INTO `sys_role_menu` VALUES (11, 4);
INSERT INTO `sys_role_menu` VALUES (11, 5);
INSERT INTO `sys_role_menu` VALUES (11, 6);
INSERT INTO `sys_role_menu` VALUES (11, 9);
INSERT INTO `sys_role_menu` VALUES (11, 10);
INSERT INTO `sys_role_menu` VALUES (11, 11);
INSERT INTO `sys_role_menu` VALUES (11, 14);
INSERT INTO `sys_role_menu` VALUES (11, 15);
INSERT INTO `sys_role_menu` VALUES (11, 24);
INSERT INTO `sys_role_menu` VALUES (11, 27);
INSERT INTO `sys_role_menu` VALUES (11, 35);
INSERT INTO `sys_role_menu` VALUES (11, 36);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `employee_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `force_change_password` tinyint NOT NULL DEFAULT 0,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `gender` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avatar_file_id` bigint NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED',
  `status_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status_effective_until` datetime NULL DEFAULT NULL,
  `user_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'INTERNAL',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT 1,
  `dept_id` bigint NULL DEFAULT NULL,
  `auth_version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_sys_user_tenant_employee_no`(`tenant_id` ASC, `employee_no` ASC) USING BTREE,
  INDEX `idx_sys_user_tenant_status`(`tenant_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_sys_user_avatar`(`tenant_id` ASC, `avatar_file_id` ASC) USING BTREE,
  INDEX `idx_sys_user_directory`(`tenant_id` ASC, `user_type` ASC, `status` ASC, `dept_id` ASC) USING BTREE,
  INDEX `idx_sys_user_lifecycle`(`tenant_id` ASC, `user_type` ASC, `status` ASC, `dept_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', NULL, '{noop}admin123', 0, '演示管理员', NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL, 'INTERNAL', '2026-07-26 19:58:50', 1, NULL, 0);

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
  `user_id` bigint NOT NULL,
  `post_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `primary_flag` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `post_id`) USING BTREE,
  INDEX `idx_sys_user_post_tenant_post`(`tenant_id` ASC, `post_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与岗位关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_post
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);
INSERT INTO `sys_user_role` VALUES (1, 2);

SET FOREIGN_KEY_CHECKS = 1;
