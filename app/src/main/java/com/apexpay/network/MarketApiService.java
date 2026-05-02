package com.apexpay.network;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MarketApiService {

    // Finnhub: live quote for a single symbol
    @GET("quote")
    Call<QuoteResponse> getQuote(
            @Query("symbol") String symbol,
            @Query("token")  String token);

    // CoinGecko: batch price + 24h change for all crypto in one call (no key needed)
    @GET("https://api.coingecko.com/api/v3/simple/price")
    Call<Map<String, Map<String, Double>>> getCryptoPrices(
            @Query("ids")                  String ids,
            @Query("vs_currencies")        String vsCurrencies,
            @Query("include_24hr_change")  boolean include24hChange);

    // ── Response DTOs ─────────────────────────────────────────────────────────

    class QuoteResponse {
        public double c;   // current price
        public double d;   // absolute change
        public double dp;  // percent change
        public double h;   // high
        public double l;   // low
        public double o;   // open
        public double pc;  // previous close
    }
}
