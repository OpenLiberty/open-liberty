package batch.fat.junit;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFatUtils;
import batch.fat.util.JobServletClient;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 * Test JobOperator API under existing UserTransaction.
 *
 */
@RunWith(FATRunner.class)
public class BatchUserTranTest {

    protected static final LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.nosecurity.fat");

    /**
     * Start the server and setup the DB.
     */
    @BeforeClass
    public static void setup() throws Exception {

        FatUtils.checkJava7();

        BatchFatUtils.addDropinsBatchFatWar(server);
        BatchFatUtils.addDropinsBatchSecurityWar(server);
        BatchFatUtils.addDropinsDbServletAppWar(server);

//        BatchFatUtils.addDropinsWebApp(server, "batchFAT.war",
//                                       "batch.fat.util",
//                                       "batch.fat.artifacts", "batch.fat.cdi", "batch.fat.common", "batch.fat.web", "batch.fat.web.customlogic",
//                                       "chunktests.artifacts",
//                                       "processitem.artifacts");
//        BatchFatUtils.addDropinsWebApp(server, "batchSecurity.war", "batch.fat.artifacts", "batch.security", "batch.fat.util");
//        BatchFatUtils.addDropinsWebApp(server, "DbServletApp.war", "batch.fat.web", "batch.fat.common.util");

        // TODO: Remove.  Obviated by addDropingsWebApp
        //if (JakartaEE9Action.isActive()) {
        //    JakartaEE9Action.transformApp(Paths.get(server.getServerRoot(), "dropins", "batchSecurity.war"));
        //    JakartaEE9Action.transformApp(Paths.get(server.getServerRoot(), "dropins", "DbServletApp.war"));
        //}

        // Start server
        server.startServer("BatchUserTranTest.log");
        FatUtils.waitForSmarterPlanet(server);
    }

    /**
     * Stop the server.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     *
     */
    @Test
    public void testJobOperatorApiUnderUserTran() throws Exception {

        JobServletClient jobServletClient = new JobServletClient(server, "batchSecurity");
        // Submit the job...
        HttpURLConnection con = jobServletClient.sendGetRequest("/jobservlet?action=startUnderUserTran&jobXMLName=test_sleepyBatchlet");
        assertEquals(BatchFatUtils.MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        JsonObject jobInstance = Json.createReader(con.getInputStream()).readObject();
        log("testJobOperatorApiUnderUserTran", "JobInstance: " + jobInstance);

        // Wait for the job execution to show up...
        JsonObject jobExecution = jobServletClient.waitForFirstJobExecution(jobInstance.getJsonNumber("instanceId").longValue());

        // Now wait for the job execution to finish...
        jobExecution = jobServletClient.waitForJobExecutionToFinish(jobExecution);

        assertEquals("COMPLETED", jobExecution.getString("batchStatus"));
    }

    /**
     *
     */
    @Test
    public void testStartUnderUserTran() throws Exception {

        JobServletClient jobServletClient = new JobServletClient(server, "batchSecurity").setBeginTran(true);

        // Submit the job...
        HttpURLConnection con = jobServletClient.sendGetRequest("/jobservlet?action=start&jobXMLName=test_sleepyBatchlet");
        assertEquals(BatchFatUtils.MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        JsonObject jobInstance = Json.createReader(con.getInputStream()).readObject();
        log("testJobOperatorApiUnderUserTran", "JobInstance: " + jobInstance);

        // Wait for the job execution to show up...
        JsonObject jobExecution = jobServletClient.waitForFirstJobExecution(jobInstance.getJsonNumber("instanceId").longValue());

        // Now wait for the job execution to finish...
        jobExecution = jobServletClient.waitForJobExecutionToFinish(jobExecution);

        assertEquals("COMPLETED", jobExecution.getString("batchStatus"));
    }

    /**
     * Log helper.
     */
    public static void log(String method, String msg) {
        Log.info(JobServletClient.class, method, msg);
    }

}
