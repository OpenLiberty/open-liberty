/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.security.saml20.fat.commonTest.actions;

/**
 * The SAML FATs take too long to run if we try to run all of the tests with Jakarata EE9 enabled.
 *  So, we'll run the tests in lite mode with Jakarta EE9 and run the tests without it in full mode.
 */
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.JakartaEE9Action;

public class JakartaEE9SAMLRepeatAction extends JakartaEE9Action {

    public static Class<?> thisClass = JakartaEE9SAMLRepeatAction.class;

    protected String nameExtension = "default";
    private TestMode testRunMode = TestModeFilter.FRAMEWORK_TEST_MODE;

    public JakartaEE9SAMLRepeatAction() {

        super();

    }

    @Override
    public boolean isEnabled() {
        Log.info(thisClass, "isEnabled", "testRunMode: " + testRunMode);
        Log.info(thisClass, "isEnabled", "Overall test mode: " + TestModeFilter.FRAMEWORK_TEST_MODE);
        // allow if mode matches or mode not set
        if (testRunMode != null && (TestModeFilter.FRAMEWORK_TEST_MODE != testRunMode)) {
            Log.info(thisClass, "isEnabled", "Skipping action '" + toString() + "' because the test mode " + testRunMode +
                                             " is not valid for current mode " + TestModeFilter.FRAMEWORK_TEST_MODE);
            return false;
        }
        return true;
    }

    public JakartaEE9Action liteFATOnly() {
        testRunMode = TestMode.LITE;
        return this;
    }

}
