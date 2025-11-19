package com.sysdev.slat.accountadmin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountFormTest {

  @Test
  @DisplayName("正常系: Setterで設定した値がGetterで取得できること")
  void testGettersAndSetters() {
    // 1. Ready
    AccountForm form = new AccountForm();

    // テストデータの定義
    String id = "uuid-1234";
    String userId = "user001";
    String password = "passwordHash";
    String name = "テスト 太郎";
    String role = "ADMIN";
    String grade = "3";
    String classId = "A組";
    Integer number = 15;

    // 2. Do (Setterの実行)
    form.setId(id);
    form.setUserId(userId);
    form.setPassword(password);
    form.setName(name);
    form.setRole(role);
    form.setGrade(grade);
    form.setClassId(classId);
    form.setNumber(number);

    // 3. Assert (Getterの結果検証)
    assertEquals(id, form.getId());
    assertEquals(userId, form.getUserId());
    assertEquals(password, form.getPassword());
    assertEquals(name, form.getName());
    assertEquals(role, form.getRole());
    assertEquals(grade, form.getGrade());
    assertEquals(classId, form.getClassId());
    assertEquals(number, form.getNumber());
  }

  @Test
  @DisplayName("境界値: Nullを設定してもエラーにならず、Nullが返されること")
  void testNullValues() {
    // 1. Ready
    AccountForm form = new AccountForm();

    // 2. Do
    form.setId(null);
    form.setUserId(null);
    form.setPassword(null);
    form.setName(null);
    form.setRole(null);
    form.setGrade(null);
    form.setClassId(null);
    form.setNumber(null); // Integer型なのでnull許容

    // 3. Assert
    assertNull(form.getId());
    assertNull(form.getUserId());
    assertNull(form.getPassword());
    assertNull(form.getName());
    assertNull(form.getRole());
    assertNull(form.getGrade());
    assertNull(form.getClassId());
    assertNull(form.getNumber());
  }
}
