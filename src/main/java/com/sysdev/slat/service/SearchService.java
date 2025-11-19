package com.sysdev.slat.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.sysdev.slat.SearchResultDto;

@Service
public class SearchService {

  @Autowired
  private JdbcTemplate jdbc;

  public List<SearchResultDto> searchMessages(String keyword) {
    String like = "%" + keyword + "%";

    String sql = """
        SELECT
          m.id                AS message_id,
          m.body              AS body,
          m.created_at        AS created_at,
          g.id                AS group_id,
          g.name            AS group_name,
          u.username          AS sender_username,
          u.display_name      AS sender_display_name
        FROM messages m
        JOIN users_s u
          ON m.sender_id = u.username
        JOIN group_s g
          ON m.group_id = g.id
        WHERE
          m.body ILIKE ?
          OR u.display_name ILIKE ?
          OR u.username ILIKE ?
          OR g.name ILIKE ?

          UNION ALL

        SELECT
          d.id                AS message_id,
          d.body              AS body,
          d.created_at        AS created_at,
          NULL::uuid          AS group_id,
          -- 疑似グループ名: DM: 送信者→受信者
          ('DM: ' || su.display_name || '→' || ru.display_name) AS group_name,
          su.username         AS sender_username,
          su.display_name     AS sender_display_name
        FROM dmmessage d
        JOIN users_s su
          ON d.sender_id = su.username
        JOIN users_s ru
          ON d.recipient_id = ru.username
        WHERE
          d.body ILIKE ?
          OR su.display_name ILIKE ?
          OR su.username ILIKE ?
          OR ru.display_name ILIKE ?

        ORDER BY created_at DESC
        LIMIT 100
        """;

    Object[] params = {
        like, like, like, like,
        like, like, like, like
    };

    List<SearchResultDto> list = jdbc.query(sql, params, (rs, rowNum) -> {
      SearchResultDto dto = new SearchResultDto();
      dto.setMessageId(rs.getString("message_id"));
      dto.setBody(rs.getString("body"));
      dto.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
      dto.setGroupId(rs.getString("group_id"));
      dto.setGroupName(rs.getString("group_name"));
      dto.setSenderUsername(rs.getString("sender_username"));
      dto.setSenderDisplayName(rs.getString("sender_display_name"));
      return dto;
    });

    System.out.println("[Search] keyword=" + keyword + ", hits=" + list.size());
    return list;
  }
}
