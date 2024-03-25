/*******************************************************************************
 * Copyright (c) 2016, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.BatchRestUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import componenttest.annotation.ExpectedFFDC;
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

    private static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.fat.memory.persistence");
        HttpUtils.trustAllCertificates();
        
        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBatchSecurityWar(server);

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
    
    /**
     * helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(InMemoryPersistenceTest.class, method, msg);
    }

    /**
     * @return a URL to the target server
     */
    public URL buildURL(String path) throws MalformedURLException {
        URL retMe = new URL("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + path);
        log("buildURL", retMe.toString());
        return retMe;
    }

    @Test
    public void testHelloWorld() throws Exception {

        BatchRestUtils restUtils = new BatchRestUtils(server);

        JsonObject jobInstance = restUtils.submitJob("batchFAT", "simple_partition", restUtils.BATCH_BASE_URL);

        jobInstance = restUtils.waitForJobInstanceToFinish(jobInstance.getJsonNumber("instanceId").longValue(), restUtils.BATCH_BASE_URL);

        assertEquals("COMPLETED", jobInstance.getString("batchStatus"));
    }
    
    @Test
    @ExpectedFFDC({"javax.batch.operations.NoSuchJobInstanceException",
                  "com.ibm.ws.jbatch.rest.internal.resources.RequestException",
                  "com.ibm.ws.jbatch.rest.internal.BatchNoSuchJobInstanceException"})
    public void testPurgeJob() throws Exception {

        String method = "testPurgeJob";
        String jobName = "sleepy_partition";

        BatchRestUtils restUtils = new BatchRestUtils(server);

        //Submit job and validate job instance returned
        JsonObject jobInstance = restUtils.submitJob("batchSecurity", "sleepy_partition", restUtils.BATCH_BASE_URL);

        log(method, "Response: jsonResponse= " + jobInstance.toString());
        String responseJobName = jobInstance.getString("jobName");
        assertEquals(jobName, responseJobName);
        long instanceId = jobInstance.getJsonNumber("instanceId").longValue(); //verifies this is a valid number
        assertEquals(restUtils.ADMIN_USERNAME, jobInstance.getString("submitter"));
        assertEquals("batchSecurity#batchSecurity.war", jobInstance.getString("appName"));

        // Attempt to purge. This should fail as the job is still running.
        restUtils.purgeJobInstanceExpectHttpConflict(instanceId, restUtils.BATCH_BASE_URL, restUtils.ADMIN_USERNAME, restUtils.ADMIN_PASS);


        //Wait for job instance to complete and attempt purge again
        jobInstance = restUtils.waitForJobInstanceToFinish(jobInstance.getJsonNumber("instanceId").longValue(), restUtils.BATCH_BASE_URL);

        assertEquals("COMPLETED", jobInstance.getString("batchStatus"));
        File instanceDir = restUtils.getInstanceDirectory(instanceId);


        restUtils.purgeJobInstance(instanceId, restUtils.BATCH_BASE_URL, restUtils.ADMIN_USERNAME, restUtils.ADMIN_PASS);


        //Check that job log directories were deleted
        assertTrue("Job log directory remained after purge: " + instanceDir.getAbsolutePath(),
                   !instanceDir.exists());

        assertTrue("Job log parent directory remained after purge: " + instanceDir.getParentFile().getAbsolutePath(),
                   !instanceDir.getParentFile().exists());

        //Attempt to purge the already deleted job instance
        restUtils.purgeJobInstanceExpectBadRequest(instanceId, restUtils.BATCH_BASE_URL, restUtils.ADMIN_USERNAME, restUtils.ADMIN_PASS);
    }
}
