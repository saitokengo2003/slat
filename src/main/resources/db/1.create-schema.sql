-- 拡張機能: UUID生成 (PostgreSQLの場合)
-- 必要に応じて実行してください。
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- -----------------------------------------------------
-- Table roles (ロール定義)
-- -----------------------------------------------------
CREATE TABLE
  roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (), -- 主キー
    code VARCHAR(50) UNIQUE NOT NULL, -- unique: student, teacher, admin
    display_name VARCHAR(100) NOT NULL -- 表示用ロール名
  );

-- -----------------------------------------------------
-- Table users (ユーザ)
-- -----------------------------------------------------
CREATE TABLE
  users_s (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (), -- 主キー, ログイン用
    username VARCHAR(100) UNIQUE, -- unique: ログインに使うID
    password_hash TEXT NOT NULL, -- ハッシュ化されたパスワード
    status VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'deleted')), -- active/suspended/deleted
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 作成日
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 更新日
    last_login_at TIMESTAMP WITH TIME ZONE, -- 最終ログイン日
    display_name VARCHAR(100) NOT NULL, -- 表示名
    role_code VARCHAR(50) NOT NULL, -- FK roles.code
    grade INTEGER, -- 学年
    class_name VARCHAR(50), -- クラス
    number INTEGER, -- 番号
    CONSTRAINT fk_user_role FOREIGN KEY (role_code) REFERENCES roles (code)
  );

-- updated_at 自動更新のためのトリガー関数
CREATE
OR REPLACE FUNCTION update_updated_at_column () RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER update_user_updated_at BEFORE
UPDATE ON users_s FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column ();

-- -----------------------------------------------------
-- Table groups (グループ/チャンネル)
-- -----------------------------------------------------
CREATE TABLE
  group_s (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (), -- 主キー
    NAME VARCHAR(255) NOT NULL, -- 検索・表示名
    TYPE VARCHAR(50) NOT NULL CHECK (
      TYPE IN ('dm', 'group', 'class')
    ), -- dm/group/class など用途別
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 作成日
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 更新日
    created_by VARCHAR(100) NOT NULL, -- FK users_s.id: 作成者
    CONSTRAINT fk_group_creator FOREIGN KEY (created_by) REFERENCES users_s (username)
  );

CREATE TRIGGER update_group_updated_at BEFORE
UPDATE ON group_s FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column ();

-- -----------------------------------------------------
-- Table group_members (グループ所属)
-- -----------------------------------------------------
CREATE TABLE
  group_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (), -- 主キー
    group_id UUID NOT NULL, -- FK groups.id
    user_id VARCHAR(100) NOT NULL, -- FK users_s.id
    role_in_group VARCHAR(20) NOT NULL DEFAULT 'member' CHECK (role_in_group IN ('owner', 'moderator', 'member')), -- owner/moderator/member
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 参加日時
    left_at TIMESTAMP WITH TIME ZONE, -- 脱退日時
    CONSTRAINT unique_group_user UNIQUE (group_id, user_id),
    CONSTRAINT fk_member_group FOREIGN KEY (group_id) REFERENCES group_s (id) ON DELETE CASCADE, -- グループ削除でメンバー情報も削除
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES users_s (username)
  );

-- -----------------------------------------------------
-- Table messages (メッセージ)
-- -----------------------------------------------------
CREATE TABLE
  messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (), -- 主キー
    group_id UUID NOT NULL, -- FK groups.id
    sender_id VARCHAR(100) NOT NULL, -- FK users_s.id: 送信者
    body TEXT NOT NULL, -- 本文
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 作成日
    -- リアクション期限機能
    reaction_deadline_at TIMESTAMP WITH TIME ZONE, -- 期限日時
    deadline_scope VARCHAR(50), -- 誰に適用
    deadline_status VARCHAR(20) NOT NULL DEFAULT 'open' CHECK (
      deadline_status IN ('open', 'closed', 'evaluated')
    ), -- open/closed/evaluated
    CONSTRAINT fk_message_group FOREIGN KEY (group_id) REFERENCES group_s (id) ON DELETE CASCADE, -- グループ削除でメッセージも削除
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users_s (username)
  );

-- -----------------------------------------------------
-- Table reactions (リアクション)
-- -----------------------------------------------------
CREATE TABLE
  reactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (), -- 主キー
    message_id UUID NOT NULL, -- FK messages.id
    user_id VARCHAR(100) NOT NULL, -- FK users_s.id
    emoji VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 作成日
    CONSTRAINT unique_reaction UNIQUE (message_id, user_id, emoji),
    CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE, -- メッセージ削除でリアクションも削除
    CONSTRAINT fk_reaction_user FOREIGN KEY (user_id) REFERENCES users_s (username)
  );
