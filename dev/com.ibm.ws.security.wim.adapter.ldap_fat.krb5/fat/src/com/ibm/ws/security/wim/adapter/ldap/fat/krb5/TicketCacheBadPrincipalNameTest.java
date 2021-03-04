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

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests Kerberos bind (GSSAPI) for Ldap, specialty test for a principalName when
 * we can't find the realm name. We need to start from a "fresh" start because
 * we can't reload the config that "new KerberosPrincipal()" uses.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class TicketCacheBadPrincipalNameTest extends CommonBindTest {

    private static final Class<?> c = TicketCacheBadPrincipalNameTest.class;

    @BeforeClass
    public static void setStopMessages() {
        stopStrings = new String[] { "CWIML4507E", "CWIML4512E" };
    }

    /**
     * Run with a krb5Principal name that is not in the ticketcache, without and without a realm name and
     * with and without a config file.
     *
     * @throws Exception
     */
    @AllowedFFDC("javax.security.auth.login.LoginException")
    @Test
    public void badPrincipalName() throws Exception {
        Log.info(c, testName.getMethodName(), "Run with a keytab and bad krb5PrincipalName");
        ServerConfiguration newServer = emptyConfiguration.clone();

        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        ldap.setKrb5Principal("badPrincipalName1");
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        newServer.getKerberos().configFile = null; // use the default config file, which doesn't exist or won't have the realm name
        updateConfigDynamically(server, newServer);

        bodyOfBadPrincipleName(newServer, ldap, "CWIML4507E");
    }

}
