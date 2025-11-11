package com.sysdev.slat.controller;

import com.sysdev.slat.accountadmin.AccountadminEntity;
import com.sysdev.slat.accountadmin.AccountadminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
   * アカウント一覧画面を表示します。
   * URL: /accountadmin
   */
  @GetMapping("/accountadmin")
  public String showAccountList(Model model) {

    // Serviceからアカウントデータを含むエンティティを取得
    AccountadminEntity entity = accountadminService.getAccountListEntity();

    // モデルに追加
    model.addAttribute("accountadminEntity", entity);

    return "accountadmin/index";
  }

  // -----------------------------------------------------------------
  // 2. アカウント作成画面表示 (GET)
  // -----------------------------------------------------------------
  @GetMapping("/accountcreate")
  public String getAccountcreate() {
    return "accountcreate/index";
  }

  // -----------------------------------------------------------------
  // 3. アカウント削除 (POST) - パスを "/accountadmin/delete" に設定
  // -----------------------------------------------------------------
  /**
   * アカウント削除処理を実行します。
   * URL: /accountadmin/delete (POSTリクエスト)
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
