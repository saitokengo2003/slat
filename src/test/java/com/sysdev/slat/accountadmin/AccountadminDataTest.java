package com.sysdev.slat.accountadmin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountadminDataTest {

  @Test
  @DisplayName("正常系: 全フィールドのGetter/Setterが正しく動作すること")
  void testGettersAndSetters() {
    // 1. Ready
    AccountadminData data = new AccountadminData();

    // テスト用データ準備
    String id = "uuid-001";
    String username = "user_test";
    String passwordHash = "hashed_pw_123";
    String status = "active";
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    String displayName = "表示名テスト";
    String roleCode = "TEACHER";
    Integer grade = 2;
    String className = "B組";
    Integer number = 15;

    // 2. Do (Setter実行)
    data.setId(id);
    data.setUsername(username);
    data.setPassword_hash(passwordHash);
    data.setStatus(status);
    data.setCreated_at(now);
    data.setUpdated_at(now);
    data.setLast_login_at(now);
    data.setDisplay_name(displayName);
    data.setRole_code(roleCode);
    data.setGrade(grade);
    data.setClass_name(className);
    data.setNumber(number);

    // 3. Assert (Getter検証)
    assertEquals(id, data.getId());
    assertEquals(username, data.getUsername());
    assertEquals(passwordHash, data.getPassword_hash());
    assertEquals(status, data.getStatus());
    assertEquals(now, data.getCreated_at());
    assertEquals(now, data.getUpdated_at());
    assertEquals(now, data.getLast_login_at());
    assertEquals(displayName, data.getDisplay_name());
    assertEquals(roleCode, data.getRole_code());

    // ★★★ ここが足りていませんでした ★★★
    assertEquals(grade, data.getGrade());
    assertEquals(className, data.getClass_name());
    assertEquals(number, data.getNumber());
  }
}
