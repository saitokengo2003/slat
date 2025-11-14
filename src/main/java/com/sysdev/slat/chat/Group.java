package com.sysdev.slat.chat;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.util.UUID;
import java.time.OffsetDateTime;

/**
 * group_s テーブルに対応するデータベースエンティティです。
 */
@Table("group_s")
public class Group {

  @Id // 主キー (group_s.id)
  private UUID id;
  private String name; // グループ名
  private Boolean isDm; // DMかどうか
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  public Group() {
  }

  // --- Getter/Setter ---
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getIsDm() {
    return isDm;
  }

  public void setIsDm(Boolean isDm) {
    this.isDm = isDm;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
