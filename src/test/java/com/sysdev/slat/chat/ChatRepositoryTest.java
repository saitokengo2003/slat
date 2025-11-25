package com.sysdev.slat.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
  private final UUID MSG_ID = UUID.randomUUID();
  private final String USER1 = "user1";
  private final String USER2 = "user2";
  private final String GROUP_ID_STR = UUID.randomUUID().toString();

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

  @Test
  @DisplayName("saveGroupMessage: 不正なグループIDの場合は例外発生")
  void testSaveGroupMessageInvalidId() {
    ChatRequest req = new ChatRequest();
    req.setGroupId("invalid-uuid-string");
    req.setSenderId(USER1);
    req.setBody("Body");

    assertThrows(IllegalArgumentException.class, () -> target.saveGroupMessage(req));
  }

  @Test
  @DisplayName("findDmHistory: 正常系 (SQL呼び出し確認)")
  void testFindDmHistory() {
    MessageHistoryDto dto = new MessageHistoryDto();
    doReturn(List.of(dto)).when(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(USER1), eq(USER2), eq(USER2),
        eq(USER1));

    List<MessageHistoryDto> result = target.findDmHistory(USER1, USER2);
    assertEquals(1, result.size());
  }

  @Test
  @DisplayName("findDmHistory: RowMapperのマッピングロジック確認")
  void testFindDmHistory_RowMapper() throws SQLException {
    // 1. ResultSetのモック作成
    ResultSet rs = mock(ResultSet.class);
    UUID uuid = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();

    doReturn(uuid).when(rs).getObject("id", UUID.class);
    doReturn("sender_A").when(rs).getString("sender_id");
    doReturn("Body_A").when(rs).getString("body");
    doReturn(now).when(rs).getObject("created_at", OffsetDateTime.class);
    doReturn(now).when(rs).getObject("expiration_time", OffsetDateTime.class);

    // 2. ArgumentCaptorでRowMapperを捕獲する準備
    ArgumentCaptor<RowMapper<MessageHistoryDto>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);

    // 3. メソッド実行 (戻り値は重要ではないので空リストでOK)
    doReturn(List.of()).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(), any(), any(), any());
    target.findDmHistory(USER1, USER2);

    // 4. 捕獲
    verify(jdbcTemplate).query(anyString(), mapperCaptor.capture(), eq(USER1), eq(USER2), eq(USER2), eq(USER1));
    RowMapper<MessageHistoryDto> mapper = mapperCaptor.getValue();

    // 5. マッピング実行
    MessageHistoryDto dto = mapper.mapRow(rs, 1);

    // 6. 検証
    assertEquals(uuid, dto.getMessageId());
    assertEquals("sender_A", dto.getSenderId());
    assertEquals("Body_A", dto.getBody());
    assertEquals(now, dto.getCreatedAt());
    assertEquals(now, dto.getExpirationTime());
  }

  @Test
  @DisplayName("findGroupHistory: 正常系 (SQL呼び出し確認)")
  void testFindGroupHistory() {
    MessageHistoryDto dto = new MessageHistoryDto();
    doReturn(List.of(dto)).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(UUID.class));

    List<MessageHistoryDto> result = target.findGroupHistory(GROUP_ID_STR);
    assertEquals(1, result.size());
  }

  @Test
  @DisplayName("findGroupHistory: RowMapperのマッピングロジック確認")
  void testFindGroupHistory_RowMapper() throws SQLException {
    // 1. ResultSetのモック
    ResultSet rs = mock(ResultSet.class);
    UUID uuid = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();

    doReturn(uuid).when(rs).getObject("id", UUID.class);
    doReturn("sender_G").when(rs).getString("sender_id");
    doReturn("Body_G").when(rs).getString("body");
    doReturn(now).when(rs).getObject("created_at", OffsetDateTime.class);
    doReturn(null).when(rs).getObject("expiration_time", OffsetDateTime.class);

    // 2. Captor
    ArgumentCaptor<RowMapper<MessageHistoryDto>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);

    // 3. 実行
    doReturn(List.of()).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(UUID.class));
    target.findGroupHistory(GROUP_ID_STR);

    // 4. 捕獲
    verify(jdbcTemplate).query(anyString(), mapperCaptor.capture(), any(UUID.class));
    RowMapper<MessageHistoryDto> mapper = mapperCaptor.getValue();

    // 5. マッピング
    MessageHistoryDto dto = mapper.mapRow(rs, 1);

    // 6. 検証
    assertEquals(uuid, dto.getMessageId());
    assertEquals("sender_G", dto.getSenderId());
    assertEquals("Body_G", dto.getBody());
    assertEquals(now, dto.getCreatedAt());
    assertNull(dto.getExpirationTime());
  }

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

  @Test
  @DisplayName("deleteMessagePhysical: 正常系")
  void testDeleteMessagePhysical() {
    target.deleteMessagePhysical(MSG_ID);
    verify(jdbcTemplate).update(contains("DELETE FROM messages"), eq(MSG_ID));
    verify(jdbcTemplate).update(contains("DELETE FROM dmmessage"), eq(MSG_ID));
  }

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

  @Test
  @DisplayName("getDmParticipants: 正常系 (SQL呼び出し確認)")
  void testGetDmParticipants() {
    List<String> expected = List.of(USER1, USER2);
    doReturn(expected).when(jdbcTemplate).queryForObject(contains("SELECT sender_id"), any(RowMapper.class),
        eq(MSG_ID));

    List<String> result = target.getDmParticipants(MSG_ID);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("getDmParticipants: RowMapperのマッピングロジック確認")
  void testGetDmParticipants_RowMapper() throws SQLException {
    // 1. ResultSetモック
    ResultSet rs = mock(ResultSet.class);
    doReturn("sender_X").when(rs).getString("sender_id");
    doReturn("recipient_Y").when(rs).getString("recipient_id");

    // 2. Captor
    ArgumentCaptor<RowMapper<List<String>>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);

    // 3. 実行
    doReturn(List.of()).when(jdbcTemplate).queryForObject(contains("SELECT sender_id"), any(RowMapper.class),
        eq(MSG_ID));
    target.getDmParticipants(MSG_ID);

    // 4. 捕獲
    verify(jdbcTemplate).queryForObject(contains("SELECT sender_id"), mapperCaptor.capture(), eq(MSG_ID));
    RowMapper<List<String>> mapper = mapperCaptor.getValue();

    // 5. マッピング実行
    List<String> result = mapper.mapRow(rs, 1);

    // 6. 検証
    assertEquals(2, result.size());
    assertEquals("sender_X", result.get(0));
    assertEquals("recipient_Y", result.get(1));
  }

  @Test
  @DisplayName("getDmParticipants: データなし")
  void testGetDmParticipantsEmpty() {
    doThrow(new EmptyResultDataAccessException(1)).when(jdbcTemplate).queryForObject(contains("SELECT sender_id"),
        any(RowMapper.class), eq(MSG_ID));
    List<String> result = target.getDmParticipants(MSG_ID);
    assertTrue(result.isEmpty());
  }

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

  @Test
  @DisplayName("isGroupMessage: null (件数が取得できない場合)")
  void testIsGroupMessageNull() {
    doReturn(null).when(jdbcTemplate).queryForObject(contains("SELECT COUNT(*) FROM messages"), eq(Integer.class),
        eq(MSG_ID));
    assertFalse(target.isGroupMessage(MSG_ID));
  }

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

  @Test
  @DisplayName("isDmMessage: null (件数が取得できない場合)")
  void testIsDmMessageNull() {
    doReturn(null).when(jdbcTemplate).queryForObject(contains("SELECT COUNT(*) FROM dmmessage"), eq(Integer.class),
        eq(MSG_ID));
    assertFalse(target.isDmMessage(MSG_ID));
  }

  @Test
  void testGroupExists() {
    assertFalse(target.groupExists("any"));
  }

  @Test
  void testInsertGroup() {
    assertDoesNotThrow(() -> target.insertGroup("id", "name", false));
  }
}
