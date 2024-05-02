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
import org.testcontainers.containers.Network;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class LdapKrb5TestContainerSuite extends TestContainerSuite {

    public static Network network;
    public static KerberosContainer kerberos;
    public static LdapContainer ldap;

    static {
        // Needed for IBM JDK 8 support.
        java.lang.System.setProperty("com.ibm.jsse2.overrideDefaultTLS", "true");
    }

    @BeforeClass
    public static void startContainers() throws Exception {
        network = Network.newNetwork();
        kerberos = new KerberosContainer(network);
        kerberos.start();
        ldap = new LdapContainer(network);
        ldap.start();
        //Wait 1 second for ldap / KDC to become ready
        Thread.sleep(1000);
        Log.info(LdapKrb5TestContainerSuite.class, "startKerberos", "Wait 1 second for ldap / KDC to become ready");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        ldap.stop();
        kerberos.stop();
        network.close();
    }
}
