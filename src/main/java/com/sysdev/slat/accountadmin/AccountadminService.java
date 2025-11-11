package com.sysdev.slat.accountadmin;

import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AccountadminService {

  private static final Logger logger = LoggerFactory.getLogger(AccountadminService.class);

  private final AccountadminRepository accountadminRepository;

  @Autowired
  public AccountadminService(AccountadminRepository accountadminRepository) {
    this.accountadminRepository = accountadminRepository;
  }

  /**
   * アカウント一覧画面表示用のエンティティを構築します。
   */
  public AccountadminEntity getAccountListEntity() {
    AccountadminEntity entity = new AccountadminEntity();

    try {
      List<AccountadminData> accountList = accountadminRepository.findAllActiveAccounts();

      entity.setTaskList(accountList);
      logger.info("アカウント一覧データ取得成功。件数: {}", accountList.size());

    } catch (Exception e) {
      // 🚨 修正箇所: 画面に具体的なエラーメッセージを表示させる
      logger.error("アカウント情報の取得中に致命的なエラーが発生しました。", e);

      // 画面に表示するエラーメッセージに、例外の原因を含める
      entity.setErrorMessage("データ取得エラー: " + e.getLocalizedMessage());
    }

    return entity;
  }

  /**
   * 指定されたIDのアカウントを削除します。
   */
  public void deleteAccount(String accountId) throws SQLException {
    accountadminRepository.delete(accountId);
    logger.info("アカウント (ID: {}) の削除に成功しました。", accountId);
  }
}
