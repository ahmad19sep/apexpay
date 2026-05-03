package com.apexpay.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.apexpay.R;
import com.apexpay.database.DatabaseHelper;
import com.apexpay.models.Asset;
import com.apexpay.services.MarketDataService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MockTradeFragment extends BottomSheetDialogFragment {

    private static final String ARG_SYMBOL = "symbol";
    private static final String ARG_IS_BUY = "isBuy";

    private static final double FEE_NETWORK = 0.01;
    private static final double FEE_SPREAD = 0.0065;

    private Asset asset;
    private boolean isBuy = true;

    private EditText etAmount;
    private TextView tvFractionalShares, tvOrderValue, tvNetworkFee;
    private TextView tvSpread, tvTotalDeducted, tvAvailableBalance;
    private TextView btnBuyTab, btnSellTab;
    private Button btnConfirm;

    private DatabaseHelper db;
    private SharedPreferences prefs;

    public static MockTradeFragment newInstance(String symbol, boolean isBuy) {
        MockTradeFragment f = new MockTradeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SYMBOL, symbol);
        args.putBoolean(ARG_IS_BUY, isBuy);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mock_trade, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String symbol;
        if (getArguments() != null) {
            symbol = getArguments().getString(ARG_SYMBOL, "BTC");
        } else {
            symbol = "BTC";
        }

        if (getArguments() != null) {
            isBuy = getArguments().getBoolean(ARG_IS_BUY, true);
        } else {
            isBuy = true;
        }

        asset = MarketDataService.getAsset(symbol);
        db = new DatabaseHelper(requireContext());
        prefs = requireActivity().getSharedPreferences("ApexPayPrefs", android.content.Context.MODE_PRIVATE);

        if (asset == null) {
            dismiss();
            return;
        }

        bindViews(view);
        populateHeader();
        updateMode();
        setupListeners(view);
        recalculate();
    }

    private void bindViews(View v) {
        etAmount = v.findViewById(R.id.etAmount);
        tvFractionalShares = v.findViewById(R.id.tvFractionalShares);
        tvOrderValue = v.findViewById(R.id.tvOrderValue);
        tvNetworkFee = v.findViewById(R.id.tvNetworkFee);
        tvSpread = v.findViewById(R.id.tvSpread);
        tvTotalDeducted = v.findViewById(R.id.tvTotalDeducted);
        tvAvailableBalance = v.findViewById(R.id.tvAvailableBalance);
        btnBuyTab = v.findViewById(R.id.btnBuyTab);
        btnSellTab = v.findViewById(R.id.btnSellTab);
        btnConfirm = v.findViewById(R.id.btnConfirmTrade);
    }

    private void populateHeader() {
        View v = requireView();
        TextView tvIcon = v.findViewById(R.id.tvTradeIcon);
        TextView tvName = v.findViewById(R.id.tvTradeAssetName);
        TextView tvPrice = v.findViewById(R.id.tvTradePrice);
        TextView tvChange = v.findViewById(R.id.tvTradeChange);

        String icon;
        if (asset.iconEmoji.isEmpty()) {
            icon = asset.symbol.substring(0, 1);
        } else {
            icon = asset.iconEmoji;
        }

        tvIcon.setText(icon);
        tvName.setText(asset.name + " (" + asset.symbol + ")");
        tvPrice.setText(String.format("$%,.2f per unit", asset.price));
        tvChange.setText(String.format("%+.2f%%", asset.changePercent));

        boolean gain = asset.changePercent >= 0;
        if (gain) {
            tvChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.gain_green));
        } else {
            tvChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
        }
    }

    private void updateMode() {
        if (isBuy) {
            btnBuyTab.setBackgroundResource(R.drawable.bg_buy_btn);
            btnBuyTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            btnSellTab.setBackgroundResource(R.drawable.bg_tab_inactive);
            btnSellTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            btnConfirm.setBackgroundResource(R.drawable.bg_confirm_btn);
            btnConfirm.setText(getString(R.string.confirm_buy));
        } else {
            btnSellTab.setBackgroundResource(R.drawable.bg_sell_btn);
            btnSellTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            btnBuyTab.setBackgroundResource(R.drawable.bg_tab_inactive);
            btnBuyTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            btnConfirm.setBackgroundResource(R.drawable.bg_sell_btn);
            btnConfirm.setText(getString(R.string.confirm_sell));
        }
    }

    private void setupListeners(View v) {
        btnBuyTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                isBuy = true;
                updateMode();
                recalculate();
            }
        });

        btnSellTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                isBuy = false;
                updateMode();
                recalculate();
            }
        });

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                recalculate();
            }
        });

        v.findViewById(R.id.btnAmountMinus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                adjustAmount(-10);
            }
        });

        v.findViewById(R.id.btnAmountPlus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                adjustAmount(10);
            }
        });

        v.findViewById(R.id.btn25pct).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                setAmountPct(0.25);
            }
        });

        v.findViewById(R.id.btn50pct).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                setAmountPct(0.50);
            }
        });

        v.findViewById(R.id.btn75pct).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                setAmountPct(0.75);
            }
        });

        v.findViewById(R.id.btnMax).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                setAmountMax();
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                executeTrade();
            }
        });
    }

    private void adjustAmount(double delta) {
        double current = parseAmount();
        double next = Math.max(0, current + delta);
        etAmount.setText(String.format("%.2f", next));
        etAmount.setSelection(etAmount.getText().length());
    }

    private void setAmountPct(double pct) {
        double cash = getSimCash();
        double val;
        if (isBuy) {
            val = cash * pct;
        } else {
            val = db.getQuantity(asset.symbol) * asset.price * pct;
        }
        etAmount.setText(String.format("%.2f", val));
        etAmount.setSelection(etAmount.getText().length());
    }

    private void setAmountMax() {
        double val;
        if (isBuy) {
            val = getSimCash();
        } else {
            val = db.getQuantity(asset.symbol) * asset.price;
        }
        etAmount.setText(String.format("%.2f", val));
        etAmount.setSelection(etAmount.getText().length());
    }

    private void recalculate() {
        double orderValue = parseAmount();

        double units;
        if (asset.price > 0) {
            units = orderValue / asset.price;
        } else {
            units = 0;
        }

        double netFee = orderValue * FEE_NETWORK;
        double spread = orderValue * FEE_SPREAD;

        double total;
        if (isBuy) {
            total = orderValue + netFee + spread;
        } else {
            total = orderValue - netFee - spread;
        }

        if (units < 1) {
            tvFractionalShares.setText(String.format("%.6f %s", units, asset.symbol));
        } else {
            tvFractionalShares.setText(String.format("%.4f %s", units, asset.symbol));
        }

        tvOrderValue.setText(String.format("$%,.2f", orderValue));
        tvNetworkFee.setText(String.format("-$%,.2f", netFee));
        tvSpread.setText(String.format("-$%,.2f", spread));
        tvTotalDeducted.setText(String.format("$%,.2f", Math.abs(total)));
        tvAvailableBalance.setText(String.format("$%,.2f", getSimCash()));
    }

    private void executeTrade() {
        double orderValue = parseAmount();
        if (orderValue <= 0) {
            Toast.makeText(getContext(), getString(R.string.error_enter_valid_amount), Toast.LENGTH_SHORT).show();
            return;
        }

        double units = orderValue / asset.price;
        double netFee = orderValue * FEE_NETWORK;
        double spread = orderValue * FEE_SPREAD;
        double cash = getSimCash();

        if (isBuy) {
            double totalCost = orderValue + netFee + spread;
            if (totalCost > cash) {
                Toast.makeText(getContext(), getString(R.string.error_insufficient_sim_cash), Toast.LENGTH_SHORT).show();
                return;
            }
            db.buyAsset(asset.symbol, asset.name, asset.type, units, asset.price);
            setSimCash(cash - totalCost);
            db.insertLedger("📈", "Mock Buy " + asset.symbol, totalCost, false);
            Toast.makeText(getContext(),
                    String.format("Bought %.6f %s for $%,.2f", units, asset.symbol, totalCost),
                    Toast.LENGTH_LONG).show();
        } else {
            double heldQty = db.getQuantity(asset.symbol);
            if (units > heldQty + 1e-9) {
                Toast.makeText(getContext(),
                        String.format("You only hold %.6f %s", heldQty, asset.symbol),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            double proceeds = orderValue - netFee - spread;
            if (!db.sellAsset(asset.symbol, units)) {
                Toast.makeText(getContext(), getString(R.string.error_sell_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            setSimCash(cash + proceeds);
            db.insertLedger("📉", "Mock Sell " + asset.symbol, proceeds, true);
            Toast.makeText(getContext(),
                    String.format("Sold %.6f %s for $%,.2f", units, asset.symbol, proceeds),
                    Toast.LENGTH_LONG).show();
        }

        Bundle result = new Bundle();
        result.putBoolean("tradeCompleted", true);
        getParentFragmentManager().setFragmentResult("trade_request", result);
        dismiss();
    }

    private double parseAmount() {
        try {
            String s = etAmount.getText().toString().trim();
            if (s.isEmpty()) {
                return 0;
            }
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double getSimCash() {
        return Double.longBitsToDouble(prefs.getLong("simCash", Double.doubleToLongBits(10000.0)));
    }

    private void setSimCash(double value) {
        prefs.edit().putLong("simCash", Double.doubleToLongBits(value)).apply();
    }
}
