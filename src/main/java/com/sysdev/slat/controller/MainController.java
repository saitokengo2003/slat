package com.sysdev.slat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

import com.sysdev.slat.chat.GroupRepository;
import com.sysdev.slat.user.UserData;
import com.sysdev.slat.user.UserService;

@Controller
public class MainController {

  @Autowired
  private UserService userService;

  @Autowired
  private GroupRepository groupRepository;

  private final String SESSION_USER_DATA_KEY = "userData";

  @GetMapping({ "/", "/home" })
  public String index(Model model, HttpSession session) {
    UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);

    if (userData != null) {
      model.addAttribute("loggedInUserId", userData.getUserId());
      model.addAttribute("displayName", userData.getDisplayName());
      model.addAttribute("role", userData.getRoleCode());
      // 必要であればIDやロールも渡せます
      // model.addAttribute("loginUser", userData.getUserId());
      // model.addAttribute("roleCode", userData.getRoleCode());

      List<UserData> otherUsers = userService.findAllOtherUsers(userData.getUserId());
      model.addAttribute("otherUsers", otherUsers);

      List<com.sysdev.slat.chat.Group> generalGroups = groupRepository.findJoinedGroupsByUserId(userData.getUserId());
      model.addAttribute("generalGroups", generalGroups);
    }

    model.addAttribute("title", "トップページ");
    return "index";
  }
}
