/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import concurrentApp.ConcurrentApp;
import concurrentApp.RepeatedTrigger;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class ConcurrencyTest {

    public static final String WAR_APP_NAME = "concurrentApp";
    public static final String SERVER_NAME = "checkpointConcurrency";
    public static final int MARGIN_OF_ERROR = 2000;
    public static final SimpleDateFormat formatter = new SimpleDateFormat("m/d/yy, h:m:s:S z");

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void exportWebApp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_APP_NAME + ".war")
                        .addClass(ConcurrentApp.class)
                        .addClass(RepeatedTrigger.class);
        ShrinkHelper.exportAppToServer(server, war, DeployOptions.OVERWRITE);
    }

    private long getTimeElapsed(String scheduledTaskLog, String taskExecutionLog) throws Exception {
        assertNotNull("Log inidating liberty resumed from checkpoint not found", scheduledTaskLog);
        assertNotNull("Schduled task not executed", taskExecutionLog);
        String timestampStr1 = scheduledTaskLog.substring(scheduledTaskLog.indexOf("[") + 1, scheduledTaskLog.indexOf("]"));
        Date timestamp1 = formatter.parse(timestampStr1);
        String timestampStr2 = taskExecutionLog.substring(taskExecutionLog.indexOf("[") + 1, taskExecutionLog.indexOf("]"));
        Date timestamp2 = formatter.parse(timestampStr2);
        return Math.abs(timestamp2.getTime() - timestamp1.getTime());
    }

    @Test
    public void testSchduledTaskTenSec() throws Exception {
        Integer scheduledDelay = 10000;

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("scheduledTime").setValue(scheduledDelay.toString());
        config.getVariables().getById("repeatTrigger").setValue("false");
        config.getVariables().getById("repeatManagedExec").setValue("false");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, (server) -> {
            String result = server.waitForStringInLog("Thread scheduled at startup", 20000);
            assertNotNull("Scheduled executor not called at checkpoint", result);
        });
        server.startServer();
        Thread.sleep(10000);
        server.checkpointRestore();
        String scheduledTaskLog = server.waitForStringInLog("The Liberty server process resumed operation from a checkpoint", 15000);
        String taskExecutionLog = server.waitForStringInLog("Scheduled thread completed", 20000);
        long timeElapsed = getTimeElapsed(scheduledTaskLog, taskExecutionLog);
        assertTrue("Expected and Observed delays differ. Expected: " + scheduledDelay +
                   " ms | Observed: " + timeElapsed + " ms",
                   Math.abs(timeElapsed - scheduledDelay) < MARGIN_OF_ERROR);
    }

    @Test
    public void testScheduledTaskTwentySec() throws Exception {
        Integer scheduledDelay = 20000;

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("scheduledTime").setValue(scheduledDelay.toString());
        config.getVariables().getById("repeatTrigger").setValue("false");
        config.getVariables().getById("repeatManagedExec").setValue("false");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, (server) -> {
            String result = server.waitForStringInLog("Thread scheduled at startup", 20000);
            assertNotNull("Scheduled executor not called at checkpoint", result);
        });
        server.startServer();
        Thread.sleep(10000);
        server.checkpointRestore();
        String scheduledTaskLog = server.waitForStringInLog("The Liberty server process resumed operation from a checkpoint", 15000);
        String taskExecutionLog = server.waitForStringInLog("Scheduled thread completed", 20000);
        long timeElapsed = getTimeElapsed(scheduledTaskLog, taskExecutionLog);
        assertTrue("Expected and Observed delays differ. Expected: " + scheduledDelay +
                   " ms | Observed: " + timeElapsed + " ms",
                   Math.abs(timeElapsed - scheduledDelay) < MARGIN_OF_ERROR);

    }

    @Test
    public void testRepeatedTrigger() throws Exception {
        Integer scheduledDelay = 15000;

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("repeatTrigger").setValue("true");
        config.getVariables().getById("scheduledTime").setValue(scheduledDelay.toString());
        config.getVariables().getById("repeatManagedExec").setValue("false");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, (server) -> {
            String result = server.waitForStringInLog("Scheduled thread completed", 20000);
            assertNotNull("Repeated task not called at checkpoint", result);
        });
        server.startServer();
        Thread.sleep(10000);
        server.checkpointRestore();
        String scheduledTaskLog = server.waitForStringInLog("The Liberty server process resumed operation from a checkpoint", 15000);
        String taskExecutionLog = server.waitForStringInLog("Scheduled thread completed", 20000);
        long timeElapsed = getTimeElapsed(scheduledTaskLog, taskExecutionLog);
        assertTrue("Expected and Observed delays differ. Expected: " + scheduledDelay +
                   " ms | Observed: " + timeElapsed + " ms",
                   Math.abs(timeElapsed - scheduledDelay) < MARGIN_OF_ERROR);
    }

    @Test
    public void testRepeatedTriggerTwentySec() throws Exception {
        Integer scheduledDelay = 20000;

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("repeatTrigger").setValue("true");
        config.getVariables().getById("scheduledTime").setValue(scheduledDelay.toString());
        config.getVariables().getById("repeatManagedExec").setValue("false");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, (server) -> {
            String result = server.waitForStringInLog("Scheduled thread completed", 20000);
            assertNotNull("Repeated task not called at checkpoint", result);
        });
        server.startServer();
        Thread.sleep(10000);
        server.checkpointRestore();
        String scheduledTaskLog = server.waitForStringInLog("The Liberty server process resumed operation from a checkpoint", 15000);
        String taskExecutionLog = server.waitForStringInLog("Scheduled thread completed", 20000);
        long timeElapsed = getTimeElapsed(scheduledTaskLog, taskExecutionLog);
        assertTrue("Expected and Observed delays differ. Expected: " + scheduledDelay +
                   " ms | Observed: " + timeElapsed + " ms",
                   Math.abs(timeElapsed - scheduledDelay) < MARGIN_OF_ERROR);
    }

    @Test
    public void testRepeatedMangedExecTenSec() throws Exception {
        Integer scheduledDelay = 10000;

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("repeatTrigger").setValue("true");
        config.getVariables().getById("scheduledTime").setValue(scheduledDelay.toString());
        config.getVariables().getById("repeatManagedExec").setValue("false");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, (server) -> {
            String result = server.waitForStringInLog("Thread scheduled at startup", 20000);
            assertNotNull("Repeated task not called at checkpoint", result);
        });
        server.startServer();
        Thread.sleep(10000);
        server.checkpointRestore();
        String scheduledTaskLog = server.waitForStringInLog("The Liberty server process resumed operation from a checkpoint", 15000);
        String taskExecutionLog = server.waitForStringInLog("Scheduled thread completed", 20000);
        long timeElapsed = getTimeElapsed(scheduledTaskLog, taskExecutionLog);
        assertTrue("Expected and Observed delays differ. Expected: " + scheduledDelay +
                   " ms | Observed: " + timeElapsed + " ms",
                   Math.abs(timeElapsed - scheduledDelay) < MARGIN_OF_ERROR);
    }

    @Test
    public void testRepeatedMangedExecTwentySec() throws Exception {
        Integer scheduledDelay = 20000;

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("repeatTrigger").setValue("true");
        config.getVariables().getById("scheduledTime").setValue(scheduledDelay.toString());
        config.getVariables().getById("repeatManagedExec").setValue("false");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, (server) -> {
            String result = server.waitForStringInLog("Thread scheduled at startup", 20000);
            assertNotNull("Repeated task not called at checkpoint", result);
        });
        server.startServer();
        Thread.sleep(10000);
        server.checkpointRestore();
        String scheduledTaskLog = server.waitForStringInLog("The Liberty server process resumed operation from a checkpoint", 15000);
        String taskExecutionLog = server.waitForStringInLog("Scheduled thread completed", 20000);
        long timeElapsed = getTimeElapsed(scheduledTaskLog, taskExecutionLog);
        assertTrue("Expected and Observed delays differ. Expected: " + scheduledDelay +
                   " ms | Observed: " + timeElapsed + " ms",
                   Math.abs(timeElapsed - scheduledDelay) < MARGIN_OF_ERROR);
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }
}
