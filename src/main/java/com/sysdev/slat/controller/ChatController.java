package com.sysdev.slat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping; // ✅ POSTingをimport
import org.springframework.web.bind.annotation.RequestBody; // ✅ RequestBodyをimport
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.sysdev.slat.user.UserData;
import com.sysdev.slat.user.UserService;
import com.sysdev.slat.chat.ChatService;
import com.sysdev.slat.chat.MessageHistoryDto;
import com.sysdev.slat.chat.ChatRequest; // ✅ ChatRequestをimport

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
public class ChatController {

  @Autowired
  private ChatService chatService;

  @Autowired
  private UserService userService;

  // セッションに保存するUserDataのキー
  private final String SESSION_USER_DATA_KEY = "userData";

  /**
   * ✅ 1. チャット画面を表示する GET メソッド
   */
  @GetMapping("/chat")
  public String getChat(Model model, HttpSession session) {

    UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);

    if (userData == null) {
      return "redirect:/login"; // 認証されていない場合はリダイレクト
    }

    // ログインユーザー情報をModelに追加 (JSがloggedInUserIdを取得するため)
    model.addAttribute("loggedInUserId", userData.getUserId());
    model.addAttribute("displayName", userData.getDisplayName());

    // DM相手リストを取得しModelに追加 (HTMLがotherUsersをループするため)
    List<UserData> otherUsers = userService.findAllOtherUsers(userData.getUserId());
    model.addAttribute("otherUsers", otherUsers);

    return "chat/index";
  }

  /**
   * ✅ 2. メッセージ送信を受け付ける POST APIエンドポイント
   */
  @PostMapping("/api/message/send")
  @ResponseBody
  public String sendMessage(
      @RequestBody ChatRequest chatRequest, // JSONで送られてきたメッセージデータ
      @SessionAttribute(name = SESSION_USER_DATA_KEY, required = false) UserData userData) {

    if (userData == null) {
      // 認証されていない場合はエラーを返す
      // このエラーが500の原因になっている可能性もある
      return "ERROR: User not authenticated.";
    }

    // ⭐ 送信元IDをセッションから取得したログインユーザーIDに設定
    chatRequest.setSenderId(userData.getUserId());

    if (chatRequest.getBody() == null || chatRequest.getBody().trim().isEmpty()
        || chatRequest.getRecipientId() == null) {
      // 必須データが欠落
      return "ERROR: Message body or recipient ID is missing.";
    }

    try {
      // ChatService に保存処理を委譲
      chatService.saveChatMessage(chatRequest);
      return "OK";
    } catch (Exception e) {
      // DBまたはServiceでのエラー
      System.err.println("Message Save Error: " + e.getMessage());
      // 🚨 ここで500エラーが起きている可能性もある
      return "ERROR: Failed to save message due to internal server error.";
    }
  }

  /**
   * ✅ 3. DMメッセージ履歴を取得する GET APIエンドポイント
   */
  @GetMapping("/api/dm/history")
  @ResponseBody
  public List<MessageHistoryDto> getDmHistory(
      @RequestParam("recipientId") String recipientId,
      HttpSession session) {

    UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);
    if (userData == null || userData.getUserId() == null) {
      throw new IllegalStateException("User not logged in.");
    }
    String loggedInUserId = userData.getUserId();

    return chatService.getDmHistory(loggedInUserId, recipientId);
  }
}
