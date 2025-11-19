package com.sysdev.slat.chat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.sysdev.slat.reactions.ReactionService; // ⭐ 追加
import com.sysdev.slat.reactions.ReactionEntity; // ⭐ 追加

@Service
public class ChatService {

  private final ChatRepository chatRepository;
  private final ReactionService reactionService;

  public ChatService(ChatRepository chatRepository, ReactionService reactionService) {
    this.chatRepository = chatRepository;
    this.reactionService = reactionService;
  }

  /**
   * ✅ DMメッセージ履歴を取得します。
   */
  public List<MessageHistoryDto> getDmHistory(String userId1, String userId2) {
    return chatRepository.findDmHistory(userId1, userId2);
  }

  /**
   * メッセージをDBに保存します。（保存先をDMとグループチャットで振り分け）
   */
  @Transactional
  public void saveChatMessage(ChatRequest request) {

    if (request.getSenderId() == null || request.getBody() == null || request.getBody().trim().isEmpty()) {
      throw new IllegalArgumentException("Sender ID and message body are required.");
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
   * ✅ グループメッセージ履歴を取得します。（リアクションデータを結合するように修正）
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
    java.util.Map<UUID, List<ReactionEntity>> reactionsMap = allReactions.stream()
        .collect(Collectors.groupingBy(ReactionEntity::getMessageId));

    // 5. 履歴DTOにリアクションデータをセット
    history.forEach(dto -> {
      List<ReactionEntity> reactions = reactionsMap.getOrDefault(dto.getMessageId(), List.of());
      dto.setReactions(reactions);
    });

    return history;
  }
}
