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
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;
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
import batch.fat.util.BatchFatUtils;

@WebServlet(urlPatterns = { "/FlowTransitionIllegalServlet/*" })
public class FlowTransitionIllegalServlet extends HttpServlet {

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
        if (testName.equals("flowTransitionIllegal")) {

            JobWaiter waiter = new JobWaiter(JobWaiter.STARTED_OR_STARTING, JobWaiter.FAILED_STATE_ONLY);
            JobExecution jobExec =
                            waiter.waitForAfterJobNotificationThenFinalState(EndOfJobNotificationListener.class, execId);

            // We've already asserted the job failed.  Ensure that the steps themselves completed successfully, letting us
            // conclude it was the illegal transition causing the job failure.
            Map<String, StepExecution> stepMap = BatchFatUtils.getStepExecutionMap(jobOp.getStepExecutions(execId));
            assertEquals("Check status of first step", BatchStatus.COMPLETED, stepMap.get("flow1step1").getBatchStatus());
            assertEquals("Check status of second step", BatchStatus.COMPLETED, stepMap.get("flow1step2").getBatchStatus());
            assertEquals("num steps", 2, stepMap.entrySet().size());

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

    public static class Batchlet extends AbstractBatchlet {
        @Override
        public String process() throws Exception {
            return "DONE";
        }
    }
}
