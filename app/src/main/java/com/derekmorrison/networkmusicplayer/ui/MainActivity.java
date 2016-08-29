package com.derekmorrison.networkmusicplayer.ui;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.derekmorrison.networkmusicplayer.R;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    static ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // todo find a way to only do this once - ?? shared prefs ??
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            OnboardingFragment fragment = new OnboardingFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mActionBar =  getSupportActionBar();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public static void setToolbarTitle(String newTitle) {
        mActionBar.setTitle(newTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_now_playing) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            NowPlayingFragment fragment = new NowPlayingFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        } else if (id == R.id.nav_current_playlist) {


        } else if (id == R.id.nav_current_folder) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            DirectoryFragment fragment = new DirectoryFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        } else if (id == R.id.nav_servers) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            AllServersFragment fragment = new AllServersFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        } else if (id == R.id.nav_discovery_status) {
            // switch to the Initial Network Scan fragment
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            InitialScanFragment fragment = new InitialScanFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
