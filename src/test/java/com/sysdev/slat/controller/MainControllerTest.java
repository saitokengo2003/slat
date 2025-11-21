package com.sysdev.slat.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.sysdev.slat.chat.Group;
import com.sysdev.slat.chat.GroupRepository;
import com.sysdev.slat.user.UserData;
import com.sysdev.slat.user.UserService;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
public class MainControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @MockBean
  private GroupRepository groupRepository;

  private final String SESSION_KEY = "userData";

  // ===========================
  // 1. ログイン済み
  // ===========================
  @Test
  @DisplayName("トップ画面表示: ログイン済み（DMユーザーとグループが複数）")
  void testIndexLoggedInWithData() throws Exception {

    UserData mockUser = new UserData();
    mockUser.setUserId("user001");
    mockUser.setDisplayName("山田 太郎");

    UserData other1 = new UserData();
    other1.setUserId("user002");
    other1.setDisplayName("佐藤 花子");

    UserData other2 = new UserData();
    other2.setUserId("user003");
    other2.setDisplayName("鈴木 次郎");

    List<UserData> dmUsers = List.of(other1, other2);

    Group g1 = new Group();
    g1.setId(UUID.randomUUID());
    g1.setName("グループA");

    Group g2 = new Group();
    g2.setId(UUID.randomUUID());
    g2.setName("グループB");

    List<Group> groups = List.of(g1, g2);

    when(userService.findAllOtherUsers("user001")).thenReturn(dmUsers);
    when(groupRepository.findJoinedGroupsByUserId("user001")).thenReturn(groups);

    mockMvc.perform(get("/").sessionAttr(SESSION_KEY, mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(model().attribute("title", "トップページ"))
        .andExpect(model().attribute("displayName", "山田 太郎"))
        .andExpect(model().attribute("otherUsers", dmUsers))
        .andExpect(model().attribute("generalGroups", groups));
  }

  // ===========================
  // 2. 未ログイン → /home へリダイレクト
  // ===========================
  @Test
  @DisplayName("トップ画面表示: 未ログインはリダイレクト")
  void testIndexNotLoggedIn() throws Exception {

    mockMvc.perform(get("/"))
        .andExpect(status().is3xxRedirection()) // 302 OK
        .andExpect(redirectedUrl("/login")); // リダイレクト先のみ確認
    // ★リダイレクト時に ModelAndView は存在しないので
    // view().name() や model() を書いてはいけない
  }
}
