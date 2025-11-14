package com.sysdev.slat.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class ChatRepository {

  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public ChatRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  // --- 省略されていたコードはすべて復元されています ---

  /**
   * DMメッセージを dmmessage テーブルに保存します。
   */
  public void saveDmMessage(ChatRequest request) {
    String sql = "INSERT INTO dmmessage (sender_id, recipient_id, body) VALUES (?, ?, ?)";
    jdbcTemplate.update(sql,
        request.getSenderId(),
        request.getRecipientId(),
        request.getBody());
  }

  /**
   * グループメッセージを messages テーブルに保存します。
   */
  public void saveGroupMessage(ChatRequest request) {
    String sql = "INSERT INTO messages (group_id, sender_id, body) VALUES (?, ?, ?)";
    jdbcTemplate.update(sql,
        UUID.fromString(request.getGroupId()),
        request.getSenderId(),
        request.getBody());
  }

  /**
   * DMメッセージ履歴を取得します。
   */
  public List<MessageHistoryDto> findDmHistory(String userId1, String userId2) {
    String sql = """
            SELECT sender_id, body, created_at
            FROM dmmessage
            WHERE
                (sender_id = ? AND recipient_id = ?) OR
                (sender_id = ? AND recipient_id = ?)
            ORDER BY created_at ASC
        """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          MessageHistoryDto dto = new MessageHistoryDto();
          dto.setSenderId(rs.getString("sender_id"));
          dto.setBody(rs.getString("body"));
          dto.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
          return dto;
        },
        userId1, userId2,
        userId2, userId1);
  }

  /**
   * ✅ グループメッセージ履歴を取得します。 (新規追加)
   */
  public List<MessageHistoryDto> findGroupHistory(String groupId) {
    // SQL: 指定された group_id のメッセージを messages テーブルから取得し、日付でソート
    String sql = """
            SELECT sender_id, body, created_at
            FROM messages
            WHERE
                group_id = ?
            ORDER BY created_at ASC
        """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          MessageHistoryDto dto = new MessageHistoryDto();
          dto.setSenderId(rs.getString("sender_id"));
          dto.setBody(rs.getString("body"));
          dto.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
          return dto;
        },
        UUID.fromString(groupId) // UUIDに変換してパラメータとして渡す
    );
  }

  // ... (groupExists, insertGroup, etc. は省略) ...
  public boolean groupExists(String groupId) {
    /* ... */ return false;
  }

  public void insertGroup(String groupId, String name, boolean isDm) {
    /* ... */ }
}
