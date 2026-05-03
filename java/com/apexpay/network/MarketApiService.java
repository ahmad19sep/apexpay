package com.apexpay.network;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MarketApiService {


    @GET("quote")
    Call<QuoteResponse> getQuote(
            @Query("symbol") String symbol,
            @Query("token") String token);


    @GET("https://api.coingecko.com/api/v3/simple/price")
    Call<Map<String, Map<String, Double>>> getCryptoPrices(
            @Query("ids") String ids,
            @Query("vs_currencies") String vsCurrencies,
            @Query("include_24hr_change") boolean include24hChange);



    class QuoteResponse {
        public double c;

        public double d;

        public double dp;

        public double h;

        public double l;

        public double o;

        public double pc;

    }
}
