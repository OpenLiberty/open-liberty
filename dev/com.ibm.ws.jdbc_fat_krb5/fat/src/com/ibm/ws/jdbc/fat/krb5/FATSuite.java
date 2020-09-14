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

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, // needed because kerberos is only supported on certain OSes
                DB2KerberosTest.class,
                OracleKerberosTest.class
})
public class FATSuite {

    public static Network network;
    public static KerberosContainer krb5;

    public static final boolean REUSE_CONTAINERS = FATRunner.FAT_TEST_LOCALRUN && !ExternalTestServiceDockerClientStrategy.useRemoteDocker();

    @BeforeClass
    public static void startKerberos() throws Exception {
        if (!KerberosPlatformRule.shouldRun(null)) {
            // bucket will not run any tests, skip
            return;
        }

        // Allows local tests to switch between using a local docker client, to using a remote docker client.
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();

        // Filter out any external docker servers in the 'libhpike' cluster
        ExternalTestServiceDockerClientStrategy.serviceFilter = (svc) -> {
            return !svc.getAddress().contains("libhpike-dockerengine");
        };

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