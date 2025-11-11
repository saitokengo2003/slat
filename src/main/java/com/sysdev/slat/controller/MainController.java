package com.sysdev.slat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import com.sysdev.slat.service.UserService;

@Controller
public class MainController {

  @Autowired
  private UserService userService; // UserServiceを注入

  @GetMapping({ "/", "/home" })
  public String index(Model model, HttpSession session) {
    // 1. セッションからログインID（username）を取得する
    Object loginUserObj = session.getAttribute("loginUser");

    // ログインIDがセッションにあれば処理を行う
    if (loginUserObj instanceof String loginUser) {
      // 2. UserServiceを使ってIDから表示名を取得する
      String displayName = userService.getDisplayNameByUsername(loginUser);

      // 3. ModelにIDと表示名を両方追加する
      model.addAttribute("loginUser", loginUser); // ID (username)
      model.addAttribute("displayName", displayName); // 表示名 (display_name)
    }

    model.addAttribute("title", "トップページ");
    return "index"; // src/main/resources/templates/index.html
  }
}
