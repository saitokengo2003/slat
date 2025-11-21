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
  private final UserService userService; // ⭐ UserServiceの依存性注入

  // ⭐ MODIFIED: UserServiceをコンストラクタに追加
  public ChatService(ChatRepository chatRepository, ReactionService reactionService, UserService userService) {
    this.chatRepository = chatRepository;
    this.reactionService = reactionService;
    this.userService = userService;
  }

  // 期限付きメッセージの送信権限をチェックするヘルパーメソッド
  private boolean isAllowedToSendExpiredMessage(String userId) {
    // ⭐ UserServiceを使用してユーザーのロールを取得
    String role = userService.getUserRole(userId);
    return "admin".equals(role) || "teacher".equals(role);
  }

  /**
   * DMメッセージ履歴を取得します。（リアクションデータを結合するように修正）
   */
  public List<MessageHistoryDto> getDmHistory(String userId1, String userId2) {
    // 1. メッセージ履歴を取得
    List<MessageHistoryDto> history = chatRepository.findDmHistory(userId1, userId2);

    // 2. 履歴内の全メッセージIDを抽出
    List<UUID> messageIds = history.stream()
        .map(MessageHistoryDto::getMessageId)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toList());

    if (messageIds.isEmpty()) {
      return history;
    }

    // ⭐ MODIFIED: DM専用リアクションを一括取得
    List<ReactionEntity> allDmReactions = reactionService.getDmReactionsByMessageIds(messageIds);

    // 3. メッセージIDをキーとしてリアクションをマップに整理
    Map<UUID, List<ReactionEntity>> reactionsMap = allDmReactions.stream()
        .collect(Collectors.groupingBy(ReactionEntity::getMessageId));

    // 4. 履歴DTOにリアクションデータをセット
    history.forEach(dto -> {
      List<ReactionEntity> reactions = reactionsMap.getOrDefault(dto.getMessageId(), List.of());
      dto.setReactions(reactions);
    });

    return history;
  }

  /**
   * メッセージをDBに保存します。（保存先をDMとグループチャットで振り分け、期限付きメッセージに対応）
   */
  @Transactional
  public void saveChatMessage(ChatRequest request) {

    if (request.getSenderId() == null || request.getBody() == null || request.getBody().trim().isEmpty()) {
      throw new IllegalArgumentException("Sender ID and message body are required.");
    }

    // 期限付きメッセージの場合の権限チェック
    if (request.getExpirationTime() != null) {
      if (!isAllowedToSendExpiredMessage(request.getSenderId())) {
        throw new SecurityException("権限エラー: 期限付きメッセージは管理者または教師のみ送信可能です。");
      }
    }

    if (request.getGroupId() != null && !request.getGroupId().isEmpty()) {
      // 2. グループチャットの場合: messages テーブルに保存
      chatRepository.saveGroupMessage(request);

    } else if (request.getRecipientId() != null && !request.getRecipientId().isEmpty()) {
      // 1. 個人チャット（DM）の場合: dmmessage テーブルに保存
      chatRepository.saveDmMessage(request);

    } else {
      throw new IllegalArgumentException("Recipient ID or Group ID is required.");
    }
  }

  /**
   * グループメッセージ履歴を取得します。（変更なし）
   */
  public List<MessageHistoryDto> getGroupHistory(String groupId) {
    // 1. メッセージ履歴を取得
    List<MessageHistoryDto> history = chatRepository.findGroupHistory(groupId);

    // 2. 履歴内の全メッセージIDを抽出
    List<UUID> messageIds = history.stream()
        .map(MessageHistoryDto::getMessageId)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toList());

    if (messageIds.isEmpty()) {
      return history;
    }

    // 3. 全メッセージのリアクションを一括取得
    List<ReactionEntity> allReactions = reactionService.getReactionsByMessageIds(messageIds);

    // 4. メッセージIDをキーとしてリアクションをマップに整理
    Map<UUID, List<ReactionEntity>> reactionsMap = allReactions.stream()
        .collect(Collectors.groupingBy(ReactionEntity::getMessageId));

    // 5. 履歴DTOにリアクションデータをセット
    history.forEach(dto -> {
      List<ReactionEntity> reactions = reactionsMap.getOrDefault(dto.getMessageId(), List.of());
      dto.setReactions(reactions);
    });

    return history;
  }
}
