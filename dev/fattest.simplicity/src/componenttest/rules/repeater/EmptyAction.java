/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;

public class EmptyAction implements RepeatTestAction {

    private static final Class<?> c = EmptyAction.class;

    public static final String ID = "NO_MODIFICATION_ACTION";
    private TestMode testRunMode = TestMode.LITE;
    private boolean liteFATOnly = false;

    @Override
    public void setup() {}

    @Override
    public boolean isEnabled() {
        if (TestModeFilter.FRAMEWORK_TEST_MODE.compareTo(testRunMode) < 0) {
            Log.info(c, "isEnabled", "Skipping action '" + toString() + "' because the test mode " + testRunMode +
                                     " is not valid for current mode " + TestModeFilter.FRAMEWORK_TEST_MODE);
            return false;
        }
        if (liteFATOnly && TestModeFilter.FRAMEWORK_TEST_MODE.compareTo(TestMode.LITE) != 0) {
            Log.info(c, "isEnabled", "Skipping action '" + toString() + "' because the test mode " + testRunMode +
                                     " is not LITE and the test is marked to run in LITE FAT mode only.");
            return false;
        }

        return true;
    }

    /**
     * Run the {@link EmptyAction} only when the testing mode is {@link TestMode#FULL}.
     *
     * @return this instance
     */
    public RepeatTestAction fullFATOnly() {
        this.testRunMode = TestMode.FULL;
        liteFATOnly = false;
        return this;
    }

    /**
     * Run the {@link EmptyAction} only when the testing mode is {@link TestMode#LITE}.
     *
     * @return this instance.
     */
    public RepeatTestAction liteFATOnly() {
        this.testRunMode = TestMode.LITE;
        liteFATOnly = true;
        return this;
    }

    @Override
    public String toString() {
        return "No modifications";
    }

    @Override
    public String getID() {
        return ID;
    }

}
