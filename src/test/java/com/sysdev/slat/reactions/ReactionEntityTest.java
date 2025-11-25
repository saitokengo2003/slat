package com.sysdev.slat.reactions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReactionEntityTest {

  @Test
  @DisplayName("正常系: 全フィールドのGetter/Setterが正しく動作すること")
  void testGettersAndSetters() {
    // 1. Ready
    ReactionEntity entity = new ReactionEntity();

    // テストデータ
    UUID id = UUID.randomUUID();
    UUID messageId = UUID.randomUUID();
    String userId = "user-123";
    String emoji = "🎉";
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    // 2. Do (Setter実行)
    entity.setId(id);
    entity.setMessageId(messageId);
    entity.setUserId(userId);
    entity.setEmoji(emoji);
    entity.setCreatedAt(now);

    // 3. Assert (Getter検証)
    assertEquals(id, entity.getId());
    assertEquals(messageId, entity.getMessageId());
    assertEquals(userId, entity.getUserId());
    assertEquals(emoji, entity.getEmoji());
    assertEquals(now, entity.getCreatedAt());
  }

  @Test
  @DisplayName("初期化: インスタンス生成時にcreatedAtに日時が自動設定されていること")
  void testDefaultValues() {
    // 1. Ready & 2. Do
    ReactionEntity entity = new ReactionEntity();

    // 3. Assert
    assertNotNull(entity.getCreatedAt(), "createdAtは初期化時に自動設定されるべき");

    assertNull(entity.getId());
    assertNull(entity.getMessageId());
    assertNull(entity.getUserId());
    assertNull(entity.getEmoji());
  }

  @Test
  @DisplayName("境界値: Nullを設定できること")
  void testNullValues() {
    // 1. Ready
    ReactionEntity entity = new ReactionEntity();

    // 2. Do
    entity.setId(null);
    entity.setMessageId(null);
    entity.setUserId(null);
    entity.setEmoji(null);
    entity.setCreatedAt(null);

    // 3. Assert
    assertNull(entity.getId());
    assertNull(entity.getMessageId());
    assertNull(entity.getUserId());
    assertNull(entity.getEmoji());
    assertNull(entity.getCreatedAt());
  }
}
