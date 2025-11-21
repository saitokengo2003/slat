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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.sysdev.slat.accountadmin.AccountForm;
import com.sysdev.slat.accountadmin.AccountadminEntity;
import com.sysdev.slat.accountadmin.AccountadminService;
import com.sysdev.slat.user.UserData;

@SpringBootTest
@AutoConfigureMockMvc
public class AccountControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AccountadminService accountadminService;
  private UserData mockUser;

  @BeforeEach
  void setUp() {
    mockUser = new UserData();
    mockUser.setUserId("admin");
    mockUser.setDisplayName("管理者");
    mockUser.setRoleCode("ADMIN");
  }

  @Test
  @DisplayName("アカウント一覧画面表示: 正常系")
  void testShowAccountList() throws Exception {
    // 1. Ready
    AccountadminEntity mockEntity = new AccountadminEntity();
    doReturn(mockEntity).when(accountadminService).getAccountListEntity();

    // 2. Do & 3. Check
    mockMvc.perform(get("/accountadmin")
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("accountadmin/index"))
        .andExpect(model().attributeExists("accountadminEntity"));
  }

  @Test
  @DisplayName("アカウント作成画面表示: 正常系")
  void testGetAccountcreate() throws Exception {
    mockMvc.perform(get("/accountcreate")
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("accountcreate/index"))
        .andExpect(model().attributeExists("accountForm"));
  }

  @Test
  @DisplayName("アカウント作成処理: 成功")
  void testCreateAccountSuccess() throws Exception {
    // 1. Ready

    // 2. Do & 3. Check
    mockMvc.perform(post("/accountcreate")
        .sessionAttr("userData", mockUser)
        .param("userId", "user1")
        .param("name", "User One"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/accountadmin"))
        .andExpect(flash().attributeExists("message"));
  }

  @Test
  @DisplayName("アカウント作成処理: 失敗（例外発生）")
  void testCreateAccountFail() throws Exception {
    // 1. Ready
    doThrow(new RuntimeException("DB Error")).when(accountadminService).createAccount(any(AccountForm.class));

    // 2. Do & 3. Check
    mockMvc.perform(post("/accountcreate")
        .sessionAttr("userData", mockUser)
        .param("userId", "user1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/accountadmin"))
        .andExpect(flash().attributeExists("errorMessage"));
  }

  @Test
  @DisplayName("アカウント削除処理: 成功")
  void testDeleteAccountSuccess() throws Exception {
    // 1. Ready
    String targetId = "uuid-123";

    // 2. Do & 3. Check
    mockMvc.perform(post("/accountadmin/delete")
        .sessionAttr("userData", mockUser)
        .param("id", targetId))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/accountadmin"))
        .andExpect(flash().attributeExists("message"));
  }

  @Test
  @DisplayName("アカウント削除処理: 失敗（SQLException発生 -> catchブロックへ）")
  void testDeleteAccountFail() throws Exception {
    // 1. Ready
    doThrow(new SQLException("Delete failed")).when(accountadminService).deleteAccount(anyString());

    // 2. Do & 3. Check
    mockMvc.perform(post("/accountadmin/delete")
        .sessionAttr("userData", mockUser)
        .param("id", "uuid-error"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/accountadmin"))
        .andExpect(flash().attributeExists("errorMessage"));
  }

  @Test
  @DisplayName("アカウント削除処理: 予期せぬ例外（RuntimeException -> catchせず異常終了）")
  void testDeleteAccountRuntimeException() throws Exception {
    // 1. Ready
    doThrow(new RuntimeException("Unexpected Error")).when(accountadminService).deleteAccount(anyString());

    // 2. Do & 3. Check
    try {
      mockMvc.perform(post("/accountadmin/delete")
          .sessionAttr("userData", mockUser)
          .param("id", "uuid-fatal"));
    } catch (Exception e) {
      assertTrue(e.getCause() instanceof RuntimeException);
    }
  }

  @Test
  @DisplayName("アカウント編集画面表示: 正常系")
  void testGetAccountEdit() throws Exception {
    // 1. Ready
    String targetId = "uuid-123";
    AccountForm mockForm = new AccountForm();
    mockForm.setId(targetId);
    mockForm.setName("Edit Target");

    doReturn(mockForm).when(accountadminService).getAccountById(targetId);

    // 2. Do & 3. Check
    mockMvc.perform(get("/accountedit")
        .sessionAttr("userData", mockUser)
        .param("id", targetId))
        .andExpect(status().isOk())
        .andExpect(view().name("accountedit/index"))
        .andExpect(model().attribute("accountForm", mockForm));
  }

  @Test
  @DisplayName("アカウント編集画面表示: 失敗（Serviceで例外発生）")
  void testGetAccountEditException() throws Exception {
    // 1. Ready
    doThrow(new RuntimeException("User not found")).when(accountadminService).getAccountById(anyString());

    // 2. Do & 3. Check
    try {
      mockMvc.perform(get("/accountedit")
          .sessionAttr("userData", mockUser)
          .param("id", "invalid-id"));
    } catch (Exception e) {
      assertTrue(e.getCause() instanceof RuntimeException);
    }
  }

  @Test
  @DisplayName("アカウント編集処理: 成功")
  void testEditAccountSuccess() throws Exception {
    // 1. Ready
    // 2. Do & 3. Check
    mockMvc.perform(post("/accountedit")
        .sessionAttr("userData", mockUser)
        .param("id", "uuid-123")
        .param("name", "Updated Name"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/accountadmin"))
        .andExpect(flash().attributeExists("message"));
  }

  @Test
  @DisplayName("アカウント編集処理: 失敗（例外発生）")
  void testEditAccountFail() throws Exception {
    // 1. Ready
    doThrow(new RuntimeException("Update Error")).when(accountadminService).updateAccount(anyString(),
        any(AccountForm.class));

    // 2. Do & 3. Check
    mockMvc.perform(post("/accountedit")
        .sessionAttr("userData", mockUser)
        .param("id", "uuid-123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/accountadmin"))
        .andExpect(flash().attributeExists("errorMessage"));
  }
}
