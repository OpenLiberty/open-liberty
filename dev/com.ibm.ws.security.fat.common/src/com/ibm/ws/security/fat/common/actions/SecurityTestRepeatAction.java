/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.RepeatTestAction;

public class SecurityTestRepeatAction implements RepeatTestAction {

    public static Class<?> thisClass = SecurityTestRepeatAction.class;

    protected String nameExtension = "";
    private TestMode testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;
    private boolean onlyAllowedOnWindows = false;

    public SecurityTestRepeatAction() {

        nameExtension = "";
        testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;
        onlyAllowedOnWindows = false;
    }

    public SecurityTestRepeatAction(String inNameExtension) {

        nameExtension = inNameExtension;
        testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;
        onlyAllowedOnWindows = false;
    }

    @Override
    public boolean isEnabled() {
        Log.info(thisClass, "isEnabled", "testRunMode: " + testRunMode);
        Log.info(thisClass, "isEnabled", "nameExtension: " + nameExtension);
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
        if (OperatingSystem.WINDOWS != currentOS && onlyAllowedOnWindows) {
            Log.info(thisClass, "isEnabled", "Skipping action '" + toString() + "' because the tests are disabled on Windows");
            return false;
        }
        return true;
    }

    public SecurityTestRepeatAction liteFATOnly() {
        testRunMode = TestMode.LITE;
        return this;
    }

    public SecurityTestRepeatAction fullFATOnly() {
        testRunMode = TestMode.FULL;
        return this;
    }

    @Override
    public void setup() throws Exception {
    }

    public SecurityTestRepeatAction onlyOnWindows() {

        Log.info(thisClass, "onlyOnWindows", "only allow on windows");
        onlyAllowedOnWindows = true;
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.rules.repeater.RepeatTestAction#getID()
     */
    @Override
    public String getID() {

        if (nameExtension == null || nameExtension.equals("")) {
            return "NO_MODIFICATION_ACTION";
        } else {
            return nameExtension;
        }

    }

}

/*
 * To use, insert one of the following into your class - by doing so, each test case will have the string appended to its name
 *
 * @ClassRule
 * public static RepeatTests r = RepeatTests.with(new SecurityTestRepeatAction(<ConstantClassName>.<ConstantName>));
 *
 * @ClassRule
 * public static RepeatTests r = RepeatTests.with(new SecurityTestRepeatAction("someString"));
 *
 */
