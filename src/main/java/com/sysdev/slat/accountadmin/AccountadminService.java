package com.sysdev.slat.accountadmin;

import com.sysdev.slat.user.User;
import com.sysdev.slat.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors; // 💡 追加
import java.util.stream.StreamSupport; // 💡 追加
import java.time.OffsetDateTime;

@Service
public class AccountadminService {

  private static final Logger logger = LoggerFactory.getLogger(AccountadminService.class);

  private final AccountadminRepository accountadminRepository;
  private final UserRepository userRepository;

  @Autowired
  public AccountadminService(AccountadminRepository accountadminRepository, UserRepository userRepository) {
    this.accountadminRepository = accountadminRepository;
    this.userRepository = userRepository;
  }

  // -----------------------------------------------------------------
  // 1. アカウント一覧取得 (GET) - 型変換エラーの解消
  // -----------------------------------------------------------------
  /**
   * @Transactionalなしでも動作するよう、RepositoryのfindAll()の戻り値をListに変換
   */
  public AccountadminEntity getAccountListEntity() {
    AccountadminEntity entity = new AccountadminEntity();

    // 1. Iterable<User> を取得
    Iterable<User> userIterable = userRepository.findAll();

    // 2. 変換: Iterable<User> を List<User> に変換する (エラー解消)
    List<User> userList = StreamSupport.stream(userIterable.spliterator(), false)
        .collect(Collectors.toList());

    // 取得成功ログ
    logger.info("アカウント一覧データ取得成功。件数: {}", userList.size());

    // 3. Entityにデータを格納 (setAccountList undefined エラー解消)
    entity.setAccountList(userList);

    // ⚠️ 注意: 既存のtaskListのロジックが残っている場合、別途処理が必要です。

    return entity;
  }

  // -----------------------------------------------------------------
  // 2. アカウント削除 (POST)
  // -----------------------------------------------------------------
  public void deleteAccount(String accountId) throws SQLException {
    accountadminRepository.delete(accountId);
    logger.info("アカウント (ID: {}) の削除に成功しました。", accountId);
  }

  // -----------------------------------------------------------------
  // 3. アカウント作成 (POST)
  // -----------------------------------------------------------------
  @Transactional
  public void createAccount(AccountForm form) {
    try {
      User newUser = new User();

      // --- データのマッピング ---
      newUser.setUsername(form.getUserId());
      newUser.setPasswordHash(form.getPassword());
      newUser.setDisplayName(form.getName());
      newUser.setRoleCode(form.getRole());

      // タイムスタンプの設定 (DBのNOT NULL制約解消)
      OffsetDateTime now = OffsetDateTime.now();
      newUser.setCreatedAt(now);
      newUser.setUpdatedAt(now);

      // 学年 (grade) - StringからIntegerへの変換
      if (form.getGrade() != null && !form.getGrade().isEmpty()) {
        try {
          newUser.setGrade(Integer.parseInt(form.getGrade()));
        } catch (NumberFormatException e) {
          logger.warn("学年 (grade) の値 '{}' が数値ではありません。", form.getGrade());
        }
      }

      // className と number の設定
      newUser.setClassName(form.getClassId()); // HTMLのclassIdをDBのclass_nameに設定
      newUser.setNumber(form.getNumber());
      newUser.setStatus("active");

      // --- データベースに保存 ---
      userRepository.save(newUser);

      logger.info("アカウント (Username: {}) の登録に成功しました。", newUser.getUsername());

    } catch (DataAccessException e) {
      logger.error("データベースへのアカウント登録中に例外が発生しました。", e);
      throw new RuntimeException("アカウント登録エラー: " + e.getLocalizedMessage(), e);
    } catch (Exception e) {
      logger.error("アカウント登録中に予期せぬ例外が発生しました。", e);
      throw new RuntimeException("アカウント登録中に予期せぬエラーが発生しました: " + e.getLocalizedMessage(), e);
    }
  }

  // -----------------------------------------------------------------
  // 4. 既存の insertAccount
  // -----------------------------------------------------------------
  public void insertAccount(AccountadminData accountData) throws SQLException {
    accountadminRepository.insert(accountData);
    logger.info("アカウント (Username: {}) の登録に成功しました。", accountData.getUsername());
  }
}
