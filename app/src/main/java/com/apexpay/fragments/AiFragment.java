package com.apexpay.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apexpay.Config;
import com.apexpay.R;
import com.apexpay.adapters.ChatAdapter;
import com.apexpay.database.DatabaseHelper;
import com.apexpay.models.Asset;
import com.apexpay.models.ChatMessage;
import com.apexpay.models.Holding;
import com.apexpay.network.AiApiService;
import com.apexpay.network.NetworkClient;
import com.apexpay.services.MarketDataService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiFragment extends Fragment {

    private RecyclerView  rvChat;
    private ChatAdapter   chatAdapter;
    private EditText      etInput;
    private ImageButton   btnSend;
    private LinearLayout  llTyping;
    private DatabaseHelper db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = new DatabaseHelper(requireContext());

        rvChat   = view.findViewById(R.id.rvChat);
        etInput  = view.findViewById(R.id.etAiInput);
        btnSend  = view.findViewById(R.id.btnAiSend);
        llTyping = view.findViewById(R.id.llTyping);

        chatAdapter = new ChatAdapter();
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(chatAdapter);

        // Load history from SQLite
        chatAdapter.setMessages(db.getChatHistory());
        scrollToBottom();

        btnSend.setOnClickListener(v -> sendMessage());

        view.findViewById(R.id.btnClearChat).setOnClickListener(v -> {
            db.clearChatHistory();
            chatAdapter.setMessages(new ArrayList<>());
        });

        view.findViewById(R.id.btnScanToInvest).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ScanToInvestFragment())
                        .addToBackStack(null)
                        .commit());
    }

    private void sendMessage() {
        String query = etInput.getText().toString().trim();
        if (query.isEmpty()) return;

        etInput.setText("");

        // Save + display user message
        db.insertMessage(ChatMessage.ROLE_USER, query);
        chatAdapter.addMessage(new ChatMessage(ChatMessage.ROLE_USER, query));
        scrollToBottom();

        setTyping(true);

        // Build messages list for API (last 20 messages for context)
        List<ChatMessage> history = db.getChatHistory();
        List<AiApiService.Message> apiMessages = new ArrayList<>();
        apiMessages.add(new AiApiService.Message("system", buildSystemPrompt()));

        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            ChatMessage m = history.get(i);
            apiMessages.add(new AiApiService.Message(m.role, m.content));
        }

        AiApiService.ChatRequest req = new AiApiService.ChatRequest(Config.GROK_MODEL, apiMessages);
        String auth = "Bearer " + Config.GROK_API_KEY;

        NetworkClient.getAiApi().getCompletion(auth, req)
                .enqueue(new Callback<AiApiService.ChatResponse>() {
                    @Override
                    public void onResponse(Call<AiApiService.ChatResponse> call,
                                           Response<AiApiService.ChatResponse> response) {
                        setTyping(false);
                        if (response.isSuccessful() && response.body() != null
                                && !response.body().choices.isEmpty()) {
                            String reply = response.body().choices.get(0).message.content;
                            db.insertMessage(ChatMessage.ROLE_AI, reply);
                            chatAdapter.addMessage(new ChatMessage(ChatMessage.ROLE_AI, reply));
                            scrollToBottom();
                        } else {
                            String body = "";
                            try {
                                if (response.errorBody() != null)
                                    body = response.errorBody().string();
                            } catch (IOException ignored) {}
                            String err = "AI error " + response.code()
                                    + (body.isEmpty() ? "" : ": " + body);
                            chatAdapter.addMessage(new ChatMessage(ChatMessage.ROLE_AI, err));
                            scrollToBottom();
                        }
                    }

                    @Override
                    public void onFailure(Call<AiApiService.ChatResponse> call, Throwable t) {
                        setTyping(false);
                        chatAdapter.addMessage(new ChatMessage(ChatMessage.ROLE_AI,
                                "Connection failed: " + t.getMessage()));
                        scrollToBottom();
                    }
                });
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are ApexPay's AI Risk Analyst — a concise, data-driven financial advisor.\n\n");

        // Inject live portfolio context
        List<Holding> holdings = db.getAllHoldings();
        if (!holdings.isEmpty()) {
            sb.append("User's Current Portfolio:\n");
            double totalValue = 0, totalCost = 0;
            for (Holding h : holdings) {
                Asset a = MarketDataService.getAsset(h.symbol);
                double price = (a != null && a.price > 0) ? a.price : h.avgBuyPrice;
                double val   = h.quantity * price;
                double pnl   = val - h.quantity * h.avgBuyPrice;
                totalValue  += val;
                totalCost   += h.quantity * h.avgBuyPrice;
                sb.append(String.format("  • %s (%s): %.4f units @ $%.2f avg buy, now $%.2f (P&L %+.2f)\n",
                        h.name, h.symbol, h.quantity, h.avgBuyPrice, price, pnl));
            }
            double portfolioPnl = totalValue - totalCost;
            String pnlStr = (portfolioPnl >= 0 ? "+" : "-") + String.format("$%,.2f", Math.abs(portfolioPnl));
            sb.append(String.format("Total Portfolio: $%,.2f | P&L: %s\n\n", totalValue, pnlStr));
        } else {
            sb.append("User has no holdings yet.\n\n");
        }

        sb.append("Rules:\n");
        sb.append("- Keep answers concise (2-4 sentences for simple questions).\n");
        sb.append("- For risk analysis, reference P/E ratios, market cap, volatility, or DCF where relevant.\n");
        sb.append("- Always remind the user this is a simulated portfolio for educational purposes.\n");
        sb.append("- If asked about a specific asset in the portfolio, reference their actual position data above.");

        return sb.toString();
    }

    private void setTyping(boolean on) {
        if (llTyping != null) llTyping.setVisibility(on ? View.VISIBLE : View.GONE);
        if (btnSend  != null) btnSend.setEnabled(!on);
    }

    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0)
            rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }
}
