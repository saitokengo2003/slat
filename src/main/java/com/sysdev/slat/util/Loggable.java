package com.sysdev.slat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ログ出力を行うためのインターフェース.
 */
public interface Loggable {

  /**
   * ロガーを取得.
   *
   * @return ロガー
   */
  default Logger log() {
    return LoggerFactory.getLogger(this.getClass());
  }
}
