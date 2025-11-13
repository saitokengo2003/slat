package com.sysdev.slat.accountadmin;

// 💡 修正: setId(String) を追加

public class AccountForm {

  private String id; // 編集/更新処理のために必要なIDフィールド
  private String userId;
  private String password;
  private String name;
  private String role;
  private String grade;
  private String classId;
  private Integer number;

  // --- Getter/Setter ---
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  } // ⬅️ エラー解消

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getGrade() {
    return grade;
  }

  public void setGrade(String grade) {
    this.grade = grade;
  }

  public String getClassId() {
    return classId;
  }

  public void setClassId(String classId) {
    this.classId = classId;
  }

  public Integer getNumber() {
    return number;
  }

  public void setNumber(Integer number) {
    this.number = number;
  }
}
