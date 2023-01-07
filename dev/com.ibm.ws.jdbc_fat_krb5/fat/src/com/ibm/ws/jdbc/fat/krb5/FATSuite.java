/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.krb5;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.Network;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jdbc.fat.krb5.containers.KerberosContainer;
import com.ibm.ws.jdbc.fat.krb5.containers.KerberosPlatformRule;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, // needed because kerberos is only supported on certain OSes
                DB2KerberosTest.class,
                PostgresKerberosTest.class,
                OracleKerberosTest.class,
                ErrorPathTest.class
})
public class FATSuite extends TestContainerSuite {

    public static Network network;
    public static KerberosContainer krb5;

    static {
        // Needed for IBM JDK 8 support.
        java.lang.System.setProperty("com.ibm.jsse2.overrideDefaultTLS", "true");
    }

    @BeforeClass
    public static void startKerberos() throws Exception {
        if (!KerberosPlatformRule.shouldRun(null)) {
            // bucket will not run any tests, skip
            return;
        }

        network = Network.newNetwork();
        krb5 = new KerberosContainer(network);
        krb5.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (!KerberosPlatformRule.shouldRun(null)) {
            // bucket will not run any tests, skip
            return;
        }

        Exception firstError = null;

        try {
            krb5.stop();
            network.close();
        } catch (Exception e) {
            if (firstError == null)
                firstError = e;
            Log.error(FATSuite.class, "tearDown", e);
        }

        if (firstError != null)
            throw firstError;
    }

}