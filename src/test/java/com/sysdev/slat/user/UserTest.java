package com.sysdev.slat.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  @DisplayName("正常系: 全フィールドのGetter/Setterが正しく動作すること")
  void testGettersAndSetters() {
    // 1. Ready
    User user = new User();

    // テストデータの作成
    UUID id = UUID.randomUUID();
    String username = "user01";
    String passwordHash = "hashed_secret";
    String status = "active";
    // 日時データ（UTCなどタイムゾーンを指定して生成）
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    String displayName = "田中 太郎";
    String roleCode = "STUDENT";
    Integer grade = 3;
    String className = "C組";
    Integer number = 15;

    // 2. Do (Setterの実行)
    user.setId(id);
    user.setUsername(username);
    user.setPasswordHash(passwordHash);
    user.setStatus(status);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    user.setLastLoginAt(now);
    user.setDisplayName(displayName);
    user.setRoleCode(roleCode);
    user.setGrade(grade);
    user.setClassName(className);
    user.setNumber(number);

    // 3. Assert (Getterの結果検証)
    assertEquals(id, user.getId());
    assertEquals(username, user.getUsername());
    assertEquals(passwordHash, user.getPasswordHash());
    assertEquals(status, user.getStatus());
    assertEquals(now, user.getCreatedAt());
    assertEquals(now, user.getUpdatedAt());
    assertEquals(now, user.getLastLoginAt());
    assertEquals(displayName, user.getDisplayName());
    assertEquals(roleCode, user.getRoleCode());
    assertEquals(grade, user.getGrade());
    assertEquals(className, user.getClassName());
    assertEquals(number, user.getNumber());
  }

  @Test
  @DisplayName("境界値: Nullを設定してもエラーにならず、Nullが返されること")
  void testNullValues() {
    // 1. Ready
    User user = new User();

    // 2. Do
    // プリミティブ型(intなど)ではなくラッパークラス(Integer, UUIDなど)なのでnull許容
    user.setId(null);
    user.setUsername(null);
    user.setPasswordHash(null);
    user.setStatus(null);
    user.setCreatedAt(null);
    user.setUpdatedAt(null);
    user.setLastLoginAt(null);
    user.setDisplayName(null);
    user.setRoleCode(null);
    user.setGrade(null);
    user.setClassName(null);
    user.setNumber(null);

    // 3. Assert
    assertNull(user.getId());
    assertNull(user.getUsername());
    assertNull(user.getPasswordHash());
    assertNull(user.getStatus());
    assertNull(user.getCreatedAt());
    assertNull(user.getUpdatedAt());
    assertNull(user.getLastLoginAt());
    assertNull(user.getDisplayName());
    assertNull(user.getRoleCode());
    assertNull(user.getGrade());
    assertNull(user.getClassName());
    assertNull(user.getNumber());
  }
}
