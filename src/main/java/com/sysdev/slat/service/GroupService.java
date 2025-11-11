package com.sysdev.slat.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GroupService {

  @Autowired
  private JdbcTemplate jdbc;

  public boolean validateName(String name) {
    return name != null && !name.isBlank() && name.length() <= 255;
  }

  public boolean createGroup(String ownerUsername, String groupName, String member) {
    try {
      UUID groupId = UUID.randomUUID();

      jdbc.update(
          "INSERT INTO group_s (id, name, type, created_by) VALUES (?, ?, 'group', ?)",
          groupId, groupName, ownerUsername);

      // 作成者を owner として group_members に INSERT
      jdbc.update(
          "INSERT INTO group_members (group_id, user_id, role_in_group) VALUES (?, ?, 'owner')",
          groupId, ownerUsername);

      // member 入力があれば member ロールで追加（ownerと同じ/空はスキップ）
      if (member != null && !member.isBlank() && !member.equals(ownerUsername)) {
        jdbc.update(
            "INSERT INTO group_members (group_id, user_id, role_in_group) VALUES (?, ?, 'member')",
            groupId, member);
      }

      return true;

    } catch (DataAccessException e) {
      System.err.println("[GroupService#createGroup] DB error: " + e.getMessage());
      Throwable c = e.getCause();
      while (c != null) {
        System.err.println("  cause: " + c.getMessage());
        c = c.getCause();
      }
      return false;
    }
  }
}
