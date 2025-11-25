package com.sysdev.slat.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatRequestTest {

    @Test
    @DisplayName("正常系: 全フィールドのGetter/Setterが正しく動作すること")
    void testGettersAndSetters() {
        // 1. Ready
        ChatRequest request = new ChatRequest();

        // テストデータの定義
        String groupId = "group-abc-123";
        String senderId = "sender-001";
        String recipientId = "recipient-999";
        String body = "こんにちは、テストメッセージです。";
        // 日時データ
        OffsetDateTime expireTime = OffsetDateTime.now(ZoneOffset.UTC);

        // 2. Do
        request.setGroupId(groupId);
        request.setSenderId(senderId);
        request.setRecipientId(recipientId);
        request.setBody(body);
        request.setExpirationTime(expireTime);

        // 3. Assert
        assertEquals(groupId, request.getGroupId());
        assertEquals(senderId, request.getSenderId());
        assertEquals(recipientId, request.getRecipientId());
        assertEquals(body, request.getBody());
        assertEquals(expireTime, request.getExpirationTime());
    }

    @Test
    @DisplayName("境界値: Nullを設定してもエラーにならず、Nullが返されること")
    void testNullValues() {
        // 1. Ready
        ChatRequest request = new ChatRequest();

        // 2. Do
        request.setGroupId(null);
        request.setSenderId(null);
        request.setRecipientId(null);
        request.setBody(null);
        request.setExpirationTime(null);

        // 3. Assert
        assertNull(request.getGroupId());
        assertNull(request.getSenderId());
        assertNull(request.getRecipientId());
        assertNull(request.getBody());
        assertNull(request.getExpirationTime());
    }
}
