package com.sysdev.slat.chat;

import java.util.UUID;

/**
 * メッセージの編集および削除リクエストを保持する DTO
 */
public class EditDeleteRequest {
  private UUID messageId;
  private String body; // 編集時にのみ使用

  public UUID getMessageId() {
    return messageId;
  }

  public String getBody() {
    return body;
  }

  public void setMessageId(UUID messageId) {
    this.messageId = messageId;
  }

  public void setBody(String body) {
    this.body = body;
  }
}
