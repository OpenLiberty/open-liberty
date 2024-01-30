/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.actions;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
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
     * On Windows, always run the default/empty/EE7/EE8 tests.
     * On other Platforms:
     * - if Java 8, run default/empty/EE7/EE8 tests.
     * - All other Java versions
     * -- If LITE mode, run EE9
     * -- If FULL mode, run EE10
     *
     * @return repeat test instances
     */
    public static RepeatTests createEE9OrEE10Repeats() {
        doIDPTransform = false;
        return createEE9OrEE10RepeatsWorker(null, null, null, null);
    }

    public static RepeatTests createEE9OrEE10SamlRepeats() {
        doIDPTransform = true;
        return createEE9OrEE10RepeatsWorker(null, null, null, null);

    }

    public static RepeatTests createEE9OrEE10Repeats(String addEE9Feature, String addEE10Feature) {
        doIDPTransform = false;
        return createEE9OrEE10RepeatsWorker(addEE9Feature, addEE10Feature, null, null);
    }

    public static RepeatTests createEE9OrEE10SamlRepeats(String addEE9Feature, String addEE10Feature) {
        doIDPTransform = true;
        return createEE9OrEE10RepeatsWorker(addEE9Feature, addEE10Feature, null, null);
    }

    public static RepeatTests createEE9OrEE10Repeats(String addEE9Feature, String addEE10Feature, Set<String> removeFeatureList, Set<String> insertFeatureList) {
        doIDPTransform = false;
        return createEE9OrEE10RepeatsWorker(addEE9Feature, addEE10Feature, removeFeatureList, insertFeatureList, null);
    }

    public static RepeatTests createEE9OrEE10SamlRepeats(String addEE9Feature, String addEE10Feature, Set<String> removeFeatureList, Set<String> insertFeatureList) {
        doIDPTransform = true;
        return createEE9OrEE10RepeatsWorker(addEE9Feature, addEE10Feature, removeFeatureList, insertFeatureList, null);
    }

    public static RepeatTests createEE9OrEE10Repeats(String addEE9Feature, String addEE10Feature, Set<String> removeFeatureList, Set<String> insertFeatureList, String... serverPaths) {

        doIDPTransform = false;
        return createEE9OrEE10RepeatsWorker(addEE9Feature, addEE10Feature, removeFeatureList, insertFeatureList, serverPaths);

    }

    public static RepeatTests createEE9OrEE10SamlRepeats(String addEE9Feature, String addEE10Feature, Set<String> removeFeatureList, Set<String> insertFeatureList, String... serverPaths) {

        doIDPTransform = true;
        return createEE9OrEE10RepeatsWorker(addEE9Feature, addEE10Feature, removeFeatureList, insertFeatureList, serverPaths);

    }

    public static RepeatTests createEE9OrEE10RepeatsWorker(String addEE9Feature, String addEE10Feature, Set<String> removeFeatureList, Set<String> insertFeatureList, String... serverPaths) {

        RepeatTests rTests = null;

        OperatingSystem currentOS = null;
        try {
            currentOS = Machine.getLocalMachine().getOperatingSystem();
        } catch (Exception e) {
            Log.info(thisClass, "createLargeProjectRepeats", "Encountered and exception trying to determine OS type - assume we'll need to run: " + e.getMessage());
        }
        Log.info(thisClass, "createLargeProjectRepeats", "OS: " + currentOS.toString());

        if (OperatingSystem.WINDOWS == currentOS) {
            Log.info(thisClass, "createLargeProjectRepeats", "Enabling the default EE7/EE8 test instance since we're running on Windows");
            rTests = addRepeat(rTests, new EmptyAction());
        } else {
            if (JavaInfo.forCurrentVM().majorVersion() > 8) {
                if (TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.LITE) {
                    Log.info(thisClass, "createLargeProjectRepeats", "Enabling the EE9 test instance (Not on Windows, Java > 8, Lite Mode)");
                    rTests = addRepeat(rTests, adjustFeatures(JakartaEE9Action.ID, addEE9Feature, removeFeatureList, insertFeatureList, serverPaths));
                    if (doIDPTransform) {
                        idpWarTransform(EEVersion.EE9);
                    }
                } else {
                    Log.info(thisClass, "createLargeProjectRepeats", "Enabling the EE10 test instance (Not on Windows, Java > 8, FULL Mode)");
                    rTests = addRepeat(rTests, adjustFeatures(JakartaEE10Action.ID, addEE10Feature, removeFeatureList, insertFeatureList, serverPaths));
                    if (doIDPTransform) {
                        idpWarTransform(EEVersion.EE10);
                    }
                }
            } else {
                Log.info(thisClass, "createLargeProjectRepeats", "Enabling the default EE7/EE8 test instance (Not on Windows, Java = 8, any Mode)");
                rTests = addRepeat(rTests, new EmptyAction());
            }
        }

        return rTests;
    }

    // We can add other methods for different complex rules

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
            featureAction = new JakartaEE9Action();
        } else if (featureType.equals(JakartaEE10Action.ID)) {
            featureAction = new JakartaEE10Action();
        } else {
            Log.info(thisClass, "adjustFeatures", "Unknown feature type, " + featureType + ", defaulting to " + JakartaEE9Action.ID);
            featureAction = new JakartaEE9Action();
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
            List<RepeatTestAction> actions = RepeatTestFilter.getRepeatActions();

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
}
