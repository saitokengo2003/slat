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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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

  public List<AccountadminData> findAllActiveAccounts() {
    return accountadminRepository.findAllActiveAccounts();
  }

  // -----------------------------------------------------------------
  // 1. アカウント一覧取得
  // -----------------------------------------------------------------
  public AccountadminEntity getAccountListEntity() {
    AccountadminEntity entity = new AccountadminEntity();

    Iterable<User> userIterable = userRepository.findAll();
    List<User> userList = StreamSupport.stream(userIterable.spliterator(), false)
        .collect(Collectors.toList());

    logger.info("アカウント一覧データ取得成功。件数: {}", userList.size());
    entity.setAccountList(userList);

    return entity;
  }

  // -----------------------------------------------------------------
  // 2. アカウント削除
  // -----------------------------------------------------------------
  public void deleteAccount(String accountId) throws SQLException {
    accountadminRepository.delete(accountId);
    logger.info("アカウント (ID: {}) の削除に成功しました。", accountId);
  }

  // -----------------------------------------------------------------
  // 3. アカウント作成
  // -----------------------------------------------------------------
  @Transactional
  public void createAccount(AccountForm form) {
    try {
      User newUser = new User();

      newUser.setUsername(form.getUserId());
      newUser.setPasswordHash(form.getPassword());
      newUser.setDisplayName(form.getName());
      newUser.setRoleCode(form.getRole());

      OffsetDateTime now = OffsetDateTime.now();
      newUser.setCreatedAt(now);
      newUser.setUpdatedAt(now);

      if (form.getGrade() != null && !form.getGrade().isEmpty()) {
        try {
          newUser.setGrade(Integer.parseInt(form.getGrade()));
        } catch (NumberFormatException e) {
          logger.warn("学年 (grade) の値 '{}' が数値ではありません。", form.getGrade());
        }
      }

      newUser.setClassName(form.getClassId());
      newUser.setNumber(form.getNumber());
      newUser.setStatus("active");

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
  // 4. insertAccount（既存）
  // -----------------------------------------------------------------
  public void insertAccount(AccountadminData accountData) throws SQLException {
    accountadminRepository.insert(accountData);
    logger.info("アカウント (Username: {}) の登録に成功しました。", accountData.getUsername());
  }

  // -----------------------------------------------------------------
  // 5. アカウント詳細取得（編集画面用）
  // -----------------------------------------------------------------
  public AccountForm getAccountById(String id) {
    UUID uuid;
    try {
      uuid = UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("不正なUUID形式のIDです: " + id, e);
    }

    User user = userRepository.findById(uuid).orElse(null);

    if (user == null) {
      throw new RuntimeException("指定されたIDのユーザーが見つかりません: " + id);
    }

    AccountForm form = new AccountForm();
    form.setUserId(user.getUsername());
    form.setName(user.getDisplayName());
    form.setPassword(""); // ← 安全のためハッシュは表示しない
    form.setRole(user.getRoleCode());
    form.setGrade(user.getGrade() != null ? String.valueOf(user.getGrade()) : "");
    form.setClassId(user.getClassName());
    form.setNumber(user.getNumber());

    return form;
  }
}
