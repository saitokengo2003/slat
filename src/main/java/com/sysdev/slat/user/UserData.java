package com.sysdev.slat.user;

/**
 * ログインユーザーの情報を保持するデータクラスです。
 */
public class UserData {

  private String userId; // usernameに対応
  private String displayName; // display_nameに対応
  private String roleCode; // role_codeに対応
  private Integer grade;// 学年
  private String className;// クラス名
  private Integer number;// 出席番号

  // --- コンストラクタ ---
  public UserData() {
  }

  // --- Getter/Setter ---

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getRoleCode() {
    return roleCode;
  }

  public void setRoleCode(String roleCode) {
    this.roleCode = roleCode;
  }

  public Integer getGrade() {
    return grade;
  }

  public void setGrade(Integer grade) {
    this.grade = grade;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public Integer getNumber() {
    return number;
  }

  public void setNumber(Integer number) {
    this.number = number;
  }
}
