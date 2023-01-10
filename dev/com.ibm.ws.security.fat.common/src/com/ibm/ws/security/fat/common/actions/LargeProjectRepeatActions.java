/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
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
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTestAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;

public class LargeProjectRepeatActions {

    public static Class<?> thisClass = LargeProjectRepeatActions.class;

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
                    rTests = addRepeat(rTests, new JakartaEE9Action());
                } else {
                    Log.info(thisClass, "createLargeProjectRepeats", "Enabling the EE10 test instance (Not on Windows, Java > 8, FULL Mode)");
                    rTests = addRepeat(rTests, new JakartaEE10Action());
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

}
