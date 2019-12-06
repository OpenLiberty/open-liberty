/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.internal;

import java.util.HashMap;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

public class ProgressBar {
    private static ProgressBar progressBar;

    private static boolean activated = false;
    private HashMap<String, Double> methodMap;
    private static final StringBuilder res = new StringBuilder();;
    private static final int MAX_EQUALS = 20;
    private static final int MAX_LINE_LENGTH = ("[] 100.00%").length() + MAX_EQUALS;
    private static final String ANSI_GREEN_BLINKING = "\033[32;5m";

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
        AnsiConsole.systemInstall();
        System.out.println();
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

    public void updateMethodMap(String key, double val){
        methodMap.put(key, val);
    }

    /**
     * Update the percentage on the progress by. After updating, log a message to see the progress bar
     * update itself.
     * @param increment amount to increment by
     */
    public void updateProgress(double increment) {
        counter += increment;

    }

    public void clearProgress() {
            System.out.print(Ansi.ansi().cursorUp(1).eraseLine().reset()); // Erase line content
            System.out.flush();
        }
    public void display() {

        String equals = progress(counter);

        StringBuilder dashes = new StringBuilder();
        for (int i = equals.length() - 1; i < MAX_EQUALS; i++) {
            dashes.append("-");
        }

        String data = String.format("%s<%s%s> %4.2f%%%s", Ansi.ansi().fg(Ansi.Color.RED),
                                    Ansi.ansi().a(ANSI_GREEN_BLINKING).a(equals).reset(), Ansi.ansi().fg(Ansi.Color.RED).a(dashes.toString()),
                                    counter, Ansi.ansi().reset());
        System.out.println(Ansi.ansi().a(data).reset());

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
        System.out.println(Ansi.ansi().cursorUp(1).eraseLine().reset()); // Erase line content
        // clear newline on current line
        System.out.print(Ansi.ansi().cursorUp(1).eraseLine().reset()); // Erase line content
        System.out.flush();
        InstallLogUtils.deactivateProgressBar();
        AnsiConsole.systemUninstall();
    }

    public void finishWithError(){
        InstallLogUtils.deactivateProgressBar();
        AnsiConsole.systemUninstall();
    }

    /**
     * Update the progress bar visually without having to log a message. Useful if you are doing
     * a task that requires constant progress bar updating without wanting to log the updates to INFO all the time.
     */
    public void manuallyUpdate(){
        clearProgress();
        display();
    }

    public double getCounter() {
        return counter;
    }

    public static boolean isActivated(){
        return activated;
    }



}
