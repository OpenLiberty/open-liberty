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
package io.openliberty.jakarta.enterprise.concurrent.tck;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.JavaInfo;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, //Need to have a passing test for Java 8, 11, and 17
                ConcurrentTckLauncherFull.class, //FULL MODE
                ConcurrentTckLauncherWeb.class //LITE MODE
})
public class FATSuite {

    public static boolean shouldRunSignatureTests(Class<?> testClass) {
        boolean result = false;
        String reason = "";

        try {
            if (!testClass.isAnnotationPresent(Mode.class)) {
                reason = testClass.getCanonicalName() + " is not run in full mode.";
                return result = false;
            }

            if (testClass.getAnnotation(Mode.class).value() != TestMode.FULL) {
                reason = testClass.getCanonicalName() + " is not run in full mode.";
                return result = false;
            }

            if (System.getProperty("os.name", "unknown").toLowerCase().contains("windows")) {
                reason = "signature test plugin not supported on Windows.";
                return result = false;
            }

            if (JavaInfo.JAVA_VERSION != 17 || JavaInfo.JAVA_VERSION != 21) {
                reason = "signature test not supported on non-LTS java versions: " + JavaInfo.JAVA_VERSION;
                return result = false;
            }

            //default option
            reason = "signature test can run as configured";
            return result = true;

        } finally {
            Log.info(testClass, "shouldRunSignatureTests", "Return: " + result + ", because " + reason);
        }

    }
}
