package com.sysdev.slat.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.sysdev.slat.user.UserData;

@SpringBootTest
@AutoConfigureMockMvc
public class MainControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final String SESSION_KEY = "userData";

  @Test
  @DisplayName("トップ画面表示: ログイン済みの場合（表示名がModelに含まれる）")
  void testIndexLoggedIn() throws Exception {
    // 1. Ready
    // セッションに入れるユーザーデータを作成
    UserData mockUser = new UserData();
    mockUser.setUserId("user001");
    mockUser.setDisplayName("山田 太郎");

    // 2. Do & 3. Check
    mockMvc.perform(get("/") // "/" と "/home" 両方対応していますが、代表して "/"
        // ★ここがポイント: セッションにデータを埋め込む
        .sessionAttr(SESSION_KEY, mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(model().attribute("title", "トップページ"))
        // ログインしているので displayName があるはず
        .andExpect(model().attribute("displayName", "山田 太郎"));
  }

  @Test
  @DisplayName("トップ画面表示: 未ログインの場合（表示名がModelに含まれない）")
  void testIndexNotLoggedIn() throws Exception {
    // 1. Ready (特になし)

    // 2. Do & 3. Check
    mockMvc.perform(get("/home")) // URLパターンのもう片方 "/home" もテスト
        // セッションデータなし
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(model().attribute("title", "トップページ"))
        // ログインしていないので displayName は存在しないはず
        .andExpect(model().attributeDoesNotExist("displayName"));
  }
}
