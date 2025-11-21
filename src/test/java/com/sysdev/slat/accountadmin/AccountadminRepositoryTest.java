package com.sysdev.slat.accountadmin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AccountadminRepositoryTest {

  @Mock
  private NamedParameterJdbcTemplate jdbc;

  @InjectMocks
  private AccountadminRepository target;

  @Test
  @DisplayName("全件取得: 正常系 (RowMapperが呼ばれるフローの確認)")
  void testFindAllActiveAccounts() {
    // 1. Ready
    AccountadminData dummyData = new AccountadminData();
    dummyData.setUsername("test_user");
    List<AccountadminData> expectedList = List.of(dummyData);
    doReturn(expectedList).when(jdbc).query(anyString(), any(Map.class), any(RowMapper.class));

    // 2. Do
    List<AccountadminData> result = target.findAllActiveAccounts();

    // 3. Assert
    assertEquals(1, result.size());
    assertEquals("test_user", result.get(0).getUsername());
    verify(jdbc).query(contains("SELECT \"id\""), eq(Collections.emptyMap()), any(RowMapper.class));
  }

  @Test
  @DisplayName("削除: 正常系 (1件削除)")
  void testDeleteSuccess() throws SQLException {
    // 1. Ready
    String targetId = "uuid-1234";
    doReturn(1).when(jdbc).update(anyString(), any(Map.class));

    // 2. Do
    int result = target.delete(targetId);

    // 3. Assert
    assertEquals(1, result);
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(jdbc).update(contains("DELETE FROM"), captor.capture());
    Map<String, Object> params = captor.getValue();
    assertEquals(targetId, params.get("id"));
  }

  @Test
  @DisplayName("削除: 失敗 (対象なし/0件削除)")
  void testDeleteFail() {
    // 1. Ready
    String targetId = "uuid-not-exists";
    doReturn(0).when(jdbc).update(anyString(), any(Map.class));

    // 2. Do & 3. Assert
    SQLException e = assertThrows(SQLException.class, () -> target.delete(targetId));
    assertTrue(e.getMessage().contains("失敗しました"));
  }

  @Test
  @DisplayName("登録: 正常系 (1件登録)")
  void testInsertSuccess() throws SQLException {
    // 1. Ready
    AccountadminData data = new AccountadminData();
    data.setUsername("new_user");
    data.setPassword_hash("hash_pass");
    data.setDisplay_name("新規 太郎");
    data.setRole_code("STUDENT");
    data.setGrade(1);
    data.setClass_name("A");
    data.setNumber(10);
    doReturn(1).when(jdbc).update(anyString(), any(Map.class));

    // 2. Do
    int result = target.insert(data);

    // 3. Assert
    assertEquals(1, result);

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(jdbc).update(contains("INSERT INTO"), captor.capture());
    Map<String, Object> params = captor.getValue();
    assertEquals("new_user", params.get("username"));
    assertEquals("hash_pass", params.get("password_hash"));
    assertEquals(1, params.get("grade"));
  }

  @Test
  @DisplayName("登録: 失敗 (何らかの理由で0件更新)")
  void testInsertFail() {
    // 1. Ready
    AccountadminData data = new AccountadminData();
    data.setUsername("fail_user");
    doReturn(0).when(jdbc).update(anyString(), any(Map.class));

    // 2. Do & 3. Assert
    SQLException e = assertThrows(SQLException.class, () -> target.insert(data));
    assertTrue(e.getMessage().contains("失敗しました"));
  }

  @Test
  @DisplayName("RowMapper: データマッピングのテスト (全項目あり)")
  void testRowMapperMapping() throws SQLException {
    // 1. Ready
    ResultSet rs = mock(ResultSet.class);
    doReturn("uuid-001").when(rs).getString("id");
    doReturn("user_test").when(rs).getString("username");
    doReturn("hashed_pass").when(rs).getString("password_hash");
    doReturn("active").when(rs).getString("status");
    doReturn("太郎").when(rs).getString("display_name");
    doReturn("STUDENT").when(rs).getString("role_code");
    doReturn("A").when(rs).getString("class_name");

    OffsetDateTime now = OffsetDateTime.now();
    doReturn(now).when(rs).getObject("created_at", OffsetDateTime.class);
    doReturn(now).when(rs).getObject("updated_at", OffsetDateTime.class);
    doReturn(now).when(rs).getObject("last_login_at", OffsetDateTime.class);

    doReturn(2).when(rs).getInt("grade");
    doReturn(15).when(rs).getInt("number");
    doReturn(false).when(rs).wasNull();

    RowMapper<AccountadminData> rowMapper = new AccountadminRepository.AccountadminDataRowMapper();

    // 2. Do
    AccountadminData data = rowMapper.mapRow(rs, 1);

    // 3. Assert
    assertEquals("uuid-001", data.getId());
    assertEquals("user_test", data.getUsername());
    assertEquals(now, data.getCreated_at());
    assertEquals(2, data.getGrade());
    assertEquals(15, data.getNumber());
  }

  @Test
  @DisplayName("RowMapper: 数値型がNULLの場合のテスト")
  void testRowMapperNullHandling() throws SQLException {
    // 1. Ready
    ResultSet rs = mock(ResultSet.class);
    doReturn("uuid-002").when(rs).getString("id");
    doReturn(0).when(rs).getInt("grade");
    doReturn(0).when(rs).getInt("number");
    doReturn(true).when(rs).wasNull();

    RowMapper<AccountadminData> rowMapper = new AccountadminRepository.AccountadminDataRowMapper();

    // 2. Do
    AccountadminData data = rowMapper.mapRow(rs, 1);

    // 3. Assert
    assertNull(data.getGrade());
    assertNull(data.getNumber());
  }
}
