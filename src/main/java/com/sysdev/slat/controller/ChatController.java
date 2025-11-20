package com.sysdev.slat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.sysdev.slat.user.UserData;
import com.sysdev.slat.user.UserService;
import com.sysdev.slat.chat.ChatService;
import com.sysdev.slat.chat.MessageHistoryDto;
import com.sysdev.slat.chat.ChatRequest;
import com.sysdev.slat.chat.GroupRepository;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import com.sysdev.slat.reactions.ReactionService; // ⭐ 追加
import com.sysdev.slat.reactions.ReactionRequest; // ⭐ 追加

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID; // 追加

@Controller
public class ChatController {

  @Autowired
  private ChatService chatService;

  @Autowired
  private UserService userService;

  @Autowired
  private GroupRepository groupRepository;
  @Autowired
  private ReactionService reactionService;

  private final String SESSION_USER_DATA_KEY = "userData";

  /**
   * 1. チャット画面を表示する GET メソッド
   */
  @GetMapping("/chat")
  public String getChat(Model model, HttpSession session,
      @RequestParam(name = "dmUserId", required = false) String dmUserId,
      @RequestParam(name = "groupId", required = false) String groupId) {

    UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);

    if (userData == null) {
      return "redirect:/login";
    }

    model.addAttribute("loggedInUserId", userData.getUserId());
    model.addAttribute("displayName", userData.getDisplayName());
    model.addAttribute("loggedInUserRole", userData.getRoleCode()); // ✅ NEW: ロールコードを追加

    // DM相手リストを取得
    List<UserData> otherUsers = userService.findAllOtherUsers(userData.getUserId());
    model.addAttribute("otherUsers", otherUsers);

    // ログインユーザーが参加しているグループチャット一覧を取得
    List<com.sysdev.slat.chat.Group> generalGroups = groupRepository.findJoinedGroupsByUserId(userData.getUserId());
    model.addAttribute("generalGroups", generalGroups);

    return "chat/index";
  }

  /**
   * 2. メッセージを送信する POST APIエンドポイント
   */
  @PostMapping("/api/message/send")
  @ResponseBody
  public String sendMessage(
      @RequestBody ChatRequest chatRequest,
      @SessionAttribute(name = SESSION_USER_DATA_KEY, required = false) UserData userData) {

    if (userData == null) {
      return "ERROR: User not authenticated.";
    }

    // ⭐ MODIFIED: 送信者が不正なIDを設定できないよう、セッションのIDで上書きする
    chatRequest.setSenderId(userData.getUserId());

    if (chatRequest.getBody() == null || chatRequest.getBody().trim().isEmpty()
        || (chatRequest.getRecipientId() == null && chatRequest.getGroupId() == null)) {

      return "ERROR: Message body, recipient ID, or group ID is missing.";
    }

    try {
      chatService.saveChatMessage(chatRequest);
      return "SUCCESS"; // 戻り値を "OK" から "SUCCESS" に変更
    } catch (SecurityException e) {
      // ✅ 権限エラーの場合の処理を追加
      return e.getMessage();
    } catch (Exception e) {
      System.err.println("Message Save Error: " + e.getMessage());
      return "ERROR: Failed to save message due to internal server error.";
    }
  }

  /**
   * 3. DMメッセージ履歴を取得する GET APIエンドポイント
   */
  @GetMapping("/api/dm/history")
  @ResponseBody
  public List<MessageHistoryDto> getDmHistory(
      @RequestParam("recipientId") String recipientId,
      HttpSession session) {

    UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);

    if (userData == null) {
      return List.of();
    }

    try {
      return chatService.getDmHistory(userData.getUserId(), recipientId);
    } catch (Exception e) {
      System.err.println("DM History Load Error: " + e.getMessage());
      return List.of();
    }
  }

  /**
   * ✅ 4. グループメッセージ履歴を取得する GET APIエンドポイント (新規追加)
   */
  @GetMapping("/api/group/history")
  @ResponseBody
  public List<MessageHistoryDto> getGroupHistory(
      @RequestParam("groupId") String groupId,
      HttpSession session) {

    UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);

    if (userData == null) {
      // 認証されていない場合は空のリストを返す
      return List.of();
    }

    try {
      // ログインチェック済みのため、Serviceを呼び出して履歴を取得
      return chatService.getGroupHistory(groupId);
    } catch (Exception e) {
      System.err.println("Group History Load Error: " + e.getMessage());
      return List.of();
    }
  }

  /**
   * ✅ 5. リアクションを登録・削除する POST APIエンドポイント (新規追加)
   */
  @PostMapping("/api/reaction/toggle")
  @ResponseBody
  public String toggleReaction(
      @RequestBody ReactionRequest reactionRequest,
      @SessionAttribute(name = SESSION_USER_DATA_KEY, required = false) UserData userData) {

    if (userData == null) {
      return "ERROR: User not authenticated.";
    }

    UUID messageId = reactionRequest.getMessageId();
    String emoji = reactionRequest.getEmoji();
    String userId = userData.getUserId();

    if (messageId == null || emoji == null || emoji.isEmpty()) {
      return "ERROR: Message ID or emoji is missing.";
    }

    try {
      boolean added = reactionService.toggleReaction(messageId, userId, emoji);
      return added ? "ADDED" : "REMOVED";
    } catch (Exception e) {
      System.err.println("Reaction Toggle Error: " + e.getMessage());
      return "ERROR: Failed to toggle reaction.";
    }
  }

}
