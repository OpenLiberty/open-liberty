package batch.fat.junit;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import batch.fat.util.BatchFatUtils;
import batch.fat.util.JobServletClient;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 *
 * Test dynamic updates to JDBC config using various schemas and tablePrefixes.
 *
 */
@RunWith(FATRunner.class)
public class BatchSecurityTest {

    private static final Class testClass = BatchSecurityTest.class;

    protected static final LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.fat");

    /**
     * Start the server and setup the DB.
     */
    @BeforeClass
    public static void setup() throws Exception {

        log("setup", "start server and execute DDLs");

        FatUtils.checkJava7();

        BatchAppUtils.addDropinsBatchSecurityWar(server);

        // Start server
        BatchFATHelper.setConfig("BatchSecurity/server.xml", testClass);
        server.startServer("BatchSecurityTest.log");
        FatUtils.waitForSmarterPlanet(server);
        FatUtils.waitForSSLKeyAndLTPAKey(server);
    }

    /**
     * Stop the server.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        log("tearDown", "stopping server");
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKY0041W");
        }
    }

    /**
     * This test has been changed to expect a failure when security is enabled. I've added another test which allows
     * EVERYONE to be a batch submitter which should not have any exceptions.
     */
    @Test
    @ExpectedFFDC({ "javax.batch.operations.JobSecurityException" })
    public void testSecurityBatchletAsUnauthenticatedUser() throws Exception {

        // Submit the job...
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=start&jobXMLName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            null,
                                                            null);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        log("testSecurityBatchletAsUnauthenticatedUser", "Response: body= " + body);

        Assert.assertTrue("Actual:" + body, body.contains("UNAUTHENTICATED"));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    /**
     *
     */
    @Test
    public void testSecurityBatchletRunAsUser1() throws Exception {

        JobServletClient jobServletClient = new JobServletClient(server, "batchSecurity").setUserAndPass("user1", "pass1");

        // Submit the job...
        HttpURLConnection con = jobServletClient.sendGetRequest("/jobservlet?action=start&jobXMLName=SecurityBatchlet");
        assertEquals(BatchFatUtils.MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        JsonObject jobInstance = Json.createReader(con.getInputStream()).readObject();
        log("testSecurityBatchletAsUser1", "JobInstance: " + jobInstance);

        // Wait for the job execution to show up...
        JsonObject jobExecution = jobServletClient.waitForFirstJobExecution(jobInstance.getJsonNumber("instanceId").longValue());

        // Now wait for the job execution to finish...
        jobExecution = jobServletClient.waitForJobExecutionToFinish(jobExecution);

        assertEquals("user1", jobExecution.getString("exitStatus"));
    }

    /**
     * Log helper.
     */
    public static void log(String method, String msg) {
        Log.info(BatchSecurityTest.class, method, msg);
    }

}
