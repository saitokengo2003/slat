"use strict";

// ✅ サイドバー開閉
const sidebar = document.getElementById("sidebar");
const toggleBtn = document.getElementById("menu-toggle");

toggleBtn.addEventListener("click", () => {
  sidebar.classList.toggle("collapsed");
});

// ✅ 3点メニュー（…）開閉
document.querySelectorAll(".dropdown-btn").forEach((btn) => {
  btn.addEventListener("click", (e) => {
    e.stopPropagation();
    const dropdown = btn.closest(".dropdown");

    // 他のメニューを閉じる
    document.querySelectorAll(".dropdown").forEach((d) => {
      if (d !== dropdown) d.classList.remove("show");
    });

    dropdown.classList.toggle("show");
  });
});

// ✅ 画面クリックでメニュー閉じる
document.addEventListener("click", () => {
  document.querySelectorAll(".dropdown").forEach((d) => d.classList.remove("show"));
});
