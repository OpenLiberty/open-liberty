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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Kerberos;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.adapter.ldap.LdapConstants;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests Kerberos bind (GSSAPI) for Ldap, using primarily the Kerberos keytab
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class KeytabBindTest extends CommonBindTest {

    private static final Class<?> c = KeytabBindTest.class;

    @BeforeClass
    public static void setStopMessages() {
        stopStrings = new String[] { "CWIML4520E", "CWWKS4345E", "CWIML4529E" };
    }

    /**
     * Run golden path tests for bindAuthMech=GSSAPI, a valid krbPrincipalName and valid keytab
     * and the contextPool disabled
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(LdapKerberosUtils.BIND_PASSWORD)
    public void basicLoginChecks() throws Exception {
        Log.info(c, testName.getMethodName(), "Run basic login checks with a standard configuration");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytab();
        addKerberosConfigAndKeytab(newServer);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        baselineTests();
    }

    /**
     * Run golden path tests for bindAuthMech=GSSAPI, a valid krbPrincipalName and valid keytab and
     * the contextPool enabled.
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(LdapKerberosUtils.BIND_PASSWORD)
    @AllowedFFDC("javax.naming.NamingException") // temporary, remove when Issue #16231 is fixed
    public void basicLoginChecksWithContextPool() throws Exception {
        Log.info(c, testName.getMethodName(), "Run basic login checks with a standard configuration");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytabWithContextPool();
        addKerberosConfigAndKeytab(newServer);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        baselineTests();
    }

    /**
     * Add a keytab file that is valid, but empty.
     *
     * @throws Exception
     */
    @AllowedFFDC("javax.security.auth.login.LoginException")
    @Test
    public void validButEmptyKeyTab() throws Exception {
        Log.info(c, testName.getMethodName(), "Run with a keytab that exists but is empty");
        File emptyCC = File.createTempFile("emptyKeytab", "keytab");
        emptyCC.deleteOnExit();

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytab();
        newServer.getLdapRegistries().add(ldap);

        Kerberos kerb = addKerberosConfig(newServer);
        kerb.keytab = emptyCC.getAbsolutePath();

        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login should fail with empty keytab file.");
        loginUserShouldFail();
        assertFalse("Expected to find Kerberos bind failure: CWIML4520E", server.findStringsInLogsAndTraceUsingMark("CWIML4520E").isEmpty());
    }

    /**
     * Add a keytab file that does not exist
     *
     * @throws Exception
     */
    @AllowedFFDC("javax.security.auth.login.LoginException")
    @Test
    public void noKeytab() throws Exception {
        Log.info(c, testName.getMethodName(), "Run with a keytab that does not exist");
        ServerConfiguration newServer = emptyConfiguration.clone();

        LdapRegistry ldap = getLdapRegistryForKeytab();
        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = addKerberosConfig(newServer);
        kerb.keytab = "thisfiledoesnotexist.keytab";
        updateConfigDynamically(server, newServer);

        assertFalse("Expected to find file not found: CWWKS4345E", server.findStringsInLogsAndTraceUsingMark("CWWKS4345E").isEmpty());

        loginUserShouldFail();
        assertFalse("Expected to find Kerberos bind failure: CWIML4520E", server.findStringsInLogsAndTraceUsingMark("CWIML4520E").isEmpty());

        kerb.keytab = keytabFile;
        updateConfigDynamically(server, newServer);
        loginUser();
    }

    /**
     * Start with a keytab defined, transition to a valid ticketcache, add a bad keytab
     * and then remove the keytab
     *
     * @throws Exception
     */
    @Test
    public void swapToTicketCache() throws Exception {
        Log.info(c, testName.getMethodName(), "Start with keytab and transition to ticketCache");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytab();
        newServer.getLdapRegistries().add(ldap);

        Kerberos kerb = addKerberosConfigAndKeytab(newServer);

        updateConfigDynamically(server, newServer);

        loginUser();

        Log.info(c, testName.getMethodName(), "Add valid ticketcache, login should be successful");
        ldap.setKrb5TicketCache(ticketCacheFile);
        updateConfigDynamically(server, newServer);

        loginUser();

        Log.info(c, testName.getMethodName(), "Update with bad keytab, should be successful with the valid ticketcache");
        kerb.keytab = "badFile.keytab";
        updateConfigDynamically(server, newServer);

        assertFalse("Expected to find file not found: CWWKS4345E", server.findStringsInLogsAndTraceUsingMark("CWWKS4345E").isEmpty());

        loginUser();

        Log.info(c, testName.getMethodName(), "Remove keytab, should be successful with the valid ticketcache");
        kerb.keytab = null;
        updateConfigDynamically(server, newServer);
        loginUser();
    }

    /**
     * Start with a valid keytab defined, swap to a bad keytab, then a valid keytab.
     *
     * ContextPool is enabled and should reset the contextPool when the keytab is updated.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "javax.naming.NamingException", "com.ibm.wsspi.security.wim.exception.WIMSystemException" })
    public void swapKeytabs() throws Exception {
        Log.info(c, testName.getMethodName(), "Start with a valid keytab");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytabWithContextPool();

        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = addKerberosConfigAndKeytab(newServer);

        updateConfigDynamically(server, newServer);

        loginUser();

        Log.info(c, testName.getMethodName(), "Update to a bad keytab, LdapRegistry should update and then fail to login");
        kerb.keytab = wrongUserKeytab;
        updateConfigDynamically(server, newServer);

        assertTrue("Should load " + wrongUserKeytab + ": CWWKS4345E", server.findStringsInLogsAndTraceUsingMark("CWWKS4345E").isEmpty());

        assertNotNull("Should have created the contextPool after the keytab update. Trace msg expected: " + LdapConstants.KERBEROS_UDPATE_MSG,
                      server.waitForStringInTrace(LdapConstants.KERBEROS_UDPATE_MSG));

        Log.info(c, testName.getMethodName(), "User login should fail -- search cache should also be cleared on keytab update");
        loginUserShouldFail();

        Log.info(c, testName.getMethodName(), "Update to a valid keytab, LdapRegistry should update and then successfully login");
        kerb.keytab = keytabFile;
        updateConfigDynamically(server, newServer);

        assertFalse("Should load " + keytabFile + ": CWWKS4346I", server.findStringsInLogsAndTraceUsingMark("CWWKS4346I").isEmpty());

        assertNotNull("Should have created the contextPool after the keytab update", server.waitForStringInTrace(LdapConstants.KERBEROS_UDPATE_MSG));

        loginUser();
    }
}
