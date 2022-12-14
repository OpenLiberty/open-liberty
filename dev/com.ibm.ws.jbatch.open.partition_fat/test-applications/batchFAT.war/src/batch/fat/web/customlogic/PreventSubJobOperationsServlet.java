/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.web.customlogic;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchRuntime;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import batch.fat.artifacts.EndOfJobNotificationListener;
import batch.fat.common.util.JobWaiter;
import batch.fat.util.BatchFATHelper;

@WebServlet(name = "PreventSubJobOperations", urlPatterns = { "/PreventSubJobOperations" })
public class PreventSubJobOperationsServlet extends HttpServlet {

    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    private final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    public class TestHelper {

        String testName;

        public TestHelper(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            testName = req.getParameter("testName");
        }

        private void execute() throws Exception {
            String jslName = null;
            if (testName.equals("testCannotOperateOnSplitFlowSubJobs")) {
                jslName = "SimpleFailingSplitFlow";
            } else if (testName.equals("testCannotOperateOnPartitionSubJobs")) {
                jslName = "SimpleFailingPartition";
            } else {
                throw new IllegalArgumentException("Unexpected testName = " + testName);
            }

            // Start jobs, creating "gap" between top-level jobs.  Waiting for failure 
            // allows to rule out the notion that the absence of a gap is simply because
            // the top-level execution hasn't advanced to the point of subjob creation yet.

            long execID1 = jobOperator.start(jslName, null);

            new JobWaiter(JobWaiter.FAILED_STATE_ONLY).waitForAfterJobNotificationThenFinalState(EndOfJobNotificationListener.class, execID1);
            long execID2 = jobOperator.start(jslName, null);
            new JobWaiter(JobWaiter.FAILED_STATE_ONLY).waitForAfterJobNotificationThenFinalState(EndOfJobNotificationListener.class, execID2);

            long gap = execID2 - execID1;
            long subJobID = execID1 + 1;

            // If this assertion were to fail, it would be because we started allocating subjob ids differently.
            // So in such a case, it would be the test that had become invalid, rather than a runtime bug having been
            // introduced.
            if (gap < 2) {
                throw new IllegalStateException("Expecting a gap between consecutive top-level executions with IDs: " + execID1 + ", and " + execID2);
            }

            boolean caughtOnRestart = false;
            try {
                jobOperator.restart(subJobID, null);
            } catch (NoSuchJobExecutionException e) {
                caughtOnRestart = true;
            }
            if (!caughtOnRestart) {
                throw new IllegalStateException("Expecting to catch NoSuchJobExecutionException restarting execution: " + subJobID + ", but didn't.");
            }

            boolean caughtOnAbandon = false;
            try {
                jobOperator.abandon(subJobID);
            } catch (NoSuchJobExecutionException e) {
                caughtOnAbandon = true;
            }
            if (!caughtOnAbandon) {
                throw new IllegalStateException("Expecting to catch NoSuchJobExecutionException abandoning execution: " + subJobID + ", but didn't.");
            }

            Properties jobParams = new Properties();
            jobParams.setProperty("sleep", "10");
            long execID3 = jobOperator.start(jslName, jobParams);
            long subJobID2 = execID3 + 1;
            Thread.sleep(3000);

            boolean caughtOnStop = false;
            try {
                jobOperator.stop(subJobID2);
            } catch (NoSuchJobExecutionException e) {
                caughtOnStop = true;
            }
            if (!caughtOnStop) {
                throw new IllegalStateException("Expecting to catch NoSuchJobExecutionException stopping execution: " + subJobID2 + ", but didn't.");
            }

        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        logger.info("In batch test servlet, called with URL: " + req.getRequestURL() + "?" + req.getQueryString());
        resp.setContentType("text/plain");
        TestHelper helper = new TestHelper(req, resp);
        try {
            helper.execute();
            // Declare success
            resp.getWriter().println(BatchFATHelper.SUCCESS_MESSAGE);
        } catch (Exception e) {
            String errorMsg = "ERROR: " + e.getMessage();
            logger.severe(errorMsg);
            resp.getWriter().println(errorMsg);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.getWriter().print("use GET method");
        resp.setStatus(200);
    }
}
