package com.sysdev.slat.reactions;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReactionService {

  private final ReactionRepository reactionRepository;

  public ReactionService(ReactionRepository reactionRepository) {
    this.reactionRepository = reactionRepository;
  }

  /**
   * リアクションを登録・または削除する（トグル機能）
   *
   * @return 登録された場合は true、削除された場合は false
   */
  @Transactional
  public boolean toggleReaction(UUID messageId, String userId, String emoji) {
    Optional<ReactionEntity> existingReaction = reactionRepository
        .findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);

    if (existingReaction.isPresent()) {
      // 既に存在する場合 -> 削除
      reactionRepository.delete(existingReaction.get());
      return false; // 削除された
    } else {
      // 存在しない場合 -> 新規登録
      ReactionEntity newReaction = new ReactionEntity();
      newReaction.setMessageId(messageId);
      newReaction.setUserId(userId);
      newReaction.setEmoji(emoji);
      reactionRepository.save(newReaction);
      return true; // 登録された
    }
  }

  /**
   * メッセージIDのリストから、対応するリアクションをまとめて取得する
   */
  public List<ReactionEntity> getReactionsByMessageIds(List<UUID> messageIds) {
    return reactionRepository.findByMessageIds(messageIds);
  }
}
