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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.JavaInfoFATUtils;
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

    @ClassRule
    public static IBMJava8Rule skipRule = new IBMJava8Rule();

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
        runTest(server, APP_NAME + '/' + SERVLET_NAME, getTestMethodSimpleName());
    }

    private static class IBMJava8Rule implements TestRule {

        @Override
        public Statement apply(Statement stmt, Description desc) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (shouldRun(desc)) {
                        stmt.evaluate();
                    }
                }
            };
        }

        public static boolean shouldRun(Description desc) {
            Class<?> c = desc == null ? IBMJava8Rule.class : desc.getTestClass();
            String m = (desc == null || desc.getMethodName() == null) ? "shouldRun" : desc.getMethodName();

            /*
             * Keystore is PKCS12 and was created using openjdk.
             * Our z/OS and SOE test systems use IBM JDK and will fail with
             * java.io.IOException: Invalid keystore format
             * since the keystore provider is SUN instead of IBMJCE.
             * Skip this test if JDK Vendor is IBM
             */
            if (!JavaInfoFATUtils.isIBMSDKJava8()) {
                Log.info(c, m, "Skipping tests because IBM JDK 8 does not support keystore format");
                return false;
            }

            return true;
        }

    }
}
