package com.sysdev.slat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchResultDtoTest {

  @Test
  @DisplayName("正常系: 全フィールドのGetter/Setterが正しく動作すること")
  void testGettersAndSetters() {
    // 1. Ready
    SearchResultDto dto = new SearchResultDto();

    // テスト用データの準備
    String messageId = "msg-001";
    String body = "テストメッセージ本文";
    OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    String groupId = "group-999";
    String groupName = "開発チーム";
    String senderUsername = "user01";
    String senderDisplayName = "山田 太郎";

    // 2. Do
    dto.setMessageId(messageId);
    dto.setBody(body);
    dto.setCreatedAt(createdAt);
    dto.setGroupId(groupId);
    dto.setGroupName(groupName);
    dto.setSenderUsername(senderUsername);
    dto.setSenderDisplayName(senderDisplayName);

    // 3. Assert
    assertEquals(messageId, dto.getMessageId());
    assertEquals(body, dto.getBody());
    assertEquals(createdAt, dto.getCreatedAt());
    assertEquals(groupId, dto.getGroupId());
    assertEquals(groupName, dto.getGroupName());
    assertEquals(senderUsername, dto.getSenderUsername());
    assertEquals(senderDisplayName, dto.getSenderDisplayName());
  }

  @Test
  @DisplayName("境界値: Nullを設定してもエラーにならず、Nullが返されること")
  void testNullValues() {
    // 1. Ready
    SearchResultDto dto = new SearchResultDto();

    // 2. Do
    dto.setMessageId(null);
    dto.setBody(null);
    dto.setCreatedAt(null);
    dto.setGroupId(null);
    dto.setGroupName(null);
    dto.setSenderUsername(null);
    dto.setSenderDisplayName(null);

    // 3. Assert
    assertNull(dto.getMessageId());
    assertNull(dto.getBody());
    assertNull(dto.getCreatedAt());
    assertNull(dto.getGroupId());
    assertNull(dto.getGroupName());
    assertNull(dto.getSenderUsername());
    assertNull(dto.getSenderDisplayName());
  }
}
