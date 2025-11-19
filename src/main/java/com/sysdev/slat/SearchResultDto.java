package com.sysdev.slat;

import java.time.OffsetDateTime;

public class SearchResultDto {

  private String messageId; // messages.id
  private String body; // メッセージ本文
  private OffsetDateTime createdAt;

  private String groupId; // group_s.id
  private String groupName; // group_s.name

  private String senderUsername; // users_s.username
  private String senderDisplayName;// users_s.display_name

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
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

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getSenderUsername() {
    return senderUsername;
  }

  public void setSenderUsername(String senderUsername) {
    this.senderUsername = senderUsername;
  }

  public String getSenderDisplayName() {
    return senderDisplayName;
  }

  public void setSenderDisplayName(String senderDisplayName) {
    this.senderDisplayName = senderDisplayName;
  }
}
