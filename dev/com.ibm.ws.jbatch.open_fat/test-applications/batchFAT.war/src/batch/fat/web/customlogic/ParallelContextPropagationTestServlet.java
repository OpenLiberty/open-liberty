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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
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

@WebServlet(name = "ParallelPropagation", urlPatterns = { "/ParallelPropagation" })
public class ParallelContextPropagationTestServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = -189207824014358889L;
    private final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        logger.info("In batch test servlet, called with URL: " + req.getRequestURL() + "?" + req.getQueryString());
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        String jslName = req.getParameter("jslName");
        String testName = req.getParameter("testName");
        executeJob(pw, jslName, testName);
    }

    private void executeJob(PrintWriter pw, String jslName, String testName) {

        //
        // 1. Start job
        //
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long execID = jobOperator.start(jslName, null);

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

        if (testName.equals("testPartitionJobExecId")) {

            String status = je.getExitStatus();

            //parse out IDs from exit strings
            String[] statusIDs = status.split(":");
            String[] jobIDs = new String[statusIDs.length - 1];
            for (int i = 1; i < statusIDs.length; i++) {//before the first ":" is unimportant
                jobIDs[i - 1] = statusIDs[i].substring(statusIDs[i].indexOf("J") + 1, statusIDs[i].indexOf("I"));
            }

            int counter = 0;
            for (String j : jobIDs) {
                counter++;
                assertEquals("For job execution id #" + counter, je.getExecutionId(), Long.parseLong(j));
            }

        } else if (testName.equals("testPartitionJobInstanceId")) {

            JobInstance ji = jobOp.getJobInstance(execId);
            long instance1Id = ji.getInstanceId();

            String status = je.getExitStatus();
            //parse out IDs from exit strings
            String[] statusIDs = status.split(":");
            String[] jobIDs = new String[statusIDs.length - 1];
            for (int i = 1; i < statusIDs.length; i++) {//before the first ":" is unimportant
                jobIDs[i - 1] = statusIDs[i].substring(statusIDs[i].indexOf("I") + 1, statusIDs[i].indexOf("S"));
            }

            int counter = 0;
            for (String j : jobIDs) {
                counter++;
                if (instance1Id != Long.parseLong(j)) {
                    throw new TestFailureException("For job instance id # " + counter + " expected : " + instance1Id + " but found: " + j);
                }
            }
        } else if (testName.equals("testPartitionStepExecId")) {

            List<StepExecution> se = jobOp.getStepExecutions(je.getExecutionId());
            if (se.size() != 1) {
                throw new TestFailureException("Only expected 1 StepExecution but found: " + se.size());
            }

            long stepExecId = se.get(0).getStepExecutionId();

            //parse out IDs from exit strings
            String status = je.getExitStatus();
            String[] statusIDs = status.split(":");
            String[] stepIDs = new String[statusIDs.length - 1];
            for (int i = 1; i < statusIDs.length; i++) {//before the first ":" is unimportant
                stepIDs[i - 1] = statusIDs[i].substring(statusIDs[i].indexOf("S") + 1);
            }

            for (String stepId : stepIDs) {
                if (stepExecId != Long.parseLong(stepId)) {
                    throw new TestFailureException("Expected stepId = " + stepExecId + " but found: " + stepId);
                }
            }

        } else if (testName.equals("testSplitFlowJobExecId")) {

            List<StepExecution> stepExecutions = jobOp.getStepExecutions(execId);

            for (StepExecution se : stepExecutions) {
                String status = se.getExitStatus();
                String[] tokens = status.split(":");
                String execIdStr = tokens[1].substring(tokens[1].indexOf("J") + 1, tokens[1].indexOf("I"));

                if (execId != Long.parseLong(execIdStr)) {
                    throw new TestFailureException("Expected job exec id = " + execId + " but found: " + execIdStr);
                }
            }
        } else if (testName.equals("testSplitFlowJobInstanceId")) {

            JobInstance ji = jobOp.getJobInstance(execId);
            long expectedInstanceId = ji.getInstanceId();

            List<StepExecution> stepExecutions = jobOp.getStepExecutions(execId);

            for (StepExecution se : stepExecutions) {
                String status = se.getExitStatus();
                String[] tokens = status.split(":");
                String instanceId = tokens[1].substring(tokens[1].indexOf("I") + 1, tokens[1].indexOf("S"));

                if (expectedInstanceId != Long.parseLong(instanceId)) {
                    throw new TestFailureException("Expected job instance id = " + expectedInstanceId + " but found: " + instanceId);
                }
            }
        } else if (testName.equals("testSplitFlowStepExecId")) {

            List<StepExecution> stepExecutions = jobOp.getStepExecutions(execId);

            for (StepExecution se : stepExecutions) {
                String status = se.getExitStatus();
                String[] tokens = status.split(":");
                String stepId = tokens[1].substring(tokens[1].indexOf("S") + 1);

                if (se.getStepExecutionId() != Long.parseLong(stepId)) {
                    throw new TestFailureException("Expected step execution id = " + se.getStepExecutionId() + " but found: " + stepId);
                }
            }
        } else if (testName.equals("testCollectorPropertyResolver")) {

            if (!je.getBatchStatus().toString().equals(BatchStatus.COMPLETED.toString())) {
                throw new TestFailureException("Expected " + BatchStatus.COMPLETED.toString() + ", but found " + je.getBatchStatus().toString());
            }

            List<StepExecution> stepExecutions = jobOp.getStepExecutions(execId);

            String data = ((String) stepExecutions.get(0).getPersistentUserData()).substring(4); // removes the null from the beginning of the string

            String value = "stepPropValuestepPropValuestepPropValuestepPropValuestepPropValue";

            if (!data.equals(value)) {
                throw new TestFailureException("Found: " + data + ", but was execting: " + value);
            }

        } else {
            throw new IllegalArgumentException("Not expecting testName = " + testName);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        pw.print("use GET method");
        resp.setStatus(200);
    }

}
