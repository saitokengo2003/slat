package com.sysdev.slat.chat;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param; // ✅ 追加
import java.util.List;
import java.util.UUID;

/**
 * group_s テーブルのデータアクセスインターフェースです。
 */
public interface GroupRepository extends CrudRepository<Group, UUID> {

  /**
   * ログインユーザーがメンバーとして参加しているグループのリストを取得します。
   * group_members テーブルを参照します。
   */
  @Query("""
      SELECT
          g.id, g.name, g.created_at, g.updated_at
      FROM
          group_s g
      INNER JOIN
          group_members gm ON g.id = gm.group_id
      WHERE
          gm.user_id = :userId
      """)
  List<Group> findJoinedGroupsByUserId(@Param("userId") String userId); // ✅ ここを修正
}
