package com.sysdev.slat.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GroupService {

  @Autowired
  private JdbcTemplate jdbc;

  public boolean validateName(String name) {
    return name != null && !name.isBlank() && name.length() <= 255;
  }

  public boolean createGroup(String ownerUsername, String groupName, List<String> members) {
    try {
      UUID groupId = UUID.randomUUID();

      jdbc.update(
          "INSERT INTO group_s (id, name, type, created_by) VALUES (?, ?, 'group', ?)",
          groupId, groupName, ownerUsername);

      // 作成者を owner として group_members に INSERT
      jdbc.update(
          "INSERT INTO group_members (group_id, user_id, role_in_group) VALUES (?, ?, 'owner')",
          groupId, ownerUsername);

      // メンバー（重複/owner 除外）
      List<String> distinctMembers = Optional.ofNullable(members)
          .orElseGet(List::of).stream()
          .filter(u -> u != null && !u.isBlank())
          .map(String::trim)
          .filter(u -> !u.equals(ownerUsername))
          .distinct()
          .collect(Collectors.toList());

      if (!distinctMembers.isEmpty()) {
        // Postgres: 重複抑止（ユニーク制約がある前提、なければ付与推奨）
        final String sql = "INSERT INTO group_members (group_id, user_id, role_in_group) " +
            "VALUES (?, ?, 'member') " +
            "ON CONFLICT (group_id, user_id) DO NOTHING";

        jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement ps, int i) throws SQLException {
            ps.setObject(1, groupId);
            ps.setString(2, distinctMembers.get(i));
          }

          @Override
          public int getBatchSize() {
            return distinctMembers.size();
          }
        });
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
