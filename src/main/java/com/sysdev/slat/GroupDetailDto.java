package com.sysdev.slat;

import java.util.List;
import java.util.Map;

public class GroupDetailDto {
  private final Map<String, Object> group;
  private final List<Map<String, Object>> members;

  public GroupDetailDto(Map<String, Object> group, List<Map<String, Object>> members) {
    this.group = group;
    this.members = members;
  }

  public Map<String, Object> getGroup() {
    return group;
  }

  public List<Map<String, Object>> getMembers() {
    return members;
  }
}
