package com.sysdev.slat.reactions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReactionRequestTest {

  @Test
  @DisplayName("正常系: Getter/Setterの動作確認")
  void testGettersAndSetters() {
    // 1. Ready
    ReactionRequest request = new ReactionRequest();

    UUID messageId = UUID.randomUUID();
    String emoji = "👍";

    // 2. Do
    request.setMessageId(messageId);
    request.setEmoji(emoji);

    // 3. Assert
    assertEquals(messageId, request.getMessageId());
    assertEquals(emoji, request.getEmoji());
  }

  @Test
  @DisplayName("境界値: Nullを設定してもエラーにならず、Nullが返されること")
  void testNullValues() {
    // 1. Ready
    ReactionRequest request = new ReactionRequest();

    // 2. Do
    request.setMessageId(null);
    request.setEmoji(null);

    // 3. Assert
    assertNull(request.getMessageId());
    assertNull(request.getEmoji());
  }
}
