/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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

package com.ibm.ws.jpa.tests.spec10.injection.common;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.RepeatTestAction;

/**
 *
 */
public class RepeatWithJPA22Hibernate implements RepeatTestAction {
    private static final Class<?> c = RepeatWithJPA22Hibernate.class;
    public static final String ID = "JPA22_HIBERNATE";

    private TestMode testRunMode = TestMode.LITE;

    @Override
    public boolean isEnabled() {
        if (TestModeFilter.FRAMEWORK_TEST_MODE.compareTo(testRunMode) < 0) {
            Log.info(c, "isEnabled", "Skipping action '" + toString() + "' because the test mode " + testRunMode +
                                     " is not valid for current mode " + TestModeFilter.FRAMEWORK_TEST_MODE);
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Switch to JPA Container 2.2 feature and use Hibernate for JPA persistence provider";
    }

    @Override
    public void setup() throws Exception {
        RepeaterInfo.repeatPhase = "hibernate-cfg.xml";
    }

    @Override
    public String getID() {
        return ID;
    }

    public RepeatWithJPA22Hibernate fullFATOnly() {
        this.testRunMode = TestMode.FULL;
        return this;
    }
}
