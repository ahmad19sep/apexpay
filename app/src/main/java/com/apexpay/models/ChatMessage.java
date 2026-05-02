package com.apexpay.models;

public class ChatMessage {
    public static final String ROLE_USER = "user";
    public static final String ROLE_AI   = "assistant";

    public long   id;
    public String role;
    public String content;
    public long   timestamp;

    public ChatMessage(String role, String content) {
        this.role      = role;
        this.content   = content;
        this.timestamp = System.currentTimeMillis();
    }
}
