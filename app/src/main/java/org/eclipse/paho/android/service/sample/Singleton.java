package org.eclipse.paho.android.service.sample;

import android.app.Activity;
import android.app.Fragment;
import android.support.v4.app.FragmentActivity;

/**
 * Created by 1 on 15.01.2017.
 */

public class Singleton {
    private static Singleton instance = null;
    public static ConnectionDetails connectionDetails;
    public static String clientHandle;
    public static Activity mainActivity;
    public static FragmentActivity publishActivity;
    public static FragmentActivity subscribeActivity;
    public static FragmentActivity historyActivity;
    public static String topic;
    public static int checked;
    public static boolean retained;
    public static boolean needToStop;

    protected Singleton() {
        // Exists only to defeat instantiation.
    }
    public static Singleton getInstance() {
        if(instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}