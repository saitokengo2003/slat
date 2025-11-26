package com.sysdev.slat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor; // ★追加
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sysdev.slat.GroupDetailDto;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

  @Mock
  private JdbcTemplate jdbc;

  @InjectMocks
  private GroupService target;

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
    List<String> members = List.of(MEMBER, "member2");

    // batchUpdateが呼ばれたら、引数のcallbackを実行してカバレッジを稼ぐ
    doAnswer(invocation -> {
      BatchPreparedStatementSetter setter = invocation.getArgument(1);
      PreparedStatement mockPs = mock(PreparedStatement.class);

      // 内部ロジック実行 (setValues, getBatchSize)
      int size = setter.getBatchSize();
      for (int i = 0; i < size; i++) {
        setter.setValues(mockPs, i);
      }
      return new int[] { 1, 1 };
    }).when(jdbc).batchUpdate(contains("INSERT INTO group_members"), any(BatchPreparedStatementSetter.class));

    boolean result = target.createGroup(OWNER, "New Group", members);

    assertTrue(result);
    verify(jdbc).update(contains("INSERT INTO group_s"), any(UUID.class), eq("New Group"), eq(OWNER));
  }

  // ★追加: フィルタリングロジック（null, 空白, オーナー除外）の完全網羅
  @Test
  @DisplayName("createGroup: メンバーのフィルタリング (null, 空白, オーナー, 重複を除外)")
  void testCreateGroup_MemberFiltering() {
    // 1. Ready: 汚いデータを用意
    List<String> members = new ArrayList<>();
    members.add(null); // null -> フィルタで除外
    members.add(""); // 空文字 -> フィルタで除外
    members.add("   "); // 空白のみ -> フィルタで除外
    members.add(OWNER); // オーナー -> フィルタで除外
    members.add("duplicate"); // 重複1
    members.add("duplicate"); // 重複2 -> distinctで除外
    members.add("valid"); // 有効 -> 残る

    // 2. Do
    boolean result = target.createGroup(OWNER, "Group", members);

    // 3. Assert
    assertTrue(result);

    // batchUpdateに渡されたSetterを捕獲して、サイズを確認する
    ArgumentCaptor<BatchPreparedStatementSetter> captor = ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
    verify(jdbc).batchUpdate(contains("INSERT INTO group_members"), captor.capture());

    BatchPreparedStatementSetter setter = captor.getValue();

    // 有効なのは "duplicate" と "valid" の2つだけのはず
    assertEquals(2, setter.getBatchSize());
  }

  @Test
  @DisplayName("createGroup: 正常系 (メンバーなし)")
  void testCreateGroupNoMembers() {
    List<String> members = Collections.emptyList();
    boolean result = target.createGroup(OWNER, "Solo Group", members);
    assertTrue(result);
    verify(jdbc, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
  }

  @Test
  @DisplayName("createGroup: 異常系 (DBエラー発生)")
  void testCreateGroupDbError() {
    doThrow(new QueryTimeoutException("DB Error")).when(jdbc).update(contains("INSERT INTO group_s"), any(), any(),
        any());
    boolean result = target.createGroup(OWNER, "Error Group", List.of(MEMBER));
    assertFalse(result);
  }

  @Test
  @DisplayName("createGroup: DB例外発生時に原因(cause)も出力されるルート")
  void testCreateGroup_dbError_withCauseChain() {
    SQLException rootCause = new SQLException("Root Cause");
    DataAccessException ex = new QueryTimeoutException("Top Level Error", rootCause);
    doThrow(ex).when(jdbc).update(contains("INSERT INTO group_s"), any(), any(), any());
    boolean result = target.createGroup(OWNER, "Error Group", List.of(MEMBER));
    assertFalse(result);
  }

  @Test
  @DisplayName("createGroup: バッチ更新中のSQLException (setValues内部)")
  void testCreateGroup_setValues_exception() {
    List<String> members = List.of(MEMBER);

    doAnswer(invocation -> {
      BatchPreparedStatementSetter setter = invocation.getArgument(1);
      PreparedStatement mockPs = mock(PreparedStatement.class);
      lenient().doThrow(new SQLException("ERR")).when(mockPs).setObject(anyInt(), any());
      lenient().doThrow(new SQLException("ERR")).when(mockPs).setString(anyInt(), anyString());

      try {
        setter.setValues(mockPs, 0);
      } catch (SQLException e) {
        throw new UncategorizedSQLException("test", "sql", e);
      }
      return null;
    }).when(jdbc).batchUpdate(contains("INSERT INTO group_members"), any(BatchPreparedStatementSetter.class));

    boolean result = target.createGroup(OWNER, "Group", members);
    assertFalse(result);
  }

  // --- 3. getGroupDetail ---

  @Test
  @DisplayName("getGroupDetail: 正常系 (データあり)")
  void testGetGroupDetailFound() {
    Map<String, Object> groupMap = Map.of("id", TEST_UUID, "name", "Test Group");
    List<Map<String, Object>> membersList = List.of(Map.of("user_id", OWNER));

    doReturn(groupMap).when(jdbc).queryForMap(anyString(), eq(TEST_UUID));
    doReturn(membersList).when(jdbc).queryForList(anyString(), eq(TEST_UUID));

    GroupDetailDto result = target.getGroupDetail(TEST_UUID);

    assertNotNull(result);
    assertEquals(groupMap, result.getGroup());
    assertEquals(membersList, result.getMembers());
  }

  @Test
  @DisplayName("getGroupDetail: 異常系 (該当なし)")
  void testGetGroupDetailNotFound() {
    doThrow(new EmptyResultDataAccessException(1)).when(jdbc).queryForMap(anyString(), eq(TEST_UUID));
    GroupDetailDto result = target.getGroupDetail(TEST_UUID);
    assertNull(result);
  }

  // --- 4. findAllGroupsWithCounts ---

  @Test
  @DisplayName("findAllGroupsWithCounts: 正常系")
  void testFindAllGroupsWithCounts() {
    List<Map<String, Object>> expectedList = new ArrayList<>();
    doReturn(expectedList).when(jdbc).queryForList(anyString());
    List<Map<String, Object>> result = target.findAllGroupsWithCounts();
    assertSame(expectedList, result);
  }

  // --- 5. deleteGroup ---

  @Test
  @DisplayName("deleteGroup: 正常系")
  void testDeleteGroupSuccess() {
    doReturn(5).when(jdbc).update(contains("DELETE FROM group_members"), eq(TEST_UUID));
    doReturn(1).when(jdbc).update(contains("DELETE FROM group_s"), eq(TEST_UUID));
    boolean result = target.deleteGroup(TEST_UUID);
    assertTrue(result);
  }

  @Test
  @DisplayName("deleteGroup: 異常系 (グループが存在しない/削除件数0)")
  void testDeleteGroupFailZero() {
    doReturn(0).when(jdbc).update(contains("DELETE FROM group_members"), eq(TEST_UUID));
    doReturn(0).when(jdbc).update(contains("DELETE FROM group_s"), eq(TEST_UUID));
    boolean result = target.deleteGroup(TEST_UUID);
    assertFalse(result);
  }

  @Test
  @DisplayName("deleteGroup: 異常系 (DB例外)")
  void testDeleteGroupException() {
    doThrow(new QueryTimeoutException("Delete Error")).when(jdbc).update(contains("DELETE FROM group_members"),
        eq(TEST_UUID));
    boolean result = target.deleteGroup(TEST_UUID);
    assertFalse(result);
  }

  // --- 6. deleteGroupMember ---

  @Test
  @DisplayName("deleteGroupMember: 正常系")
  void testDeleteGroupMemberSuccess() {
    doReturn(1).when(jdbc).update(contains("DELETE FROM group_members"), eq(TEST_UUID), eq(MEMBER));
    boolean result = target.deleteGroupMember(TEST_UUID, MEMBER);
    assertTrue(result);
  }

  @Test
  @DisplayName("deleteGroupMember: 失敗")
  void testDeleteGroupMemberFail() {
    doReturn(0).when(jdbc).update(contains("DELETE FROM group_members"), eq(TEST_UUID), eq(OWNER));
    boolean result = target.deleteGroupMember(TEST_UUID, OWNER);
    assertFalse(result);
  }

  @Test
  @DisplayName("deleteGroupMember: 異常系 (DB例外)")
  void testDeleteGroupMemberException() {
    doThrow(new QueryTimeoutException("Error")).when(jdbc).update(contains("DELETE FROM group_members"),
        any(Object.class), any(Object.class));
    assertFalse(target.deleteGroupMember(TEST_UUID, MEMBER));
  }

  // --- 7. addGroupMember ---

  @Test
  @DisplayName("addGroupMember: 正常系")
  void testAddGroupMemberSuccess() {
    doReturn(1).when(jdbc).update(contains("INSERT INTO group_members"), eq(TEST_UUID), eq(MEMBER));
    boolean result = target.addGroupMember(TEST_UUID, MEMBER);
    assertTrue(result);
  }

  @Test
  @DisplayName("addGroupMember: 失敗 (既に参加済み)")
  void testAddGroupMemberFail() {
    doReturn(0).when(jdbc).update(contains("INSERT INTO group_members"), eq(TEST_UUID), eq(MEMBER));
    boolean result = target.addGroupMember(TEST_UUID, MEMBER);
    assertFalse(result);
  }

  @Test
  @DisplayName("addGroupMember: 異常系 (DB例外)")
  void testAddGroupMemberException() {
    doThrow(new QueryTimeoutException("Insert Error"))
        .when(jdbc).update(contains("INSERT INTO group_members"), any(Object.class), any(Object.class));
    boolean result = target.addGroupMember(TEST_UUID, MEMBER);
    assertFalse(result);
  }

  @Test
  @DisplayName("addGroupMember: DB例外発生時に原因(cause)も出力されるルート")
  void testAddGroupMember_dbError_withCause() {
    SQLException rootCause = new SQLException("Root Cause");
    DataAccessException ex = new QueryTimeoutException("Top Level Error", rootCause);
    doThrow(ex).when(jdbc).update(contains("INSERT INTO group_members"), any(Object.class), any(Object.class));
    boolean result = target.addGroupMember(TEST_UUID, MEMBER);
    assertFalse(result);
  }
}
