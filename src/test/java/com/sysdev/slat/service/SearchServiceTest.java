package com.sysdev.slat.service;

import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sysdev.slat.SearchResultDto;

public class SearchServiceTest {

  @Test
  @DisplayName("searchMessages：1件の結果を返す")
  void testSearchMessages() throws Exception {

    // JdbcTemplate のモック
    JdbcTemplate mockJdbc = mock(JdbcTemplate.class);

    // SearchService に注入
    SearchService service = new SearchService();
    var f = SearchService.class.getDeclaredField("jdbc");
    f.setAccessible(true);
    f.set(service, mockJdbc);

    // ResultSet モック
    ResultSet rs = mock(ResultSet.class);

    when(rs.getString("message_id")).thenReturn("msg123");
    when(rs.getString("body")).thenReturn("本文");
    when(rs.getObject("created_at", OffsetDateTime.class))
        .thenReturn(OffsetDateTime.parse("2024-01-01T10:00:00+09:00"));
    when(rs.getString("group_id")).thenReturn("group001");
    when(rs.getString("group_name")).thenReturn("テストG");
    when(rs.getString("sender_username")).thenReturn("user001");
    when(rs.getString("sender_display_name")).thenReturn("太郎");

    // query のモック設定（曖昧性除去済）
    when(mockJdbc.query(
        anyString(),
        any(Object[].class),
        any(org.springframework.jdbc.core.RowMapper.class))).thenAnswer(invocation -> {

          @SuppressWarnings("unchecked")
          var rowMapper = (org.springframework.jdbc.core.RowMapper<SearchResultDto>) invocation.getArgument(2);

          SearchResultDto dto = rowMapper.mapRow(rs, 0);
          return List.of(dto);
        });

    // 実行
    List<SearchResultDto> list = service.searchMessages("テスト");

    // 検証
    assert list.size() == 1;
    SearchResultDto dto = list.get(0);

    assert dto.getMessageId().equals("msg123");
    assert dto.getBody().equals("本文");
    assert dto.getGroupId().equals("group001");
    assert dto.getGroupName().equals("テストG");
    assert dto.getSenderUsername().equals("user001");
    assert dto.getSenderDisplayName().equals("太郎");
  }
}
