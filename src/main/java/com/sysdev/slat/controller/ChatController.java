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
import com.sysdev.slat.chat.EditDeleteRequest;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;
import com.sysdev.slat.reactions.ReactionService;
import com.sysdev.slat.reactions.ReactionRequest;

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
    model.addAttribute("loggedInUserRole", userData.getRoleCode());

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

    chatRequest.setSenderId(userData.getUserId());

    if (chatRequest.getBody() == null || chatRequest.getBody().trim().isEmpty()
        || (chatRequest.getRecipientId() == null && chatRequest.getGroupId() == null)) {

      return "ERROR: Message body, recipient ID, or group ID is missing.";
    }

    try {
      chatService.saveChatMessage(chatRequest);
      return "SUCCESS";
    } catch (SecurityException e) {
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
   * 4. グループメッセージ履歴を取得する GET APIエンドポイント
   */
  @GetMapping("/api/group/history")
  @ResponseBody
  public List<MessageHistoryDto> getGroupHistory(
      @RequestParam("groupId") String groupId,
      HttpSession session) {

    UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);

    if (userData == null) {
      return List.of();
    }

    try {
      return chatService.getGroupHistory(groupId);
    } catch (Exception e) {
      System.err.println("Group History Load Error: " + e.getMessage());
      return List.of();
    }
  }

  /**
   * 5. リアクションを登録・削除する POST APIエンドポイント
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

  /**
   * 6. メッセージを削除する POST APIエンドポイント (物理削除)
   */
  @PostMapping("/api/message/delete")
  @ResponseBody
  public String deleteMessage(
      @RequestBody EditDeleteRequest request,
      @SessionAttribute(name = SESSION_USER_DATA_KEY, required = false) UserData userData) {

    if (userData == null || request.getMessageId() == null) {
      return "ERROR: User not authenticated or missing message ID.";
    }

    try {
      chatService.deleteMessage(request.getMessageId(), userData.getUserId());
      return "SUCCESS";
    } catch (SecurityException e) {
      return e.getMessage();
    } catch (IllegalArgumentException e) {
      return "ERROR: " + e.getMessage();
    } catch (Exception e) {
      System.err.println("Message Delete Error: " + e.getMessage());
      return "ERROR: Failed to delete message.";
    }
  }

  /**
   * 7. メッセージを編集する POST APIエンドポイント
   */
  @PostMapping("/api/message/edit")
  @ResponseBody
  public String editMessage(
      @RequestBody EditDeleteRequest request,
      @SessionAttribute(name = SESSION_USER_DATA_KEY, required = false) UserData userData) {

    if (userData == null || request.getMessageId() == null || request.getBody() == null) {
      return "ERROR: User not authenticated or missing message ID/body.";
    }

    try {
      chatService.editMessage(request.getMessageId(), request.getBody(), userData.getUserId());
      return "SUCCESS";
    } catch (SecurityException e) {
      return e.getMessage();
    } catch (IllegalArgumentException e) {
      return "ERROR: " + e.getMessage();
    } catch (Exception e) {
      System.err.println("Message Edit Error: " + e.getMessage());
      return "ERROR: Failed to edit message.";
    }
  }
}
