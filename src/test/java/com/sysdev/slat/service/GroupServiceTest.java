package com.sysdev.slat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
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
  private GroupService service;

  @Test
  @DisplayName("validateName: 名前のバリデーション")
  void testValidateName() {
    assertTrue(service.validateName("abc"));
    assertFalse(service.validateName(""));
    assertFalse(service.validateName(null));
    assertTrue(service.validateName("x".repeat(255)));
    assertFalse(service.validateName("x".repeat(256)));
  }

  @Test
  @DisplayName("createGroup: 成功 (メンバーなし) - batchUpdate呼ばれない")
  void testCreateGroup_success_noMembers() {
    when(jdbc.update(startsWith("INSERT INTO group_s"), any(UUID.class), anyString(), anyString()))
        .thenReturn(1);

    when(jdbc.update(startsWith("INSERT INTO group_members"), any(UUID.class), anyString()))
        .thenReturn(1);

    boolean result = service.createGroup("owner", "group1", Collections.emptyList());
    assertTrue(result);

    verify(jdbc).update(contains("INSERT INTO group_s"), any(UUID.class), eq("group1"), eq("owner"));
    verify(jdbc).update(contains("INSERT INTO group_members"), any(UUID.class), eq("owner"));

    verify(jdbc, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
  }

  @Test
  @DisplayName("createGroup: 成功 (メンバーあり) - batchUpdate呼ばれる")
  void testCreateGroup_success_withMembers() {
    when(jdbc.update(startsWith("INSERT INTO group_s"), any(UUID.class), anyString(), anyString()))
        .thenReturn(1);

    when(jdbc.update(startsWith("INSERT INTO group_members"), any(UUID.class), anyString()))
        .thenReturn(1);

    doAnswer(inv -> {
      BatchPreparedStatementSetter setter = inv.getArgument(1);
      PreparedStatement ps = mock(PreparedStatement.class);
      int size = setter.getBatchSize();
      assertEquals(2, size);
      for (int i = 0; i < size; i++) {
        setter.setValues(ps, i);
      }
      return new int[] { 1, 1 };
    }).when(jdbc).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));

    boolean result = service.createGroup("owner", "g1", List.of("a", "b", "owner", "b"));
    assertTrue(result);

    verify(jdbc).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
  }

  @Test
  @DisplayName("createGroup: membersがnullの場合でも正常終了すること")
  void testCreateGroup_membersNull() {
    when(jdbc.update(startsWith("INSERT INTO group_s"), any(UUID.class), anyString(), anyString()))
        .thenReturn(1);
    when(jdbc.update(startsWith("INSERT INTO group_members"), any(UUID.class), anyString()))
        .thenReturn(1);

    boolean result = service.createGroup("owner", "group1", null);
    assertTrue(result);

    verify(jdbc, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
  }

  @Test
  @DisplayName("createGroup: membersにnullや空文字が含まれる場合")
  void testCreateGroup_membersWithNullAndBlank() {
    when(jdbc.update(startsWith("INSERT INTO group_s"), any(UUID.class), anyString(), anyString()))
        .thenReturn(1);
    when(jdbc.update(startsWith("INSERT INTO group_members"), any(UUID.class), anyString()))
        .thenReturn(1);

    doAnswer(inv -> {
      BatchPreparedStatementSetter setter = inv.getArgument(1);
      assertEquals(2, setter.getBatchSize());
      return new int[2];
    }).when(jdbc).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));

    List<String> dirtyMembers = Arrays.asList("a", null, "", "   ", "b");
    boolean result = service.createGroup("owner", "g", dirtyMembers);
    assertTrue(result);
  }

  @Test
  @DisplayName("createGroup: setValues 内で SQLException発生時は失敗")
  void testCreateGroup_setValues_exception() throws Exception {
    when(jdbc.update(startsWith("INSERT INTO group_s"), any(UUID.class), anyString(), anyString()))
        .thenReturn(1);

    when(jdbc.update(startsWith("INSERT INTO group_members"), any(UUID.class), anyString()))
        .thenReturn(1);

    PreparedStatement ps = mock(PreparedStatement.class);
    doThrow(new SQLException("ERR")).when(ps).setObject(anyInt(), any());

    when(jdbc.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
        .thenAnswer(inv -> {
          BatchPreparedStatementSetter setter = inv.getArgument(1);
          setter.setValues(ps, 0);
          return new int[] { 0 };
        });

    boolean result = service.createGroup("owner", "gX", List.of("m1"));
    assertFalse(result);
  }

  @Test
  @DisplayName("createGroup: DBエラー (Causeチェーンあり)")
  void testCreateGroup_dbError_withCauseChain() {
    Throwable c2 = new RuntimeException("cause2");
    Throwable c1 = new RuntimeException("cause1", c2);
    DataAccessException ex = new DataAccessException("top", c1) {
    };

    when(jdbc.update(anyString(), any(), any(), any()))
        .thenThrow(ex);

    boolean result = service.createGroup("owner", "g", List.of("a"));
    assertFalse(result);
  }

  @Test
  @DisplayName("createGroup: DBエラー (単発)")
  void testCreateGroup_dbError() {
    when(jdbc.update(anyString(), any(), any(), any()))
        .thenThrow(new DataAccessException("err") {
        });
    assertFalse(service.createGroup("owner", "g", List.of("x")));
  }

  @Test
  @DisplayName("createGroup: batchUpdateでDBエラー")
  void testCreateGroup_batchUpdate_dbError() {
    when(jdbc.update(startsWith("INSERT INTO group_s"), any(UUID.class), anyString(), anyString()))
        .thenReturn(1);
    when(jdbc.update(startsWith("INSERT INTO group_members"), any(UUID.class), anyString()))
        .thenReturn(1);

    when(jdbc.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
        .thenThrow(new DataAccessException("batch err") {
        });

    assertFalse(service.createGroup("owner", "groupX", List.of("a", "b")));
  }

  @Test
  @DisplayName("getGroupDetail: 正常取得")
  void testGetGroupDetail_found() {
    UUID gid = UUID.randomUUID();

    when(jdbc.queryForMap(anyString(), eq(gid)))
        .thenReturn(Map.of("id", gid, "name", "g"));

    when(jdbc.queryForList(anyString(), eq(gid)))
        .thenReturn(List.of(Map.of("user_id", "u1")));

    GroupDetailDto dto = service.getGroupDetail(gid);
    assertNotNull(dto);
    assertEquals(gid, dto.getGroup().get("id"));
  }

  @Test
  @DisplayName("getGroupDetail: 存在しない場合 (null返却)")
  void testGetGroupDetail_notFound() {
    UUID gid = UUID.randomUUID();
    when(jdbc.queryForMap(anyString(), eq(gid)))
        .thenThrow(new EmptyResultDataAccessException(1));

    assertNull(service.getGroupDetail(gid));
  }

  @Test
  @DisplayName("findAllGroupsWithCounts: 正常取得")
  void testFindAllGroupsWithCounts() {
    when(jdbc.queryForList(anyString()))
        .thenReturn(List.of(Map.of("id", 1)));

    List<Map<String, Object>> result = service.findAllGroupsWithCounts();
    assertEquals(1, result.size());
  }

  @Test
  @DisplayName("deleteGroup: 成功")
  void testDeleteGroup_success() {
    UUID gid = UUID.randomUUID();

    when(jdbc.update(startsWith("DELETE FROM group_members"), any(UUID.class)))
        .thenReturn(5);
    when(jdbc.update(startsWith("DELETE FROM group_s"), any(UUID.class)))
        .thenReturn(1);

    assertTrue(service.deleteGroup(gid));
  }

  @Test
  @DisplayName("deleteGroup: 削除対象なし (0件削除)")
  void testDeleteGroup_notFound() {
    UUID gid = UUID.randomUUID();
    when(jdbc.update(startsWith("DELETE FROM group_members"), any(UUID.class)))
        .thenReturn(0);
    when(jdbc.update(startsWith("DELETE FROM group_s"), any(UUID.class)))
        .thenReturn(0);

    assertFalse(service.deleteGroup(gid));
  }

  @Test
  @DisplayName("deleteGroup: DBエラー")
  void testDeleteGroup_dbError() {
    when(jdbc.update(startsWith("DELETE FROM group_members"), any(UUID.class)))
        .thenThrow(new DataAccessException("err") {
        });
    assertFalse(service.deleteGroup(UUID.randomUUID()));
  }

  @Test
  @DisplayName("deleteGroupMember: 成功")
  void testDeleteGroupMember_success() {
    UUID gid = UUID.randomUUID();
    when(jdbc.update(anyString(), any(UUID.class), anyString()))
        .thenReturn(1);
    assertTrue(service.deleteGroupMember(gid, "u1"));
  }

  @Test
  @DisplayName("deleteGroupMember: 削除対象なし (0件削除)")
  void testDeleteGroupMember_notFound() {
    UUID gid = UUID.randomUUID();
    when(jdbc.update(anyString(), any(UUID.class), anyString()))
        .thenReturn(0);
    assertFalse(service.deleteGroupMember(gid, "u1"));
  }

  @Test
  @DisplayName("deleteGroupMember: DBエラー")
  void testDeleteGroupMember_fail() {
    UUID gid = UUID.randomUUID();
    when(jdbc.update(anyString(), any(UUID.class), anyString()))
        .thenThrow(new DataAccessException("err") {
        });
    assertFalse(service.deleteGroupMember(gid, "u1"));
  }

  @Test
  @DisplayName("addGroupMember: 成功")
  void testAddGroupMember_success() {
    UUID gid = UUID.randomUUID();
    when(jdbc.update(anyString(), any(UUID.class), anyString()))
        .thenReturn(1);
    assertTrue(service.addGroupMember(gid, "u1"));
  }

  @Test
  @DisplayName("addGroupMember: 競合/追加なし (0件挿入)")
  void testAddGroupMember_conflict() {
    UUID gid = UUID.randomUUID();
    when(jdbc.update(anyString(), any(UUID.class), anyString()))
        .thenReturn(0);
    assertFalse(service.addGroupMember(gid, "u1"));
  }

  @Test
  @DisplayName("addGroupMember: DBエラー (Causeあり)")
  void testAddGroupMember_dbError_withCause() {
    Throwable c1 = new IllegalStateException("L2");
    DataAccessException ex = new DataAccessException("xyz", c1) {
    };
    when(jdbc.update(anyString(), any(UUID.class), anyString()))
        .thenThrow(ex);
    assertFalse(service.addGroupMember(UUID.randomUUID(), "u1"));
  }

  @Test
  @DisplayName("addGroupMember: DBエラー (単発)")
  void testAddGroupMember_dbError() {
    when(jdbc.update(anyString(), any(UUID.class), anyString()))
        .thenThrow(new DataAccessException("err") {
        });
    assertFalse(service.addGroupMember(UUID.randomUUID(), "u1"));
  }
}
