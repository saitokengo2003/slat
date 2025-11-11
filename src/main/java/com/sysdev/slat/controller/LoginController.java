package com.sysdev.slat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sysdev.slat.service.UserService;
import com.sysdev.slat.service.UserData; // UserDataをインポート

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

  // セッションに保存するUserDataのキー
  private final String SESSION_USER_DATA_KEY = "userData";

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

    // UserServiceを呼び出して認証を実行し、UserDataを取得
    UserData userData = userService.authenticate(id, password);

    if (userData != null) {
      // 認証成功時: セッションにUserDataオブジェクト全体を保存
      session.setAttribute(SESSION_USER_DATA_KEY, userData);
      return "redirect:/"; // 認証後のトップページなどにリダイレクト
    } else {
      // 認証失敗時
      model.addAttribute("loginError", "IDまたはパスワードが間違っています。");
      return "login/index";
    }
  }

  // ログアウト処理を行う POST リクエスト
  @PostMapping("/logout")
  public String postLogout(HttpSession session) {
    // セッションを無効化し、セッションに保存されたすべての属性をクリアする
    session.invalidate();
    return "redirect:/login";
  }
}
