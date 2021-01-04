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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
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
@WebServlet(urlPatterns = { "/NullPropOnJobExecutionServlet/*" })
public class NullPropOnJobExecutionServlet extends HttpServlet {

    private final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

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

        String sleepPropName = "sleepTime";
        String sleepPropVal = "100";

        props.setProperty(sleepPropName, sleepPropVal);

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
        if (testName.equals("testNullPropOnJobExecution")) {
            JobInstance ji = jobOp.getJobInstance(execId);
            JobExecution jex = jobOp.getJobExecutions(ji).get(0);

            if (!jex.getBatchStatus().toString().equals(BatchStatus.COMPLETED.toString())) {
                throw new TestFailureException("Expected " + BatchStatus.COMPLETED.toString() + ", but found " + jex.getBatchStatus().toString());
            }

            Properties storedProps = jex.getJobParameters();

            if (storedProps.size() > 1) {
                throw new TestFailureException("Unexpected number of properties returned, expected 1 but found " + storedProps.size());
            }

            if (!storedProps.getProperty("sleepTime").equals("100")) {
                throw new TestFailureException("Unexpected property value returned, expected " + "100" + ", but found " + storedProps.getProperty("sleepTime"));
            }

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

}
