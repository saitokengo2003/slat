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

  // -----------------------------------------------------------------
  // 💡 SQL 定数定義 (すべてのSQLをクラスの最上部に集約)
  // -----------------------------------------------------------------

  /** SQL 全件取得（アクティブなユーザー） */
  private static final String SQL_SELECT_ALL_ACTIVE = "SELECT \"id\", \"username\", \"password_hash\", \"status\", \"created_at\", \"updated_at\", \"last_login_at\", "
      +
      "\"display_name\", \"role_code\", \"grade\", \"class_name\", \"number\" " +
      "FROM \"users_s\" WHERE \"status\" = 'active' ORDER BY \"grade\", \"class_name\", \"number\"";

  /** SQL ID検索 */
  private static final String SQL_SELECT_BY_ID = "SELECT \"id\", \"username\", \"password_hash\", \"status\", \"created_at\", \"updated_at\", \"last_login_at\", "
      +
      "\"display_name\", \"role_code\", \"grade\", \"class_name\", \"number\" " +
      "FROM \"users_s\" WHERE \"id\" = CAST(:id AS uuid)";

  /** SQL 1件挿入 */
  private static final String SQL_INSERT_ONE = "INSERT INTO \"users_s\" (\"username\", \"password_hash\", \"display_name\", \"role_code\", \"grade\", \"class_name\", \"number\", \"status\") "
      +
      "VALUES (:username, :password_hash, :display_name, :role_code, :grade, :class_name, :number, 'active')";

  /** SQL 1件更新 (重複エラーを解消) */
  private static final String SQL_UPDATE_ONE = "UPDATE \"users_s\" SET " +
      " \"username\" = :username, " +
      " \"password_hash\" = :password_hash, " +
      " \"display_name\" = :display_name, " +
      " \"role_code\" = :role_code, " +
      " \"grade\" = :grade, " +
      " \"class_name\" = :class_name, " +
      " \"number\" = :number, " +
      " \"updated_at\" = CURRENT_TIMESTAMP " +
      "WHERE \"id\" = CAST(:id AS uuid)";

  /** SQL 1件削除 */
  private static final String SQL_DELETE_ONE = "DELETE FROM \"users_s\" WHERE \"id\" = CAST(:id AS uuid)";

  // -----------------------------------------------------------------
  // コンストラクタ
  // -----------------------------------------------------------------

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

      // 基本フィールド
      data.setId(rs.getString("id"));
      data.setUsername(rs.getString("username"));
      data.setPassword_hash(rs.getString("password_hash"));
      data.setStatus(rs.getString("status"));
      data.setDisplay_name(rs.getString("display_name"));
      data.setRole_code(rs.getString("role_code"));
      data.setClass_name(rs.getString("class_name"));

      // 日時型
      data.setCreated_at(rs.getObject("created_at", OffsetDateTime.class));
      data.setUpdated_at(rs.getObject("updated_at", OffsetDateTime.class));
      data.setLast_login_at(rs.getObject("last_login_at", OffsetDateTime.class));

      // NULL許容のInteger型を安全に取得
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

  // -----------------------------------------------------------------
  // CRUD メソッド
  // -----------------------------------------------------------------

  public List<AccountadminData> findAllActiveAccounts() {
    return jdbc.query(SQL_SELECT_ALL_ACTIVE, Collections.emptyMap(), new AccountadminDataRowMapper());
  }

  public AccountadminData findById(String id) {
    Map<String, Object> params = Collections.singletonMap("id", id);
    try {
      return jdbc.queryForObject(SQL_SELECT_BY_ID, params, new AccountadminDataRowMapper());
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      return null;
    }
  }

  public int insert(AccountadminData data) throws SQLException {
    Map<String, Object> params = new HashMap<>();
    params.put("username", data.getUsername());
    params.put("password_hash", data.getPassword_hash());
    params.put("display_name", data.getDisplayName());
    params.put("role_code", data.getRoleCode());
    params.put("grade", data.getGrade());
    params.put("class_name", data.getClassName());
    params.put("number", data.getNumber());

    int updateRow = jdbc.update(SQL_INSERT_ONE, params);
    if (updateRow != 1) {
      throw new SQLException("アカウント登録に失敗しました。");
    }
    return updateRow;
  }

  // 💡 update(AccountadminData) メソッドを一つに統合し、重複エラーを解消
  public int update(AccountadminData data) throws SQLException {
    Map<String, Object> params = new HashMap<>();

    params.put("id", data.getId());
    params.put("username", data.getUsername());
    params.put("password_hash", data.getPassword_hash());
    params.put("display_name", data.getDisplayName());
    params.put("role_code", data.getRoleCode());
    params.put("grade", data.getGrade());
    params.put("class_name", data.getClassName());
    params.put("number", data.getNumber());

    int updateRow = jdbc.update(SQL_UPDATE_ONE, params);
    if (updateRow != 1) {
      throw new SQLException("アカウント更新に失敗しました。");
    }
    return updateRow;
  }

  public int delete(String id) throws SQLException {
    Map<String, Object> params = Collections.singletonMap("id", id);
    int updateRow = jdbc.update(SQL_DELETE_ONE, params);

    if (updateRow != 1) {
      throw new SQLException("アカウント削除に失敗しました。更新件数が0件または複数件でした。");
    }
    return updateRow;
  }
}
