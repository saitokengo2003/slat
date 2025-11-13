package com.sysdev.slat.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime; // ✅ OffsetDateTimeをインポート

@Repository
public class ChatRepository {

  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public ChatRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  // -------------------------------------------------------------------
  // ✅ DM履歴の取得
  // -------------------------------------------------------------------

  /**
   * DMメッセージ履歴を取得します。
   */
  public List<MessageHistoryDto> findDmHistory(String userId1, String userId2) {
    // SQL: (A->B) または (B->A) のメッセージを取得し、日付でソート
    String sql = """
            SELECT sender_id, body, created_at
            FROM dmmessage
            WHERE
                (sender_id = ? AND recipient_id = ?) OR
                (sender_id = ? AND recipient_id = ?)
            ORDER BY created_at ASC
        """;

    // パラメータ: ユーザーIDの組み合わせを2パターン渡す (A, B, B, A)
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          MessageHistoryDto dto = new MessageHistoryDto();
          dto.setSenderId(rs.getString("sender_id"));
          dto.setBody(rs.getString("body"));
          // ⭐ 修正: OffsetDateTimeにマッピング
          dto.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
          return dto;
        },
        userId1, userId2, // 1st pair: A -> B
        userId2, userId1 // 2nd pair: B -> A
    );
  }

  // -------------------------------------------------------------------
  // ✅ メッセージの保存 (省略しないバージョン)
  // -------------------------------------------------------------------

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
   * グループIDが group_s テーブルに存在するか確認します。
   */
  public boolean groupExists(String groupId) {
    // ... (省略)
    String sql = "SELECT COUNT(*) FROM group_s WHERE id = ?";
    try {
      Integer count = jdbcTemplate.queryForObject(sql, Integer.class, UUID.fromString(groupId));
      return count != null && count > 0;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * group_s テーブルに新しいグループを登録します。
   */
  public void insertGroup(String groupId, String name, boolean isDm) {
    // ... (省略)
    String sql = "INSERT INTO group_s (id, name, is_dm) VALUES (?, ?, ?)";
    jdbcTemplate.update(sql,
        UUID.fromString(groupId),
        name,
        isDm);
  }

  /**
   * グループメッセージを messages テーブルに保存します。
   */
  public void saveGroupMessage(ChatRequest request) {
    // ... (省略)
    String sql = "INSERT INTO messages (group_id, sender_id, body) VALUES (?, ?, ?)";
    jdbcTemplate.update(sql,
        UUID.fromString(request.getGroupId()),
        request.getSenderId(),
        request.getBody());
  }
}
