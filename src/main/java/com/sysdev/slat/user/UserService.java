package com.sysdev.slat.user;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UserService {

  @Autowired
  private UserRepository userRepository;

  /**
   * ユーザーID（username）とパスワードで認証を行い、成功時はUserDataを返す。
   */
  public UserData authenticate(String username, String password) {
    Optional<User> userOpt = userRepository.findByUsername(username);
    if (userOpt.isEmpty()) {
      return null;
    }
    User user = userOpt.get();
    if (!user.getPasswordHash().equals(password)) {
      return null;
    }
    UserData data = new UserData();
    data.setUserId(user.getUsername());
    data.setDisplayName(user.getDisplayName());
    data.setRoleCode(user.getRoleCode());
    data.setGrade(user.getGrade());
    data.setClassName(user.getClassName());
    data.setNumber(user.getNumber());
    return data;
  }

  /**
   * ログインユーザー以外の全ユーザーを取得し、UserDataリストとして返す。
   */
  public List<UserData> findAllOtherUsers(String currentUsername) {
    // 1. 修正: userRepository.findAll() が返す Iterable<User> を List<User> に変換
    List<User> allUsers = StreamSupport.stream(userRepository.findAll().spliterator(), false)
        .collect(Collectors.toList());
    // 2. ログインユーザーを除外し、UserDataに変換
    return allUsers.stream()
        .filter(user -> !user.getUsername().equals(currentUsername))
        .map(user -> {
          UserData data = new UserData();
          data.setUserId(user.getUsername());
          data.setDisplayName(user.getDisplayName());
          return data;
        })
        .collect(Collectors.toList());
  }

  // 期限付きメッセージの権限チェック
  /**
   * 指定されたユーザーIDのロールコードを取得します。
   */
  public String getUserRole(String userId) {
    Optional<User> userOpt = userRepository.findByUsername(userId);
    if (userOpt.isEmpty()) {
      return "guest";
    }
    return userOpt.get().getRoleCode();
  }

  /**
   * ユーザーID (username) に基づいて、ユーザーの表示名を取得します。
   */
  public String getDisplayName(String userId) {
    Optional<User> userOpt = userRepository.findByUsername(userId);

    if (userOpt.isEmpty()) {
      return "不明なユーザー (" + userId + ")";
    }

    // Userオブジェクトが持つ表示名を返す
    return userOpt.get().getDisplayName();
  }

  /**
   * 全ての生徒のユーザーIDリストを取得します。
   * (ロールコード 'student' を持つユーザーと仮定)
   */
  public List<String> getAllStudentIds() {
    return StreamSupport.stream(userRepository.findAll().spliterator(), false)
        .filter(user -> "student".equals(user.getRoleCode()))
        .map(User::getUsername)
        .collect(Collectors.toList());
  }
}
