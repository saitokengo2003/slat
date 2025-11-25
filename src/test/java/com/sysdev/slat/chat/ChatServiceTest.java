package com.sysdev.slat.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sysdev.slat.reactions.DmReactionEntity;
import com.sysdev.slat.reactions.ReactionEntity;
import com.sysdev.slat.reactions.ReactionService;
import com.sysdev.slat.user.UserService;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

  @Mock
  private ChatRepository chatRepository;

  @Mock
  private ReactionService reactionService;

  @Mock
  private UserService userService;

  @InjectMocks
  private ChatService target;

  private final UUID MSG_ID = UUID.randomUUID();
  private final String SENDER_ID = "teacher1";
  private final String RECIPIENT_ID = "student1";
  private final String GROUP_ID = "group-A";

  @Test
  @DisplayName("saveChatMessage: DM送信 (期限なし)")
  void testSaveChatMessageDm() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(SENDER_ID);
    req.setRecipientId(RECIPIENT_ID);
    req.setBody("Hello");

    target.saveChatMessage(req);

    verify(chatRepository).saveDmMessage(req);
    verify(chatRepository, never()).saveGroupMessage(any());
  }

  @Test
  @DisplayName("saveChatMessage: グループ送信 (期限なし)")
  void testSaveChatMessageGroup() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(SENDER_ID);
    req.setGroupId(GROUP_ID);
    req.setBody("Hello Group");

    target.saveChatMessage(req);

    verify(chatRepository).saveGroupMessage(req);
    verify(chatRepository, never()).saveDmMessage(any());
  }

  @Test
  @DisplayName("saveChatMessage: 空文字IDのハンドリング (GroupIdが空文字ならDM判定へ)")
  void testSaveChatMessageEmptyGroupId() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(SENDER_ID);
    req.setGroupId("");
    req.setRecipientId(RECIPIENT_ID);
    req.setBody("Hello");

    target.saveChatMessage(req);

    verify(chatRepository).saveDmMessage(req);
  }

  @Test
  @DisplayName("saveChatMessage: RecipientIdが空文字の場合 (分岐網羅 -> 例外)")
  void testSaveChatMessage_RecipientIdEmptyString() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(SENDER_ID);
    req.setBody("Body");
    req.setGroupId(null);
    req.setRecipientId("");

    assertThrows(IllegalArgumentException.class, () -> target.saveChatMessage(req));
  }

  @Test
  @DisplayName("saveChatMessage: 期限付き (Teacher権限あり)")
  void testSaveChatMessageExpiredAllowedTeacher() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(SENDER_ID);
    req.setRecipientId(RECIPIENT_ID);
    req.setBody("Expired Message");
    req.setExpirationTime(OffsetDateTime.now().plusDays(1));

    doReturn("teacher").when(userService).getUserRole(SENDER_ID);

    target.saveChatMessage(req);

    verify(chatRepository).saveDmMessage(req);
  }

  @Test
  @DisplayName("saveChatMessage: 期限付き (Admin権限あり)")
  void testSaveChatMessageExpiredAllowedAdmin() {
    ChatRequest req = new ChatRequest();
    req.setSenderId("admin_user");
    req.setRecipientId(RECIPIENT_ID);
    req.setBody("Expired Message");
    req.setExpirationTime(OffsetDateTime.now().plusDays(1));

    doReturn("admin").when(userService).getUserRole("admin_user");

    target.saveChatMessage(req);

    verify(chatRepository).saveDmMessage(req);
  }

  @Test
  @DisplayName("saveChatMessage: 期限付き (Student権限なし -> エラー)")
  void testSaveChatMessageExpiredForbidden() {
    ChatRequest req = new ChatRequest();
    req.setSenderId("student_user");
    req.setRecipientId(RECIPIENT_ID);
    req.setBody("Expired Message");
    req.setExpirationTime(OffsetDateTime.now().plusDays(1));

    doReturn("student").when(userService).getUserRole("student_user");

    SecurityException e = assertThrows(SecurityException.class, () -> target.saveChatMessage(req));
    assertTrue(e.getMessage().contains("権限エラー"));
  }

  @Test
  @DisplayName("saveChatMessage: バリデーションエラー (SenderIdなし)")
  void testSaveChatMessageNoSenderId() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(null);
    req.setBody("Body");
    assertThrows(IllegalArgumentException.class, () -> target.saveChatMessage(req));
  }

  @Test
  @DisplayName("saveChatMessage: バリデーションエラー (Bodyなし)")
  void testSaveChatMessageInvalidBody() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(SENDER_ID);
    req.setBody(null);
    assertThrows(IllegalArgumentException.class, () -> target.saveChatMessage(req));
  }

  @Test
  @DisplayName("saveChatMessage: バリデーションエラー (Body空白のみ)")
  void testSaveChatMessageBodyWhitespace() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(SENDER_ID);
    req.setRecipientId(RECIPIENT_ID);
    req.setBody("   ");
    assertThrows(IllegalArgumentException.class, () -> target.saveChatMessage(req));
  }

  @Test
  @DisplayName("saveChatMessage: バリデーションエラー (宛先なし)")
  void testSaveChatMessageNoRecipient() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(SENDER_ID);
    req.setBody("Body");
    req.setGroupId("");
    req.setRecipientId(null);
    assertThrows(IllegalArgumentException.class, () -> target.saveChatMessage(req));
  }

  @Test
  @DisplayName("getDmHistory: リアクションあり (DmReactionEntity -> ReactionEntity 変換確認)")
  void testGetDmHistoryWithReactions() {
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setBody("Msg with Reaction");
    msg.setExpirationTime(null);

    doReturn(List.of(msg)).when(chatRepository).findDmHistory(SENDER_ID, RECIPIENT_ID);

    DmReactionEntity dmReaction = new DmReactionEntity();
    dmReaction.setDmMessageId(MSG_ID);
    dmReaction.setUserId(RECIPIENT_ID);
    dmReaction.setEmoji("😍");
    dmReaction.setCreatedAt(OffsetDateTime.now());

    doReturn(List.of(dmReaction)).when(reactionService).getDmReactionsByMessageIds(anyList());

    List<MessageHistoryDto> result = target.getDmHistory(SENDER_ID, RECIPIENT_ID);

    assertEquals(1, result.size());
    assertNotNull(result.get(0).getReactions());
    assertEquals(1, result.get(0).getReactions().size());

    ReactionEntity converted = result.get(0).getReactions().get(0);
    assertEquals("😍", converted.getEmoji());
    assertEquals(RECIPIENT_ID, converted.getUserId());
  }

  @Test
  @DisplayName("getDmHistory: 期限切れメッセージ (未読ロジック実行)")
  void testGetDmHistoryExpired_LogicRun() {
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setBody("Expired Msg");
    msg.setExpirationTime(OffsetDateTime.now().minusDays(1));

    doReturn(List.of(msg)).when(chatRepository).findDmHistory(SENDER_ID, RECIPIENT_ID);
    doReturn(Collections.emptyList()).when(reactionService).getDmReactionsByMessageIds(anyList());
    doReturn(false).when(chatRepository).isGroupMessage(MSG_ID);
    doReturn(true).when(chatRepository).isDmMessage(MSG_ID);

    doReturn(List.of(SENDER_ID, RECIPIENT_ID)).when(chatRepository).getDmParticipants(MSG_ID);
    doReturn(Collections.emptyList()).when(reactionService).getDmReactionsBefore(eq(MSG_ID), any(OffsetDateTime.class));
    doReturn(List.of(RECIPIENT_ID)).when(userService).getAllStudentIds();
    doReturn("生徒 太郎").when(userService).getDisplayName(RECIPIENT_ID);

    List<MessageHistoryDto> result = target.getDmHistory(SENDER_ID, RECIPIENT_ID);

    assertEquals(1, result.size());
    assertEquals(1, result.get(0).getNonReactingStudentNames().size());
    assertEquals("生徒 太郎", result.get(0).getNonReactingStudentNames().get(0));
  }

  @Test
  @DisplayName("getDmHistory: 期限切れ & 一部リアクション済み (フィルター分岐の網羅)")
  void testGetDmHistory_Expired_MixedReaction() {
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setBody("Expired Msg");
    msg.setExpirationTime(OffsetDateTime.now().minusDays(1));

    doReturn(List.of(msg)).when(chatRepository).findDmHistory(SENDER_ID, RECIPIENT_ID);
    doReturn(Collections.emptyList()).when(reactionService).getDmReactionsByMessageIds(anyList());

    doReturn(false).when(chatRepository).isGroupMessage(MSG_ID);
    doReturn(true).when(chatRepository).isDmMessage(MSG_ID);
    String studentReacted = "student_A";
    String studentNoReact = "student_B";
    doReturn(List.of(studentReacted, studentNoReact)).when(chatRepository).getDmParticipants(MSG_ID);
    DmReactionEntity reaction = new DmReactionEntity();
    reaction.setUserId(studentReacted);
    doReturn(List.of(reaction)).when(reactionService).getDmReactionsBefore(eq(MSG_ID), any(OffsetDateTime.class));

    doReturn(List.of(studentReacted, studentNoReact)).when(userService).getAllStudentIds();
    doReturn("生徒B(未読)").when(userService).getDisplayName(studentNoReact);

    List<MessageHistoryDto> result = target.getDmHistory(SENDER_ID, RECIPIENT_ID);

    List<String> names = result.get(0).getNonReactingStudentNames();
    assertEquals(1, names.size());
    assertEquals("生徒B(未読)", names.get(0));
  }

  @Test
  @DisplayName("getDmHistory: 期限が未来 (未読ロジックはスキップ)")
  void testGetDmHistoryFuture_LogicSkip() {
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setExpirationTime(OffsetDateTime.now().plusDays(1));

    doReturn(List.of(msg)).when(chatRepository).findDmHistory(SENDER_ID, RECIPIENT_ID);
    doReturn(Collections.emptyList()).when(reactionService).getDmReactionsByMessageIds(anyList());

    List<MessageHistoryDto> result = target.getDmHistory(SENDER_ID, RECIPIENT_ID);

    assertEquals(1, result.size());
    assertTrue(result.get(0).getNonReactingStudentNames().isEmpty());
    verify(chatRepository, never()).isDmMessage(any());
  }

  @Test
  @DisplayName("getDmHistory: 期限あり/なし混在 (カバレッジ網羅)")
  void testGetDmHistory_Mixed_Coverage() {
    // 1. 期限あり
    MessageHistoryDto msg1 = new MessageHistoryDto();
    msg1.setMessageId(UUID.randomUUID());
    msg1.setBody("Expired");
    msg1.setExpirationTime(OffsetDateTime.now().minusDays(1));

    // 2. 期限なし
    MessageHistoryDto msg2 = new MessageHistoryDto();
    msg2.setMessageId(UUID.randomUUID());
    msg2.setBody("Normal");
    msg2.setExpirationTime(null);

    doReturn(List.of(msg1, msg2)).when(chatRepository).findDmHistory(SENDER_ID, RECIPIENT_ID);
    doReturn(Collections.emptyList()).when(reactionService).getDmReactionsByMessageIds(anyList());
    doReturn(true).when(chatRepository).isDmMessage(msg1.getMessageId());
    doReturn(List.of(RECIPIENT_ID)).when(chatRepository).getDmParticipants(msg1.getMessageId());
    doReturn(Collections.emptyList()).when(reactionService).getDmReactionsBefore(eq(msg1.getMessageId()), any());
    doReturn(List.of(RECIPIENT_ID)).when(userService).getAllStudentIds();
    doReturn("Student1").when(userService).getDisplayName(RECIPIENT_ID);

    // 実行
    List<MessageHistoryDto> result = target.getDmHistory(SENDER_ID, RECIPIENT_ID);

    assertEquals(2, result.size());
    assertFalse(result.get(0).getNonReactingStudentNames().isEmpty());
    assertTrue(result.get(1).getNonReactingStudentNames().isEmpty());
  }

  @Test
  @DisplayName("getDmHistory: 履歴が空の場合")
  void testGetDmHistoryEmpty() {
    doReturn(Collections.emptyList()).when(chatRepository).findDmHistory(SENDER_ID, RECIPIENT_ID);
    List<MessageHistoryDto> result = target.getDmHistory(SENDER_ID, RECIPIENT_ID);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("getDmHistory: メッセージタイプ不明 (DMでもGroupでもない)")
  void testGetDmHistoryUnknownType() {
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setExpirationTime(OffsetDateTime.now().minusDays(1));

    doReturn(List.of(msg)).when(chatRepository).findDmHistory(SENDER_ID, RECIPIENT_ID);
    doReturn(Collections.emptyList()).when(reactionService).getDmReactionsByMessageIds(anyList());

    doReturn(false).when(chatRepository).isGroupMessage(MSG_ID);
    doReturn(false).when(chatRepository).isDmMessage(MSG_ID);

    List<MessageHistoryDto> result = target.getDmHistory(SENDER_ID, RECIPIENT_ID);
    assertTrue(result.get(0).getNonReactingStudentNames().isEmpty());
  }

  @Test
  @DisplayName("getGroupHistory: 期限切れメッセージ (未読ロジック実行)")
  void testGetGroupHistoryExpired_LogicRun() {
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setExpirationTime(OffsetDateTime.now().minusDays(1));

    doReturn(List.of(msg)).when(chatRepository).findGroupHistory(GROUP_ID);
    doReturn(Collections.emptyList()).when(reactionService).getReactionsByMessageIds(anyList());

    doReturn(true).when(chatRepository).isGroupMessage(MSG_ID);
    doReturn(Optional.of(UUID.randomUUID())).when(chatRepository).getGroupIdByMessageId(MSG_ID);
    doReturn(List.of("student1")).when(chatRepository).getGroupMembers(any(UUID.class));
    doReturn(Collections.emptyList()).when(reactionService).getGroupReactionsBefore(eq(MSG_ID),
        any(OffsetDateTime.class));
    doReturn(List.of("student1")).when(userService).getAllStudentIds();
    doReturn("生徒1").when(userService).getDisplayName("student1");

    List<MessageHistoryDto> result = target.getGroupHistory(GROUP_ID);

    assertEquals(1, result.size());
    assertEquals(1, result.get(0).getNonReactingStudentNames().size());
  }

  @Test
  @DisplayName("getGroupHistory: 期限あり/なし混在 (カバレッジ網羅)")
  void testGetGroupHistory_Mixed_Coverage() {
    // 1. 期限あり
    MessageHistoryDto msg1 = new MessageHistoryDto();
    msg1.setMessageId(UUID.randomUUID());
    msg1.setBody("Expired");
    msg1.setExpirationTime(OffsetDateTime.now().minusDays(1));

    // 2. 期限なし
    MessageHistoryDto msg2 = new MessageHistoryDto();
    msg2.setMessageId(UUID.randomUUID());
    msg2.setBody("Normal");
    msg2.setExpirationTime(null);

    doReturn(List.of(msg1, msg2)).when(chatRepository).findGroupHistory(GROUP_ID);
    doReturn(Collections.emptyList()).when(reactionService).getReactionsByMessageIds(anyList());
    doReturn(true).when(chatRepository).isGroupMessage(msg1.getMessageId());
    doReturn(Optional.of(UUID.randomUUID())).when(chatRepository).getGroupIdByMessageId(msg1.getMessageId());
    doReturn(List.of("s1")).when(chatRepository).getGroupMembers(any());
    doReturn(Collections.emptyList()).when(reactionService).getGroupReactionsBefore(any(), any());
    doReturn(List.of("s1")).when(userService).getAllStudentIds();
    doReturn("Student1").when(userService).getDisplayName("s1");

    // 実行
    List<MessageHistoryDto> result = target.getGroupHistory(GROUP_ID);

    assertEquals(2, result.size());
    assertFalse(result.get(0).getNonReactingStudentNames().isEmpty());
    assertTrue(result.get(1).getNonReactingStudentNames().isEmpty());
  }

  @Test
  @DisplayName("getGroupHistory: 履歴空")
  void testGetGroupHistoryEmpty() {
    doReturn(Collections.emptyList()).when(chatRepository).findGroupHistory(GROUP_ID);
    List<MessageHistoryDto> result = target.getGroupHistory(GROUP_ID);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("getGroupHistory: グループID取得失敗 (例外)")
  void testGetGroupHistoryGroupIdNotFound() {
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setExpirationTime(OffsetDateTime.now().minusDays(1));

    doReturn(List.of(msg)).when(chatRepository).findGroupHistory(GROUP_ID);
    doReturn(Collections.emptyList()).when(reactionService).getReactionsByMessageIds(anyList());

    doReturn(true).when(chatRepository).isGroupMessage(MSG_ID);
    doReturn(Optional.empty()).when(chatRepository).getGroupIdByMessageId(MSG_ID);

    assertThrows(IllegalArgumentException.class, () -> target.getGroupHistory(GROUP_ID));
  }

  @Test
  @DisplayName("deleteMessage: 正常系 (自分のメッセージ)")
  void testDeleteMessageSuccess() {
    doReturn(SENDER_ID).when(chatRepository).findSenderIdByMessageId(MSG_ID);
    target.deleteMessage(MSG_ID, SENDER_ID);
    verify(chatRepository).deleteMessagePhysical(MSG_ID);
  }

  @Test
  @DisplayName("deleteMessage: 異常系 (他人のメッセージ)")
  void testDeleteMessageForbidden() {
    doReturn("other_user").when(chatRepository).findSenderIdByMessageId(MSG_ID);
    SecurityException e = assertThrows(SecurityException.class, () -> target.deleteMessage(MSG_ID, SENDER_ID));
    assertTrue(e.getMessage().contains("権限エラー"));
  }

  @Test
  @DisplayName("editMessage: 正常系")
  void testEditMessageSuccess() {
    doReturn(SENDER_ID).when(chatRepository).findSenderIdByMessageId(MSG_ID);
    doReturn(1).when(chatRepository).updateMessageBody(MSG_ID, "New Body");

    target.editMessage(MSG_ID, "New Body", SENDER_ID);

    verify(chatRepository).updateMessageBody(MSG_ID, "New Body");
  }

  @Test
  @DisplayName("editMessage: 異常系 (更新対象なし)")
  void testEditMessageNotFound() {
    doReturn(SENDER_ID).when(chatRepository).findSenderIdByMessageId(MSG_ID);
    doReturn(0).when(chatRepository).updateMessageBody(MSG_ID, "New Body");

    assertThrows(IllegalArgumentException.class, () -> target.editMessage(MSG_ID, "New Body", SENDER_ID));
  }

  @Test
  @DisplayName("editMessage: 異常系 (本文空文字)")
  void testEditMessageEmptyBody() {
    assertThrows(IllegalArgumentException.class, () -> target.editMessage(MSG_ID, "", SENDER_ID));
  }

  @Test
  @DisplayName("editMessage: 異常系 (本文null)")
  void testEditMessageNullBody() {
    assertThrows(IllegalArgumentException.class, () -> target.editMessage(MSG_ID, null, SENDER_ID));
  }

  @Test
  @DisplayName("editMessage: 異常系 (権限なし)")
  void testEditMessageForbidden() {
    doReturn("other_user").when(chatRepository).findSenderIdByMessageId(MSG_ID);
    SecurityException e = assertThrows(SecurityException.class, () -> target.editMessage(MSG_ID, "Body", SENDER_ID));
    assertTrue(e.getMessage().contains("権限エラー"));
  }
}
