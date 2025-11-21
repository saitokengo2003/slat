package com.sysdev.slat.chat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.sysdev.slat.reactions.ReactionService;
import com.sysdev.slat.reactions.ReactionEntity;
import com.sysdev.slat.user.UserService;
import java.util.Map;

@Service
public class ChatService {

  private final ChatRepository chatRepository;
  private final ReactionService reactionService;
  private final UserService userService;

  public ChatService(ChatRepository chatRepository, ReactionService reactionService, UserService userService) {
    this.chatRepository = chatRepository;
    this.reactionService = reactionService;
    this.userService = userService;
  }

  // 期限付きメッセージの送信権限をチェックするヘルパーメソッド
  private boolean isAllowedToSendExpiredMessage(String userId) {
    String role = userService.getUserRole(userId);
    return "admin".equals(role) || "teacher".equals(role);
  }

  // --- 履歴取得とリアクション統合 ---

  /**
   * DMメッセージ履歴を取得します。（DM専用リアクションデータを結合）
   */
  public List<MessageHistoryDto> getDmHistory(String userId1, String userId2) {
    List<MessageHistoryDto> history = chatRepository.findDmHistory(userId1, userId2);
    List<UUID> messageIds = history.stream()
        .map(MessageHistoryDto::getMessageId)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toList());

    if (messageIds.isEmpty()) {
      return history;
    }

    // DM専用リアクションを取得
    List<ReactionEntity> allDmReactions = reactionService.getDmReactionsByMessageIds(messageIds);
    Map<UUID, List<ReactionEntity>> reactionsMap = allDmReactions.stream()
        .collect(Collectors.groupingBy(ReactionEntity::getMessageId));

    history.forEach(dto -> {
      List<ReactionEntity> reactions = reactionsMap.getOrDefault(dto.getMessageId(), List.of());
      dto.setReactions(reactions);
    });

    return history;
  }

  /**
   * グループメッセージ履歴を取得します。（グループ専用リアクションデータを結合）
   */
  public List<MessageHistoryDto> getGroupHistory(String groupId) {
    List<MessageHistoryDto> history = chatRepository.findGroupHistory(groupId);
    List<UUID> messageIds = history.stream()
        .map(MessageHistoryDto::getMessageId)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toList());

    if (messageIds.isEmpty()) {
      return history;
    }

    List<ReactionEntity> allReactions = reactionService.getReactionsByMessageIds(messageIds);
    Map<UUID, List<ReactionEntity>> reactionsMap = allReactions.stream()
        .collect(Collectors.groupingBy(ReactionEntity::getMessageId));

    history.forEach(dto -> {
      List<ReactionEntity> reactions = reactionsMap.getOrDefault(dto.getMessageId(), List.of());
      dto.setReactions(reactions);
    });

    return history;
  }

  // --- メッセージ保存 ---

  /**
   * メッセージをDBに保存します。（期限付きメッセージに対応）
   */
  @Transactional
  public void saveChatMessage(ChatRequest request) {

    if (request.getSenderId() == null || request.getBody() == null || request.getBody().trim().isEmpty()) {
      throw new IllegalArgumentException("Sender ID and message body are required.");
    }

    if (request.getExpirationTime() != null) {
      if (!isAllowedToSendExpiredMessage(request.getSenderId())) {
        throw new SecurityException("権限エラー: 期限付きメッセージは管理者または教師のみ送信可能です。");
      }
    }

    if (request.getGroupId() != null && !request.getGroupId().isEmpty()) {
      chatRepository.saveGroupMessage(request);
    } else if (request.getRecipientId() != null && !request.getRecipientId().isEmpty()) {
      chatRepository.saveDmMessage(request);
    } else {
      throw new IllegalArgumentException("Recipient ID or Group ID is required.");
    }
  }

  // --- 削除機能 ---

  /**
   * メッセージの削除 (物理削除)
   */
  @Transactional
  public void deleteMessage(UUID messageId, String currentUserId) {
    // 1. 権限チェック (メッセージの送信者であるかを確認)
    String senderId = chatRepository.findSenderIdByMessageId(messageId);

    if (!currentUserId.equals(senderId)) {
      throw new SecurityException("権限エラー: 他のユーザーのメッセージは削除できません。");
    }

    // 2. 物理削除の実行
    chatRepository.deleteMessagePhysical(messageId);
  }

  // --- ⭐ NEW: 編集機能 ---

  /**
   * メッセージの編集 (本文更新)
   */
  @Transactional
  public void editMessage(UUID messageId, String newBody, String currentUserId) {
    if (newBody == null || newBody.trim().isEmpty()) {
      throw new IllegalArgumentException("メッセージ本文は必須です。");
    }

    // 1. 権限チェック (メッセージの送信者であるかを確認)
    String senderId = chatRepository.findSenderIdByMessageId(messageId);

    if (!currentUserId.equals(senderId)) {
      throw new SecurityException("権限エラー: 他のユーザーのメッセージは編集できません。");
    }

    // 2. 本文の更新を実行
    int updatedRows = chatRepository.updateMessageBody(messageId, newBody);

    if (updatedRows == 0) {
      // updateMessageBody はメッセージIDが見つからない場合 0 を返すため
      throw new IllegalArgumentException("メッセージIDが見つかりません。");
    }
  }
}
