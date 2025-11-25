package com.sysdev.slat.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class LoggableTest {
  static class TestTarget implements Loggable {

  }

  @Test
  @DisplayName("正常系: 実装クラスに応じたロガーが取得できること")
  void testLog() {
    // 1. Ready
    TestTarget target = new TestTarget();

    // 2. Do
    Logger logger = target.log();

    // 3. Assert
    assertNotNull(logger, "ロガーが取得できること");

    assertEquals(TestTarget.class.getName(), logger.getName(), "ロガー名がクラス名と一致すること");
  }
}
