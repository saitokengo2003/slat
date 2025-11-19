package com.sysdev.slat.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.sysdev.slat.user.UserData;
import com.sysdev.slat.user.UserService;

@SpringBootTest
@AutoConfigureMockMvc
class LoginControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  private final String SESSION_USER_DATA_KEY = "userData";

  // --- 1. ログイン画面表示 (GET) ---

  @Test
  @DisplayName("ログイン画面表示: 正常系")
  void testGetLogin() throws Exception {
    // 1. Ready (特になし)

    // 2. Do & 3. Check
    mockMvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(view().name("login/index"));
  }

  // --- 2. ログイン処理 (POST) ---

  @Test
  @DisplayName("ログイン処理: 成功（セッションに保存してリダイレクト）")
  void testPostLoginSuccess() throws Exception {
    // 1. Ready
    UserData mockUser = new UserData();
    mockUser.setUserId("user1");
    mockUser.setDisplayName("テスト 太郎");

    // 認証成功時はUserDataオブジェクトを返す
    doReturn(mockUser).when(userService).authenticate("user1", "password123");

    // 2. Do & 3. Check
    mockMvc.perform(post("/login")
        .param("id", "user1")
        .param("password", "password123"))
        .andExpect(status().is3xxRedirection()) // リダイレクト
        .andExpect(redirectedUrl("/"))
        // ★重要: セッションに "userData" 属性が保存されているか確認
        .andExpect(request().sessionAttribute(SESSION_USER_DATA_KEY, notNullValue()))
        .andExpect(request().sessionAttribute(SESSION_USER_DATA_KEY, mockUser));
  }

  @Test
  @DisplayName("ログイン処理: 失敗（エラーメッセージを表示して画面維持）")
  void testPostLoginFail() throws Exception {
    // 1. Ready
    // 認証失敗時はnullを返す
    doReturn(null).when(userService).authenticate(anyString(), anyString());

    // 2. Do & 3. Check
    mockMvc.perform(post("/login")
        .param("id", "wrong_user")
        .param("password", "wrong_pass"))
        .andExpect(status().isOk()) // リダイレクトせず200 OK
        .andExpect(view().name("login/index"))
        .andExpect(model().attributeExists("loginError")) // エラーメッセージがあるか
        .andExpect(model().attribute("loginError", "IDまたはパスワードが間違っています。"))
        // セッションには保存されていないことを確認
        .andExpect(request().sessionAttributeDoesNotExist(SESSION_USER_DATA_KEY));
  }

  // --- 3. ログアウト処理 (POST) ---

  @Test
  @DisplayName("ログアウト処理: セッションが無効化されること")
  void testPostLogout() throws Exception {
    // 1. Ready
    // テスト用のセッションを作成し、データを入れておく
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SESSION_USER_DATA_KEY, new UserData());

    // 2. Do
    mockMvc.perform(post("/logout")
        .session(session)) // 作成したセッションをリクエストに乗せる
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));

    // 3. Check
    // ★重要: セッションが無効化されているか（isInvalid() が true か）を確認
    assertTrue(session.isInvalid(), "セッションが無効化されているべき");
  }
}
