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

    when(registry.addInterceptor(any(LoginInterceptor.class))).thenReturn(registration);

    when(registration.addPathPatterns(anyString())).thenReturn(registration);

    // 2. Do
    config.addInterceptors(registry);

    // 3. Assert
    verify(registry, times(1)).addInterceptor(any(LoginInterceptor.class));
    verify(registration).addPathPatterns("/**");
    verify(registration).excludePathPatterns(
        "/login",
        "/css/**",
        "/js/**",
        "/images/**",
        "/favicon.ico");
  }
}
