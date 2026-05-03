package com.apexpay.services;

import com.apexpay.Config;
import com.apexpay.models.Asset;
import com.apexpay.models.CandlePoint;
import com.apexpay.network.MarketApiService;
import com.apexpay.network.NetworkClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MarketDataService {

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    // CoinGecko coin IDs matching our crypto symbols
    private static final Map<String, String> COIN_IDS = new HashMap<String, String>() {{
        put("BTC",  "bitcoin");
        put("ETH",  "ethereum");
        put("BNB",  "binancecoin");
        put("SOL",  "solana");
        put("XRP",  "ripple");
        put("ADA",  "cardano");
        put("AVAX", "avalanche-2");
        put("DOGE", "dogecoin");
    }};

    private static List<Asset>          sAssets;
    private static OnDataChangedListener sListener;

    public static void setListener(OnDataChangedListener l) { sListener = l; }

    // ── Public accessors ──────────────────────────────────────────────────────

    public static synchronized List<Asset> getAllAssets() {
        if (sAssets == null) init();
        return new ArrayList<>(sAssets);
    }

    public static Asset getAsset(String symbol) {
        if (sAssets == null) init();
        for (Asset a : sAssets) {
            if (a.symbol.equals(symbol)) return a;
        }
        return null;
    }

    public static List<Asset> getByType(String type) {
        List<Asset> out = new ArrayList<>();
        for (Asset a : getAllAssets()) {
            if (a.type.equals(type)) out.add(a);
        }
        return out;
    }

    public static List<Asset> getTopGainers() {
        List<Asset> sorted = getAllAssets();
        sorted.sort((a, b) -> Double.compare(b.changePercent, a.changePercent));
        return sorted.subList(0, Math.min(10, sorted.size()));
    }

    // ── Live price fetch (call this instead of the old refreshMarketData) ─────

    /**
     * Fetches live prices from Finnhub (stocks/ETFs) and CoinGecko (crypto).
     * Calls {@code onComplete} on a background thread once all calls finish.
     */
    public static void fetchLivePrices(Runnable onComplete) {
        if (sAssets == null) init();

        List<Asset> stocks  = new ArrayList<>();
        List<Asset> cryptos = new ArrayList<>();

        for (Asset a : sAssets) {
            if (a.type.equals(Asset.TYPE_CRYPTO)) cryptos.add(a);
            else                                   stocks.add(a);  // stocks + ETFs
        }

        int total = stocks.size() + (cryptos.isEmpty() ? 0 : 1);
        if (total == 0) { if (onComplete != null) onComplete.run(); return; }

        AtomicInteger done = new AtomicInteger(0);
        Runnable tick = () -> {
            if (done.incrementAndGet() >= total && onComplete != null) onComplete.run();
        };

        // ── Stocks + ETFs from Finnhub ─────────────────────────────────────
        for (Asset asset : stocks) {
            NetworkClient.getMarketApi()
                    .getQuote(asset.symbol, Config.FINNHUB_API_KEY)
                    .enqueue(new Callback<MarketApiService.QuoteResponse>() {
                        @Override
                        public void onResponse(Call<MarketApiService.QuoteResponse> call,
                                               Response<MarketApiService.QuoteResponse> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().c > 0) {
                                asset.price         = response.body().c;
                                asset.changePercent = response.body().dp;
                            }
                            tick.run();
                        }

                        @Override
                        public void onFailure(Call<MarketApiService.QuoteResponse> call, Throwable t) {
                            tick.run();
                        }
                    });
        }

        // ── Crypto batch from CoinGecko ───────────────────────────────────
        if (!cryptos.isEmpty()) {
            StringBuilder ids = new StringBuilder();
            for (Asset c : cryptos) {
                String id = COIN_IDS.get(c.symbol);
                if (id != null) {
                    if (ids.length() > 0) ids.append(",");
                    ids.append(id);
                }
            }

            NetworkClient.getMarketApi()
                    .getCryptoPrices(ids.toString(), "usd", true)
                    .enqueue(new Callback<Map<String, Map<String, Double>>>() {
                        @Override
                        public void onResponse(Call<Map<String, Map<String, Double>>> call,
                                               Response<Map<String, Map<String, Double>>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Map<String, Map<String, Double>> data = response.body();
                                for (Asset asset : cryptos) {
                                    String coinId = COIN_IDS.get(asset.symbol);
                                    if (coinId != null && data.containsKey(coinId)) {
                                        Map<String, Double> vals = data.get(coinId);
                                        if (vals.containsKey("usd") && vals.get("usd") > 0)
                                            asset.price = vals.get("usd");
                                        if (vals.containsKey("usd_24h_change"))
                                            asset.changePercent = vals.get("usd_24h_change");
                                    }
                                }
                            }
                            tick.run();
                        }

                        @Override
                        public void onFailure(Call<Map<String, Map<String, Double>>> call, Throwable t) {
                            tick.run();
                        }
                    });
        }
    }

    // ── Candle data (generated, anchored to live price) ───────────────────────

    public static List<CandlePoint> getCandleData(String symbol, int days) {
        Asset asset = getAsset(symbol);
        double base = asset != null && asset.price > 0 ? asset.price : 100.0;
        return buildCandles(base, days);
    }

    private static List<CandlePoint> buildCandles(double currentPrice, int days) {
        List<CandlePoint> list = new ArrayList<>();
        Random r = new Random(Double.doubleToLongBits(currentPrice) + days);

        int candles = 30;
        double price = currentPrice * 0.85;
        double step  = (currentPrice - price) / candles;
        long now     = System.currentTimeMillis();
        long period  = (long) days * 24L * 3600L * 1000L / candles;
        double vol   = currentPrice * 0.025;

        for (int i = 0; i < candles; i++) {
            double open  = price;
            double close = open + step + (r.nextGaussian() * vol);
            double wick  = Math.abs(r.nextGaussian() * vol * 0.5);
            double high  = Math.max(open, close) + wick;
            double low   = Math.min(open, close) - wick;

            list.add(new CandlePoint((float) open, (float) close,
                    (float) high, (float) low,
                    now - (candles - i) * period));
            price = close;
        }

        if (!list.isEmpty()) {
            CandlePoint last = list.get(list.size() - 1);
            last.close = (float) currentPrice;
            last.high  = Math.max(last.high,  last.close);
            last.low   = Math.min(last.low,   last.close);
        }
        return list;
    }

    // ── Asset catalog (prices start at 0, filled by fetchLivePrices) ──────────

    private static synchronized void init() {
        sAssets = new ArrayList<>();

        // Crypto
        sAssets.add(new Asset("BTC",  "Bitcoin",        Asset.TYPE_CRYPTO,  0, 0, 843.5e9, 0, 28.4e9, "₿"));
        sAssets.add(new Asset("ETH",  "Ethereum",        Asset.TYPE_CRYPTO,  0, 0, 274.8e9, 0, 12.1e9, "Ξ"));
        sAssets.add(new Asset("BNB",  "Binance Coin",    Asset.TYPE_CRYPTO,  0, 0,  63.4e9, 0,  1.8e9, "B"));
        sAssets.add(new Asset("SOL",  "Solana",          Asset.TYPE_CRYPTO,  0, 0,  43.2e9, 0,  3.2e9, "◎"));
        sAssets.add(new Asset("XRP",  "Ripple",          Asset.TYPE_CRYPTO,  0, 0,  28.7e9, 0,  1.4e9, "✕"));
        sAssets.add(new Asset("ADA",  "Cardano",         Asset.TYPE_CRYPTO,  0, 0,  15.9e9, 0,  0.8e9, "₳"));
        sAssets.add(new Asset("AVAX", "Avalanche",       Asset.TYPE_CRYPTO,  0, 0,  15.4e9, 0,  0.9e9, "A"));
        sAssets.add(new Asset("DOGE", "Dogecoin",        Asset.TYPE_CRYPTO,  0, 0,  17.3e9, 0,  1.1e9, "Ð"));

        // Stocks
        sAssets.add(new Asset("AAPL", "Apple Inc.",      Asset.TYPE_STOCK,   0, 0, 2940e9, 31.2, 87.4e9, ""));
        sAssets.add(new Asset("MSFT", "Microsoft",       Asset.TYPE_STOCK,   0, 0, 2820e9, 35.8, 62.1e9, ""));
        sAssets.add(new Asset("GOOGL","Alphabet Inc.",   Asset.TYPE_STOCK,   0, 0, 2080e9, 27.4, 54.8e9, ""));
        sAssets.add(new Asset("AMZN", "Amazon.com",      Asset.TYPE_STOCK,   0, 0, 2050e9, 62.3, 78.9e9, ""));
        sAssets.add(new Asset("TSLA", "Tesla Inc.",      Asset.TYPE_STOCK,   0, 0,  789e9, 78.9,112.3e9, ""));
        sAssets.add(new Asset("NVDA", "NVIDIA Corp.",    Asset.TYPE_STOCK,   0, 0, 2160e9, 65.4,245.6e9, ""));
        sAssets.add(new Asset("META", "Meta Platforms",  Asset.TYPE_STOCK,   0, 0, 1310e9, 25.7, 48.2e9, ""));
        sAssets.add(new Asset("NFLX", "Netflix Inc.",    Asset.TYPE_STOCK,   0, 0,  267e9, 42.1, 12.7e9, ""));

        // ETFs
        sAssets.add(new Asset("SPY",  "SPDR S&P 500",    Asset.TYPE_ETF,     0, 0,  498e9, 25.1, 92.4e9, ""));
        sAssets.add(new Asset("QQQ",  "Invesco QQQ",     Asset.TYPE_ETF,     0, 0,  248e9, 32.6, 58.7e9, ""));
        sAssets.add(new Asset("VTI",  "Vanguard Total",  Asset.TYPE_ETF,     0, 0,  379e9, 24.8, 34.2e9, ""));
    }
}
