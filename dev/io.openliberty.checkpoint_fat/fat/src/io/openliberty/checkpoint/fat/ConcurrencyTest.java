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
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import concurrentApp.ConcurrentApp;
import concurrentApp.RepeatedTrigger;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class ConcurrencyTest {

    public static final String WAR_APP_NAME = "concurrentApp";
    public static final String SERVER_NAME = "checkpointConcurrency";
    public static final int MARGIN_OF_ERROR = 2000;
    public static final SimpleDateFormat formatter = new SimpleDateFormat("m/d/yy, h:m:s:S z");

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultMPRepeat(SERVER_NAME);

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

    /*
     * Tests scheduling a task at checkpoint with a seven second delay through the ManagedExecutor,
     * then waiting ten seconds between checkpoint and restore and checking the timestamp to ensure
     * that there is a seven second delay upon restore (inculding a two second margin of error)
     */
    @Test
    public void testSchduledTask() throws Exception {
        Integer scheduledDelay = 7000;

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("scheduledTime").setValue(scheduledDelay.toString());
        config.getVariables().getById("repeatTrigger").setValue("false");
        config.getVariables().getById("repeatManagedExec").setValue("false");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, (server) -> {
            String result = server.waitForStringInLog("Thread scheduled at startup");
            assertNotNull("Scheduled executor not called at checkpoint", result);
        });
        server.startServer();
        Thread.sleep(10000);
        server.checkpointRestore();
        String scheduledTaskLog = server.waitForStringInLog("The Liberty server process resumed operation from a checkpoint", 15000);
        String taskExecutionLog = server.waitForStringInLog("Scheduled thread completed", 20000);
        long timeElapsed = getTimeElapsed(scheduledTaskLog, taskExecutionLog);
        System.out.println("time Elapsed: " + timeElapsed);
        assertTrue("Expected and Observed delays differ greater than the margin of error. Expected: " + scheduledDelay +
                   "ms | Observed: " + timeElapsed + "ms | margin of error: " + MARGIN_OF_ERROR + "ms",
                   Math.abs(timeElapsed - scheduledDelay) < MARGIN_OF_ERROR);
    }

    /*
     * Tests starting a repeated task at checkpoint with a ten second repeat interval using a trigger,
     * then waiting seven seconds between checkpoint and restore and checking the timestamp to ensure
     * that there is a seven second delay upon restore (inculding a two second margin of error)
     */
    @Test
    public void testRepeatedTrigger() throws Exception {
        Integer scheduledDelay = 10000;

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("repeatTrigger").setValue("true");
        config.getVariables().getById("scheduledTime").setValue(scheduledDelay.toString());
        config.getVariables().getById("repeatManagedExec").setValue("false");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, (server) -> {
            String result = server.waitForStringInLog("Scheduled thread completed");
            assertNotNull("Repeated task not called at checkpoint", result);
        });
        server.startServer();
        Thread.sleep(7000);
        server.checkpointRestore();
        String scheduledTaskLog = server.waitForStringInLog("The Liberty server process resumed operation from a checkpoint", 15000);
        String taskExecutionLog = server.waitForStringInLog("Scheduled thread completed");
        long timeElapsed = getTimeElapsed(scheduledTaskLog, taskExecutionLog);
        System.out.println("time Elapsed: " + timeElapsed);
        assertTrue("Expected and Observed delays differ greater than the margin of error. Expected: " + scheduledDelay +
                   "ms | Observed: " + timeElapsed + "ms | margin of error: " + MARGIN_OF_ERROR + "ms",
                   Math.abs(timeElapsed - scheduledDelay) < MARGIN_OF_ERROR);
    }

    /*
     * Tests starting a repeated task at checkpoint with a seven second repeat interval using the
     * managedExecutor's repeating function. then waiting ten seconds between checkpoint and
     * restore and checking the timestamp to ensure that there is a seven second delay upon restore
     * (inculding a two second margin of error)
     */
    @Test
    public void testRepeatedMangedExecTenSec() throws Exception {
        Integer scheduledDelay = 7000;

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("repeatTrigger").setValue("false");
        config.getVariables().getById("scheduledTime").setValue(scheduledDelay.toString());
        config.getVariables().getById("repeatManagedExec").setValue("true");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, (server) -> {
            String result = server.waitForStringInLog("Thread scheduled at startup");
            assertNotNull("Repeated task not scheduled at checkpoint", result);
        });
        server.startServer();
        Thread.sleep(10000);
        server.checkpointRestore();
        String scheduledTaskLog = server.waitForStringInLog("The Liberty server process resumed operation from a checkpoint", 15000);
        String taskExecutionLog = server.waitForStringInLog("Scheduled thread completed", 20000);
        long timeElapsed = getTimeElapsed(scheduledTaskLog, taskExecutionLog);
        System.out.println("time Elapsed: " + timeElapsed);
        assertTrue("Expected and Observed delays differ greater than the margin of error. Expected: " + scheduledDelay +
                   " ms | Observed: " + timeElapsed + " ms | margin of error: " + MARGIN_OF_ERROR,
                   Math.abs(timeElapsed - scheduledDelay) < MARGIN_OF_ERROR);
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }
}
