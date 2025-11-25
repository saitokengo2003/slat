package com.sysdev.slat.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sysdev.slat.GroupDetailDto;
import com.sysdev.slat.accountadmin.AccountadminService;
import com.sysdev.slat.service.GroupService;
import com.sysdev.slat.user.UserData;

@SpringBootTest
@AutoConfigureMockMvc
public class GroupControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GroupService groupService;

  @MockBean
  private AccountadminService accountadminService;

  @MockBean
  private LoginController loginController;

  private final UUID TEST_UUID = UUID.randomUUID();
  private UserData mockUser;

  @BeforeEach
  void setUp() {
    mockUser = new UserData();
    mockUser.setUserId("admin");
    mockUser.setDisplayName("管理者");
  }

  private Map<String, Object> dummyGroupData() {
    Map<String, Object> map = new HashMap<>();
    map.put("id", TEST_UUID);
    map.put("name", "ダミーグループ");
    map.put("created_by", "作成者");
    map.put("created_at", OffsetDateTime.now());
    return map;
  }

  // --- 1. グループ作成画面 (GET) ---

  @Test
  @DisplayName("作成画面表示: 正常系")
  void testGetGroupcreate() throws Exception {
    doReturn(Collections.emptyList()).when(accountadminService).findAllActiveAccounts();

    mockMvc.perform(get("/groupcreate")
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("groupcreate/index"))
        .andExpect(model().attributeExists("accounts"));
  }

  // --- 2. グループ作成処理 (POST) ---

  @Test
  @DisplayName("作成処理: 成功")
  void testCreateGroupSuccess() throws Exception {
    doReturn(true).when(groupService).validateName(anyString());
    doReturn(true).when(groupService).createGroup(anyString(), anyString(), anyList());

    mockMvc.perform(post("/groupcreate")
        .sessionAttr("userData", mockUser)
        .param("name", "New Group")
        .param("owner", "owner-id")
        .param("members", "mem1", "mem2"))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("message"));
  }

  @Test
  @DisplayName("作成処理: バリデーションエラー（名前不正）")
  void testCreateGroupValidationFail() throws Exception {
    doReturn(false).when(groupService).validateName(anyString());

    mockMvc.perform(post("/groupcreate")
        .sessionAttr("userData", mockUser)
        .param("name", "")
        .param("owner", "owner-id")
        .param("members", "mem1"))
        .andExpect(status().isOk())
        .andExpect(view().name("groupcreate/index"))
        .andExpect(model().attributeExists("errorMessage"));
  }

  @Test
  @DisplayName("作成処理: DB登録失敗")
  void testCreateGroupServiceFail() throws Exception {
    doReturn(true).when(groupService).validateName(anyString());
    doReturn(false).when(groupService).createGroup(anyString(), anyString(), anyList());

    mockMvc.perform(post("/groupcreate")
        .sessionAttr("userData", mockUser)
        .param("name", "Valid Name")
        .param("owner", "owner-id")
        .param("members", "mem1"))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("errorMessage"));
  }

  // --- 3. グループ一覧表示 (GET) ---

  @Test
  @DisplayName("一覧表示: 正常系")
  void testGroupinfoList() throws Exception {
    doReturn(Collections.emptyList()).when(groupService).findAllGroupsWithCounts();

    mockMvc.perform(get("/groupinfo")
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("groups"));
  }

  // --- 4. グループ詳細表示 (GET) ---

  @Test
  @DisplayName("詳細表示: 正常系 (グループが存在する)")
  void testGroupinfoDetailFound() throws Exception {
    Map<String, Object> groupData = dummyGroupData();

    Map<String, Object> memberMap = new HashMap<>();
    memberMap.put("user_id", "u1");
    memberMap.put("display_name", "メンバー1");
    memberMap.put("role_in_group", "member");
    memberMap.put("role_code", "STUDENT");

    List<Map<String, Object>> members = List.of(memberMap);
    GroupDetailDto dto = new GroupDetailDto(groupData, members);

    doReturn(dto).when(groupService).getGroupDetail(TEST_UUID);
    doReturn(Collections.emptyList()).when(accountadminService).findAllActiveAccounts();

    mockMvc.perform(get("/groupinfo/" + TEST_UUID)
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("group"))
        .andExpect(model().attributeExists("members"))
        .andExpect(model().attributeExists("accounts"));
  }

  @Test
  @DisplayName("詳細表示: 該当グループなし")
  void testGroupinfoDetailNotFound() throws Exception {
    doReturn(null).when(groupService).getGroupDetail(TEST_UUID);

    mockMvc.perform(get("/groupinfo/" + TEST_UUID)
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("errorMessage"));
  }

  @Test
  @DisplayName("詳細表示: ID形式不正 (ExceptionHandler)")
  void testGroupinfoDetailBadId() throws Exception {
    mockMvc.perform(get("/groupinfo/invalid-uuid")
        .sessionAttr("userData", mockUser))
        .andExpect(status().isNotFound())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("errorMessage"));
  }

  // --- 5. グループ削除処理 (POST) ---

  @Test
  @DisplayName("削除処理: 成功")
  void testDeleteGroupSuccess() throws Exception {
    doReturn(true).when(groupService).deleteGroup(TEST_UUID);

    mockMvc.perform(post("/group/" + TEST_UUID + "/delete")
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("message"));
  }

  @Test
  @DisplayName("削除処理: 失敗（データは残っているため再表示）")
  void testDeleteGroupFail() throws Exception {
    doReturn(false).when(groupService).deleteGroup(TEST_UUID);

    GroupDetailDto dto = new GroupDetailDto(dummyGroupData(), Collections.emptyList());
    doReturn(dto).when(groupService).getGroupDetail(TEST_UUID);

    mockMvc.perform(post("/group/" + TEST_UUID + "/delete")
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("errorMessage"))
        .andExpect(model().attributeExists("group"));
  }

  @Test
  @DisplayName("削除処理: 失敗（かつ、再表示しようとしたらグループ自体が消えていた場合）")
  void testDeleteGroupFailDetailNull() throws Exception {
    doReturn(false).when(groupService).deleteGroup(TEST_UUID);
    doReturn(null).when(groupService).getGroupDetail(TEST_UUID);

    mockMvc.perform(post("/group/" + TEST_UUID + "/delete")
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("errorMessage"))
        .andExpect(model().attributeDoesNotExist("group"))
        .andExpect(model().attributeExists("groups"));
  }

  // --- 6. メンバー削除処理 (POST) ---

  @Test
  @DisplayName("メンバー削除: 成功")
  void testDeleteGroupMemberSuccess() throws Exception {
    String userId = "user-001";
    doReturn(true).when(groupService).deleteGroupMember(TEST_UUID, userId);

    GroupDetailDto dto = new GroupDetailDto(dummyGroupData(), Collections.emptyList());
    doReturn(dto).when(groupService).getGroupDetail(TEST_UUID);

    mockMvc.perform(post("/group/" + TEST_UUID + "/member/" + userId + "/delete")
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("message"));
  }

  @Test
  @DisplayName("メンバー削除: 失敗")
  void testDeleteGroupMemberFail() throws Exception {
    String userId = "user-001";
    doReturn(false).when(groupService).deleteGroupMember(TEST_UUID, userId);

    GroupDetailDto dto = new GroupDetailDto(dummyGroupData(), Collections.emptyList());
    doReturn(dto).when(groupService).getGroupDetail(TEST_UUID);

    mockMvc.perform(post("/group/" + TEST_UUID + "/member/" + userId + "/delete")
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("errorMessage"));
  }

  @Test
  @DisplayName("メンバー削除: 処理後にグループが見つからない場合")
  void testDeleteGroupMemberGroupNotFound() throws Exception {
    String userId = "user-001";
    doReturn(true).when(groupService).deleteGroupMember(TEST_UUID, userId);
    doReturn(null).when(groupService).getGroupDetail(TEST_UUID);

    mockMvc.perform(post("/group/" + TEST_UUID + "/member/" + userId + "/delete")
        .sessionAttr("userData", mockUser))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("errorMessage"))
        .andExpect(model().attribute("errorMessage", "指定されたグループが見つかりません。"));
  }

  // --- 7. メンバー追加処理 (POST) ---

  @Test
  @DisplayName("メンバー追加: 成功")
  void testAddGroupMemberSuccess() throws Exception {
    String userId = "user-new";
    doReturn(true).when(groupService).addGroupMember(TEST_UUID, userId);

    GroupDetailDto dto = new GroupDetailDto(dummyGroupData(), Collections.emptyList());
    doReturn(dto).when(groupService).getGroupDetail(TEST_UUID);

    mockMvc.perform(post("/group/" + TEST_UUID + "/member/add")
        .sessionAttr("userData", mockUser)
        .param("userId", userId))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("message"));
  }

  @Test
  @DisplayName("メンバー追加: 失敗")
  void testAddGroupMemberFail() throws Exception {
    String userId = "user-new";
    doReturn(false).when(groupService).addGroupMember(TEST_UUID, userId);

    GroupDetailDto dto = new GroupDetailDto(dummyGroupData(), Collections.emptyList());
    doReturn(dto).when(groupService).getGroupDetail(TEST_UUID);

    mockMvc.perform(post("/group/" + TEST_UUID + "/member/add")
        .sessionAttr("userData", mockUser)
        .param("userId", userId))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("errorMessage"));
  }

  @Test
  @DisplayName("メンバー追加: 処理後にグループが見つからない場合")
  void testAddGroupMemberGroupNotFound() throws Exception {
    String userId = "user-new";
    doReturn(true).when(groupService).addGroupMember(TEST_UUID, userId);
    doReturn(null).when(groupService).getGroupDetail(TEST_UUID);

    mockMvc.perform(post("/group/" + TEST_UUID + "/member/add")
        .sessionAttr("userData", mockUser)
        .param("userId", userId))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("errorMessage"))
        .andExpect(model().attribute("errorMessage", "指定されたグループが見つかりません。"));
  }
}
