package com.sysdev.slat.chat;

import java.time.OffsetDateTime;
import java.util.UUID; // 追加
import java.util.List; // 追加
import com.sysdev.slat.reactions.ReactionEntity; // ⭐ 追加

/**
 * DBから取得したメッセージ履歴を格納するDTO (Data Transfer Object) です。
 * JSONレスポンスとしてクライアントに送信されます。
 */
public class MessageHistoryDto {
  private UUID messageId;
  private String senderId;
  private String body;
  private OffsetDateTime createdAt; // ⭐ DBのTIMESTAMP WITH TIME ZONEに対応
  private List<ReactionEntity> reactions;

  // --- Getter/Setter ---
  public UUID getMessageId() { // ⭐ 追加
    return messageId;
  }

  public void setMessageId(UUID messageId) { // ⭐ 追加
    this.messageId = messageId;
  }

  public List<ReactionEntity> getReactions() { // ⭐ 追加
    return reactions;
  }

  public void setReactions(List<ReactionEntity> reactions) { // ⭐ 追加
    this.reactions = reactions;
  }

  public String getSenderId() {
    return senderId;
  }

  public void setSenderId(String senderId) {
    this.senderId = senderId;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
