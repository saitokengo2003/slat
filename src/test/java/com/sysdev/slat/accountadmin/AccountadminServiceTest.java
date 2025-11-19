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

  // テスト用のダミーUUID文字列
  private final String VALID_UUID_STR = "550e8400-e29b-41d4-a716-446655440000";

  @Test
  @DisplayName("アカウント一覧取得(Entity): 正常系")
  void testGetAccountListEntity() {
    // 1. Ready
    User user1 = new User();
    user1.setUsername("user1");
    User user2 = new User();
    user2.setUsername("user2");

    // findAllが呼ばれたらリストを返す
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
    // voidメソッドなので何もしない設定（省略可能だが明示）
    doNothing().when(accountadminRepository).delete(accountId);

    // 2. Do
    target.deleteAccount(accountId);

    // 3. Assert
    // accountadminRepository.delete が1回呼ばれたことを検証
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
    form.setGrade("1"); // 数値変換できる文字列
    form.setClassId("A");
    form.setNumber("10");

    // 2. Do
    target.createAccount(form);

    // 3. Assert
    // saveメソッドに渡されたUserオブジェクトを捕まえて中身をチェックする
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertEquals("new_user", savedUser.getUsername());
    assertEquals("新規 太郎", savedUser.getDisplayName());
    assertEquals(1, savedUser.getGrade()); // 数値に変換されていること
    assertEquals("active", savedUser.getStatus()); // デフォルト値の確認
    assertNotNull(savedUser.getCreatedAt()); // 日時が入っていること
  }

  @Test
  @DisplayName("アカウント作成: 学年が数値でない場合（例外にならず警告で進む）")
  void testCreateAccountWithInvalidGrade() {
    // 1. Ready
    AccountForm form = new AccountForm();
    form.setUserId("user_ng_grade");
    form.setGrade("NotNumber"); // 数値ではない

    // 2. Do
    target.createAccount(form);

    // 3. Assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertEquals("user_ng_grade", savedUser.getUsername());
    // Grade設定時にエラーになっても処理は続き、Gradeは設定されない（null or 0）
    // Userクラスの初期値に依存しますが、setGradeが呼ばれていないことを確認
    assertNull(savedUser.getGrade()); // Integerフィールドの初期値がnullの場合
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
    assertEquals("2", result.getGrade()); // Stringに変換されていること
    assertEquals("", result.getPassword()); // パスワードは空文字になっていること
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

    // 既存データのモック
    User existingUser = new User();
    existingUser.setUsername("old_name");
    doReturn(Optional.of(existingUser)).when(userRepository).findById(uuid);

    // 更新用フォームデータ
    AccountForm form = new AccountForm();
    form.setUserId("new_name");
    form.setPassword("new_pass");
    form.setGrade("3");

    // 2. Do
    target.updateAccount(VALID_UUID_STR, form);

    // 3. Assert
    // saveが呼ばれたか検証
    verify(userRepository).save(existingUser);

    // 中身が書き換わっているか検証
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
}
