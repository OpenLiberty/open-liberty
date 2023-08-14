/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.extension.tests;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ibm.websphere.simplicity.LocalFile;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatActions;
import componenttest.rules.repeater.RepeatActions.EEVersion;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

public class CDIExtensionRepeatActions {

    public static final String BUNDLE_PATH = "publish/bundles/";

    public static final String SPI_XTOR_FAIL_EXTENSION_BUNDLE_ID = "cdi.spi.constructor.fail.extension";
    public static final String HELLOWORLD_EXTENSION_BUNDLE_ID = "cdi.helloworld.extension";
    public static final String CDI_SPI_EXTENSION_BUNDLE_ID = "cdi.spi.extension";
    public static final String CDI_INTERNALS_BUNDLE_ID = "cdi.internals";
    public static final String CDI_SPI_MISPLACED_BUNDLE_ID = "cdi.spi.misplaced";

    public static final String CDI_EXT_ID = "_CDI_EXT";
    public static final String EE7_PLUS_ID = EE7FeatureReplacementAction.ID + CDI_EXT_ID;
    public static final String EE8_PLUS_ID = EE8FeatureReplacementAction.ID + CDI_EXT_ID;
    public static final String EE9_PLUS_ID = JakartaEE9Action.ID + CDI_EXT_ID;
    public static final String EE10_PLUS_ID = JakartaEE10Action.ID + CDI_EXT_ID;

    public static String getBundlePath(String bundleName) {
        return BUNDLE_PATH + bundleName + ".jar";
    }

    public static String getBundleName(String bundleID, boolean jakarta) {
        return bundleID + (jakarta ? "-jakarta" : "");
    }

    public static String getFeatureName(String bundleID) {
        String name;
        if (isJEE7Active()) {
            name = getFeatureName(bundleID, EEVersion.EE7);
        } else if (isJEE8Active()) {
            name = getFeatureName(bundleID, EEVersion.EE8);
        } else if (isJEE9Active()) {
            name = getFeatureName(bundleID, EEVersion.EE9);
        } else if (isJEE10Active()) {
            name = getFeatureName(bundleID, EEVersion.EE10);
        } else {
            throw new RuntimeException("Unknown Repeat Version: " + RepeatTestFilter.getRepeatActionsAsString());
        }
        return name;
    }

    public static String getFeatureName(String bundleID, EEVersion eeVersion) {
        String name;
        if (eeVersion == EEVersion.EE7) {
            name = bundleID + "-1.2";
        } else if (eeVersion == EEVersion.EE8) {
            name = bundleID + "-1.2";
        } else if (eeVersion == EEVersion.EE9) {
            name = bundleID + "-3.0";
        } else if (eeVersion == EEVersion.EE10) {
            name = bundleID + "-3.0";
        } else {
            throw new RuntimeException("Unknown EE version: " + eeVersion);
        }
        return name;
    }

    public static final FeatureSet EE7_PLUS = EERepeatActions.EE7.addFeature("usr:" + getFeatureName(SPI_XTOR_FAIL_EXTENSION_BUNDLE_ID, EEVersion.EE7))
                                                                 .addFeature("usr:" + getFeatureName(HELLOWORLD_EXTENSION_BUNDLE_ID, EEVersion.EE7))
                                                                 .addFeature("usr:" + getFeatureName(CDI_SPI_EXTENSION_BUNDLE_ID, EEVersion.EE7))
                                                                 .addFeature(getFeatureName(CDI_INTERNALS_BUNDLE_ID, EEVersion.EE7))
                                                                 .build(EE7_PLUS_ID);

    public static final FeatureSet EE8_PLUS = EERepeatActions.EE8.addFeature("usr:" + getFeatureName(SPI_XTOR_FAIL_EXTENSION_BUNDLE_ID, EEVersion.EE8))
                                                                 .addFeature("usr:" + getFeatureName(HELLOWORLD_EXTENSION_BUNDLE_ID, EEVersion.EE8))
                                                                 .addFeature("usr:" + getFeatureName(CDI_SPI_EXTENSION_BUNDLE_ID, EEVersion.EE8))
                                                                 .addFeature(getFeatureName(CDI_INTERNALS_BUNDLE_ID, EEVersion.EE8))
                                                                 .build(EE8_PLUS_ID);

    public static final FeatureSet EE9_PLUS = EERepeatActions.EE9.addFeature("usr:" + getFeatureName(SPI_XTOR_FAIL_EXTENSION_BUNDLE_ID, EEVersion.EE9))
                                                                 .addFeature("usr:" + getFeatureName(HELLOWORLD_EXTENSION_BUNDLE_ID, EEVersion.EE9))
                                                                 .addFeature("usr:" + getFeatureName(CDI_SPI_EXTENSION_BUNDLE_ID, EEVersion.EE9))
                                                                 .addFeature(getFeatureName(CDI_INTERNALS_BUNDLE_ID, EEVersion.EE9))
                                                                 .build(EE9_PLUS_ID);

    public static final FeatureSet EE10_PLUS = EERepeatActions.EE10.addFeature("usr:" + getFeatureName(SPI_XTOR_FAIL_EXTENSION_BUNDLE_ID, EEVersion.EE10))
                                                                   .addFeature("usr:" + getFeatureName(HELLOWORLD_EXTENSION_BUNDLE_ID, EEVersion.EE10))
                                                                   .addFeature("usr:" + getFeatureName(CDI_SPI_EXTENSION_BUNDLE_ID, EEVersion.EE10))
                                                                   .addFeature(getFeatureName(CDI_INTERNALS_BUNDLE_ID, EEVersion.EE10))
                                                                   .build(EE10_PLUS_ID);

    //All CDI FeatureSets - must be descending order
    private static final FeatureSet[] ALL_SETS_ARRAY = { EE10_PLUS, EE9_PLUS, EE8_PLUS, EE7_PLUS };
    public static final List<FeatureSet> ALL = Collections.unmodifiableList(Arrays.asList(ALL_SETS_ARRAY));

    public static RepeatTests repeat(String server, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return RepeatActions.repeat(server, TestMode.FULL, ALL, firstFeatureSet, otherFeatureSets);
    }

    public static boolean isJakartaActive() {
        return isJEE9Active() || isJEE10Active();
    }

    public static boolean isJEE7Active() {
        return (RepeatTestFilter.isRepeatActionActive(EE7FeatureReplacementAction.ID));
    }

    public static boolean isJEE8Active() {
        return (RepeatTestFilter.isRepeatActionActive(EE8FeatureReplacementAction.ID));
    }

    public static boolean isJEE9Active() {
        return (RepeatTestFilter.isRepeatActionActive(JakartaEE9Action.ID));
    }

    public static boolean isJEE10Active() {
        return (RepeatTestFilter.isRepeatActionActive(JakartaEE10Action.ID));
    }

    public static void installUserExtension(LibertyServer server, String bundleID) throws Exception {
        installUserBundle(server, bundleID);
        installUserFeature(server, bundleID);
    }

    public static void uninstallUserExtension(LibertyServer server, String bundleID) throws Exception {
        uninstallUserBundle(server, bundleID);
        uninstallUserFeature(server, bundleID);
    }

    public static void installUserFeature(LibertyServer server, String bundleID) throws Exception {
        server.installUserFeature(getFeatureName(bundleID));
    }

    public static void installSystemFeature(LibertyServer server, String bundleID) throws Exception {
        server.installSystemFeature(getFeatureName(bundleID));
    }

    public static String transformUserBundle(LibertyServer server, String bundleID) throws Exception {
        String bundleName = getBundleName(bundleID, false);
        String originalPath = getBundlePath(bundleName);
        if (isJEE9Active()) {
            bundleName = getBundleName(bundleID, true);
            String newPath = getBundlePath(bundleName);
            JakartaEE9Action.transformApp(Paths.get(originalPath), Paths.get(newPath));
        } else if (isJEE10Active()) {
            bundleName = getBundleName(bundleID, true);
            String newPath = getBundlePath(bundleName);
            JakartaEE10Action.transformApp(Paths.get(originalPath), Paths.get(newPath));
        }

        return bundleName;
    }

    public static void installUserBundle(LibertyServer server, String bundleID) throws Exception {
        String bundleName = transformUserBundle(server, bundleID);
        server.installUserBundle(bundleName);
    }

    public static void uninstallUserBundle(LibertyServer server, String bundleID) throws Exception {
        String bundleName = getBundleName(bundleID, isJakartaActive());
        server.uninstallUserBundle(bundleName);
        if (isJakartaActive()) {
            //Destroy the old file the transformer created to prevent a collision when the next transformation happens.
            //This may or may not fix https://wasrtc.hursley.ibm.com:9443/jazz/resource/itemName/com.ibm.team.workitem.WorkItem/290609
            LocalFile bundleFile = new LocalFile(getBundlePath(bundleName));
            bundleFile.delete();
        }
    }

    public static void uninstallUserFeature(LibertyServer server, String bundleID) throws Exception {
        server.uninstallUserFeature(getFeatureName(bundleID));
    }

    public static void uninstallSystemFeature(LibertyServer server, String bundleID) throws Exception {
        server.uninstallSystemFeature(getFeatureName(bundleID));
    }

}
