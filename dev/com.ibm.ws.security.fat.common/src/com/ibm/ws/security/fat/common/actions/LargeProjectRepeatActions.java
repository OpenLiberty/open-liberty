/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.fat.common.actions;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
 
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE11Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatActions.EEVersion;
import componenttest.rules.repeater.RepeatTestAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.utils.LibertyServerUtils;

public class LargeProjectRepeatActions {

    public static Class<?> thisClass = LargeProjectRepeatActions.class;

    private static boolean doIDPTransform = false;

    /**
     * Create repeats for large security projects.
     * On all Platforms:
     * EE11 -> Lite if Java>=17 -> Full if Java >=17
     * EE10 -> Lite if Java == 11 -> Full if java>=11
     * EE9 -> Lite if Java == 8 -> Full if java>=8
     * EE7/EE8/default -> Full only
     *
     * On Windows:
     * Only a single EE repeat is run (the highest supported by the Java version)
     * to avoid timeouts due to slower Windows systems in the pipeline.
     *
     * @return repeat test instances
     */
    public static RepeatTests createEERepeats() {
        doIDPTransform = false;
        return createEERepeatsWorker(null, null, null, null, null);
    }

    public static RepeatTests createEESamlRepeats() {
        doIDPTransform = true;
        return createEERepeatsWorker(null, null, null, null, null);

    }

    public static RepeatTests createEERepeats(String addEE9Feature, String addEE10Feature, String addEE11Feature) {
        doIDPTransform = false;
        return createEERepeatsWorker(addEE9Feature, addEE10Feature, addEE11Feature, null, null);
    }

    public static RepeatTests createEESamlRepeats(String addEE9Feature, String addEE10Feature, String addEE11Feature) {
        doIDPTransform = true;
        return createEERepeatsWorker(addEE9Feature, addEE10Feature, addEE11Feature, null, null);
    }

    public static RepeatTests createEERepeats(String addEE9Feature, String addEE10Feature, String addEE11Feature, Set<String> removeFeatureList, Set<String> insertFeatureList) {
        doIDPTransform = false;
        return createEERepeatsWorker(addEE9Feature, addEE10Feature, addEE11Feature, removeFeatureList, insertFeatureList);
    }

    public static RepeatTests createEESamlRepeats(String addEE9Feature, String addEE10Feature, String addEE11Feature, Set<String> removeFeatureList, Set<String> insertFeatureList) {
        doIDPTransform = true;
        return createEERepeatsWorker(addEE9Feature, addEE10Feature, addEE11Feature, removeFeatureList, insertFeatureList);
    }

    public static RepeatTests createEERepeats(String addEE9Feature, String addEE10Feature, String addEE11Feature, Set<String> removeFeatureList, Set<String> insertFeatureList, String... serverPaths) {

        doIDPTransform = false;
        return createEERepeatsWorker(addEE9Feature, addEE10Feature, addEE11Feature, removeFeatureList, insertFeatureList, serverPaths);

    }

    public static RepeatTests createEESamlRepeats(String addEE9Feature, String addEE10Feature, String addEE11Feature, Set<String> removeFeatureList, Set<String> insertFeatureList, String... serverPaths) {

        doIDPTransform = true;
        return createEERepeatsWorker(addEE9Feature, addEE10Feature, addEE11Feature, removeFeatureList, insertFeatureList, serverPaths);

    }

    private static RepeatTests createEERepeatsWorker(String addEE9Feature, String addEE10Feature, String addEE11Feature, Set<String> removeFeatureList, Set<String> insertFeatureList, String... serverPaths) {

        RepeatTests rTests = null;
        boolean isLite = TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.LITE;
        boolean isWindows = isWindows();
        TreeSet<Integer> eeJavaBoundaries = new TreeSet<>(Arrays.asList(17, 11, 8));
        int javaMajorVer = JavaInfo.forCurrentVM().majorVersion();
        
        // Start with default so that it runs before any repeat transformations.
        if (!isLite) {
            Log.info(thisClass, "createLargeProjectRepeats", "Enabling the default EE7/EE8 test instance Mode: " + TestModeFilter.FRAMEWORK_TEST_MODE);
            rTests = addRepeat(rTests, FeatureReplacementAction.NO_REPLACEMENT());
        }
        
        switch (eeJavaBoundaries.floor(javaMajorVer)) {
        case 17:
            Log.info(thisClass, "createLargeProjectRepeats", "Enabling the EE11 test instance (Java >= 17, Mode: " + TestModeFilter.FRAMEWORK_TEST_MODE);
            rTests = addRepeatAndTransform(EEVersion.EE11, addEE11Feature, removeFeatureList, insertFeatureList, rTests, serverPaths);
            if (isLite || isWindows) {
                break;
            }
        case 11:
            Log.info(thisClass, "createLargeProjectRepeats", "Enabling the EE10 test instance (Java >= 11, Mode: " + TestModeFilter.FRAMEWORK_TEST_MODE);
            rTests = addRepeatAndTransform(EEVersion.EE10, addEE10Feature, removeFeatureList, insertFeatureList, rTests, serverPaths);
            if (isLite || isWindows) {
                break;
            }
        case 8:
            Log.info(thisClass, "createLargeProjectRepeats", "Enabling the EE9 test instance (Java >= 8, Mode: " + TestModeFilter.FRAMEWORK_TEST_MODE);
            rTests = addRepeatAndTransform(EEVersion.EE9, addEE9Feature, removeFeatureList, insertFeatureList, rTests, serverPaths);
            break;

        default:
            Log.info(thisClass, "createLargeProjectRepeats", "Invalid Java version detected. Version:" + javaMajorVer);
            break;
        }

        return rTests;
    }

    private static RepeatTests addRepeatAndTransform(EEVersion eeVersion, String eeFeature, Set<String> removeFeatureList, Set<String> insertFeatureList, RepeatTests rTests, String... serverPaths) {
        rTests = addRepeat(rTests, adjustFeatures(getActionID(eeVersion), eeFeature, removeFeatureList, insertFeatureList, serverPaths));
        if (doIDPTransform) {
            Log.info(thisClass, "addRepeatAndTransform", "did repeat transform");
            idpWarTransform(eeVersion);
        }
        return rTests;
    }

    private static String getActionID(EEVersion eeVersion) {
        String actionID = JakartaEE9Action.ID;
        if (eeVersion == EEVersion.EE10) {
            actionID = JakartaEE10Action.ID;
        } else if (eeVersion == EEVersion.EE11) {
            actionID = JakartaEE11Action.ID;
        }
        Log.info(thisClass, "getActionID", "ActionID: " + actionID);

        return actionID;
    }

    public static RepeatTests addRepeat(RepeatTests rTests, RepeatTestAction currentRepeat) {
        
        if (rTests == null) {
            return RepeatTests.with(currentRepeat);
        } else {
            return rTests.andWith(currentRepeat);
        }
    }

    /**
     * Create the requests level of EE action and then add or remove the requested features.
     *
     * @param featureType
     * @param addEEFeature
     * @param removeFeatureList
     * @param insertFeatureList
     * @return
     */
    public static FeatureReplacementAction adjustFeatures(String featureType, String addEEFeature, Set<String> removeFeatureList, Set<String> insertFeatureList, String... serverPaths) {
        FeatureReplacementAction featureAction = null;
        if (featureType.equals(JakartaEE9Action.ID)) {
            featureAction = FeatureReplacementAction.EE9_FEATURES();
        } else if (featureType.equals(JakartaEE10Action.ID)) {
            featureAction = FeatureReplacementAction.EE10_FEATURES();
        } else if (featureType.equals(JakartaEE11Action.ID)) {
            featureAction = FeatureReplacementAction.EE11_FEATURES();
        } else {
            Log.info(thisClass, "adjustFeatures", "Unknown feature type, " + featureType + ", defaulting to " + JakartaEE9Action.ID);
            featureAction = FeatureReplacementAction.EE9_FEATURES();
        }

        if (addEEFeature != null) {
            featureAction.alwaysAddFeature(addEEFeature);
        }
        if (removeFeatureList != null) {
            featureAction.removeFeatures(removeFeatureList);
        }
        if (insertFeatureList != null) {
            featureAction.addFeatures(insertFeatureList);
        }
        if (serverPaths != null && serverPaths.length != 0) {
            featureAction.forServerConfigPaths(serverPaths);
        }
        return featureAction;
    }

    public static void idpWarTransform(EEVersion eeVersion) {

        try {
            String currentPath = new java.io.File(".").getCanonicalPath();
            Log.info(thisClass, "idpWarTransform", "Current dir :" + currentPath);
            String shibDir = currentPath + "/publish/servers/com.ibm.ws.security.saml.sso-2.0_fat.shibboleth/idp-apps";
            Log.info(thisClass, "idpWarTransform", "shibDir: " + shibDir);

            File appDir = new java.io.File(LibertyServerUtils.makeJavaCompatible(shibDir));

            File[] list = null;
            try {
                if (appDir.isDirectory()) {
                    Log.info(thisClass, "idpWarTransform", "appDir is a directory");
                    list = appDir.listFiles();
                }
            } catch (Exception e) {
                Log.error(thisClass, "idpWarTransform", e);
            }
            if (list != null) {
                Log.info(thisClass, "idpWarTransform", "list is not null");
                for (File app : list) {
                    String fullAppName = shibDir + "/" + app.getName();
                    if (!app.getName().contains("3.3.1") && !app.getName().contains(eeVersion.toString())) {
                        Path appPathName = Paths.get(fullAppName);
                        Path appPathNewName = Paths.get(fullAppName + "." + eeVersion.toString());
                        Log.info(thisClass, "idpWarTransform", "From IDP war name: " + appPathName.toString());
                        Log.info(thisClass, "idpWarTransform", "To IDP war name: " + appPathNewName.toString());
                        JakartaEEAction.transformApp(appPathName, appPathNewName, eeVersion);
                    } else {
                        Log.info(thisClass, "idpWarTransform", "Skipping transform since we will only use the 3.3.1 version with Java 8");
                    }
                }
            }

        } catch (Exception e) {
            Log.info(thisClass, "idpWarTransform", "Failure trying to transform the idp wars" + e.getMessage());
            e.getStackTrace();
        }

    }

    public static String getParent(String dir) {
        Log.info(thisClass, "getParent", "Starting path: " + dir);
        return new java.io.File(dir).getParent();
    }

    /**
     * Determines if the current operating system is Windows.
     *
     * @return true if running on Windows, false otherwise
     */
    private static boolean isWindows() {
        OperatingSystem currentOS = null;
        try {
            currentOS = Machine.getLocalMachine().getOperatingSystem();
        } catch (Exception e) {
            Log.info(thisClass, "isWindows", "Encountered an exception trying to determine OS type: " + e.getMessage());
        }
        Log.info(thisClass, "isWindows", "OS: " + currentOS);
        return currentOS == OperatingSystem.WINDOWS;
    }
}
