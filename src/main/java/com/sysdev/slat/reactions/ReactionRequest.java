package com.sysdev.slat.reactions;

import java.util.UUID;

/**
 * リアクション操作のリクエストを保持する DTO
 */
public class ReactionRequest {
  private UUID messageId; // リアクション対象のメッセージID
  private String emoji; // 使用する絵文字

  // --- Getter/Setter ---
  public UUID getMessageId() {
    return messageId;
  }

  public void setMessageId(UUID messageId) {
    this.messageId = messageId;
  }

  public String getEmoji() {
    return emoji;
  }

  public void setEmoji(String emoji) {
    this.emoji = emoji;
  }
}
