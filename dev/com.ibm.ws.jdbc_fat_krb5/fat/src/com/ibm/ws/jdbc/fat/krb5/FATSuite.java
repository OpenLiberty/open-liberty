/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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

import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.Network;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jdbc.fat.krb5.containers.KerberosContainer;
import com.ibm.ws.jdbc.fat.krb5.rules.KerberosPlatformRule;

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

    static {
        Log.info(FATSuite.class, "<init>", "Setting overrideDefaultTLS to true, needed for IBM JDK 8 support.");
        java.lang.System.setProperty("com.ibm.jsse2.overrideDefaultTLS", "true");
    }

    public static Network network;
    public static KerberosContainer krb5;

    @BeforeClass
    public static void setup() throws Exception {
        // Manually apply rule so that the AlwaysPassesTest runs since having zero test results is considered an error
        if (KerberosPlatformRule.shouldRun(null)) {
            network = Network.newNetwork();
            krb5 = new KerberosContainer(network);
            krb5.start();
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (krb5 == null && network == null) {
            return; // Nothing to cleanup
        }

        Exception firstError = null;

        try {
            krb5.stop();
        } catch (Exception e) {
            firstError = e;
            Log.error(FATSuite.class, "teardown", e);
        } finally {
            network.close();
        }

        if (firstError != null)
            throw firstError;
    }

    /**
     * Requests the KDC to generate a keytable for this user.
     * Then returns the location the keytable was saved in the container.
     *
     * @param user the user to create the keytable for
     * @return the container location for the keytable
     * @throws Exception if generating the keytable failed
     */
    public static String requestKeyTable(final String user) throws Exception {
        if (EXTERNAL_KEY_TABLE.containsKey(user)) {
            return EXTERNAL_KEY_TABLE.get(user);
        }

        final String m = "requestKeyTable";
        String containerLocation = "/tmp/client_" + user + "_krb5.keytab";

        ExecResult result = krb5.execInContainer("kadmin.local", "-q", "ktadd -k " + containerLocation + " " + user);
        if (result.getExitCode() != 0) {
            Log.info(c, m, "STDOUT: " + result.getStdout());
            Log.info(c, m, "STDERR: " + result.getStderr());
            throw new IllegalStateException("Could not generate keytab file because exit code was: " + result.getExitCode() + " see logs for details.");
        } else {
            Log.info(c, m, "Keytab file for user: " + user + " has been generated in-container at: " + krb5.getContainerId() + ":" + containerLocation);
            EXTERNAL_KEY_TABLE.put(user, containerLocation);
        }

        return containerLocation;
    }

}