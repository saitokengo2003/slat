package com.sysdev.slat.reactions;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.util.UUID;
import java.time.OffsetDateTime;

/**
 * reactions テーブルに対応するデータベースエンティティです。
 */
@Table("reactions")
public class ReactionEntity {

  @Id
  private UUID id;

  private UUID messageId; // どのメッセージに付けられたか (messages.id)
  private String userId; // 誰がリアクションしたか
  private String emoji; // どの絵文字か (例: "👍", "❤️")
  private OffsetDateTime createdAt = OffsetDateTime.now();

  // Constructor, Getters, Setters...
  public ReactionEntity() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getMessageId() {
    return messageId;
  }

  public void setMessageId(UUID messageId) {
    this.messageId = messageId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getEmoji() {
    return emoji;
  }

  public void setEmoji(String emoji) {
    this.emoji = emoji;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
