package com.sysdev.slat.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GroupTest {

  @Test
  @DisplayName("正常系: 全フィールドのGetter/Setterが正しく動作すること")
  void testGettersAndSetters() {
    // 1. Ready
    Group group = new Group();

    // テストデータの作成
    UUID id = UUID.randomUUID();
    String name = "テストグループA";
    Boolean isDm = true;
    // 日時データ
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    // 2. Do (Setterの実行)
    group.setId(id);
    group.setName(name);
    group.setIsDm(isDm);
    group.setCreatedAt(now);
    group.setUpdatedAt(now);

    // 3. Assert (Getterの結果検証)
    assertEquals(id, group.getId());
    assertEquals(name, group.getName());
    assertEquals(isDm, group.getIsDm());
    assertEquals(now, group.getCreatedAt());
    assertEquals(now, group.getUpdatedAt());
  }

  @Test
  @DisplayName("境界値: Nullを設定してもエラーにならず、Nullが返されること")
  void testNullValues() {
    // 1. Ready
    Group group = new Group();

    // 2. Do
    // 全フィールドに明示的にnullをセット
    // (プリミティブ型ではなくラッパークラスを使用しているためnull許容)
    group.setId(null);
    group.setName(null);
    group.setIsDm(null);
    group.setCreatedAt(null);
    group.setUpdatedAt(null);

    // 3. Assert
    assertNull(group.getId());
    assertNull(group.getName());
    assertNull(group.getIsDm());
    assertNull(group.getCreatedAt());
    assertNull(group.getUpdatedAt());
  }
}
