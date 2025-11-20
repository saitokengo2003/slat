package com.sysdev.slat.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.*;
import com.sysdev.slat.GroupDetailDto;

class GroupServiceTest {

  @Mock
  private JdbcTemplate jdbc;

  @InjectMocks
  private GroupService service;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  // --------------------------------------------------------
  // validateName
  // --------------------------------------------------------
  @Test
  void testValidateName() {
    assertTrue(service.validateName("abc"));
    assertFalse(service.validateName(""));
    assertFalse(service.validateName(null));
    assertTrue(service.validateName("x".repeat(255)));
    assertFalse(service.validateName("x".repeat(256)));
  }

  // --------------------------------------------------------
  // createGroup: success no members（batchUpdate が呼ばれない）
  // --------------------------------------------------------
  @Test
  void testCreateGroup_success_noMembers() {

    when(jdbc.update(startsWith("INSERT INTO group_s"), any(), any(), any()))
        .thenReturn(1);

    when(jdbc.update(startsWith("INSERT INTO group_members"), any(), any()))
        .thenReturn(1);

    boolean result = service.createGroup("owner", "group1", Collections.emptyList());
    assertTrue(result);

    verify(jdbc).update(contains("INSERT INTO group_s"), any(), eq("group1"), eq("owner"));
    verify(jdbc).update(contains("INSERT INTO group_members"), any(), eq("owner"));

    // batchUpdate が呼ばれないこと
    verify(jdbc, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
  }

  // --------------------------------------------------------
  // createGroup: success with members
  // --------------------------------------------------------
  @Test
  void testCreateGroup_success_withMembers() throws Exception {

    when(jdbc.update(startsWith("INSERT INTO group_s"), any(), any(), any()))
        .thenReturn(1);

    when(jdbc.update(startsWith("INSERT INTO group_members"), any(), any()))
        .thenReturn(1);

    // batchUpdate の擬似実行
    doAnswer(inv -> {
      BatchPreparedStatementSetter setter = inv.getArgument(1);
      PreparedStatement ps = mock(PreparedStatement.class);
      int size = setter.getBatchSize();
      assertEquals(2, size);
      for (int i = 0; i < size; i++)
        setter.setValues(ps, i);
      return 1;
    }).when(jdbc).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));

    boolean result = service.createGroup("owner", "g1", List.of("a", "b", "owner", "b"));
    assertTrue(result);

    verify(jdbc).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
  }

  // --------------------------------------------------------
  // createGroup: setValues 内で SQLException → false
  // --------------------------------------------------------
  @Test
  void testCreateGroup_setValues_exception() throws Exception {

    when(jdbc.update(startsWith("INSERT INTO group_s"), any(), any(), any()))
        .thenReturn(1);

    when(jdbc.update(startsWith("INSERT INTO group_members"), any(), any()))
        .thenReturn(1);

    // setValues 内で例外を発生させる PreparedStatement
    PreparedStatement ps = mock(PreparedStatement.class);
    doThrow(new SQLException("ERR")).when(ps).setObject(anyInt(), any());
    doThrow(new SQLException("ERR")).when(ps).setString(anyInt(), anyString());

    when(jdbc.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
        .thenAnswer(inv -> {
          BatchPreparedStatementSetter setter = inv.getArgument(1);
          setter.setValues(ps, 0); // 例外発生
          return 1;
        });

    boolean result = service.createGroup("owner", "gX", List.of("m1"));
    assertFalse(result);
  }

  // --------------------------------------------------------
  // createGroup: DB error with cause chain（2段階 cause）
  // --------------------------------------------------------
  @Test
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

  // --------------------------------------------------------
  // createGroup: normal DB error
  // --------------------------------------------------------
  @Test
  void testCreateGroup_dbError() {
    when(jdbc.update(anyString(), any(), any(), any()))
        .thenThrow(mock(DataAccessException.class));
    assertFalse(service.createGroup("owner", "g", List.of("x")));
  }

  // --------------------------------------------------------
  // batchUpdate エラー → false
  // --------------------------------------------------------
  @Test
  void testCreateGroup_batchUpdate_dbError() {

    when(jdbc.update(startsWith("INSERT INTO group_s"), any(), any(), any()))
        .thenReturn(1);

    when(jdbc.update(startsWith("INSERT INTO group_members"), any(), any()))
        .thenReturn(1);

    when(jdbc.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
        .thenThrow(new DataAccessException("batch err") {
        });

    assertFalse(service.createGroup("owner", "groupX", List.of("a", "b")));
  }

  // --------------------------------------------------------
  // getGroupDetail: found
  // --------------------------------------------------------
  @Test
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

  // --------------------------------------------------------
  // getGroupDetail: not found
  // --------------------------------------------------------
  @Test
  void testGetGroupDetail_notFound() {
    UUID gid = UUID.randomUUID();

    when(jdbc.queryForMap(anyString(), eq(gid)))
        .thenThrow(new EmptyResultDataAccessException(1));

    assertNull(service.getGroupDetail(gid));
  }

  // --------------------------------------------------------
  // findAllGroupsWithCounts
  // --------------------------------------------------------
  @Test
  void testFindAllGroupsWithCounts() {
    when(jdbc.queryForList(anyString()))
        .thenReturn(List.of(Map.of("id", 1)));

    List<Map<String, Object>> result = service.findAllGroupsWithCounts();
    assertEquals(1, result.size());
  }

  // --------------------------------------------------------
  // deleteGroup: success
  // --------------------------------------------------------
  @Test
  void testDeleteGroup_success() {
    UUID gid = UUID.randomUUID();

    when(jdbc.update(startsWith("DELETE FROM group_members"), eq(gid)))
        .thenReturn(5);

    when(jdbc.update(startsWith("DELETE FROM group_s"), eq(gid)))
        .thenReturn(1);

    assertTrue(service.deleteGroup(gid));
  }

  // --------------------------------------------------------
  // deleteGroup: dbError
  // --------------------------------------------------------
  @Test
  void testDeleteGroup_dbError() {
    when(jdbc.update(anyString(), any(UUID.class)))
        .thenThrow(new DataAccessException("err") {
        });
    assertFalse(service.deleteGroup(UUID.randomUUID()));
  }

  // --------------------------------------------------------
  // deleteGroupMember: success
  // --------------------------------------------------------
  @Test
  void testDeleteGroupMember_success() {
    UUID gid = UUID.randomUUID();
    when(jdbc.update(anyString(), any(), any()))
        .thenReturn(1);
    assertTrue(service.deleteGroupMember(gid, "u1"));
  }

  // --------------------------------------------------------
  // deleteGroupMember: fail
  // --------------------------------------------------------
  @Test
  void testDeleteGroupMember_fail() {
    UUID gid = UUID.randomUUID();
    when(jdbc.update(anyString(), any(), any()))
        .thenThrow(mock(DataAccessException.class));
    assertFalse(service.deleteGroupMember(gid, "u1"));
  }

  // --------------------------------------------------------
  // addGroupMember: success
  // --------------------------------------------------------
  @Test
  void testAddGroupMember_success() {
    UUID gid = UUID.randomUUID();
    when(jdbc.update(anyString(), any(), any()))
        .thenReturn(1);
    assertTrue(service.addGroupMember(gid, "u1"));
  }

  // --------------------------------------------------------
  // addGroupMember: conflict (inserted=0)
  // --------------------------------------------------------
  @Test
  void testAddGroupMember_conflict() {
    UUID gid = UUID.randomUUID();
    when(jdbc.update(anyString(), any(), any()))
        .thenReturn(0);
    assertFalse(service.addGroupMember(gid, "u1"));
  }

  // --------------------------------------------------------
  // addGroupMember: dbError with cause
  // --------------------------------------------------------
  @Test
  void testAddGroupMember_dbError_withCause() {
    Throwable c1 = new IllegalStateException("L2");
    DataAccessException ex = new DataAccessException("xyz", c1) {
    };
    when(jdbc.update(anyString(), any(), any()))
        .thenThrow(ex);
    assertFalse(service.addGroupMember(UUID.randomUUID(), "u1"));
  }

  // --------------------------------------------------------
  // addGroupMember: normal dbError
  // --------------------------------------------------------
  @Test
  void testAddGroupMember_dbError() {
    when(jdbc.update(anyString(), any(), any()))
        .thenThrow(mock(DataAccessException.class));
    assertFalse(service.addGroupMember(UUID.randomUUID(), "u1"));
  }
}
