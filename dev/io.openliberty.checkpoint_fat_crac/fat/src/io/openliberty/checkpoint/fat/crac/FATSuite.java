/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat.crac;

import static componenttest.topology.utils.FATServletClient.getTestMethodSimpleName;

import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                CRaCResourceOrderTest.class,
                CRaCResourceFailCheckpointTest.class,
                CRaCResourceFailRestoreTest.class,
                CRaCResourceRequestNotSupportedTest.class,
                CRaCResourceRequestPhaseAfterAppStartTest.class,
                CRaCResourceRequestPhaseBeforeAppStartTest.class,
                CRaCResourceMultiAppTest.class,
                CRaCMXBeanTest.class
})

public class FATSuite {
    /**
     * Gets only the test method of the TestName without the class name
     * and without the repeating rule name.
     *
     * @param testName
     * @return the test method only
     */
    static String getTestMethodNameOnly(TestName testName) {
        String testMethodSimpleName = getTestMethodSimpleName(testName);
        // Sometimes the method name includes the class name; remove the class name.
        int dot = testMethodSimpleName.indexOf('.');
        if (dot != -1) {
            testMethodSimpleName = testMethodSimpleName.substring(dot + 1);
        }
        return testMethodSimpleName;
    }
}
