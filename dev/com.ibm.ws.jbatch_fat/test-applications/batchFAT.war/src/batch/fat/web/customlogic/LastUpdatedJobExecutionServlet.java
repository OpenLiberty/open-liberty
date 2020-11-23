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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.api.chunk.listener.AbstractChunkListener;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import batch.fat.artifacts.EndOfJobNotificationListener;
import batch.fat.common.util.JobWaiter;
import batch.fat.common.util.TestFailureException;
import batch.fat.util.BatchFATHelper;

/**
 *
 */
@WebServlet(urlPatterns = { "/LastUpdatedJobExecutionServlet/*" })
public class LastUpdatedJobExecutionServlet extends HttpServlet {

    private final static Logger logger = Logger.getLogger("test");

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

        //
        // 2. Wait for completion
        JobWaiter waiter = new JobWaiter();
        try {
            // All tests expect COMPLETED status
            JobExecution jobExec = waiter.waitForAfterJobNotificationThenFinalState(EndOfJobNotificationListener.class, execID);
            //
            // 3. Now that it's complete do further verification based on the specific variation.
            //
            verifyResults(pw, jobExec, testName);

            // 4a. For simple "screen scraping" parsing
            pw.println(BatchFATHelper.SUCCESS_MESSAGE);

        } catch (Exception e) {
            // 4b. Key signifying error.
            pw.println("ERROR: " + e.getMessage());
        }
    }

    private void verifyResults(PrintWriter pw, JobExecution je, String testName) throws TestFailureException {

        // Common stuff
        JobOperator jobOp = BatchRuntime.getJobOperator();
        long execId = je.getExecutionId();

        // Tests
        if (testName.equals("testLastUpdatedJobExecution")) {
            JobExecution jex = jobOp.getJobExecution(execId);

            assertEquals(BatchStatus.COMPLETED, jex.getBatchStatus());

            // Check that last updated time equals end time
            // 
            // Perhaps there could be a reason one day to relax/remove this check, I don't think the
            // spec or contract demands they are the same.  For now it's  sanity check that the code 
            // is functioning as expected.
            assertEquals("Last updated time doesn't match end time", jex.getEndTime().getTime(), jex.getLastUpdatedTime().getTime());
            // Test includes sleep so super-fast execution shouldn't be a concern.
            assertTrue("Last updated time isn't greater than create time", (jex.getLastUpdatedTime().getTime() - jex.getCreateTime().getTime()) > 0);

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

    public static class ChunkListener extends AbstractChunkListener {

        @Inject
        JobContext jobCtx;

        @Inject
        StepContext stepCtx;

        @Override
        public void beforeChunk() throws Exception {
            // Only for debug
            Integer chunkNum = (Integer) stepCtx.getTransientUserData();
            if (chunkNum == null) {
                chunkNum = new Integer(0);
            } else {
                chunkNum++;
            }
            logger.fine("Beginning chunk number: " + chunkNum);
            stepCtx.setTransientUserData(chunkNum);
        }

        @Override
        public void afterChunk() throws Exception {
            logger.finer("In chunk number: " + stepCtx.getTransientUserData());

            JobExecution jobEx = BatchRuntime.getJobOperator().getJobExecution(jobCtx.getExecutionId());
            Long newLastUpdated = jobEx.getLastUpdatedTime().getTime();
            logger.finer("current last updated: " + newLastUpdated);

            Long previousLastUpdated = (Long) jobCtx.getTransientUserData();
            logger.finer("previous last updated: " + previousLastUpdated);

            if (previousLastUpdated != null) {
                assertTrue("Last updated time hasn't advanced, in chunk number: " + stepCtx.getTransientUserData(),
                           (newLastUpdated - previousLastUpdated) > 0);
            } else {
                logger.finer("No comparable yet.");
            }

            jobCtx.setTransientUserData(newLastUpdated);

            delay();
        }

        private void delay() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.fine("Calculation: " + Math.pow(Math.random(), Math.random()));

        }
    }

}
