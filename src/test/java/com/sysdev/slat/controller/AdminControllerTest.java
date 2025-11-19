package com.sysdev.slat.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("管理者トップ画面表示: 正常系")
  void testGetAdmin() throws Exception {
    // 1. Ready (特になし)

    // 2. Do & 3. Check
    // GETリクエストを送り、ステータス200とビュー名を確認
    mockMvc.perform(get("/admin"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/index"));
  }
}
