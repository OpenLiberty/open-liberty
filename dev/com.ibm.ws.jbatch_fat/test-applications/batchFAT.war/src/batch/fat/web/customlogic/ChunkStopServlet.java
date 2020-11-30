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
package batch.fat.web.customlogic;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import batch.fat.artifacts.EndOfJobNotificationListener;
import batch.fat.common.util.JobWaiter;
import batch.fat.common.util.TestFailureException;
import batch.fat.util.BatchFATHelper;

@WebServlet(urlPatterns = { "/ChunkStopServlet/*" })
public class ChunkStopServlet extends HttpServlet {

    private final static Logger logger = Logger.getLogger("test");
    private final static int SLEEP_TIME = 2000;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("In batch test servlet, called with URL: " + request.getRequestURL() + "?" + request.getQueryString());
        response.setContentType("text/plain");
        PrintWriter pw = response.getWriter();
        String jslName = request.getParameter("jslName");
        String testName = request.getParameter("testName");
        executeJob(pw, jslName, testName);
    }

    private void executeJob(PrintWriter pw, String jslName, String testName) {

        //
        // 1. Start job
        //
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties props = new Properties();

        long execID = jobOperator.start(jslName, props);

        try {
            // Verify based on execId
            verifyResults(pw, execID, testName);

            // 4a. For simple "screen scraping" parsing
            pw.println(BatchFATHelper.SUCCESS_MESSAGE);

        } catch (Exception e) {
            // 4b. Key signifying error.
            pw.println("ERROR: " + e.getMessage());
        }
    }

    private void verifyResults(PrintWriter pw, long execId, String testName) throws TestFailureException {

        // Common stuff
        JobOperator jobOp = BatchRuntime.getJobOperator();

        // Tests
        if (testName.equals("chunkStop")) {

            try {
                // Give it some time to get started before stopping
                Thread.sleep(SLEEP_TIME * 2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            jobOp.stop(execId);

            JobWaiter waiter = new JobWaiter(JobWaiter.STOPPING_OR_STOPPED_STATES, new BatchStatus[] { BatchStatus.STOPPED });
            JobExecution jobExec =
                            waiter.waitForAfterJobNotificationThenFinalState(EndOfJobNotificationListener.class, execId);

            StepExecution step = jobOp.getStepExecutions(execId).get(0);
            assertEquals("Step BatchStatus: ", BatchStatus.STOPPED, step.getBatchStatus());

        } else {
            throw new IllegalArgumentException("Not expecting testName = " + testName);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        PrintWriter pw = response.getWriter();
        pw.print("use GET method");
        response.setStatus(200);
    }

    public static class Reader extends AbstractItemReader {
        @Override
        public Object readItem() throws Exception {
            Thread.sleep(SLEEP_TIME);
            return Long.toString(System.currentTimeMillis());
        }
    }

    public static class Writer extends AbstractItemWriter {
        @Override
        public void writeItems(List<Object> items) throws Exception {
            Thread.sleep(SLEEP_TIME);
            for (Object item : items) {
                logger.finer("Next item: " + item);
            }
        }
    }
}
