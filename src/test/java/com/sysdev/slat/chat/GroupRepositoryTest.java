package com.sysdev.slat.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // H2 Databaseを使用
class GroupRepositoryTest {

  @Autowired
  private GroupRepository target;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("findJoinedGroupsByUserId: 指定したユーザーが参加しているグループのみ取得できること")
  void testFindJoinedGroupsByUserId() {
    // 1. Ready (テストデータの準備)
    // GroupRepositoryは Group エンティティの管理用ですが、
    // group_members テーブルへの INSERT は Repository の範疇外（または自動マッピング外）の可能性があるため、
    // 確実性を期して JdbcTemplate で直接テストデータを投入します。

    // グループAを作成
    UUID groupA_Id = UUID.randomUUID();
    insertGroup(groupA_Id, "Group A");

    // グループBを作成
    UUID groupB_Id = UUID.randomUUID();
    insertGroup(groupB_Id, "Group B");

    // グループCを作成（誰も参加しない、または別の人が参加）
    UUID groupC_Id = UUID.randomUUID();
    insertGroup(groupC_Id, "Group C");

    // ユーザー "target_user" を グループA と グループB に参加させる
    insertMember(groupA_Id, "target_user");
    insertMember(groupB_Id, "target_user");

    // 別のユーザー "other_user" を グループC に参加させる
    insertMember(groupC_Id, "other_user");

    // 2. Do (実行)
    List<Group> results = target.findJoinedGroupsByUserId("target_user");

    // 3. Assert (検証)
    // 2件取得できるはず
    assertThat(results).hasSize(2);

    // 取得されたグループIDに A と B が含まれていること
    List<UUID> resultIds = results.stream().map(Group::getId).toList();
    assertThat(resultIds).containsExactlyInAnyOrder(groupA_Id, groupB_Id);

    // C は含まれていないこと
    assertThat(resultIds).doesNotContain(groupC_Id);
  }

  @Test
  @DisplayName("findJoinedGroupsByUserId: 参加していないユーザーの場合は空リスト")
  void testFindJoinedGroupsByUserId_NoGroups() {
    // 1. Ready
    UUID groupA_Id = UUID.randomUUID();
    insertGroup(groupA_Id, "Group A");
    insertMember(groupA_Id, "user1");

    // 2. Do
    // 全く関係ないユーザーで検索
    List<Group> results = target.findJoinedGroupsByUserId("lonely_user");

    // 3. Assert
    assertThat(results).isEmpty();
  }

  // --- ヘルパーメソッド (テストデータ投入用) ---

  private void insertGroup(UUID id, String name) {
    String sql = "INSERT INTO group_s (id, name, created_at, updated_at) VALUES (?, ?, ?, ?)";
    jdbcTemplate.update(sql, id, name, OffsetDateTime.now(), OffsetDateTime.now());
  }

  private void insertMember(UUID groupId, String userId) {
    // role_in_group は今回のクエリ条件に関係ないため適当な値('member')を入れます
    String sql = "INSERT INTO group_members (group_id, user_id, role_in_group) VALUES (?, ?, 'member')";
    jdbcTemplate.update(sql, groupId, userId);
  }
}
