/**
 *
 */
package io.openliberty.jcache.fat;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;

@SuppressWarnings("restriction")
public abstract class BaseTestCase {

    private static Class<?> thisClass = BaseTestCase.class;

    private static final String HAZELCAST_GROUP_PASSWORD = "groupPassword";
    protected static final String USER1_NAME = "user1";
    protected static final String USER1_PASSWORD = "user1Password";
    protected static final String JCACHE_HIT_USER1_BASICAUTH = "JCache HIT for key BasicRealm:user1:BjVc2C4Xh1a2Xc1EJ5Y1F0zyui8=";
    protected static final String JCACHE_MISS_USER1_BASICAUTH = "JCache MISS for key BasicRealm:user1:BjVc2C4Xh1a2Xc1EJ5Y1F0zyui8=";

    @Server("io.openliberty.jcache.internal.fat.multi.1")
    public static LibertyServer server1;

    @Server("io.openliberty.jcache.internal.fat.multi.2")
    public static LibertyServer server2;

    @Rule
    public final TestName testName = new TestName();
    public static String _testName = "";

    /**
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        _testName = testName.getMethodName();
        System.out.println("----- Start:  " + _testName + "   ----------------------------------------------------");

        if (server1.isStarted()) {
            logTestCaseInServerSideLog("STARTING", server1);
        }
        if (server2.isStarted()) {
            logTestCaseInServerSideLog("STARTING", server2);
        }
    }

    @After
    public void afterTest() throws Exception {
        if (server1.isStarted()) {
            logTestCaseInServerSideLog("STOPPING", server1);
        }
        if (server2.isStarted()) {
            logTestCaseInServerSideLog("STOPPING", server2);
        }
    }

    protected static void startServer1(String hazelcastGroupName) throws Exception {

        /*
         * Determine whether we need to disable multicast in the configuration.
         */
        String hazecastConfigFile = "hazelcast-localhost-only.xml";
        if (FATSuite.isMulticastDisabled()) {
            hazecastConfigFile = "hazelcast-localhost-only-multicastDisabled.xml";
        }

        server1.addInstalledAppForValidation("basicauth");
        server1.addInstalledAppForValidation("testmarker");
        server1.setHttpDefaultPort(8010);
        server1.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + hazelcastGroupName,
                                            "-Dhazelcast.group.password=" + HAZELCAST_GROUP_PASSWORD,
                                            "-Dhazelcast.config.file=" + hazecastConfigFile,
                                            "-Dhazelcast.jcache.provider.type=server")); // TODO Hazelcast 4 defaults to client, maybe use member
        server1.startServer();

        /*
         * Wait for each server to finish startup.
         */
        assertNotNull("Security service did not come up",
                      server1.waitForStringInLog("CWWKS0008I")); // CWWKS0008I: The security service is ready.
        assertNotNull("FeatureManager did not report update was complete",
                      server1.waitForStringInLog("CWWKF0008I")); // CWWKF0008I: Feature update completed
        assertNotNull("Server did not came up",
                      server1.waitForStringInLog("CWWKF0011I")); // CWWKF0011I: The server is ready to run a smarter planet.
    }

    protected static void startServer2(String hazelcastGroupName) throws Exception {

        String hazecastConfigFile = "hazelcast-client-localhost-only.xml";

        server2.addInstalledAppForValidation("basicauth");
        server2.addInstalledAppForValidation("testmarker");
        server2.setHttpDefaultPort(8030);
        server2.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + hazelcastGroupName,
                                            "-Dhazelcast.group.password=" + HAZELCAST_GROUP_PASSWORD,
                                            "-Dhazelcast.config.file=" + hazecastConfigFile,
                                            "-Dhazelcast.jcache.provider.type=client")); // TODO
        server2.startServer();

        /*
         * Wait for each server to finish startup.
         */
        assertNotNull("Security service did not come up",
                      server1.waitForStringInLog("CWWKS0008I")); // CWWKS0008I: The security service is ready.
        assertNotNull("FeatureManager did not report update was complete",
                      server2.waitForStringInLog("CWWKF0008I")); // CWWKF0008I: Feature update completed
        assertNotNull("Server did not came up",
                      server2.waitForStringInLog("CWWKF0011I")); // CWWKF0011I: The server is ready to run a smarter planet.
    }

    protected static void stopServer1(String... expectedExceptions) throws Exception {
        if (server1 != null && server1.isStarted()) {
            server1.stopServer(expectedExceptions);
        }
    }

    protected static void stopServer2(String... expectedExceptions) throws Exception {
        if (server2 != null && server2.isStarted()) {
            server2.stopServer(expectedExceptions);
        }
    }

    public static void logTestCaseInServerSideLog(String action, LibertyServer server) throws Exception {
        String method = "logTestCaseInServerSideLog";
        try {
            if (server != null) {
                WebConversation wc = new WebConversation();
                String markerUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/testmarker/testMarker";
                Log.info(thisClass, method, "Server " + server.getServerName() + " marker url is: " + markerUrl);
                WebRequest request = new GetMethodWebRequest(markerUrl);
                request.setParameter("action", action);
                request.setParameter("testCaseName", _testName);
                wc.getResponse(request);
            }
        } catch (Exception e) {
            // just log the failure - we shouldn't allow a failure here to cause
            // a test case to fail.
            Log.error(thisClass, method, e);
        }
    }

    /**
     * Reset the marks in all Liberty logs.
     *
     * @param server The server for the logs to reset the marks.
     * @throws Exception If there was an error resetting the marks.
     */
    public static void resetMarksInLogs(LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());
    }
}
