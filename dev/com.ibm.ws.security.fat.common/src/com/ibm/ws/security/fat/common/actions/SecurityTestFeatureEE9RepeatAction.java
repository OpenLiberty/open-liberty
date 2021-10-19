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
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.JakartaEE9Action;

public class SecurityTestFeatureEE9RepeatAction extends JakartaEE9Action {

    public static Class<?> thisClass = SecurityTestFeatureEE9RepeatAction.class;

    protected static String complexId = JakartaEE9Action.ID;
    private TestMode testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;

    public SecurityTestFeatureEE9RepeatAction(String inNameExtension) {

        complexId = JakartaEE9Action.ID + "_" + inNameExtension;
        testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;
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
        return true;
    }

    public static boolean isActive() {
        return RepeatTestFilter.isRepeatActionActive(complexId);
    }

    @Override
    public JakartaEE9Action liteFATOnly() {
        testRunMode = TestMode.LITE;
        return this;
    }

    @Override
    public JakartaEE9Action fullFATOnly() {
        testRunMode = TestMode.FULL;
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.rules.repeater.RepeatTestAction#getID()
     */
    @Override
    public String getID() {
        return complexId;
    }

}
