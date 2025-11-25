package com.sysdev.slat.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sysdev.slat.user.UserData;

@ExtendWith(MockitoExtension.class)
public class AdminControllerTest {

  private MockMvc mockMvc;

  @InjectMocks
  private AdminController adminController;

  private UserData mockAdminUser;
  private UserData mockStudentUser;
  private final String SESSION_KEY = "userData";

  @BeforeEach
  void setUp() {
    this.mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();

    // 管理者ユーザー
    mockAdminUser = new UserData();
    mockAdminUser.setUserId("admin");
    mockAdminUser.setDisplayName("管理者");
    mockAdminUser.setRoleCode("admin");

    // 一般ユーザー
    mockStudentUser = new UserData();
    mockStudentUser.setUserId("student");
    mockStudentUser.setDisplayName("学生");
    mockStudentUser.setRoleCode("student");
  }

  // --- 1. 正常系: 管理者としてログイン ---
  @Test
  @DisplayName("管理者トップ画面表示: 管理者の場合は表示される")
  void testGetAdmin_Success() throws Exception {
    mockMvc.perform(get("/admin")
        .sessionAttr(SESSION_KEY, mockAdminUser))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/index"))
        .andExpect(model().attribute("displayName", "管理者"));
  }

  // --- 2. 異常系: 一般ユーザーとしてログイン ---
  @Test
  @DisplayName("管理者トップ画面表示: 一般ユーザーはトップへリダイレクト")
  void testGetAdmin_NotAdmin() throws Exception {
    mockMvc.perform(get("/admin")
        .sessionAttr(SESSION_KEY, mockStudentUser))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  // --- 3. 異常系: 未ログイン (nullチェックの網羅) ---
  @Test
  @DisplayName("管理者トップ画面表示: 未ログインはトップへリダイレクト (nullチェック)")
  void testGetAdmin_NotLoggedIn() throws Exception {
    mockMvc.perform(get("/admin"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }
}
