package com.apexpay.network;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AiApiService {

    @POST("v1/chat/completions")
    Call<ChatResponse> getCompletion(
        @Header("Authorization") String auth,
        @Body ChatRequest request
    );

    class ChatRequest {
        public String model;
        public List<Message> messages;

        public ChatRequest(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }
    }

    class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    class ChatResponse {
        public List<Choice> choices;
    }

    class Choice {
        public Message message;
    }
}
