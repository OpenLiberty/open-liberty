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
package com.ibm.ws.jdbc.fat.sqlserver;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assume.assumeTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.ssl.SQLServerTestSSLServlet;

@Mode(FULL)
@RunWith(FATRunner.class)
public class SQLServerSSLTest extends FATServletClient {

    public static final String APP_NAME = "sqlserversslfat";
    public static final String SERVLET_NAME = "SQLServerTestSSLServlet";

    @Server("com.ibm.ws.jdbc.fat.sqlserver.ssl")
    @TestServlet(servlet = SQLServerTestSSLServlet.class, path = APP_NAME + '/' + SERVLET_NAME)
    public static LibertyServer server;

    private static final DockerImageName sqlserverImage = DockerImageName.parse("kyleaure/sqlserver-ssl:2019-CU10-ubuntu-16.04")//
                    .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server");

    @ClassRule
    public static MSSQLServerContainer<?> sqlserver = new MSSQLServerContainer<>(sqlserverImage) //
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "sqlserver")) //
                    .acceptLicense();

    @BeforeClass
    public static void setUp() throws Exception {
        FATSuite.setupDatabase(sqlserver, true);

        server.addEnvVar("DBNAME", FATSuite.DB_NAME);
        server.addEnvVar("HOST", sqlserver.getContainerIpAddress());
        server.addEnvVar("PORT", Integer.toString(sqlserver.getFirstMappedPort()));
        server.addEnvVar("USER", sqlserver.getUsername());
        server.addEnvVar("PASSWORD", sqlserver.getPassword());
        server.addEnvVar("TRUSTSTORE_PASS", "WalletPasswd123");

        // Create a normal Java EE application and export to server
        ShrinkHelper.defaultApp(server, APP_NAME, "web.ssl");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testConnectionWithSSLSecure() throws Exception {
        /*
         * Keystore is PKCS12 and was created using openjdk.
         * Our z/OS and SOE test systems use IBM JDK and will fail with
         * java.io.IOException: Invalid keystore format
         * since the keystore provider is SUN instead of IBMJCE.
         * Skip this test if JDK Vendor is IBM
         */
        assumeTrue(JavaInfo.forCurrentVM().vendor() != JavaInfo.Vendor.IBM);
        /*
         * On MAC OS the IBM JDK reports a vendor of SUN_ORACLE.
         * Ensure this test is skipped in that instance as well.
         */
        assumeTrue(System.getProperty("os.name").contains("Mac OS") && //
                   (JavaInfo.forCurrentVM().vendor() != JavaInfo.Vendor.SUN_ORACLE));
        runTest(server, APP_NAME + '/' + SERVLET_NAME, getTestMethodSimpleName());
    }
}
