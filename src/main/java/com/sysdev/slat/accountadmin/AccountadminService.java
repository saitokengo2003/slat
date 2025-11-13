package com.sysdev.slat.accountadmin;

import com.sysdev.slat.user.User;
import com.sysdev.slat.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

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
  // 1. アカウント一覧取得 (Read)
  // -----------------------------------------------------------------
  public AccountadminEntity getAccountListEntity() {
    AccountadminEntity entity = new AccountadminEntity();
    try {
      List<AccountadminData> accountList = accountadminRepository.findAllActiveAccounts();
      entity.setTaskList(accountList);
      logger.info("アカウント一覧データ取得成功。件数: {}", accountList.size());
    } catch (Exception e) {
      logger.error("アカウント情報の取得中に致命的なエラーが発生しました。", e);
      entity.setErrorMessage("データ取得エラー: " + e.getLocalizedMessage());
    }
    return entity;
  }

  // -----------------------------------------------------------------
  // 2. アカウント削除 (Delete)
  // -----------------------------------------------------------------
  public void deleteAccount(String accountId) throws SQLException {
    accountadminRepository.delete(accountId);
    logger.info("アカウント (ID: {}) の削除に成功しました。", accountId);
  }

  // -----------------------------------------------------------------
  // 3. アカウント作成 (Create)
  // -----------------------------------------------------------------
  @Transactional
  public void createAccount(AccountForm form) {
    AccountadminData data = convertFormToData(form);
    try {
      accountadminRepository.insert(data);
      logger.info("アカウント (Username: {}) の登録に成功しました。", data.getUsername());
    } catch (Exception e) {
      logger.error("アカウント登録中に予期せぬ例外が発生しました。", e);
      throw new RuntimeException("アカウント登録中にエラーが発生しました: " + e.getLocalizedMessage(), e);
    }
  }

  // -----------------------------------------------------------------
  // 4. アカウント更新 (Update) - 統合ロジック
  // -----------------------------------------------------------------
  /**
   * 既存のアカウントを更新します。
   */
  @Transactional
  public void updateAccount(String id, AccountForm form) throws SQLException {

    // 1. 既存のAccountadminData (DBの全てのカラム値) を取得
    AccountadminData existingData = accountadminRepository.findById(id);

    if (existingData == null) {
      throw new RuntimeException("更新対象のユーザーIDが見つかりません: " + id);
    }

    // 2. フォームデータと既存データをマージ (変更のない項目は既存データを保持)
    AccountadminData updatedData = mergeAccountData(existingData, form);

    // 3. Repositoryへ更新実行
    accountadminRepository.update(updatedData);
    logger.info("アカウント (ID: {}) の更新に成功しました。", id);
  }

  // -----------------------------------------------------------------
  // 5. アカウント詳細取得 (Edit GET - 既存データのフォームへのマッピング)
  // -----------------------------------------------------------------
  public AccountForm getAccountById(String id) {
    AccountadminData data = accountadminRepository.findById(id);

    if (data == null) {
      throw new RuntimeException("指定されたIDのユーザーが見つかりません: " + id);
    }

    AccountForm form = new AccountForm();
    form.setId(data.getId()); // IDをFormに保持
    form.setUserId(data.getUsername());
    form.setName(data.getDisplayName());
    form.setPassword(""); // パスワードハッシュは表示しない
    form.setRole(data.getRoleCode());
    form.setGrade(data.getGrade() != null ? String.valueOf(data.getGrade()) : "");
    form.setClassId(data.getClassName());
    form.setNumber(data.getNumber());

    return form;
  }

  // -----------------------------------------------------------------
  // Helper: AccountForm -> AccountadminData 変換 (Create/Update Helper)
  // -----------------------------------------------------------------
  private AccountadminData convertFormToData(AccountForm form) {
    AccountadminData data = new AccountadminData();

    data.setUsername(form.getUserId());
    data.setPasswordHash(form.getPassword()); // 💡 正しいセッターを使用
    data.setDisplayName(form.getName());
    data.setRoleCode(form.getRole());

    if (form.getGrade() != null && !form.getGrade().isEmpty()) {
      try {
        data.setGrade(Integer.parseInt(form.getGrade()));
      } catch (NumberFormatException e) {
        data.setGrade(null);
      }
    }

    data.setClassName(form.getClassId());
    data.setNumber(form.getNumber());
    data.setStatus("active");

    return data;
  }

  // -----------------------------------------------------------------
  // Helper: 既存データとフォームデータをマージするメソッド (Update Helper)
  // -----------------------------------------------------------------
  private AccountadminData mergeAccountData(AccountadminData existingData, AccountForm form) {

    // フォームのフィールドが空/nullでない場合のみ、既存のデータ (existingData) を上書きします。

    // **ユーザー名**
    if (form.getUserId() != null && !form.getUserId().trim().isEmpty()) {
      existingData.setUsername(form.getUserId());
    }

    // **表示名**
    if (form.getName() != null && !form.getName().trim().isEmpty()) {
      existingData.setDisplayName(form.getName());
    }

    // **パスワード** (空でない場合のみ更新 - NOT NULL制約対策)
    String newPassword = form.getPassword();
    if (newPassword != null && !newPassword.isEmpty()) {
      // 💡 修正: 正しいセッターを使用
      existingData.setPassword_hash(newPassword);
    }

    // **権限 (role_code)**
    if (form.getRole() != null && !form.getRole().isEmpty()) {
      existingData.setRoleCode(form.getRole());
    }

    // **学年 (grade)**
    if (form.getGrade() != null) {
      try {
        existingData.setGrade(form.getGrade().isEmpty() ? null : Integer.parseInt(form.getGrade()));
      } catch (NumberFormatException e) {
        // 数値変換エラーの場合は、既存の値を保持
      }
    }

    // **クラス (class_name)**
    if (form.getClassId() != null) {
      existingData.setClassName(form.getClassId().isEmpty() ? null : form.getClassId());
    }

    // **番号 (number)**
    if (form.getNumber() != null) {
      existingData.setNumber(form.getNumber());
    }

    return existingData;
  }
}
