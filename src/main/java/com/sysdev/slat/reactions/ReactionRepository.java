package com.sysdev.slat.reactions;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jdbc.repository.query.Query;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface ReactionRepository extends CrudRepository<ReactionEntity, UUID> {

  Optional<ReactionEntity> findByMessageIdAndUserIdAndEmoji(UUID messageId, String userId, String emoji);

  @Query("SELECT * FROM reactions WHERE message_id IN (:messageIds)")
  List<ReactionEntity> findByMessageIds(@Param("messageIds") List<UUID> messageIds);

  /**
   * 特定のメッセージに、期限内にリアクションしたユーザーのリストを取得する
   * created_at が expirationTime 以前 (<=) のリアクションを取得
   */
  @Query("""
      SELECT * FROM reactions
      WHERE message_id = :messageId AND created_at <= :expirationTime
      """)
  List<ReactionEntity> findByMessageIdAndCreatedAtBefore(
      @Param("messageId") UUID messageId,
      @Param("expirationTime") OffsetDateTime expirationTime);
}
