package com.sysdev.slat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {

  @GetMapping({ "/", "/home" })
  public String index(Model model, HttpSession session) {
    Object loginUser = session.getAttribute("loginUser");
    model.addAttribute("title", "トップページ");
    model.addAttribute("loginUser", loginUser);
    return "index"; // src/main/resources/templates/index.html
  }
}
