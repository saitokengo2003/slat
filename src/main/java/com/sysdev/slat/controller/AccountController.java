package com.sysdev.slat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {
  @GetMapping("/accountadmin")
  public String getAccountadmin() {

    return "accountadmin/index";
  }

  @GetMapping("/accountcreate")
  public String getAccountcreate() {
    return "accountcreate/index";
  }

}
