/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80015
 Source Host           : localhost:3306
 Source Schema         : alone

 Target Server Type    : MySQL
 Target Server Version : 80015
 File Encoding         : 65001

 Date: 07/02/2021 17:14:06
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for alloc
-- ----------------------------
DROP TABLE IF EXISTS `alloc`;
CREATE TABLE `alloc`  (
  `biz_key` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '业务名称',
  `max_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '当前最大递增值',
  `step` int(11) NOT NULL COMMENT '递增步伐',
  `description` varchar(256) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '描述',
  `update_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`biz_key`) USING BTREE,
  INDEX `idx_biz_tag`(`biz_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '分配表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
