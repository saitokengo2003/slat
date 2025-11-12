package com.sysdev.slat.controller;

import com.sysdev.slat.accountadmin.AccountadminEntity;
import com.sysdev.slat.accountadmin.AccountadminService;
import com.sysdev.slat.accountadmin.AccountForm; // AccountFormのパッケージに注意
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.sql.SQLException;

@Controller
public class AccountController {

  private final AccountadminService accountadminService;

  @Autowired
  public AccountController(AccountadminService accountadminService) {
    this.accountadminService = accountadminService;
  }

  /**
   * 1. アカウント一覧画面を表示します。
   * URL: /accountadmin
   */
  @GetMapping("/accountadmin")
  public String showAccountList(Model model) {
    AccountadminEntity entity = accountadminService.getAccountListEntity();
    model.addAttribute("accountadminEntity", entity);
    return "accountadmin/index";
  }

  /**
   * 2. アカウント作成画面表示 (GET)
   * URL: /accountcreate
   */
  @GetMapping("/accountcreate")
  public String getAccountcreate(Model model) {
    // Thymeleafのth:object="${accountForm}"にバインドするため
    model.addAttribute("accountForm", new AccountForm());
    return "accountcreate/index";
  }

  /**
   * 3. アカウント作成処理 (POST)
   * URL: /accountcreate
   */
  @PostMapping("/accountcreate")
  public String createAccount(
      @ModelAttribute AccountForm accountForm,
      RedirectAttributes redirectAttributes) {

    try {
      // Serviceを呼び出してアカウントを保存
      accountadminService.createAccount(accountForm);

      // 成功メッセージをリダイレクト先に追加
      redirectAttributes.addFlashAttribute("message", "新しいアカウントを正常に作成しました。");

    } catch (Exception e) {
      // ServiceからスローされたDBエラーなどをキャッチ
      redirectAttributes.addFlashAttribute("errorMessage", "アカウント作成に失敗しました: " + e.getLocalizedMessage());
    }

    // 処理後、一覧画面にリダイレクト
    return "redirect:/accountadmin";
  }

  /**
   * 4. アカウント削除処理 (POST)
   * URL: /accountadmin/delete
   */
  @PostMapping("/accountadmin/delete")
  public String deleteAccount(
      @RequestParam("id") String id,
      RedirectAttributes redirectAttributes) {
    try {
      // Service経由で削除処理を実行
      accountadminService.deleteAccount(id);

      // 削除成功メッセージをリダイレクト先に追加
      redirectAttributes.addFlashAttribute("message", "アカウント (ID: " + id + ") を正常に削除しました。");

    } catch (SQLException e) {
      // DBエラーをキャッチし、エラーメッセージを設定
      redirectAttributes.addFlashAttribute("errorMessage", "アカウント削除に失敗しました: " + e.getLocalizedMessage());
    }

    // 処理後、一覧画面にリダイレクト
    return "redirect:/accountadmin";
  }
}
