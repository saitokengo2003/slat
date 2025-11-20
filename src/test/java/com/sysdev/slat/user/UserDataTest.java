package com.sysdev.slat.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserDataTest {

  @Test
  @DisplayName("正常系: 全フィールドのGetter/Setterが正しく動作すること")
  void testGettersAndSetters() {
    // 1. Ready
    UserData userData = new UserData();

    // テストデータの定義
    String userId = "user001";
    String displayName = "山田 太郎";
    String roleCode = "STUDENT";
    Integer grade = 2;
    String className = "A組";
    Integer number = 10;

    // 2. Do (Setterの実行)
    userData.setUserId(userId);
    userData.setDisplayName(displayName);
    userData.setRoleCode(roleCode);
    userData.setGrade(grade);
    userData.setClassName(className);
    userData.setNumber(number);

    // 3. Assert (Getterの結果検証)
    assertEquals(userId, userData.getUserId());
    assertEquals(displayName, userData.getDisplayName());
    assertEquals(roleCode, userData.getRoleCode());
    assertEquals(grade, userData.getGrade());
    assertEquals(className, userData.getClassName());
    assertEquals(number, userData.getNumber());
  }

  @Test
  @DisplayName("境界値: Nullを設定してもエラーにならず、Nullが返されること")
  void testNullValues() {
    // 1. Ready
    UserData userData = new UserData();

    // 2. Do
    // 全フィールドに明示的にnullをセット
    userData.setUserId(null);
    userData.setDisplayName(null);
    userData.setRoleCode(null);
    userData.setGrade(null);
    userData.setClassName(null);
    userData.setNumber(null);

    // 3. Assert
    assertNull(userData.getUserId());
    assertNull(userData.getDisplayName());
    assertNull(userData.getRoleCode());
    assertNull(userData.getGrade());
    assertNull(userData.getClassName());
    assertNull(userData.getNumber());
  }
}
