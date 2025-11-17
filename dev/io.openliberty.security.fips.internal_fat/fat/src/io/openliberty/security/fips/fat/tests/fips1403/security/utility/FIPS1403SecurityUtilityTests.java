package io.openliberty.security.fips.fat.tests.fips1403.security.utility;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;
import io.openliberty.security.fips.fat.FIPSTestUtils;
import io.openliberty.security.fips.fat.tests.fips1403.server.FIPS1403ServerTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import static io.openliberty.security.fips.fat.FIPSTestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeThat;


/**
 * Due to nature of these tests, If you are having to provide a default.env file ahead of these tests being run
 * e.g. supply JAVA_HOME, include the beta_flag for the server
 * You must include `# enable_variable_expansion' for the tests to work correctly
 *
 * The commands have the beta flag currently added for them. but for other scenarios the beta flag might be required
 */
@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
@SkipIfSysProp({SkipIfSysProp.OS_ZOS, SkipIfSysProp.OS_IBMI, SkipIfSysProp.OS_ISERIES})
public class FIPS1403SecurityUtilityTests {

    private static final Class<?> thisClass = FIPS1403SecurityUtilityTests.class;

    private static final String SERVER_NAME = "FIPSServer";
    private static final String CLIENT_NAME = "FIPSClient";
    private static String expectedProvider="InvalidProvider";
    private static JavaInfo ji;
    private static boolean GLOBAL_FIPS=false;

    private static Machine machine;
    private static Properties env;
    private static String installRoot;
    private static final String SEC_UTILITY_COMMAND = "/bin/securityUtility";
    private static final String SEC_CONF_FIPS_COMMAND = "configureFIPS";
    private static final String DEFAULT_ENV_BACKUP_FILE = "default.env.bkp";

    // Command Options
    private static final String OPT_DISABLE = "--disable";
    private static final String OPT_CUSTOM_PROFILE_FILE_NAME="--customProfileFile";
    private static final String OPT_CLIENT = "--client";
    private static final String OPT_SERVER = "--server";


    private enum FIPS_TARGET {
        INSTALL,
        SERVER,
        CLIENT
    }

    @Server(SERVER_NAME)
    public static LibertyServer server;

    public static LibertyClient client = LibertyClientFactory.getLibertyClient(CLIENT_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        ji = JavaInfo.forServer(server);
        assumeThat(FIPSTestUtils.validFIPS140_3Environment(ji), is(true));
        if (Boolean.parseBoolean(PrivHelper.getProperty("global.fips_140-3", "false"))) {
            GLOBAL_FIPS=true;
        }
        if (ji.majorVersion() > 8) {
            expectedProvider = SEMERU_FIPS_PROVIDER;
            // temporarily enable Beta for Semeru
            server.addEnvVar("JVM_ARGS","-Dcom.ibm.ws.beta.edition=true");
        } else {
            expectedProvider = IBM_FIPS_PROVIDER;
        }

        installRoot = server.getInstallRoot();
        env = new Properties();
        env.put("JVM_ARGS","-Dcom.ibm.ws.beta.edition=true");
        machine = server.getMachine();
        // Save configuration at this point, so each test can restore to this point so that we don't pollute each test
        server.saveServerConfiguration();

        // Incase there is an existing file which might contain properties that are require elsewhere e.g. JAVA_HOME take a backup and restore after the tests
        File defaultEnv = new File(installRoot + "/etc/" + DEFAULT_ENV_FILE);
        if (defaultEnv.exists()){
            Files.copy(defaultEnv.toPath(), new File(installRoot+"/etc/"+DEFAULT_ENV_BACKUP_FILE).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    public void helpConfigureFIPSTest() throws Exception {
        ProgramOutput po = runSecurityUtilityCommand( new String[] {"help", SEC_CONF_FIPS_COMMAND});
        assertEquals("securityUtility help configureFIPS returned a non-0 return code", 0, po.getReturnCode());
        String resultText = po.getStdout();
        // usage text
        assertTrue(resultText.contains("securityUtility configureFIPS [options]"));
        // check all options appear
        assertTrue(resultText.contains("--server=name"));
        assertTrue(resultText.contains("--client=name"));
        assertTrue(resultText.contains("--customProfileFile=name"));
        assertTrue(resultText.contains("--disable"));
    }

    @Test
    public void defaultEnvFipsTest() throws Exception {
        // assumeThat(GLOBAL_FIPS, is(false));
        ProgramOutput po = runSecurityUtilityCommand( new String[] {SEC_CONF_FIPS_COMMAND});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",0, po.getReturnCode());

        File defaultEnv = new File(installRoot + "/etc/" + DEFAULT_ENV_FILE);
        assertTrue(defaultEnv.exists());
        if(SEMERU_FIPS_PROVIDER.equals(expectedProvider)) {
            File fipsProfileFile = new File(installRoot + "/etc/" + FIPS_PROFILE_FILE_NAME);
            assertTrue(fipsProfileFile.exists());
            checkFileExpectedValue(defaultEnv, ENABLE_FIPS140_3_ENV_VAR + "=" + fipsProfileFile.getPath());
        } else {
            checkFileExpectedValue(defaultEnv, ENABLE_FIPS140_3_ENV_VAR + "=true");
        }
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
        disableFIPS(FIPS_TARGET.INSTALL, null);
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void multipleEnablementsFIPSTest() throws Exception {
        ProgramOutput po = runSecurityUtilityCommand( new String[] {SEC_CONF_FIPS_COMMAND});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",0, po.getReturnCode());
        po = runSecurityUtilityCommand( new String[] {SEC_CONF_FIPS_COMMAND});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",1, po.getReturnCode());
        assertTrue(po.getStdout().contains("FIPS 140-3 is already enabled"));
        disableFIPS(FIPS_TARGET.INSTALL, null);
        po = runSecurityUtilityCommand(new String[] {SEC_CONF_FIPS_COMMAND, OPT_DISABLE});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",1, po.getReturnCode());

        // test to cover what should happen when enablement of FIPS is run without disablement in-between
        po = runSecurityUtilityCommand( new String[] {SEC_CONF_FIPS_COMMAND,OPT_SERVER+ "=" + SERVER_NAME});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",0, po.getReturnCode());
        po = runSecurityUtilityCommand( new String[] {SEC_CONF_FIPS_COMMAND,OPT_SERVER+ "=" + SERVER_NAME});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",1, po.getReturnCode());
        assertTrue(po.getStdout().contains("FIPS 140-3 is already enabled"));
        disableFIPS(FIPS_TARGET.SERVER, SERVER_NAME);
        po = runSecurityUtilityCommand(new String[] {SEC_CONF_FIPS_COMMAND, OPT_DISABLE, OPT_SERVER+"="+SERVER_NAME});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",1, po.getReturnCode());
    }

    @Test
    public void serverEnvFipsTest() throws Exception {
        // assumeThat(GLOBAL_FIPS, is(false));
        ProgramOutput po = runSecurityUtilityCommand( new String[] {SEC_CONF_FIPS_COMMAND,OPT_SERVER+ "=" + SERVER_NAME});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",0, po.getReturnCode());
        File serverEnv = new File(server.getServerRoot() + "/"+ SERVER_ENV_FILE);
        assertTrue(serverEnv.exists());
        if(SEMERU_FIPS_PROVIDER.equals(expectedProvider)) {
            File fipsProfileFile = new File(server.getServerRoot() + "/resources/security/" + FIPS_PROFILE_FILE_NAME);
            assertTrue("FIPS Profile file does not exist", fipsProfileFile.exists());
            checkFileExpectedValue(serverEnv, ENABLE_FIPS140_3_ENV_VAR + "=" + fipsProfileFile.getPath());
        } else {
            checkFileExpectedValue(serverEnv, ENABLE_FIPS140_3_ENV_VAR + "=true");
        }
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
        disableFIPS(FIPS_TARGET.SERVER, SERVER_NAME);
    }

    @Test
    public void clientEnvFIPSTest() throws Exception {
        ProgramOutput po = runSecurityUtilityCommand( new String[] {SEC_CONF_FIPS_COMMAND, OPT_CLIENT+ "=" + CLIENT_NAME});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",0, po.getReturnCode());
        File clientEnv = new File(client.getClientRoot() + "/"+ CLIENT_ENV_FILE);
        assertTrue(clientEnv.exists());
        if(SEMERU_FIPS_PROVIDER.equals(expectedProvider)) {
            File fipsProfileFile = new File(client.getClientRoot() + "/resources/security/" + FIPS_PROFILE_FILE_NAME);
            assertTrue("FIPS Profile file does not exist", fipsProfileFile.exists());
            checkFileExpectedValue(clientEnv, ENABLE_FIPS140_3_ENV_VAR + "=" + fipsProfileFile.getPath());
        } else {
            checkFileExpectedValue(clientEnv, ENABLE_FIPS140_3_ENV_VAR + "=true");
        }
        client.startClient();
        checkClientLogForFipsEnablementMessage(client, expectedProvider);
        disableFIPS(FIPS_TARGET.CLIENT, CLIENT_NAME);
    }

    @Test
    public void customProfileSingleFileTest() throws Exception {
        // Does not apply to IBM SDK 8
        // assumeThat(GLOBAL_FIPS, is(false));
        assumeThat(expectedProvider, is(SEMERU_FIPS_PROVIDER));
        String customProfileName = "Custom-FIPS-Profile";
        // Create Custom Profile under default enablement
        ProgramOutput po = runSecurityUtilityCommand( new String[] {SEC_CONF_FIPS_COMMAND, OPT_CUSTOM_PROFILE_FILE_NAME + "="+ customProfileName + PROPS_FILE_EXTENSION});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",0, po.getReturnCode());
        File defaultEnv = new File(installRoot + "/etc/" + DEFAULT_ENV_FILE);
        assertTrue(defaultEnv.exists());
        if(SEMERU_FIPS_PROVIDER.equals(expectedProvider)) {
            File fipsProfileFile = new File(installRoot + "/" + customProfileName + PROPS_FILE_EXTENSION);
            assertTrue(fipsProfileFile.exists());
            checkFileExpectedValue(defaultEnv, ENABLE_FIPS140_3_ENV_VAR + "=" + fipsProfileFile.getPath());
        }
        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
        disableFIPS(FIPS_TARGET.INSTALL, null);
    }

    @Test
    public void multipleCustomProfileMultipleFileTest() throws Exception {
        // Does not apply to IBM SDK 8
        // assumeThat(GLOBAL_FIPS, is(false));
        assumeThat(expectedProvider, is(SEMERU_FIPS_PROVIDER));
        String customProfile1 = "customFIPSProfile1";
        String customProfile2 = "customFIPSProfile2";
        String customProfile3 = "customFIPSProfile3";
        String customProfile1FilePath = server.getServerRoot() + "/resources/security/" + customProfile1 + PROPS_FILE_EXTENSION;
        String customProfile2FilePath = server.getServerRoot() + "/resources/security/" + customProfile2 + PROPS_FILE_EXTENSION;
        String customProfile3FilePath = server.getServerRoot() + "/resources/security/" + customProfile3 + PROPS_FILE_EXTENSION;

        ProgramOutput po = runSecurityUtilityCommand( new String[] {SEC_CONF_FIPS_COMMAND, OPT_SERVER + "=" + SERVER_NAME, OPT_CUSTOM_PROFILE_FILE_NAME + "=" + customProfile1FilePath + File.pathSeparator + customProfile2FilePath + File.pathSeparator +customProfile3FilePath});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",0, po.getReturnCode());

        File serverEnv = new File(server.getServerRoot() + "/"+ SERVER_ENV_FILE);
        assertTrue(serverEnv.exists());
        File fipsProfileFile1 = new File(server.getServerRoot() + "/resources/security/" + customProfile1 + PROPS_FILE_EXTENSION);
        assertTrue(fipsProfileFile1.exists());
        checkFileExpectedValue(fipsProfileFile1, customProfile1);
        File fipsProfileFile2 = new File(server.getServerRoot() + "/resources/security/" + customProfile2 + PROPS_FILE_EXTENSION);
        assertTrue(fipsProfileFile2.exists());
        checkFileExpectedValue(fipsProfileFile2, customProfile2);
        File fipsProfileFile3 = new File(server.getServerRoot() + "/resources/security/" + customProfile3 + PROPS_FILE_EXTENSION);
        assertTrue(fipsProfileFile3.exists());
        checkFileExpectedValue(fipsProfileFile3, customProfile3);
        String expectedValue = fipsProfileFile1.getPath() + File.pathSeparator + fipsProfileFile2.getPath() + File.pathSeparator + fipsProfileFile3.getPath();
        checkFileExpectedValue(serverEnv, ENABLE_FIPS140_3_ENV_VAR + "=" +expectedValue);

        server.startServer();
        checkServerLogForFipsEnablementMessage(server, expectedProvider);
        disableFIPS(FIPS_TARGET.SERVER, SERVER_NAME);

        // basic test cleanup
        fipsProfileFile1.delete();
        fipsProfileFile2.delete();
        fipsProfileFile3.delete();
    }

    @Test
    public void fips140_3CreateLTPAKeysTest() throws Exception {
        // Tests that the tools script it operating as expected when the environment is set
        // Enable FIPS at install level
        ProgramOutput po = runSecurityUtilityCommand( new String[] {SEC_CONF_FIPS_COMMAND});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",0, po.getReturnCode());
        // recreate the LTPA keys and they should be v2.
        ProgramOutput ltpaPO = runSecurityUtilityCommand( new String[] {"createLTPAKeys", "--server=" + SERVER_NAME, "--password=passw0rd"});
        assertEquals("securityUtility configureFIPS did not result in expected return code.",0, ltpaPO.getReturnCode());
        File ltpaKeysFile = new File(server.getServerRoot() + "/resources/security/ltpa.keys");
        assertTrue("LTPA.keys file should exist", ltpaKeysFile.exists());
        checkFileExpectedValue(ltpaKeysFile, "com.ibm.websphere.ltpa.version=2.0");

    }

    @After
    public void teardown() throws Exception {
        server.stopServer();
        // clean up all the potentially created files
        File defaultEnv = new File(installRoot + "/etc/" + DEFAULT_ENV_FILE);
        File fipsDefaultProfileFile = new File(installRoot +"/etc/" + FIPS_PROFILE_FILE_NAME);
        File serverEnv = new File(server.getServerRoot() + "/" + SERVER_ENV_FILE);
        File clientEnv = new File(client.getClientRoot()+"/"+ CLIENT_ENV_FILE);
        File fipsServerProfileFile = new File(server.getServerRoot() + "/resources/security" + FIPS_PROFILE_FILE_NAME);
        File fipsClientProfileFile = new File(client.getClientRoot() + "/resources/security" + FIPS_PROFILE_FILE_NAME);

        // As Default env can contain entries for setting JAVA_HOME or additional Java properties, if we have taken a backup, restore between tests
        // if no default.env exists, just delete
        // All other files should be deleted
        if (defaultEnv.exists()){
            File defaultBackupEnv = new File(installRoot + "/etc/" + DEFAULT_ENV_BACKUP_FILE);
            if (defaultBackupEnv.exists()){
                Files.copy(defaultBackupEnv.toPath(), new File(installRoot+"/etc/"+DEFAULT_ENV_FILE).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                defaultEnv.delete();
            }
        }
        if (fipsDefaultProfileFile.exists()) fipsDefaultProfileFile.delete();
        if (serverEnv.exists()) serverEnv.delete();
        if (clientEnv.exists()) clientEnv.delete();
        if (fipsServerProfileFile.exists()) fipsServerProfileFile.delete();
        if (fipsClientProfileFile.exists()) fipsClientProfileFile.delete();
    }

    @AfterClass
    public static void reset() throws IOException {
        File defaultBackupEnv = new File(installRoot + "/etc/" + DEFAULT_ENV_BACKUP_FILE);
        if (defaultBackupEnv.exists()){
            Files.copy(defaultBackupEnv.toPath(), new File(installRoot+"/etc/"+DEFAULT_ENV_FILE).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        defaultBackupEnv.delete();
    }

    /**
     * Disabling FIPS via the ENV files will not impact any running servers or clients as the env is not-reread.
     *
     * Confirms that FIPS has been disabled, so if the DISABLE option is expected to fail, do not use this method
     *
     * But this should be used at the end of the test as the test knows what it is setting the environment for, @After or @AfterClass do not, nor should they
     * @param target
     * @param name
     * @throws Exception
     */
    private void disableFIPS(FIPS_TARGET target, String name) throws Exception {
        ProgramOutput po = null;
        File envFile = null;
        switch(target){
            case SERVER:
                po = runSecurityUtilityCommand(new String[] {SEC_CONF_FIPS_COMMAND, OPT_DISABLE, OPT_SERVER+"="+name});
                envFile = new File(installRoot + "/usr/servers/" + name + "/" + SERVER_ENV_FILE);
                break;
            case CLIENT:
                po = runSecurityUtilityCommand(new String[] {SEC_CONF_FIPS_COMMAND, OPT_DISABLE, OPT_CLIENT+"="+name});
                envFile = new File(installRoot + "/usr/clients/" + name + "/" + CLIENT_ENV_FILE);
                break;
            default:
                po = runSecurityUtilityCommand(new String[] {SEC_CONF_FIPS_COMMAND, OPT_DISABLE});
                envFile = new File(installRoot + "/etc/" + DEFAULT_ENV_FILE);
                break;
        }
        assertEquals(0, po.getReturnCode());
        assertTrue(po.getStdout().contains(envFile.getPath()+ " file to disable FIPS 140-3."));
        checkFileExpectedValue(envFile, ENABLE_FIPS140_3_ENV_VAR + "=false");
    }

    public void checkFileExpectedValue(File file, String expectedValue) throws IOException {
        byte[] contentsB = Files.readAllBytes(file.toPath());
        String contentsS = new String(contentsB);
        assertTrue("Contents "+ contentsS +" did not contain expected string: " + expectedValue, contentsS.contains(expectedValue));
    }

    /**
     * Execute the securityUtility command
     *
     * Logs
     *
     * @param parameters includes the Task to be run and any arguments required
     * @return
     * @throws Exception
     */
    public ProgramOutput runSecurityUtilityCommand(String[] parameters) throws Exception {
        ProgramOutput po = machine.execute(installRoot + SEC_UTILITY_COMMAND, parameters, env);
        Log.info(thisClass, "serverEnvFipsTest", "Executed securityUtility configureFIPS command: "+ po.getCommand());
        Log.info(thisClass, "serverEnvFipsTest", "Result: "+ po.getStdout());
        Log.info(thisClass, "serverEnvFipsTest", "Error: "+ po.getStderr());
        return po;
    }

}
