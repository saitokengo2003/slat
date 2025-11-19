package com.sysdev.slat.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sysdev.slat.GroupDetailDto;
import com.sysdev.slat.accountadmin.AccountadminData;
import com.sysdev.slat.accountadmin.AccountadminService;
import com.sysdev.slat.service.GroupService;

@SpringBootTest
@AutoConfigureMockMvc
public class GroupControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GroupService groupService;

  @MockBean
  private AccountadminService accountadminService;

  // LoginControllerは今回のテスト対象メソッド内で直接使われていないが、
  // AutowiredされているためMock化しておくのが無難
  @MockBean
  private LoginController loginController;

  // テスト用UUID
  private final UUID TEST_UUID = UUID.randomUUID();

  // --- 1. グループ作成画面 (GET) ---

  @Test
  @DisplayName("作成画面表示: 正常系")
  void testGetGroupcreate() throws Exception {
    // 1. Ready
    doReturn(Collections.emptyList()).when(accountadminService).findAllActiveAccounts();

    // 2. Do & 3. Check
    mockMvc.perform(get("/groupcreate"))
        .andExpect(status().isOk())
        .andExpect(view().name("groupcreate/index"))
        .andExpect(model().attributeExists("accounts"));
  }

  // --- 2. グループ作成処理 (POST) ---

  @Test
  @DisplayName("作成処理: 成功")
  void testCreateGroupSuccess() throws Exception {
    // 1. Ready
    // バリデーション通過
    doReturn(true).when(groupService).validateName(anyString());
    // 作成成功
    doReturn(true).when(groupService).createGroup(anyString(), anyString(), anyList());

    // 2. Do & 3. Check
    mockMvc.perform(post("/groupcreate")
        .param("name", "New Group")
        .param("owner", "owner-id")
        .param("members", "mem1", "mem2"))
        .andExpect(status().isOk()) // redirectではなくViewを返している仕様のため200
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("message"))
        .andExpect(model().attributeDoesNotExist("errorMessage"));
  }

  @Test
  @DisplayName("作成処理: バリデーションエラー（名前不正）")
  void testCreateGroupValidationFail() throws Exception {
    // 1. Ready
    // バリデーション失敗
    doReturn(false).when(groupService).validateName(anyString());

    // 2. Do & 3. Check
    mockMvc.perform(post("/groupcreate")
        .param("name", "") // 不正な名前
        .param("owner", "owner-id")
        .param("members", "mem1"))
        .andExpect(status().isOk())
        .andExpect(view().name("groupcreate/index")) // 入力画面に戻る
        .andExpect(model().attributeExists("errorMessage"));
  }

  @Test
  @DisplayName("作成処理: DB登録失敗")
  void testCreateGroupServiceFail() throws Exception {
    // 1. Ready
    doReturn(true).when(groupService).validateName(anyString());
    // 作成失敗
    doReturn(false).when(groupService).createGroup(anyString(), anyString(), anyList());

    // 2. Do & 3. Check
    mockMvc.perform(post("/groupcreate")
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
    // 1. Ready
    doReturn(Collections.emptyList()).when(groupService).findAllGroupsWithCounts();

    // 2. Do & 3. Check
    mockMvc.perform(get("/groupinfo"))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("groups"));
  }

  // --- 4. グループ詳細表示 (GET) ---

  @Test
  @DisplayName("詳細表示: 正常系 (グループが存在する)")
  void testGroupinfoDetailFound() throws Exception {
    // 1. Ready
    Map<String, Object> groupData = Map.of("id", TEST_UUID, "name", "Test Group");
    List<Map<String, Object>> members = List.of(Map.of("userId", "u1"));
    GroupDetailDto dto = new GroupDetailDto(groupData, members);

    doReturn(dto).when(groupService).getGroupDetail(TEST_UUID);
    doReturn(Collections.emptyList()).when(accountadminService).findAllActiveAccounts();

    // 2. Do & 3. Check
    mockMvc.perform(get("/groupinfo/" + TEST_UUID))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("group"))
        .andExpect(model().attributeExists("members"))
        .andExpect(model().attributeExists("accounts"));
  }

  @Test
  @DisplayName("詳細表示: 該当グループなし")
  void testGroupinfoDetailNotFound() throws Exception {
    // 1. Ready
    doReturn(null).when(groupService).getGroupDetail(TEST_UUID);

    // 2. Do & 3. Check
    mockMvc.perform(get("/groupinfo/" + TEST_UUID))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("errorMessage"));
  }

  @Test
  @DisplayName("詳細表示: ID形式不正 (ExceptionHandlerのテスト)")
  void testGroupinfoDetailBadId() throws Exception {
    // 1. Ready (特になし)

    // 2. Do & 3. Check
    // UUIDではない文字列 "invalid-uuid" を渡す
    mockMvc.perform(get("/groupinfo/invalid-uuid"))
        .andExpect(status().isNotFound()) // @ResponseStatus(HttpStatus.NOT_FOUND)がついているため
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("errorMessage"))
        .andExpect(model().attribute("errorMessage", "グループIDの形式が不正です。"));
  }

  // --- 5. グループ削除処理 (POST) ---

  @Test
  @DisplayName("削除処理: 成功")
  void testDeleteGroupSuccess() throws Exception {
    // 1. Ready
    doReturn(true).when(groupService).deleteGroup(TEST_UUID);

    // 2. Do & 3. Check
    mockMvc.perform(post("/group/" + TEST_UUID + "/delete"))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("message"));
  }

  @Test
  @DisplayName("削除処理: 失敗")
  void testDeleteGroupFail() throws Exception {
    // 1. Ready
    doReturn(false).when(groupService).deleteGroup(TEST_UUID);
    // 削除失敗時は詳細画面を再描画するために情報取得が走る
    GroupDetailDto dto = new GroupDetailDto(Collections.emptyMap(), Collections.emptyList());
    doReturn(dto).when(groupService).getGroupDetail(TEST_UUID);

    // 2. Do & 3. Check
    mockMvc.perform(post("/group/" + TEST_UUID + "/delete"))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("errorMessage"));
  }

  // --- 6. メンバー削除処理 (POST) ---

  @Test
  @DisplayName("メンバー削除: 成功")
  void testDeleteGroupMemberSuccess() throws Exception {
    // 1. Ready
    String userId = "user-001";
    doReturn(true).when(groupService).deleteGroupMember(TEST_UUID, userId);

    // 再描画用のデータ
    GroupDetailDto dto = new GroupDetailDto(Collections.emptyMap(), Collections.emptyList());
    doReturn(dto).when(groupService).getGroupDetail(TEST_UUID);

    // 2. Do & 3. Check
    mockMvc.perform(post("/group/" + TEST_UUID + "/member/" + userId + "/delete"))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("message"));
  }

  // --- 7. メンバー追加処理 (POST) ---

  @Test
  @DisplayName("メンバー追加: 成功")
  void testAddGroupMemberSuccess() throws Exception {
    // 1. Ready
    String userId = "user-new";
    doReturn(true).when(groupService).addGroupMember(TEST_UUID, userId);

    // 再描画用のデータ
    GroupDetailDto dto = new GroupDetailDto(Collections.emptyMap(), Collections.emptyList());
    doReturn(dto).when(groupService).getGroupDetail(TEST_UUID);

    // 2. Do & 3. Check
    mockMvc.perform(post("/group/" + TEST_UUID + "/member/add")
        .param("userId", userId))
        .andExpect(status().isOk())
        .andExpect(view().name("groupinfo/index"))
        .andExpect(model().attributeExists("message"));
  }
}
