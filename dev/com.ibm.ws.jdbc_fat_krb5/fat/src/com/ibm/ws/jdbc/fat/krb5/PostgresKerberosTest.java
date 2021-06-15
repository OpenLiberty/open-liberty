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
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jdbc.fat.krb5.containers.KerberosPlatformRule;
import com.ibm.ws.jdbc.fat.krb5.containers.PostgresKerberosContainer;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.krb5.pg.web.PgKerberosTestServlet;

@RunWith(FATRunner.class)
public class PostgresKerberosTest extends FATServletClient {

    private static final Class<?> c = PostgresKerberosTest.class;

    public static final String APP_NAME = "krb5-pg-app";

    public static final PostgresKerberosContainer postgresql = new PostgresKerberosContainer(FATSuite.network);

    @Server("com.ibm.ws.jdbc.fat.krb5.postgresql")
    @TestServlet(servlet = PgKerberosTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeat = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action()
                                    .forServers("com.ibm.ws.jdbc.fat.krb5.postgresql")
                                    .fullFATOnly());

    @ClassRule
    public static KerberosPlatformRule skipRule = new KerberosPlatformRule();

    @BeforeClass
    public static void setUp() throws Exception {
        Path krbConfPath = Paths.get(server.getServerRoot(), "security", "krb5.conf");
        FATSuite.krb5.generateConf(krbConfPath);

        postgresql.start();

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jdbc.krb5.pg.web");

        server.addEnvVar("PG_DBNAME", postgresql.getDatabaseName());
        server.addEnvVar("PG_HOSTNAME", postgresql.getContainerIpAddress());
        server.addEnvVar("PG_PORT", "" + postgresql.getMappedPort(PostgresKerberosContainer.PG_PORT));
        server.addEnvVar("PG_USER", postgresql.getUsername());
        server.addEnvVar("PG_PASS", postgresql.getPassword());
        server.addEnvVar("KRB5_USER", postgresql.getKerberosUsername());
        server.addEnvVar("KRB5_PASS", postgresql.getKerberosPassword());
        server.addEnvVar("KRB5_CONF", krbConfPath.toAbsolutePath().toString());
        List<String> jvmOpts = new ArrayList<>();
        jvmOpts.add("-Dsun.security.krb5.debug=true"); // Hotspot/OpenJ9
        jvmOpts.add("-Dcom.ibm.security.krb5.krb5Debug=true"); // IBM JDK

        if (JavaInfo.JAVA_VERSION >= 9) {
            jvmOpts.add("--illegal-access=permit"); // Java 16 JEPS 396
        }
        server.setJvmOptions(jvmOpts);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Exception firstError = null;

        try {
            server.stopServer("CWWKS4345E: .*BOGUS_KEYTAB", // expected by testBasicPassword
                              "DSRA0304E", "DSRA0302E", "WTRN0048W"); // expected by testXARecovery
        } catch (Exception e) {
            firstError = e;
            Log.error(c, "tearDown", e);
        }
        try {
            postgresql.stop();
        } catch (Exception e) {
            if (firstError == null)
                firstError = e;
            Log.error(c, "tearDown", e);
        }

        if (firstError != null)
            throw firstError;
    }

}
