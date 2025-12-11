/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.security.fips.fat.tests.fips1403.server;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;
import io.openliberty.security.fips.fat.FIPSTestUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static io.openliberty.security.fips.fat.FIPSTestUtils.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
@SkipIfSysProp({SkipIfSysProp.OS_ZOS, SkipIfSysProp.OS_IBMI, SkipIfSysProp.OS_ISERIES})
public class FIPS1403ServerTest {

    public static final String SERVER_NAME = "FIPSServer";
    public static String expectedProvider="InvalidProvider";
    public static boolean isIBMJava8 = false;
    public static JavaInfo ji;
    public static boolean GLOBAL_FIPS=false;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ji = JavaInfo.forServer(server);
        assumeThat(FIPSTestUtils.validFIPS140_3Environment(ji), is(true));
        if (Boolean.parseBoolean(PrivHelper.getProperty("global.fips_140-3", "false"))) {
            Log.info(FIPS1403ServerTest.class,"setup","global.fips_140-3 is set, letting LibertyServer configure FIPS");
            GLOBAL_FIPS=true;
        }
        if (ji.majorVersion() > 8) {
            expectedProvider = "OpenJCEPlusFIPS";
            // temporarily enable Beta for Semeru
            server.addEnvVar("JVM_ARGS","-Dcom.ibm.ws.beta.edition=true");
        } else {
            expectedProvider = "IBMJCEPlusFIPS";
        }
        // Save configuration at this point, so each test can restore to this point so that we don't pollute each test
        server.saveServerConfiguration();
    }

    @Test
    public void serverFIPS140_3JVMArgsTest() throws Exception {
        Log.info(FIPS1403ServerTest.class,"setup","Setting FIPS140-3 JVM Options");
        HashMap<String, String> opts = new HashMap<>();
        //Semeru >=11
        if (ji.majorVersion() > 8) {
            server.copyFileToLibertyServerRoot("publish/resources", "resources", STANDALONE_FIPS_PROFILE_FILENAME);
            opts.put("-Dsemeru.fips", "true");
            opts.put("-Dsemeru.customprofile", "OpenJCEPlusFIPS.FIPS140-3-Custom");
            opts.put("-Djava.security.properties", server.getServerRoot() + "/resources/" + STANDALONE_FIPS_PROFILE_FILENAME);
        // IBM SDK 8
        } else {
            isIBMJava8 = true;
            opts.put("-Xenablefips140-3", null);
            opts.put("-Dcom.ibm.jsse2.usefipsprovider", "true");
            opts.put("-Dcom.ibm.jsse2.usefipsProviderName", "IBMJCEPlusFIPS");
        }
        server.setJvmOptions(opts);
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
    }

    @Test
    public void serverFIPS140_3EnvVarTest() throws Exception {
        if(isIBMJava8) {
            server.addEnvVar(ENABLE_FIPS140_3_ENV_VAR, "true");
        }else {
            server.copyFileToLibertyServerRoot("publish/resources", "resources", LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
            server.addEnvVar(ENABLE_FIPS140_3_ENV_VAR, server.getServerRoot() + "/resources/" + LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
        }
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
    }

    /**
     * FIPS should still be enabled, but will use just the baseline Liberty profile when starting
     * @throws Exception
     */
    @Test
    public void serverFIPS140_3EmptyEnvVarTest() throws Exception {
        Path path = Paths.get(server.getServerRoot() + "/"+ SERVER_ENV_FILE);
        String serverEnv = VAR_EXPANSION_ENV + System.lineSeparator() + ENABLE_FIPS140_3_ENV_VAR + "=\"\"";
        Files.write(path, serverEnv.getBytes(StandardCharsets.UTF_8));
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
    }

    @Test
    public void serverFIPS140_3DirectoryQuotedTest() throws Exception {
        server.copyFileToLibertyServerRoot("publish/resources", "resources" , LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
        Path path = Paths.get(server.getServerRoot() + "/"+ SERVER_ENV_FILE);
        String serverEnv = VAR_EXPANSION_ENV + System.lineSeparator() + ENABLE_FIPS140_3_ENV_VAR + "=\"" + server.getServerRoot() + "/resources/" + LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME + "\"";
        Files.write(path, serverEnv.getBytes(StandardCharsets.UTF_8));
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
    }

    /**
     * Test that a non-quoted path with a space in is correctly handled by Windows
     *
     * @throws Exception
     */
    @Test
    public void serverFIPS140_3DirectorySpaceNoQuotesTest() throws Exception {
        assumeThat(server.getMachine().getOperatingSystem(), is(OperatingSystem.WINDOWS));
        String testDir = "resources/test dir";
        server.copyFileToLibertyServerRoot("publish/resources", testDir , LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
        server.addEnvVar(ENABLE_FIPS140_3_ENV_VAR, server.getServerRoot()+"/" + testDir + "/" + LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
        server.startServer();

        checkServerLogForFipsEnablementMessage(server, expectedProvider);
    }

    @Test
    public void serverFIPS140_3DirectorySpaceQuotesTest() throws Exception {
        assumeThat(server.getMachine().getOperatingSystem(), not(OperatingSystem.WINDOWS));
        String testDir = "resources/test dir";
        server.copyFileToLibertyServerRoot("publish/resources", testDir , LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
        Path path = Paths.get(server.getServerRoot() + "/"+ SERVER_ENV_FILE);
        String serverEnv = VAR_EXPANSION_ENV + System.lineSeparator() + ENABLE_FIPS140_3_ENV_VAR + "=\"" + server.getServerRoot() + "/" + testDir + "/" + LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME + "\"";
        Files.write(path, serverEnv.getBytes(StandardCharsets.UTF_8));
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
    }

    @Test
    public void serverFIPS140_3DirectorySpaceSlashTest() throws Exception {
        assumeThat(server.getMachine().getOperatingSystem(), not(OperatingSystem.WINDOWS));
        String testDir = "resources/test\\ dir";
        server.copyFileToLibertyServerRoot("publish/resources", testDir , LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
        Path path = Paths.get(server.getServerRoot() + "/"+ SERVER_ENV_FILE);
        String serverEnv = VAR_EXPANSION_ENV + System.lineSeparator() + ENABLE_FIPS140_3_ENV_VAR + "=" + server.getServerRoot() + "/" + testDir + "/" + LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME + "";
        Files.write(path, serverEnv.getBytes(StandardCharsets.UTF_8));
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
    }


    @After
    public void teardown() throws Exception {
        server.stopServer();
        server.restoreServerConfiguration();
        // restore Server does not change the JVM options
        HashMap<String, String> opts = new HashMap<>();
        server.setJvmOptions(opts);
    }
}
