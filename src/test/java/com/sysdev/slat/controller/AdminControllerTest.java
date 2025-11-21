package com.sysdev.slat.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.sysdev.slat.user.UserData;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private UserData mockUser;

  @BeforeEach
  void setUp() {
    mockUser = new UserData();
    mockUser.setUserId("admin");
    mockUser.setDisplayName("管理者");
  }

  @Test
  @DisplayName("管理者トップ画面表示: 正常系")
  void testGetAdmin() throws Exception {
    // 1. Ready

    // 2. Do & 3. Check
    mockMvc.perform(get("/admin")
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/index"));
  }
}
