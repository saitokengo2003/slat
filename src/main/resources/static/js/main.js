// Updated main.js with scroll-to-message feature.
// ... (Due to character limits please paste your existing main.js here and indicate the location where to insert the scroll logic.)
"use strict";

// ログインユーザーIDをDOMから取得
const loggedInUserId = document.getElementById("logged-in-user-id")
  ? document.getElementById("logged-in-user-id").value
  : null;

// ログインユーザーのロールをDOMから取得
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

// モーダル要素の取得と初期化
const expirationModalElement = document.getElementById("expirationModal");
const expirationModal = expirationModalElement ? new bootstrap.Modal(expirationModalElement) : null;

//  編集モーダル関連の変数
const editMessageModalElement = document.getElementById("editMessageModal");
const editMessageModal = editMessageModalElement
  ? new bootstrap.Modal(editMessageModalElement)
  : null;
const editMessageIdInput = document.getElementById("edit-message-id");
const editMessageBodyTextarea = document.getElementById("edit-message-body");
const saveEditBtn = document.getElementById("saveEditBtn");

// 未リアクション警告モーダル
const unreactedAlertModalElement = document.getElementById("unreactedAlertModal");
const unreactedAlertModal = unreactedAlertModalElement
  ? new bootstrap.Modal(unreactedAlertModalElement)
  : null;

// 選択中のチャット相手のID (DM用)
let currentRecipientId = null;
// 追加: 選択中のグループのID (グループチャット用)
let currentGroupId = null;
// 追加: 期限付きメッセージの有効期限
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
          msg.expirationTime,
          msg.nonReactingStudentNames
        );
      });

      // 期限切れ & 未リアクションメッセージのチェック
      scheduleMissedReactionAlert(messages);
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
    // グループチャットの場合
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

      // 期限設定をリセットし、ボタンの色を戻す
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
  リアクションAPIを呼び出し、画面を更新するハンドラ (グループチャット/DM兼用)
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
        alert("リアクション処理失敗: " + result); // 失敗時もユーザーに通知
        console.error("リアクションのトグル失敗:", result);
      }

      // アニメーションはそのまま実行
      reactionElement.classList.add("clicked");
      setTimeout(() => reactionElement.classList.remove("clicked"), 200);
    })
    .catch((error) => {
      console.error("リアクションのトグル失敗:", error);
      alert("通信エラーが発生しました。");
    });
}

/* --------------------------------------------------------
  右側（自分）・左側（相手）表示対応 addMessage() の修正
-------------------------------------------------------- */
// expirationTime, nonReactingStudentNames を引数に追加
function addMessage(
  text,
  isRight = true,
  displayName = "",
  messageId = null,
  initialReactions = [],
  createdAt = null,
  expirationTime = null,
  nonReactingStudentNames = []
) {
  const message = document.createElement("div");
  message.classList.add("message");
  message.classList.add(isRight ? "right" : "left");

  // --- 期限ラベル ---
  if (expirationTime) {
    const now = new Date();
    const expiryDate = new Date(expirationTime);

    if (now >= expiryDate) {
      message.classList.add("expired");
      if (!text.startsWith("[期限切れ] ")) {
        text = `[期限切れ] ${text}`;
      }
    }

    const labelElement = document.createElement("div");
    labelElement.classList.add("expiration-label");

    const expiryString = expiryDate.toLocaleString("ja-JP", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });

    if (now < expiryDate) {
      labelElement.textContent = `有効期限: ${expiryString}まで`;
    } else {
      labelElement.textContent = `このメッセージは ${expiryString} に期限切れとなりました`;
    }

    message.appendChild(labelElement);
  }

  if (messageId) {
    message.setAttribute("data-message-id", messageId);
  }

  // --- アイコン ---
  const avatar = document.createElement("div");
  avatar.classList.add("avatar");
  const initial = displayName ? displayName.trim().charAt(0).toUpperCase() : "?";
  avatar.textContent = initial;

  // --- バブルまわりのラッパ ---
  const wrapper = document.createElement("div");
  wrapper.classList.add("bubble-wrapper");

  // --- 吹き出し ---
  const bubble = document.createElement("div");
  bubble.classList.add("bubble");
  bubble.textContent = text;
  wrapper.appendChild(bubble);

  // --- 時刻 ---
  if (createdAt) {
    const timeElement = document.createElement("div");
    timeElement.classList.add("message-time");

    const date = new Date(createdAt);

    const dateString = date.toLocaleDateString("ja-JP", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });

    const timeString = date.toLocaleTimeString("ja-JP", {
      hour: "2-digit",
      minute: "2-digit",
    });

    timeElement.textContent = `${dateString} ${timeString}`;
    wrapper.appendChild(timeElement);
  }

  // --- リアクション ---
  if (messageId) {
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
      });

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

    renderReactions(initialReactions);
    wrapper.appendChild(reactionsContainer);
  }

  // --- 未リアクション生徒名 ---
  if (expirationTime) {
    const now = new Date();
    const expiryDate = new Date(expirationTime);

    if (now >= expiryDate && nonReactingStudentNames && nonReactingStudentNames.length > 0) {
      const nonReactingDiv = document.createElement("div");
      nonReactingDiv.classList.add(
        "non-reacting-students",
        "mt-1",
        "p-1",
        "border-top",
        "border-warning",
        "bg-light"
      );

      const title = document.createElement("small");
      title.classList.add("d-block", "fw-bold", "text-danger");
      title.textContent = "⚠️ 期限内にリアクションしなかった生徒:";
      nonReactingDiv.appendChild(title);

      const nameList = nonReactingStudentNames
        .map((name) => `<small class="d-inline-block me-2">${name}</small>`)
        .join("");

      nonReactingDiv.innerHTML += nameList;

      wrapper.appendChild(nonReactingDiv);
    }
  }

  if (isRight) {
    // 自分のメッセージ → バブル → アイコン
    message.appendChild(wrapper);
    message.appendChild(avatar);
  } else {
    // 相手のメッセージ → アイコン → バブル
    message.appendChild(avatar);
    message.appendChild(wrapper);
  }

  chatArea.appendChild(message);
  scrollToBottom();
}

/* --------------------------------------------------------
  未リアクションメッセージ一覧をモーダルに描画するヘルパー
-------------------------------------------------------- */
function populateUnreactedMessageList(messages) {
  const listEl = document.getElementById("unreacted-message-list");
  if (!listEl) return;

  listEl.innerHTML = "";

  messages.forEach((msg) => {
    const li = document.createElement("li");
    li.classList.add("list-group-item");
    li.textContent = msg.body || "(本文なし)";

    // クリックで対象メッセージへスクロール＆ハイライト（任意）
    li.style.cursor = "pointer";
    li.addEventListener("click", () => {
      const target = document.querySelector(`.message[data-message-id="${msg.messageId}"]`);
      if (target) {
        target.scrollIntoView({ behavior: "smooth", block: "center" });
        target.classList.add("highlight-msg");
        setTimeout(() => target.classList.remove("highlight-msg"), 1500);
      }
    });

    listEl.appendChild(li);
  });
}

/* --------------------------------------------------------
  有効期限までにリアクションしていないメッセージを検知してモーダル表示
-------------------------------------------------------- */
function scheduleMissedReactionAlert(messages) {
  if (!unreactedAlertModal || !loggedInUserId) return;

  // 生徒のみ対象
  if (loggedInUserRole !== "student") return;

  const now = new Date();
  const missedNow = [];

  messages.forEach((msg) => {
    if (msg.senderId === loggedInUserId) return;
    if (!msg.expirationTime) return;

    const expiryDate = new Date(msg.expirationTime);

    const userHasReacted = (msg.reactions || []).some((r) => r.userId === loggedInUserId);
    if (userHasReacted) return;

    // すでに期限切れのもの → 一覧表示
    if (now >= expiryDate) {
      missedNow.push(msg);
      return;
    }

    // これから期限切れ → 期限到達時に再チェック
    const msUntilExpiry = expiryDate.getTime() - now.getTime();

    setTimeout(() => {
      // → 再チェック（今リアクションした可能性があるため）
      fetch(`/api/message/one?id=${msg.messageId}`)
        .then((res) => res.json())
        .then((latest) => {
          const reacted = (latest.reactions || []).some((r) => r.userId === loggedInUserId);
          if (!reacted) {
            populateUnreactedMessageList([latest]);
            unreactedAlertModal.show();
          }
        });
    }, msUntilExpiry);
  });

  // すでに期限切れのものを一括表示（毎回）
  if (missedNow.length > 0) {
    populateUnreactedMessageList(missedNow);
    unreactedAlertModal.show();
  }
}

/* --------------------------------------------------------
  グループメッセージ履歴の読み込み（リアクション情報に対応）
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
          msg.expirationTime,
          msg.nonReactingStudentNames
        );
      });

      // 期限切れ & 未リアクションメッセージのチェック
      scheduleMissedReactionAlert(messages);
      scrollToBottom();
    })
    .catch((err) => {
      chatArea.innerHTML = `<p class="error-message">読み込みエラー: ${err}</p>`;
    });
}

/* --------------------------------------------------------
  編集モーダルを開く
-------------------------------------------------------- */
function openEditModal(messageId, currentBody) {
  if (!editMessageModal) return;

  // 1. メッセージIDと現在の本文をモーダルにセット
  editMessageIdInput.value = messageId;
  // [期限切れ] ラベルが本文についている場合は除去してセット
  editMessageBodyTextarea.value = currentBody.replace("[期限切れ] ", "");

  // 2. モーダルを表示
  editMessageModal.show();
}

/* --------------------------------------------------------
  編集API呼び出し
-------------------------------------------------------- */
async function editMessageApi() {
  const messageId = editMessageIdInput.value;
  const newBody = editMessageBodyTextarea.value.trim();

  if (!messageId || !newBody) {
    alert("メッセージIDまたは本文が不足しています。");
    return;
  }

  try {
    const response = await fetch("/api/message/edit", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ messageId: messageId, body: newBody }),
    });

    const result = await response.text();
    if (result === "SUCCESS") {
      editMessageModal.hide();
      if (currentGroupId) {
        loadGroupHistory(currentGroupId);
      } else if (currentRecipientId) {
        loadDmHistory(currentRecipientId);
      }
    } else {
      alert("編集失敗: " + result);
    }
  } catch (e) {
    console.error("編集APIエラー:", e);
    alert("編集中にエラーが発生しました。");
  }
}

/* --------------------------------------------------------
  削除確認
-------------------------------------------------------- */
function deleteMessageConfirm(messageId) {
  if (confirm("このメッセージを完全に削除しますか？ (復元できません)")) {
    deleteMessageApi(messageId);
  }
}

/* --------------------------------------------------------
  削除API呼び出し
-------------------------------------------------------- */
async function deleteMessageApi(messageId) {
  try {
    const response = await fetch("/api/message/delete", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ messageId: messageId }),
    });

    const result = await response.text();
    if (result === "SUCCESS") {
      if (currentGroupId) {
        loadGroupHistory(currentGroupId);
      } else if (currentRecipientId) {
        loadDmHistory(currentRecipientId);
      }
    } else {
      alert("削除失敗: " + result);
    }
  } catch (e) {
    console.error("削除APIエラー:", e);
    alert("削除中にエラーが発生しました。");
  }
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

      currentRecipientId = userId;
      currentGroupId = null;

      userListItems.forEach((i) => i.classList.remove("selected"));
      item.classList.add("selected");

      if (chatPartnerHeader) {
        chatPartnerHeader.textContent = displayName;
      }

      loadDmHistory(currentRecipientId);
    });
  });

  const groupListItems = document.querySelectorAll(".group-list-item");

  groupListItems.forEach((item) => {
    item.addEventListener("click", () => {
      const groupId = item.getAttribute("data-group-id");
      const groupName = item.getAttribute("data-display-name");

      currentGroupId = groupId;
      currentRecipientId = null;

      document
        .querySelectorAll(".user-list-item, .group-list-item")
        .forEach((i) => i.classList.remove("selected"));
      item.classList.add("selected");

      const chatPartnerHeader = document.getElementById("chat-partner-name");
      chatPartnerHeader.textContent = groupName;

      loadGroupHistory(groupId);
    });
  });

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
  }

  const menuTitles = document.querySelectorAll(".menu-title");

  menuTitles.forEach((title) => {
    title.addEventListener("click", () => {
      const nextList = title.nextElementSibling;

      if (!nextList || !nextList.classList.contains("menu-list")) return;

      nextList.classList.toggle("collapsed");
      title.classList.toggle("collapsed");

      if (title.textContent.trim().startsWith("▼")) {
        title.textContent = title.textContent.replace("▼", "▶");
      } else {
        title.textContent = title.textContent.replace("▶", "▼");
      }
    });
  });

  if (chatInput && sendBtn) {
    chatInput.addEventListener("input", () => {
      if (chatInput.value.trim() !== "") {
        sendBtn.classList.add("active");
      } else {
        sendBtn.classList.remove("active");
      }
    });
  }

  // 編集保存ボタンのイベント処理
  if (saveEditBtn) {
    saveEditBtn.addEventListener("click", editMessageApi);
  }

  // ======================================
  // 期限設定ボタンのクリックイベントリスナー (権限チェック)
  // ======================================

  if (expirationBtn && expirationModal) {
    expirationBtn.addEventListener("click", () => {
      if (loggedInUserRole === "student") {
        alert("権限エラー: 期限付きメッセージは管理者または講師のみ設定可能です。");
        return;
      }

      if (expirationTime) {
        document.getElementById("expiryDate").value = expiryDate.toISOString().substring(0, 10);
        document.getElementById("expiryTime").value = expiryDate.toTimeString().substring(0, 5);
      } else {
        document.getElementById("expiryDate").value = "";
        document.getElementById("expiryTime").value = "";
      }
      expirationModal.show();
    });
  }

  // ======================================
  // カスタム日時設定ボタンのイベント処理
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

      const combinedDateTimeString = `${dateInput}T${timeInput}:00`;
      const selectedDate = new Date(combinedDateTimeString);
      const now = new Date();

      if (selectedDate <= now) {
        alert("有効期限は現在時刻よりも未来の日時である必要があります。");
        return;
      }

      expirationTime = selectedDate.toISOString();
      expirationBtn.classList.add("active");

      alert(`有効期限を設定しました: ${selectedDate.toLocaleString("ja-JP")}`);

      expirationModal.hide();
    });
  }
});
