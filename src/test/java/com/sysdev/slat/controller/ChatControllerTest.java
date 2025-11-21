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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysdev.slat.chat.ChatRequest;
import com.sysdev.slat.chat.ChatService;
import com.sysdev.slat.chat.EditDeleteRequest;
import com.sysdev.slat.chat.GroupRepository;
import com.sysdev.slat.chat.MessageHistoryDto;
import com.sysdev.slat.reactions.ReactionRequest;
import com.sysdev.slat.reactions.ReactionService;
import com.sysdev.slat.user.UserData;
import com.sysdev.slat.user.UserService;

@SpringBootTest
@AutoConfigureMockMvc
public class ChatControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ChatService chatService;

  @MockBean
  private UserService userService;

  @MockBean
  private GroupRepository groupRepository;

  @MockBean
  private ReactionService reactionService;

  private UserData mockUser;
  private final String SESSION_KEY = "userData";

  @BeforeEach
  void setUp() {
    mockUser = new UserData();
    mockUser.setUserId("user-001");
    mockUser.setDisplayName("テスト 太郎");
    mockUser.setRoleCode("student");
  }

  // --- 1. 画面表示 (GET /chat) ---

  @Test
  @DisplayName("画面表示: 正常系")
  void testGetChat() throws Exception {
    doReturn(Collections.emptyList()).when(userService).findAllOtherUsers(anyString());
    doReturn(Collections.emptyList()).when(groupRepository).findJoinedGroupsByUserId(anyString());

    mockMvc.perform(get("/chat")
        .sessionAttr(SESSION_KEY, mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("chat/index"))
        .andExpect(model().attributeExists("loggedInUserId"))
        .andExpect(model().attributeExists("otherUsers"))
        .andExpect(model().attributeExists("generalGroups"));
  }

  @Test
  @DisplayName("画面表示: 未ログイン時はリダイレクト")
  void testGetChatNotLoggedIn() throws Exception {
    mockMvc.perform(get("/chat"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  // --- 2. メッセージ送信 (POST /api/message/send) ---

  @Test
  @DisplayName("送信: 正常系")
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
  @DisplayName("送信: バリデーションエラー (本文なし)")
  void testSendMessageValidationError() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setRecipientId("target-user");
    request.setBody(""); // 空文字

    mockMvc.perform(post("/api/message/send")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ERROR: Message body")));
  }

  @Test
  @DisplayName("送信: 権限エラー (SecurityException)")
  void testSendMessageSecurityError() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setRecipientId("target");
    request.setBody("Expired Msg");

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

  // --- 3. DM履歴取得 (GET /api/dm/history) ---

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
  @DisplayName("DM履歴: 例外発生時は空リスト")
  void testGetDmHistoryException() throws Exception {
    doThrow(new RuntimeException("DB Error")).when(chatService).getDmHistory(anyString(), anyString());

    mockMvc.perform(get("/api/dm/history")
        .sessionAttr(SESSION_KEY, mockUser)
        .param("recipientId", "target-user"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // --- 4. グループ履歴取得 (GET /api/group/history) ---

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
  @DisplayName("グループ履歴: 例外発生時は空リスト")
  void testGetGroupHistoryException() throws Exception {
    doThrow(new RuntimeException("DB Error")).when(chatService).getGroupHistory(anyString());

    mockMvc.perform(get("/api/group/history")
        .sessionAttr(SESSION_KEY, mockUser)
        .param("groupId", "group-A"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // --- 5. リアクション操作 (POST /api/reaction/toggle) ---

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
  @DisplayName("リアクション: バリデーションエラー (IDなし)")
  void testToggleReactionValidationError() throws Exception {
    ReactionRequest request = new ReactionRequest();
    // ID, Emoji なし

    mockMvc.perform(post("/api/reaction/toggle")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ERROR: Message ID or emoji is missing")));
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
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ERROR: Failed to toggle reaction")));
  }

  // --- 6. メッセージ削除 (POST /api/message/delete) ---

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
  @DisplayName("削除: バリデーションエラー (IDなし)")
  void testDeleteMessageValidationError() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    // ID なし

    mockMvc.perform(post("/api/message/delete")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content()
            .string(org.hamcrest.Matchers.containsString("ERROR: User not authenticated or missing message ID")));
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
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ERROR: Failed to delete message")));
  }

  // --- 7. メッセージ編集 (POST /api/message/edit) ---

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
  @DisplayName("編集: バリデーションエラー (本文なし)")
  void testEditMessageValidationError() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());
    // Body なし

    mockMvc.perform(post("/api/message/edit")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content()
            .string(org.hamcrest.Matchers.containsString("ERROR: User not authenticated or missing message ID/body")));
  }

  @Test
  @DisplayName("編集: 失敗 (対象なし/IllegalArgumentException)")
  void testEditMessageError() throws Exception {
    EditDeleteRequest request = new EditDeleteRequest();
    request.setMessageId(UUID.randomUUID());
    request.setBody("New Body");

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
    request.setBody("New Body");

    doThrow(new RuntimeException("DB Error")).when(chatService).editMessage(any(), anyString(), anyString());

    mockMvc.perform(post("/api/message/edit")
        .sessionAttr(SESSION_KEY, mockUser)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ERROR: Failed to edit message")));
  }
}
