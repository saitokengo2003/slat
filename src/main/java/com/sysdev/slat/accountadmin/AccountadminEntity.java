package com.sysdev.slat.accountadmin;

import com.sysdev.slat.user.User;
import java.util.ArrayList;
import java.util.List;

public class AccountadminEntity {

  private List<AccountadminData> taskList = new ArrayList<AccountadminData>();

  private String errorMessage;

  private List<User> accountList = new ArrayList<>();

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

  public List<User> getAccountList() {
    return accountList;
  }

  public void setAccountList(List<User> accountList) {
    this.accountList = accountList;
  }
}
