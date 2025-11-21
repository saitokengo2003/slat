package com.sysdev.slat.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EditDeleteRequestTest {

  @Test
  @DisplayName("正常系: Getter/Setterの動作確認")
  void testGettersAndSetters() {
    // 1. Ready
    EditDeleteRequest request = new EditDeleteRequest();

    UUID uuid = UUID.randomUUID();
    String body = "編集後のメッセージ本文";

    // 2. Do
    request.setMessageId(uuid);
    request.setBody(body);

    // 3. Assert
    assertEquals(uuid, request.getMessageId());
    assertEquals(body, request.getBody());
  }

  @Test
  @DisplayName("境界値: Nullを設定してもエラーにならず、Nullが返されること")
  void testNullValues() {
    // 1. Ready
    EditDeleteRequest request = new EditDeleteRequest();

    // 2. Do
    request.setMessageId(null);
    request.setBody(null);

    // 3. Assert
    assertNull(request.getMessageId());
    assertNull(request.getBody());
  }
}
