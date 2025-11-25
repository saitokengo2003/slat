package com.sysdev.slat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sysdev.slat.chat.ChatRequest;
import com.sysdev.slat.chat.ChatService;
import com.sysdev.slat.chat.EditDeleteRequest;
import com.sysdev.slat.chat.GroupRepository;
import com.sysdev.slat.chat.MessageHistoryDto;
import com.sysdev.slat.reactions.ReactionRequest;
import com.sysdev.slat.reactions.ReactionService;
import com.sysdev.slat.user.UserData;
import com.sysdev.slat.user.UserService;

@ExtendWith(MockitoExtension.class)
public class ChatControllerTest {

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Mock
  private ChatService chatService;

  @Mock
  private UserService userService;

  @Mock
  private GroupRepository groupRepository;

  @Mock
  private ReactionService reactionService;

  @InjectMocks
  private ChatController chatController;

  private UserData mockUser;
  private final String SESSION_KEY = "userData";

  @BeforeEach
  void setUp() {
    this.mockMvc = MockMvcBuilders.standaloneSetup(chatController)
        .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
        .build();

    mockUser = new UserData();
    mockUser.setUserId("user-001");
    mockUser.setDisplayName("テスト 太郎");
    mockUser.setRoleCode("student");
  }

  // ===================================================================================
  // 1. 画面表示 (GET /chat)
  // ===================================================================================

  @Test
  @DisplayName("画面表示: 正常系")
  void testGetChat() throws Exception {
    doReturn(Collections.emptyList()).when(userService).findAllOtherUsers(anyString());
    doReturn(Collections.emptyList()).when(groupRepository).findJoinedGroupsByUserId(anyString());

    mockMvc.perform(get("/chat")
        .sessionAttr(SESSION_KEY, mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("chat/index"))
        .andExpect(model().attributeExists("loggedInUserId"));
  }

  @Test
  @DisplayName("画面表示: 未ログイン時はリダイレクト")
  void testGetChatNotLoggedIn() throws Exception {
    mockMvc.perform(get("/chat"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  // ===================================================================================
  // 2. メッセージ送信 (POST /api/message/send)
  // ===================================================================================

  @Test
  @DisplayName("送信: 未ログイン")
  void testSendMessageNotLoggedIn() throws Exception {
    ChatRequest request = new ChatRequest();
    mockMvc.perform(post("/api/message/send")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: User not authenticated."));
  }

  @Test
  @DisplayName("送信: 正常系 (DM)")
  void testSendMessageSuccess() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setRecipientId("target-user");
    request.setBody("Hello");

    mockMvc.perform(post("/api/message/send")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("SUCCESS"));

    verify(chatService).saveChatMessage(any(ChatRequest.class));
  }

  @Test
  @DisplayName("送信: 正常系 (グループ)")
  void testSendMessageSuccess_Group() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setGroupId("group-A");
    request.setRecipientId(null); // nullにする
    request.setBody("Hello Group");

    mockMvc.perform(post("/api/message/send")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("SUCCESS"));

    verify(chatService).saveChatMessage(any(ChatRequest.class));
  }

  @Test
  @DisplayName("送信: バリデーション (宛先なし)")
  void testSendMessageNoRecipient() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setBody("Hello");
    // RecipientId=null, GroupId=null

    mockMvc.perform(post("/api/message/send")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: Message body, recipient ID, or group ID is missing."));
  }

  @Test
  @DisplayName("送信: バリデーション (Bodyがnull)")
  void testSendMessageNullBody() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setRecipientId("user");
    request.setBody(null);

    mockMvc.perform(post("/api/message/send")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: Message body, recipient ID, or group ID is missing."));
  }

  @Test
  @DisplayName("送信: バリデーション (Bodyが空白のみ)")
  void testSendMessageEmptyBody() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setRecipientId("user");
    request.setBody("   "); // trim().isEmpty()

    mockMvc.perform(post("/api/message/send")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: Message body, recipient ID, or group ID is missing."));
  }

  @Test
  @DisplayName("送信: 権限エラー (SecurityException)")
  void testSendMessageSecurityError() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setRecipientId("target");
    request.setBody("Msg");

    doThrow(new SecurityException("権限エラー")).when(chatService).saveChatMessage(any());

    mockMvc.perform(post("/api/message/send")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("権限エラー"));
  }

  @Test
  @DisplayName("送信: 予期せぬエラー (Exception)")
  void testSendMessageGeneralException() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setRecipientId("target");
    request.setBody("Msg");

    doThrow(new RuntimeException("DB Error")).when(chatService).saveChatMessage(any());

    mockMvc.perform(post("/api/message/send")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ERROR: Failed to save message")));
  }

  // ===================================================================================
  // 3. DM履歴取得 (GET /api/dm/history)
  // ===================================================================================

  @Test
  @DisplayName("DM履歴: 未ログイン")
  void testGetDmHistoryNotLoggedIn() throws Exception {
    mockMvc.perform(get("/api/dm/history")
        .param("recipientId", "target"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("DM履歴: 正常系")
  void testGetDmHistory() throws Exception {
    MessageHistoryDto dto = new MessageHistoryDto();
    dto.setBody("DM Body");
    doReturn(List.of(dto)).when(chatService).getDmHistory(anyString(), anyString());

    mockMvc.perform(get("/api/dm/history")
        .sessionAttr(SESSION_KEY, mockUser)
        .param("recipientId", "target-user"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].body").value("DM Body"));
  }

  @Test
  @DisplayName("DM履歴: 予期せぬエラー")
  void testGetDmHistoryException() throws Exception {
    doThrow(new RuntimeException("DB Error")).when(chatService).getDmHistory(anyString(), anyString());

    mockMvc.perform(get("/api/dm/history")
        .sessionAttr(SESSION_KEY, mockUser)
        .param("recipientId", "target-user"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // ===================================================================================
  // 4. グループ履歴取得 (GET /api/group/history)
  // ===================================================================================

  @Test
  @DisplayName("グループ履歴: 未ログイン")
  void testGetGroupHistoryNotLoggedIn() throws Exception {
    mockMvc.perform(get("/api/group/history")
        .param("groupId", "group-A"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("グループ履歴: 正常系")
  void testGetGroupHistory() throws Exception {
    MessageHistoryDto dto = new MessageHistoryDto();
    dto.setBody("Group Body");
    doReturn(List.of(dto)).when(chatService).getGroupHistory(anyString());

    mockMvc.perform(get("/api/group/history")
        .sessionAttr(SESSION_KEY, mockUser)
        .param("groupId", "group-A"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].body").value("Group Body"));
  }

  @Test
  @DisplayName("グループ履歴: 予期せぬエラー")
  void testGetGroupHistoryException() throws Exception {
    doThrow(new RuntimeException("DB Error")).when(chatService).getGroupHistory(anyString());

    mockMvc.perform(get("/api/group/history")
        .sessionAttr(SESSION_KEY, mockUser)
        .param("groupId", "group-A"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // ===================================================================================
  // 5. リアクション操作 (POST /api/reaction/toggle)
  // ===================================================================================

  @Test
  @DisplayName("リアクション: 未ログイン")
  void testToggleReactionNotLoggedIn() throws Exception {
    ReactionRequest request = new ReactionRequest();
    mockMvc.perform(post("/api/reaction/toggle")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: User not authenticated."));
  }

  @Test
  @DisplayName("リアクション: 追加 (ADDED)")
  void testToggleReactionAdded() throws Exception {
    ReactionRequest request = new ReactionRequest();
    request.setMessageId(UUID.randomUUID());
    request.setEmoji("👍");

    doReturn(true).when(reactionService).toggleReaction(any(UUID.class), anyString(), anyString());

    mockMvc.perform(post("/api/reaction/toggle")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ADDED"));
  }

  @Test
  @DisplayName("リアクション: 削除 (REMOVED)")
  void testToggleReactionRemoved() throws Exception {
    ReactionRequest request = new ReactionRequest();
    request.setMessageId(UUID.randomUUID());
    request.setEmoji("👍");

    doReturn(false).when(reactionService).toggleReaction(any(UUID.class), anyString(), anyString());

    mockMvc.perform(post("/api/reaction/toggle")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("REMOVED"));
  }

  @Test
  @DisplayName("リアクション: バリデーション (MessageIDなし)")
  void testToggleReactionNoMessageId() throws Exception {
    ReactionRequest request = new ReactionRequest();
    request.setEmoji("👍");

    mockMvc.perform(post("/api/reaction/toggle")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ERROR: Message ID or emoji is missing.")));
  }

  @Test
  @DisplayName("リアクション: バリデーション (Emojiがnull)")
  void testToggleReactionNullEmoji() throws Exception {
    ReactionRequest request = new ReactionRequest();
    request.setMessageId(UUID.randomUUID());
    request.setEmoji(null);

    mockMvc.perform(post("/api/reaction/toggle")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ERROR: Message ID or emoji is missing.")));
  }

  @Test
  @DisplayName("リアクション: バリデーション (Emojiが空文字)")
  void testToggleReactionEmptyEmoji() throws Exception {
    ReactionRequest request = new ReactionRequest();
    request.setMessageId(UUID.randomUUID());
    request.setEmoji("");

    mockMvc.perform(post("/api/reaction/toggle")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ERROR: Message ID or emoji is missing.")));
  }

  @Test
  @DisplayName("リアクション: 予期せぬエラー")
  void testToggleReactionException() throws Exception {
    ReactionRequest request = new ReactionRequest();
    request.setMessageId(UUID.randomUUID());
    request.setEmoji("👍");

    doThrow(new RuntimeException("DB Error")).when(reactionService).toggleReaction(any(), anyString(), anyString());

    mockMvc.perform(post("/api/reaction/toggle")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ERROR: Failed to toggle reaction.")));
  }

  // ===================================================================================
  // 6. メッセージ削除 (POST /api/message/delete)
  // ===================================================================================

  @Test
  @DisplayName("削除: 未ログイン")
  void testDeleteMessageNotLoggedIn() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());

    mockMvc.perform(post("/api/message/delete")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: User not authenticated or missing message ID."));
  }

  @Test
  @DisplayName("削除: メッセージIDなし")
  void testDeleteMessageNoId() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();

    mockMvc.perform(post("/api/message/delete")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: User not authenticated or missing message ID."));
  }

  @Test
  @DisplayName("削除: 正常系")
  void testDeleteMessageSuccess() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());

    mockMvc.perform(post("/api/message/delete")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("SUCCESS"));

    verify(chatService).deleteMessage(eq(request.getMessageId()), eq(mockUser.getUserId()));
  }

  @Test
  @DisplayName("削除: 権限エラー")
  void testDeleteMessageSecurityError() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());

    doThrow(new SecurityException("削除権限がありません")).when(chatService).deleteMessage(any(), any());

    mockMvc.perform(post("/api/message/delete")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("削除権限がありません"));
  }

  @Test
  @DisplayName("削除: 引数エラー (IllegalArgumentException)")
  void testDeleteMessageIllegalArgument() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());

    doThrow(new IllegalArgumentException("不正なID")).when(chatService).deleteMessage(any(), any());

    mockMvc.perform(post("/api/message/delete")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: 不正なID"));
  }

  @Test
  @DisplayName("削除: 予期せぬエラー")
  void testDeleteMessageGeneralException() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());

    doThrow(new RuntimeException("DB Error")).when(chatService).deleteMessage(any(), any());

    mockMvc.perform(post("/api/message/delete")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: Failed to delete message"));
  }

  // ===================================================================================
  // 7. メッセージ編集 (POST /api/message/edit)
  // ===================================================================================

  @Test
  @DisplayName("編集: 未ログイン")
  void testEditMessageNotLoggedIn() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());
    request.setBody("body");

    mockMvc.perform(post("/api/message/edit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: User not authenticated or missing message ID/body."));
  }

  @Test
  @DisplayName("編集: 本文なし")
  void testEditMessageNoBody() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());
    request.setBody(null);

    mockMvc.perform(post("/api/message/edit")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: User not authenticated or missing message ID/body."));
  }

  @Test
  @DisplayName("編集: IDなし (追加)")
  void testEditMessageNoId() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setBody("Body");

    mockMvc.perform(post("/api/message/edit")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: User not authenticated or missing message ID/body."));
  }

  @Test
  @DisplayName("編集: 正常系")
  void testEditMessageSuccess() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());
    request.setBody("New Body");

    mockMvc.perform(post("/api/message/edit")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("SUCCESS"));

    verify(chatService).editMessage(eq(request.getMessageId()), eq("New Body"), eq(mockUser.getUserId()));
  }

  @Test
  @DisplayName("編集: 権限エラー")
  void testEditMessageSecurityError() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());
    request.setBody("body");

    doThrow(new SecurityException("編集権限なし")).when(chatService).editMessage(any(), anyString(), anyString());

    mockMvc.perform(post("/api/message/edit")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("編集権限なし"));
  }

  @Test
  @DisplayName("編集: 引数エラー (IllegalArgumentException)")
  void testEditMessageIllegalArgument() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());
    request.setBody("body");

    doThrow(new IllegalArgumentException("メッセージが見つかりません")).when(chatService).editMessage(any(), anyString(),
        anyString());

    mockMvc.perform(post("/api/message/edit")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ERROR: メッセージが見つかりません"));
  }

  @Test
  @DisplayName("編集: 予期せぬエラー")
  void testEditMessageGeneralException() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());
    request.setBody("body");

    doThrow(new RuntimeException("Fatal Error")).when(chatService).editMessage(any(), anyString(), anyString());

    mockMvc.perform(post("/api/message/edit")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ERROR: Failed to edit message")));
  }
}
