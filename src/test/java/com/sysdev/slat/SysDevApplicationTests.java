package com.sysdev.slat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SysDevApplicationTest {

  /**
   * コンテキストロードのテスト.
   * 通常の起動プロセスでエラーが出ないかを確認します。
   */
  @Test
  @DisplayName("Spring Bootアプリケーションが正常に起動すること (Context Load)")
  void contextLoads() {
    // @SpringBootTest アノテーションにより自動的に起動チェックが行われます
  }

  /**
   * mainメソッドのカバレッジを確保するためのテスト.
   * 直接 main メソッドを叩くことで、該当行を実行済みにします。
   */
  @Test
  @DisplayName("mainメソッドの実行確認")
  void testMain() {
    // ポート衝突エラーを防ぐため、サーバーポートを0(ランダム)に設定して実行
    assertDoesNotThrow(() -> SysDevApplication.main(new String[] { "--server.port=0" }));
  }

  /**
   * コンストラクタのカバレッジを確保するためのテスト.
   * クラス定義行が未カバーになるのを防ぎます。
   */
  @Test
  @DisplayName("コンストラクタの実行確認")
  void testConstructor() {
    assertDoesNotThrow(() -> new SysDevApplication());
  }
}
