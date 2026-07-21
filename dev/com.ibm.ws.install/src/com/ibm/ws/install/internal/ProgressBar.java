/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.internal;

import java.util.HashMap;

public class ProgressBar {
    private static ProgressBar progressBar;

    private static boolean activated = false;
    private HashMap<String, Double> methodMap;
    private static final StringBuilder res = new StringBuilder();;
    private static final int MAX_EQUALS = 20;
    private static final int MAX_LINE_LENGTH = ("[] 100.00%").length() + MAX_EQUALS;
    
    // ANSI escape codes for terminal control
    private static final String ANSI_CURSOR_UP = "\033[1A";
    private static final String ANSI_ERASE_LINE = "\033[2K";
    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_RED = "\033[31m";
    private static final String ANSI_GREEN = "\033[32m";
    private static final String ANSI_GREEN_BLINKING = "\033[32;5m";
    
    private static boolean ansiSupported = true;

    private static double counter;

    public static ProgressBar getInstance() {
        if (progressBar == null) {
            progressBar = new ProgressBar();
        }
        activated = true;
        return progressBar;
    }

    private ProgressBar() {
        initMap();
        counter = 0;
        InstallLogUtils.activateProgressBar();
        detectAnsiSupport();
        System.out.println();
    }
    
    /**
     * Detect if ANSI escape codes are supported in the current terminal.
     * Disables ANSI on Windows unless running in a modern terminal.
     */
    private void detectAnsiSupport() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String term = System.getenv("TERM");
        
        // Disable ANSI on older Windows terminals
        if (os.contains("win")) {
            // Windows 10+ with modern terminal support
            String wtSession = System.getenv("WT_SESSION");
            if (wtSession == null && (term == null || !term.contains("xterm"))) {
                ansiSupported = false;
            }
        }
    }

    // TODO auto scaling with method map
    public void setMethodMap(HashMap<String, Double> methodMap) {
        this.methodMap = methodMap;
    }

    public double getMethodIncrement(String method) {
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

        methodMap.put("initializeMap", 10.00);
        methodMap.put("fetchJsons", 10.00);
        // in installFeature we have 80 units to work with
        methodMap.put("resolvedFeatures", 20.00);
        methodMap.put("fetchArtifacts", 20.00);
        methodMap.put("installFeatures", 30.00);
        methodMap.put("cleanUp", 10.00);
    }

    public void updateMethodMap(String key, double val) {
        methodMap.put(key, val);
    }

    /**
     * Update the percentage on the progress by. After updating, log a message to see the progress bar
     * update itself.
     *
     * @param increment amount to increment by
     */
    public void updateProgress(double increment) {
        counter += increment;

    }

    public void clearProgress() {
        if (ansiSupported) {
            System.out.print(ANSI_CURSOR_UP + ANSI_ERASE_LINE + ANSI_RESET);
        }
        System.out.flush();
    }

    public void display() {
        String equals = progress(counter);

        StringBuilder dashes = new StringBuilder();
        for (int i = equals.length() - 1; i < MAX_EQUALS; i++) {
            dashes.append("-");
        }

        String data;
        if (ansiSupported) {
            data = String.format("%s<%s%s> %.0f%%%s",
                ANSI_RED,
                ANSI_GREEN_BLINKING + equals + ANSI_RESET + ANSI_RED,
                dashes.toString(),
                counter,
                ANSI_RESET);
        } else {
            // Fallback for terminals without ANSI support
            data = String.format("<%s%s> %.0f%%", equals, dashes.toString(), counter);
        }
        System.out.println(data);
    }

    private static String progress(double pct) {
        res.delete(0, res.length());
        int numEquals = 2 * (int) (((pct + 9) / 10));
        for (int i = 0; i < numEquals; i++) {
            res.append('=');
        }
        return res.toString();
    }

    public void finish() {
        if (ansiSupported) {
            System.out.println(ANSI_CURSOR_UP + ANSI_ERASE_LINE + ANSI_RESET);
            // clear newline on current line
            System.out.print(ANSI_CURSOR_UP + ANSI_ERASE_LINE + ANSI_RESET);
        }
        System.out.flush();
        InstallLogUtils.deactivateProgressBar();
    }

    public void finishWithError() {
        InstallLogUtils.deactivateProgressBar();
    }

    /**
     * Update the progress bar visually without having to log a message. Useful if you are doing
     * a task that requires constant progress bar updating without wanting to log the updates to INFO all the time.
     */
    public void manuallyUpdate() {
        clearProgress();
        display();
    }

    public double getCounter() {
        return counter;
    }

    public static boolean isActivated() {
        return activated;
    }

}
