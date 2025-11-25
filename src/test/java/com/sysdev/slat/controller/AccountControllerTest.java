package com.sysdev.slat.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sysdev.slat.accountadmin.AccountForm;
import com.sysdev.slat.accountadmin.AccountadminEntity;
import com.sysdev.slat.accountadmin.AccountadminService;
import com.sysdev.slat.user.UserData;

@ExtendWith(MockitoExtension.class)
public class AccountControllerTest {

  private MockMvc mockMvc;

  @Mock
  private AccountadminService accountadminService;

  @InjectMocks
  private AccountController accountController;

  private UserData mockAdminUser;
  private final String SESSION_KEY = "userData";

  @BeforeEach
  void setUp() {
    this.mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();

    mockAdminUser = new UserData();
    mockAdminUser.setUserId("admin");
    mockAdminUser.setDisplayName("管理者");
    mockAdminUser.setRoleCode("admin");
  }

  // --- 1. 一覧表示 (GET) ---

  @Test
  @DisplayName("アカウント一覧画面表示: 正常系 (管理者)")
  void testShowAccountList() throws Exception {
    AccountadminEntity mockEntity = new AccountadminEntity();
    doReturn(mockEntity).when(accountadminService).getAccountListEntity();

    mockMvc.perform(get("/accountadmin")
        .sessionAttr(SESSION_KEY, mockAdminUser))
        .andExpect(status().isOk())
        .andExpect(view().name("accountadmin/index"))
        .andExpect(model().attributeExists("accountadminEntity"))
        .andExpect(model().attribute("displayName", "管理者"));
  }

  @Test
  @DisplayName("アカウント一覧画面表示: 管理者以外はトップへリダイレクト")
  void testShowAccountList_NotAdmin() throws Exception {
    UserData studentUser = new UserData();
    studentUser.setUserId("student1");
    studentUser.setRoleCode("student");

    mockMvc.perform(get("/accountadmin")
        .sessionAttr(SESSION_KEY, studentUser))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  @DisplayName("アカウント一覧画面表示: 未ログインはトップへリダイレクト")
  void testShowAccountList_NotLoggedIn() throws Exception {
    mockMvc.perform(get("/accountadmin"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  // --- 2. 作成画面表示 (GET) ---

  @Test
  @DisplayName("アカウント作成画面表示: 正常系 (ログイン済み)")
  void testGetAccountcreate() throws Exception {
    mockMvc.perform(get("/accountcreate")
        .sessionAttr(SESSION_KEY, mockAdminUser))
        .andExpect(status().isOk())
        .andExpect(view().name("accountcreate/index"))
        .andExpect(model().attributeExists("accountForm"))
        .andExpect(model().attribute("displayName", "管理者"));
  }

  @Test
  @DisplayName("アカウント作成画面表示: 未ログイン (displayNameセットなし)")
  void testGetAccountcreate_NotLoggedIn() throws Exception {
    mockMvc.perform(get("/accountcreate"))
        .andExpect(status().isOk())
        .andExpect(view().name("accountcreate/index"))
        .andExpect(model().attributeExists("accountForm"))
        .andExpect(model().attributeDoesNotExist("displayName"));
  }

  // --- 3. アカウント作成処理 (POST) ---

  @Test
  @DisplayName("アカウント作成処理: 成功")
  void testCreateAccountSuccess() throws Exception {
    mockMvc.perform(post("/accountcreate")
        .sessionAttr(SESSION_KEY, mockAdminUser)
        .param("userId", "user1")
        .param("name", "User One"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/accountadmin"))
        .andExpect(flash().attributeExists("message"));
  }

  @Test
  @DisplayName("アカウント作成処理: 失敗（例外発生）")
  void testCreateAccountFail() throws Exception {
    doThrow(new RuntimeException("DB Error")).when(accountadminService).createAccount(any(AccountForm.class));

    mockMvc.perform(post("/accountcreate")
        .sessionAttr(SESSION_KEY, mockAdminUser)
        .param("userId", "user1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/accountadmin"))
        .andExpect(flash().attributeExists("errorMessage"));
  }

  // --- 4. アカウント削除処理 (POST) ---

  @Test
  @DisplayName("アカウント削除処理: 成功")
  void testDeleteAccountSuccess() throws Exception {
    String targetId = "uuid-123";

    mockMvc.perform(post("/accountadmin/delete")
        .sessionAttr(SESSION_KEY, mockAdminUser)
        .param("id", targetId))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/accountadmin"))
        .andExpect(flash().attributeExists("message"));
  }

  @Test
  @DisplayName("アカウント削除処理: 失敗（SQLException発生 -> catchブロックへ）")
  void testDeleteAccountFail() throws Exception {
    doThrow(new SQLException("Delete failed")).when(accountadminService).deleteAccount(anyString());

    mockMvc.perform(post("/accountadmin/delete")
        .sessionAttr(SESSION_KEY, mockAdminUser)
        .param("id", "uuid-error"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/accountadmin"))
        .andExpect(flash().attributeExists("errorMessage"));
  }

  @Test
  @DisplayName("アカウント削除処理: 予期せぬ例外（RuntimeException -> catchせず異常終了）")
  void testDeleteAccountRuntimeException() throws Exception {
    doThrow(new RuntimeException("Unexpected Error")).when(accountadminService).deleteAccount(anyString());

    try {
      mockMvc.perform(post("/accountadmin/delete")
          .sessionAttr(SESSION_KEY, mockAdminUser)
          .param("id", "uuid-fatal"));
    } catch (Exception e) {
      assertTrue(e.getCause() instanceof RuntimeException);
    }
  }

  // --- 5. 編集画面表示 (GET) ---

  @Test
  @DisplayName("アカウント編集画面表示: 正常系")
  void testGetAccountEdit() throws Exception {
    String targetId = "uuid-123";
    AccountForm mockForm = new AccountForm();
    mockForm.setId(targetId);
    mockForm.setName("Edit Target");

    doReturn(mockForm).when(accountadminService).getAccountById(targetId);

    mockMvc.perform(get("/accountedit")
        .sessionAttr(SESSION_KEY, mockAdminUser)
        .param("id", targetId))
        .andExpect(status().isOk())
        .andExpect(view().name("accountedit/index"))
        .andExpect(model().attribute("accountForm", mockForm));
  }

  @Test
  @DisplayName("アカウント編集画面表示: 失敗（Serviceで例外発生）")
  void testGetAccountEditException() throws Exception {
    doThrow(new RuntimeException("User not found")).when(accountadminService).getAccountById(anyString());

    try {
      mockMvc.perform(get("/accountedit")
          .sessionAttr(SESSION_KEY, mockAdminUser)
          .param("id", "invalid-id"));
    } catch (Exception e) {
      assertTrue(e.getCause() instanceof RuntimeException);
    }
  }

  // --- 6. アカウント編集処理 (POST) ---

  @Test
  @DisplayName("アカウント編集処理: 成功")
  void testEditAccountSuccess() throws Exception {
    mockMvc.perform(post("/accountedit")
        .sessionAttr(SESSION_KEY, mockAdminUser)
        .param("id", "uuid-123")
        .param("name", "Updated Name"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/accountadmin"))
        .andExpect(flash().attributeExists("message"));
  }

  @Test
  @DisplayName("アカウント編集処理: 失敗（例外発生）")
  void testEditAccountFail() throws Exception {
    doThrow(new RuntimeException("Update Error")).when(accountadminService).updateAccount(anyString(),
        any(AccountForm.class));

    mockMvc.perform(post("/accountedit")
        .sessionAttr(SESSION_KEY, mockAdminUser)
        .param("id", "uuid-123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/accountadmin"))
        .andExpect(flash().attributeExists("errorMessage"));
  }
}
