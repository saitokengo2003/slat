package com.sysdev.slat.service;

import org.springframework.stereotype.Service;

@Service
public class UserService {

  /**
   * ユーザーID（username）とパスワードで認証を行い、成功時はUserDataを返す。
   * 【暫定実装】2.insert-data.sqlのデータに基づいて静的な値を返す
   */
  public UserData authenticate(String username, String password) {

    // 暫定的な認証ロジック: パスワードは 'pass' のみ有効
    if ("pass".equals(password)) {
      UserData userData = getMockUserData(username);
      // ユーザーデータが存在する場合のみ認証成功とみなす
      if (userData != null) {
        return userData;
      }
    }

    // 認証失敗
    return null;
  }

  /**
   * username (ID) に対応する users_sテーブルの指定されたデータをモックとして返す。
   * 実際はデータベースから取得する処理に置き換える必要があります。
   */
  private UserData getMockUserData(String username) {
    UserData data = new UserData();

    // 2.insert-data.sql のデータに基づいて、必要な6項目を設定
    switch (username) {
      case "admin_user":
        data.setUserId("admin_user");
        data.setDisplayName("システム管理者");
        data.setRoleCode("admin");
        data.setGrade(null); // NULL
        data.setClassName(null); // NULL
        data.setNumber(null); // NULL
        break;
      case "teacher_a":
        data.setUserId("teacher_a");
        data.setDisplayName("山田 太郎 (講師)");
        data.setRoleCode("teacher");
        data.setGrade(null); // NULL
        data.setClassName("R科担当");
        data.setNumber(null); // NULL
        break;
      case "student_a":
        data.setUserId("student_a");
        data.setDisplayName("佐藤 花子");
        data.setRoleCode("student");
        data.setGrade(3);
        data.setClassName("R科");
        data.setNumber(5);
        break;
      case "student_b":
        data.setUserId("student_b");
        data.setDisplayName("山本 由伸");
        data.setRoleCode("student");
        data.setGrade(3);
        data.setClassName("R科");
        data.setNumber(5);
        break;
      default:
        return null;
    }
    return data;
  }
}
