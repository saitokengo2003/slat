"use strict";

// ログインユーザーIDをDOMから取得 (index.htmlのhidden inputを参照)
const loggedInUserId = document.getElementById("logged-in-user-id")
  ? document.getElementById("logged-in-user-id").value
  : null; // 取得できない場合のフォールバック

// ✅ サイドバー開閉
const sidebar = document.getElementById("sidebar");
const toggleBtn = document.getElementById("menu-toggle");

if (toggleBtn) {
  toggleBtn.addEventListener("click", () => {
    sidebar.classList.toggle("collapsed");
  });
}

// ✅ チャット送信関連
const sendBtn = document.querySelector(".send-btn");
const chatInput = document.querySelector(".chat-input");
const chatArea = document.querySelector(".chat-area");

// ⭐ 選択された相手/グループのIDを保持するグローバル変数
let currentRecipientId = null;
let currentGroupId = null; // ✅ グループIDを保持する変数を追加

// 自動スクロール関数
const scrollToBottom = () => {
  chatArea.scrollTop = chatArea.scrollHeight;
};

/**
 * DMメッセージ履歴を取得し、チャットエリアに表示します。
 * @param {string} recipientId チャット相手のID
 */
function loadDmHistory(recipientId) {
  const chatArea = document.querySelector(".chat-area");
  chatArea.innerHTML = ""; // 既存のチャットエリアをクリア

  if (!loggedInUserId) {
    console.error("エラー: ログインユーザーIDが取得できませんでした。");
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
      }

      messages.forEach((msg) => {
        const isSentByMe = msg.senderId === loggedInUserId;

        const messageHtml = `
                    <div class="message-container ${
          isSentByMe ? "my-message-container" : "other-message-container"
        }">
                        <div class="message ${isSentByMe ? "my-message" : "other-message"}">
                            <div class="message-content">
                                <span class="sender-name">${msg.senderId}</span>
                                <p class="body">${msg.body}</p>
                                <span class="timestamp">${new Date(
          msg.createdAt
        ).toLocaleTimeString()}</span>
                            </div>
                        </div>
                    </div>
                `;
        chatArea.innerHTML += messageHtml;
      });

      chatArea.scrollTop = chatArea.scrollHeight;
    })
    .catch((error) => {
      console.error("履歴取得エラー:", error);
      chatArea.innerHTML = `<p class="error-message">メッセージ履歴の読み込み中にエラーが発生しました: ${error.message}</p>`;
    });
}

// ⭐ ✅ 変更箇所: loadGroupHistoryをAPI呼び出し実装に置き換え
function loadGroupHistory(groupId, groupName) {
  const chatArea = document.querySelector(".chat-area");
  const chatPartnerHeader = document.getElementById("chat-partner-name");

  currentGroupId = groupId; // グループIDを設定
  currentRecipientId = null; // DM相手IDをクリア

  chatPartnerHeader.textContent = groupName;
  chatArea.innerHTML = '<p class="guide-text">グループ履歴を読み込み中...</p>'; // ローディング表示

  fetch(`/api/group/history?groupId=${groupId}`)
    .then((response) => {
      if (!response.ok) throw new Error("グループ履歴の取得に失敗しました");

      return response.json();
    })
    .then((history) => {
      chatArea.innerHTML = ""; // クリア

      if (history.length === 0) {
        chatArea.innerHTML = '<p class="no-message-guide">まだメッセージはありません。</p>';
        return;
      }

      history.forEach((msg) => {
        const isSentByMe = msg.senderId === loggedInUserId;
        const messageHtml = `
                    <div class="message-container ${
                      isSentByMe ? "my-message-container" : "other-message-container"
                    }">
                        <div class="message ${isSentByMe ? "my-message" : "other-message"}">
                            <div class="message-content">
                                <span class="sender-name">${msg.senderId}</span>
                                <p class="body">${msg.body}</p>
                                <span class="timestamp">${new Date(
                                  msg.createdAt
                                ).toLocaleTimeString()}</span>
                            </div>
                        </div>
                    </div>
                `;
        chatArea.innerHTML += messageHtml;
      });

      chatArea.scrollTop = chatArea.scrollHeight;
    })
    .catch((error) => {
      console.error("グループ履歴エラー:", error);
      chatArea.innerHTML = '<p class="guide-text error">グループ履歴の読み込みに失敗しました。</p>';
    });
}

// ✅ メッセージ送信とDB保存を処理するメイン関数
async function sendMessageHandler(messageBody) {
  if (!messageBody.trim()) return;

  if (!loggedInUserId) {
    alert("ログイン状態が不正です。再ログインしてください。");
    return;
  } // ⭐ 送信先 (DMかグループ) が選択されているかチェック

  if (currentRecipientId === null && currentGroupId === null) {
    alert("チャット相手またはグループを選択してください。");
    return;
  }

  let requestBody = { body: messageBody }; // ⭐ 送信先 ID の振り分けロジック: Group IDを優先

  if (currentGroupId) {
    requestBody.groupId = currentGroupId;
  } else if (currentRecipientId) {
    requestBody.recipientId = currentRecipientId;
  } // ChatControllerがsenderIdをセッションから設定するため、ここでは bodyとIDのみ送信 // (ログインIDはChatControllerでセットされるため、requestBody.senderIdは不要)

  try {
    const response = await fetch("/api/message/send", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(requestBody),
    });

    if (response.ok && response.status === 200) {
      console.log(`Message successfully sent to ${currentRecipientId || currentGroupId}.`);
      addMessage(messageBody, true); // 画面に追加 // 送信後、履歴を再読み込み

      if (currentGroupId) {
        loadGroupHistory(currentGroupId, "現在のグループ"); // ✅ loadGroupHistoryが正しく動作する
      } else if (currentRecipientId) {
        loadDmHistory(currentRecipientId);
      }
    } else {
      const errorText = await response.text();
      console.error("Failed to send message. Server responded:", errorText);
      alert("メッセージの送信と保存に失敗しました。詳細: " + errorText);
    }
  } catch (error) {
    console.error("Network error during message send:", error);
    alert("通信エラーが発生しました。");
  }
}

// ✅ メッセージ追加関数 (画面表示用)
function addMessage(text, isRight = true) {
  const message = document.createElement("div");
  message.classList.add("message");
  message.classList.add(isRight ? "right" : "left");

  const avatar = document.createElement("div");
  avatar.classList.add("avatar");
  avatar.textContent = isRight ? "R" : "T";

  const bubble = document.createElement("div");
  bubble.classList.add("bubble");
  bubble.textContent = text;

  const reaction = document.createElement("div");
  reaction.classList.add("reaction");
  reaction.innerHTML = `<span class="emoji">😊</span> <span class="count">0</span>`; // ✅ リアクション増減処理

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

    countSpan.textContent = count; // 押したアニメーション

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

// ----------------------------------------------------------------------
// ⭐ 【追加】入力フィールドの状態を監視し、ボタンを活性化する関数
// ----------------------------------------------------------------------
function toggleSendButtonState() {
  const hasText = chatInput.value.trim().length > 0;

  if (hasText) {
    sendBtn.classList.add("active");
    sendBtn.removeAttribute("disabled");
  } else {
    sendBtn.classList.remove("active");
    sendBtn.setAttribute("disabled", "true"); // テキストがない場合は無効化
  }
}

// ✅ ドキュメントロード後の初期設定とイベントリスナー設定
document.addEventListener("DOMContentLoaded", () => {
  const userListItems = document.querySelectorAll(".user-list-item");
  const groupListItems = document.querySelectorAll(".group-list-item"); // ✅ グループリストを取得
  const chatPartnerHeader = document.getElementById("chat-partner-name"); // ⭐ ページロード時の初期状態を設定し、inputイベントを監視

  toggleSendButtonState();
  if (chatInput) {
    chatInput.addEventListener("input", toggleSendButtonState);
  } // ⭐ 1. DMユーザーリストアイテムのクリックイベントを設定

  userListItems.forEach((item) => {
    item.addEventListener("click", () => {
      const userId = item.getAttribute("data-user-id");
      const displayName = item.getAttribute("data-display-name"); // 状態更新: DM相手を設定し、グループをクリア

      currentRecipientId = userId;
      currentGroupId = null; // UIの更新

      document.querySelectorAll(".menu-list li").forEach((i) => i.classList.remove("selected"));
      item.classList.add("selected");
      if (chatPartnerHeader) {
        chatPartnerHeader.textContent = displayName;
      } // 履歴読み込み関数を呼び出す

      loadDmHistory(currentRecipientId);
      chatInput.value = "";
      toggleSendButtonState();

      console.log(`DM partner selected: ${displayName} (ID: ${userId})`);
    });
  }); // ⭐ 2. グループリストアイテムのクリックイベントを設定

  groupListItems.forEach((item) => {
    item.addEventListener("click", () => {
      const groupId = item.getAttribute("data-group-id");
      const groupName = item.getAttribute("data-display-name"); // 状態更新: グループを設定し、DM相手をクリア

      currentGroupId = groupId;
      currentRecipientId = null; // UIの更新

      document.querySelectorAll(".menu-list li").forEach((i) => i.classList.remove("selected"));
      item.classList.add("selected");
      if (chatPartnerHeader) {
        chatPartnerHeader.textContent = groupName;
      } // 履歴読み込み関数を呼び出す

      loadGroupHistory(currentGroupId, groupName); // ✅ 修正された関数を呼び出し
      chatInput.value = "";
      toggleSendButtonState();

      console.log(`Group selected: ${groupName} (ID: ${groupId})`);
    });
  }); // ✅ 送信ボタン

  if (sendBtn) {
    sendBtn.addEventListener("click", () => {
      if (chatInput.value.trim().length === 0) return;
      sendMessageHandler(chatInput.value);
    });
  } // ✅ Enterキー送信

  if (chatInput) {
    chatInput.addEventListener("keypress", (e) => {
      if (e.key === "Enter") {
        if (chatInput.value.trim().length === 0) return;
        e.preventDefault();
        sendMessageHandler(chatInput.value);
      }
    });
  }
});
