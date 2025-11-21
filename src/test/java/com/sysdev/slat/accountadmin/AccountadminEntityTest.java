package com.sysdev.slat.accountadmin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.sysdev.slat.user.User;

class AccountadminEntityTest {

  @Test
  @DisplayName("初期状態: リストがnullではなく空のリストとして初期化されていること")
  void testInitialState() {
    // 1. Ready
    AccountadminEntity entity = new AccountadminEntity();

    // 2. Do & 3. Assert
    assertNotNull(entity.getTaskList());
    assertTrue(entity.getTaskList().isEmpty());
    assertNotNull(entity.getAccountList());
    assertTrue(entity.getAccountList().isEmpty());
    assertNull(entity.getErrorMessage());
  }

  @Test
  @DisplayName("正常系: Setterで設定したリストやメッセージがGetterで取得できること")
  void testGettersAndSetters() {
    // 1. Ready
    AccountadminEntity entity = new AccountadminEntity();

    // テストデータ作成
    List<AccountadminData> taskList = new ArrayList<>();
    taskList.add(new AccountadminData());
    List<User> accountList = new ArrayList<>();
    User user = new User();
    user.setUsername("test_user");
    accountList.add(user);
    String errorMessage = "エラーが発生しました";

    // 2. Do
    entity.setTaskList(taskList);
    entity.setAccountList(accountList);
    entity.setErrorMessage(errorMessage);

    // 3. Assert
    assertSame(taskList, entity.getTaskList());
    assertEquals(1, entity.getTaskList().size());
    assertSame(accountList, entity.getAccountList());
    assertEquals(1, entity.getAccountList().size());
    assertEquals("test_user", entity.getAccountList().get(0).getUsername());
    assertEquals(errorMessage, entity.getErrorMessage());
  }

  @Test
  @DisplayName("境界値: Nullをセットした場合の挙動")
  void testSetNull() {
    // 1. Ready
    AccountadminEntity entity = new AccountadminEntity();

    // 2. Do
    entity.setTaskList(null);
    entity.setAccountList(null);
    entity.setErrorMessage(null);

    // 3. Assert
    assertNull(entity.getTaskList());
    assertNull(entity.getAccountList());
    assertNull(entity.getErrorMessage());
  }
}
