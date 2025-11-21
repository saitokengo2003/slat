package com.sysdev.slat.accountadmin;

import java.time.OffsetDateTime;

public class AccountadminData {

  public AccountadminData() {
  }

  // フィールド定義
  private String id;
  private String username;
  private String password_hash;
  private String status;
  private OffsetDateTime created_at;
  private OffsetDateTime updated_at;
  private OffsetDateTime last_login_at;
  private String display_name; // エラーの原因となったフィールド
  private String role_code;
  private Integer grade;
  private String class_name;
  private Integer number;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword_hash() {
    return password_hash;
  }

  public void setPassword_hash(String password_hash) {
    this.password_hash = password_hash;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public OffsetDateTime getCreated_at() {
    return created_at;
  }

  public void setCreated_at(OffsetDateTime created_at) {
    this.created_at = created_at;
  }

  public OffsetDateTime getUpdated_at() {
    return updated_at;
  }

  public void setUpdated_at(OffsetDateTime updated_at) {
    this.updated_at = updated_at;
  }

  public OffsetDateTime getLast_login_at() {
    return last_login_at;
  }

  public void setLast_login_at(OffsetDateTime last_login_at) {
    this.last_login_at = last_login_at;
  }

  public String getDisplay_name() {
    return display_name;
  }

  public void setDisplay_name(String display_name) {
    this.display_name = display_name;
  }

  // 💡 その他の不足しているセッター/ゲッターも追加
  public String getRole_code() {
    return role_code;
  }

  public void setRole_code(String role_code) {
    this.role_code = role_code;
  }

  public Integer getGrade() {
    return grade;
  }

  public void setGrade(Integer grade) {
    this.grade = grade;
  }

  public String getClass_name() {
    return class_name;
  }

  public void setClass_name(String class_name) {
    this.class_name = class_name;
  }

  public Integer getNumber() {
    return number;
  }

  public void setNumber(Integer number) {
    this.number = number;
  }
}
