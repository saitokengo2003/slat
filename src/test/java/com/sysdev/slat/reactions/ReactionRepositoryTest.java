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

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // H2 Databaseを使用
class ReactionRepositoryTest {

  @Autowired
  private ReactionRepository target;

  @BeforeEach
  void setUp() {
    // テスト間の干渉を防ぐためデータをクリア
    target.deleteAll();
  }

  @Test
  @DisplayName("findByMessageIdAndUserIdAndEmoji: 条件に一致するリアクションが取得できる")
  void testFindByMessageIdAndUserIdAndEmoji() {
    // 1. Ready
    UUID messageId = UUID.randomUUID();
    String userId = "user-001";
    String emoji = "🎉";

    ReactionEntity entity = createReaction(messageId, userId, emoji, OffsetDateTime.now());
    target.save(entity);

    // 2. Do
    Optional<ReactionEntity> result = target.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);

    // 3. Assert
    assertThat(result).isPresent();
    assertThat(result.get().getEmoji()).isEqualTo(emoji);
  }

  @Test
  @DisplayName("findByMessageIdAndUserIdAndEmoji: 存在しない条件ではEmptyが返る")
  void testFindByMessageIdAndUserIdAndEmoji_NotFound() {
    // 1. Ready
    UUID messageId = UUID.randomUUID();
    target.save(createReaction(messageId, "user-001", "🎉", OffsetDateTime.now()));

    // 2. Do
    // 違うユーザーIDで検索
    Optional<ReactionEntity> result = target.findByMessageIdAndUserIdAndEmoji(messageId, "other-user", "🎉");

    // 3. Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findByMessageIds: 指定リストに含まれるメッセージIDのリアクションを取得 (IN句)")
  void testFindByMessageIds() {
    // 1. Ready
    UUID msg1 = UUID.randomUUID();
    UUID msg2 = UUID.randomUUID();
    UUID msg3 = UUID.randomUUID(); // 対象外

    target.save(createReaction(msg1, "u1", "A", OffsetDateTime.now()));
    target.save(createReaction(msg2, "u2", "B", OffsetDateTime.now()));
    target.save(createReaction(msg3, "u3", "C", OffsetDateTime.now()));

    // 2. Do
    List<ReactionEntity> results = target.findByMessageIds(List.of(msg1, msg2));

    // 3. Assert
    assertThat(results).hasSize(2);
    assertThat(results.stream().map(ReactionEntity::getMessageId))
        .containsExactlyInAnyOrder(msg1, msg2);
  }

  @Test
  @DisplayName("findByMessageIdAndCreatedAtBefore: 期限内(Before)のリアクションのみ取得")
  void testFindByMessageIdAndCreatedAtBefore() {
    // 1. Ready
    UUID messageId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    OffsetDateTime expirationTime = now;

    // A: 期限より前 (1時間前) -> 取得されるべき
    ReactionEntity past = createReaction(messageId, "past_user", "A", now.minusHours(1));
    target.save(past);

    // B: 期限より後 (1時間後) -> 取得されないべき
    ReactionEntity future = createReaction(messageId, "future_user", "B", now.plusHours(1));
    target.save(future);

    // 2. Do
    List<ReactionEntity> results = target.findByMessageIdAndCreatedAtBefore(messageId, expirationTime);

    // 3. Assert
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getUserId()).isEqualTo("past_user");
  }

  @Test
  @DisplayName("findByMessageIdAndCreatedAtBefore: 境界値 (期限ぴったり) も含まれること")
  void testFindByMessageIdAndCreatedAtBefore_Boundary() {
    // 1. Ready
    UUID messageId = UUID.randomUUID();
    OffsetDateTime justNow = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    // 期限と同じ日時のデータ
    ReactionEntity boundary = createReaction(messageId, "boundary_user", "C", justNow);
    target.save(boundary);

    // 2. Do
    // SQL条件は <= なので含まれるはず
    List<ReactionEntity> results = target.findByMessageIdAndCreatedAtBefore(messageId, justNow);

    // 3. Assert
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getUserId()).isEqualTo("boundary_user");
  }

  // --- Helper Method ---
  private ReactionEntity createReaction(UUID msgId, String userId, String emoji, OffsetDateTime time) {
    ReactionEntity e = new ReactionEntity();
    e.setId(UUID.randomUUID());
    e.setMessageId(msgId);
    e.setUserId(userId);
    e.setEmoji(emoji);
    e.setCreatedAt(time);
    return e;
  }
}
