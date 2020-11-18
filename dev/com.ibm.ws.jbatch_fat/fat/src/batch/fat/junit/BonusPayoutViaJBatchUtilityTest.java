/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package batch.fat.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jbatch.test.BatchManagerCliUtils;
import com.ibm.ws.jbatch.test.FatUtils;
import com.ibm.ws.jbatch.test.dbservlet.DbServletClient;

import batch.fat.util.BatchFATHelper;
import batch.fat.util.StringUtils;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class BonusPayoutViaJBatchUtilityTest {

    public static final LibertyServer server = LibertyServerFactory.getLibertyServer("ManagedBonusPayout");

    BatchManagerCliUtils batchManagerCliUtils = new BatchManagerCliUtils(server);

    private static final Class testClass = BonusPayoutViaJBatchUtilityTest.class;

    private final String user = "bob";
    private final String pass = "bobpwd";

    /**
     * Start the server
     */
    @BeforeClass
    public static void setUp() throws Exception {
        
        if (JakartaEE9Action.isActive()) {
            JakartaEE9Action.transformApp(Paths.get(server.getServerRoot(), "dropins", "BonusPayout.war"));
            JakartaEE9Action.transformApp(Paths.get(server.getServerRoot(), "dropins", "BonusPayoutEAR.ear"));
            JakartaEE9Action.transformApp(Paths.get(server.getServerRoot(), "dropins", "DbServletApp.war"));
        }

        setConfig("BonusPayoutViaJBatchUtility/server.xml", testClass);
        BatchFATHelper.startServer(server, testClass);

        FatUtils.waitForSmarterPlanet(server);
        FatUtils.waitForSSLKeyAndLTPAKey(server);

        // Setup BonusPayout app tables
        new DbServletClient().setDataSourceJndi("jdbc/BonusPayoutDS").setDataSourceUser("user", "pass").setHostAndPort(server.getHostname(),
                                                                                                                       server.getHttpDefaultPort()).loadSql(server.pathToAutoFVTTestFiles
                                                                                                                                                            + "common/BonusPayout.derby.ddl",
                                                                                                                                                            "JBATCH",
                                                                                                                                                            "").executeUpdate();
    }

    /**
     * Stop the server
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKY0011W");
        }
    }

    /**
     * helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(BonusPayoutViaJBatchUtilityTest.class, method, msg);
    }

    /**
     * Apply the config file, restart the server, and wait for the msgs
     * to show up in the log.
     */
    protected static void setConfig(String config, Class testClass) throws Exception {
        Log.info(testClass, "setConfig", "Setting server.xml to: " + config);
        server.setServerConfigurationFile(config);

    }

    @Test
    public void testRunBonusPayoutStandaloneWARDefault() throws Exception {

        String appName = "BonusPayout";
        String jobXMLName = "BonusPayoutJob";

        ProgramOutput po = batchManagerCliUtils.submitJob(
                                                          new String[] { "submit",
                                                                         "--batchManager=" + batchManagerCliUtils.getHostAndPort(),
                                                                         "--user=" + user,
                                                                         "--password=" + pass,
                                                                         "--wait",
                                                                         "--pollingInterval_s=2",
                                                                         "--applicationName=" + appName,
                                                                         "--jobXMLName=" + jobXMLName });

        log("testRunBonusPayoutStandaloneWARDefault", "stdout:\n" + po.getStdout());
        log("testRunBonusPayoutStandaloneWARDefault", "stderr:\n" + po.getStderr());

        assertEquals(35, po.getReturnCode()); //  BatchStatus.COMPLETED.ordinal()
        assertTrue(po.getStdout().contains("CWWKY0105I:")); // job finished message
    }

    @Test
    public void testRunBonusPayoutStandaloneWARJavaCompEnvDefault() throws Exception {
        String method = "testRunBonusPayoutStandaloneWARJavaCompEnvDefault";

        String appName = "BonusPayout";
        String jobXMLName = "BonusPayoutJob";

        String jobParamsFileName = StringUtils.join(Arrays.asList(server.getServerRoot(), method + ".properties"), File.separator);
        File jobParamsFile = new File(jobParamsFileName);
        PrintWriter pw = new PrintWriter(jobParamsFile);
        pw.println("dsJNDI=java:comp/env/jdbc/BonusPayoutDS");
        pw.close();

        ProgramOutput po = batchManagerCliUtils.submitJob(
                                                          new String[] { "submit",
                                                                         "--batchManager=" + batchManagerCliUtils.getHostAndPort(),
                                                                         "--user=" + user,
                                                                         "--password=" + pass,
                                                                         "--wait",
                                                                         "--pollingInterval_s=2",
                                                                         "--applicationName=" + appName,
                                                                         "--jobParametersFile=" + jobParamsFileName,
                                                                         "--jobXMLName=" + jobXMLName });

        log(method, "stdout:\n" + po.getStdout());
        log(method, "stderr:\n" + po.getStderr());

        assertEquals(35, po.getReturnCode()); //  BatchStatus.COMPLETED.ordinal()
        assertTrue(po.getStdout().contains("CWWKY0105I:")); // job finished message

    }

    @Test
    public void testRunBonusPayoutWARInEARJavaCompEnvDefault() throws Exception {
        String method = "testRunBonusPayoutWARInEARJavaCompEnvDefault";

        String appName = "BonusPayoutEAR";
        String moduleName = "BonusPayout.war";
        String jobXMLName = "BonusPayoutJob";

        String jobParamsFileName = StringUtils.join(Arrays.asList(server.getServerRoot(), method + ".properties"), File.separator);
        File jobParamsFile = new File(jobParamsFileName);
        PrintWriter pw = new PrintWriter(jobParamsFile);
        pw.println("dsJNDI=java:comp/env/jdbc/BonusPayoutDS");
        pw.close();

        ProgramOutput po = batchManagerCliUtils.submitJob(
                                                          new String[] { "submit",
                                                                         "--batchManager=" + batchManagerCliUtils.getHostAndPort(),
                                                                         "--user=" + user,
                                                                         "--password=" + pass,
                                                                         "--wait",
                                                                         "--pollingInterval_s=2",
                                                                         "--applicationName=" + appName,
                                                                         "--moduleName=" + moduleName,
                                                                         "--jobParametersFile=" + jobParamsFileName,
                                                                         "--jobXMLName=" + jobXMLName });

        log(method, "stdout:\n" + po.getStdout());
        log(method, "stderr:\n" + po.getStderr());

        assertEquals(35, po.getReturnCode()); //  BatchStatus.COMPLETED.ordinal()
        assertTrue(po.getStdout().contains("CWWKY0105I:")); // job finished message

    }

    @Test
    @ExpectedFFDC({ "javax.naming.NameNotFoundException", "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testRunBonusPayoutBadJNDILookup() throws Exception {
        String method = "testRunBonusPayoutBadJNDILookup";

        String appName = "BonusPayout";
        String jobXMLName = "BonusPayoutJob";

        String jobParamsFileName = StringUtils.join(Arrays.asList(server.getServerRoot(), method + ".properties"), File.separator);
        File jobParamsFile = new File(jobParamsFileName);
        PrintWriter pw = new PrintWriter(jobParamsFile);
        pw.println("dsJNDI=java:comp/env/jdbc/blah/blah"); // BAD on purpose !!!!
        pw.close();

        ProgramOutput po = batchManagerCliUtils.submitJob(
                                                          new String[] { "submit",
                                                                         "--batchManager=" + batchManagerCliUtils.getHostAndPort(),
                                                                         "--user=" + user,
                                                                         "--password=" + pass,
                                                                         "--wait",
                                                                         "--pollingInterval_s=2",
                                                                         "--applicationName=" + appName,
                                                                         "--jobParametersFile=" + jobParamsFileName,
                                                                         "--jobXMLName=" + jobXMLName });

        log(method, "stdout:\n" + po.getStdout());
        log(method, "stderr:\n" + po.getStderr());

        assertEquals(34, po.getReturnCode()); //  BatchStatus.FAILED.ordinal()
        assertTrue(po.getStdout().contains("CWWKY0105I:")); // job finished message

    }

}
