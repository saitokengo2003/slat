"use strict";

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

// 自動スクロール関数
const scrollToBottom = () => {
  chatArea.scrollTop = chatArea.scrollHeight;
};

// ✅ メッセージ追加関数
function addMessage(text, isRight = true) {
  if (!text.trim()) return;

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
  reaction.innerHTML = `<span class="emoji">😊</span> <span class="count">0</span>`;

  // ✅ リアクション増減処理
  let reacted = false; // クリック状態を保持
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

    // 押したアニメーション
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

// ✅ 送信ボタン
if (sendBtn) {
  sendBtn.addEventListener("click", () => {
    addMessage(chatInput.value, true); // 自分のメッセージは右側
  });
}

// ✅ Enterキー送信
if (chatInput) {
  chatInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      addMessage(chatInput.value, true);
    }
  });
}
