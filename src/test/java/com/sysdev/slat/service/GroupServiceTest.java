package com.sysdev.slat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sysdev.slat.GroupDetailDto;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

  @Mock
  private JdbcTemplate jdbc;

  @InjectMocks
  private GroupService target;

  // テスト用定数
  private final UUID TEST_UUID = UUID.randomUUID();
  private final String OWNER = "owner-user";
  private final String MEMBER = "member-user";

  // --- 1. validateName ---

  @Test
  @DisplayName("validateName: 正常系")
  void testValidateName() {
    assertTrue(target.validateName("Valid Name"));
    assertTrue(target.validateName("A"));
  }

  @Test
  @DisplayName("validateName: 異常系 (null, 空文字, 空白, 長すぎ)")
  void testValidateNameInvalid() {
    assertFalse(target.validateName(null));
    assertFalse(target.validateName(""));
    assertFalse(target.validateName("   "));

    String longName = "a".repeat(256);
    assertFalse(target.validateName(longName));
  }

  // --- 2. createGroup ---

  @Test
  @DisplayName("createGroup: 正常系 (メンバーあり)")
  void testCreateGroupSuccess() {
    // 1. Ready
    List<String> members = List.of(MEMBER, "member2");

    // 2. Do
    boolean result = target.createGroup(OWNER, "New Group", members);

    // 3. Assert
    assertTrue(result);

    // SQL呼び出し検証
    verify(jdbc).update(contains("INSERT INTO group_s"), any(UUID.class), eq("New Group"), eq(OWNER));
    verify(jdbc).update(contains("INSERT INTO group_members"), any(UUID.class), eq(OWNER));
    verify(jdbc).batchUpdate(contains("INSERT INTO group_members"), any(BatchPreparedStatementSetter.class));
  }

  @Test
  @DisplayName("createGroup: 正常系 (メンバーなし)")
  void testCreateGroupNoMembers() {
    // 1. Ready
    List<String> members = Collections.emptyList();

    // 2. Do
    boolean result = target.createGroup(OWNER, "Solo Group", members);

    // 3. Assert
    assertTrue(result);
    verify(jdbc, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
  }

  @Test
  @DisplayName("createGroup: 異常系 (DBエラー発生)")
  void testCreateGroupDbError() {
    // 1. Ready
    doThrow(new QueryTimeoutException("DB Error")).when(jdbc).update(contains("INSERT INTO group_s"), any(), any(),
        any());

    // 2. Do
    boolean result = target.createGroup(OWNER, "Error Group", List.of(MEMBER));

    // 3. Assert
    assertFalse(result);
  }

  // --- 3. getGroupDetail ---

  @Test
  @DisplayName("getGroupDetail: 正常系 (データあり)")
  void testGetGroupDetailFound() {
    // 1. Ready
    Map<String, Object> groupMap = Map.of("id", TEST_UUID, "name", "Test Group");
    List<Map<String, Object>> membersList = List.of(Map.of("user_id", OWNER));

    doReturn(groupMap).when(jdbc).queryForMap(anyString(), eq(TEST_UUID));
    doReturn(membersList).when(jdbc).queryForList(anyString(), eq(TEST_UUID));

    // 2. Do
    GroupDetailDto result = target.getGroupDetail(TEST_UUID);

    // 3. Assert
    assertNotNull(result);
    assertEquals(groupMap, result.getGroup());
    assertEquals(membersList, result.getMembers());
  }

  @Test
  @DisplayName("getGroupDetail: 異常系 (該当なし)")
  void testGetGroupDetailNotFound() {
    // 1. Ready
    doThrow(new EmptyResultDataAccessException(1)).when(jdbc).queryForMap(anyString(), eq(TEST_UUID));

    // 2. Do
    GroupDetailDto result = target.getGroupDetail(TEST_UUID);

    // 3. Assert
    assertNull(result);
  }

  // --- 4. findAllGroupsWithCounts ---

  @Test
  @DisplayName("findAllGroupsWithCounts: 正常系")
  void testFindAllGroupsWithCounts() {
    // 1. Ready
    List<Map<String, Object>> expectedList = new ArrayList<>();
    doReturn(expectedList).when(jdbc).queryForList(anyString());

    // 2. Do
    List<Map<String, Object>> result = target.findAllGroupsWithCounts();

    // 3. Assert
    assertSame(expectedList, result);
  }

  // --- 5. deleteGroup ---

  @Test
  @DisplayName("deleteGroup: 正常系")
  void testDeleteGroupSuccess() {
    // 1. Ready
    doReturn(5).when(jdbc).update(contains("DELETE FROM group_members"), eq(TEST_UUID));
    doReturn(1).when(jdbc).update(contains("DELETE FROM group_s"), eq(TEST_UUID));

    // 2. Do
    boolean result = target.deleteGroup(TEST_UUID);

    // 3. Assert
    assertTrue(result);
  }

  @Test
  @DisplayName("deleteGroup: 異常系 (グループが存在しない/削除件数0)")
  void testDeleteGroupFailZero() {
    // 1. Ready
    // 【修正】1回目の削除呼び出し（group_members）に対するスタブを追加
    doReturn(0).when(jdbc).update(contains("DELETE FROM group_members"), eq(TEST_UUID));

    // 2回目の削除呼び出し（group_s）に対するスタブ
    doReturn(0).when(jdbc).update(contains("DELETE FROM group_s"), eq(TEST_UUID));

    // 2. Do
    boolean result = target.deleteGroup(TEST_UUID);

    // 3. Assert
    assertFalse(result);
  }

  @Test
  @DisplayName("deleteGroup: 異常系 (DB例外)")
  void testDeleteGroupException() {
    // 1. Ready
    doThrow(new QueryTimeoutException("Delete Error")).when(jdbc).update(contains("DELETE FROM group_members"),
        eq(TEST_UUID));

    // 2. Do
    boolean result = target.deleteGroup(TEST_UUID);

    // 3. Assert
    assertFalse(result);
  }

  // --- 6. deleteGroupMember ---

  @Test
  @DisplayName("deleteGroupMember: 正常系")
  void testDeleteGroupMemberSuccess() {
    // 1. Ready
    doReturn(1).when(jdbc).update(contains("DELETE FROM group_members"), eq(TEST_UUID), eq(MEMBER));

    // 2. Do
    boolean result = target.deleteGroupMember(TEST_UUID, MEMBER);

    // 3. Assert
    assertTrue(result);
  }

  @Test
  @DisplayName("deleteGroupMember: 失敗")
  void testDeleteGroupMemberFail() {
    // 1. Ready
    doReturn(0).when(jdbc).update(contains("DELETE FROM group_members"), eq(TEST_UUID), eq(OWNER));

    // 2. Do
    boolean result = target.deleteGroupMember(TEST_UUID, OWNER);

    // 3. Assert
    assertFalse(result);
  }

  // --- 7. addGroupMember ---

  @Test
  @DisplayName("addGroupMember: 正常系")
  void testAddGroupMemberSuccess() {
    // 1. Ready
    doReturn(1).when(jdbc).update(contains("INSERT INTO group_members"), eq(TEST_UUID), eq(MEMBER));

    // 2. Do
    boolean result = target.addGroupMember(TEST_UUID, MEMBER);

    // 3. Assert
    assertTrue(result);
  }

  @Test
  @DisplayName("addGroupMember: 失敗 (既に参加済み)")
  void testAddGroupMemberFail() {
    // 1. Ready
    doReturn(0).when(jdbc).update(contains("INSERT INTO group_members"), eq(TEST_UUID), eq(MEMBER));

    // 2. Do
    boolean result = target.addGroupMember(TEST_UUID, MEMBER);

    // 3. Assert
    assertFalse(result);
  }

  @Test
  @DisplayName("addGroupMember: 異常系 (DB例外)")
  void testAddGroupMemberException() {
    // 1. Ready
    // 【修正】引数のマッチャーを any() ではなく any(Object.class) で明示
    doThrow(new QueryTimeoutException("Insert Error"))
        .when(jdbc).update(contains("INSERT INTO group_members"), any(Object.class), any(Object.class));

    // 2. Do
    boolean result = target.addGroupMember(TEST_UUID, MEMBER);

    // 3. Assert
    assertFalse(result);
  }
}
