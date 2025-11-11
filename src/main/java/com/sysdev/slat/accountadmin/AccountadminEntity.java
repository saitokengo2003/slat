package com.sysdev.slat.accountadmin;

import java.util.ArrayList;
import java.util.List;

public class AccountadminEntity {
  /** タスク情報のリスト */
  private List<AccountadminData> taskList = new ArrayList<AccountadminData>();

  /** エラーメッセージ(表示用) */
  private String errorMessage;

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
}
