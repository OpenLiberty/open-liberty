/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

/**
 * Start server, directly prime db with entries, restart server.
 * Use Job Operator to query execution id(s) that were created in the first step
 * Verify their status
 */
@RunWith(FATRunner.class)
public class LocalServerJobRecoveryAtStartUpTest extends BatchFATHelper {

    private static List<Long> executionInstances = new ArrayList<Long>();
    private static final Class testClass = LocalServerJobRecoveryAtStartUpTest.class;

    @BeforeClass
    public static void setup() throws Exception {
        BatchFATHelper.setConfig("LocalJobRecoveryResources/server.xml", testClass);

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        BatchFATHelper.startServer(server, testClass);
        FatUtils.waitForSmarterPlanet(server);

        createDefaultRuntimeTables();

        setUpDB();

        restartServerAndWaitForAppStart();

        HttpUtils.trustAllCertificates();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Send a request to servlet to prime database
     * Receive a list of execution ids back.
     * They will be used in the tests
     */
    public static void setUpDB() throws IOException {
        String urlParms = "testName=setUpDB&hostName=" + server.getHostname() +
                          "&userDir=" + server.getUserDir() + "&serverName=" + server.getServerName();

        String urlAppend = (urlParms == null ? null : "?" + urlParms);

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
                          "/" + "batchFAT" + "/" + "StartUpRecovery" + urlAppend);
        String output = HttpUtils.getHttpResponseAsString(url);

        assertNotNull(output);
        assertNotNull(output.trim());

        //parse output for data to input to test case.
        executionInstances = convert(output.trim());
    }

    private static List<Long> convert(String string) {
        String[] strings = string.replace("[", "").replace("]", "").split(", ");
        List<Long> result = new ArrayList<Long>();
        for (int i = 0; i < strings.length; i++) {
            result.add(Long.parseLong(strings[i]));
        }
        return result;
    }

    @Test
    public void recoverLocalJobsInStartingTest() throws Exception {
        test("StartUpRecovery", "testName=recoverLocalJobsInStartingTest&executionId=" +
                                executionInstances.get(0) + "&expectedStatus=" + "FAILED");
    }

    @Test
    public void recoverLocalJobsInStartedTest() throws Exception {
        test("StartUpRecovery", "testName=recoverLocalJobsInStartedTest&executionId=" +
                                executionInstances.get(1) + "&expectedStatus=" + "FAILED");
    }

    @Test
    public void recoverLocalJobsInStartedWithExitStatusSetTest() throws Exception {
        test("StartUpRecovery", "testName=recoverLocalJobsInStartedWithExitStatusSetTest&executionId=" +
                                executionInstances.get(2) + "&expectedStatus=" + "FAILED");
    }

    @Test
    public void recoverLocalJobsInStoppingTest() throws Exception {
        test("StartUpRecovery", "testName=recoverLocalJobsInStoppingTest&executionId=" +
                                executionInstances.get(3) + "&expectedStatus=" + "FAILED");
    }

    @Test
    public void recoverLocalJobsInStoppedTest() throws Exception {
        test("StartUpRecovery", "testName=recoverLocalJobsInStoppedTest&executionId=" +
                                executionInstances.get(4) + "&expectedStatus=" + "STOPPED");
    }

    @Test
    public void recoverStepExecutionInStartingTest() throws Exception {
        test("StartUpRecovery", "testName=recoverStepExecutionInStartingTest&executionId=" +
                                executionInstances.get(0) + "&expectedStatus=" + "FAILED");
    }

    @Test
    public void recoverStepExecutionInStartedTest() throws Exception {
        test("StartUpRecovery", "testName=recoverStepExecutionInStartedTest&executionId=" +
                                executionInstances.get(1) + "&expectedStatus=" + "FAILED");
    }

    @Test
    public void recoverStepExecutionInStartedWithExitStatusSetTest() throws Exception {
        test("StartUpRecovery", "testName=recoverStepExecutionInStartedWithExitStatusSetTest&executionId=" +
                                executionInstances.get(2) + "&expectedStatus=" + "FAILED");
    }

    @Test
    public void recoverStepExecutionInStoppingTest() throws Exception {
        test("StartUpRecovery", "testName=recoverStepExecutionInStoppingTest&executionId=" +
                                executionInstances.get(3) + "&expectedStatus=" + "FAILED");
    }

    @Test
    public void recoverStepExecutionInStoppedTest() throws Exception {
        test("StartUpRecovery", "testName=recoverStepExecutionInStoppedTest&executionId=" +
                                executionInstances.get(4) + "&expectedStatus=" + "STOPPED");
    }

}
