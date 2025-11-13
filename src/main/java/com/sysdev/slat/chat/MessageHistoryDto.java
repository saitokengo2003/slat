package com.sysdev.slat.chat;

import java.time.OffsetDateTime;

/**
 * DBから取得したメッセージ履歴を格納するDTO (Data Transfer Object) です。
 * JSONレスポンスとしてクライアントに送信されます。
 */
public class MessageHistoryDto {
  private String senderId;
  private String body;
  private OffsetDateTime createdAt; // ⭐ DBのTIMESTAMP WITH TIME ZONEに対応

  // --- Getter/Setter ---

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
