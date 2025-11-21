package com.sysdev.slat.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class ChatRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private ChatRepository target;

  // テスト用定数
  private final String SENDER_ID = "sender123";
  private final String RECIPIENT_ID = "recipient456";
  private final String GROUP_ID_STR = "550e8400-e29b-41d4-a716-446655440000";
  private final String BODY = "Hello World";
  private final OffsetDateTime EXPIRATION = OffsetDateTime.now().plusDays(1);

  // -------------------------------------------------------
  // saveDmMessage (DM保存) のテスト
  // -------------------------------------------------------

  @Test
  @DisplayName("saveDmMessage: 期限(expirationTime)がある場合")
  void testSaveDmMessage_WithExpiration() {
    // 1. Ready
    ChatRequest request = new ChatRequest();
    request.setSenderId(SENDER_ID);
    request.setRecipientId(RECIPIENT_ID);
    request.setBody(BODY);
    request.setExpirationTime(EXPIRATION);

    // 2. Do
    target.saveDmMessage(request);

    // 3. Assert
    // updateメソッドが適切な引数で呼ばれたか検証
    verify(jdbcTemplate, times(1)).update(
        contains("INSERT INTO dmmessage"), // SQLにINSERTが含まれているか
        any(UUID.class), // ID (ランダム生成なので any)
        eq(SENDER_ID), // sender_id
        eq(RECIPIENT_ID), // recipient_id
        eq(BODY), // body
        eq(EXPIRATION) // expiration_time
    );
  }

  @Test
  @DisplayName("saveDmMessage: 期限(expirationTime)がない(null)場合")
  void testSaveDmMessage_NoExpiration() {
    // 1. Ready
    ChatRequest request = new ChatRequest();
    request.setSenderId(SENDER_ID);
    request.setRecipientId(RECIPIENT_ID);
    request.setBody(BODY);
    request.setExpirationTime(null);

    // 2. Do
    target.saveDmMessage(request);

    // 3. Assert
    verify(jdbcTemplate, times(1)).update(
        contains("INSERT INTO dmmessage"),
        any(UUID.class),
        eq(SENDER_ID),
        eq(RECIPIENT_ID),
        eq(BODY)
    // expiration_time 引数がないSQLが呼ばれるはず
    );
  }

  // -------------------------------------------------------
  // saveGroupMessage (グループ保存) のテスト
  // -------------------------------------------------------

  @Test
  @DisplayName("saveGroupMessage: 期限がある場合")
  void testSaveGroupMessage_WithExpiration() {
    // 1. Ready
    ChatRequest request = new ChatRequest();
    request.setGroupId(GROUP_ID_STR);
    request.setSenderId(SENDER_ID);
    request.setBody(BODY);
    request.setExpirationTime(EXPIRATION);

    // 2. Do
    target.saveGroupMessage(request);

    // 3. Assert
    verify(jdbcTemplate, times(1)).update(
        contains("INSERT INTO messages"),
        eq(UUID.fromString(GROUP_ID_STR)), // UUID変換後の値
        eq(SENDER_ID),
        eq(BODY),
        eq(EXPIRATION));
  }

  @Test
  @DisplayName("saveGroupMessage: 期限がない場合")
  void testSaveGroupMessage_NoExpiration() {
    // 1. Ready
    ChatRequest request = new ChatRequest();
    request.setGroupId(GROUP_ID_STR);
    request.setSenderId(SENDER_ID);
    request.setBody(BODY);
    request.setExpirationTime(null);

    // 2. Do
    target.saveGroupMessage(request);

    // 3. Assert
    verify(jdbcTemplate, times(1)).update(
        contains("INSERT INTO messages"),
        eq(UUID.fromString(GROUP_ID_STR)),
        eq(SENDER_ID),
        eq(BODY));
  }

  // -------------------------------------------------------
  // findDmHistory (DM履歴取得) のテスト
  // -------------------------------------------------------

  @Test
  @DisplayName("findDmHistory: SQLとパラメータが正しく渡されているか")
  @SuppressWarnings("unchecked")
  void testFindDmHistory() {
    // 1. Ready
    // queryメソッドが呼ばれたときに空リストを返すようにモック化
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any(), any()))
        .thenReturn(new ArrayList<>());

    // 2. Do
    List<MessageHistoryDto> result = target.findDmHistory(SENDER_ID, RECIPIENT_ID);

    // 3. Assert
    assertNotNull(result);

    // 呼び出し引数の検証
    verify(jdbcTemplate).query(
        contains("SELECT id, sender_id"), // SQLの一部確認
        any(RowMapper.class),
        eq(SENDER_ID), // パラメータ順序1
        eq(RECIPIENT_ID), // パラメータ順序2
        eq(RECIPIENT_ID), // パラメータ順序3
        eq(SENDER_ID) // パラメータ順序4
    );
  }

  // -------------------------------------------------------
  // findGroupHistory (グループ履歴取得) のテスト
  // -------------------------------------------------------

  @Test
  @DisplayName("findGroupHistory: SQLとパラメータが正しく渡されているか")
  @SuppressWarnings("unchecked")
  void testFindGroupHistory() {
    // 1. Ready
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object.class)))
        .thenReturn(new ArrayList<>());

    // 2. Do
    List<MessageHistoryDto> result = target.findGroupHistory(GROUP_ID_STR);

    // 3. Assert
    assertNotNull(result);

    verify(jdbcTemplate).query(
        contains("SELECT id, sender_id"),
        any(RowMapper.class),
        eq(UUID.fromString(GROUP_ID_STR)));
  }

  // -------------------------------------------------------
  // isGroupMessage (存在確認) のテスト
  // -------------------------------------------------------

  @Test
  @DisplayName("isGroupMessage: 存在する場合(count > 0) trueを返す")
  void testIsGroupMessage_True() {
    // 1. Ready
    UUID msgId = UUID.randomUUID();
    // queryForObject が 1 を返すように設定
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(msgId)))
        .thenReturn(1);

    // 2. Do
    boolean result = target.isGroupMessage(msgId);

    // 3. Assert
    assertTrue(result);
    verify(jdbcTemplate).queryForObject(contains("FROM messages"), eq(Integer.class), eq(msgId));
  }

  @Test
  @DisplayName("isGroupMessage: 存在しない場合(count == 0) falseを返す")
  void testIsGroupMessage_False() {
    // 1. Ready
    UUID msgId = UUID.randomUUID();
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(msgId)))
        .thenReturn(0);

    // 2. Do
    boolean result = target.isGroupMessage(msgId);

    // 3. Assert
    assertFalse(result);
  }

  @Test
  @DisplayName("isGroupMessage: nullが返ってきた場合 falseを返す")
  void testIsGroupMessage_Null() {
    // 1. Ready
    UUID msgId = UUID.randomUUID();
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(msgId)))
        .thenReturn(null);

    // 2. Do
    boolean result = target.isGroupMessage(msgId);

    // 3. Assert
    assertFalse(result);
  }

  // -------------------------------------------------------
  // isDmMessage (存在確認) のテスト
  // -------------------------------------------------------

  @Test
  @DisplayName("isDmMessage: 存在する場合 trueを返す")
  void testIsDmMessage_True() {
    // 1. Ready
    UUID msgId = UUID.randomUUID();
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(msgId)))
        .thenReturn(1);

    // 2. Do
    boolean result = target.isDmMessage(msgId);

    // 3. Assert
    assertTrue(result);
    verify(jdbcTemplate).queryForObject(contains("FROM dmmessage"), eq(Integer.class), eq(msgId));
  }

  @Test
  @DisplayName("isDmMessage: 存在しない場合 falseを返す")
  void testIsDmMessage_False() {
    // 1. Ready
    UUID msgId = UUID.randomUUID();
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(msgId)))
        .thenReturn(0);

    // 2. Do
    boolean result = target.isDmMessage(msgId);

    // 3. Assert
    assertFalse(result);
  }
}
