/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap.fat.krb5;

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Kerberos;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests Kerberos bind (GSSAPI) for Ldap, using primarily the keytab
 *
 * The tests in this bucket use multiple registries.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 9)
public class KeytabBindMultiRegistryTest extends CommonBindTest {

    private static final Class<?> c = KeytabBindMultiRegistryTest.class;

    @BeforeClass
    public static void setStopMessages() {
        stopStrings = new String[] { "CWIML4520E", "CWIML4537E" };
    }

    /**
     * Run with two ldapRegistries, allowOpIfRepoDown is false. Verify that if the Kerberos
     * Ldap has failures, no users can login. When Kerberos Ldap recovers, both Ldap should
     * be successful
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "javax.naming.CommunicationException", "javax.security.auth.login.LoginException" })
    public void multiLdapRegistry() throws Exception {
        Log.info(c, testName.getMethodName(), "Run failover tests with two registries and allowOpIfRepoDown=false");
        try {
            ServerConfiguration newServer = emptyConfiguration.clone();
            LdapRegistry ldap = getLdapRegistryForKeytab();
            Kerberos kerb = addKerberosConfigAndKeytab(newServer);
            newServer.getLdapRegistries().add(ldap);

            Log.info(c, testName.getMethodName(), "Run ApacheDS restart tests");
            bodyOfMultiRegistryTest(newServer);

            Log.info(c, testName.getMethodName(), "Update with an bad keytab");
            kerb.keytab = wrongUserKeytab;
            updateConfigDynamically(server, newServer);

            Log.info(c, testName.getMethodName(), "Since allowOp=false, expected logins on both Ldap repos to fail");
            loginUserShouldFail();
            loginUserShouldFailUnboundID();

            Log.info(c, testName.getMethodName(), "Update to valid keytab");
            kerb.keytab = keytabFile;
            updateConfigDynamically(server, newServer);

            Log.info(c, testName.getMethodName(), "Both registries should login again successfully");
            loginUser();
            loginUserUnboundID();

        } finally {
            stopUnboundIDLdapServer();
        }
    }

    /**
     * Run with two ldapRegistries where allowOpIfRepoDown is true. Verify that if the Kerberos
     * Ldap has failures, the Simple bind Ldap can still log in. When Kerberos Ldap recovers, both Ldap should
     * be successful
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "javax.naming.CommunicationException", "javax.security.auth.login.LoginException" })
    public void multiLdapRegistryAllowOp() throws Exception {
        Log.info(c, testName.getMethodName(), "Run failover tests with two registries and allowOpIfRepoDown=true");
        try {
            ServerConfiguration newServer = emptyConfiguration.clone();
            LdapRegistry ldap = getLdapRegistryForKeytab();
            Kerberos kerb = addKerberosConfigAndKeytab(newServer);
            newServer.getLdapRegistries().add(ldap);

            Log.info(c, testName.getMethodName(), "Run ApacheDS restart tests");
            bodyOfMultiRegistryTestAllowOp(newServer);

            Log.info(c, testName.getMethodName(), "Update with an bad keytab");
            kerb.keytab = wrongUserKeytab;
            updateConfigDynamically(server, newServer);

            Log.info(c, testName.getMethodName(), "Since allowOp=false, expected login to fail on the Keberos Ldap and succeed on the simple bind Ldap");
            loginUserShouldFail();
            loginUserUnboundID();

            Log.info(c, testName.getMethodName(), "Update to valid keytab");
            kerb.keytab = keytabFile;
            updateConfigDynamically(server, newServer);

            Log.info(c, testName.getMethodName(), "Both registries should login successfully");
            loginUser();
            loginUserUnboundID();
        } finally {
            stopUnboundIDLdapServer();
        }
    }

}
