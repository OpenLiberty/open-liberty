/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.krb5;

import static componenttest.annotation.SkipForSecurity.FIPS_140_3;
import static componenttest.annotation.SkipForSecurity.SEMERU;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jdbc.fat.krb5.containers.PostgresKerberosContainer;
import com.ibm.ws.jdbc.fat.krb5.rules.KerberosPlatformRule;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForSecurity;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.krb5.pg.web.PgKerberosTestServlet;

@SkipForSecurity(property = FIPS_140_3, runtimeName = SEMERU)
@RunWith(FATRunner.class)
public class PostgresKerberosTest extends FATServletClient {

    public static final String APP_NAME = "krb5-pg-app";

    @Server("com.ibm.ws.jdbc.fat.krb5.postgresql")
    @TestServlet(servlet = PgKerberosTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    public static PostgresKerberosContainer postgresql = new PostgresKerberosContainer(FATSuite.network);

    public static RepeatTests repeat = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action()
                                    .forServers("com.ibm.ws.jdbc.fat.krb5.postgresql")
                                    .fullFATOnly())
                    .andWith(new JakartaEE10Action()
                                    .forServers("com.ibm.ws.jdbc.fat.krb5.postgresql")
                                    .fullFATOnly());

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(KerberosPlatformRule.instance()).around(postgresql).around(repeat);

    @BeforeClass
    public static void setUp() throws Exception {
        Path krbConfPath = Paths.get(server.getServerRoot(), "security", "krb5.conf");
        FATSuite.krb5.generateConf(krbConfPath);

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jdbc.krb5.pg.web");

        server.addEnvVar("PG_DBNAME", postgresql.getDatabaseName());
        server.addEnvVar("PG_HOSTNAME", postgresql.getHost());
        server.addEnvVar("PG_PORT", "" + postgresql.getFirstMappedPort());
        server.addEnvVar("PG_USER", postgresql.getUsername());
        server.addEnvVar("PG_PASS", postgresql.getPassword());
        server.addEnvVar("KRB5_PRIN", postgresql.getKerberosPrinciple());
        server.addEnvVar("KRB5_USER", postgresql.getKerberosUsername());
        server.addEnvVar("KRB5_PASS", postgresql.getKerberosPassword());
        server.addEnvVar("KRB5_CONF", krbConfPath.toAbsolutePath().toString());

        List<String> jvmOpts = new ArrayList<>();
        jvmOpts.add("-Dsun.security.krb5.debug=true"); // Hotspot/OpenJ9
        jvmOpts.add("-Dcom.ibm.security.krb5.krb5Debug=true"); // IBM JDK
        jvmOpts.add("-Dsun.security.jgss.debug=true");
        server.setJvmOptions(jvmOpts);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKS4345E: .*BOGUS_KEYTAB", // expected by testBasicPassword
                          "DSRA0304E", "DSRA0302E", "WTRN0048W"); // expected by testXARecovery
    }

}
