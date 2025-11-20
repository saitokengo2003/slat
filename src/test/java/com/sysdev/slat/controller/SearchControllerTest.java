package com.sysdev.slat.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.ui.Model;

import com.sysdev.slat.SearchResultDto;
import com.sysdev.slat.service.SearchService;

class SearchControllerTest {

  @InjectMocks
  private SearchController controller;

  @Mock
  private SearchService searchService;

  @Mock
  private Model model;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testSearchMessage_keywordNullOrBlank() {
    // キーワードが null の場合
    String view1 = controller.searchMessage(null, model);
    verify(model).addAttribute("errorMessage", "検索キーワードを入力してください。");
    assertEquals("index", view1);

    // キーワードが空文字の場合
    String view2 = controller.searchMessage("", model);
    verify(model, times(2)).addAttribute("errorMessage", "検索キーワードを入力してください。");
    assertEquals("index", view2);

    // キーワードが空白のみの場合
    String view3 = controller.searchMessage("   ", model);
    verify(model, times(3)).addAttribute("errorMessage", "検索キーワードを入力してください。");
    assertEquals("index", view3);
  }

  @Test
  void testSearchMessage_normal() {
    String keyword = "test";

    // SearchResultDto をモックで作成
    SearchResultDto dto1 = mock(SearchResultDto.class);
    SearchResultDto dto2 = mock(SearchResultDto.class);

    List<SearchResultDto> results = List.of(dto1, dto2);

    when(searchService.searchMessages(keyword)).thenReturn(results);

    String view = controller.searchMessage(keyword, model);

    // searchService が呼ばれていることを確認
    verify(searchService).searchMessages(keyword);
    // model にキーワードと結果が追加されていることを確認
    verify(model).addAttribute("keyword", keyword);
    verify(model).addAttribute("results", results);

    assertEquals("index", view);
  }
}
