package com.lam.lam_project.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.navigation.NavigationView;
import com.lam.lam_project.R;

public class MenuActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FrameLayout container;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private Toolbar toolbar;

    @Override
    public void setContentView(View view) {
        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_menu, null);
        container = drawerLayout.findViewById(R.id.activity_container);
        container.addView(view);
        super.setContentView(drawerLayout);

        toolbar = drawerLayout.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        navigationView = drawerLayout.findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.nav_map:
                drawerLayout.closeDrawer(GravityCompat.START);
                if (this instanceof MapActivity) break;

                startActivity(new Intent(this, MapActivity.class));
                overridePendingTransition(0,0);
                break;
            case R.id.nav_settings:
                drawerLayout.closeDrawer(GravityCompat.START);

                if (this instanceof SettingsActivity) break;

                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(0,0);
                break;
        }

        return true;
    }


    protected void setActivityTitle(String title){
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(title);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Base Activity Created");
    }
}