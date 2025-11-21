package com.sysdev.slat.reactions;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import com.sysdev.slat.chat.ChatRepository;

@Service
public class ReactionService {

  private final ReactionRepository groupReactionRepository;
  private final DmReactionRepository dmReactionRepository;
  private final ChatRepository chatRepository;

  public ReactionService(ReactionRepository groupReactionRepository, DmReactionRepository dmReactionRepository,
      ChatRepository chatRepository) {
    this.groupReactionRepository = groupReactionRepository;
    this.dmReactionRepository = dmReactionRepository;
    this.chatRepository = chatRepository;
  }

  // --- リアクション操作 ---

  @Transactional
  public boolean toggleReaction(UUID messageId, String userId, String emoji) {
    if (chatRepository.isGroupMessage(messageId)) {
      Optional<ReactionEntity> existingReaction = groupReactionRepository.findByMessageIdAndUserIdAndEmoji(messageId,
          userId, emoji);

      if (existingReaction.isPresent()) {
        groupReactionRepository.delete(existingReaction.get());
        return false; // 削除
      } else {
        ReactionEntity newReaction = new ReactionEntity();
        newReaction.setMessageId(messageId);
        newReaction.setUserId(userId);
        newReaction.setEmoji(emoji);
        groupReactionRepository.save(newReaction);
        return true; // 追加
      }
    } else if (chatRepository.isDmMessage(messageId)) {
      Optional<DmReactionEntity> existingReaction = dmReactionRepository.findByDmMessageIdAndUserIdAndEmoji(messageId,
          userId, emoji);

      if (existingReaction.isPresent()) {
        dmReactionRepository.delete(existingReaction.get());
        return false; // 削除
      } else {
        DmReactionEntity newReaction = new DmReactionEntity();
        newReaction.setDmMessageId(messageId);
        newReaction.setUserId(userId);
        newReaction.setEmoji(emoji);
        dmReactionRepository.save(newReaction);
        return true; // 追加
      }
    }
    throw new IllegalArgumentException("メッセージIDが見つかりません。");
  }

  // --- リアクション取得 (履歴統合用) ---

  public List<ReactionEntity> getReactionsByMessageIds(List<UUID> messageIds) {
    return groupReactionRepository.findByMessageIds(messageIds);
  }

  public List<DmReactionEntity> getDmReactionsByMessageIds(List<UUID> dmMessageIds) {
    return dmReactionRepository.findByDmMessageIds(dmMessageIds);
  }

  /**
   * 特定のメッセージに、期限 (expirationTime) までに付けられたリアクションを取得する (グループチャット用)
   */
  public List<ReactionEntity> getGroupReactionsBefore(UUID messageId, OffsetDateTime expirationTime) {
    return groupReactionRepository.findByMessageIdAndCreatedAtBefore(messageId, expirationTime);
  }

  /**
   * 特定のDMメッセージに、期限 (expirationTime) までに付けられたリアクションを取得する (DMチャット用)
   */
  public List<DmReactionEntity> getDmReactionsBefore(UUID dmMessageId, OffsetDateTime expirationTime) {
    return dmReactionRepository.findByDmMessageIdAndCreatedAtBefore(dmMessageId, expirationTime);
  }
}
