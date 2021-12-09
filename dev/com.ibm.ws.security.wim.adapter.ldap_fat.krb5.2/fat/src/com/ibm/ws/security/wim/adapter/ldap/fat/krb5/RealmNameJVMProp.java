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
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Kerberos;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests Kerberos bind (GSSAPI) for Ldap, testing with the krb5 realm name JVM property
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 9)
public class RealmNameJVMProp extends CommonBindTest {

    private static final Class<?> c = RealmNameJVMProp.class;

    @BeforeClass
    public static void setStopMessages() {
        stopStrings = new String[] { "CWIML4520E" };
    }

    /**
     * Use a config without a realm name defined, we should fail until we add the JVM realm name property.
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(LdapKerberosUtils.BIND_PASSWORD)
    @AllowedFFDC({ "com.ibm.wsspi.security.wim.exception.WIMSystemException" })
    public void setRealmViaJVMProp() throws Exception {
        /*
         * Can only run this test if we were able to set the default port of 88 for the KDC. The
         * -Djava.security.krb5.kdc property will only connect on the default port (can't set a custom port). To test the
         * -Djava.security.krb5.realm, the -Djava.security.krb5.kdc property is also required.
         */
        assumeTrue(KDC_PORT == ApacheDSandKDC.DEFAULT_KDC_PORT);
        Log.info(c, testName.getMethodName(), "Define a realm name using the JVM propertry.");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytabWithContextPool();
        String altConfigFile = ApacheDSandKDC.createConfigFile("noRealmConfig-", KDC_PORT, false, false);
        Kerberos kerb = newServer.getKerberos();
        kerb.configFile = altConfigFile;
        kerb.keytab = keytabFile;
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        loginUserShouldFail();

        server.stopServer(stopStrings);

        Log.info(c, testName.getMethodName(), "Add a realm name as a JVM property, restart server to take effect");
        server.setJvmOptions(Arrays.asList("-Djava.security.krb5.realm=" + DOMAIN, "-Djava.security.krb5.kdc=localhost"));

        server.startServer();
        startupChecks();

        loginUser();

    }

}
