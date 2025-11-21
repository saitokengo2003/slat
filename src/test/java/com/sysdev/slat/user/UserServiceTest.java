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

    User mockUser = new User();
    mockUser.setUsername(username);
    mockUser.setPasswordHash(password);
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

    List<User> dbList = Arrays.asList(me, other1, other2);
    doReturn(dbList).when(userRepository).findAll();

    // 2. Do
    List<UserData> result = target.findAllOtherUsers(myUsername);

    // 3. Assert
    assertEquals(2, result.size());
    boolean containsMe = result.stream()
        .anyMatch(u -> u.getUserId().equals(myUsername));
    assertFalse(containsMe, "リストに自分自身が含まれてはいけません");
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

  // --- getUserRole のテスト ---

  @Test
  @DisplayName("getUserRole: ユーザーが存在する場合、DBのロールコードが返却されること")
  void testGetUserRole_Found() {
    // 1. Ready
    String username = "teacher1";
    String expectedRole = "TEACHER";

    User mockUser = new User();
    mockUser.setUsername(username);
    mockUser.setRoleCode(expectedRole);

    doReturn(Optional.of(mockUser)).when(userRepository).findByUsername(username);

    // 2. Do
    String result = target.getUserRole(username);

    // 3. Assert
    assertEquals(expectedRole, result);
  }

  @Test
  @DisplayName("getUserRole: ユーザーが存在しない場合、guest が返却されること")
  void testGetUserRole_NotFound() {
    // 1. Ready
    String username = "unknown_user";
    doReturn(Optional.empty()).when(userRepository).findByUsername(username);

    // 2. Do
    String result = target.getUserRole(username);

    // 3. Assert
    assertEquals("guest", result);
  }

  // --- getDisplayName のテスト (追加) ---

  @Test
  @DisplayName("getDisplayName: ユーザーが存在する場合、表示名が返る")
  void testGetDisplayName_Found() {
    // 1. Ready
    String username = "user01";
    String displayName = "表示名 太郎";
    User mockUser = new User();
    mockUser.setUsername(username);
    mockUser.setDisplayName(displayName);

    doReturn(Optional.of(mockUser)).when(userRepository).findByUsername(username);

    // 2. Do
    String result = target.getDisplayName(username);

    // 3. Assert
    assertEquals(displayName, result);
  }

  @Test
  @DisplayName("getDisplayName: ユーザーが存在しない場合、'不明なユーザー' が返る")
  void testGetDisplayName_NotFound() {
    // 1. Ready
    String username = "unknown_id";
    doReturn(Optional.empty()).when(userRepository).findByUsername(username);

    // 2. Do
    String result = target.getDisplayName(username);

    // 3. Assert
    // UserServiceの実装: return "不明なユーザー (" + userId + ")";
    assertEquals("不明なユーザー (unknown_id)", result);
  }

  // --- getAllStudentIds のテスト (追加) ---

  @Test
  @DisplayName("getAllStudentIds: roleCodeが'student'のユーザーのみIDがリスト化される")
  void testGetAllStudentIds() {
    // 1. Ready
    User student1 = new User();
    student1.setUsername("s1");
    student1.setRoleCode("student");

    User student2 = new User();
    student2.setUsername("s2");
    student2.setRoleCode("student");

    User teacher = new User();
    teacher.setUsername("t1");
    teacher.setRoleCode("teacher");

    List<User> allUsers = Arrays.asList(student1, teacher, student2);
    doReturn(allUsers).when(userRepository).findAll();

    // 2. Do
    List<String> result = target.getAllStudentIds();

    // 3. Assert
    assertEquals(2, result.size());
    assertTrue(result.contains("s1"));
    assertTrue(result.contains("s2"));
    assertFalse(result.contains("t1")); // 先生は含まれない
  }
}
