/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
package com.ibm.ws.security.wim.adapter.ldap.fat.krb5;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.Network;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                /*
                 * Currently all tests are > Java8 as ApacheDS fails to authenticate the Kerberos token on Java8
                 */
                Krb5ConfigTest.class,
                KeytabBindTest.class,
                KeytabBindLongRunTest.class,
                KeytabBindMultiRegistryTest.class,
                SimpleBindTest.class,
                TicketCacheBindMultiRegistryTest.class
/*
 * Do not add more tests to this suite or the FULL fat tends to time out on Window runs.
 */

})
public class FATSuite extends TestContainerSuite {
    /*
     * The ApacheDS Directory Service, Ldap and KDC are started globally in ApacheDSandKDC (beforeClass and afterClass).
     *
     * ApacheDS trace will appear in output.txt. To enable more ApacheDS trace, see the setupService method in ApacheDSandKDC.
     *
     */

    public static String KDC_REALM = "EXAMPLE.COM";

    public static Network network;
    public static LdapContainer ldapkrb5;

    static {
        // Needed for IBM JDK 8 support.
        java.lang.System.setProperty("com.ibm.jsse2.overrideDefaultTLS", "true");
    }

    @BeforeClass
    public static void startKerberos() throws Exception {
        network = Network.newNetwork();
        ldapkrb5 = new LdapContainer(network);
        ldapkrb5.start();
        //Wait 1 second for ldap / KDC to become ready
        Thread.sleep(1000);
        Log.info(FATSuite.class, "startKerberos", "Wait 1 second for ldap / KDC to become ready");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Exception firstError = null;

        try {
            //TODO: remove
            //Log.info(FATSuite.class, "tearDown", "Sleeping 1000 seconds to allow time to check the container logs");
            //Thread.sleep(1000000);
            ldapkrb5.stop();
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
