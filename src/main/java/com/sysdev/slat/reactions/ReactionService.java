package com.sysdev.slat.reactions;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.sysdev.slat.chat.ChatRepository; // ⭐ ChatRepositoryをインポート

@Service
public class ReactionService {

  private final ReactionRepository groupReactionRepository; // グループ用 (reactionsテーブル)
  private final DmReactionRepository dmReactionRepository; // ⭐ NEW: DM用 (dm_reactionsテーブル)
  private final ChatRepository chatRepository; // ⭐ NEW: メッセージIDの判別用

  // ⭐ MODIFIED: コンストラクタにDM用リポジトリとChatRepositoryを追加
  public ReactionService(ReactionRepository groupReactionRepository, DmReactionRepository dmReactionRepository,
      ChatRepository chatRepository) {
    this.groupReactionRepository = groupReactionRepository;
    this.dmReactionRepository = dmReactionRepository;
    this.chatRepository = chatRepository;
  }

  /**
   * リアクションを登録・または削除する（トグル機能）
   * * ⭐ MODIFIED: messageId が DM か Group かを判定し、処理を振り分ける
   */
  @Transactional
  public boolean toggleReaction(UUID messageId, String userId, String emoji) {

    // 1. Group Message (messagesテーブル) にIDが存在するか確認
    if (chatRepository.isGroupMessage(messageId)) {
      return toggleGroupReaction(messageId, userId, emoji);
    }
    // 2. DM Message (dmmessageテーブル) にIDが存在するか確認
    else if (chatRepository.isDmMessage(messageId)) {
      return toggleDmReaction(messageId, userId, emoji);
    }

    // どちらのIDにも該当しない場合
    return false;
  }

  // --- プライベートヘルパーメソッド ---

  private boolean toggleGroupReaction(UUID messageId, String userId, String emoji) {
    Optional<ReactionEntity> existingReaction = groupReactionRepository
        .findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);

    if (existingReaction.isPresent()) {
      groupReactionRepository.delete(existingReaction.get());
      return false; // 削除
    } else {
      ReactionEntity newReaction = new ReactionEntity();
      newReaction.setMessageId(messageId);
      newReaction.setUserId(userId);
      newReaction.setEmoji(emoji);
      groupReactionRepository.save(newReaction);
      return true; // 登録
    }
  }

  // ⭐ NEW: DMリアクションの登録・削除を処理するメソッド
  private boolean toggleDmReaction(UUID messageId, String userId, String emoji) {
    // DmReactionEntity/DmReactionRepository を使用してトグル
    Optional<DmReactionEntity> existingReaction = dmReactionRepository
        .findByDmMessageIdAndUserIdAndEmoji(messageId, userId, emoji);

    if (existingReaction.isPresent()) {
      dmReactionRepository.delete(existingReaction.get());
      return false; // 削除
    } else {
      DmReactionEntity newReaction = new DmReactionEntity();
      newReaction.setDmMessageId(messageId); // ⭐ DM用のFKを使用
      newReaction.setUserId(userId);
      newReaction.setEmoji(emoji);
      dmReactionRepository.save(newReaction);
      return true; // 登録
    }
  }

  /**
   * メッセージIDのリストから、対応するリアクションをまとめて取得する (グループチャット用)
   */
  public List<ReactionEntity> getReactionsByMessageIds(List<UUID> messageIds) {
    return groupReactionRepository.findByMessageIds(messageIds);
  }

  /**
   * ⭐ NEW: DMメッセージIDのリストから、対応するリアクションをまとめて取得する (DMチャット用)
   */
  public List<ReactionEntity> getDmReactionsByMessageIds(List<UUID> messageIds) {
    List<DmReactionEntity> dmReactions = dmReactionRepository.findByDmMessageIds(messageIds);

    // 戻り値の型を ReactionEntity に合わせるため、マッピングを行う
    return dmReactions.stream()
        .map(dm -> {
          // ReactionEntity はグループチャットのDTOだが、クライアントの期待する形式に合わせるため流用
          ReactionEntity re = new ReactionEntity();
          re.setMessageId(dm.getDmMessageId()); // MessageIdとしてDMのIDをセット
          re.setUserId(dm.getUserId());
          re.setEmoji(dm.getEmoji());
          return re;
        })
        .collect(Collectors.toList());
  }
}
