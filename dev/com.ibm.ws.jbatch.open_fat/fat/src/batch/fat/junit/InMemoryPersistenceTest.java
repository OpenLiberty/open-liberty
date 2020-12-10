/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.BatchAppUtils;
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
        
        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBatchSecurityWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

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
