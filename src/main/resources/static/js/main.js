"use strict";

// ログインユーザーIDをDOMから取得
const loggedInUserId = document.getElementById("logged-in-user-id")
  ? document.getElementById("logged-in-user-id").value
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

// 選択中のチャット相手のID
let currentRecipientId = null;

// スクロール最下部へ
const scrollToBottom = () => {
  chatArea.scrollTop = chatArea.scrollHeight;
};

/* --------------------------------------------------------
  DM履歴の読み込み（addMessage を利用する形に変更）
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

        addMessage(msg.body, isSentByMe, displayName);
      });

      scrollToBottom();
    })
    .catch((error) => {
      console.error("履歴取得エラー:", error);
      chatArea.innerHTML = `<p class="error-message">メッセージ履歴の読み込み中にエラー: ${error.message}</p>`;
    });
}

/* --------------------------------------------------------
  メッセージ送信（DB保存）
-------------------------------------------------------- */
async function sendMessageHandler(messageBody) {
  if (!messageBody.trim()) return;

  if (!loggedInUserId) {
    alert("ログインエラー。再ログインしてください。");
    return;
  }
  if (!currentRecipientId) {
    alert("チャット相手を選択してください。");
    return;
  }

  const messageData = {
    recipientId: currentRecipientId,
    body: messageBody,
  };

  try {
    const response = await fetch("/api/message/send", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(messageData),
    });

    if (response.ok) {
      addMessage(messageBody, true, loggedInUserId);
      // メッセージ送信後：ボタン色を元に戻す
      sendBtn.classList.remove("active");
    } else {
      alert("送信失敗: " + (await response.text()));
    }
  } catch (err) {
    console.error("送信中のネットワークエラー:", err);
    alert("通信エラーが発生しました。");
  }
}

/* --------------------------------------------------------
  ★ 右側（自分）・左側（相手）表示対応 addMessage()
-------------------------------------------------------- */
function addMessage(text, isRight = true, displayName = "") {
  const message = document.createElement("div");
  message.classList.add("message");
  message.classList.add(isRight ? "right" : "left");

  // ★ アバター（頭文字）
  const avatar = document.createElement("div");
  avatar.classList.add("avatar");

  const initial = displayName ? displayName.trim().charAt(0).toUpperCase() : "?";

  avatar.textContent = initial;

  // ★ メッセージ吹き出し
  const bubble = document.createElement("div");
  bubble.classList.add("bubble");
  bubble.textContent = text;

  // ★ リアクション
  const reaction = document.createElement("div");
  reaction.classList.add("reaction");
  reaction.innerHTML = `<span class="emoji">😊</span> <span class="count">0</span>`;

  let reacted = false;
  reaction.addEventListener("click", () => {
    const countSpan = reaction.querySelector(".count");
    let count = parseInt(countSpan.textContent);

    if (!reacted) {
      count++;
      reaction.classList.add("active");
      reacted = true;
    } else {
      count--;
      reaction.classList.remove("active");
      reacted = false;
    }

    countSpan.textContent = count;

    reaction.classList.add("clicked");
    setTimeout(() => reaction.classList.remove("clicked"), 200);
  });

  message.appendChild(avatar);
  message.appendChild(bubble);
  message.appendChild(reaction);

  chatArea.appendChild(message);
  chatInput.value = "";

  scrollToBottom();
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

      userListItems.forEach((i) => i.classList.remove("selected"));
      item.classList.add("selected");

      if (chatPartnerHeader) {
        chatPartnerHeader.textContent = displayName;
      }

      loadDmHistory(currentRecipientId);
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

  // =============================
  // ★ グループクリック処理追加
  // =============================
  const groupListItems = document.querySelectorAll(".group-list-item");

  groupListItems.forEach((item) => {
    item.addEventListener("click", () => {
      const groupId = item.getAttribute("data-group-id");
      const groupName = item.getAttribute("data-display-name");

      currentRecipientId = "group-" + groupId; // ← ※DMと区別するため prefix

      // 選択状態を切り替え
      document
        .querySelectorAll(".user-list-item, .group-list-item")
        .forEach((i) => i.classList.remove("selected"));
      item.classList.add("selected");

      // 上部タイトル変更
      const chatPartnerHeader = document.getElementById("chat-partner-name");
      chatPartnerHeader.textContent = groupName;

      // グループ履歴表示
      loadGroupHistory(groupId);
    });
  });

  // ======================================
  // ▼ メニュータイトル（開閉ボタン）の開閉処理
  // ======================================
  const menuTitles = document.querySelectorAll(".menu-title");

  menuTitles.forEach((title) => {
    title.addEventListener("click", () => {
      const nextList = title.nextElementSibling;

      if (!nextList || !nextList.classList.contains("menu-list")) return;

      // 開閉切り替え
      nextList.classList.toggle("collapsed");
      title.classList.toggle("collapsed");

      // ▼ の向きを切り替え
      if (title.textContent.trim().startsWith("▼")) {
        title.textContent = title.textContent.replace("▼", "▶");
      } else {
        title.textContent = title.textContent.replace("▶", "▼");
      }
    });
  });

  // ------------------------------
  // ★ 送信ボタンの活性化（色変更）
  // ------------------------------
  if (chatInput && sendBtn) {
    chatInput.addEventListener("input", () => {
      if (chatInput.value.trim() !== "") {
        sendBtn.classList.add("active");
      } else {
        sendBtn.classList.remove("active");
      }
    });
  }
});

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

        addMessage(msg.body, isSentByMe, displayName);
      });
    })
    .catch((err) => {
      chatArea.innerHTML = `<p class="error-message">読み込みエラー: ${err}</p>`;
    });
}
