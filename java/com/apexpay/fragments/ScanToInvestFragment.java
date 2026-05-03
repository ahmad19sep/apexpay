package com.apexpay.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.apexpay.R;
import com.apexpay.services.MarketDataService;
import com.apexpay.models.Asset;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanToInvestFragment extends Fragment {

    private static final int CAMERA_PERMISSION_CODE = 101;

    // Brand/product label → ticker mapping (includes both brand names and MLKit generic labels)
    private static final Map<String, String> BRAND_MAP = new HashMap<String, String>() {{
        // Apple devices — MLKit may return these specific labels
        put("apple",      "AAPL"); put("macbook",     "AAPL"); put("iphone",   "AAPL");
        put("ipad",       "AAPL"); put("imac",        "AAPL"); put("airpods",  "AAPL");
        put("mac",        "AAPL"); put("macintosh",   "AAPL"); put("ios",      "AAPL");
        // Smartphones / laptops → AAPL as most recognisable in demo context
        put("smartphone", "AAPL"); put("mobile phone","AAPL"); put("iphone x","AAPL");
        put("earphone",   "AAPL"); put("earbuds",     "AAPL"); put("tablet",   "AAPL");
        // Microsoft
        put("microsoft",  "MSFT"); put("windows",  "MSFT"); put("xbox",     "MSFT");
        put("surface",    "MSFT"); put("office",   "MSFT"); put("laptop",   "MSFT");
        put("computer",   "MSFT");
        // Google / Alphabet
        put("google",     "GOOGL"); put("alphabet","GOOGL"); put("android", "GOOGL");
        put("youtube",    "GOOGL"); put("pixel",   "GOOGL");
        // Amazon
        put("amazon",     "AMZN"); put("alexa",    "AMZN"); put("kindle",   "AMZN");
        put("echo",       "AMZN"); put("fire",     "AMZN");
        // Tesla
        put("tesla",      "TSLA"); put("model s",  "TSLA"); put("model 3",  "TSLA");
        put("electric car","TSLA"); put("electric vehicle","TSLA"); put("ev","TSLA");
        put("automobile", "TSLA"); put("car",      "TSLA"); put("vehicle",  "TSLA");
        // NVIDIA
        put("nvidia",     "NVDA"); put("geforce",  "NVDA"); put("rtx",      "NVDA");
        put("gpu",        "NVDA"); put("graphics", "NVDA");
        // Meta
        put("meta",       "META"); put("facebook", "META"); put("instagram","META");
        put("whatsapp",   "META"); put("oculus",   "META"); put("vr headset","META");
        // Netflix
        put("netflix",    "NFLX"); put("streaming","NFLX");
        // Crypto
        put("bitcoin",    "BTC");  put("btc",       "BTC"); put("coin",     "BTC");
        put("ethereum",   "ETH");  put("eth",        "ETH"); put("crypto",   "BTC");
        put("cryptocurrency","BTC");
        // Nike — running shoes
        put("nike",       "NKE"); put("running shoe","NKE"); put("sneaker",  "NKE");
        put("shoe",       "NKE"); put("footwear",    "NKE");
    }};

    private PreviewView  previewView;
    private TextView     tvResult, tvHint;
    private View         cardResult;
    private TextView     tvDetectedAsset, tvDetectedPrice;

    private ExecutorService cameraExecutor;
    private ImageLabeler    labeler;
    private volatile boolean detected = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView      = view.findViewById(R.id.previewView);
        tvResult         = view.findViewById(R.id.tvScanResult);
        tvHint           = view.findViewById(R.id.tvScanHint);
        cardResult       = view.findViewById(R.id.cardScanResult);
        tvDetectedAsset  = view.findViewById(R.id.tvDetectedAsset);
        tvDetectedPrice  = view.findViewById(R.id.tvDetectedPrice);

        view.findViewById(R.id.btnScanBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.btnRescan).setOnClickListener(v -> {
            detected = false;
            cardResult.setVisibility(View.GONE);
            tvHint.setVisibility(View.VISIBLE);
        });

        cameraExecutor = Executors.newSingleThreadExecutor();
        labeler = ImageLabeling.getClient(
                new ImageLabelerOptions.Builder().setConfidenceThreshold(0.5f).build());

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                provider.unbindAll();
                provider.bindToLifecycle(getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);

            } catch (Exception e) {
                Toast.makeText(getContext(), "Camera failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @androidx.camera.core.ExperimentalGetImage
    private void analyzeFrame(ImageProxy proxy) {
        if (detected || proxy.getImage() == null) {
            proxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                proxy.getImage(), proxy.getImageInfo().getRotationDegrees());

        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    String matchedTicker = null;
                    for (ImageLabel label : labels) {
                        // Check full label text and each word in it
                        String fullText = label.getText().toLowerCase();
                        String ticker = findTicker(fullText);
                        if (ticker == null) {
                            for (String word : fullText.split("\\s+")) {
                                ticker = findTicker(word);
                                if (ticker != null) break;
                            }
                        }
                        if (ticker != null) {
                            matchedTicker = ticker;
                            break;
                        }
                    }
                    if (matchedTicker != null) {
                        detected = true;
                        final String finalTicker = matchedTicker;
                        android.app.Activity act = getActivity();
                        if (act != null) {
                            act.runOnUiThread(() -> {
                                if (isAdded()) showResult(finalTicker);
                            });
                        }
                    }
                })
                .addOnCompleteListener(t -> proxy.close());
    }

    private String findTicker(String labelText) {
        for (Map.Entry<String, String> e : BRAND_MAP.entrySet()) {
            if (labelText.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    private void showResult(String ticker) {
        Asset asset = MarketDataService.getAsset(ticker);
        if (asset == null) return;

        tvHint.setVisibility(View.GONE);
        cardResult.setVisibility(View.VISIBLE);

        tvDetectedAsset.setText("Detected → " + asset.name + " (" + ticker + ")");
        tvDetectedPrice.setText(asset.price > 0
                ? String.format("$%,.2f per unit", asset.price)
                : "Loading price…");

        cardResult.findViewById(R.id.btnBuyDetected).setOnClickListener(v -> {
            MockTradeFragment sheet = MockTradeFragment.newInstance(ticker, true);
            sheet.show(getParentFragmentManager(), "scan_trade");
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(getContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}
