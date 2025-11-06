package com.sysdev.hcs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.sysdev.hcs.util.Loggable;

/**
 * メインコントローラ.
 */
@Controller
public class MainController implements Loggable {

  @GetMapping("/")
  public String index() {
    log().info("[index]");
    return "index";
  }

}
