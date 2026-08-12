/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import componenttest.topology.impl.JavaInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MaximumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

import java.io.IOException;

/**
 * Java 8 test where we hit an NPE if the krb5Principal name was not found in the ticketCache
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@MaximumJavaLevel(javaLevel = 8)
public class TicketCacheBadPrincipalJava8 extends CommonBindTest {

    private static final Class<?> c = TicketCacheBadPrincipalJava8.class;

    @BeforeClass
    public static void setStopMessages() throws IOException {
        if(System.getProperty("os.name").equalsIgnoreCase("OS/400")){
            JavaInfo ji = JavaInfo.forServer(server);
            assumeThat(ji.serviceRelease()>8 || ji.fixpack() >= 65, is(true));
        }
        stopStrings = new String[] { "CWIML4507E", "CWWKS4347E", "CWIML4512E", "CWIML4520E" };
    }

    /**
     * Provide a principalName that doesn't exist in the cache
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.security.auth.login.LoginException" })
    @Test
    public void badPrincipalName() throws Exception {
        Log.info(c, testName.getMethodName(), "Run with a bad principal name");

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        ldap.setKrb5Principal("badPrincipalName");
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login expected to fail, config has a bad principalName");
        loginUserShouldFail();

        /*
         * The same base exception is not always thrown in Java8, multi options here, either confirms that we tried to use an
         * invalid principal name.
         */
        boolean foundBadPrincipalName = !server.findStringsInLogsAndTraceUsingMark("CWIML4512E").isEmpty();
        boolean foundKerberosLevelError = !server.findStringsInLogsAndTraceUsingMark("CWWKS4347E").isEmpty();
        /*
         * Sometimes the JDK logs a more specific error, java.io.IOException: Primary principals don't match, but throws a more generic
         * error: Unable to obtain password from user
         */
        boolean foundGenFailure = !server.findStringsInLogsAndTraceUsingMark("CWIML4520E").isEmpty();
        assertTrue("Expected to find Kerberos bind failure: Either `CWIML4512E` or `CWWKS4347E` or `CWIML4520E`",
                   foundBadPrincipalName || foundKerberosLevelError || foundGenFailure);

        if (foundKerberosLevelError) { // should be wrapped in a WIM level message.
            assertFalse("Expected to find Kerberos bind failure: CWIML4507E", server.findStringsInLogsAndTraceUsingMark("CWIML4507E").isEmpty());
        }
    }

}
