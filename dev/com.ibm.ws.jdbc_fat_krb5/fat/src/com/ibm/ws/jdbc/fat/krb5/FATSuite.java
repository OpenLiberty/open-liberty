/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, // needed because kerberos is only supported on certain OSes
                DB2KerberosTest.class,
                PostgresKerberosTest.class,
                OracleKerberosTest.class,
                ErrorPathTest.class
})
public class FATSuite {

    public static Network network;
    public static KerberosContainer krb5;

    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();

        // Filter out any external docker servers in the 'libhpike' cluster
        ExternalTestServiceDockerClientStrategy.serviceFilter = (svc) -> {
            return !svc.getAddress().contains("libhpike-dockerengine");
        };
    }

    public static final boolean REUSE_CONTAINERS = FATRunner.FAT_TEST_LOCALRUN && !ExternalTestServiceDockerClientStrategy.useRemoteDocker();

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
        } catch (Exception e) {
            if (firstError == null)
                firstError = e;
            Log.error(FATSuite.class, "tearDown", e);
        }
        if (!REUSE_CONTAINERS) {
            network.close();
        }

        if (firstError != null)
            throw firstError;
    }

}