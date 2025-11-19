package com.sysdev.slat.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService target;

  // --- authenticate (認証) のテスト ---

  @Test
  @DisplayName("authenticate: 認証成功（ユーザーが存在しパスワードが一致）")
  void testAuthenticateSuccess() {
    // 1. Ready
    String username = "user01";
    String password = "password123";

    // DBから取得されるユーザー情報（パスワードは平文比較の仕様に基づく）
    User mockUser = new User();
    mockUser.setUsername(username);
    mockUser.setPasswordHash(password); // ※本来はハッシュ値ですが、コード通り平文で設定
    mockUser.setDisplayName("テスト 太郎");
    mockUser.setRoleCode("STUDENT");
    mockUser.setGrade(1);
    mockUser.setClassName("A");
    mockUser.setNumber(10);

    doReturn(Optional.of(mockUser)).when(userRepository).findByUsername(username);

    // 2. Do
    UserData result = target.authenticate(username, password);

    // 3. Assert
    assertNotNull(result);
    assertEquals(username, result.getUserId());
    assertEquals("テスト 太郎", result.getDisplayName());
    assertEquals("STUDENT", result.getRoleCode());
    assertEquals(1, result.getGrade());
    assertEquals("A", result.getClassName());
    assertEquals(10, result.getNumber());
  }

  @Test
  @DisplayName("authenticate: 失敗（ユーザーが存在しない）")
  void testAuthenticateUserNotFound() {
    // 1. Ready
    String username = "unknown_user";
    doReturn(Optional.empty()).when(userRepository).findByUsername(username);

    // 2. Do
    UserData result = target.authenticate(username, "any_pass");

    // 3. Assert
    assertNull(result);
  }

  @Test
  @DisplayName("authenticate: 失敗（パスワード不一致）")
  void testAuthenticatePasswordMismatch() {
    // 1. Ready
    String username = "user01";
    User mockUser = new User();
    mockUser.setUsername(username);
    mockUser.setPasswordHash("correct_password");

    doReturn(Optional.of(mockUser)).when(userRepository).findByUsername(username);

    // 2. Do
    UserData result = target.authenticate(username, "wrong_password");

    // 3. Assert
    assertNull(result);
  }

  // --- findAllOtherUsers (他ユーザー取得) のテスト ---

  @Test
  @DisplayName("findAllOtherUsers: 自分以外のユーザーのみ取得できること")
  void testFindAllOtherUsers() {
    // 1. Ready
    String myUsername = "me";

    User me = new User();
    me.setUsername(myUsername);
    me.setDisplayName("自分");

    User other1 = new User();
    other1.setUsername("other1");
    other1.setDisplayName("他人1");

    User other2 = new User();
    other2.setUsername("other2");
    other2.setDisplayName("他人2");

    // リポジトリは Iterable<User> を返すが、List は Iterable を継承しているのでそのまま返せる
    List<User> dbList = Arrays.asList(me, other1, other2);
    doReturn(dbList).when(userRepository).findAll();

    // 2. Do
    List<UserData> result = target.findAllOtherUsers(myUsername);

    // 3. Assert
    assertEquals(2, result.size()); // 自分(me)が除外されて2件になるはず

    // 中身の検証（自分が入っていないか）
    boolean containsMe = result.stream()
        .anyMatch(u -> u.getUserId().equals(myUsername));
    assertFalse(containsMe, "リストに自分自身が含まれてはいけません");

    // 他人が含まれているか
    assertEquals("other1", result.get(0).getUserId());
    assertEquals("other2", result.get(1).getUserId());
  }

  @Test
  @DisplayName("findAllOtherUsers: ユーザーが自分一人の場合は空リスト")
  void testFindAllOtherUsersOnlyMe() {
    // 1. Ready
    String myUsername = "lonely_user";
    User me = new User();
    me.setUsername(myUsername);

    doReturn(Collections.singletonList(me)).when(userRepository).findAll();

    // 2. Do
    List<UserData> result = target.findAllOtherUsers(myUsername);

    // 3. Assert
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("findAllOtherUsers: DBが空の場合は空リスト")
  void testFindAllOtherUsersEmptyDB() {
    // 1. Ready
    doReturn(Collections.emptyList()).when(userRepository).findAll();

    // 2. Do
    List<UserData> result = target.findAllOtherUsers("anyone");

    // 3. Assert
    assertTrue(result.isEmpty());
  }
}
