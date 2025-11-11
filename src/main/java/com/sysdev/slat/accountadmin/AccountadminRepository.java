package com.sysdev.slat.accountadmin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountadminRepository {

  private final NamedParameterJdbcTemplate jdbc;

  // 💡 SQL SELECT: ダブルクォーテーションでカラム名を保護し、大文字小文字の問題を回避
  private static final String SQL_SELECT_ALL_ACTIVE = "SELECT \"id\", \"username\", \"password_hash\", \"status\", \"created_at\", \"updated_at\", \"last_login_at\", "
      +
      "\"display_name\", \"role_code\", \"grade\", \"class_name\", \"number\" " +
      "FROM \"users_s\" WHERE \"status\" = 'active' ORDER BY \"grade\", \"class_name\", \"number\"";

  /** SQL DELETE: 🚨 修正箇所: :id パラメータを UUID 型にキャスト */
  private static final String SQL_DELETE_ONE = "DELETE FROM \"users_s\" WHERE \"id\" = CAST(:id AS uuid)";

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
      data.setClass_name(rs.getString("class_name"));

      // 日時型 (OffsetDateTime)
      data.setCreated_at(rs.getObject("created_at", OffsetDateTime.class));
      data.setUpdated_at(rs.getObject("updated_at", OffsetDateTime.class));
      data.setLast_login_at(rs.getObject("last_login_at", OffsetDateTime.class));

      // NULL許容のInteger型を安全に取得 (rs.getInt + rs.wasNull)
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
    return jdbc.query(SQL_SELECT_ALL_ACTIVE, Collections.emptyMap(), new AccountadminDataRowMapper());
  }

  /**
   * 指定されたIDのデータを削除します。
   */
  public int delete(String id) throws SQLException {
    // 💡 SQL DELETE実行
    Map<String, Object> params = Collections.singletonMap("id", id);
    int updateRow = jdbc.update(SQL_DELETE_ONE, params);

    if (updateRow != 1) {
      throw new SQLException("アカウント削除に失敗しました (ID: " + id + ")。更新件数が0件または複数件でした。");
    }
    return updateRow;
  }

  /** アカウント作成 */
  private static final String SQL_INSERT_ONE = "INSERT INTO \"users_s\" (\"username\", \"password_hash\", \"display_name\", \"role_code\", \"grade\", \"class_name\", \"number\", \"status\") "
      +
      "VALUES (:username, :password_hash, :display_name, :role_code, :grade, :class_name, :number, 'active')";

  // ... (既存のコンストラクタ、RowMapper、findAllActiveAccounts メソッドは維持) ...

  public int insert(AccountadminData data) throws SQLException {
    // 💡 HashMap のインポートが必要 (java.util.HashMap)
    Map<String, Object> params = new HashMap<>();

    params.put("username", data.getUsername());
    // 🚨 DBは password_hash をNOT NULLで要求するため、ハッシュ化された文字列を渡す必要があります
    params.put("password_hash", data.getPassword_hash());
    params.put("display_name", data.getDisplay_name()); // ※ フォームに display_name がないため、別途設定が必要
    params.put("role_code", data.getRole_code());
    params.put("grade", data.getGrade());
    params.put("class_name", data.getClass_name());
    params.put("number", data.getNumber());

    int updateRow = jdbc.update(SQL_INSERT_ONE, params);

    if (updateRow != 1) {
      throw new SQLException("アカウント登録に失敗しました。更新件数が異常です。");
    }
    return updateRow;
  }
}
