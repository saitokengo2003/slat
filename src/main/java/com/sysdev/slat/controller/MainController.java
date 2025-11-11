package com.sysdev.slat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

import com.sysdev.slat.user.UserData; // UserDataをインポート

@Controller
public class MainController {

  private final String SESSION_USER_DATA_KEY = "userData"; // LoginControllerとキーを合わせる

  @GetMapping({ "/", "/home" })
  public String index(Model model, HttpSession session) {
    // 1. セッションからUserDataオブジェクトを取得する
    UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);

    // ログインデータが存在すれば、表示名とIDをModelに追加する
    if (userData != null) {
      model.addAttribute("displayName", userData.getDisplayName());
      // 必要であればIDやロールも渡せます
      // model.addAttribute("loginUser", userData.getUserId());
      // model.addAttribute("roleCode", userData.getRoleCode());
    }

    model.addAttribute("title", "トップページ");
    return "index"; // src/main/resources/templates/index.html
  }
}
