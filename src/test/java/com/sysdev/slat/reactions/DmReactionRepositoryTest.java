package com.sysdev.slat.reactions;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // H2 Databaseを使用
class DmReactionRepositoryTest {

  @Autowired
  private DmReactionRepository target;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    // テストごとにデータをクリアする（@DataJdbcTestは自動ロールバックするが、念のため）
    target.deleteAll();
  }

  @Test
  @DisplayName("findByDmMessageIdAndUserIdAndEmoji: 条件に一致するリアクションが取得できる")
  void testFindByDmMessageIdAndUserIdAndEmoji() {
    // 1. Ready
    UUID messageId = UUID.randomUUID();
    String userId = "user-001";
    String emoji = "👍";

    DmReactionEntity entity = new DmReactionEntity();
    entity.setId(UUID.randomUUID());
    entity.setDmMessageId(messageId);
    entity.setUserId(userId);
    entity.setEmoji(emoji);
    entity.setCreatedAt(OffsetDateTime.now());
    target.save(entity);

    // 2. Do
    Optional<DmReactionEntity> result = target.findByDmMessageIdAndUserIdAndEmoji(messageId, userId, emoji);

    // 3. Assert
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(entity.getId());
  }

  @Test
  @DisplayName("findByDmMessageIdAndUserIdAndEmoji: 条件が異なると取得できない")
  void testFindByDmMessageIdAndUserIdAndEmoji_NotFound() {
    // 1. Ready
    UUID messageId = UUID.randomUUID();
    String userId = "user-001";

    DmReactionEntity entity = new DmReactionEntity();
    entity.setId(UUID.randomUUID());
    entity.setDmMessageId(messageId);
    entity.setUserId(userId);
    entity.setEmoji("👍"); // 保存するのは "👍"
    target.save(entity);

    // 2. Do
    // 違う絵文字 "👎" で検索
    Optional<DmReactionEntity> result = target.findByDmMessageIdAndUserIdAndEmoji(messageId, userId, "👎");

    // 3. Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findByDmMessageIds: 指定リストに含まれるメッセージIDのリアクションを取得 (IN句)")
  void testFindByDmMessageIds() {
    // 1. Ready
    UUID msgId1 = UUID.randomUUID();
    UUID msgId2 = UUID.randomUUID();
    UUID msgId3 = UUID.randomUUID(); // 検索対象外

    // メッセージ1 に対するリアクション
    createAndSaveReaction(msgId1, "u1", "A");
    // メッセージ2 に対するリアクション
    createAndSaveReaction(msgId2, "u2", "B");
    // メッセージ3 に対するリアクション (これは取得されないはず)
    createAndSaveReaction(msgId3, "u3", "C");

    List<UUID> searchIds = List.of(msgId1, msgId2);

    // 2. Do
    List<DmReactionEntity> results = target.findByDmMessageIds(searchIds);

    // 3. Assert
    assertThat(results).hasSize(2);
    assertThat(results.stream().map(DmReactionEntity::getDmMessageId))
        .containsExactlyInAnyOrder(msgId1, msgId2);
  }

  @Test
  @DisplayName("findByDmMessageIdAndCreatedAtBefore: 期限内(Before)のリアクションのみ取得")
  void testFindByDmMessageIdAndCreatedAtBefore() {
    // 1. Ready
    UUID messageId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    // 基準となる期限（現在）
    OffsetDateTime expirationTime = now;

    // A: 期限より前 (10分前) -> 取得されるべき
    DmReactionEntity pastReaction = new DmReactionEntity();
    pastReaction.setId(UUID.randomUUID());
    pastReaction.setDmMessageId(messageId);
    pastReaction.setUserId("past_user");
    pastReaction.setCreatedAt(now.minusMinutes(10));
    target.save(pastReaction);

    // B: 期限より後 (10分後) -> 取得されないべき
    DmReactionEntity futureReaction = new DmReactionEntity();
    futureReaction.setId(UUID.randomUUID());
    futureReaction.setDmMessageId(messageId);
    futureReaction.setUserId("future_user");
    futureReaction.setCreatedAt(now.plusMinutes(10));
    target.save(futureReaction);

    // 2. Do
    List<DmReactionEntity> results = target.findByDmMessageIdAndCreatedAtBefore(messageId, expirationTime);

    // 3. Assert
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getUserId()).isEqualTo("past_user");
  }

  @Test
  @DisplayName("findByDmMessageIdAndCreatedAtBefore: 境界値テスト (ピッタリ同時の場合)")
  void testFindByDmMessageIdAndCreatedAtBefore_Boundary() {
    // 1. Ready
    UUID messageId = UUID.randomUUID();
    OffsetDateTime justNow = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    // 期限と全く同じ日時のデータを作成
    DmReactionEntity justTimeReaction = new DmReactionEntity();
    justTimeReaction.setId(UUID.randomUUID());
    justTimeReaction.setDmMessageId(messageId);
    justTimeReaction.setUserId("boundary_user");
    justTimeReaction.setCreatedAt(justNow);
    target.save(justTimeReaction);

    // 2. Do
    // SQLが <= (以下) なので含まれるはず
    List<DmReactionEntity> results = target.findByDmMessageIdAndCreatedAtBefore(messageId, justNow);

    // 3. Assert
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getUserId()).isEqualTo("boundary_user");
  }

  // --- Helper Method ---
  private void createAndSaveReaction(UUID msgId, String userId, String emoji) {
    DmReactionEntity e = new DmReactionEntity();
    e.setId(UUID.randomUUID());
    e.setDmMessageId(msgId);
    e.setUserId(userId);
    e.setEmoji(emoji);
    e.setCreatedAt(OffsetDateTime.now());
    target.save(e);
  }
}
