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
});
