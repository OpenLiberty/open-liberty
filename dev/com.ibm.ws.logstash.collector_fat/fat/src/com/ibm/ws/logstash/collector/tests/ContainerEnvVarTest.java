/**
 *
 */
package com.ibm.ws.logstash.collector.tests;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class ContainerEnvVarTest extends LogstashCollectorTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("ContainerEnvServer");

    private String testName = "";
    private static Class<?> c = ContainerEnvVarTest.class;

    protected static boolean runTest = true;

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(c, "setUp", "runTest = " + runTest);
        if (!runTest) {
            return;
        }

        clearContainerOutput();
        String host = logstashContainer.getContainerIpAddress();
        String port = String.valueOf(logstashContainer.getMappedPort(5043));
        Log.info(c, "setUp", "Logstash container: host=" + host + "  port=" + port);
        server.addEnvVar("LOGSTASH_HOST", host);
        server.addEnvVar("LOGSTASH_PORT", port);

        Log.info(c, "setUp", "installed liberty root is at: " + server.getInstallRoot());
        Log.info(c, "setUp", "server root is at: " + server.getServerRoot());

        ShrinkHelper.defaultDropinApp(server, "LogstashApp", "com.ibm.logs");

        serverStart();
    }

    @Before
    public void setUpTest() throws Exception {
        Log.info(c, testName, "runTest = " + runTest);
        Assume.assumeTrue(runTest); // runTest must be true to run test

        testName = "setUpTest";
        if (!server.isStarted()) {
            serverStart();
        }
    }

    @After
    public void tearDown() {}

    @AfterClass
    public static void completeTest() throws Exception {
        try {
            if (server.isStarted()) {
                Log.info(c, "competeTest", "---> Stopping server..");
                server.stopServer("TRAS4301W");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Test to verify CONTAINER_NAME and CONTAINER_HOST env vars are picked up */
    @Test
    public void containerEnvVarTest() throws Exception {

        boolean foundEnvVars = true;

        String stringHost = waitForStringInContainerOutput("TEST_HOST_NAME");
        String stringServer = waitForStringInContainerOutput("TEST_SERVER_NAME");

        Log.info(c, "containerEnvVarTest", "stringHost: " + stringHost);
        Log.info(c, "containerEnvVarTest", "stringServer: " + stringServer);

        if (stringHost == null || stringServer == null) {
            foundEnvVars = false;
        }

        assertTrue("Could not find one or more of the set env vars CONTAINER_HOST and CONTAINER_NAME", foundEnvVars);
    }

    private static void serverStart() throws Exception {
        Log.info(c, "serverStart", "--->  Starting Server.. ");
        server.startServer();
    }

    @Override
    protected LibertyServer getServer() {
        return server;
    }

}
