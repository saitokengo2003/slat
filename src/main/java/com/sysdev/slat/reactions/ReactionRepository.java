package com.sysdev.slat.reactions;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jdbc.repository.query.Query;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface ReactionRepository extends CrudRepository<ReactionEntity, UUID> {

  /**
   * 特定のメッセージに、特定のユーザーが、同じ絵文字を付けているかを確認する
   */
  Optional<ReactionEntity> findByMessageIdAndUserIdAndEmoji(UUID messageId, String userId, String emoji); // ←
                                                                                                          // 本文なし、セミコロン

  /**
   * 複数のメッセージIDに対するリアクションをまとめて取得する
   */
  @Query("SELECT * FROM reactions WHERE message_id IN (:messageIds)")
  List<ReactionEntity> findByMessageIds(@Param("messageIds") List<UUID> messageIds); // ← 本文なし、セミコロン

  // このインターフェースのすべてのメソッドは、本文を持っていません。
}
