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
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.adapter.ldap.LdapConstants;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests Kerberos bind (GSSAPI) for Ldap, testing with the config JVM prop for the Kerberos config file
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 9)
public class Krb5ConfigJVMProp extends CommonBindTest {

    private static final Class<?> c = Krb5ConfigJVMProp.class;

    @BeforeClass
    public static void setStopMessages() {
        stopStrings = new String[] { "" };
    }

    /**
     * Swap between the Kerberos defined config file and the JVM config file property.
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(LdapKerberosUtils.BIND_PASSWORD)
    public void swapToJVMConfigProp() throws Exception {
        Log.info(c, testName.getMethodName(), "Swap config from Kerberos file to JVM property");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytabWithContextPool();
        addKerberosConfigAndKeytab(newServer); // start with default config
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        loginUser();

        Log.info(c, testName.getMethodName(), "Add a valid config file with a new name as a JVM property, restart server to take effect");
        String altConfigFile = ApacheDSandKDC.createConfigFile("altConfig-", KDC_PORT, true, false);
        server.setJvmOptions(Arrays.asList("-Djava.security.krb5.conf=" + altConfigFile));
        server.restartServer();

        // Double check that login is still fine
        loginUser();

        Log.info(c, testName.getMethodName(), "Remove config from Kerberos element, login should still work by using the JVM property.");
        newServer.getKerberos().configFile = null;
        updateConfigDynamically(server, newServer);

        assertNotNull("Should have created the contextPool after the keytab update. Trace msg expected: " + LdapConstants.KERBEROS_UDPATE_MSG,
                      server.waitForStringInTrace(LdapConstants.KERBEROS_UDPATE_MSG));

        loginUser();

    }

}
