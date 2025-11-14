package com.sysdev.slat.chat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ChatService {

  private final ChatRepository chatRepository;

  public ChatService(ChatRepository chatRepository) {
    this.chatRepository = chatRepository;
  }

  /**
   * ✅ DMメッセージ履歴を取得します。
   */
  public List<MessageHistoryDto> getDmHistory(String userId1, String userId2) {
    return chatRepository.findDmHistory(userId1, userId2);
  }

  /**
   * ✅ グループメッセージ履歴を取得します。 (新規追加)
   */
  public List<MessageHistoryDto> getGroupHistory(String groupId) {
    return chatRepository.findGroupHistory(groupId);
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
}
