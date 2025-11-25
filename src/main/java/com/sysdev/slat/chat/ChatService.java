package com.sysdev.slat.chat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;
import com.sysdev.slat.reactions.ReactionService;
import com.sysdev.slat.reactions.ReactionEntity;
import com.sysdev.slat.reactions.DmReactionEntity;
import com.sysdev.slat.user.UserService;
import java.util.Map;

@Service
public class ChatService {

  private final ChatRepository chatRepository;
  private final ReactionService reactionService;
  private final UserService userService;

  public ChatService(ChatRepository chatRepository, ReactionService reactionService, UserService userService) {
    this.chatRepository = chatRepository;
    this.reactionService = reactionService;
    this.userService = userService;
  }

  // 期限付きメッセージの送信権限をチェックするヘルパーメソッド
  private boolean isAllowedToSendExpiredMessage(String userId) {
    String role = userService.getUserRole(userId);
    return "admin".equals(role) || "teacher".equals(role);
  }

  /**
   * 期限付きメッセージに対し、期限内にリアクションしなかった生徒の名前リストを取得する
   */
  private List<String> getNonReactingStudentNames(UUID messageId, OffsetDateTime expirationTime) {

    // 【修正】到達不能コード(expirationTime == null)を削除
    if (expirationTime.isAfter(OffsetDateTime.now())) {
      return List.of();
    }

    List<String> allParticipantIds = List.of();
    List<String> reactedUserIds = List.of();

    // 2. メッセージタイプを判別し、参加者IDとリアクションしたユーザーIDを取得
    if (chatRepository.isGroupMessage(messageId)) {
      UUID groupId = chatRepository.getGroupIdByMessageId(messageId)
          .orElseThrow(() -> new IllegalArgumentException("グループIDが見つかりません。"));

      allParticipantIds = chatRepository.getGroupMembers(groupId);

      List<ReactionEntity> reactions = reactionService.getGroupReactionsBefore(messageId, expirationTime);
      reactedUserIds = reactions.stream()
          .map(ReactionEntity::getUserId)
          .collect(Collectors.toList());

    } else if (chatRepository.isDmMessage(messageId)) {
      allParticipantIds = chatRepository.getDmParticipants(messageId);

      List<DmReactionEntity> dmReactions = reactionService.getDmReactionsBefore(messageId, expirationTime);
      reactedUserIds = dmReactions.stream()
          .map(DmReactionEntity::getUserId)
          .collect(Collectors.toList());

    } else {
      return List.of();
    }

    // 3. 全ての参加者から、リアクションしたユーザー、および**生徒ではないユーザー**を除外する
    List<String> allStudentIds = userService.getAllStudentIds();

    List<String> targetStudentIds = allParticipantIds.stream()
        .filter(allStudentIds::contains)
        .collect(Collectors.toList());

    List<String> reactedStudentIds = reactedUserIds.stream()
        .filter(allStudentIds::contains)
        .collect(Collectors.toList());

    // 4. リアクションしなかった生徒のIDリストを抽出
    List<String> nonReactingStudentIds = targetStudentIds.stream()
        .filter(studentId -> !reactedStudentIds.contains(studentId))
        .collect(Collectors.toList());

    // 5. IDから表示名に変換
    return nonReactingStudentIds.stream()
        .map(userService::getDisplayName)
        .collect(Collectors.toList());
  }

  public List<MessageHistoryDto> getDmHistory(String userId1, String userId2) {
    List<MessageHistoryDto> history = chatRepository.findDmHistory(userId1, userId2);

    List<UUID> messageIds = history.stream()
        .map(MessageHistoryDto::getMessageId)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toList());

    if (messageIds.isEmpty()) {
      return history;
    }

    List<DmReactionEntity> allDmReactions = reactionService.getDmReactionsByMessageIds(messageIds);
    Map<UUID, List<ReactionEntity>> reactionsMap = allDmReactions.stream()
        .map(dm -> {
          ReactionEntity re = new ReactionEntity();
          re.setMessageId(dm.getDmMessageId());
          re.setUserId(dm.getUserId());
          re.setEmoji(dm.getEmoji());
          re.setCreatedAt(dm.getCreatedAt());
          return re;
        })
        .collect(Collectors.groupingBy(ReactionEntity::getMessageId));

    history.forEach(dto -> {
      List<ReactionEntity> reactions = reactionsMap.getOrDefault(dto.getMessageId(), List.of());
      dto.setReactions(reactions);

      if (dto.getExpirationTime() != null) {
        List<String> nonReactingNames = getNonReactingStudentNames(dto.getMessageId(), dto.getExpirationTime());
        dto.setNonReactingStudentNames(nonReactingNames);
      } else {
        dto.setNonReactingStudentNames(List.of());
      }
    });

    return history;
  }

  public List<MessageHistoryDto> getGroupHistory(String groupId) {
    List<MessageHistoryDto> history = chatRepository.findGroupHistory(groupId);
    List<UUID> messageIds = history.stream()
        .map(MessageHistoryDto::getMessageId)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toList());

    if (messageIds.isEmpty()) {
      return history;
    }

    List<ReactionEntity> allReactions = reactionService.getReactionsByMessageIds(messageIds);
    Map<UUID, List<ReactionEntity>> reactionsMap = allReactions.stream()
        .collect(Collectors.groupingBy(ReactionEntity::getMessageId));

    history.forEach(dto -> {
      List<ReactionEntity> reactions = reactionsMap.getOrDefault(dto.getMessageId(), List.of());
      dto.setReactions(reactions);

      if (dto.getExpirationTime() != null) {
        List<String> nonReactingNames = getNonReactingStudentNames(dto.getMessageId(), dto.getExpirationTime());
        dto.setNonReactingStudentNames(nonReactingNames);
      } else {
        dto.setNonReactingStudentNames(List.of());
      }
    });

    return history;
  }

  @Transactional
  public void saveChatMessage(ChatRequest request) {
    if (request.getSenderId() == null || request.getBody() == null || request.getBody().trim().isEmpty()) {
      throw new IllegalArgumentException("Sender ID and message body are required.");
    }

    if (request.getExpirationTime() != null) {
      if (!isAllowedToSendExpiredMessage(request.getSenderId())) {
        throw new SecurityException("権限エラー: 期限付きメッセージは管理者または教師のみ送信可能です。");
      }
    }

    if (request.getGroupId() != null && !request.getGroupId().isEmpty()) {
      chatRepository.saveGroupMessage(request);
    } else if (request.getRecipientId() != null && !request.getRecipientId().isEmpty()) {
      chatRepository.saveDmMessage(request);
    } else {
      throw new IllegalArgumentException("Recipient ID or Group ID is required.");
    }
  }

  @Transactional
  public void deleteMessage(UUID messageId, String currentUserId) {
    String senderId = chatRepository.findSenderIdByMessageId(messageId);
    if (!currentUserId.equals(senderId)) {
      throw new SecurityException("権限エラー: 他のユーザーのメッセージは削除できません。");
    }
    chatRepository.deleteMessagePhysical(messageId);
  }

  @Transactional
  public void editMessage(UUID messageId, String newBody, String currentUserId) {
    if (newBody == null || newBody.trim().isEmpty()) {
      throw new IllegalArgumentException("メッセージ本文は必須です。");
    }
    String senderId = chatRepository.findSenderIdByMessageId(messageId);
    if (!currentUserId.equals(senderId)) {
      throw new SecurityException("権限エラー: 他のユーザーのメッセージは編集できません。");
    }
    int updatedRows = chatRepository.updateMessageBody(messageId, newBody);
    if (updatedRows == 0) {
      throw new IllegalArgumentException("メッセージIDが見つかりません。");
    }
  }
}
