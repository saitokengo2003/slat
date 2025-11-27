"use strict";

/* ===============================
  ログイン情報取得
================================ */
const loggedInUserId = document.getElementById("logged-in-user-id")
  ? document.getElementById("logged-in-user-id").value
  : null;

const loggedInUserRole = document.getElementById("logged-in-user-role")
  ? document.getElementById("logged-in-user-role").value
  : null;

/* ===============================
  UI要素取得
================================ */
const sidebar = document.getElementById("sidebar");
const toggleBtn = document.getElementById("menu-toggle");
const sendBtn = document.querySelector(".send-btn");
const chatInput = document.querySelector(".chat-input");
const chatArea = document.querySelector(".chat-area");
const expirationBtn = document.querySelector(".expiration-btn");

const expirationModalElement = document.getElementById("expirationModal");
const expirationModal = expirationModalElement ? new bootstrap.Modal(expirationModalElement) : null;

const editMessageModalElement = document.getElementById("editMessageModal");
const editMessageModal = editMessageModalElement
  ? new bootstrap.Modal(editMessageModalElement)
  : null;
const editMessageIdInput = document.getElementById("edit-message-id");
const editMessageBodyTextarea = document.getElementById("edit-message-body");
const saveEditBtn = document.getElementById("saveEditBtn");

const unreactedAlertModalElement = document.getElementById("unreactedAlertModal");
const unreactedAlertModal = unreactedAlertModalElement
  ? new bootstrap.Modal(unreactedAlertModalElement)
  : null;

/* ===============================
  グローバル変数
================================ */
let currentRecipientId = null;
let currentGroupId = null;
let expirationTime = null;

/* ===============================
  スクロール
================================ */
const scrollToBottom = () => {
  chatArea.scrollTop = chatArea.scrollHeight;
};

/* ===============================
  サイドバー開閉
================================ */
if (toggleBtn && sidebar) {
  toggleBtn.addEventListener("click", () => {
    sidebar.classList.toggle("collapsed");
  });
}

/* ===============================
  DM履歴読み込み
================================ */
function loadDmHistory(recipientId) {
  chatArea.innerHTML = "";

  if (!loggedInUserId) {
    console.error("ログインID取得失敗");
    chatArea.innerHTML = '<p class="error-message">ログイン状態を確認できません。</p>';
    return;
  }

  fetch(`/api/dm/history?recipientId=${recipientId}`)
    .then((response) => response.json())
    .then((messages) => {
      if (messages.length === 0) {
        chatArea.innerHTML = '<p class="no-message-guide">まだメッセージはありません。</p>';
        return;
      }

      messages.forEach((msg) => {
        addMessage(
          msg.body,
          msg.senderId === loggedInUserId,
          msg.senderName || msg.senderId,
          msg.messageId,
          msg.reactions || [],
          msg.createdAt,
          msg.expirationTime,
          msg.nonReactingStudentNames
        );
      });

      scheduleMissedReactionAlert(messages);
      scrollToBottom();
    });
}

/* ===============================
  グループ履歴読み込み
================================ */
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
        addMessage(
          msg.body,
          msg.senderId === loggedInUserId,
          msg.senderName || msg.senderId,
          msg.messageId,
          msg.reactions || [],
          msg.createdAt,
          msg.expirationTime,
          msg.nonReactingStudentNames
        );
      });

      scheduleMissedReactionAlert(messages);
      scrollToBottom();
    });
}

/* ===============================
  メッセージ送信
================================ */
async function sendMessageHandler(messageBody) {
  if (!messageBody.trim()) return;

  let messageData = null;

  if (currentGroupId) {
    messageData = { groupId: currentGroupId, body: messageBody, expirationTime };
  } else if (currentRecipientId) {
    messageData = { recipientId: currentRecipientId, body: messageBody, expirationTime };
  } else {
    alert("宛先を選択してください。");
    return;
  }

  const res = await fetch("/api/message/send", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(messageData),
  });

  if (res.ok) {
    if (currentGroupId) loadGroupHistory(currentGroupId);
    else loadDmHistory(currentRecipientId);

    chatInput.value = "";
    sendBtn.classList.remove("active");
    expirationBtn.classList.remove("active");
    expirationTime = null;
  }
}

/* ===============================
  リアクション
================================ */
function handleReactionClick(messageId, reactionElement) {
  const emoji = reactionElement.getAttribute("data-emoji");

  fetch("/api/reaction/toggle", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ messageId, emoji }),
  })
    .then((res) => res.text())
    .then(() => {
      if (currentGroupId) loadGroupHistory(currentGroupId);
      else loadDmHistory(currentRecipientId);

      reactionElement.classList.add("clicked");
      setTimeout(() => reactionElement.classList.remove("clicked"), 200);
    });
}

/* ===============================================================
  addMessage（A構成 / wrapper方式 / 完全最新版）
=============================================================== */
function addMessage(
  text,
  isRight,
  displayName,
  messageId,
  initialReactions,
  createdAt,
  expirationTime,
  nonReactingStudentNames
) {
  const message = document.createElement("div");
  message.classList.add("message", isRight ? "right" : "left");

  if (messageId) message.dataset.messageId = messageId;

  /* アイコン */
  const avatar = document.createElement("div");
  avatar.classList.add("avatar");
  avatar.textContent = displayName?.trim()[0]?.toUpperCase() || "?";

  /* wrapper（ここに全て入れる） */
  const wrapper = document.createElement("div");
  wrapper.classList.add("bubble-wrapper");

  /* bubble */
  const bubble = document.createElement("div");
  bubble.classList.add("bubble");
  bubble.textContent = text;

  /* 期限ラベル */
  if (expirationTime) {
    const now = new Date();
    const expiryDate = new Date(expirationTime);
    const exp = document.createElement("div");
    exp.classList.add("expiration-label");

    if (now >= expiryDate) {
      message.classList.add("expired");
      if (!text.startsWith("[期限切れ]")) bubble.textContent = `[期限切れ] ${text}`;
      exp.textContent = `このメッセージは ${expiryDate.toLocaleString()} に期限切れ`;
    } else {
      exp.textContent = `有効期限: ${expiryDate.toLocaleString()} まで`;
    }

    wrapper.appendChild(exp);
  }

  wrapper.appendChild(bubble);

  /* ✎ / 🗑（A構成：bubbleのすぐ下） */
  if (isRight && messageId) {
    const menu = document.createElement("div");
    menu.classList.add("message-menu");

    const editBtn = document.createElement("button");
    editBtn.classList.add("btn", "btn-sm", "btn-light");
    editBtn.textContent = "✎";
    editBtn.onclick = () => openEditModal(messageId, text);

    const delBtn = document.createElement("button");
    delBtn.classList.add("btn", "btn-sm", "btn-light");
    delBtn.textContent = "🗑";
    delBtn.onclick = () => deleteMessageConfirm(messageId);

    menu.appendChild(editBtn);
    menu.appendChild(delBtn);
    wrapper.appendChild(menu);
  }

  /* 時刻（編集削除のすぐ下） */
  if (createdAt) {
    const time = document.createElement("div");
    time.classList.add("message-time");

    const d = new Date(createdAt);
    const date = d.toLocaleDateString("ja-JP");
    const t = d.toLocaleTimeString("ja-JP", { hour: "2-digit", minute: "2-digit" });

    time.textContent = `${date} ${t}`;
    wrapper.appendChild(time);
  }

  /* リアクション（時刻の下） */
  if (messageId) {
    const rc = document.createElement("div");
    rc.classList.add("reactions-container");

    const map = {};
    (initialReactions || []).forEach((r) => {
      if (!map[r.emoji]) map[r.emoji] = { count: 0, me: false };
      map[r.emoji].count++;
      if (r.userId === loggedInUserId) map[r.emoji].me = true;
    });

    Object.entries(map).forEach(([emoji, data]) => {
      const r = document.createElement("div");
      r.classList.add("reaction");
      if (data.me) r.classList.add("active");
      r.dataset.emoji = emoji;
      r.innerHTML = `<span class="emoji">${emoji}</span> <span class="count">${data.count}</span>`;
      r.onclick = () => handleReactionClick(messageId, r);
      rc.appendChild(r);
    });

    // 👍リアクションが存在するか確認
    const hasThumbsUp = Object.keys(map).includes("👍");

    // 👍がない時だけ追加ボタンを出す
    if (!hasThumbsUp) {
      const addBtn = document.createElement("div");
      addBtn.classList.add("reaction", "add-reaction");
      addBtn.dataset.emoji = "👍";
      addBtn.innerHTML = `<span class="emoji">👍</span>`;
      addBtn.onclick = () => handleReactionClick(messageId, addBtn);
      rc.appendChild(addBtn);
    }
    wrapper.appendChild(rc);
  }

  /* 未リアクション */
  if (expirationTime) {
    const now = new Date();
    const exp = new Date(expirationTime);

    if (now >= exp && nonReactingStudentNames?.length) {
      const nr = document.createElement("div");
      nr.classList.add("non-reacting-students");
      nr.innerHTML =
        `<div class="fw-bold text-danger">⚠️ 期限内にリアクションしなかった生徒:</div>` +
        nonReactingStudentNames.map((n) => `<small>${n}</small>`).join(" ");
      wrapper.appendChild(nr);
    }
  }

  /* DOM構造（左右反転対応） */
  if (isRight) {
    message.appendChild(wrapper);
    message.appendChild(avatar);
  } else {
    message.appendChild(avatar);
    message.appendChild(wrapper);
  }

  chatArea.appendChild(message);
  scrollToBottom();
}

/* ===============================
  編集モーダル
================================ */
function openEditModal(messageId, currentBody) {
  editMessageIdInput.value = messageId;
  editMessageBodyTextarea.value = currentBody.replace("[期限切れ] ", "");
  editMessageModal.show();
}

async function editMessageApi() {
  const id = editMessageIdInput.value;
  const body = editMessageBodyTextarea.value.trim();

  const res = await fetch("/api/message/edit", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ messageId: id, body }),
  });

  if ((await res.text()) === "SUCCESS") {
    editMessageModal.hide();
    if (currentGroupId) loadGroupHistory(currentGroupId);
    else loadDmHistory(currentRecipientId);
  }
}

/* ===============================
  削除
================================ */
function deleteMessageConfirm(id) {
  if (confirm("削除しますか？")) deleteMessageApi(id);
}

async function deleteMessageApi(id) {
  const res = await fetch("/api/message/delete", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ messageId: id }),
  });

  if ((await res.text()) === "SUCCESS") {
    if (currentGroupId) loadGroupHistory(currentGroupId);
    else loadDmHistory(currentRecipientId);
  }
}

/* ===============================
  未リアクション警告
================================ */
function populateUnreactedMessageList(messages) {
  const listEl = document.getElementById("unreacted-message-list");
  listEl.innerHTML = "";

  messages.forEach((msg) => {
    const li = document.createElement("li");
    li.classList.add("list-group-item");
    li.textContent = msg.body;
    li.onclick = () => {
      const target = document.querySelector(`.message[data-message-id="${msg.messageId}"]`);
      if (target) {
        target.scrollIntoView({ behavior: "smooth", block: "center" });
        target.classList.add("highlight-msg");
        setTimeout(() => target.classList.remove("highlight-msg"), 1500);
      }
    };
    listEl.appendChild(li);
  });
}

function scheduleMissedReactionAlert(messages) {
  if (loggedInUserRole !== "student") return;

  const now = new Date();
  const missedNow = [];

  messages.forEach((msg) => {
    if (msg.senderId === loggedInUserId) return;
    if (!msg.expirationTime) return;

    const exp = new Date(msg.expirationTime);
    const reacted = msg.reactions?.some((r) => r.userId === loggedInUserId);

    if (reacted) return;

    if (now >= exp) {
      missedNow.push(msg);
    }
  });

  if (missedNow.length) {
    populateUnreactedMessageList(missedNow);
    unreactedAlertModal.show();
  }
}

/* ===============================
  イベント登録
================================ */
document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".user-list-item").forEach((item) => {
    item.onclick = () => {
      currentRecipientId = item.dataset.userId;
      currentGroupId = null;

      document
        .querySelectorAll(".user-list-item,.group-list-item")
        .forEach((i) => i.classList.remove("selected"));
      item.classList.add("selected");

      document.getElementById("chat-partner-name").textContent = item.dataset.displayName;

      loadDmHistory(currentRecipientId);
    };

    /* ===============================
  サイドバー メニュー開閉（▼ / ▶）
================================ */
    const menuTitles = document.querySelectorAll(".menu-title");

    menuTitles.forEach((title) => {
      title.addEventListener("click", () => {
        const nextList = title.nextElementSibling;

        if (!nextList || !nextList.classList.contains("menu-list")) return;

        nextList.classList.toggle("collapsed");
        title.classList.toggle("collapsed");

        // ▼ と ▶ の切り替え
        if (title.textContent.trim().startsWith("▼")) {
          title.textContent = title.textContent.replace("▼", "▶");
        } else {
          title.textContent = title.textContent.replace("▶", "▼");
        }
      });
    });
  });

  document.querySelectorAll(".group-list-item").forEach((item) => {
    item.onclick = () => {
      currentGroupId = item.dataset.groupId;
      currentRecipientId = null;

      document
        .querySelectorAll(".user-list-item,.group-list-item")
        .forEach((i) => i.classList.remove("selected"));
      item.classList.add("selected");

      document.getElementById("chat-partner-name").textContent = item.dataset.displayName;

      loadGroupHistory(currentGroupId);
    };
  });

  sendBtn.onclick = () => sendMessageHandler(chatInput.value);
  chatInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") sendMessageHandler(chatInput.value);
  });

  chatInput.oninput = () =>
    chatInput.value.trim() ? sendBtn.classList.add("active") : sendBtn.classList.remove("active");

  saveEditBtn.onclick = editMessageApi;
});

/* ===============================
  有効期限ボタン（時計ボタン）の復活
================================ */
if (expirationBtn && expirationModal) {
  expirationBtn.addEventListener("click", () => {
    // 学生は使えない
    if (loggedInUserRole === "student") {
      alert("権限エラー: 期限付きメッセージは管理者または講師のみ設定可能です。");
      return;
    }

    // 既に設定済みの場合は値をセット
    if (expirationTime) {
      const exp = new Date(expirationTime);
      document.getElementById("expiryDate").value = exp.toISOString().substring(0, 10);
      document.getElementById("expiryTime").value = exp.toTimeString().substring(0, 5);
    } else {
      document.getElementById("expiryDate").value = "";
      document.getElementById("expiryTime").value = "";
    }

    expirationModal.show();
  });
}

/* ===============================
  カスタム期限設定ボタン
================================ */
const setCustomExpirationBtn = document.getElementById("setCustomExpirationBtn");

if (setCustomExpirationBtn) {
  setCustomExpirationBtn.addEventListener("click", () => {
    const dateInput = document.getElementById("expiryDate").value;
    const timeInput = document.getElementById("expiryTime").value;

    if (!dateInput || !timeInput) {
      alert("日付と時刻の両方を指定してください。");
      return;
    }

    const selected = new Date(`${dateInput}T${timeInput}:00`);
    const now = new Date();

    if (selected <= now) {
      alert("有効期限は現在より未来を指定してください。");
      return;
    }

    expirationTime = selected.toISOString();
    expirationBtn.classList.add("active");

    alert(`有効期限を設定しました: ${selected.toLocaleString("ja-JP")}`);

    expirationModal.hide();
  });
}
