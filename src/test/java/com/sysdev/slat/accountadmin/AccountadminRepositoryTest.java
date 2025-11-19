package com.sysdev.slat.accountadmin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    // モックが返すダミーデータ
    AccountadminData dummyData = new AccountadminData();
    dummyData.setUsername("test_user");
    List<AccountadminData> expectedList = List.of(dummyData);

    // jdbc.query が呼ばれたら、上記のリストを返すように設定
    // (RowMapperの実装自体はモック内では実行されませんが、引数として渡されることは確認できます)
    doReturn(expectedList).when(jdbc).query(anyString(), any(Map.class), any(RowMapper.class));

    // 2. Do
    List<AccountadminData> result = target.findAllActiveAccounts();

    // 3. Assert
    assertEquals(1, result.size());
    assertEquals("test_user", result.get(0).getUsername());

    // 正しいSQL（の一部）が呼ばれているか確認
    verify(jdbc).query(contains("SELECT \"id\""), eq(Collections.emptyMap()), any(RowMapper.class));
  }

  @Test
  @DisplayName("削除: 正常系 (1件削除)")
  void testDeleteSuccess() throws SQLException {
    // 1. Ready
    String targetId = "uuid-1234";
    // 更新件数 1 を返す
    doReturn(1).when(jdbc).update(anyString(), any(Map.class));

    // 2. Do
    int result = target.delete(targetId);

    // 3. Assert
    assertEquals(1, result);

    // パラメータが正しく渡されたか検証
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
    // 更新件数 0 を返す
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

    // 更新件数 1 を返す
    doReturn(1).when(jdbc).update(anyString(), any(Map.class));

    // 2. Do
    int result = target.insert(data);

    // 3. Assert
    assertEquals(1, result);

    // パラメータマップの中身を詳細に検証
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(jdbc).update(contains("INSERT INTO"), captor.capture());

    Map<String, Object> params = captor.getValue();
    assertEquals("new_user", params.get("username"));
    assertEquals("hash_pass", params.get("password_hash"));
    assertEquals("新規 太郎", params.get("display_name"));
    assertEquals("STUDENT", params.get("role_code"));
    assertEquals(1, params.get("grade"));
    assertEquals("A", params.get("class_name"));
    assertEquals(10, params.get("number"));
  }

  @Test
  @DisplayName("登録: 失敗 (何らかの理由で0件更新)")
  void testInsertFail() {
    // 1. Ready
    AccountadminData data = new AccountadminData();
    data.setUsername("fail_user");

    // 更新件数 0 を返す
    doReturn(0).when(jdbc).update(anyString(), any(Map.class));

    // 2. Do & 3. Assert
    SQLException e = assertThrows(SQLException.class, () -> target.insert(data));
    assertTrue(e.getMessage().contains("失敗しました"));
  }
}
