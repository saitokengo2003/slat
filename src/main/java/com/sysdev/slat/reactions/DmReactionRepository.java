// DmReactionRepository.java (新規作成)
package com.sysdev.slat.reactions;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jdbc.repository.query.Query;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

// ⭐ DmReactionEntity を扱うリポジトリ
public interface DmReactionRepository extends CrudRepository<DmReactionEntity, UUID> {

  /**
   * 特定のDMメッセージに、特定のユーザーが、同じ絵文字を付けているかを確認する
   */
  Optional<DmReactionEntity> findByDmMessageIdAndUserIdAndEmoji(UUID dmMessageId, String userId, String emoji);

  /**
   * 複数のDMメッセージIDに対するリアクションをまとめて取得する
   */
  @Query("SELECT * FROM dm_reactions WHERE dm_message_id IN (:messageIds)")
  List<DmReactionEntity> findByDmMessageIds(@Param("messageIds") List<UUID> messageIds);
}
