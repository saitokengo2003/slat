package com.sysdev.slat.controller;

import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sysdev.slat.accountadmin.AccountForm;
import com.sysdev.slat.accountadmin.AccountadminEntity;
import com.sysdev.slat.accountadmin.AccountadminService;
import com.sysdev.slat.user.UserData;
import jakarta.servlet.http.HttpSession;

@Controller
public class AccountController {

  private final AccountadminService accountadminService;

  @Autowired
  public AccountController(AccountadminService accountadminService) {
    this.accountadminService = accountadminService;
  }

  private final String SESSION_USER_DATA_KEY = "userData"; // LoginControllerとキーを合わせる

  /**
   * 1. アカウント一覧画面を表示します。
   */
  @GetMapping("/accountadmin")
  public String showAccountList(Model model, HttpSession session) {
    UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);

    // ログインデータが存在すれば、表示名とIDをModelに追加する
    if (userData != null) {
      model.addAttribute("displayName", userData.getDisplayName());
      if ("admin".equals(userData.getRoleCode())) {
        AccountadminEntity entity = accountadminService.getAccountListEntity();
        model.addAttribute("accountadminEntity", entity);
        return "accountadmin/index";
      }
    }
    return "redirect:/";
  }

  /**
   * 2. アカウント作成画面表示 (GET)
   */
  @GetMapping("/accountcreate")
  public String getAccountcreate(Model model, HttpSession session) {
    UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);
    if (userData != null) {
      model.addAttribute("displayName", userData.getDisplayName());
    }
    model.addAttribute("accountForm", new AccountForm());
    return "accountcreate/index";
  }

  /**
   * 3. アカウント作成処理 (POST)
   */
  @PostMapping("/accountcreate")
  public String createAccount(
      @ModelAttribute AccountForm accountForm,
      RedirectAttributes redirectAttributes) {

    try {
      accountadminService.createAccount(accountForm);
      redirectAttributes.addFlashAttribute("message", "新しいアカウントを正常に作成しました。");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "アカウント作成に失敗しました: " + e.getLocalizedMessage());
    }

    return "redirect:/accountadmin";
  }

  /**
   * 4. アカウント削除処理 (POST)
   */
  @PostMapping("/accountadmin/delete")
  public String deleteAccount(
      @RequestParam("id") String id,
      RedirectAttributes redirectAttributes) {
    try {
      accountadminService.deleteAccount(id);
      redirectAttributes.addFlashAttribute("message",
          "アカウント (ID: " + id + ") を正常に削除しました。");
    } catch (SQLException e) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "アカウント削除に失敗しました: " + e.getLocalizedMessage());
    }

    return "redirect:/accountadmin";
  }

  /**
   * 5. アカウント編集画面表示 (GET)
   */
  @GetMapping("/accountedit")
  public String getAccountEdit(@RequestParam("id") String id, Model model) {
    AccountForm accountForm = accountadminService.getAccountById(id);
    model.addAttribute("accountForm", accountForm);
    return "accountedit/index";
  }

  @PostMapping("/accountedit")
  public String editAccount(
      @ModelAttribute AccountForm accountForm,
      RedirectAttributes redirectAttributes) {

    System.out.println("edit呼び出し");
    try {
      String uuid = accountForm.getId();

      accountadminService.updateAccount(uuid, accountForm);

      redirectAttributes.addFlashAttribute("message", "アカウントを正常に更新しました。");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "アカウント更新に失敗しました: " + e.getLocalizedMessage());
    }

    return "redirect:/accountadmin";
  }
}
