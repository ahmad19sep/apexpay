package com.apexpay;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.apexpay.fragments.AiFragment;
import com.apexpay.fragments.BrokerageFragment;
import com.apexpay.fragments.HomeFragment;
import com.apexpay.fragments.ProfileFragment;
import com.apexpay.fragments.WalletFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private String userEmail;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userEmail = getIntent().getStringExtra("userEmail");
        if (userEmail == null) {
            SharedPreferences prefs = getSharedPreferences("ApexPayPrefs", MODE_PRIVATE);
            userEmail = prefs.getString("userEmail", "");
        }

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment = null;
            
            if (id == R.id.nav_home)      fragment = HomeFragment.newInstance(userEmail);
            else if (id == R.id.nav_wallet)    fragment = new WalletFragment();
            else if (id == R.id.nav_brokerage) fragment = new BrokerageFragment();
            else if (id == R.id.nav_ai)        fragment = new AiFragment();
            else if (id == R.id.nav_profile)   fragment = ProfileFragment.newInstance(userEmail);

            if (fragment != null) {
                // For main tabs, we clear the back stack so we don't pile up fragments
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                loadFragment(fragment, false);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            loadFragment(HomeFragment.newInstance(userEmail), false);
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /**
     * Helper to load fragments. 
     * @param addToBackStack true if this is a sub-page, false if it's a main tab.
     */
    public boolean loadFragment(Fragment fragment, boolean addToBackStack) {
        var transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment);
        
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        
        transaction.commit();
        return true;
    }

    // Default loadFragment for backward compatibility with existing fragment calls
    public boolean loadFragment(Fragment fragment) {
        return loadFragment(fragment, true);
    }
}
