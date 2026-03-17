/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.fat.common.actions;

import java.util.Set;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE11Action;

public class SecurityTestFeatureEE11RepeatAction extends JakartaEE11Action {

    public static Class<?> thisClass = SecurityTestFeatureEE11RepeatAction.class;

    protected String complexId = JakartaEE11Action.ID;
    private TestMode testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;
    private boolean notAllowedOnWindows = false;

    public SecurityTestFeatureEE11RepeatAction() {

        super();
        complexId = JakartaEE11Action.ID;
        Log.info(thisClass, "instance", complexId);
        testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;
        notAllowedOnWindows = false;
        withID(complexId);

    }

    public SecurityTestFeatureEE11RepeatAction(String inNameExtension) {

        super();
        complexId = JakartaEE11Action.ID + "_" + inNameExtension;
        Log.info(thisClass, "instance", complexId);
        testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;
        notAllowedOnWindows = false;
        withID(complexId);
    }

    @Override
    public SecurityTestFeatureEE11RepeatAction withID(String id) {
        return (SecurityTestFeatureEE11RepeatAction) super.withID(id);
    }

    @Override
    public boolean isEnabled() {

        Log.info(thisClass, "isEnabled", "testRunMode: " + testRunMode);
        Log.info(thisClass, "isEnabled", "complexId: " + complexId);
        Log.info(thisClass, "isEnabled", "Overall test mode: " + TestModeFilter.FRAMEWORK_TEST_MODE);
        // allow if mode matches or mode not set
        if (testRunMode != null && (TestModeFilter.FRAMEWORK_TEST_MODE != testRunMode)) {
            Log.info(thisClass, "isEnabled", "Skipping action '" + toString() + "' because the test mode " + testRunMode +
                    " is not valid for current mode " + TestModeFilter.FRAMEWORK_TEST_MODE);
            return false;
        }

        // perform standard checks
        if (!super.isEnabled()) {
            return false;
        }

        // Some Security projects restrict some tests from running in certain modes on windows
        OperatingSystem currentOS = null;
        try {
            currentOS = Machine.getLocalMachine().getOperatingSystem();
        } catch (Exception e) {
            Log.info(thisClass, "isEnabled", "Encountered and exception trying to determine OS type - assume we'll need to run: " + e.getMessage());
        }
        Log.info(thisClass, "isEnabled", "OS: " + currentOS.toString());
        if (OperatingSystem.WINDOWS == currentOS && notAllowedOnWindows) {
            Log.info(thisClass, "isEnabled", "Skipping action '" + toString() + "' because the tests are disabled on Windows");
            return false;
        }
        return true;
    }

    @Override
    public SecurityTestFeatureEE11RepeatAction liteFATOnly() {
        testRunMode = TestMode.LITE;
        return this;
    }

    @Override
    public SecurityTestFeatureEE11RepeatAction fullFATOnly() {
        testRunMode = TestMode.FULL;
        return this;
    }

    public SecurityTestFeatureEE11RepeatAction notOnWindows() {

        Log.info(thisClass, "notOnWindows", "set disallow on windows");
        notAllowedOnWindows = true;
        return this;
    }

    @Override
    public SecurityTestFeatureEE11RepeatAction addFeature(String addFeature) {
        return (SecurityTestFeatureEE11RepeatAction) super.addFeature(addFeature);
    }

    @Override
    public SecurityTestFeatureEE11RepeatAction alwaysAddFeatures(Set<String> alwaysAddedFeatures) {
        return (SecurityTestFeatureEE11RepeatAction) super.alwaysAddFeatures(alwaysAddedFeatures);
    }

    @Override
    public SecurityTestFeatureEE11RepeatAction alwaysAddFeature(String alwaysAddedFeature) {
        Log.info(thisClass, "alwaysAddedFeature", alwaysAddedFeature);
        return (SecurityTestFeatureEE11RepeatAction) super.alwaysAddFeature(alwaysAddedFeature);
    }

    @Override
    public SecurityTestFeatureEE11RepeatAction forServerConfigPaths(String... serverPaths) {
        return (SecurityTestFeatureEE11RepeatAction) super.forServerConfigPaths(serverPaths);
    }

    @Override
    public SecurityTestFeatureEE11RepeatAction forceAddFeatures(boolean force) {
        return (SecurityTestFeatureEE11RepeatAction) super.forceAddFeatures(force);
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.rules.repeater.RepeatTestAction#getID()
     */
    @Override
    public String getID() {
        Log.info(thisClass, "getID", "complexId: " + complexId);
        return complexId;
    }

}
