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

  // テスト用定数
  private final UUID MSG_ID = UUID.randomUUID();
  private final String SENDER_ID = "teacher1";
  private final String RECIPIENT_ID = "student1";
  private final String GROUP_ID = "group-A";

  // --- saveChatMessage のテスト ---

  @Test
  @DisplayName("saveChatMessage: 正常系 (DM, 期限なし)")
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
  @DisplayName("saveChatMessage: 正常系 (Group, 期限なし)")
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
  @DisplayName("saveChatMessage: 正常系 (期限付き, 教師権限あり)")
  void testSaveChatMessageExpiredAllowed() {
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
  @DisplayName("saveChatMessage: 異常系 (期限付き, 生徒権限なし)")
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
  @DisplayName("saveChatMessage: 異常系 (必須項目不足 - Body)")
  void testSaveChatMessageInvalidBody() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(SENDER_ID);
    // Body なし
    assertThrows(IllegalArgumentException.class, () -> target.saveChatMessage(req));
  }

  @Test
  @DisplayName("saveChatMessage: 異常系 (必須項目不足 - Recipient/Group)")
  void testSaveChatMessageNoRecipient() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(SENDER_ID);
    req.setBody("Body");
    // 宛先なし
    assertThrows(IllegalArgumentException.class, () -> target.saveChatMessage(req));
  }

  // --- getDmHistory のテスト ---

  @Test
  @DisplayName("getDmHistory: 正常系 (期限付きメッセージの未読判定含む)")
  void testGetDmHistoryWithExpiration() {
    // 1. メッセージ履歴のモック
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setBody("Test Msg");
    msg.setExpirationTime(OffsetDateTime.now().plusDays(1)); // 未来の日付

    doReturn(List.of(msg)).when(chatRepository).findDmHistory(SENDER_ID, RECIPIENT_ID);

    // 2. リアクション履歴のモック（空リスト）
    doReturn(Collections.emptyList()).when(reactionService).getDmReactionsByMessageIds(anyList());

    // 3. 未読判定ロジック (isDmMessage -> true)
    // ※ここで lenient() を使うと、もし呼ばれなくてもエラーになりませんが、今回は確実に呼ばれる前提で設定
    doReturn(false).when(chatRepository).isGroupMessage(MSG_ID);
    doReturn(true).when(chatRepository).isDmMessage(MSG_ID);

    // DM参加者取得
    doReturn(List.of(SENDER_ID, RECIPIENT_ID)).when(chatRepository).getDmParticipants(MSG_ID);

    // 期限内リアクション取得（空＝誰もリアクションしていない）
    doReturn(Collections.emptyList()).when(reactionService).getDmReactionsBefore(eq(MSG_ID), any(OffsetDateTime.class));

    // 生徒IDリスト取得
    doReturn(List.of(RECIPIENT_ID, "other_student")).when(userService).getAllStudentIds();

    // 表示名取得
    doReturn("生徒 一郎").when(userService).getDisplayName(RECIPIENT_ID);

    // --- 実行 ---
    List<MessageHistoryDto> result = target.getDmHistory(SENDER_ID, RECIPIENT_ID);

    // --- 検証 ---
    assertEquals(1, result.size());
    List<String> nonReactingNames = result.get(0).getNonReactingStudentNames();
    assertEquals(1, nonReactingNames.size());
    assertEquals("生徒 一郎", nonReactingNames.get(0));
  }

  @Test
  @DisplayName("getDmHistory: 期限なしの場合は未読チェックしない")
  void testGetDmHistoryNoExpiration() {
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setExpirationTime(null); // 期限なし

    doReturn(List.of(msg)).when(chatRepository).findDmHistory(SENDER_ID, RECIPIENT_ID);
    doReturn(Collections.emptyList()).when(reactionService).getDmReactionsByMessageIds(anyList());

    List<MessageHistoryDto> result = target.getDmHistory(SENDER_ID, RECIPIENT_ID);

    assertTrue(result.get(0).getNonReactingStudentNames().isEmpty());
    // 期限がないため、未読チェック用メソッドは呼ばれないはず
    verify(chatRepository, never()).isDmMessage(any());
  }

  @Test
  @DisplayName("getDmHistory: 履歴が空の場合")
  void testGetDmHistoryEmpty() {
    doReturn(Collections.emptyList()).when(chatRepository).findDmHistory(SENDER_ID, RECIPIENT_ID);
    List<MessageHistoryDto> result = target.getDmHistory(SENDER_ID, RECIPIENT_ID);
    assertTrue(result.isEmpty());
  }

  // --- getGroupHistory のテスト ---

  @Test
  @DisplayName("getGroupHistory: 正常系 (期限付き、未読判定あり)")
  void testGetGroupHistoryWithExpiration() {
    // 1. メッセージ履歴
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setExpirationTime(OffsetDateTime.now().plusDays(1));

    doReturn(List.of(msg)).when(chatRepository).findGroupHistory(GROUP_ID);
    doReturn(Collections.emptyList()).when(reactionService).getReactionsByMessageIds(anyList());

    // 2. 未読判定ロジック (isGroupMessage -> true)
    doReturn(true).when(chatRepository).isGroupMessage(MSG_ID);

    // グループID取得
    doReturn(Optional.of(UUID.randomUUID())).when(chatRepository).getGroupIdByMessageId(MSG_ID);

    // グループメンバー取得
    doReturn(List.of("student1", "student2")).when(chatRepository).getGroupMembers(any(UUID.class));

    // 期限内リアクション取得（空）
    doReturn(Collections.emptyList()).when(reactionService).getGroupReactionsBefore(eq(MSG_ID),
        any(OffsetDateTime.class));

    // 生徒IDリスト
    doReturn(List.of("student1", "student2")).when(userService).getAllStudentIds();

    // 表示名取得（2人分）
    doReturn("生徒1").when(userService).getDisplayName("student1");
    doReturn("生徒2").when(userService).getDisplayName("student2");

    // --- 実行 ---
    List<MessageHistoryDto> result = target.getGroupHistory(GROUP_ID);

    // --- 検証 ---
    assertEquals(2, result.get(0).getNonReactingStudentNames().size());
  }

  @Test
  @DisplayName("getGroupHistory: 期限なし")
  void testGetGroupHistoryNoExpiration() {
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setExpirationTime(null);

    doReturn(List.of(msg)).when(chatRepository).findGroupHistory(GROUP_ID);
    doReturn(Collections.emptyList()).when(reactionService).getReactionsByMessageIds(anyList());

    List<MessageHistoryDto> result = target.getGroupHistory(GROUP_ID);

    assertTrue(result.get(0).getNonReactingStudentNames().isEmpty());
    verify(chatRepository, never()).isGroupMessage(any());
  }

  @Test
  @DisplayName("getGroupHistory: 履歴空")
  void testGetGroupHistoryEmpty() {
    doReturn(Collections.emptyList()).when(chatRepository).findGroupHistory(GROUP_ID);
    List<MessageHistoryDto> result = target.getGroupHistory(GROUP_ID);
    assertTrue(result.isEmpty());
  }

  // --- deleteMessage のテスト ---

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

  // --- editMessage のテスト ---

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
  @DisplayName("editMessage: 異常系 (本文空)")
  void testEditMessageEmptyBody() {
    assertThrows(IllegalArgumentException.class, () -> target.editMessage(MSG_ID, "", SENDER_ID));
  }

  // --- getNonReactingStudentNames の特殊ケース（privateメソッドだがpublic経由でテスト） ---

  @Test
  @DisplayName("getNonReactingStudentNames: どちらでもないメッセージIDの場合（不正データ）")
  void testGetNonReactingStudentNamesInvalidType() {
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setExpirationTime(OffsetDateTime.now().plusDays(1)); // 期限あり

    doReturn(List.of(msg)).when(chatRepository).findDmHistory(SENDER_ID, RECIPIENT_ID);
    doReturn(Collections.emptyList()).when(reactionService).getDmReactionsByMessageIds(anyList());

    // isGroupMessage = false, isDmMessage = false を返すように設定
    doReturn(false).when(chatRepository).isGroupMessage(MSG_ID);
    doReturn(false).when(chatRepository).isDmMessage(MSG_ID);

    List<MessageHistoryDto> result = target.getDmHistory(SENDER_ID, RECIPIENT_ID);

    // 未読リストは空になるはず
    assertTrue(result.get(0).getNonReactingStudentNames().isEmpty());
  }

  @Test
  @DisplayName("getNonReactingStudentNames: グループIDが見つからない場合（例外）")
  void testGetNonReactingStudentNamesGroupNotFound() {
    MessageHistoryDto msg = new MessageHistoryDto();
    msg.setMessageId(MSG_ID);
    msg.setExpirationTime(OffsetDateTime.now().plusDays(1));

    doReturn(List.of(msg)).when(chatRepository).findGroupHistory(GROUP_ID);
    doReturn(Collections.emptyList()).when(reactionService).getReactionsByMessageIds(anyList());

    // isGroupMessage = true なのに getGroupIdByMessageId が empty を返すケース
    doReturn(true).when(chatRepository).isGroupMessage(MSG_ID);
    doReturn(Optional.empty()).when(chatRepository).getGroupIdByMessageId(MSG_ID);

    assertThrows(IllegalArgumentException.class, () -> target.getGroupHistory(GROUP_ID));
  }
}
