package com.sysdev.slat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GroupDetailDtoTest {

  @Test
  @DisplayName("正常系: コンストラクタで設定した値がGetterで取得できること")
  void testConstructorAndGetters() {
    // 1. Ready
    // テストデータの作成
    Map<String, Object> groupData = new HashMap<>();
    groupData.put("groupId", 100);
    groupData.put("groupName", "テストグループ");

    List<Map<String, Object>> memberList = new ArrayList<>();
    Map<String, Object> member1 = new HashMap<>();
    member1.put("userId", "user1");
    member1.put("name", "メンバー１");
    memberList.add(member1);

    // 2. Do
    GroupDetailDto dto = new GroupDetailDto(groupData, memberList);

    // 3. Assert
    assertEquals(groupData, dto.getGroup());
    assertEquals(memberList, dto.getMembers());
    assertSame(groupData, dto.getGroup());
    assertSame(memberList, dto.getMembers());
  }

  @Test
  @DisplayName("異常系: 引数がnullの場合でもインスタンス化でき、nullが返却されること")
  void testNullValues() {
    // 1. Ready & 2. Do
    GroupDetailDto dto = new GroupDetailDto(null, null);

    // 3. Assert
    assertNull(dto.getGroup());
    assertNull(dto.getMembers());
  }

  @Test
  @DisplayName("境界値: 空のMapとListの場合")
  void testEmptyValues() {
    // 1. Ready
    Map<String, Object> emptyGroup = Collections.emptyMap();
    List<Map<String, Object>> emptyMembers = Collections.emptyList();

    // 2. Do
    GroupDetailDto dto = new GroupDetailDto(emptyGroup, emptyMembers);

    // 3. Assert
    assertEquals(0, dto.getGroup().size());
    assertEquals(0, dto.getMembers().size());
  }
}
