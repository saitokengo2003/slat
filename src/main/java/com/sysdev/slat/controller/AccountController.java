package com.sysdev.slat.controller;

import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sysdev.slat.accountadmin.AccountForm;
import com.sysdev.slat.accountadmin.AccountadminEntity;
import com.sysdev.slat.accountadmin.AccountadminService;

@Controller
@RequestMapping("/") // ルートマッピングをクラス外に移すか、/accountadmin にリダイレクトさせる必要があります
public class AccountController {

  private final AccountadminService accountadminService;

  @Autowired
  public AccountController(AccountadminService accountadminService) {
    this.accountadminService = accountadminService;
  }

  // -----------------------------------------------------------------
  // 1. アカウント一覧表示 (Read)
  // URL: /accountadmin
  // -----------------------------------------------------------------
  @GetMapping("/accountadmin")
  public String showAccountList(Model model) {
    AccountadminEntity entity = accountadminService.getAccountListEntity();
    model.addAttribute("accountadminEntity", entity);
    return "accountadmin/index";
  }

  // -----------------------------------------------------------------
  // 2. アカウント作成画面表示 (New GET)
  // URL: /accountcreate
  // -----------------------------------------------------------------
  @GetMapping("/accountcreate")
  public String getAccountcreate(Model model) {
    model.addAttribute("accountForm", new AccountForm());
    return "accountcreate/index";
  }

  // -----------------------------------------------------------------
  // 3. アカウント作成処理 (New POST)
  // URL: /accountcreate
  // -----------------------------------------------------------------
  @PostMapping("/accountcreate")
  public String createAccount(
      @ModelAttribute AccountForm accountForm,
      RedirectAttributes redirectAttributes) {

    try {
      // 💡 ServiceのcreateAccountに処理を委譲
      accountadminService.createAccount(accountForm);
      redirectAttributes.addFlashAttribute("message", "新しいアカウントを正常に作成しました。");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "アカウント作成に失敗しました: " + e.getLocalizedMessage());
    }

    return "redirect:/accountadmin"; // 一覧へリダイレクト
  }

  // -----------------------------------------------------------------
  // 4. アカウント削除処理 (Delete POST)
  // URL: /accountadmin/delete
  // -----------------------------------------------------------------
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

  // -----------------------------------------------------------------
  // 5. アカウント編集画面表示 (Edit GET)
  // URL: /accountedit?id={id}
  // -----------------------------------------------------------------
  @GetMapping("/accountedit")
  public String getAccountEdit(@RequestParam("id") String id, Model model) {
    // IDを使ってアカウント情報を取得
    AccountForm accountForm = accountadminService.getAccountById(id);
    model.addAttribute("accountForm", accountForm);
    return "accountedit/index"; // 編集画面(accountedit/index.html)を表示
  }

  // -----------------------------------------------------------------
  // 6. アカウント更新処理 (Edit POST)
  // URL: /accountedit/update
  // -----------------------------------------------------------------
  @PostMapping("/accountedit/update")
  public String updateAccount(
      @RequestParam("id") String id, // HTMLのhiddenフィールドからIDを受け取る
      @ModelAttribute AccountForm accountForm,
      RedirectAttributes redirectAttributes) {

    try {
      accountadminService.updateAccount(id, accountForm);
      redirectAttributes.addFlashAttribute("message",
          "アカウント (ID: " + id + ") を正常に更新しました。");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "アカウント更新に失敗しました: " + e.getLocalizedMessage());
    }

    return "redirect:/accountadmin";
  }
}
