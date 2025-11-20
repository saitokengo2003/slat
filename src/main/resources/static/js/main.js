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

// 選択中のチャット相手のID (DM用)
let currentRecipientId = null;
// ⭐ 追加: 選択中のグループのID (グループチャット用)
let currentGroupId = null;

// スクロール最下部へ
const scrollToBottom = () => {
  chatArea.scrollTop = chatArea.scrollHeight;
};

/* --------------------------------------------------------
  DM履歴の読み込み（変更なし）
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
        const displayName = msg.senderName || msg.senderId; // DMは messageId/reactions は null のまま addMessage を呼び出す

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
    };
  } else if (currentRecipientId) {
    // DMの場合
    messageData = {
      recipientId: currentRecipientId,
      body: messageBody,
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
        addMessage(messageBody, true, loggedInUserId);
      } // メッセージ送信後：入力欄とボタン色を元に戻す
      chatInput.value = "";
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
   ⭐ リアクションAPIを呼び出し、画面を更新するハンドラ (グループチャット用)
-------------------------------------------------------- */
function handleReactionClick(messageId, reactionElement) {
  if (!messageId || !currentGroupId) {
    console.error("メッセージIDまたはグループIDがないためリアクションできません。", {
      messageId,
      currentGroupId,
    });
    return;
  } // リアクションボタンが持つ emoji データを取得

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
        // API成功後、画面を再読み込みして最新のリアクションを取得 (簡易的な方法)
        loadGroupHistory(currentGroupId);
      } else {
        console.error("リアクションのトグル失敗:", result);
      } // アニメーション (クリック感のフィードバック)

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
// ⭐ messageId, initialReactions を引数に追加
function addMessage(
  text,
  isRight = true,
  displayName = "",
  messageId = null,
  initialReactions = []
) {
  const message = document.createElement("div");
  message.classList.add("message");
  message.classList.add(isRight ? "right" : "left"); // ⭐ メッセージIDを data 属性として設定 (グループチャット用)

  if (messageId) {
    message.setAttribute("data-message-id", messageId);
  }

  const avatar = document.createElement("div");
  avatar.classList.add("avatar");

  const initial = displayName ? displayName.trim().charAt(0).toUpperCase() : "?";

  avatar.textContent = initial;

  const bubble = document.createElement("div");
  bubble.classList.add("bubble");
  bubble.textContent = text; // ⭐ リアクションの表示ロジック

  if (messageId) {
    // 1. グループチャット (機能するリアクション)
    const reactionsContainer = document.createElement("div");
    reactionsContainer.classList.add("reactions-container");

    function renderReactions(reactions) {
      const reactionCounts = reactions.reduce((acc, reaction) => {
        acc[reaction.emoji] = acc[reaction.emoji] || { count: 0, isReacted: false };
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

    // グループチャット履歴の初期リアクションをレンダリング
    renderReactions(initialReactions);
    message.appendChild(reactionsContainer); // ⭐ メッセージにコンテナを追加 (バブル外)
  } else {
    // 2. DM / 送信直後のメッセージ (見かけだけのリアクションを復元)
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

    message.appendChild(reaction); // ⭐ ダミーのリアクションをメッセージに追加
  }

  message.appendChild(avatar);
  message.appendChild(bubble);
  // ⭐ リアクションは既に上記の分岐内で message に追加済み

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
        const displayName = msg.senderName || msg.senderId; // ⭐ messageId と reactions を渡す

        addMessage(msg.body, isSentByMe, displayName, msg.messageId, msg.reactions || []);
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
});
