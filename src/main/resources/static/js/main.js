"use strict";

// ログインユーザーIDをDOMから取得
const loggedInUserId = document.getElementById("logged-in-user-id")
  ? document.getElementById("logged-in-user-id").value
  : null;

// ✅ NEW: ログインユーザーのロールをDOMから取得
const loggedInUserRole = document.getElementById("logged-in-user-role")
  ? document.getElementById("logged-in-user-role").value
  : null;

// サイドバー開閉
const sidebar = document.getElementById("sidebar");
const toggleBtn = document.getElementById("menu-toggle");
if (toggleBtn) {
  toggleBtn.addEventListener("click", () => {
    sidebar.classList.toggle("collapsed");
  });
}

// チャット関連
const sendBtn = document.querySelector(".send-btn");
const chatInput = document.querySelector(".chat-input");
const chatArea = document.querySelector(".chat-area");
const expirationBtn = document.querySelector(".expiration-btn");

// ✅ NEW: モーダル要素の取得と初期化
const expirationModalElement = document.getElementById("expirationModal");
const expirationModal = expirationModalElement ? new bootstrap.Modal(expirationModalElement) : null;

// 選択中のチャット相手のID (DM用)
let currentRecipientId = null;
// ⭐ 追加: 選択中のグループのID (グループチャット用)
let currentGroupId = null;
// ✅ 追加: 期限付きメッセージの有効期限
let expirationTime = null;

// スクロール最下部へ
const scrollToBottom = () => {
  chatArea.scrollTop = chatArea.scrollHeight;
};

/* --------------------------------------------------------
  DM履歴の読み込み
-------------------------------------------------------- */
function loadDmHistory(recipientId) {
  chatArea.innerHTML = "";

  if (!loggedInUserId) {
    console.error("ログインID取得失敗");
    chatArea.innerHTML = '<p class="error-message">ログイン状態を確認できません。</p>';
    return;
  }

  fetch(`/api/dm/history?recipientId=${recipientId}`)
    .then((response) => {
      if (!response.ok) {
        throw new Error(`DM履歴の取得に失敗しました: ${response.status}`);
      }
      return response.json();
    })
    .then((messages) => {
      if (messages.length === 0) {
        chatArea.innerHTML = '<p class="no-message-guide">まだメッセージはありません。</p>';
        return;
      }

      messages.forEach((msg) => {
        const isSentByMe = msg.senderId === loggedInUserId;
        const displayName = msg.senderName || msg.senderId;

        addMessage(
          msg.body,
          isSentByMe,
          displayName,
          msg.messageId,
          msg.reactions || [],
          msg.createdAt,
          msg.expirationTime
        );
      });

      scrollToBottom();
    })
    .catch((error) => {
      console.error("履歴取得エラー:", error);
      chatArea.innerHTML = `<p class="error-message">メッセージ履歴の読み込み中にエラー: ${error.message}</p>`;
    });
}

/* --------------------------------------------------------
  メッセージ送信（DM/グループ判別）
-------------------------------------------------------- */
async function sendMessageHandler(messageBody) {
  if (!messageBody.trim()) return;

  if (!loggedInUserId) {
    alert("ログインエラー。再ログインしてください。");
    return;
  }

  let messageData = null;
  let isGroup = false;

  if (currentGroupId) {
    // ⭐ グループチャットの場合
    isGroup = true;
    messageData = {
      groupId: currentGroupId,
      body: messageBody,
      expirationTime: expirationTime,
    };
  } else if (currentRecipientId) {
    // DMの場合
    messageData = {
      recipientId: currentRecipientId,
      body: messageBody,
      expirationTime: expirationTime,
    };
  } else {
    alert("チャット相手を選択してください。");
    return;
  }

  try {
    const response = await fetch("/api/message/send", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(messageData),
    });

    if (response.ok) {
      // メッセージ送信後、表示を更新
      if (isGroup) {
        // グループチャットは再読み込み
        loadGroupHistory(currentGroupId);
      } else {
        // DMはローカルで追加
        loadDmHistory(currentRecipientId);
      } // メッセージ送信後：入力欄とボタン色を元に戻す
      chatInput.value = "";
      sendBtn.classList.remove("active");

      // ✅ 期限設定をリセットし、ボタンの色を戻す
      expirationTime = null;
      expirationBtn.classList.remove("active");
    } else {
      alert("送信失敗: " + (await response.text()));
    }
  } catch (err) {
    console.error("送信中のネットワークエラー:", err);
    alert("通信エラーが発生しました。");
  }
}

/* --------------------------------------------------------
   ⭐ リアクションAPIを呼び出し、画面を更新するハンドラ (グループチャット/DM兼用)
-------------------------------------------------------- */
function handleReactionClick(messageId, reactionElement) {
  if (!messageId) {
    console.error("メッセージIDがないためリアクションできません。", { messageId });
    return;
  }

  // サーバー側でmessageIdからテーブルを判断すると仮定し、既存のAPIをそのまま使う
  const emoji = reactionElement.getAttribute("data-emoji");

  fetch("/api/reaction/toggle", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      messageId: messageId,
      emoji: emoji,
    }),
  })
    .then((response) => response.text())
    .then((result) => {
      if (result === "ADDED" || result === "REMOVED") {
        // API成功後、画面を再読み込み
        if (currentGroupId) {
          loadGroupHistory(currentGroupId);
        } else if (currentRecipientId) {
          loadDmHistory(currentRecipientId);
        }
      } else {
        console.error("リアクションのトグル失敗:", result);
      }

      reactionElement.classList.add("clicked");
      setTimeout(() => reactionElement.classList.remove("clicked"), 200);
    })
    .catch((error) => {
      console.error("リアクションのトグル失敗:", error);
    });
}

/* --------------------------------------------------------
  ★ 右側（自分）・左側（相手）表示対応 addMessage() の修正
-------------------------------------------------------- */
// ⭐ MODIFIED: expirationTime を引数に追加
function addMessage(
  text,
  isRight = true,
  displayName = "",
  messageId = null,
  initialReactions = [],
  createdAt = null,
  expirationTime = null // 期限情報を受け取る
) {
  const message = document.createElement("div");
  message.classList.add("message");
  message.classList.add(isRight ? "right" : "left");

  // ✅ NEW: 期限ラベルの追加と期限切れ判定ロジック
  if (expirationTime) {
    const now = new Date();
    const expiryDate = new Date(expirationTime);

    // 1. 期限切れ判定と本文の変更
    if (now >= expiryDate) {
      message.classList.add("expired");
      text = `[期限切れ] ${text}`;
    }

    // 2. ⭐ NEW: 期限ラベル要素の作成
    const labelElement = document.createElement("div");
    labelElement.classList.add("expiration-label");

    // 日付と時刻の整形 (toLocaleStringで簡潔に)
    const expiryString = expiryDate.toLocaleString("ja-JP", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });

    // 期限切れでない場合は「有効期限: YYYY/MM/DD HH:mm」と表示
    if (now < expiryDate) {
      labelElement.textContent = `有効期限: ${expiryString}まで`;
    } else {
      labelElement.textContent = `このメッセージは ${expiryString} に期限切れとなりました`;
    }

    // ⭐ NEW: message コンテナの先頭にラベルを挿入 (order: 0で最上位に)
    message.appendChild(labelElement);
  }

  if (messageId) {
    message.setAttribute("data-message-id", messageId);
  }

  const avatar = document.createElement("div");
  avatar.classList.add("avatar");

  const initial = displayName ? displayName.trim().charAt(0).toUpperCase() : "?";

  avatar.textContent = initial;

  const bubble = document.createElement("div");
  bubble.classList.add("bubble");
  bubble.textContent = text;

  // ✅ MODIFIED: 時刻要素の生成とフォーマット（日付と時刻）
  if (createdAt) {
    const timeElement = document.createElement("div");
    timeElement.classList.add("message-time");

    const date = new Date(createdAt);

    // 日付 (YYYY/MM/DD) の整形
    const dateString = date
      .toLocaleDateString("ja-JP", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
      })
      .replace(/\//g, "/");

    // 時刻 (HH:mm) の整形
    const timeString = date.toLocaleTimeString("ja-JP", {
      hour: "2-digit",
      minute: "2-digit",
    });

    // 日付と時刻を結合して表示
    timeElement.textContent = `${dateString} ${timeString}`;

    message.appendChild(timeElement); // メッセージコンテナに時刻を追加
  }

  // ⭐ MODIFIED: リアクションを機能リアクションに一本化 (復元)
  if (messageId) {
    // 1. グループチャット / DM (機能するリアクション)
    const reactionsContainer = document.createElement("div");
    reactionsContainer.classList.add("reactions-container");

    function renderReactions(reactions) {
      const reactionCounts = reactions.reduce((acc, reaction) => {
        acc[reaction.emoji] = acc[reaction.emoji] || {
          count: 0,
          isReacted: false,
        };
        acc[reaction.emoji].count++;
        if (reaction.userId === loggedInUserId) {
          acc[reaction.emoji].isReacted = true;
        }
        return acc;
      }, {});

      reactionsContainer.innerHTML = "";

      Object.entries(reactionCounts).forEach(([emoji, data]) => {
        const reaction = document.createElement("div");
        reaction.classList.add("reaction");
        reaction.setAttribute("data-emoji", emoji);
        if (data.isReacted) {
          reaction.classList.add("active");
        }
        reaction.innerHTML = `<span class="emoji">${emoji}</span> <span class="count">${data.count}</span>`;

        reaction.addEventListener("click", () => {
          handleReactionClick(messageId, reaction);
        });
        reactionsContainer.appendChild(reaction);
      }); // [新規リアクション追加] ボタン (今回は「👍」に限定)

      if (!reactionCounts["👍"]) {
        const newReactionBtn = document.createElement("div");
        newReactionBtn.classList.add("reaction", "add-reaction");
        newReactionBtn.setAttribute("data-emoji", "👍");
        newReactionBtn.innerHTML = `<span class="emoji">👍</span>`;

        newReactionBtn.addEventListener("click", () => {
          handleReactionClick(messageId, newReactionBtn);
        });
        reactionsContainer.appendChild(newReactionBtn);
      }
    }

    // グループチャット/DM履歴の初期リアクションをレンダリング
    renderReactions(initialReactions);
    // ⭐ MODIFIED: リアクションコンテナをDOMに追加
    message.appendChild(reactionsContainer);
  }

  message.appendChild(avatar);
  message.appendChild(bubble);

  chatArea.appendChild(message);

  scrollToBottom();
}

/* --------------------------------------------------------
  ★ グループメッセージ履歴の読み込み（リアクション情報に対応）
-------------------------------------------------------- */
function loadGroupHistory(groupId) {
  chatArea.innerHTML = "";

  fetch(`/api/group/history?groupId=${groupId}`)
    .then((res) => res.json())
    .then((messages) => {
      if (messages.length === 0) {
        chatArea.innerHTML = '<p class="no-message-guide">まだメッセージはありません。</p>';
        return;
      }

      messages.forEach((msg) => {
        const isSentByMe = msg.senderId === loggedInUserId;
        const displayName = msg.senderName || msg.senderId;

        addMessage(
          msg.body,
          isSentByMe,
          displayName,
          msg.messageId,
          msg.reactions || [],
          msg.createdAt,
          msg.expirationTime
        );
      });

      scrollToBottom();
    })
    .catch((err) => {
      chatArea.innerHTML = `<p class="error-message">読み込みエラー: ${err}</p>`;
    });
}

/* --------------------------------------------------------
  初期化処理
-------------------------------------------------------- */
document.addEventListener("DOMContentLoaded", () => {
  const userListItems = document.querySelectorAll(".user-list-item");
  const chatPartnerHeader = document.getElementById("chat-partner-name");

  userListItems.forEach((item) => {
    item.addEventListener("click", () => {
      const userId = item.getAttribute("data-user-id");
      const displayName = item.getAttribute("data-display-name");

      currentRecipientId = userId; // ⭐ currentGroupId をリセット
      currentGroupId = null;

      userListItems.forEach((i) => i.classList.remove("selected"));
      item.classList.add("selected");

      if (chatPartnerHeader) {
        chatPartnerHeader.textContent = displayName;
      }

      loadDmHistory(currentRecipientId);
    });
  }); // ============================= // ★ グループクリック処理の修正 // =============================

  const groupListItems = document.querySelectorAll(".group-list-item");

  groupListItems.forEach((item) => {
    item.addEventListener("click", () => {
      const groupId = item.getAttribute("data-group-id");
      const groupName = item.getAttribute("data-display-name"); // ⭐ currentGroupId を設定

      currentGroupId = groupId; // ⭐ currentRecipientId をリセット
      currentRecipientId = null; // 選択状態を切り替え

      document
        .querySelectorAll(".user-list-item, .group-list-item")
        .forEach((i) => i.classList.remove("selected"));
      item.classList.add("selected"); // 上部タイトル変更

      const chatPartnerHeader = document.getElementById("chat-partner-name");
      chatPartnerHeader.textContent = groupName; // グループ履歴表示

      loadGroupHistory(groupId);
    });
  }); // ... (sendBtn, chatInput のイベントリスナーは省略) ...

  if (sendBtn) {
    sendBtn.addEventListener("click", () => {
      sendMessageHandler(chatInput.value);
    });
  }

  if (chatInput) {
    chatInput.addEventListener("keypress", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        sendMessageHandler(chatInput.value);
      }
    });
  } // ====================================== // ▼ メニュータイトル（開閉ボタン）の開閉処理 (変更なし) // ======================================

  const menuTitles = document.querySelectorAll(".menu-title");

  menuTitles.forEach((title) => {
    title.addEventListener("click", () => {
      const nextList = title.nextElementSibling;

      if (!nextList || !nextList.classList.contains("menu-list")) return; // 開閉切り替え

      nextList.classList.toggle("collapsed");
      title.classList.toggle("collapsed"); // ▼ の向きを切り替え

      if (title.textContent.trim().startsWith("▼")) {
        title.textContent = title.textContent.replace("▼", "▶");
      } else {
        title.textContent = title.textContent.replace("▶", "▼");
      }
    });
  }); // ------------------------------ // ★ 送信ボタンの活性化（色変更）(変更なし) // ------------------------------

  if (chatInput && sendBtn) {
    chatInput.addEventListener("input", () => {
      if (chatInput.value.trim() !== "") {
        sendBtn.classList.add("active");
      } else {
        sendBtn.classList.remove("active");
      }
    });
  }

  // ======================================
  // ✅ MODIFIED: 期限設定ボタンのクリックイベントリスナー (権限チェック)
  // ======================================

  if (expirationBtn && expirationModal) {
    expirationBtn.addEventListener("click", () => {
      // ⭐ MODIFIED: ロールチェック。権限がない場合は alert して処理を確実に終了
      if (loggedInUserRole === "student") {
        alert("権限エラー: 期限付きメッセージは管理者または教師のみ設定可能です。");
        return;
      }

      // 権限がある場合（admin または teacher）はモーダルを表示する準備へ

      // 既存の値をモーダルにセット（任意）
      if (expirationTime) {
        const expiryDate = new Date(expirationTime);
        document.getElementById("expiryDate").value = expiryDate.toISOString().substring(0, 10);
        document.getElementById("expiryTime").value = expiryDate.toTimeString().substring(0, 5);
      } else {
        // デフォルトで空にしておく
        document.getElementById("expiryDate").value = "";
        document.getElementById("expiryTime").value = "";
      }
      expirationModal.show();
    });
  }

  // ======================================
  // ✅ NEW: カスタム日時設定ボタンのイベント処理
  // ======================================
  const setCustomExpirationBtn = document.getElementById("setCustomExpirationBtn");
  if (setCustomExpirationBtn) {
    setCustomExpirationBtn.addEventListener("click", () => {
      const dateInput = document.getElementById("expiryDate").value;
      const timeInput = document.getElementById("expiryTime").value;

      if (!dateInput || !timeInput) {
        alert("日付と時刻の両方を指定してください。");
        return;
      }

      // 日付と時刻を結合して Date オブジェクトを作成
      const combinedDateTimeString = `${dateInput}T${timeInput}:00`;
      const selectedDate = new Date(combinedDateTimeString);
      const now = new Date();

      if (selectedDate <= now) {
        alert("有効期限は現在時刻よりも未来の日時である必要があります。");
        return;
      }

      // サーバーに送信するために ISO 8601 形式に変換
      expirationTime = selectedDate.toISOString();
      expirationBtn.classList.add("active");

      alert(`有効期限を設定しました: ${selectedDate.toLocaleString("ja-JP")}`);

      expirationModal.hide(); // モーダルを閉じる
    });
  }
});
