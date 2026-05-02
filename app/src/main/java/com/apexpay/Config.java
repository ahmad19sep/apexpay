package com.apexpay;

public class Config
{
    // --- API KEYS ---
    // Finnhub (stocks/ETFs): https://finnhub.io/
    public static final String FINNHUB_API_KEY = "d7qr5b9r01qudmin88dgd7qr5b9r01qudmin88e0";

    // OpenAI: https://platform.openai.com/ — paste your key here if using GPT
    public static final String OPENAI_API_KEY = "YOUR_OPENAI_KEY_HERE";

    // Grok (xAI): https://x.ai/api — your key is already set below
    public static final String GROK_API_KEY = "Yxai-VZS5iwXYa0OT5UJgTAbOwFW6dLho1vk8Zd0p4Q14p74VxNurDdT74PAagMMUfDzLRTY91mPQ2E787iMD";

    // Model to use — grok-3-mini is fast & cheap; swap to grok-3 for deeper analysis
    public static final String GROK_MODEL = "grok-3-mini";

    // --- BASE URLS ---
    public static final String FINNHUB_BASE_URL  = "https://finnhub.io/api/v1/";
    public static final String COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3/";
    public static final String GROK_BASE_URL      = "https://api.x.ai/";
}
