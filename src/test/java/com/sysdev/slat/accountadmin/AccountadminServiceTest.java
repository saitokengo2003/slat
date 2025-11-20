package com.sysdev.slat.accountadmin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import com.sysdev.slat.user.User;
import com.sysdev.slat.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AccountadminServiceTest {

  @Mock
  private AccountadminRepository accountadminRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private AccountadminService target;

  private final String VALID_UUID_STR = "550e8400-e29b-41d4-a716-446655440000";

  // -------------------------------------------------------
  // テストケース
  // -------------------------------------------------------

  @Test
  @DisplayName("アカウント一覧取得(Entity): 正常系")
  void testGetAccountListEntity() {
    // 1. Ready
    User user1 = new User();
    user1.setUsername("user1");
    User user2 = new User();
    user2.setUsername("user2");

    doReturn(List.of(user1, user2)).when(userRepository).findAll();

    // 2. Do
    AccountadminEntity result = target.getAccountListEntity();

    // 3. Assert
    assertNotNull(result);
    assertEquals(2, result.getAccountList().size());
    assertEquals("user1", result.getAccountList().get(0).getUsername());
  }

  @Test
  @DisplayName("アカウント削除: 正常系")
  void testDeleteAccount() throws SQLException {
    // 1. Ready
    String accountId = "user123";

    // 【修正】deleteメソッドはintを返すため、doNothing()ではなくdoReturn(1)を使用
    doReturn(1).when(accountadminRepository).delete(accountId);

    // 2. Do
    target.deleteAccount(accountId);

    // 3. Assert
    verify(accountadminRepository, times(1)).delete(accountId);
  }

  @Test
  @DisplayName("アカウント作成: 正常系（全項目あり）")
  void testCreateAccount() {
    // 1. Ready
    AccountForm form = new AccountForm();
    form.setUserId("new_user");
    form.setPassword("password123");
    form.setName("新規 太郎");
    form.setRole("STUDENT");
    form.setGrade("1");
    form.setClassId("A");
    form.setNumber(10); // Integer型

    // 2. Do
    target.createAccount(form);

    // 3. Assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertEquals("new_user", savedUser.getUsername());
    assertEquals("新規 太郎", savedUser.getDisplayName());
    assertEquals(1, savedUser.getGrade());
    assertEquals("active", savedUser.getStatus());
    assertNotNull(savedUser.getCreatedAt());
  }

  @Test
  @DisplayName("アカウント作成: 学年が数値でない場合（例外にならず警告で進む）")
  void testCreateAccountWithInvalidGrade() {
    // 1. Ready
    AccountForm form = new AccountForm();
    form.setUserId("user_ng_grade");
    form.setGrade("NotNumber");

    // 2. Do
    target.createAccount(form);

    // 3. Assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertEquals("user_ng_grade", savedUser.getUsername());
    assertNull(savedUser.getGrade());
  }

  @Test
  @DisplayName("アカウント詳細取得: 正常系")
  void testGetAccountById() {
    // 1. Ready
    UUID uuid = UUID.fromString(VALID_UUID_STR);
    User mockUser = new User();
    mockUser.setUsername("target_user");
    mockUser.setDisplayName("ターゲット");
    mockUser.setGrade(2);

    doReturn(Optional.of(mockUser)).when(userRepository).findById(uuid);

    // 2. Do
    AccountForm result = target.getAccountById(VALID_UUID_STR);

    // 3. Assert
    assertEquals(VALID_UUID_STR, result.getId());
    assertEquals("target_user", result.getUserId());
    assertEquals("ターゲット", result.getName());
    assertEquals("2", result.getGrade());
    assertEquals("", result.getPassword());
  }

  @Test
  @DisplayName("アカウント詳細取得: IDが見つからない場合")
  void testGetAccountByIdNotFound() {
    // 1. Ready
    UUID uuid = UUID.fromString(VALID_UUID_STR);
    doReturn(Optional.empty()).when(userRepository).findById(uuid);

    // 2. Do & 3. Assert
    RuntimeException e = assertThrows(RuntimeException.class, () -> {
      target.getAccountById(VALID_UUID_STR);
    });
    assertTrue(e.getMessage().contains("見つかりません"));
  }

  @Test
  @DisplayName("アカウント詳細取得: IDが不正な形式の場合")
  void testGetAccountByIdInvalidUUID() {
    // 1. Ready & 2. Do & 3. Assert
    assertThrows(RuntimeException.class, () -> {
      target.getAccountById("not-a-uuid");
    });
  }

  @Test
  @DisplayName("アカウント更新: 正常系")
  void testUpdateAccount() {
    // 1. Ready
    UUID uuid = UUID.fromString(VALID_UUID_STR);
    User existingUser = new User();
    existingUser.setUsername("old_name");
    doReturn(Optional.of(existingUser)).when(userRepository).findById(uuid);

    AccountForm form = new AccountForm();
    form.setUserId("new_name");
    form.setPassword("new_pass");
    form.setGrade("3");

    // 2. Do
    target.updateAccount(VALID_UUID_STR, form);

    // 3. Assert
    verify(userRepository).save(existingUser);
    assertEquals("new_name", existingUser.getUsername());
    assertEquals("new_pass", existingUser.getPasswordHash());
    assertEquals(3, existingUser.getGrade());
  }

  @Test
  @DisplayName("アカウント更新: 更新対象が存在しない場合")
  void testUpdateAccountNotFound() {
    // 1. Ready
    UUID uuid = UUID.fromString(VALID_UUID_STR);
    doReturn(Optional.empty()).when(userRepository).findById(uuid);

    AccountForm form = new AccountForm();

    // 2. Do & 3. Assert
    assertThrows(RuntimeException.class, () -> {
      target.updateAccount(VALID_UUID_STR, form);
    });
  }

  // --- 追加カバレッジ ---

  @Test
  @DisplayName("findAllActiveAccounts: 正常系")
  void testFindAllActiveAccounts() {
    // 1. Ready
    AccountadminData data = new AccountadminData();
    doReturn(List.of(data)).when(accountadminRepository).findAllActiveAccounts();

    // 2. Do
    List<AccountadminData> result = target.findAllActiveAccounts();

    // 3. Assert
    assertEquals(1, result.size());
    verify(accountadminRepository, times(1)).findAllActiveAccounts();
  }

  @Test
  @DisplayName("insertAccount: 正常系")
  void testInsertAccount() throws SQLException {
    // 1. Ready
    AccountadminData data = new AccountadminData();
    data.setUsername("insert_user");

    // 2. Do
    target.insertAccount(data);

    // 3. Assert
    verify(accountadminRepository, times(1)).insert(data);
  }

  @Test
  @DisplayName("createAccount: DB例外発生時 (DataAccessException)")
  void testCreateAccountDataAccessException() {
    // 1. Ready
    AccountForm form = new AccountForm();
    form.setUserId("error_user");

    doThrow(new QueryTimeoutException("DB Timeout")).when(userRepository).save(any(User.class));

    // 2. Do & 3. Assert
    RuntimeException e = assertThrows(RuntimeException.class, () -> target.createAccount(form));
    assertTrue(e.getMessage().contains("アカウント登録エラー"));
  }

  @Test
  @DisplayName("createAccount: 予期せぬ例外発生時 (Exception)")
  void testCreateAccountGeneralException() {
    // 1. Ready
    AccountForm form = new AccountForm();
    form.setUserId("error_user");

    doThrow(new RuntimeException("Unexpected")).when(userRepository).save(any(User.class));

    // 2. Do & 3. Assert
    RuntimeException e = assertThrows(RuntimeException.class, () -> target.createAccount(form));
    assertTrue(e.getMessage().contains("予期せぬエラーが発生しました"));
  }

  @Test
  @DisplayName("getAccountById: 学年(Grade)がnullの場合")
  void testGetAccountByIdNullGrade() {
    // 1. Ready
    UUID uuid = UUID.fromString(VALID_UUID_STR);
    User mockUser = new User();
    mockUser.setUsername("null_grade_user");
    mockUser.setGrade(null);

    doReturn(Optional.of(mockUser)).when(userRepository).findById(uuid);

    // 2. Do
    AccountForm result = target.getAccountById(VALID_UUID_STR);

    // 3. Assert
    assertEquals("", result.getGrade());
  }

  @Test
  @DisplayName("updateAccount: 学年が数値でない場合 (NumberFormatException)")
  void testUpdateAccountInvalidGrade() {
    // 1. Ready
    UUID uuid = UUID.fromString(VALID_UUID_STR);
    User existingUser = new User();
    existingUser.setUsername("existing");
    doReturn(Optional.of(existingUser)).when(userRepository).findById(uuid);

    AccountForm form = new AccountForm();
    form.setGrade("InvalidNumber");

    // 2. Do
    target.updateAccount(VALID_UUID_STR, form);

    // 3. Assert
    verify(userRepository).save(existingUser);
    assertNull(existingUser.getGrade());
  }

  @Test
  @DisplayName("updateAccount: DB例外発生時")
  void testUpdateAccountDataAccessException() {
    // 1. Ready
    UUID uuid = UUID.fromString(VALID_UUID_STR);
    User existingUser = new User();
    doReturn(Optional.of(existingUser)).when(userRepository).findById(uuid);

    AccountForm form = new AccountForm();
    doThrow(new QueryTimeoutException("DB Error")).when(userRepository).save(any(User.class));

    // 2. Do & 3. Assert
    RuntimeException e = assertThrows(RuntimeException.class, () -> target.updateAccount(VALID_UUID_STR, form));
    assertTrue(e.getMessage().contains("アカウント登録エラー"));
  }

  @Test
  @DisplayName("updateAccount: 予期せぬ例外発生時")
  void testUpdateAccountGeneralException() {
    // 1. Ready
    UUID uuid = UUID.fromString(VALID_UUID_STR);
    User existingUser = new User();
    doReturn(Optional.of(existingUser)).when(userRepository).findById(uuid);

    AccountForm form = new AccountForm();
    doThrow(new RuntimeException("Unknown")).when(userRepository).save(any(User.class));

    // 2. Do & 3. Assert
    RuntimeException e = assertThrows(RuntimeException.class, () -> target.updateAccount(VALID_UUID_STR, form));
    assertTrue(e.getMessage().contains("予期せぬエラーが発生しました"));
  }
}
