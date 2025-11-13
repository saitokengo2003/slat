"use strict";

// ログインユーザーIDをDOMから取得 (index.htmlのhidden inputを参照)
// ChatControllerでModelに追加された ${loggedInUserId} の値がHTMLに埋め込まれている前提
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

// ⭐ 選択された相手のIDを保持するグローバル変数
let currentRecipientId = null;

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
  chatArea.innerHTML = ""; // 既存のチャットエリアをクリア // ログインIDが取得できていない場合は処理を中断

  if (!loggedInUserId) {
    console.error("エラー: ログインユーザーIDが取得できませんでした。");
    chatArea.innerHTML = '<p class="error-message">ログイン状態を確認できません。</p>';
    return;
  } // APIを呼び出し

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
      } // メッセージリストを画面に表示

      messages.forEach((msg) => {
        // 送信者IDに基づいて、メッセージの表示スタイルを決定
        const isSentByMe = msg.senderId === loggedInUserId; // メッセージHTMLを生成

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
      }); // チャットエリアを最下部までスクロール

      chatArea.scrollTop = chatArea.scrollHeight;
    })
    .catch((error) => {
      console.error("履歴取得エラー:", error);
      chatArea.innerHTML = `<p class="error-message">メッセージ履歴の読み込み中にエラーが発生しました: ${error.message}</p>`;
    });
}

// ✅ メッセージ送信とDB保存を処理するメイン関数
async function sendMessageHandler(messageBody) {
  if (!messageBody.trim()) return; // ⭐ ログインIDチェック (main.jsの最初で処理済みだが、念のため)

  if (!loggedInUserId) {
    alert("ログイン状態が不正です。再ログインしてください。");
    return;
  } // ⭐ チャット相手が選択されているかチェック

  if (currentRecipientId === null) {
    alert("チャット相手を選択してください。");
    return;
  }

  const messageData = {
    recipientId: currentRecipientId, // 選択された相手のIDを送信
    body: messageBody,
  };

  try {
    const response = await fetch("/api/message/send", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(messageData),
    });

    if (response.ok && response.status === 200) {
      console.log(`Message successfully sent to ${currentRecipientId}.`);
      // 送信成功後、画面にメッセージを一時的に追加
      addMessage(messageBody, true);

      // 💡 必要に応じて、送信後に履歴を再読み込みすることで、DBのデータを元に画面を更新する
      // loadDmHistory(currentRecipientId);
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

// ✅ ドキュメントロード後の初期設定とイベントリスナー設定
document.addEventListener("DOMContentLoaded", () => {
  const userListItems = document.querySelectorAll(".user-list-item");
  const chatPartnerHeader = document.getElementById("chat-partner-name"); // ⭐ ユーザーリストアイテムのクリックイベントを設定

  userListItems.forEach((item) => {
    item.addEventListener("click", () => {
      // 1. 相手のIDと名前を取得
      const userId = item.getAttribute("data-user-id");
      const displayName = item.getAttribute("data-display-name"); // 2. グローバル変数に格納 (チャット相手を識別)

      currentRecipientId = userId; // 3. UIの更新: 選択状態のハイライト

      userListItems.forEach((i) => i.classList.remove("selected"));
      item.classList.add("selected"); // 4. UIの更新: チャット相手名をヘッダーに表示

      if (chatPartnerHeader) {
        chatPartnerHeader.textContent = displayName;
      } // 5. 履歴読み込み関数を呼び出す

      loadDmHistory(currentRecipientId);

      console.log(`Chat partner selected: ${displayName} (ID: ${userId})`);
    });
  }); // ✅ 送信ボタン

  const sendBtn = document.querySelector(".send-btn");
  const chatInput = document.querySelector(".chat-input");

  if (sendBtn) {
    sendBtn.addEventListener("click", () => {
      sendMessageHandler(chatInput.value);
    });
  } // ✅ Enterキー送信

  if (chatInput) {
    chatInput.addEventListener("keypress", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        sendMessageHandler(chatInput.value);
      }
    });
  }
});
