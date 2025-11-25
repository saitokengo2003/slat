package com.sysdev.slat.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sysdev.slat.chat.GroupRepository;
import com.sysdev.slat.user.UserData;
import com.sysdev.slat.user.UserService;

@ExtendWith(MockitoExtension.class)
public class MainControllerTest {

  private MockMvc mockMvc;

  @Mock
  private UserService userService;

  @Mock
  private GroupRepository groupRepository;

  @InjectMocks
  private MainController mainController;

  private final String SESSION_KEY = "userData";
  private UserData mockUser;

  @BeforeEach
  void setUp() {
    this.mockMvc = MockMvcBuilders.standaloneSetup(mainController).build();

    mockUser = new UserData();
    mockUser.setUserId("user001");
    mockUser.setDisplayName("山田 太郎");
    mockUser.setRoleCode("STUDENT");
  }

  @Test
  @DisplayName("トップ画面表示: ログイン済み (URL: / )")
  void testIndexLoggedIn() throws Exception {
    // 1. Ready
    doReturn(Collections.emptyList()).when(userService).findAllOtherUsers(anyString());
    doReturn(Collections.emptyList()).when(groupRepository).findJoinedGroupsByUserId(anyString());

    // 2. Do & 3. Check
    mockMvc.perform(get("/")
        .sessionAttr(SESSION_KEY, mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(model().attribute("title", "トップページ"))
        .andExpect(model().attribute("displayName", "山田 太郎"))
        .andExpect(model().attribute("loggedInUserId", "user001"))
        .andExpect(model().attribute("role", "STUDENT"))
        .andExpect(model().attributeExists("otherUsers"))
        .andExpect(model().attributeExists("generalGroups"));
  }

  @Test
  @DisplayName("トップ画面表示: 未ログイン (URL: /home )")
  void testIndexNotLoggedIn() throws Exception {
    // 1. Ready

    // 2. Do & 3. Check
    mockMvc.perform(get("/home"))
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(model().attribute("title", "トップページ"))
        .andExpect(model().attributeDoesNotExist("displayName"))
        .andExpect(model().attributeDoesNotExist("loggedInUserId"))
        .andExpect(model().attributeDoesNotExist("generalGroups"));
  }
}
