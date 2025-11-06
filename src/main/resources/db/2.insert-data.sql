-- 💡 注意: 実際の環境で実行する際は、UUIDを扱うRDBMSの環境やツールに応じて、
--          以下の変数設定とINSERTの記述方法を適宜調整してください。
--          (例: DBeaver, pgAdminなどのSQLクライアントで実行することを想定)
-- -----------------------------------------------------
-- 1. roles (ロール定義)
-- -----------------------------------------------------
-- ロールIDを保持するための変数 (または一時テーブル)
SELECT
  gen_random_uuid () INTO public.role_admin_id;

SELECT
  gen_random_uuid () INTO public.role_teacher_id;

SELECT
  gen_random_uuid () INTO public.role_student_id;

INSERT INTO
  roles (id, code, display_name)
VALUES
  (public.role_admin_id, 'admin', '管理者'),
  (public.role_teacher_id, 'teacher', '講師'),
  (public.role_student_id, 'student', '学生');

-- -----------------------------------------------------
-- 2. users (ユーザ)
-- -----------------------------------------------------
-- ユーザーIDを保持するための変数
SELECT
  gen_random_uuid () INTO public.user_admin_id;

SELECT
  gen_random_uuid () INTO public.user_teacher_a_id;

SELECT
  gen_random_uuid () INTO public.user_student_a_id;

SELECT
  gen_random_uuid () INTO public.user_student_b_id;

INSERT INTO
  users (
    id,
    username,
    password_hash,
    display_name,
    role_id,
    grade,
    CLASS,
    number,
    status
  )
VALUES
  (
    public.user_admin_id,
    'admin_user',
    'hashed_password_admin',
    'システム管理者',
    public.role_admin_id,
    NULL,
    NULL,
    NULL,
    'active'
  ),
  (
    public.user_teacher_a_id,
    'teacher_a',
    'hashed_password_teacher_a',
    '山田 太郎 (講師)',
    public.role_teacher_id,
    NULL,
    'R科担当',
    NULL,
    'active'
  ),
  (
    public.user_student_a_id,
    'student_a',
    'hashed_password_student_a',
    '佐藤 花子',
    public.role_student_id,
    3,
    'R科',
    5,
    'active'
  ),
  (
    public.user_student_b_id,
    'student_b',
    'hashed_password_student_b',
    '田中 一郎',
    public.role_student_id,
    3,
    'R科',
    18,
    'active'
  );

-- -----------------------------------------------------
-- 3. groups (グループ/チャンネル)
-- -----------------------------------------------------
-- グループIDを保持するための変数
SELECT
  gen_random_uuid () INTO public.group_class_a_id;

SELECT
  gen_random_uuid () INTO public.group_dm_atob_id;

INSERT INTO
  GROUPS (
    id,
    NAME,
    TYPE,
    created_by
  )
VALUES
  (
    public.group_class_a_id,
    'R科 連絡チャンネル',
    'class',
    public.user_teacher_a_id
  ), -- 講師が作成したクラスグループ
  (
    public.group_dm_atob_id,
    '佐藤-田中 DM',
    'dm',
    public.user_student_a_id
  );

-- 学生Aが作成したDM
-- -----------------------------------------------------
-- 4. group_members (グループ所属)
-- -----------------------------------------------------
INSERT INTO
  group_members (group_id, user_id, role_in_group)
VALUES
  -- R科 連絡チャンネル
  (
    public.group_class_a_id,
    public.user_teacher_a_id,
    'owner'
  ), -- 講師A: オーナー
  (
    public.group_class_a_id,
    public.user_student_a_id,
    'member'
  ), -- 学生A: メンバー
  (
    public.group_class_a_id,
    public.user_student_b_id,
    'member'
  ), -- 学生B: メンバー
  -- 佐藤-田中 DM
  (
    public.group_dm_atob_id,
    public.user_student_a_id,
    'owner'
  ), -- 学生A: オーナー
  (
    public.group_dm_atob_id,
    public.user_student_b_id,
    'member'
  );

-- 学生B: メンバー
-- -----------------------------------------------------
-- 5. messages (メッセージ)
-- -----------------------------------------------------
-- メッセージIDを保持するための変数
SELECT
  gen_random_uuid () INTO public.message_a_id;

SELECT
  gen_random_uuid () INTO public.message_b_id;

SELECT
  gen_random_uuid () INTO public.message_c_id;

INSERT INTO
  messages (
    id,
    group_id,
    sender_id,
    body,
    reaction_deadline_at,
    deadline_status
  )
VALUES
  -- メッセージA: クラス連絡 (リアクション期限なし)
  (
    public.message_a_id,
    public.group_class_a_id,
    public.user_teacher_a_id,
    '【重要】来週のテストは学生証が必要です。',
    NULL,
    'open'
  ),
  -- メッセージB: クラス連絡 (リアクション期限あり)
  (
    public.message_b_id,
    public.group_class_a_id,
    public.user_teacher_a_id,
    'これを見た人は「確認」リアクションを付けてください。',
    (CURRENT_TIMESTAMP + INTERVAL '1 day'),
    'open'
  ),
  -- メッセージC: DM (リアクション期限なし)
  (
    public.message_c_id,
    public.group_dm_atob_id,
    public.user_student_a_id,
    'こんにちは',
    NULL,
    'open'
  );

-- -----------------------------------------------------
-- 6. reactions (リアクション)
-- -----------------------------------------------------
INSERT INTO
  reactions (message_id, user_id, emoji)
VALUES
  -- メッセージA (重要連絡)へのリアクション
  (
    public.message_a_id,
    public.user_student_a_id,
    '👍'
  ), -- 学生Aが「いいね」
  -- メッセージB (期限付きリアクション)へのリアクション
  (
    public.message_b_id,
    public.user_student_b_id,
    '✅'
  ), -- 学生Bが「確認」
  -- メッセージC (DM)へのリアクション
  (
    public.message_c_id,
    public.user_student_b_id,
    '🤔'
  );

-- 学生Bが「考える」
