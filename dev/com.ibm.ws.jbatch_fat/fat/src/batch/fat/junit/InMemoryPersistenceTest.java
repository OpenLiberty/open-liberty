/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package batch.fat.junit;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.BatchRestUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class InMemoryPersistenceTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.fat.memory.persistence");

    @BeforeClass
    public static void beforeClass() throws Exception {
        HttpUtils.trustAllCertificates();

        FatUtils.checkJava7();
        
        if (JakartaEE9Action.isActive()) {
            JakartaEE9Action.transformApp(Paths.get(server.getServerRoot(), "dropins", "batchFAT.war"));
            JakartaEE9Action.transformApp(Paths.get(server.getServerRoot(), "dropins", "batchSecurity.war"));
            JakartaEE9Action.transformApp(Paths.get(server.getServerRoot(), "dropins", "DbServletApp.war"));
        }

        server.startServer();

        FatUtils.waitForStartupAndSsl(server);

        FatUtils.waitForLTPA(server);
        FatUtils.waitForRestAPI(server);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testHelloWorld() throws Exception {

        BatchRestUtils restUtils = new BatchRestUtils(server);

        JsonObject jobInstance = restUtils.submitJob("batchFAT", "simple_partition", restUtils.BATCH_BASE_URL);

        jobInstance = restUtils.waitForJobInstanceToFinish(jobInstance.getJsonNumber("instanceId").longValue(), restUtils.BATCH_BASE_URL);

        assertEquals("COMPLETED", jobInstance.getString("batchStatus"));
    }
}
