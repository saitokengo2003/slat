package com.sysdev.slat.chat;

/**
 * チャットメッセージの送信リクエストを保持する DTO
 */
public class ChatRequest {
  private String groupId;
  private String senderId;
  private String recipientId; // ⭐ フィールドを追加
  private String body;

  public ChatRequest() {
  }

  // --- 既存の Getter/Setter (省略) ---

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

  // --- 新規追加する Getter/Setter ---

  public String getRecipientId() { // ⭐ ゲッター
    return recipientId;
  }

  public void setRecipientId(String recipientId) { // ⭐ セッター
    this.recipientId = recipientId;
  }
}
