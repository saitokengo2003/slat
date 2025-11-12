package com.sysdev.slat.accountadmin;

import com.sysdev.slat.user.User; // 💡 Userクラスをインポート
import java.util.ArrayList;
import java.util.List;

public class AccountadminEntity {

  // 既存のフィールド
  /** タスク情報のリスト (旧形式または別用途) */
  private List<AccountadminData> taskList = new ArrayList<AccountadminData>();

  /** エラーメッセージ(表示用) */
  private String errorMessage;

  // 💡 新規追加: Userエンティティのリスト（アカウント一覧表示用）
  private List<User> accountList = new ArrayList<>();

  // -----------------------------------------------------------------
  // 既存の Getter/Setter (taskList, errorMessage)
  // -----------------------------------------------------------------
  public List<AccountadminData> getTaskList() {
    return taskList;
  }

  public void setTaskList(List<AccountadminData> taskList) {
    this.taskList = taskList;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  // -----------------------------------------------------------------
  // 💡 追加された Getter/Setter (accountList - エラー解消用)
  // -----------------------------------------------------------------
  /**
   * アカウント情報（Userエンティティのリスト）を取得します。
   */
  public List<User> getAccountList() {
    return accountList;
  }

  /**
   * アカウント情報（Userエンティティのリスト）を設定します。（エラー解消）
   */
  public void setAccountList(List<User> accountList) {
    this.accountList = accountList;
  }
}
