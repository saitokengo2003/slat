package com.sysdev.slat.user;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  @Autowired
  private UserRepository userRepository;

  /**
   * ユーザーID（username）とパスワードで認証を行い、成功時はUserDataを返す。
   */
  public UserData authenticate(String username, String password) {

    // 1. データベースから username に対応するユーザーオブジェクトを取得
    Optional<User> userOpt = userRepository.findByUsername(username);

    // ユーザーが見つからない場合は終了
    if (userOpt.isEmpty()) {
      return null;
    }

    User user = userOpt.get();

    // 2. パスワードをチェック
    if (!user.getPasswordHash().equals(password)) {
      // パスワードが一致しない
      return null;
    }

    // 3. 認証成功: DBから取得した情報を使って UserData を構築して返す
    UserData data = new UserData();
    data.setUserId(user.getUsername());
    data.setDisplayName(user.getDisplayName());
    data.setRoleCode(user.getRoleCode());
    data.setGrade(user.getGrade());
    data.setClassName(user.getClassName());
    data.setNumber(user.getNumber());

    return data;
  }
}
