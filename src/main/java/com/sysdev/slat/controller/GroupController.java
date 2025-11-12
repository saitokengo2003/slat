package com.sysdev.slat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sysdev.slat.accountadmin.AccountadminEntity;
import com.sysdev.slat.accountadmin.AccountadminRepository;
import com.sysdev.slat.accountadmin.AccountadminService;
import com.sysdev.slat.service.GroupService;

@Controller
public class GroupController {

  @Autowired
  private GroupService groupService;

  @Autowired
  private LoginController loginController;

  @Autowired
  private AccountadminService accountadminService;

  @GetMapping("/groupcreate")
  public String getGroupcreate(Model model) {
    // アクティブユーザー一覧
    model.addAttribute("accounts", accountadminService.findAllActiveAccounts());
    return "groupcreate/index";
  }

  @PostMapping("/groupcreate")
  public String createGroup(
      @RequestParam(name = "name") String name,
      @RequestParam(name = "owner") String owner,
      @RequestParam(name = "members") List<String> members,
      Model model) {

    System.out.println("[Log] /post groupcreate");

    AccountadminEntity entity = accountadminService.getAccountListEntity();
    model.addAttribute("accountadminEntity", entity);

    // 入力チェック
    boolean isValid = groupService.validateName(name);
    if (!isValid) {
      model.addAttribute("errorMessage", "グループ名が不正です");
      return "groupcreate/index";
    }

    // 登録
    boolean ok = groupService.createGroup(owner, name, members);

    if (ok) {
      model.addAttribute("message", "グループを作成しました");
    } else {
      model.addAttribute("errorMessage", "作成に失敗しました。もう一度お試しください");
    }

    return "groupcreate/index";
  }

  @GetMapping("/groupinfo")
  public String getGroupinfo() {
    return "groupinfo/index";
  }
}
