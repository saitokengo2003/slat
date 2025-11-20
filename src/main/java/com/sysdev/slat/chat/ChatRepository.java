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

  // --- saveDmMessage (ID生成と挿入を追加) ---

  /**
   * DMメッセージを dmmessage テーブルに保存します。
   */
  public void saveDmMessage(ChatRequest request) {
    // ⭐ MODIFIED: IDを生成して保存に追加 (DB側で自動生成されている場合は削除または修正が必要)
    UUID messageId = UUID.randomUUID();

    if (request.getExpirationTime() != null) {
      // ⭐ MODIFIED: expiration_timeの挿入に対応
      String sql = "INSERT INTO dmmessage (id, sender_id, recipient_id, body, expiration_time) VALUES (?, ?, ?, ?, ?)";
      jdbcTemplate.update(sql,
          messageId,
          request.getSenderId(),
          request.getRecipientId(),
          request.getBody(),
          request.getExpirationTime());
    } else {
      String sql = "INSERT INTO dmmessage (id, sender_id, recipient_id, body) VALUES (?, ?, ?, ?)";
      jdbcTemplate.update(sql,
          messageId,
          request.getSenderId(),
          request.getRecipientId(),
          request.getBody());
    }
  }

  /**
   * グループメッセージを messages テーブルに保存します。（期限情報に対応）
   */
  public void saveGroupMessage(ChatRequest request) {
    if (request.getExpirationTime() != null) {
      String sql = "INSERT INTO messages (group_id, sender_id, body, expiration_time) VALUES (?, ?, ?, ?)";
      jdbcTemplate.update(sql,
          UUID.fromString(request.getGroupId()),
          request.getSenderId(),
          request.getBody(),
          request.getExpirationTime());
    } else {
      String sql = "INSERT INTO messages (group_id, sender_id, body) VALUES (?, ?, ?)";
      jdbcTemplate.update(sql,
          UUID.fromString(request.getGroupId()),
          request.getSenderId(),
          request.getBody());
    }
  }

  // --- findDmHistory (IDとexpiration_timeの取得を追加) ---

  /**
   * DMメッセージ履歴を取得します。（IDとexpiration_timeを取得）
   */
  public List<MessageHistoryDto> findDmHistory(String userId1, String userId2) {
    String sql = """
            SELECT id, sender_id, body, created_at, expiration_time
            FROM dmmessage
            WHERE
                ((sender_id = ? AND recipient_id = ?) OR
                (sender_id = ? AND recipient_id = ?))
            ORDER BY created_at ASC
        """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          MessageHistoryDto dto = new MessageHistoryDto();
          dto.setMessageId(rs.getObject("id", UUID.class));
          dto.setSenderId(rs.getString("sender_id"));
          dto.setBody(rs.getString("body"));
          dto.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
          dto.setExpirationTime(rs.getObject("expiration_time", OffsetDateTime.class)); // 期限情報を設定
          return dto;
        },
        userId1, userId2,
        userId2, userId1);
  }

  /**
   * グループメッセージ履歴を取得します。 (IDとexpiration_timeを取得)
   */
  public List<MessageHistoryDto> findGroupHistory(String groupId) {
    String sql = """
            SELECT id, sender_id, body, created_at, expiration_time
            FROM messages
            WHERE
                group_id = ?
            ORDER BY created_at ASC
        """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          MessageHistoryDto dto = new MessageHistoryDto();
          dto.setMessageId(rs.getObject("id", UUID.class));
          dto.setSenderId(rs.getString("sender_id"));
          dto.setBody(rs.getString("body"));
          dto.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
          dto.setExpirationTime(rs.getObject("expiration_time", OffsetDateTime.class));
          return dto;
        },
        UUID.fromString(groupId));
  }

  // --- NEW: リアクション処理用ヘルパーメソッド ---

  /**
   * ⭐ NEW: 指定されたIDが messages テーブルに存在するか確認します。
   */
  public boolean isGroupMessage(UUID messageId) {
    String sql = "SELECT COUNT(*) FROM messages WHERE id = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, messageId);
    return count != null && count > 0;
  }

  /**
   * ⭐ NEW: 指定されたIDが dmmessage テーブルに存在するか確認します。
   */
  public boolean isDmMessage(UUID messageId) {
    String sql = "SELECT COUNT(*) FROM dmmessage WHERE id = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, messageId);
    return count != null && count > 0;
  }

  // ... (groupExists, insertGroup, etc. は省略) ...
  public boolean groupExists(String groupId) {
    /* ... */ return false;
  }

  public void insertGroup(String groupId, String name, boolean isDm) {
    /* ... */ }
}
