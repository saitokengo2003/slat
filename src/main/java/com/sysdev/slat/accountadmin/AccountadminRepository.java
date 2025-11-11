package com.sysdev.slat.accountadmin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountadminRepository {

  private final NamedParameterJdbcTemplate jdbc;

  // 💡 SQL: 大文字小文字の問題を回避するため、すべてダブルクォーテーションで囲んでいます。
  private static final String SQL_SELECT_ALL_ACTIVE = "SELECT \"id\", \"username\", \"password_hash\", \"status\", \"created_at\", \"updated_at\", \"last_login_at\", "
      +
      "\"display_name\", \"role_code\", \"grade\", \"class_name\", \"number\" " +
      "FROM \"users_s\" WHERE \"status\" = 'active' ORDER BY \"grade\", \"class_name\", \"number\"";

  /** SQL 1件削除 */
  private static final String SQL_DELETE_ONE = "DELETE FROM \"users_s\" WHERE \"id\" = :id";

  @Autowired
  public AccountadminRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  // -----------------------------------------------------------------
  // RowMapper (DBアクセス時のマッピング)
  // -----------------------------------------------------------------
  private static class AccountadminDataRowMapper implements RowMapper<AccountadminData> {
    @Override
    public AccountadminData mapRow(ResultSet rs, int rowNum) throws SQLException {
      AccountadminData data = new AccountadminData();

      // 基本フィールド (String型)
      data.setId(rs.getString("id"));
      data.setUsername(rs.getString("username"));
      data.setPassword_hash(rs.getString("password_hash"));
      data.setStatus(rs.getString("status"));
      data.setDisplay_name(rs.getString("display_name"));
      data.setRole_code(rs.getString("role_code"));
      data.setClass_name(rs.getString("class_name")); // 修正されたセッター名を使用

      // 日時型 (OffsetDateTime)
      data.setCreated_at(rs.getObject("created_at", OffsetDateTime.class));
      data.setUpdated_at(rs.getObject("updated_at", OffsetDateTime.class));
      data.setLast_login_at(rs.getObject("last_login_at", OffsetDateTime.class));

      // 💡 NULL許容のInteger型を安全に取得 (rs.getInt + rs.wasNull)
      rs.getInt("grade");
      if (!rs.wasNull()) {
        data.setGrade(rs.getInt("grade"));
      } else {
        data.setGrade(null);
      }

      rs.getInt("number");
      if (!rs.wasNull()) {
        data.setNumber(rs.getInt("number"));
      } else {
        data.setNumber(null);
      }

      return data;
    }
  }

  /**
   * アクティブな全アカウントを取得します。（DBアクセス）
   */
  public List<AccountadminData> findAllActiveAccounts() {

    // 💡 データベースアクセスを有効化
    return jdbc.query(SQL_SELECT_ALL_ACTIVE, Collections.emptyMap(), new AccountadminDataRowMapper());
  }

  /**
   * 指定されたIDのデータを削除します。
   */
  public int delete(String id) throws SQLException {
    Map<String, Object> params = Collections.singletonMap("id", id);

    // 💡 DBアクセスを有効化
    int updateRow = jdbc.update(SQL_DELETE_ONE, params);

    if (updateRow != 1) {
      throw new SQLException("アカウント削除に失敗しました (ID: " + id + ")。更新件数が0件または複数件でした。");
    }
    return updateRow;
  }
}
