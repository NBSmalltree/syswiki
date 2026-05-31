-- =============================================
-- 系统百科平台 - Oracle 建表脚本
-- 执行方式: sqlplus syswiki/password@//host:1521/orcl @init_oracle.sql
-- =============================================

-- 清理旧对象（首次部署可跳过此段，重复部署时取消注释）
-- BEGIN
--   FOR t IN (SELECT table_name FROM user_tables WHERE table_name LIKE 'SYS_%') LOOP
--     EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS';
--   END LOOP;
--   FOR s IN (SELECT sequence_name FROM user_sequences WHERE sequence_name LIKE 'SEQ_%') LOOP
--     EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
--   END LOOP;
-- END;
-- /

-- =============================================
-- 1. 用户账号表
-- =============================================
CREATE TABLE sys_user (
    user_id         VARCHAR2(32)    NOT NULL,
    username        VARCHAR2(64)    NOT NULL,
    password        VARCHAR2(128)   NOT NULL,
    nickname        VARCHAR2(64),
    role            VARCHAR2(16)    DEFAULT 'VIEWER' NOT NULL,
    status          VARCHAR2(16)    DEFAULT 'ACTIVE' NOT NULL,
    create_time     TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    update_time     TIMESTAMP,
    CONSTRAINT pk_sys_user PRIMARY KEY (user_id),
    CONSTRAINT uk_user_username UNIQUE (username)
);

COMMENT ON TABLE sys_user IS '用户账号表';
COMMENT ON COLUMN sys_user.user_id IS '用户唯一编号';
COMMENT ON COLUMN sys_user.username IS '登录用户名';
COMMENT ON COLUMN sys_user.password IS '密码（BCrypt加密）';
COMMENT ON COLUMN sys_user.nickname IS '昵称/显示名';
COMMENT ON COLUMN sys_user.role IS '全局角色：ADMIN/EDITOR/VIEWER';
COMMENT ON COLUMN sys_user.status IS '状态：ACTIVE/DISABLED';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';

-- 预置管理员（密码由应用启动时自动修正）
INSERT INTO sys_user (user_id, username, password, nickname, role, status)
VALUES ('U00000000000001', 'admin', 'placeholder', '系统管理员', 'ADMIN', 'ACTIVE');
COMMIT;

-- =============================================
-- 2. 系统成员关系表
-- =============================================
CREATE TABLE sys_system_member (
    id              VARCHAR2(32)    NOT NULL,
    system_id       VARCHAR2(32)    NOT NULL,
    user_id         VARCHAR2(32)    NOT NULL,
    role            VARCHAR2(16)    DEFAULT 'ADMIN' NOT NULL,
    create_time     TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_system_member PRIMARY KEY (id),
    CONSTRAINT uk_system_user UNIQUE (system_id, user_id)
);

CREATE INDEX idx_member_user ON sys_system_member(user_id);

COMMENT ON TABLE sys_system_member IS '系统成员关系表';
COMMENT ON COLUMN sys_system_member.id IS '主键';
COMMENT ON COLUMN sys_system_member.system_id IS '系统编号';
COMMENT ON COLUMN sys_system_member.user_id IS '用户编号';
COMMENT ON COLUMN sys_system_member.role IS '系统角色：OWNER/ADMIN';

-- =============================================
-- 3. 登录日志表
-- =============================================
CREATE TABLE sys_login_log (
    log_id          VARCHAR2(32)    NOT NULL,
    username        VARCHAR2(64)    NOT NULL,
    login_ip        VARCHAR2(64),
    status          VARCHAR2(16)    NOT NULL,
    message         VARCHAR2(256),
    create_time     TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_login_log PRIMARY KEY (log_id)
);

CREATE INDEX idx_login_user ON sys_login_log(username);
CREATE INDEX idx_login_time ON sys_login_log(create_time);

COMMENT ON TABLE sys_login_log IS '登录日志表';
COMMENT ON COLUMN sys_login_log.log_id IS '日志编号';
COMMENT ON COLUMN sys_login_log.username IS '登录用户名';
COMMENT ON COLUMN sys_login_log.login_ip IS '登录IP';
COMMENT ON COLUMN sys_login_log.status IS '成功/失败';
COMMENT ON COLUMN sys_login_log.message IS '备注信息';
COMMENT ON COLUMN sys_login_log.create_time IS '登录时间';

-- =============================================
-- 4. 系统空间主表
-- =============================================
CREATE TABLE sys_ency_space (
    system_id       VARCHAR2(32)    NOT NULL,
    system_name     VARCHAR2(128)   NOT NULL,
    system_code     VARCHAR2(32)    NOT NULL,
    owner           VARCHAR2(64)    NOT NULL,
    description     VARCHAR2(512),
    status          VARCHAR2(16)    DEFAULT 'ACTIVE' NOT NULL,
    create_time     TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    update_time     TIMESTAMP,
    CONSTRAINT pk_ency_space PRIMARY KEY (system_id),
    CONSTRAINT uk_space_code UNIQUE (system_code)
);

CREATE INDEX idx_space_owner ON sys_ency_space(owner);

COMMENT ON TABLE sys_ency_space IS '系统百科-空间主表';
COMMENT ON COLUMN sys_ency_space.system_id IS '系统唯一编号';
COMMENT ON COLUMN sys_ency_space.system_name IS '系统名称';
COMMENT ON COLUMN sys_ency_space.system_code IS '系统代号（唯一）';
COMMENT ON COLUMN sys_ency_space.owner IS '系统负责人（username）';
COMMENT ON COLUMN sys_ency_space.description IS '系统简要描述';
COMMENT ON COLUMN sys_ency_space.status IS '状态：ACTIVE/DISABLED';

-- =============================================
-- 5. 百科内容明细表
-- =============================================
CREATE TABLE sys_ency_content (
    content_id      VARCHAR2(32)    NOT NULL,
    system_id       VARCHAR2(32)    NOT NULL,
    module_type     VARCHAR2(32)    NOT NULL,
    md_content      CLOB,
    version         NUMBER(8)       DEFAULT 1 NOT NULL,
    operator        VARCHAR2(64),
    create_time     TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    update_time     TIMESTAMP,
    CONSTRAINT pk_ency_content PRIMARY KEY (content_id),
    CONSTRAINT uk_content_module UNIQUE (system_id, module_type),
    CONSTRAINT fk_content_space FOREIGN KEY (system_id) REFERENCES sys_ency_space(system_id)
);

CREATE INDEX idx_content_system ON sys_ency_content(system_id);
CREATE INDEX idx_content_module ON sys_ency_content(module_type);

COMMENT ON TABLE sys_ency_content IS '系统百科-内容明细表';
COMMENT ON COLUMN sys_ency_content.content_id IS '内容唯一编号';
COMMENT ON COLUMN sys_ency_content.system_id IS '外键-系统编号';
COMMENT ON COLUMN sys_ency_content.module_type IS '模块类型';
COMMENT ON COLUMN sys_ency_content.md_content IS 'Markdown源码内容';
COMMENT ON COLUMN sys_ency_content.version IS '版本号';
COMMENT ON COLUMN sys_ency_content.operator IS '最近操作人';

-- =============================================
-- 6. 内容版本历史表
-- =============================================
CREATE TABLE sys_ency_content_version (
    version_id      VARCHAR2(32)    NOT NULL,
    content_id      VARCHAR2(32)    NOT NULL,
    system_id       VARCHAR2(32)    NOT NULL,
    module_type     VARCHAR2(32)    NOT NULL,
    version         NUMBER(8)       NOT NULL,
    md_content      CLOB,
    operator        VARCHAR2(64),
    create_time     TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_content_version PRIMARY KEY (version_id)
);

CREATE INDEX idx_version_content ON sys_ency_content_version(content_id);
CREATE INDEX idx_version_system ON sys_ency_content_version(system_id);

COMMENT ON TABLE sys_ency_content_version IS '系统百科-内容版本历史表';

-- =============================================
-- 7. 拓扑链路配置表
-- =============================================
CREATE TABLE sys_ency_topology (
    link_id             VARCHAR2(32)    NOT NULL,
    system_id           VARCHAR2(32)    NOT NULL,
    from_node           VARCHAR2(64)    NOT NULL,
    to_node             VARCHAR2(64)    NOT NULL,
    protocol            VARCHAR2(32),
    interface_name      VARCHAR2(128),
    interface_details   CLOB,
    sort_order          NUMBER(4)       DEFAULT 0,
    create_time         TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    update_time         TIMESTAMP,
    CONSTRAINT pk_ency_topology PRIMARY KEY (link_id),
    CONSTRAINT fk_topo_space FOREIGN KEY (system_id) REFERENCES sys_ency_space(system_id)
);

CREATE INDEX idx_topo_system ON sys_ency_topology(system_id);

COMMENT ON TABLE sys_ency_topology IS '系统百科-拓扑链路配置表';

-- =============================================
-- 8. 运维SQL库表
-- =============================================
CREATE TABLE sys_ency_sql_lib (
    sql_id          VARCHAR2(32)    NOT NULL,
    system_id       VARCHAR2(32)    NOT NULL,
    title           VARCHAR2(128)   NOT NULL,
    category        VARCHAR2(32)    NOT NULL,
    sql_template    CLOB            NOT NULL,
    description     VARCHAR2(512),
    params_json     CLOB,
    sort_order      NUMBER(4)       DEFAULT 0,
    operator        VARCHAR2(64),
    create_time     TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    update_time     TIMESTAMP,
    CONSTRAINT pk_ency_sql_lib PRIMARY KEY (sql_id),
    CONSTRAINT fk_sql_space FOREIGN KEY (system_id) REFERENCES sys_ency_space(system_id)
);

CREATE INDEX idx_sql_system ON sys_ency_sql_lib(system_id);
CREATE INDEX idx_sql_category ON sys_ency_sql_lib(system_id, category);

COMMENT ON TABLE sys_ency_sql_lib IS '系统百科-运维SQL与指令库';

-- =============================================
-- 9. 示例数据
-- =============================================
INSERT INTO sys_ency_space (system_id, system_name, system_code, owner, description)
VALUES ('SP20260530000001', '示例核心系统', 'DEMO', 'admin', '系统百科平台示例空间');

INSERT INTO sys_system_member (id, system_id, user_id, role)
VALUES ('SM20260530000001', 'SP20260530000001', 'U00000000000001', 'OWNER');

INSERT INTO sys_ency_content (content_id, system_id, module_type, md_content, version, operator)
VALUES ('CT20260530000001', 'SP20260530000001', 'INTRO',
'# 系统简介

本系统是系统百科平台的示例核心系统，用于演示平台各项功能。

## 技术栈

- Java 11
- Spring Boot 2.7
- Oracle 11g
- Vue 3
- ECharts 5.x',
1, 'admin');

COMMIT;

EXIT;
