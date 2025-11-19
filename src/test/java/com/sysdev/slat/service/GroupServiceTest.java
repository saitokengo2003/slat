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
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
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

    // 256文字の文字列生成
    String longName = "a".repeat(256);
    assertFalse(target.validateName(longName));
  }

  // --- 2. createGroup ---

  @Test
  @DisplayName("createGroup: 正常系 (メンバーあり)")
  void testCreateGroupSuccess() {
    // 1. Ready
    List<String> members = List.of(MEMBER, "member2");

    // jdbc.update は成功すると更新件数(int)を返すが、戻り値を使わない実装なので設定不要(0が返る)
    // 必要なら doReturn(1).when(jdbc).update(...);

    // 2. Do
    boolean result = target.createGroup(OWNER, "New Group", members);

    // 3. Assert
    assertTrue(result);

    // SQL呼び出し検証
    // (1) グループ本体のINSERT (UUIDはランダム生成なので any() で受ける)
    verify(jdbc).update(contains("INSERT INTO group_s"), any(UUID.class), eq("New Group"), eq(OWNER));

    // (2) オーナーのINSERT
    verify(jdbc).update(contains("INSERT INTO group_members"), any(UUID.class), eq(OWNER));

    // (3) メンバーのバッチINSERT
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

    // メンバー追加の batchUpdate は呼ばれないはず
    verify(jdbc, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
  }

  @Test
  @DisplayName("createGroup: 異常系 (DBエラー発生)")
  void testCreateGroupDbError() {
    // 1. Ready
    // 最初のINSERTで例外を投げる
    doThrow(new DataAccessException("DB Error") {
    }).when(jdbc).update(contains("INSERT INTO group_s"), any(), any(), any());

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
    // メンバー削除(件数は問わない)
    doReturn(5).when(jdbc).update(contains("DELETE FROM group_members"), eq(TEST_UUID));
    // グループ削除(1件)
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
    doThrow(new DataAccessException("Delete Error") {
    }).when(jdbc).update(contains("DELETE FROM group_members"), eq(TEST_UUID));

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
  @DisplayName("deleteGroupMember: 失敗 (オーナー削除しようとした場合などはSQL条件で0件になる)")
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
  @DisplayName("addGroupMember: 失敗 (既に参加済みなどはSQL条件で0件になる)")
  void testAddGroupMemberFail() {
    // 1. Ready
    // ON CONFLICT DO NOTHING で0件更新になった場合
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
    doThrow(new DataAccessException("Insert Error") {
    }).when(jdbc).update(contains("INSERT INTO group_members"), any(), any());

    // 2. Do
    boolean result = target.addGroupMember(TEST_UUID, MEMBER);

    // 3. Assert
    assertFalse(result);
  }
}
