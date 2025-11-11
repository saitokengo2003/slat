package com.sysdev.slat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sysdev.slat.service.UserService;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

  @Autowired
  private UserService userService;

  // ログイン画面を表示する GET リクエスト
  @GetMapping("/login")
  public String getlogin() {
    return "login/index"; // テンプレート名（例: login/index.html）
  }

  // ログインフォームの送信を受け付ける POST リクエスト
  @PostMapping("/login")
  public String postLogin(
      @RequestParam("id") String id,
      @RequestParam("password") String password,
      Model model,
      HttpSession session) {

    // IDをusers_sテーブルのusernameとして認証を実行
    // 【重要】認証ロジックはUserServiceの実装に依存します
    boolean isAuthenticated = userService.authenticate(id, password);

    if (isAuthenticated) {
      // 認証成功時: セッションにユーザー情報（ここではID/username）を保存
      session.setAttribute("loginUser", id);
      return "redirect:/"; // 認証後のトップページなどにリダイレクト
    } else {
      // 認証失敗時: エラーメッセージをモデルに追加し、ログイン画面に戻る
      model.addAttribute("loginError", "IDまたはパスワードが間違っています。");
      return "login/index";
    }
  }

  // ログアウト処理を行う POST リクエストを追加
  @PostMapping("/logout")
  public String postLogout(HttpSession session) {
    // セッションを無効化し、セッションに保存されたすべての属性をクリアする
    session.invalidate();

    // ログアウト後、ログインページにリダイレクト
    return "redirect:/login";
  }
}
