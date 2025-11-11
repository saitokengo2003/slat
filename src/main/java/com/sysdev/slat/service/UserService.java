package com.sysdev.slat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  // データベースアクセスを行うためのUserRepositoryを注入（実際の実装が必要）
  // @Autowired
  // private UserRepository userRepository;

  /**
   * ユーザーID（username）とパスワードで認証を行う。
   * 【暫定実装】2.insert-data.sqlのデータに基づいてプレーンテキスト比較
   */
  public boolean authenticate(String username, String password) {
    // **暫定的な認証ロジック (DB未実装の場合)**:
    if (("admin_user".equals(username) || "teacher_a".equals(username) || "student_a".equals(username)
        || "student_b".equals(username))
        && "pass".equals(password)) {
      return true;
    }
    return false;
  }

  /**
   * username (ID) に対応する display_name (表示名) を取得する
   * 【暫定実装】2.insert-data.sqlのデータに基づいて静的な値を返す
   *
   * @param username users_s.username
   * @return ユーザーの display_name、見つからない場合は null
   */
  public String getDisplayNameByUsername(String username) {
    // 実際は userRepository.findByUsername(username).getDisplayName() のように実装
    return switch (username) {
      case "admin_user" -> "システム管理者";
      case "teacher_a" -> "山田 太郎 (講師)";
      case "student_a" -> "佐藤 花子";
      case "student_b" -> "山本 由伸";
      default -> null;
    };
  }
}
