package com.derekmorrison.networkmusicplayer.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.derekmorrison.networkmusicplayer.ui.GlobalApp;

/**
 * Created by Derek on 9/24/2016.
 */
public class Utility {

    private static Utility instance = null;

    private Utility() {}

    public static Utility getInstance() {
        if (instance == null)
            instance = new Utility();

        return instance;
    }

    // from: https://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
    public boolean isWiFiConnected() {
        boolean isWiFi = false;
        Context context = GlobalApp.getContext();

        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        }

        return isWiFi;
    }
}
