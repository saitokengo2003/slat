package com.sysdev.slat.reactions;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jdbc.repository.query.Query;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface DmReactionRepository extends CrudRepository<DmReactionEntity, UUID> {

  Optional<DmReactionEntity> findByDmMessageIdAndUserIdAndEmoji(UUID dmMessageId, String userId, String emoji);

  @Query("SELECT * FROM dm_reactions WHERE dm_message_id IN (:dmMessageIds)")
  List<DmReactionEntity> findByDmMessageIds(@Param("dmMessageIds") List<UUID> dmMessageIds);

  /**
   * 特定のDMメッセージに、期限内にリアクションしたユーザーのリストを取得する
   * created_at が expirationTime 以前 (<=) のリアクションを取得
   */
  @Query("""
      SELECT * FROM dm_reactions
      WHERE dm_message_id = :dmMessageId AND created_at <= :expirationTime
      """)
  List<DmReactionEntity> findByDmMessageIdAndCreatedAtBefore(
      @Param("dmMessageId") UUID dmMessageId,
      @Param("expirationTime") OffsetDateTime expirationTime);
}
