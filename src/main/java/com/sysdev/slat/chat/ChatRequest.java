package com.sysdev.slat.chat;

import java.time.OffsetDateTime;

/**
 * チャットメッセージの送信リクエストを保持する DTO
 */
public class ChatRequest {
  private String groupId;
  private String senderId;
  private String recipientId;
  private String body;
  private OffsetDateTime expirationTime;

  public ChatRequest() {
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
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

  public String getRecipientId() {
    return recipientId;
  }

  public void setRecipientId(String recipientId) {
    this.recipientId = recipientId;
  }

  public OffsetDateTime getExpirationTime() {
    return expirationTime;
  }

  public void setExpirationTime(OffsetDateTime expirationTime) {
    this.expirationTime = expirationTime;
  }
}
