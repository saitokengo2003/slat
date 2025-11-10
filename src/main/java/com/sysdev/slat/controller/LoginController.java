package com.sysdev.slat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

  // ログイン画面を表示する GET リクエスト
  @GetMapping("/login")
  public String getlogin() {
    return "login/index"; // src/main/resources/templates/login/index.html を返す
  }

  // ログインフォームの送信を受け付ける POST リクエスト
  @PostMapping("/login")
  public String postLogin(
      @RequestParam("id") String id,
      @RequestParam("password") String password,
      Model model,
      HttpSession session) {

    boolean isAuthenticated = authenticate(id, password); // 既存の認証ロジック

    if (isAuthenticated) {
      session.setAttribute("loginUser", id); // セッションにユーザー情報を保存
      return "redirect:/"; // templates/index.html を表示するためにルートへリダイレクト
    } else {
      model.addAttribute("loginError", "IDまたはパスワードが間違っています。");
      return "login/index";
    }
  }

  /**
   * 簡易的な認証処理（実際は別サービスに切り出す）
   */
  private boolean authenticate(String id, String password) {
    // ★ 実際の開発では、データベースからユーザー情報を取得し、
    // パスワードのハッシュ値を検証する必要があります。

    // 例: テスト用の資格情報
    if ("test@slat.com".equals(id) && "password123".equals(password)) {
      return true;
    }
    return false;
  }
}
