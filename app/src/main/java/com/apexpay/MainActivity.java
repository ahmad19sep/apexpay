package com.apexpay;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.apexpay.fragments.AiFragment;
import com.apexpay.fragments.BrokerageFragment;
import com.apexpay.fragments.HomeFragment;
import com.apexpay.fragments.ProfileFragment;
import com.apexpay.fragments.WalletFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userEmail = getIntent().getStringExtra("userEmail");
        if (userEmail == null) {
            SharedPreferences prefs = getSharedPreferences("ApexPayPrefs", MODE_PRIVATE);
            userEmail = prefs.getString("userEmail", "");
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)      return loadFragment(HomeFragment.newInstance(userEmail));
            if (id == R.id.nav_wallet)    return loadFragment(new WalletFragment());
            if (id == R.id.nav_brokerage) return loadFragment(new BrokerageFragment());
            if (id == R.id.nav_ai)        return loadFragment(new AiFragment());
            if (id == R.id.nav_profile)   return loadFragment(ProfileFragment.newInstance(userEmail));
            return false;
        });

        if (savedInstanceState == null) {
            loadFragment(HomeFragment.newInstance(userEmail));
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private boolean loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        return true;
    }
}
