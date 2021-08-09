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

import static batch.fat.common.util.JobWaiter.COMPLETED_OR_FAILED_STATES;
import static batch.fat.common.util.JobWaiter.STARTED_OR_STARTING;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

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
import batch.fat.util.BatchFATHelper;

@WebServlet(name = "TranTimeout", urlPatterns = { "/TranTimeout" })
/*
 * Used to catch expected failures and check for rollback
 */
public class TranTimeoutServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = -189207824014358889L;
    private final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");
    protected final static String jslName = "ChunkTranTimeout";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        logger.info("In batch test servlet, called with URL: " + req.getRequestURL() + "?" + req.getQueryString());
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        Integer variation = Integer.parseInt(req.getParameter("variation"));
        executeJob(pw, variation);
    }

    private Properties getVariationParms(int variation) {
        String NO_DELAY = "0";
        String ONE_SECOND = "1";
        String FIVE_SECONDS = "5";
        String TWENTY_SECONDS = "20";
        String LONG_TIMEOUT = "180";
        Properties props = new Properties();
        if (variation == 1) {
            props.setProperty("step1.timeout", FIVE_SECONDS);
            props.setProperty("step1.delay", TWENTY_SECONDS); // FORCE TIMEOUT
            props.setProperty("step2.timeout", LONG_TIMEOUT);
            props.setProperty("step2.delay", NO_DELAY);
        } else if (variation == 2) {
            props.setProperty("step1.timeout", TWENTY_SECONDS);
            props.setProperty("step1.delay", ONE_SECOND);
            props.setProperty("step2.timeout", FIVE_SECONDS);
            props.setProperty("step2.delay", TWENTY_SECONDS); // FORCE TIMEOUT
        } else if (variation == 3) {
            // Don't set delay
            props.setProperty("step1.timeout", TWENTY_SECONDS);
            props.setProperty("step1.delay", NO_DELAY);
            props.setProperty("step2.timeout", TWENTY_SECONDS);
            props.setProperty("step2.delay", NO_DELAY);
        }
        return props;
    }

    private void executeJob(PrintWriter pw, Integer variation) {

        //
        // 1. Start job
        // 
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long execID = jobOperator.start(jslName, getVariationParms(variation));

        // 
        // 2. Use the waiter, configured to except FAILED (for expected timeout cases).
        // 
        JobWaiter waiter = new JobWaiter(STARTED_OR_STARTING, COMPLETED_OR_FAILED_STATES);
        try {
            JobExecution jobExec = waiter.waitForAfterJobNotificationThenFinalState(EndOfJobNotificationListener.class, execID);
            // 
            // 3.  Now that it's complete do further verification based on the specific variation.
            // 
            boolean pass = verifyResults(pw, jobExec, variation);
            if (pass) {
                pw.println(BatchFATHelper.SUCCESS_MESSAGE);
            } else {
                pw.println("ERROR: Test failed.");
            }
        } catch (Exception e) {
            pw.println("ERROR: " + e.getMessage());
        }
    }

    private boolean verifyResults(PrintWriter pw, JobExecution jobExec, int variation) {

        if (variation == 1) {
            // assert job status failed
            // assert step 1 status failed
            if (jobExec.getBatchStatus() != BatchStatus.FAILED) {
                pw.println("Expecting FAILED status, found: " + jobExec.getBatchStatus());
                return false;
            }
            List<StepExecution> stepExecs = BatchRuntime.getJobOperator().getStepExecutions(jobExec.getExecutionId());
            for (StepExecution se : stepExecs) {
                if (se.getStepName().equals("step1")) {
                    if (se.getBatchStatus() == BatchStatus.FAILED) {
                        pw.println("Test #1 completed successfully");
                        return true;
                    } else {
                        pw.println("Expecting FAILED status, found: " + se.getBatchStatus());
                        return false;
                    }
                }
            }
            pw.println("Didn't find step1 execution ");
            return false;
        } else if (variation == 2) {
            // assert job status failed
            // assert step 1 status completed
            // assert step 2 status failed
            if (jobExec.getBatchStatus() != BatchStatus.FAILED) {
                pw.println("Expecting FAILED status, found: " + jobExec.getBatchStatus());
                return false;
            }
            boolean foundStep1 = false;
            List<StepExecution> stepExecs = BatchRuntime.getJobOperator().getStepExecutions(jobExec.getExecutionId());
            for (StepExecution se : stepExecs) {
                if (se.getStepName().equals("step1")) {
                    if (se.getBatchStatus() == BatchStatus.COMPLETED) {
                        pw.println("Test #2, step1 completed successfully");
                        foundStep1 = true;
                        break;
                    } else {
                        pw.println("Expecting COMPLETED status for step1, found: " + se.getBatchStatus());
                        return false;
                    }
                }
            }
            if (!foundStep1) {
                pw.println("Didn't find step1 execution ");
                return false;
            }

            // Now look at step 2
            for (StepExecution se : stepExecs) {
                if (se.getStepName().equals("step2")) {
                    if (se.getBatchStatus() == BatchStatus.FAILED) {
                        pw.println("Test #2 completed successfully");
                        return true;
                    } else {
                        pw.println("Expecting FAILED status, found: " + se.getBatchStatus());
                        return false;
                    }
                }
            }
            pw.println("Didn't find step2 execution ");
            return false;

        } else if (variation == 3) {
            // assert job status completed

            if (jobExec.getBatchStatus() != BatchStatus.COMPLETED) {
                pw.println("Expecting completed status, found: " + jobExec.getBatchStatus());
                return false;
            } else {
                pw.println("Test #3 completed successfully");
                return true;
            }
        } else {
            throw new IllegalArgumentException("Expecting variation= 1, 2, 3 got : " + variation);
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
