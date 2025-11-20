// DmReactionEntity.java (新規作成)
package com.sysdev.slat.reactions;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.util.UUID;
import java.time.OffsetDateTime;

/**
 * dm_reactions テーブルに対応するデータベースエンティティです。
 */
@Table("dm_reactions") // ⭐ DM専用テーブルを参照
public class DmReactionEntity {

  @Id
  private UUID id;

  private UUID dmMessageId; // ⭐ DMメッセージID (dmmessage.id)
  private String userId;
  private String emoji;
  private OffsetDateTime createdAt = OffsetDateTime.now();

  // Constructor, Getters, Setters...
  public DmReactionEntity() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getDmMessageId() {
    return dmMessageId;
  }

  public void setDmMessageId(UUID dmMessageId) {
    this.dmMessageId = dmMessageId;
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
