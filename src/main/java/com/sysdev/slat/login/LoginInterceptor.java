package com.sysdev.slat.login; // パッケージを変更

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.servlet.HandlerInterceptor;

import com.sysdev.slat.user.UserData;

public class LoginInterceptor implements HandlerInterceptor {

  private final String SESSION_USER_DATA_KEY = "userData";

  /**
   * コントローラーの処理が実行される前に呼び出されます。
   */
  @Override
  public boolean preHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler) throws Exception {

    // セッションを取得 (false: セッションがなければ新規作成しない)
    HttpSession session = request.getSession(false);

    // UserDataオブジェクトがセッションに存在するか確認
    Object userDataObj = (session != null) ? session.getAttribute(SESSION_USER_DATA_KEY) : null;

    // ログインしていない場合（セッションがない、またはUserDataがない）
    if (!(userDataObj instanceof UserData)) {

      // ログイン画面にリダイレクト
      response.sendRedirect(request.getContextPath() + "/login");

      // 以降の処理（コントローラーメソッド）を実行せずに中断する
      return false;
    }

    // ログイン済みであれば、そのまま処理を続行する
    return true;
  }
}
