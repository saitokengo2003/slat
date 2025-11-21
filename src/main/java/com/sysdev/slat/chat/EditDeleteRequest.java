package com.sysdev.slat.chat;

import java.util.UUID;

/**
 * メッセージの編集および削除リクエストを保持する DTO
 * (現在は削除機能で使用されます)
 */
public class EditDeleteRequest {
  private UUID messageId;
  private String body; // 編集時にのみ使用（削除機能では未使用だが、以前の互換性のために保持可能）

  // --- Getter ---
  public UUID getMessageId() {
    return messageId;
  }

  public String getBody() {
    return body;
  }

  // --- Setter ---
  public void setMessageId(UUID messageId) {
    this.messageId = messageId;
  }

  public void setBody(String body) {
    this.body = body;
  }
}
