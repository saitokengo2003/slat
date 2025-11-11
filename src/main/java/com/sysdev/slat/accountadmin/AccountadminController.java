package com.sysdev.slat.accountadmin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.sql.SQLException;
import java.util.List; // デバッグ用モックから復帰したため必要

@Controller
@RequestMapping("/accountadmin")
public class AccountadminController {

  private final AccountadminService accountadminService;

  // 💡 依存性注入 (DI)
  @Autowired
  public AccountadminController(AccountadminService accountadminService) {
    this.accountadminService = accountadminService;
  }

  // -----------------------------------------------------------------
  // 1. 一覧表示 (GET)
  // -----------------------------------------------------------------
  /**
   * アカウント一覧画面を表示します。
   * URL: /accountadmin または /accountadmin/list
   */
  @GetMapping({ "/", "/list" })
  public String showAccountList(Model model) {

    // Serviceからアカウントデータを含むエンティティを取得
    AccountadminEntity entity = accountadminService.getAccountListEntity();

    model.addAttribute("accountadminEntity", entity);

    // message や errorMessage は RedirectAttributes 経由で渡されたものがあれば表示されます

    return "accountadmin/index";
  }

  // -----------------------------------------------------------------
  // 2. アカウント削除 (POST)
  // -----------------------------------------------------------------
  /**
   * アカウント削除処理を実行します。
   * URL: /accountadmin/delete (POSTリクエスト)
   */
  @PostMapping("/delete")
  public String deleteAccount(@RequestParam("id") String id, RedirectAttributes redirectAttributes) {
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
    return "redirect:/accountadmin/";
  }
}
