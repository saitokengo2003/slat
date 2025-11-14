package com.sysdev.slat.login; // パッケージを変更

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LoginEntity implements WebMvcConfigurer { // クラス名を変更

  /**
   * インターセプターを登録します。
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {

    registry.addInterceptor(new LoginInterceptor())
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/login",
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico");

  }
}
