package com.sysdev.slat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysdev.slat.chat.ChatRequest;
import com.sysdev.slat.chat.ChatService;
import com.sysdev.slat.chat.Group;
import com.sysdev.slat.chat.GroupRepository;
import com.sysdev.slat.chat.MessageHistoryDto;
import com.sysdev.slat.user.UserData;
import com.sysdev.slat.user.UserService;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper; // JSON変換用

  @MockBean
  private ChatService chatService;

  @MockBean
  private UserService userService;

  @MockBean
  private GroupRepository groupRepository;

  // テストで使い回すログインユーザー情報
  private UserData mockUser;
  private final String SESSION_KEY = "userData";

  @BeforeEach
  void setUp() {
    mockUser = new UserData();
    mockUser.setUserId("user-001");
    mockUser.setDisplayName("テスト 太郎");
  }

  // --- 1. チャット画面表示 (GET /chat) ---

  @Test
  @DisplayName("画面表示: 未ログイン時はログイン画面へリダイレクト")
  void testGetChatNotLoggedIn() throws Exception {
    mockMvc.perform(get("/chat")) // セッションなし
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  @DisplayName("画面表示: ログイン時はチャット画面を表示し、Modelに必要な情報をセットする")
  void testGetChatLoggedIn() throws Exception {
    // 1. Ready
    List<UserData> otherUsers = List.of(new UserData()); // 中身は適当でOK
    List<Group> groups = List.of(new Group());

    doReturn(otherUsers).when(userService).findAllOtherUsers("user-001");
    doReturn(groups).when(groupRepository).findJoinedGroupsByUserId("user-001");

    // 2. Do & 3. Assert
    mockMvc.perform(get("/chat")
        .sessionAttr(SESSION_KEY, mockUser)) // ★セッションにユーザー情報をセット
        .andExpect(status().isOk())
        .andExpect(view().name("chat/index"))
        .andExpect(model().attribute("loggedInUserId", "user-001"))
        .andExpect(model().attribute("displayName", "テスト 太郎"))
        .andExpect(model().attribute("otherUsers", otherUsers))
        .andExpect(model().attribute("generalGroups", groups));
  }

  // --- 2. メッセージ送信 (POST /api/message/send) ---

  @Test
  @DisplayName("送信: 正常系 (DM)")
  void testSendMessageSuccess() throws Exception {
    // 1. Ready
    ChatRequest request = new ChatRequest();
    request.setRecipientId("friend-001");
    request.setBody("こんにちは");

    // 2. Do & 3. Assert
    mockMvc.perform(post("/api/message/send")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))) // リクエストボディをJSON化
        .andExpect(status().isOk())
        .andExpect(content().string("OK"));

    // Serviceが呼ばれたか検証
    verify(chatService).saveChatMessage(any(ChatRequest.class));
  }

  @Test
  @DisplayName("送信: 未ログイン時はエラー文字列を返す")
  void testSendMessageNotLoggedIn() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setRecipientId("friend-001");
    request.setBody("Hello");

    mockMvc.perform(post("/api/message/send")
        // セッションなし
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk()) // REST API的には200 OKだが、中身がエラーメッセージ
        .andExpect(content().string("ERROR: User not authenticated."));
  }

  @Test
  @DisplayName("送信: バリデーションエラー (本文なし)")
  void testSendMessageValidationEmptyBody() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setRecipientId("friend-001");
    request.setBody(""); // 空文字

    mockMvc.perform(post("/api/message/send")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: Message body, recipient ID, or group ID is missing."));
  }

  @Test
  @DisplayName("送信: サーバーエラー発生時")
  void testSendMessageException() throws Exception {
    // 1. Ready
    ChatRequest request = new ChatRequest();
    request.setGroupId("group-A");
    request.setBody("Hello Group");

    // Serviceが例外を投げるように設定
    doThrow(new RuntimeException("DB Error")).when(chatService).saveChatMessage(any(ChatRequest.class));

    // 2. Do & 3. Assert
    mockMvc.perform(post("/api/message/send")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: Failed to save message due to internal server error."));
  }

  // --- 3. DM履歴取得 (GET /api/dm/history) ---

  @Test
  @DisplayName("DM履歴: 正常にリストを取得できる")
  void testGetDmHistory() throws Exception {
    // 1. Ready
    MessageHistoryDto msg1 = new MessageHistoryDto(); // 必要に応じてフィールドセット
    List<MessageHistoryDto> historyList = List.of(msg1);

    doReturn(historyList).when(chatService).getDmHistory("user-001", "friend-99");

    // 2. Do & 3. Assert
    mockMvc.perform(get("/api/dm/history")
        .param("recipientId", "friend-99")
        .sessionAttr(SESSION_KEY, mockUser))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1)); // 配列サイズが1であること
  }

  @Test
  @DisplayName("DM履歴: 未ログイン時は空リスト")
  void testGetDmHistoryNotLoggedIn() throws Exception {
    mockMvc.perform(get("/api/dm/history")
        .param("recipientId", "friend-99"))
        // セッションなし
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // --- 4. グループ履歴取得 (GET /api/group/history) ---

  @Test
  @DisplayName("グループ履歴: 正常にリストを取得できる")
  void testGetGroupHistory() throws Exception {
    // 1. Ready
    MessageHistoryDto msg1 = new MessageHistoryDto();
    MessageHistoryDto msg2 = new MessageHistoryDto();
    List<MessageHistoryDto> historyList = List.of(msg1, msg2);

    doReturn(historyList).when(chatService).getGroupHistory("group-X");

    // 2. Do & 3. Assert
    mockMvc.perform(get("/api/group/history")
        .param("groupId", "group-X")
        .sessionAttr(SESSION_KEY, mockUser))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @DisplayName("グループ履歴: 例外発生時は空リスト")
  void testGetGroupHistoryException() throws Exception {
    // 1. Ready
    doThrow(new RuntimeException("Fetch error")).when(chatService).getGroupHistory(anyString());

    // 2. Do & 3. Assert
    mockMvc.perform(get("/api/group/history")
        .param("groupId", "group-Error")
        .sessionAttr(SESSION_KEY, mockUser))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0)); // 空配列 []
  }
}
