package com.sysdev.slat.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class LoggableTest {

  /**
   * テスト用の実装クラス.
   * Loggableインターフェースをimplementsします。
   */
  static class TestTarget implements Loggable {
    // 実装は空でOK（defaultメソッドのテストなので）
  }

  @Test
  @DisplayName("正常系: 実装クラスに応じたロガーが取得できること")
  void testLog() {
    // 1. Ready
    TestTarget target = new TestTarget();

    // 2. Do
    Logger logger = target.log();

    // 3. Assert
    // (1) ロガーがnullでないこと
    assertNotNull(logger, "ロガーが取得できること");

    // (2) ロガーの名前が、実装クラスの完全修飾名と一致していること
    // LoggerFactory.getLogger(this.getClass()) の挙動確認
    assertEquals(TestTarget.class.getName(), logger.getName(), "ロガー名がクラス名と一致すること");
  }
}
