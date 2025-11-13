package com.sysdev.slat.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.sysdev.slat.GroupDetailDto;
import com.sysdev.slat.accountadmin.AccountadminEntity;
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

    return "groupinfo/index";
  }

  @GetMapping("/groupinfo")
  public String groupinfoList(Model model) {
    model.addAttribute("groups", groupService.findAllGroupsWithCounts());
    return "groupinfo/index";
  }

  @GetMapping("/groupinfo/{groupId}")
  public String groupinfoDetail(@PathVariable("groupId") UUID groupId, Model model) {
    GroupDetailDto detail = groupService.getGroupDetail(groupId);
    if (detail == null) {
      model.addAttribute("errorMessage", "指定されたグループが見つかりません。");
      model.addAttribute("groups", groupService.findAllGroupsWithCounts());
      return "groupinfo/index";
    }
    model.addAttribute("group", detail.getGroup()); // Map<String,Object>
    model.addAttribute("members", detail.getMembers()); // List<Map<String,Object>>
    return "groupinfo/index";
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String handleBadId(Model model) {
    model.addAttribute("errorMessage", "グループIDの形式が不正です。");
    model.addAttribute("groups", groupService.findAllGroupsWithCounts());
    return "groupinfo/index";
  }

  @PostMapping("/group/{groupId}/delete")
  public String deleteGroup(@PathVariable("groupId") UUID groupId, Model model) {
    boolean ok = groupService.deleteGroup(groupId);

    if (ok) {
      model.addAttribute("message", "グループを削除しました。");
    } else {
      model.addAttribute("errorMessage", "グループ削除に失敗しました。");
    }
    model.addAttribute("groups", groupService.findAllGroupsWithCounts());
    return "groupinfo/index";
  }

  @PostMapping("/group/{groupId}/member/{userId}/delete")
  public String deleteGroupMember(
      @PathVariable("groupId") UUID groupId,
      @PathVariable("userId") String userId,
      Model model) {

    boolean ok = groupService.deleteGroupMember(groupId, userId);

    if (ok) {
      model.addAttribute("message", "メンバーを削除しました。");
    } else {
      model.addAttribute("errorMessage", "メンバー削除に失敗しました。（オーナーは削除できません）");
    }

    // 削除後も同じグループ詳細画面に戻る
    GroupDetailDto detail = groupService.getGroupDetail(groupId);
    if (detail == null) {
      // グループ自体が消えていた場合など
      model.addAttribute("errorMessage", "指定されたグループが見つかりません。");
      model.addAttribute("groups", groupService.findAllGroupsWithCounts());
      return "groupinfo/index";
    }

    model.addAttribute("group", detail.getGroup());
    model.addAttribute("members", detail.getMembers());
    return "groupinfo/index";
  }

}
