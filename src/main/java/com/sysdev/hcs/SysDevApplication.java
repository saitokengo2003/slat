package com.sysdev.hcs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Bootの起動クラス.
 */
@SpringBootApplication
public class SysDevApplication {

  /**
   * Spring Bootの起動.
   *
   * @param args 起動引数
   */
  public static void main(String[] args) {
    SpringApplication.run(SysDevApplication.class, args);
  }
}
