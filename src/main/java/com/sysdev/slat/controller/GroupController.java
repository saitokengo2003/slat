package com.sysdev.slat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GroupController {

  @GetMapping("/groupcreate")
  public String getGroupcreate() {
    return "groupcreate/index";
  }

  @GetMapping("/groupinfo")
  public String getGroupinfo() {
    return "groupinfo/index";
  }
}
