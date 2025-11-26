package com.sysdev.slat.chat;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import com.sysdev.slat.reactions.ReactionEntity;

/**
 * DBから取得したメッセージ履歴を格納するDTOです。
 * JSONレスポンスとしてクライアントに送信されます。
 */
public class MessageHistoryDto {
  private UUID messageId;
  private String senderId;

  // ★ 追加：表示名（display_name）
  private String senderName;

  private String body;
  private OffsetDateTime createdAt;
  private List<ReactionEntity> reactions;
  private OffsetDateTime expirationTime;

  private List<String> nonReactingStudentNames;

  // --- Getter/Setter ---

  public UUID getMessageId() {
    return messageId;
  }

  public void setMessageId(UUID messageId) {
    this.messageId = messageId;
  }

  public List<ReactionEntity> getReactions() {
    return reactions;
  }

  public void setReactions(List<ReactionEntity> reactions) {
    this.reactions = reactions;
  }

  public String getSenderId() {
    return senderId;
  }

  public void setSenderId(String senderId) {
    this.senderId = senderId;
  }

  // ★ Getter/Setter 追加
  public String getSenderName() {
    return senderName;
  }

  public void setSenderName(String senderName) {
    this.senderName = senderName;
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

  public OffsetDateTime getExpirationTime() {
    return expirationTime;
  }

  public void setExpirationTime(OffsetDateTime expirationTime) {
    this.expirationTime = expirationTime;
  }

  public List<String> getNonReactingStudentNames() {
    return nonReactingStudentNames;
  }

  public void setNonReactingStudentNames(List<String> nonReactingStudentNames) {
    this.nonReactingStudentNames = nonReactingStudentNames;
  }
}
