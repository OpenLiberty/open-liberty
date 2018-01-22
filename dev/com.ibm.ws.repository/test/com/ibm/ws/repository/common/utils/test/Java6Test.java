/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.common.utils.test;

import static org.junit.Assert.fail;

import org.junit.Test;

public class Java6Test {

    /**
     * This test checks that the tests are running on a Java 6 VM. This will only be true if
     * a Java 6 VM is available. Effectively the test is checking that the gradle build scripts are
     * invoking the java 6 vm correctly. Gradle will set SHOULD_USE_JAVA_6 if it knows about
     * a Java 6 VM. If the property is true, then this test can check that the VM running the tests is
     * a Java 6 one.
     */
    @Test
    public void testRunningWithJava6() {
        if (Boolean.getBoolean("SHOULD_USE_JAVA_6")) {
            String version = System.getProperty("java.version");
            if (version != null) {
                if (!version.startsWith("1.6")) {
                    fail("The test should run with java 6 but it is " + version);
                }
            }
        }
    }

}
