package com.sysdev.slat.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.sysdev.slat.user.UserData;

class LoginInterceptorTest {

  // テスト対象
  private final LoginInterceptor interceptor = new LoginInterceptor();

  private final String SESSION_KEY = "userData";

  @Test
  @DisplayName("正常系: ログイン済み（セッションにUserDataあり）なら true を返す")
  void testPreHandleLoggedIn() throws Exception {
    // 1. Ready
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SESSION_KEY, new UserData());
    request.setSession(session);

    // 2. Do
    boolean result = interceptor.preHandle(request, response, new Object());

    // 3. Assert
    assertTrue(result, "ログイン済みなので true が返るべき");
    assertNull(response.getRedirectedUrl(), "リダイレクトは設定されないべき");
  }

  @Test
  @DisplayName("異常系: セッション自体が存在しない場合は false を返しリダイレクト")
  void testPreHandleNoSession() throws Exception {
    // 1. Ready
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // 2. Do
    boolean result = interceptor.preHandle(request, response, new Object());

    // 3. Assert
    assertFalse(result, "未ログインなので false が返るべき");
    assertEquals("/login", response.getRedirectedUrl(), "ログイン画面へリダイレクトされるべき");
  }

  @Test
  @DisplayName("異常系: セッションはあるが UserData が入っていない場合は false を返しリダイレクト")
  void testPreHandleSessionEmpty() throws Exception {
    // 1. Ready
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockHttpSession session = new MockHttpSession();
    request.setSession(session);

    // 2. Do
    boolean result = interceptor.preHandle(request, response, new Object());

    // 3. Assert
    assertFalse(result);
    assertEquals("/login", response.getRedirectedUrl());
  }

  @Test
  @DisplayName("異常系: セッションに UserData 以外のオブジェクトが入っている場合")
  void testPreHandleInvalidObject() throws Exception {
    // 1. Ready
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SESSION_KEY, "I am not UserData");
    request.setSession(session);

    // 2. Do
    boolean result = interceptor.preHandle(request, response, new Object());

    // 3. Assert
    assertFalse(result);
    assertEquals("/login", response.getRedirectedUrl());
  }
}
