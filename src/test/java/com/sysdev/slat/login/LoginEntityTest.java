package com.sysdev.slat.login;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@ExtendWith(MockitoExtension.class)
class LoginEntityTest {

  @Mock
  private InterceptorRegistry registry;

  @Mock
  private InterceptorRegistration registration;

  @Test
  @DisplayName("インターセプターが正しいパス設定で登録されること")
  void testAddInterceptors() {
    // 1. Ready
    LoginEntity config = new LoginEntity();

    // モックの挙動定義 (メソッドチェーンをつなぐため)
    // registry.addInterceptor(...) が呼ばれたら registration モックを返す
    when(registry.addInterceptor(any(LoginInterceptor.class))).thenReturn(registration);

    // registration.addPathPatterns(...) が呼ばれたら registration モック自身を返す
    when(registration.addPathPatterns(anyString())).thenReturn(registration);

    // (excludePathPatterns は戻り値を使わないので定義しなくても動くが、チェーンの最後なので念のため)
    // when(registration.excludePathPatterns(any(String[].class))).thenReturn(registration);

    // 2. Do
    config.addInterceptors(registry);

    // 3. Assert

    // (1) LoginInterceptorのインスタンスが登録されたか検証
    verify(registry, times(1)).addInterceptor(any(LoginInterceptor.class));

    // (2) 対象パス "/**" が追加されたか検証
    verify(registration).addPathPatterns("/**");

    // (3) 除外パスが正しく指定されたか検証
    // excludePathPatternsは可変長引数(String...)なので、並び順通りに渡されているか確認
    verify(registration).excludePathPatterns(
        "/login",
        "/css/**",
        "/js/**",
        "/images/**",
        "/favicon.ico");
  }
}
