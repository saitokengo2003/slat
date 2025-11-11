package com.sysdev.slat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.sysdev.slat.util.Loggable;

@Controller
public class MainController implements Loggable {

  // 💡 修正: @GetMapping("/") を一時的に無効化します。
  // これにより、AccountadminController へのルーティングが妨げられるのを防ぎます。
  // @GetMapping("/")
  public String index() {
    // ログも無効化または修正
    // log().info("[index]");
    return "index";
  }

}
