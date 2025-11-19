package com.sysdev.slat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sysdev.slat.SearchResultDto;
import com.sysdev.slat.service.SearchService;

@Controller
public class SearchController {
  @Autowired
  private SearchService searchService;

  @PostMapping("/search")
  public String searchMessage(
      @RequestParam("keyword") String keyword,
      Model model) {

    if (keyword == null || keyword.isBlank()) {
      model.addAttribute("errorMessage", "検索キーワードを入力してください。");
      return "index"; // トップ画面など、実際のテンプレート名に合わせて
    }

    List<SearchResultDto> results = searchService.searchMessages(keyword);
    System.out.println("[Search] keyword=" + keyword + ", hits=" + results.size());

    model.addAttribute("keyword", keyword);
    model.addAttribute("results", results);

    return "index";
  }

}
