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

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.MinimumJavaLevel;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static io.openliberty.security.fips.fat.FIPSTestUtils.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
@SkipIfSysProp({SkipIfSysProp.OS_IBMI, SkipIfSysProp.OS_ISERIES})
public class FIPS1403ServerTest {

    private static final Class<?> c = FIPS1403ServerTest.class;

    public static final String SERVER_NAME = "FIPSServer";
    public static String expectedProvider="InvalidProvider";
    public static boolean isIBMJava8 = false;
    public static JavaInfo ji;
    public static boolean GLOBAL_FIPS=false;
    public static final String OPEN_JCE_PLUS_FIPS_PROVIDER = "OpenJCEPlusFIPS";
    public static final String IBM_JCE_PLUS_FIPS_PROVIDER = "IBMJCEPlusFIPS";
    private static Path serverEnVPath = null;

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
            expectedProvider = OPEN_JCE_PLUS_FIPS_PROVIDER;
        } else {
            expectedProvider = IBM_JCE_PLUS_FIPS_PROVIDER;
        }
        // Save configuration at this point, so each test can restore to this point so that we don't pollute each test
        server.saveServerConfiguration();
        serverEnVPath = Paths.get(server.getServerRoot() + "/"+ SERVER_ENV_FILE);
    }

    @Test
    public void serverFIPS140_3JVMArgsTest() throws Exception {
        Log.info(FIPS1403ServerTest.class,"setup","Setting FIPS140-3 JVM Options");
        HashMap<String, String> opts = new HashMap<>();
        //Semeru >=11
        if (expectedProvider.equals(OPEN_JCE_PLUS_FIPS_PROVIDER)) {
            server.copyFileToLibertyServerRoot("publish/resources", "resources", STANDALONE_FIPS_PROFILE_FILENAME);
            opts.put("-Dsemeru.fips", "true");
            opts.put("-Dsemeru.customprofile", "OpenJCEPlusFIPS.FIPS140-3-Custom");
            opts.put("-Djava.security.properties", server.getServerRoot() + "/resources/" + STANDALONE_FIPS_PROFILE_FILENAME);
        // IBM SDK 8
        } else {
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
        if (expectedProvider.equals(OPEN_JCE_PLUS_FIPS_PROVIDER)) {
            server.copyFileToLibertyServerRoot("publish/resources", "resources", LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
            server.addEnvVar(ENABLE_FIPS140_3_ENV_VAR, server.getServerRoot() + "/resources/" + LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
        }else {
            server.addEnvVar(ENABLE_FIPS140_3_ENV_VAR, "true");
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
        assumeThat(server.getMachine().getOperatingSystem(), not(OperatingSystem.ZOS));
        String serverEnv = VAR_EXPANSION_ENV + System.lineSeparator() + ENABLE_FIPS140_3_ENV_VAR + "=\"\"";
        Files.write(serverEnVPath, serverEnv.getBytes());
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
    }

    @Test
    @MinimumJavaLevel(javaLevel=11)
    public void serverFIPS140_3DirectoryQuotedTest() throws Exception {
        assumeThat(server.getMachine().getOperatingSystem(), not(OperatingSystem.ZOS));
        server.copyFileToLibertyServerRoot("publish/resources", "resources" , LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
        String serverEnv = VAR_EXPANSION_ENV + System.lineSeparator() + ENABLE_FIPS140_3_ENV_VAR + "=\"" + server.getServerRoot() + "/resources/" + LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME + "\"";
        Files.write(serverEnVPath, serverEnv.getBytes());
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
    }

    /**
     * Test that a non-quoted path with a space in is correctly handled by Windows
     *
     * @throws Exception
     */
    @Test
    @MinimumJavaLevel(javaLevel=11)
    public void serverFIPS140_3DirectorySpaceNoQuotesTest() throws Exception {
        assumeThat(server.getMachine().getOperatingSystem(), not(anyOf(is(OperatingSystem.WINDOWS), is(OperatingSystem.ZOS))));
        String testDir = "resources/test dir";
        server.copyFileToLibertyServerRoot("publish/resources", testDir , LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
        server.addEnvVar(ENABLE_FIPS140_3_ENV_VAR, server.getServerRoot()+"/" + testDir + "/" + LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
        server.startServer();

        checkServerLogForFipsEnablementMessage(server, expectedProvider);
    }

    @Test
    @MinimumJavaLevel(javaLevel=11)
    public void serverFIPS140_3DirectorySpaceQuotesTest() throws Exception {
        assumeThat(server.getMachine().getOperatingSystem(), not(anyOf(is(OperatingSystem.WINDOWS), is(OperatingSystem.ZOS))));
        String testDir = "resources/test dir";
        server.copyFileToLibertyServerRoot("publish/resources", testDir , LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
        String serverEnv = VAR_EXPANSION_ENV + System.lineSeparator() + ENABLE_FIPS140_3_ENV_VAR + "=\"" + server.getServerRoot() + "/" + testDir + "/" + LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME + "\"";
        Files.write(serverEnVPath, serverEnv.getBytes(StandardCharsets.UTF_8));
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
    }

    @Test
    @MinimumJavaLevel(javaLevel=11)
    public void serverFIPS140_3DirectorySpaceSlashTest() throws Exception {
        assumeThat(server.getMachine().getOperatingSystem(), not(anyOf(is(OperatingSystem.WINDOWS), is(OperatingSystem.ZOS))));
        String testDir = "resources/test\\ dir";
        server.copyFileToLibertyServerRoot("publish/resources", testDir , LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME);
        String serverEnv = VAR_EXPANSION_ENV + System.lineSeparator() + ENABLE_FIPS140_3_ENV_VAR + "=" + server.getServerRoot() + "/" + testDir + "/" + LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME + "";
        Files.write(serverEnVPath, serverEnv.getBytes(StandardCharsets.UTF_8));
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
    }

    @Test
    public void fips140_3PacakageTest() throws Exception {
        Machine machine = server.getMachine();
        String[] parameters = new String[]{"package", server.getServerName()};
        ProgramOutput po = machine.execute(server.getInstallRoot()+"/bin/server", parameters);
        Log.info(FIPS1403ServerTest.class, "fips140_3PacakageTest", "Executed securityUtility configureFIPS command: "+ po.getCommand());
        Log.info(FIPS1403ServerTest.class, "fips140_3PacakageTest", "Result: "+ po.getStdout());
        Log.info(FIPS1403ServerTest.class, "fips140_3PacakageTest", "Error: "+ po.getStderr());
        assertEquals("Package command failed with a non-zero return code", 0, po.getReturnCode());

        boolean foundFipsSecurityFile = false;
        if(!server.getMachine().getOperatingSystem().equals(OperatingSystem.ZOS)) {
            Path zipPath = Paths.get(server.getServerRoot() + "/" + server.getServerName() + ".zip");
            try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements() && !foundFipsSecurityFile) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.endsWith(LIBERTY_BASE_FIPS_PROFILE_FILENAME)) {
                        foundFipsSecurityFile = true;
                    }
                }
            }
        // ZOS packaging produces `.pax` files instead of `.zip`
        } else {
            Path paxPath =  Paths.get(server.getServerRoot() + "/" + server.getServerName() + ".pax");
            // run pax command without `r` or `w` to get a list of the contents
            ProgramOutput paxcmd = server.getMachine().execute("pax -ppx -f " + paxPath);
            Log.info(c, "unpax server package", " command:" + paxcmd.getCommand());
            Log.info(c, "unpax server package", " command exit code: " + paxcmd.getReturnCode());
            String[] contents = paxcmd.getStdout().split(System.lineSeparator());
            Log.info(c, "pax list package contents" , "size: " + contents.length);
            for(String line : contents) {
                if (line.endsWith(LIBERTY_BASE_FIPS_PROFILE_FILENAME)) {
                    foundFipsSecurityFile = true;
                    break;
                }
            }
        }
        assertTrue("Did not locate the base Liberty Profile in the zip file", foundFipsSecurityFile);
    }

    @Test
    public void fips140_3MinifiedPackageTest() throws Exception{
        assumeThat(Files.exists(Paths.get(server.getInstallRoot() + "/lib/extract")), is(true));
        Machine machine = server.getMachine();
        String[] parameters = new String[]{"package", server.getServerName(), "--include=minify"};
        ProgramOutput po = machine.execute(server.getInstallRoot()+"/bin/server", parameters);
        Log.info(FIPS1403ServerTest.class, "fips140_3MinifiedPackageTest", "Executed server package command: "+ po.getCommand());
        Log.info(FIPS1403ServerTest.class, "fips140_3MinifiedPackageTest", "Result: "+ po.getStdout());
        Log.info(FIPS1403ServerTest.class, "serverEnvFipsTest", "Error: "+ po.getStderr());
        assertEquals("Package command failed with a non-zero return code", 0, po.getReturnCode());

        boolean foundFipsSecurityFile = false;
        if(!server.getMachine().getOperatingSystem().equals(OperatingSystem.ZOS)) {
            Path zipPath = Paths.get(server.getServerRoot() + "/" + server.getServerName() + ".zip");
            try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements() && !foundFipsSecurityFile) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.endsWith(LIBERTY_BASE_FIPS_PROFILE_FILENAME)) {
                        foundFipsSecurityFile = true;
                    }
                }
            }
            // ZOS packaging produces `.pax` files instead of `.zip`
        } else {
            Path paxPath =  Paths.get(server.getServerRoot() + "/" + server.getServerName() + ".pax");
            // run pax command without `r` or `w` to get a list of the contents
            ProgramOutput paxcmd = server.getMachine().execute("pax -ppx -f " + paxPath);
            Log.info(c, "pax list package contents", " command:" + paxcmd.getCommand());
            Log.info(c, "pax list package contents", " command exit code: " + paxcmd.getReturnCode());
            String[] contents = paxcmd.getStdout().split(System.lineSeparator());
            Log.info(c, "pax list package contents" , "size: " + contents.length);
            for(String line : contents) {
                if (line.endsWith(LIBERTY_BASE_FIPS_PROFILE_FILENAME)) {
                    foundFipsSecurityFile = true;
                    break;
                }
            }
        }
        assertTrue("Did not locate the base Liberty Profile in the zip file", foundFipsSecurityFile);
    }


    @After
    public void teardown() throws Exception {
        server.stopServer();
        server.restoreServerConfiguration();
        // delete the server.env file to remove any potential conflicts in
        if(Files.exists(serverEnVPath)) {
            Files.delete(serverEnVPath);
        }
        // restore Server does not change the JVM options
        HashMap<String, String> opts = new HashMap<>();
        server.setJvmOptions(opts);
    }
}
