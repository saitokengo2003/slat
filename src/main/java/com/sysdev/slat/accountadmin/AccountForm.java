package com.sysdev.slat.accountadmin;

// フォームが受け取るデータに対応するクラス
public class AccountForm {

  private String id;
  private String userId; // HTML: user-id / DB: username
  private String password; // HTML: password / DB: password_hash
  private String name; // HTML: name / DB: display_name
  private String role; // HTML: role / DB: role_code
  private String grade; // HTML: grade / ServiceでIntegerに変換
  private String classId; // HTML: class / DB: class_name
  private Integer number; // HTML: number / DB: number

  // ★追加: idのgetter/setter
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

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
