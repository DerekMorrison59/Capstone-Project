package com.derekmorrison.networkmusicplayer.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.renderscript.ScriptGroup;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.ActionBar;
import android.transition.Fade;
import android.transition.Slide;
import android.util.Log;
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
import com.derekmorrison.networkmusicplayer.util.Playlist;
import com.derekmorrison.networkmusicplayer.util.SharedPrefUtils;
import com.derekmorrison.networkmusicplayer.util.SongList;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String TAG = "MainActivity";

    private static FragmentManager mSupportFragmentManager = null;

    public static final int FRAGMENT_INITIAL_SCAN = 0;
    public static final int FRAGMENT_ALL_SERVERS = 1;
    public static final int FRAGMENT_DIRECTORY = 2;
    public static final int FRAGMENT_NOW_PLAYING = 3;
    public static final int FRAGMENT_ALL_PLAYLIST = 4;
    public static final int FRAGMENT_ONBOARDING = 5;
    public static final int FRAGMENT_PLAYLIST = 6;
    public static final int FRAGMENT_SHARE = 7;
    public static final int FRAGMENT_FAV_DIRS = 8;

    public static final String TAG_INITIAL_SCAN = "initial_scan";
    public static final String TAG_ALL_SERVERS = "all_servers";
    public static final String TAG_DIRECTORY = "directory";
    public static final String TAG_NOW_PLAYING = "now_playing";
    public static final String TAG_ALL_PLAYLIST = "all_playlist";
    public static final String TAG_ONBOARDING = "onboarding";
    public static final String TAG_PLAYLIST = "playlist";
    public static final String TAG_EDIT_PLAYLIST = "edit_playlist";
    public static final String TAG_SHARE = "share";
    public static final String TAG_FAV_DIRS = "fav_dirs";

    public static final String SERVICE_CONNECTED = "service_connected";

    static ActionBar mActionBar;

    private SongService mSongService;
    private boolean mIsBound = false;
    private static int mLastScreen = FRAGMENT_INITIAL_SCAN;
    private static GlobalApp mGlobalApp;
    private static NavigationView mNavigationView;
    private static DrawerLayout mDrawer;
    private int mSaveScreen = -1;
    private static View mReferenceView;

    private FirebaseAnalytics mFirebaseAnalytics;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mSongService = ((SongService.LocalBinder)service).getService();

            if (false == mSongService.hasContext()) {
                mSongService.setUp(getApplicationContext());
                Log.i("MainActivity", "- ServiceConnection - onServiceConnected --- FIRST TIME");
            } else {
                Log.i("MainActivity", "- ServiceConnection - onServiceConnected --- RECONNECTED");
            }

            if (mSaveScreen > 0) {
                setFragment(mSaveScreen);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mSongService = null;
            Log.i("MainActivity", "- ServiceConnection - onServiceDisconnected");
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(MainActivity.this, SongService.class),
                mConnection, Context.BIND_AUTO_CREATE);

        Log.i("ServiceConnection", " doBindService");

        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            Log.i("ServiceConnection", " doUnbindService");
        }
    }

    public static View getReferenceView() { return mReferenceView; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSupportFragmentManager = getSupportFragmentManager();


/*
        if (savedInstanceState != null) {
            mLastScreen = savedInstanceState.getInt(LAST_SCREEN, FRAGMENT_DIRECTORY);

            Log.d("MainActivity", " onCreate - mLastScreen: " + mLastScreen);

            setFragment(mLastScreen);

*/
/*
            if (FRAGMENT_DIRECTORY == mLastScreen) {

                DirectoryFragment fragment = (DirectoryFragment) getSupportFragmentManager().findFragmentByTag(TAG_DIRECTORY);

                if (null != fragment) {
                    Log.d("MainActivity", " onCreate - DirectoryFragment EXISTS");
//                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//                    transaction.replace(R.id.sample_content_fragment, fragment);
//                    transaction.commit();
                } else {
                    Log.d("MainActivity", " onCreate - DirectoryFragment NOT FOUND");
                    setFragment(FRAGMENT_DIRECTORY);
                }

            }

        }
*/

        doBindService();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mActionBar =  getSupportActionBar();
        mReferenceView = toolbar;

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.setDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        mGlobalApp = ((GlobalApp) getApplication());

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        MobileAds.initialize(getApplicationContext(), getResources().getString(R.string.BANNER_ID));

        // find the banner ad widget
        AdView mAdView = (AdView) findViewById(R.id.adView);

        // Create an ad request.
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("B0024BA4FB2E21C06AD469BB3FB6959E")
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mAdView.loadAd(adRequest);

        int playlistId = SharedPrefUtils.getInstance().getLastPlaylistId();
        if (-1 == playlistId) {
            //mGlobalApp.getSongList().buildFromLocalSongs(getApplicationContext());
        } else {

            int lastSong = SharedPrefUtils.getInstance().getLastSong();
            mGlobalApp.getSongList().setPlaylistId(getApplicationContext(), playlistId,
                    SharedPrefUtils.getInstance().getLastPlaylistName());
            mGlobalApp.getSongList().gotoSong(lastSong);


            String songTitle = mGlobalApp.getSongList().getCurrentSong().getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.ITEM_ID, String.valueOf(playlistId));
            params.putString(FirebaseAnalytics.Param.ITEM_NAME, songTitle);
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "song_name");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params);

            //Log.d(TAG, "Create - logEvent");

            mFirebaseAnalytics.setUserProperty("song_name", songTitle);
        }

        if (SharedPrefUtils.getInstance().isFirstTime()) {
            // disable the nav. menu
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            setFragment(FRAGMENT_ONBOARDING);

        } else {
            mLastScreen = SharedPrefUtils.getInstance().getLastScreen();
            boolean service = (null != mSongService);

            Log.i("MainActivity", "- onCreate - mLastScreen: " + mLastScreen + " service running: " + service + " mIsBound: " + mIsBound);

            // can't go to now playing before the service is up and running
            if (1 > mLastScreen || mLastScreen > FRAGMENT_FAV_DIRS ) {
                mLastScreen = FRAGMENT_DIRECTORY;
            }

            if (mLastScreen == FRAGMENT_NOW_PLAYING && false == service) {
                mSaveScreen = mLastScreen;
                mLastScreen = FRAGMENT_DIRECTORY;
            }
            setFragment(mLastScreen);

            if (isTablet(getApplicationContext())) {
                FragmentTransaction tx = mSupportFragmentManager.beginTransaction();
                NowPlayingFragment fragment = new NowPlayingFragment();
                tx.replace(R.id.right_side_fragment, fragment, TAG_NOW_PLAYING);
                tx.commit();
            }
        }

/*
        if (savedInstanceState != null) {
            //setFragment(savedInstanceState.getInt(LAST_SCREEN, FRAGMENT_DIRECTORY));
            mLastScreen = savedInstanceState.getInt(LAST_SCREEN, FRAGMENT_DIRECTORY);

            Log.d("MainActivity", " onCreate - mLastScreen: " + mLastScreen);

            setFragment(mLastScreen);

        } else {
            int playlistId = SharedPrefUtils.getInstance().getLastPlaylistId();
            if (-1 == playlistId) {
                //mGlobalApp.getSongList().buildFromLocalSongs(getApplicationContext());
            } else {

                Playlist selectedPlaylist = new Playlist();
                selectedPlaylist.loadFromDb(this, playlistId, SharedPrefUtils.getInstance().getLastPlaylistName());
                mGlobalApp.getSongList().loadFromPlaylist(selectedPlaylist, mGlobalApp);
            }

            if (SharedPrefUtils.getInstance().isFirstTime()) {
                //SharedPrefUtils.getInstance().firstTimeCompleted();
                // todo disable the nav. menu
                mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                setFragment(FRAGMENT_ONBOARDING);

            } else {
                mLastScreen = SharedPrefUtils.getInstance().getLastScreen();
                if (1 > mLastScreen || mLastScreen > FRAGMENT_SHARE) {
                    mLastScreen = FRAGMENT_DIRECTORY;
                }
                setFragment(mLastScreen);
            }
        }
*/
    }


/*
    @Override
    protected void onStart(){
        super.onStart();


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(LAST_SCREEN, mLastScreen);
    }
*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();

    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public static void setToolbarTitle(String newTitle) {
        mActionBar.setTitle(newTitle);
    }

    public static void setFragment(int newFragment){

        mLastScreen = newFragment;
        SharedPrefUtils.getInstance().setLastScreen(mLastScreen);

        FragmentTransaction transaction = mSupportFragmentManager.beginTransaction();

        if (FRAGMENT_NOW_PLAYING == newFragment) {

            if (isTablet(mGlobalApp.getApplicationContext())) {
                FragmentTransaction tx = mSupportFragmentManager.beginTransaction();
                NowPlayingFragment fragment = new NowPlayingFragment();
                tx.replace(R.id.right_side_fragment, fragment, TAG_NOW_PLAYING);
                tx.commit();
            } else {
                NowPlayingFragment fragment = (NowPlayingFragment) mSupportFragmentManager.findFragmentByTag(TAG_NOW_PLAYING);
                if (null == fragment) {
                    fragment = new NowPlayingFragment();
                }
                fragment.setEnterTransition(new Fade());

                if (true == fragment.isVisible()) {
                    transaction.show(fragment);
                } else {
                    transaction.replace(R.id.sample_content_fragment, fragment, TAG_NOW_PLAYING);
                }
            }
            mNavigationView.setCheckedItem(R.id.nav_now_playing);

        } else if (FRAGMENT_PLAYLIST == newFragment) {
            EditPlaylistFragment fragment = new EditPlaylistFragment();
            fragment.setEnterTransition(new Fade());
            transaction.replace(R.id.sample_content_fragment, fragment, TAG_EDIT_PLAYLIST);
            mNavigationView.setCheckedItem(R.id.nav_current_playlist);
        } else if (FRAGMENT_ALL_PLAYLIST == newFragment) {
            AllPlaylistFragment fragment = new AllPlaylistFragment();
            transaction.replace(R.id.sample_content_fragment, fragment, TAG_ALL_PLAYLIST);
            mNavigationView.setCheckedItem(R.id.nav_all_playlists);
        } else if (FRAGMENT_DIRECTORY == newFragment) {
            DirectoryFragment fragment = (DirectoryFragment) mSupportFragmentManager.findFragmentByTag(TAG_DIRECTORY);
            if (null == fragment) {
                fragment = new DirectoryFragment();
            }
            fragment.setEnterTransition(new Fade());
            if (true == fragment.isVisible()) {
                transaction.show(fragment);
            } else {
                transaction.replace(R.id.sample_content_fragment, fragment, TAG_DIRECTORY);
            }
            mNavigationView.setCheckedItem(R.id.nav_current_folder);
        } else if (FRAGMENT_FAV_DIRS == newFragment) {
            FavFolderFragment fragment = new FavFolderFragment();
            fragment.setEnterTransition(new Fade());
            transaction.replace(R.id.sample_content_fragment, fragment, TAG_FAV_DIRS);
        } else if (FRAGMENT_ALL_SERVERS == newFragment) {

            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            // set the domain as the new DirNode
            mGlobalApp.getNodeNavigator().gotoNode(0);

            DirectoryFragment fragment = (DirectoryFragment) mSupportFragmentManager.findFragmentByTag(TAG_DIRECTORY);

            if (null == fragment) {
                fragment = new DirectoryFragment();
            }
            fragment.setEnterTransition(new Fade());
            if (true == fragment.isVisible()) {
                SharedPrefUtils.getInstance().saveFileListPosition(0);
                SharedPrefUtils.getInstance().saveDirListPosition(0);
                //SharedPrefUtils.getInstance().saveFoldersDisplayed(true);
                fragment.restartLoadersUpdateBreadCrumbs(DirectoryFragment.UPDATE_SHOW_FOLDERS);
                transaction.show(fragment);
            } else {
                transaction.replace(R.id.sample_content_fragment, fragment, TAG_DIRECTORY);
            }
        } else if (FRAGMENT_INITIAL_SCAN == newFragment) {
            InitialScanFragment fragment = (InitialScanFragment) mSupportFragmentManager.findFragmentByTag(TAG_INITIAL_SCAN);
            if (null == fragment) {
                fragment = new InitialScanFragment();
            }
            fragment.setEnterTransition(new Fade());
            transaction.replace(R.id.sample_content_fragment, fragment, TAG_INITIAL_SCAN);

        } else if (FRAGMENT_ONBOARDING == newFragment) {
            OnboardingFragment fragment = (OnboardingFragment) mSupportFragmentManager.findFragmentByTag(TAG_ONBOARDING);
            if (null == fragment) {
                fragment = new OnboardingFragment();
            }
            transaction.replace(R.id.sample_content_fragment, fragment, TAG_ONBOARDING);
        }

        transaction.commitAllowingStateLoss();
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_now_playing) {
            setFragment(FRAGMENT_NOW_PLAYING);

        } else if (id == R.id.nav_current_playlist) {

            // make sure the 'edit' playlist ID matches the 'current' playlist ID
            SharedPrefUtils.getInstance().saveEditingPlaylistId(mGlobalApp.getSongList().getPlaylistId());

            setFragment(FRAGMENT_PLAYLIST);

        } else if (id == R.id.nav_all_playlists) {
            setFragment(FRAGMENT_ALL_PLAYLIST);
        } else if (id == R.id.nav_current_folder) {
            setFragment(FRAGMENT_DIRECTORY);
        } else if (id == R.id.nav_servers) {
            setFragment(FRAGMENT_ALL_SERVERS);
        } else if (id == R.id.nav_discovery_status) {
            setFragment(FRAGMENT_INITIAL_SCAN);

//        } else if (id == R.id.nav_search) {

        } else if (id == R.id.nav_favorite_folders) {
            setFragment(FRAGMENT_FAV_DIRS);
        }

        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
