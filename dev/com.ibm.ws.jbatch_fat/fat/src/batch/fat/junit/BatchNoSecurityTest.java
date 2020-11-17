package batch.fat.junit;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import batch.fat.util.BatchFatUtils;
import batch.fat.util.JobServletClient;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jbatch.test.FatUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * 
 * Test dynamic updates to JDBC config using various schemas and tablePrefixes.
 * 
 */
@RunWith(FATRunner.class)
public class BatchNoSecurityTest {

    protected static final LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.nosecurity.fat");

    /**
     * Start the server and setup the DB.
     */
    @BeforeClass
    public static void setup() throws Exception {

        log("setup", "start server and execute DDLs");

        FatUtils.checkJava7();

        // Start server 
        server.startServer("BatchSecurityTest.log");
        FatUtils.waitForSmarterPlanet(server);
    }

    /**
     * Stop the server.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        log("tearDown", "stopping server");
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * 
     */
    @Test
    public void testSecurityBatchletAsDefaultUserWithNoBatchSecurity() throws Exception {

        JobServletClient jobServletClient = new JobServletClient(server, "batchSecurity");

        // Submit the job...
        HttpURLConnection con = jobServletClient.sendGetRequest("/jobservlet?action=start&jobXMLName=SecurityBatchlet");
        assertEquals(BatchFatUtils.MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        JsonObject jobInstance = Json.createReader(con.getInputStream()).readObject();
        log("testSecurityBatchletAsDefaultUser", "JobInstance: " + jobInstance);

        // Wait for the job execution to show up...
        JsonObject jobExecution = jobServletClient.waitForFirstJobExecution(jobInstance.getJsonNumber("instanceId").longValue());

        // Now wait for the job execution to finish...
        jobExecution = jobServletClient.waitForJobExecutionToFinish(jobExecution);

        //verify the job completed
        assertEquals("COMPLETED", jobExecution.getString("batchStatus"));
        assertEquals("null", jobExecution.getString("exitStatus"));
    }

    /**
     * Log helper.
     */
    public static void log(String method, String msg) {
        Log.info(BatchNoSecurityTest.class, method, msg);
    }

}
