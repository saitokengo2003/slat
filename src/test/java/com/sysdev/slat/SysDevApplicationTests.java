package com.sysdev.slat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SysDevApplicationTest {
  @Test
  @DisplayName("Spring Bootアプリケーションが正常に起動すること (Context Load)")
  void contextLoads() {

  }

  @Test
  @DisplayName("mainメソッドの実行確認")
  void testMain() {
    assertDoesNotThrow(() -> SysDevApplication.main(new String[] { "--server.port=0" }));
  }

  @Test
  @DisplayName("コンストラクタの実行確認")
  void testConstructor() {
    assertDoesNotThrow(() -> new SysDevApplication());
  }
}
