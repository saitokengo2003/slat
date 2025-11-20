package com.sysdev.slat.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.sysdev.slat.user.UserData;

@Controller
public class AdminController {

  private final String SESSION_USER_DATA_KEY = "userData"; // LoginControllerとキーを合わせる

  @GetMapping("/admin")
  public String getAdmin(Model model, HttpSession session) {
    UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);
    System.out.println("[Log]" + userData);

    if (userData != null) {
      model.addAttribute("displayName", userData.getDisplayName());
      if ("admin".equals(userData.getRoleCode())) {
        return "admin/index";
      }
    }
    return "redirect:/";
  }
}
