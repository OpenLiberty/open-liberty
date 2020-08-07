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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jdbc.fat.krb5.containers.DB2KerberosContainer;
import com.ibm.ws.jdbc.fat.krb5.containers.KerberosPlatformRule;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.krb5.db2.web.DB2KerberosTestServlet;

@RunWith(FATRunner.class)
public class DB2KerberosTest extends FATServletClient {

    private static final Class<?> c = DB2KerberosTest.class;

    public static final String KRB5_USER = "dbuser";

    public static final String APP_NAME = "krb5-db2-app";

    public static final DB2KerberosContainer db2 = new DB2KerberosContainer(FATSuite.network);

    @Server("com.ibm.ws.jdbc.fat.krb5")
    @TestServlet(servlet = DB2KerberosTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static KerberosPlatformRule skipRule = new KerberosPlatformRule();

    @BeforeClass
    public static void setUp() throws Exception {
        Path krbConfPath = Paths.get(server.getServerRoot(), "security", "krb5.conf");
        FATSuite.krb5.generateConf(krbConfPath);

        db2.start();

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jdbc.krb5.db2.web");

        server.addEnvVar("DB2_DBNAME", db2.getDatabaseName());
        server.addEnvVar("DB2_HOSTNAME", db2.getContainerIpAddress());
        server.addEnvVar("DB2_PORT", "" + db2.getMappedPort(50000));
        server.addEnvVar("DB2_USER", db2.getUsername());
        server.addEnvVar("DB2_PASS", db2.getPassword());
        server.addEnvVar("KRB5_USER", KRB5_USER);
        server.addEnvVar("KRB5_CONF", krbConfPath.toAbsolutePath().toString());
        List<String> jvmOpts = new ArrayList<>();
        jvmOpts.add("-Dsun.security.krb5.debug=true"); // Hotspot/OpenJ9
        jvmOpts.add("-Dcom.ibm.security.krb5.krb5Debug=true"); // IBM JDK
        server.setJvmOptions(jvmOpts);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Exception firstError = null;

        try {
            server.stopServer("CWWKS4345E: .*BOGUS_KEYTAB"); // expected by testBasicPassword
        } catch (Exception e) {
            firstError = e;
            Log.error(c, "tearDown", e);
        }
        try {
            db2.stop();
        } catch (Exception e) {
            if (firstError == null)
                firstError = e;
            Log.error(c, "tearDown", e);
        }

        if (firstError != null)
            throw firstError;
    }

    @Test
    @Mode(TestMode.FULL)
    public void testBasicPassword() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        String originalKeytab = config.getKerberos().keytab;
        try {
            Log.info(c, testName.getMethodName(), "Changing the keystore to an invalid value so that password from the <authData> gets used");
            config.getKerberos().keytab = "BOGUS_KEYTAB";
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

            FATServletClient.runTest(server, APP_NAME + "/DB2KerberosTestServlet", testName);
        } finally {
            Log.info(c, testName.getMethodName(), "Restoring original config");
            config.getKerberos().keytab = originalKeytab;
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        }
    }

}
