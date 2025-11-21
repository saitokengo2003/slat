package com.sysdev.slat.reactions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sysdev.slat.chat.ChatRepository;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

  @Mock
  private ReactionRepository groupReactionRepository;

  @Mock
  private DmReactionRepository dmReactionRepository;

  @Mock
  private ChatRepository chatRepository;

  @InjectMocks
  private ReactionService target;

  // テスト用定数
  private final UUID MSG_ID = UUID.randomUUID();
  private final String USER_ID = "user-001";
  private final String EMOJI = "👍";

  // --- toggleReaction (グループチャット) ---

  @Test
  @DisplayName("toggleReaction(Group): 追加 (リアクションが存在しない場合)")
  void testToggleReactionGroupAdd() {
    // 1. Ready
    // グループメッセージであると判定させる
    doReturn(true).when(chatRepository).isGroupMessage(MSG_ID);
    // 既存リアクションはない
    doReturn(Optional.empty()).when(groupReactionRepository).findByMessageIdAndUserIdAndEmoji(MSG_ID, USER_ID, EMOJI);

    // 2. Do
    boolean result = target.toggleReaction(MSG_ID, USER_ID, EMOJI);

    // 3. Assert
    assertTrue(result, "追加時はtrueが返るべき");
    // saveが呼ばれたか検証
    verify(groupReactionRepository).save(any(ReactionEntity.class));
    // deleteは呼ばれていないこと
    verify(groupReactionRepository, never()).delete(any());
  }

  @Test
  @DisplayName("toggleReaction(Group): 削除 (リアクションが既に存在する場合)")
  void testToggleReactionGroupRemove() {
    // 1. Ready
    doReturn(true).when(chatRepository).isGroupMessage(MSG_ID);

    ReactionEntity existing = new ReactionEntity();
    doReturn(Optional.of(existing)).when(groupReactionRepository).findByMessageIdAndUserIdAndEmoji(MSG_ID, USER_ID,
        EMOJI);

    // 2. Do
    boolean result = target.toggleReaction(MSG_ID, USER_ID, EMOJI);

    // 3. Assert
    assertFalse(result, "削除時はfalseが返るべき");
    // deleteが呼ばれたか検証
    verify(groupReactionRepository).delete(existing);
    // saveは呼ばれていないこと
    verify(groupReactionRepository, never()).save(any());
  }

  // --- toggleReaction (DM) ---

  @Test
  @DisplayName("toggleReaction(DM): 追加")
  void testToggleReactionDmAdd() {
    // 1. Ready
    // Groupではない
    doReturn(false).when(chatRepository).isGroupMessage(MSG_ID);
    // DMである
    doReturn(true).when(chatRepository).isDmMessage(MSG_ID);

    // 既存なし
    doReturn(Optional.empty()).when(dmReactionRepository).findByDmMessageIdAndUserIdAndEmoji(MSG_ID, USER_ID, EMOJI);

    // 2. Do
    boolean result = target.toggleReaction(MSG_ID, USER_ID, EMOJI);

    // 3. Assert
    assertTrue(result);
    verify(dmReactionRepository).save(any(DmReactionEntity.class));
  }

  @Test
  @DisplayName("toggleReaction(DM): 削除")
  void testToggleReactionDmRemove() {
    // 1. Ready
    doReturn(false).when(chatRepository).isGroupMessage(MSG_ID);
    doReturn(true).when(chatRepository).isDmMessage(MSG_ID);

    DmReactionEntity existing = new DmReactionEntity();
    doReturn(Optional.of(existing)).when(dmReactionRepository).findByDmMessageIdAndUserIdAndEmoji(MSG_ID, USER_ID,
        EMOJI);

    // 2. Do
    boolean result = target.toggleReaction(MSG_ID, USER_ID, EMOJI);

    // 3. Assert
    assertFalse(result);
    verify(dmReactionRepository).delete(existing);
  }

  @Test
  @DisplayName("toggleReaction: 異常系 (メッセージが存在しない)")
  void testToggleReactionNotFound() {
    // 1. Ready
    // どちらでもない
    doReturn(false).when(chatRepository).isGroupMessage(MSG_ID);
    doReturn(false).when(chatRepository).isDmMessage(MSG_ID);

    // 2. Do & 3. Assert
    assertThrows(IllegalArgumentException.class, () -> target.toggleReaction(MSG_ID, USER_ID, EMOJI));
  }

  // --- 取得メソッド群 ---

  @Test
  @DisplayName("getReactionsByMessageIds: グループリアクション一覧取得")
  void testGetReactionsByMessageIds() {
    // 1. Ready
    List<UUID> ids = List.of(MSG_ID);
    List<ReactionEntity> expected = List.of(new ReactionEntity());
    doReturn(expected).when(groupReactionRepository).findByMessageIds(ids);

    // 2. Do
    List<ReactionEntity> result = target.getReactionsByMessageIds(ids);

    // 3. Assert
    assertSame(expected, result);
  }

  @Test
  @DisplayName("getDmReactionsByMessageIds: DMリアクション一覧取得")
  void testGetDmReactionsByMessageIds() {
    // 1. Ready
    List<UUID> ids = List.of(MSG_ID);
    List<DmReactionEntity> expected = List.of(new DmReactionEntity());
    doReturn(expected).when(dmReactionRepository).findByDmMessageIds(ids);

    // 2. Do
    List<DmReactionEntity> result = target.getDmReactionsByMessageIds(ids);

    // 3. Assert
    assertSame(expected, result);
  }

  @Test
  @DisplayName("getGroupReactionsBefore: 期限内リアクション取得(Group)")
  void testGetGroupReactionsBefore() {
    // 1. Ready
    OffsetDateTime time = OffsetDateTime.now();
    List<ReactionEntity> expected = List.of(new ReactionEntity());
    doReturn(expected).when(groupReactionRepository).findByMessageIdAndCreatedAtBefore(MSG_ID, time);

    // 2. Do
    List<ReactionEntity> result = target.getGroupReactionsBefore(MSG_ID, time);

    // 3. Assert
    assertSame(expected, result);
  }

  @Test
  @DisplayName("getDmReactionsBefore: 期限内リアクション取得(DM)")
  void testGetDmReactionsBefore() {
    // 1. Ready
    OffsetDateTime time = OffsetDateTime.now();
    List<DmReactionEntity> expected = List.of(new DmReactionEntity());
    doReturn(expected).when(dmReactionRepository).findByDmMessageIdAndCreatedAtBefore(MSG_ID, time);

    // 2. Do
    List<DmReactionEntity> result = target.getDmReactionsBefore(MSG_ID, time);

    // 3. Assert
    assertSame(expected, result);
  }
}
