package com.ibm.ws.install.internal;

import java.io.IOException;
import java.util.HashMap;

public class ProgressBar {
    private static ProgressBar progressBar;

    private HashMap<String, Integer> methodMap;
    private static final StringBuilder res = new StringBuilder();;
    private static double counter;

    public static ProgressBar getInstance() {
        if (progressBar == null) {
            progressBar = new ProgressBar();
        }
        return progressBar;
    }

    private ProgressBar() {
        initMap(); // TODO remove later once we have other actions done, they will set their own
                   // method maps
        counter = 0;
    }

    // TODO auto scaling with method map
    public void setMethodMap(HashMap<String, Integer> methodMap) {
        this.methodMap = methodMap;
    }

    public int getMethodIncrement(String method) {
        if (methodMap.containsKey(method)) {
            return methodMap.get(method);
        }
        return 0;
    }

    /**
     * Initialize with default increment values for feature utility install features
     */
    private void initMap() {
        methodMap = new HashMap<>();

        methodMap.put("initializeMap", 10);
        methodMap.put("fetchJsons", 10);
        // in installFeature we have 80 units to work with
        methodMap.put("resolvedFeatures", 20);
        methodMap.put("fetchArtifacts", 20);
        methodMap.put("installFeatures", 30);
        methodMap.put("cleanUp", 10);
    }

    public void updateProgress(double increment) {
        counter += increment;

    }

    public void display() {
        String data = String.format("[%s] %4.2f%%\r", progress(counter), counter);
        try {
            System.out.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String progress(double pct) {
        res.delete(0, res.length());
        int numEquals = 2 * (int) (((pct + 9) / 10));
        for (int i = 0; i < numEquals; i++) {
            res.append('=');
        }
        while (res.length() < 20) {
            res.append(' ');
        }
        return res.toString();
    }

    public void finish() {
        System.out.print("\033[2K"); // Erase line content

    }

    public double getCounter() {
        return counter;
    }

}
