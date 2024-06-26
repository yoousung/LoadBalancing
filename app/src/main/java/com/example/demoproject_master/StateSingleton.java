package com.example.demoproject_master;

public class StateSingleton {

    // Singleton for the state management
    public static boolean waitInterval = false;
    public static boolean runScanning = false;
    private static StateSingleton INSTANCE = null;
    private StateSingleton() {};

    public static StateSingleton getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new StateSingleton();
        }
        return(INSTANCE);
    }
}
