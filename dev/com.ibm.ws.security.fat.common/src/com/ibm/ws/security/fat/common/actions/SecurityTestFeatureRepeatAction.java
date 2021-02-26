/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.actions;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.RepeatTestAction;

public class SecurityTestFeatureRepeatAction implements RepeatTestAction {

    public static Class<?> thisClass = SecurityTestFeatureRepeatAction.class;

    protected String nameExtension = "default";
    private TestMode testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;

    public SecurityTestFeatureRepeatAction(String inNameExtension) {

        nameExtension = inNameExtension;
        testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;

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
        return true;
    }

    public RepeatTestAction liteFATOnly() {
        testRunMode = TestMode.LITE;
        return this;
    }

    public RepeatTestAction fullFATOnly() {
        testRunMode = TestMode.FULL;
        return this;
    }

    @Override
    public void setup() throws Exception {
        nameExtension = "default";
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.rules.repeater.RepeatTestAction#getID()
     */
    @Override
    public String getID() {

        return nameExtension;

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