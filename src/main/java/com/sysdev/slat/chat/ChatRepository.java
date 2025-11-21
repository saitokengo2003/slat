package com.sysdev.slat.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.EmptyResultDataAccessException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public class ChatRepository {

  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public ChatRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * DMメッセージを dmmessage テーブルに保存します。（期限情報に対応）
   */
  public void saveDmMessage(ChatRequest request) {
    UUID messageId = UUID.randomUUID();

    if (request.getExpirationTime() != null) {
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

  /**
   * DMメッセージ履歴を取得します。
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
          dto.setExpirationTime(rs.getObject("expiration_time", OffsetDateTime.class));
          return dto;
        },
        userId1, userId2,
        userId2, userId1);
  }

  /**
   * グループメッセージ履歴を取得します。
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

  /**
   * メッセージIDから送信者IDを取得します。（権限チェック用）
   */
  public String findSenderIdByMessageId(UUID messageId) {
    // 1. Group Messageから試行
    String groupSql = "SELECT sender_id FROM messages WHERE id = ?";
    try {
      return jdbcTemplate.queryForObject(groupSql, String.class, messageId);
    } catch (EmptyResultDataAccessException e) {
      // 2. DM Messageから試行
      String dmSql = "SELECT sender_id FROM dmmessage WHERE id = ?";
      try {
        return jdbcTemplate.queryForObject(dmSql, String.class, messageId);
      } catch (EmptyResultDataAccessException dmE) {
        // どちらにも見つからない場合
        throw new IllegalArgumentException("メッセージIDが見つかりません。");
      }
    }
  }

  /**
   * メッセージを物理的に削除します (DELETE文)
   */
  public void deleteMessagePhysical(UUID messageId) {
    // Group Messageの削除
    String groupSql = "DELETE FROM messages WHERE id = ?";
    jdbcTemplate.update(groupSql, messageId);

    // DM Messageの削除 (存在しない場合は 0 件更新で終了)
    String dmSql = "DELETE FROM dmmessage WHERE id = ?";
    jdbcTemplate.update(dmSql, messageId);
  }

  /**
   * メッセージIDをキーに、メッセージ本文を更新します。
   * (DM, グループメッセージのどちらにも対応します)
   *
   * @return 更新されたレコード数
   */
  public int updateMessageBody(UUID messageId, String newBody) {
    // 1. Group Messageの更新を試行
    String groupSql = "UPDATE messages SET body = ? WHERE id = ?";
    int updatedRows = jdbcTemplate.update(groupSql, newBody, messageId);

    if (updatedRows > 0) {
      return updatedRows;
    }

    // 2. DM Messageの更新を試行
    String dmSql = "UPDATE dmmessage SET body = ? WHERE id = ?";
    updatedRows = jdbcTemplate.update(dmSql, newBody, messageId);

    return updatedRows;
  }

  /**
   * DMメッセージの送信者と受信者のIDを取得する
   */
  public List<String> getDmParticipants(UUID messageId) {
    String sql = "SELECT sender_id, recipient_id FROM dmmessage WHERE id = ?";

    try {
      return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
        String senderId = rs.getString("sender_id");
        String recipientId = rs.getString("recipient_id");
        return List.of(senderId, recipientId);
      }, messageId);
    } catch (EmptyResultDataAccessException e) {
      return List.of();
    }
  }

  /**
   * グループメッセージのグループIDを取得する
   */
  public Optional<UUID> getGroupIdByMessageId(UUID messageId) {
    String sql = "SELECT group_id FROM messages WHERE id = ?";
    try {
      return Optional.ofNullable(jdbcTemplate.queryForObject(sql, UUID.class, messageId));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  /**
   * グループメンバーのユーザーIDリストを取得する
   */
  public List<String> getGroupMembers(UUID groupId) {
    String sql = "SELECT user_id FROM group_members WHERE group_id = ?";
    return jdbcTemplate.queryForList(sql, String.class, groupId);
  }

  public boolean isGroupMessage(UUID messageId) {
    String sql = "SELECT COUNT(*) FROM messages WHERE id = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, messageId);
    return count != null && count > 0;
  }

  public boolean isDmMessage(UUID messageId) {
    String sql = "SELECT COUNT(*) FROM dmmessage WHERE id = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, messageId);
    return count != null && count > 0;
  }

  public boolean groupExists(String groupId) {
    /* ... */ return false;
  }

  public void insertGroup(String groupId, String name, boolean isDm) {
    /* ... */ }
}
