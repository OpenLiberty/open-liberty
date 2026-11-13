/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package io.openliberty.security.fips.fat.tests.fips1403.client;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.utils.PrivHelper;
import io.openliberty.security.fips.fat.FIPSTestUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static io.openliberty.security.fips.fat.FIPSTestUtils.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
@SkipIfSysProp({SkipIfSysProp.OS_IBMI})
public class FIPS1403ClientTest {

    private static final Class<?> c = FIPS1403ClientTest.class;

    public static final String CLIENT_NAME = "FIPSClient";
    public static String expectedProvider="InvalidProvider";
    public static EnterpriseArchive earHAC;
    public static LibertyClient client;
    public static boolean GLOBAL_CLIENT_FIPS = false;
    public static String baseLibertySecurityProfileLocation;

    @BeforeClass
    public static void setup() throws Exception {
        client = LibertyClientFactory.getLibertyClient(CLIENT_NAME);
        JavaInfo ji = JavaInfo.forClient(client);
        assumeThat(FIPSTestUtils.validFIPS140_3Environment(ji), is(true));
        //HelloAppClient ear
        String APP_NAME = "HelloAppClient";
        JavaArchive jar = ShrinkHelper.buildJavaArchive(APP_NAME + ".jar", "com.ibm.ws.clientcontainer.HelloAppClient.test");
        earHAC = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                .addAsModule(jar)
                .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/application.xml"));

        ShrinkHelper.exportAppToClient(client, earHAC);
        if (Boolean.parseBoolean(PrivHelper.getProperty("global.client.fips_140-3", "false"))) {
            Log.info(FIPS1403ClientTest.class,"setup","global.fips_140-3 is set, letting LibertyClient configure FIPS");
            GLOBAL_CLIENT_FIPS = true;
        }
        if (ji.majorVersion() > 8) {
            expectedProvider = "OpenJCEPlusFIPS";
        } else {
            expectedProvider = "IBMJCEPlusFIPS";
        }

        baseLibertySecurityProfileLocation = client.getInstallRoot() + "/lib/security/fips140_3/FIPS140-3-Liberty.properties";
    }

    @Test
    public void clientJFIPS140_3JVMArgsTest() throws Exception {
        // if FIPS140-3 is defined at a global level, the LibertyClient class will apply the settings for us
        if (!GLOBAL_CLIENT_FIPS) {
            Log.info(FIPS1403ClientTest.class,"setup","Setting FIPS140-3 JVM Options");
            HashMap<String, String> opts = new HashMap<>();
            //Semeru >=11
            if (expectedProvider.equals("OpenJCEPlusFIPS")) {
                client.copyFileToLibertyClientRoot("publish/resources", "resources", STANDALONE_FIPS_PROFILE_FILENAME);
                opts.put("-Dsemeru.fips", "true");
                opts.put("-Dsemeru.customprofile", "OpenJCEPlusFIPS.FIPS140-3-Custom");
                opts.put("-Djava.security.properties", client.getClientRoot() + "/resources/" + STANDALONE_FIPS_PROFILE_FILENAME);
                // IBM SDK 8
            } else {
                opts.put("-Xenablefips140-3", null);
                opts.put("-Dcom.ibm.jsse2.usefipsprovider", "true");
                opts.put("-Dcom.ibm.jsse2.usefipsProviderName", "IBMJCEPlusFIPS");
            }

            client.setJvmOptions(opts);
        } else {
            Log.info(FIPS1403ClientTest.class,"setup","global.fips_140-3 is set, letting LibertyClient configure FIPS");
        }
        client.startClient();
        checkClientLogForFipsEnablementMessage(client, expectedProvider);
    }
    
    @Test
    public void clientFIPS140_3EnvTest() throws Exception {
        client.copyFileToLibertyClientRoot("publish/resources", "resources" , STANDALONE_FIPS_PROFILE_FILENAME);
        client.addEnvVar(ENABLE_FIPS140_3_ENV_VAR, client.getClientRoot()+"/resources/" + STANDALONE_FIPS_PROFILE_FILENAME);
        // if global.client.fips_140-3=true is specified, the client will still use the envVar setting instead of applying the default args
        client.startClient();
        checkClientLogForFipsEnablementMessage(client, expectedProvider);
    }

    @Test
    public void fips140_3PacakageTest() throws Exception {
        Machine machine = client.getMachine();
        String[] parameters = new String[]{"package", client.getClientName()};
        ProgramOutput po = machine.execute(client.getInstallRoot()+"/bin/client", parameters);
        Log.info(FIPS1403ClientTest.class, "fips140_3PacakageTest", "Executed securityUtility configureFIPS command: "+ po.getCommand());
        Log.info(FIPS1403ClientTest.class, "fips140_3PacakageTest", "Result: "+ po.getStdout());
        Log.info(FIPS1403ClientTest.class, "fips140_3PacakageTest", "Error: "+ po.getStderr());
        assertEquals("Package command failed with a non-zero return code", 0, po.getReturnCode());

        boolean foundFipsSecurityFile = false;
        if(!client.getMachine().getOperatingSystem().equals(OperatingSystem.ZOS)) {
            Path zipPath = Paths.get(client.getClientRoot() + "/" + client.getClientName() + ".zip");
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
            Path paxPath =  Paths.get(client.getClientRoot() + "/" + client.getClientName() + ".pax");
            // run pax command without `r` or `w` to get a list of the contents
            ProgramOutput paxcmd = client.getMachine().execute("pax -ppx -f " + paxPath);
            Log.info(c, "unpax client package", " command:" + paxcmd.getCommand());
            Log.info(c, "unpax client package", " command exit code: " + paxcmd.getReturnCode());
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
        assumeThat(Files.exists(Paths.get(client.getInstallRoot() + "/lib/extract")), is(true));
        Machine machine = client.getMachine();
        String[] parameters = new String[]{"package", client.getClientName(), "--include=minify"};
        ProgramOutput po = machine.execute(client.getInstallRoot()+"/bin/client", parameters);
        Log.info(FIPS1403ClientTest.class, "fips140_3MinifiedPackageTest", "Executed client package command: "+ po.getCommand());
        Log.info(FIPS1403ClientTest.class, "fips140_3MinifiedPackageTest", "Result: "+ po.getStdout());
        Log.info(FIPS1403ClientTest.class, "clientEnvFipsTest", "Error: "+ po.getStderr());
        assertEquals("Package command failed with a non-zero return code", 0, po.getReturnCode());

        boolean foundFipsSecurityFile = false;
        if(!client.getMachine().getOperatingSystem().equals(OperatingSystem.ZOS)) {
            Path zipPath = Paths.get(client.getClientRoot() + "/" + client.getClientName() + ".zip");
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
            Path paxPath =  Paths.get(client.getClientRoot() + "/" + client.getClientName() + ".pax");
            // run pax command without `r` or `w` to get a list of the contents
            ProgramOutput paxcmd = client.getMachine().execute("pax -ppx -f " + paxPath);
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
        client.setJvmOptions(new HashMap<>());
    }

}
