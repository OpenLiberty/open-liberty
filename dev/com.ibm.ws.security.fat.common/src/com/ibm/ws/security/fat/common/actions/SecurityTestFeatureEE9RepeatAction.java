/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.actions;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.JakartaEE9Action;

public class SecurityTestFeatureEE9RepeatAction extends JakartaEE9Action {

    public static Class<?> thisClass = SecurityTestFeatureEE9RepeatAction.class;

    protected String complexId = JakartaEE9Action.ID;
    private TestMode testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;
    private boolean notAllowedOnWindows = false;

    public SecurityTestFeatureEE9RepeatAction() {

        complexId = JakartaEE9Action.ID;
        Log.info(thisClass, "instance", complexId);
        testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;
        notAllowedOnWindows = false;
        withID(complexId);
    }

    public SecurityTestFeatureEE9RepeatAction(String inNameExtension) {

        complexId = JakartaEE9Action.ID + "_" + inNameExtension;
        Log.info(thisClass, "instance", complexId);
        testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;
        notAllowedOnWindows = false;
        withID(complexId);
    }

    @Override
    public SecurityTestFeatureEE9RepeatAction withID(String id) {
        return (SecurityTestFeatureEE9RepeatAction) super.withID(id);
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
    public SecurityTestFeatureEE9RepeatAction liteFATOnly() {
        testRunMode = TestMode.LITE;
        return this;
    }

    @Override
    public SecurityTestFeatureEE9RepeatAction fullFATOnly() {
        testRunMode = TestMode.FULL;
        return this;
    }

    public SecurityTestFeatureEE9RepeatAction notOnWindows() {

        Log.info(thisClass, "notOnWindows", "set disallow on windows");
        notAllowedOnWindows = true;
        return this;
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
