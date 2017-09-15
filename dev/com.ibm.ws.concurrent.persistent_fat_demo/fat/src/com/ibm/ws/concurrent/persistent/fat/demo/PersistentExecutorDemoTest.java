/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.concurrent.persistent.fat.demo;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests using the demo app for persistent scheduled executor
 */
public class PersistentExecutorDemoTest extends FATServletClient {

    public static final String APP_NAME = "persistentdemo";

    private static final LibertyServer server = FATSuite.server;

    private static final String FAILURE_OUTPUT = "web.DemoTaskException: Intentionally failed";
    private static final String SUCCESSFUL_SCHEDULE = "Successfully scheduled task ";

    /**
     * Interval in milliseconds between polling for task results.
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Maximum number of milliseconds to wait for a task to finish.
     */
    private static final long TIMEOUT = 10000;

    /**
     * Runs a test in the servlet.
     *
     * @param queryString query string. Can be empty. Otherwise, if specified, must start with the ? character.
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    protected StringBuilder runInServlet(String queryString) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/persistentdemo" + queryString);
        Log.info(getClass(), "runInServlet", "URL is " + url);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(getClass(), "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                Log.info(getClass(), "runInServlet", "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }

            return lines;
        } finally {
            con.disconnect();
            Log.info(getClass(), "runInServlet", "disconnected from servlet");
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage("web");
        ShrinkHelper.exportToServer(server, "dropins", app);
        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKC1501W", "CWWKC1511W");
    }

    /**
     * Schedule a repeating task. Cancel it. Verify the canceled status.
     */
    @Test
    public void testCancelTask() throws Exception {
        StringBuilder output = runInServlet("?newTaskName=TaskA&initialDelay=3&interval=2&units=SECONDS");
        int index = output.indexOf(SUCCESSFUL_SCHEDULE);
        if (index < 0)
            throw new Exception("Didn't find message about successful scheduling in " + output);
        String taskId = output.substring(index + SUCCESSFUL_SCHEDULE.length(), output.indexOf("<", index));

        output = runInServlet("?" + taskId + "=Cancel");
        if (output.indexOf("Successfully canceled task " + taskId) < 0)
            throw new Exception("Missing message about successful cancel of task " + taskId + " in servlet output. " + output);

        if (output.indexOf("ENDED,CANCEL") < 0)
            throw new Exception("Missing ENDED/CANCELED status for task " + taskId + " in servlet output. " + output);
    }

    /**
     * Schedule a task, tell it to fail the next execution. Verify that it attempts the execution twice
     * and then shows the result as the expected exception.
     */
    @Test
    public void testFailingTask() throws Exception {
        StringBuilder output = runInServlet("?newTaskName=TaskB&initialDelay=4&interval=1&units=SECONDS");
        int index = output.indexOf(SUCCESSFUL_SCHEDULE);
        if (index < 0)
            throw new Exception("Didn't find message about successful scheduling in " + output);
        String taskId = output.substring(index + SUCCESSFUL_SCHEDULE.length(), output.indexOf("<", index));

        output = runInServlet("?" + taskId + "=Fail");
        if (output.indexOf("Persistent Executor Demo") < 0)
            throw new Exception("Failed to invoke servlet. " + output);

        int count = server.waitForMultipleStringsInLog(2, "Task " + taskId + " attempting execution 1");
        if (count != 2)
            throw new Exception("Found wrong number of messages " + count + " about task " + taskId + " execution within allotted interval.");

        for (long start = System.currentTimeMillis(); output.indexOf(FAILURE_OUTPUT) < 0 && System.currentTimeMillis() - start < TIMEOUT;) {
            Thread.sleep(POLL_INTERVAL);
            output = runInServlet("");
        }
        if (output.indexOf(FAILURE_OUTPUT) < 0)
            throw new Exception("Task " + taskId + " failure result missing from servlet output. " + output);
    }

    /**
     * Schedule a task to run once. Verify that it runs once.
     */
    @Test
    public void testOneShotTask() throws Exception {
        server.setMarkToEndOfLog();
        StringBuilder output = runInServlet("?newTaskName=TaskC&initialDelay=0&units=SECONDS");
        int index = output.indexOf(SUCCESSFUL_SCHEDULE);
        if (index < 0)
            throw new Exception("Didn't find message about successful scheduling in " + output);
        String taskId = output.substring(index + SUCCESSFUL_SCHEDULE.length(), output.indexOf("<", index));
        String found = server.waitForStringInLogUsingMark("Task " + taskId + " attempting execution 1");
        if (found == null)
            throw new Exception("Did not find message from task " + taskId + " execution within allotted interval.");

        for (long start = System.currentTimeMillis(); output.indexOf("RESULT-1") < 0 && System.currentTimeMillis() - start < TIMEOUT;) {
            Thread.sleep(POLL_INTERVAL);
            output = runInServlet("");
        }
        if (output.indexOf("RESULT-1") < 0)
            throw new Exception("Task " + taskId + " result missing or incorrect in servlet output. " + output);
    }

    /**
     * Schedule a repeating task. Remove it. Verify it was removed.
     */
    @Test
    public void testRemoveTask() throws Exception {
        StringBuilder output = runInServlet("?newTaskName=TaskD&initialDelay=2&interval=4&units=SECONDS");
        int index = output.indexOf(SUCCESSFUL_SCHEDULE);
        if (index < 0)
            throw new Exception("Didn't find message about successful scheduling in " + output);
        String taskId = output.substring(index + SUCCESSFUL_SCHEDULE.length(), output.indexOf("<", index));

        output = runInServlet("?" + taskId + "=Remove");
        if (output.indexOf("Successfully removed task " + taskId) < 0)
            throw new Exception("Missing message about successful remove of task " + taskId + " in servlet output. " + output);

        if (output.indexOf(">" + taskId + "<") > 0)
            throw new Exception("Status for removed task " + taskId + " should not appear in servlet output. " + output);
    }

    /**
     * Schedule tasks in the distant future. Remove them by name and state. Verify they are gone.
     */
    @Test
    public void testRemoveTasksByNameAndState() throws Exception {
        StringBuilder output = runInServlet("?newTaskName=TaskE1&initialDelay=5&units=DAYS");
        int index = output.indexOf(SUCCESSFUL_SCHEDULE);
        if (index < 0)
            throw new Exception("Didn't find message about successful scheduling of TaskE1 in " + output);
        String taskId_E1 = output.substring(index + SUCCESSFUL_SCHEDULE.length(), output.indexOf("<", index));

        output = runInServlet("?newTaskName=TaskE2&initialDelay=6&units=HOURS");
        index = output.indexOf(SUCCESSFUL_SCHEDULE);
        if (index < 0)
            throw new Exception("Didn't find message about successful scheduling of TaskE2 in " + output);
        String taskId_E2 = output.substring(index + SUCCESSFUL_SCHEDULE.length(), output.indexOf("<", index));

        output = runInServlet("?removeTaskPattern=TaskE_&state=UNATTEMPTED&inState=true");
        if (output.indexOf("Successfully removed 2 tasks") < 0)
            throw new Exception("Missing message about successful removal of 2 tasks in servlet output. " + output);

        if (output.indexOf(">" + taskId_E1 + "<") > 0)
            throw new Exception("Status for removed task " + taskId_E1 + " (TaskE1) should not appear in servlet output. " + output);

        if (output.indexOf(">" + taskId_E2 + "<") > 0)
            throw new Exception("Status for removed task " + taskId_E2 + " (TaskE2) should not appear in servlet output. " + output);
    }

    /**
     * Schedule a task to run with the persistent executor's transaction suspended. Verify that it runs once.
     */
    @Test
    public void testSuspendExecutorsTransaction() throws Exception {
        server.setMarkToEndOfLog();
        StringBuilder output = runInServlet("?newTaskName=TaskF&initialDelay=0&units=HOURS&suspendTran=true");
        int index = output.indexOf(SUCCESSFUL_SCHEDULE);
        if (index < 0)
            throw new Exception("Didn't find message about successful scheduling in " + output);
        String taskId = output.substring(index + SUCCESSFUL_SCHEDULE.length(), output.indexOf("<", index));
        String found = server.waitForStringInLogUsingMark("Task " + taskId + " attempting execution 1");
        if (found == null)
            throw new Exception("Did not find message from task " + taskId + " execution within allotted interval.");

        for (long start = System.currentTimeMillis(); output.indexOf("RESULT-1") < 0 && System.currentTimeMillis() - start < TIMEOUT;) {
            Thread.sleep(POLL_INTERVAL);
            output = runInServlet("");
        }
        if (output.indexOf("RESULT-1") < 0)
            throw new Exception("Task " + taskId + " result missing or incorrect in servlet output. " + output);
    }

    /**
     * Schedule a task to run 3 times. Skip one of the attempts. Verify that the skipped attempt is reattempted,
     * and that it does run 3 times.
     */
    @Test
    public void testThreeTimeTaskWithSkip() throws Exception {
        server.setMarkToEndOfLog();
        StringBuilder output = runInServlet("?newTaskName=TaskG&initialDelay=1&interval=2&units=SECONDS&numExecutions=3");
        int index = output.indexOf(SUCCESSFUL_SCHEDULE);
        if (index < 0)
            throw new Exception("Didn't find message about successful scheduling in " + output);
        String taskId = output.substring(index + SUCCESSFUL_SCHEDULE.length(), output.indexOf("<", index));

        output = runInServlet("?" + taskId + "=Skip");
        if (output.indexOf("Persistent Executor Demo") < 0)
            throw new Exception("Failed to invoke servlet. " + output);

        String found = server.waitForStringInLogUsingMark("Task " + taskId + " execution skipped");
        if (found == null)
            throw new Exception("Did not find message about skipped task " + taskId + " within allotted interval.");
        int count = server.waitForMultipleStringsInLog(3, "Task " + taskId + " attempting execution ");
        if (count != 3)
            throw new Exception("Found wrong number of messages " + count + " about task " + taskId + " execution within allotted interval.");

        for (long start = System.currentTimeMillis(); output.indexOf("RESULT-3") < 0 && System.currentTimeMillis() - start < TIMEOUT;) {
            Thread.sleep(POLL_INTERVAL);
            output = runInServlet("");
        }
        if (output.indexOf("RESULT-3") < 0)
            throw new Exception("Task " + taskId + " result missing or incorrect in servlet output. " + output);
    }
}