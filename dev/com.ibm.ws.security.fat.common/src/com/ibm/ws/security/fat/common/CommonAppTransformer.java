/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import java.io.File;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.utils.FileUtils;

public class CommonAppTransformer {

    private final static Class<?> thisClass = CommonAppTransformer.class;

    /**
     * This method will update apps found in the build/libs/autoFVT/publish/servers/<*>/<appDirName> directories
     * It will back the original app up to <appName>.<ear|war|...> to <appName>.<ear|war|...>.orig
     * If we're running with JakartaEE9, we'll transform the app and we'll create <aapName>.<ear|war|...>.jakarta
     *
     * Another transform method will run once the server is copy from the build/libs/autoFVT/publish/servers directory over
     * to the build.image/wlp/usr/servers directory. That transform will copy <appName>.<ear|war|...>.orig or
     * <appName>.<ear|war|...>.jakarta to <appName>.<ear|war|...> depending on the repeat action (with JakartaEE9 or without)
     *
     * The original design would transform the files once they were copied over to the build.image/wlp/usr/servers directory, but
     * that results in the same applications being transformed many times (basically once for each each server in each test class)
     * That is more obvious, but, more time consuming
     *
     * We should be able to count on <app>.jakara being the transformed file
     * <app>.orig being the original file
     * <app> could be either and can't be trusted...
     *
     * @param appDirName
     *            - the directory to look in (usually dropins or test-apps)
     */
    private static void transformAppsInPublish(String appDirName) {

        String thisMethod = "transformAppsInPublish";

        File serverDir = new File(System.getProperty("user.dir") + "/publish/servers");

        Log.info(thisClass, thisMethod, serverDir.toString());

        try {
            if (serverDir.isDirectory()) {
                Log.info(thisClass, thisMethod, "Is a directory");
                String[] list = serverDir.list();
                for (String dir : list) {
                    String dirName = serverDir.toString() + "/" + dir;
                    Log.info(thisClass, thisMethod, "In publish/servers: " + dirName);
                    Log.info(thisClass, thisMethod, "Checking for files in: " + dirName + "/" + appDirName);
                    File appDir = new File(dirName, appDirName);
                    Log.info(thisClass, thisMethod, "Built appDir is: " + appDir.toString());
                    if (appDir.isDirectory()) {
                        Log.info(thisClass, thisMethod, appDir.toString() + " is really a directory");
                        String[] apps = appDir.list();
                        if (apps != null) {
                            for (String app : apps) {
                                Log.info(thisClass, thisMethod, "app: " + app);
                                if (app.endsWith(".orig") || app.endsWith(".jakarta")) {
                                    Log.info(thisClass, thisMethod, "Skipping");
                                    continue;
                                }
                                File srcFile = new File(appDir, app);
                                File theApp_jakartaVersion = new File(appDir, app + ".jakarta");
                                File origFile = new File(appDir, app + ".orig");
                                if (!origFile.exists()) {
                                    Log.info(thisClass, thisMethod, "Save file " + app + " as orig");
                                    FileUtils.copyDirectory(srcFile, origFile);
                                }
                                if (JakartaEE9Action.isActive()) {
                                    if (theApp_jakartaVersion.exists()) {
                                        Log.info(thisClass, thisMethod, "Transform skipped for app: " + theApp_jakartaVersion + " already exists!");
                                    } else {
                                        JakartaEE9Action.transformApp(srcFile.toPath());
                                        // copy the transformed jakarta file (from the "original file name" to the altered jakarta named file.
                                        FileUtils.copyDirectory(srcFile, theApp_jakartaVersion);
                                        FileUtils.recursiveDelete(srcFile);
                                    }
                                }
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            Log.error(thisClass, "transformAppsInDefaultDirs", e);
        }

    }

    /**
     * JakartaEE9 transform applications in the test code runtime source location
     */
    public static void transformAppsInPublish() {

        Log.info(thisClass, "transformAppsInPublish", "Will attempt to transform apps");
        transformAppsInPublish("dropins");
        transformAppsInPublish("test-apps");

    }

}
