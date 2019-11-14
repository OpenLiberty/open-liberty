package com.ibm.ws.install.internal;

import java.io.IOException;
import java.util.HashMap;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public class ProgressBar {
    private static ProgressBar progressBar;

    private HashMap<String, Integer> methodMap;
    private static final StringBuilder res = new StringBuilder();;
    private static final int MAX_EQUALS = 20;
    private static final int MAX_LINE_LENGTH = ("[] 100.00%").length() + MAX_EQUALS;
    private static final String ANSI_WHITE_BLINKING = "\033[37;5m";

    private static double counter;
    private final boolean isWindows = (System.getProperty("os.name").toLowerCase()).contains("win");
    // TODO remove this need for windows checking progress bar

    public static ProgressBar getInstance() {
        if (progressBar == null) {
            progressBar = new ProgressBar();
        }
        return progressBar;
    }

    private ProgressBar() {
        initMap();
        AnsiConsole.systemInstall();
        System.out.println();
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

//    public void clearProgress(boolean isWindows){
//        if(isWindows){
//            for(int i = 0; i < MAX_LINE_LENGTH;i ++){
//                System.out.print("\b");
//            }
//        } else {
//            System.out.print("\033[2K"); // Erase line content
//        }
//
//    }
    public void clearProgress(boolean isWindows) {
        System.out.print(Ansi.ansi().cursorUp(1).eraseLine().reset()); // Erase line content

}

//    public void display() {
//        String data = String.format("[%s] %4.2f%%\r", progress(counter), counter);
//        try {
//            System.out.write(data.getBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
    public void display() {
        String equals = progress(counter);

        StringBuilder dashes = new StringBuilder();
        for (int i = equals.length() - 1; i < MAX_EQUALS; i++) {
            dashes.append("-");
        }

        String data = String.format("%s<%s%s> %4.2f%%%s", Ansi.ansi().fg(Ansi.Color.RED),
                                    Ansi.ansi().a(ANSI_WHITE_BLINKING).a(equals).reset(), Ansi.ansi().fg(Ansi.Color.RED).a(dashes.toString()),
                                    counter, Ansi.ansi().reset());
        System.out.println(Ansi.ansi().a(data).reset());

    }

    private static String progress(double pct) {
        res.delete(0, res.length());
        int numEquals = 2 * (int) (((pct + 9) / 10));
        for (int i = 0; i < numEquals; i++) {
            res.append('=');
        }
//        while (res.length() < MAX_EQUALS) {
//            res.append(' ');
//        }
        return res.toString();
    }

//    public void finish() {
//        if (!isWindows)
//            System.out.print("\033[2K"); // Erase line content
//
//    }
    public void finish() {
        System.out.println(Ansi.ansi().cursorUp(1).eraseLine().reset()); // Erase line content
        AnsiConsole.systemUninstall();

    }

    public double getCounter() {
        return counter;
    }

}
