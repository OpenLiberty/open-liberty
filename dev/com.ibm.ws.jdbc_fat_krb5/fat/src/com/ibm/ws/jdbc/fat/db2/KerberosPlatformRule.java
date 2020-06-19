/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.db2;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;

/**
 * Rule that automatically skips tests if we are running on a platform that does NOT
 * support Kerberos
 */
public class KerberosPlatformRule implements TestRule {

    @Override
    public Statement apply(Statement stmt, Description desc) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (shouldRun(desc)) {
                    stmt.evaluate();
                }
            }
        };
    }

    private static boolean shouldRun(Description desc) {
        // Kerberos is only supported on certain operating systems
        // Skip the tests if we are not on one of the supported OSes
        String os = System.getProperty("os.name", "UNKNOWN").toUpperCase();
        if (!os.contains("LINUX") && !os.contains("MAC OS")) {
            if (FATRunner.FAT_TEST_LOCALRUN) {
                throw new RuntimeException("Running on an unsupported os: " + os);
            } else {
                Log.info(desc.getTestClass(), desc.getMethodName(), "Skipping test because of unsupported os: " + os);
                return false;
            }
        }

        // Currently we only have Kerberos setup for Hotspot/OpenJ9. Need to skip on IBM JDK
        if (System.getProperty("java.vm.name").toUpperCase().contains("IBM J9")) {
            if (FATRunner.FAT_TEST_LOCALRUN) {
                throw new RuntimeException("Running on an unsupported JVM: " + JavaInfo.forCurrentVM().vendor());
            } else {
                Log.info(desc.getTestClass(), desc.getMethodName(), "Skipping test because of unsupported JVM: " + JavaInfo.forCurrentVM().vendor());
                return false;
            }
        }

        return true;
    }

}
