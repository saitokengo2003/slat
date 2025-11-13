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
   *
   * @param userId1 ログインユーザーID
   * @param userId2 チャット相手ID
   */
  public List<MessageHistoryDto> getDmHistory(String userId1, String userId2) {
    // Repositoryを呼び出し、履歴を取得する
    return chatRepository.findDmHistory(userId1, userId2);
  }

  /**
   * メッセージをDBに保存します。（保存先をDMとグループチャットで振り分け）
   */
  @Transactional
  public void saveChatMessage(ChatRequest request) {

    // 必須チェック: senderId, body
    if (request.getSenderId() == null || request.getBody() == null || request.getBody().trim().isEmpty()) {
      throw new IllegalArgumentException("Sender ID and message body are required.");
    }

    if (request.getRecipientId() != null && !request.getRecipientId().isEmpty()) {
      // 1. 個人チャット（DM）の場合: dmmessage テーブルに保存
      if (request.getRecipientId() == null || request.getRecipientId().isEmpty()) {
        throw new IllegalArgumentException("Recipient ID is required for DM.");
      }
      chatRepository.saveDmMessage(request);

    } else if (request.getGroupId() != null && !request.getGroupId().isEmpty()) {
      // 2. グループチャットの場合: messages テーブルに保存 (将来の実装)
      chatRepository.saveGroupMessage(request);

    } else {
      // 保存先が不明
      throw new IllegalArgumentException("Chat must specify either a recipientId (DM) or a groupId (Group Chat).");
    }
  }
}
