package com.sysdev.slat.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class ChatRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private ChatRepository target;

  // テスト用定数
  private final UUID MSG_ID = UUID.randomUUID();
  private final String USER1 = "user1";
  private final String USER2 = "user2";
  private final String GROUP_ID_STR = UUID.randomUUID().toString();

  // --- saveDmMessage ---

  @Test
  @DisplayName("saveDmMessage: 期限なしの場合")
  void testSaveDmMessageNoExpiration() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(USER1);
    req.setRecipientId(USER2);
    req.setBody("Hello");
    req.setExpirationTime(null);

    target.saveDmMessage(req);

    verify(jdbcTemplate).update(contains("INSERT INTO dmmessage"),
        any(UUID.class), eq(USER1), eq(USER2), eq("Hello"));
  }

  @Test
  @DisplayName("saveDmMessage: 期限ありの場合")
  void testSaveDmMessageWithExpiration() {
    ChatRequest req = new ChatRequest();
    req.setSenderId(USER1);
    req.setRecipientId(USER2);
    req.setBody("Hello Expire");
    req.setExpirationTime(OffsetDateTime.now());

    target.saveDmMessage(req);

    verify(jdbcTemplate).update(contains("INSERT INTO dmmessage"),
        any(UUID.class), eq(USER1), eq(USER2), eq("Hello Expire"), any(OffsetDateTime.class));
  }

  // --- saveGroupMessage ---

  @Test
  @DisplayName("saveGroupMessage: 期限なしの場合")
  void testSaveGroupMessageNoExpiration() {
    ChatRequest req = new ChatRequest();
    req.setGroupId(GROUP_ID_STR);
    req.setSenderId(USER1);
    req.setBody("Group Hello");

    target.saveGroupMessage(req);

    verify(jdbcTemplate).update(contains("INSERT INTO messages"),
        any(UUID.class), eq(USER1), eq("Group Hello"));
  }

  @Test
  @DisplayName("saveGroupMessage: 期限ありの場合")
  void testSaveGroupMessageWithExpiration() {
    ChatRequest req = new ChatRequest();
    req.setGroupId(GROUP_ID_STR);
    req.setSenderId(USER1);
    req.setBody("Group Expire");
    req.setExpirationTime(OffsetDateTime.now());

    target.saveGroupMessage(req);

    verify(jdbcTemplate).update(contains("INSERT INTO messages"),
        any(UUID.class), eq(USER1), eq("Group Expire"), any(OffsetDateTime.class));
  }

  // --- findDmHistory ---

  @Test
  @DisplayName("findDmHistory: 正常系")
  void testFindDmHistory() {
    MessageHistoryDto dto = new MessageHistoryDto();
    dto.setBody("History Body");

    doReturn(List.of(dto)).when(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(USER1), eq(USER2), eq(USER2),
        eq(USER1));

    List<MessageHistoryDto> result = target.findDmHistory(USER1, USER2);

    assertEquals(1, result.size());
    assertEquals("History Body", result.get(0).getBody());
  }

  // --- findGroupHistory ---

  @Test
  @DisplayName("findGroupHistory: 正常系")
  void testFindGroupHistory() {
    MessageHistoryDto dto = new MessageHistoryDto();
    dto.setBody("Group History Body");

    doReturn(List.of(dto)).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(UUID.class));

    List<MessageHistoryDto> result = target.findGroupHistory(GROUP_ID_STR);

    assertEquals(1, result.size());
    assertEquals("Group History Body", result.get(0).getBody());
  }

  // --- findSenderIdByMessageId ---

  @Test
  @DisplayName("findSenderIdByMessageId: Groupテーブルで見つかった場合")
  void testFindSenderIdByMessageIdFoundInGroup() {
    doReturn(USER1).when(jdbcTemplate).queryForObject(contains("SELECT sender_id FROM messages"), eq(String.class),
        eq(MSG_ID));

    String senderId = target.findSenderIdByMessageId(MSG_ID);

    assertEquals(USER1, senderId);
  }

  @Test
  @DisplayName("findSenderIdByMessageId: DMテーブルで見つかった場合")
  void testFindSenderIdByMessageIdFoundInDm() {
    doThrow(new EmptyResultDataAccessException(1)).when(jdbcTemplate)
        .queryForObject(contains("SELECT sender_id FROM messages"), eq(String.class), eq(MSG_ID));
    doReturn(USER2).when(jdbcTemplate).queryForObject(contains("SELECT sender_id FROM dmmessage"), eq(String.class),
        eq(MSG_ID));

    String senderId = target.findSenderIdByMessageId(MSG_ID);

    assertEquals(USER2, senderId);
  }

  @Test
  @DisplayName("findSenderIdByMessageId: どちらにもない場合")
  void testFindSenderIdByMessageIdNotFound() {
    doThrow(new EmptyResultDataAccessException(1)).when(jdbcTemplate).queryForObject(anyString(), eq(String.class),
        eq(MSG_ID));

    assertThrows(IllegalArgumentException.class, () -> target.findSenderIdByMessageId(MSG_ID));
  }

  // --- deleteMessagePhysical ---

  @Test
  @DisplayName("deleteMessagePhysical: 正常系")
  void testDeleteMessagePhysical() {
    target.deleteMessagePhysical(MSG_ID);

    verify(jdbcTemplate).update(contains("DELETE FROM messages"), eq(MSG_ID));
    verify(jdbcTemplate).update(contains("DELETE FROM dmmessage"), eq(MSG_ID));
  }

  // --- updateMessageBody ---

  @Test
  @DisplayName("updateMessageBody: Groupテーブルで更新成功")
  void testUpdateMessageBodyGroup() {
    doReturn(1).when(jdbcTemplate).update(contains("UPDATE messages"), eq("New Body"), eq(MSG_ID));

    int result = target.updateMessageBody(MSG_ID, "New Body");

    assertEquals(1, result);
    verify(jdbcTemplate, never()).update(contains("UPDATE dmmessage"), any(), any());
  }

  @Test
  @DisplayName("updateMessageBody: DMテーブルで更新成功")
  void testUpdateMessageBodyDm() {
    doReturn(0).when(jdbcTemplate).update(contains("UPDATE messages"), eq("New Body"), eq(MSG_ID));
    doReturn(1).when(jdbcTemplate).update(contains("UPDATE dmmessage"), eq("New Body"), eq(MSG_ID));

    int result = target.updateMessageBody(MSG_ID, "New Body");

    assertEquals(1, result);
  }

  // --- getDmParticipants ---

  @Test
  @DisplayName("getDmParticipants: 正常系")
  void testGetDmParticipants() {
    List<String> expected = List.of(USER1, USER2);
    doReturn(expected).when(jdbcTemplate).queryForObject(contains("SELECT sender_id"), any(RowMapper.class),
        eq(MSG_ID));

    List<String> result = target.getDmParticipants(MSG_ID);

    assertEquals(expected, result);
  }

  @Test
  @DisplayName("getDmParticipants: データなし")
  void testGetDmParticipantsEmpty() {
    doThrow(new EmptyResultDataAccessException(1)).when(jdbcTemplate).queryForObject(contains("SELECT sender_id"),
        any(RowMapper.class), eq(MSG_ID));

    List<String> result = target.getDmParticipants(MSG_ID);

    assertTrue(result.isEmpty());
  }

  // --- getGroupIdByMessageId ---

  @Test
  @DisplayName("getGroupIdByMessageId: 正常系")
  void testGetGroupIdByMessageId() {
    UUID groupId = UUID.randomUUID();
    doReturn(groupId).when(jdbcTemplate).queryForObject(contains("SELECT group_id"), eq(UUID.class), eq(MSG_ID));

    Optional<UUID> result = target.getGroupIdByMessageId(MSG_ID);

    assertTrue(result.isPresent());
    assertEquals(groupId, result.get());
  }

  @Test
  @DisplayName("getGroupIdByMessageId: データなし")
  void testGetGroupIdByMessageIdEmpty() {
    doThrow(new EmptyResultDataAccessException(1)).when(jdbcTemplate).queryForObject(contains("SELECT group_id"),
        eq(UUID.class), eq(MSG_ID));

    Optional<UUID> result = target.getGroupIdByMessageId(MSG_ID);

    assertTrue(result.isEmpty());
  }

  // --- getGroupMembers (追加) ---

  @Test
  @DisplayName("getGroupMembers: 正常系")
  void testGetGroupMembers() {
    List<String> members = List.of("u1", "u2");
    UUID groupId = UUID.randomUUID();

    doReturn(members).when(jdbcTemplate).queryForList(contains("SELECT user_id FROM group_members"), eq(String.class),
        eq(groupId));

    List<String> result = target.getGroupMembers(groupId);

    assertEquals(members, result);
  }

  // --- isGroupMessage ---

  @Test
  @DisplayName("isGroupMessage: true")
  void testIsGroupMessageTrue() {
    doReturn(1).when(jdbcTemplate).queryForObject(contains("SELECT COUNT(*) FROM messages"), eq(Integer.class),
        eq(MSG_ID));
    assertTrue(target.isGroupMessage(MSG_ID));
  }

  @Test
  @DisplayName("isGroupMessage: false")
  void testIsGroupMessageFalse() {
    doReturn(0).when(jdbcTemplate).queryForObject(contains("SELECT COUNT(*) FROM messages"), eq(Integer.class),
        eq(MSG_ID));
    assertFalse(target.isGroupMessage(MSG_ID));
  }

  // --- isDmMessage ---

  @Test
  @DisplayName("isDmMessage: true")
  void testIsDmMessageTrue() {
    doReturn(1).when(jdbcTemplate).queryForObject(contains("SELECT COUNT(*) FROM dmmessage"), eq(Integer.class),
        eq(MSG_ID));
    assertTrue(target.isDmMessage(MSG_ID));
  }

  @Test
  @DisplayName("isDmMessage: false")
  void testIsDmMessageFalse() {
    doReturn(0).when(jdbcTemplate).queryForObject(contains("SELECT COUNT(*) FROM dmmessage"), eq(Integer.class),
        eq(MSG_ID));
    assertFalse(target.isDmMessage(MSG_ID));
  }

  // --- groupExists / insertGroup (プレースホルダー実装のテスト) ---

  @Test
  void testGroupExists() {
    assertFalse(target.groupExists("any"));
  }

  @Test
  void testInsertGroup() {
    assertDoesNotThrow(() -> target.insertGroup("id", "name", false));
  }
}
