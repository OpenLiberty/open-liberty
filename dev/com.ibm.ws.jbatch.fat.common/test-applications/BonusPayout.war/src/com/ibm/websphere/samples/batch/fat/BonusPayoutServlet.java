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
package com.ibm.websphere.samples.batch.fat;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import batch.fat.common.util.JobWaiter;

import com.ibm.websphere.samples.batch.fat.EndOfJobNotificationListener;
import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;
import com.ibm.websphere.samples.batch.util.BonusPayoutUtils;

@WebServlet(name = "BonusPayoutServlet", urlPatterns = { "/BonusPayoutServlet" })
public class BonusPayoutServlet extends HttpServlet implements BonusPayoutConstants {

    /**  */
    private static final long serialVersionUID = -189207824014358889L;

    private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        PrintWriter pw = resp.getWriter();
        try
        {
            logger.fine((new StringBuilder()).append("In BonusPayout, called with URL: ").append(req.getRequestURL()).append("?").append(req.getQueryString()).toString());
            resp.setContentType("text/plain");
            Properties props = getPropertiesFromServletParms(req);
            String jslName = extractJSLName(props);
            int numRestarts = extractNumRestarts(props);
            executeJobAndWaitToCompletion(pw, jslName, numRestarts, props);
            pw.println("TEST PASSED");
            resp.setStatus(200);
        } catch (Exception e)
        {
            logger.info((new StringBuilder()).append("BatchServlet.doGet: Unexpected exception: ").append(e).toString());
            pw.println((new StringBuilder()).append("ERROR: ").append(e.getMessage()).toString());
            e.printStackTrace(pw);
            resp.setStatus(500);
        }
    }

    /**
     * @param req
     * @return
     */
    private Properties getPropertiesFromServletParms(HttpServletRequest req) {
        Properties props = new Properties();
        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String[] paramValues = req.getParameterValues(paramName);
            if (paramValues.length > 1) {
                throw new IllegalArgumentException("Only support 1 instance of parameter name, but found more than one for parameter name = " + paramName);
            }
            props.setProperty(paramName, paramValues[0]);
        }
        return props;
    }

    private void executeJobAndWaitToCompletion(PrintWriter pw, String jslName, int numRestarts, Properties jobParameters) {

        JobOperator jobOperator = BatchRuntime.getJobOperator();

        //
        // Let's do the calculation upfront rather than pushing this into the app, even though that might be a nice alternative.
        //
        // Some of these properties are the same as the defaults in the JSL..but leaving nothing to chance.
        // 
        // For a non-partitioned job, there's no harm in setting "numValidationPartitions".
        // 
        boolean partitioned = Boolean.parseBoolean(jobParameters.getProperty("junit.partitioned", "false"));
        if (numRestarts == 1) {
            jobParameters.setProperty("numRecords", "1000");
            jobParameters.setProperty("chunkSize", "100");
            jobParameters.setProperty("numValidationPartitions", "5");
            jobParameters.setProperty("forceFailure", partitioned ? "102" : "502"); // for partitioned will get to 100 on 1st exec, then complete
        } else if (numRestarts == 2) {
            jobParameters.setProperty("numRecords", "1000");
            jobParameters.setProperty("chunkSize", "50");
            jobParameters.setProperty("numValidationPartitions", "4");
            jobParameters.setProperty("forceFailure", partitioned ? "102" : "450"); // for partitioned will get to 100 on 1st exec, 200 on 2nd exec, then complete
        }

        // No matter what, we begin with a start.  We could have removed all the properties starting with 'junit.*', 
        // since the intention is to distinguish between properties used to parameterize the test execution vs. the
        // job itself, but no need to rush to do this.
        long mostRecentExecutionID = jobOperator.start(jslName, jobParameters);

        if (numRestarts == 0) {
            try {
                logger.fine("In BonusPayout, execution is done, now waiting for final status");
                JobWaiter waiter = new JobWaiter();
                waiter.waitForAfterJobNotification(EndOfJobNotificationListener.class);
                waiter.pollForFinalState(mostRecentExecutionID);
                return;
            } catch (Exception e) {
            	BonusPayoutUtils.throwIllegalStateExc("Exception waiting for final status");
            }
        } else {
            // First execution will fail
            try {
                logger.fine("In BonusPayout, initial execution is done, now waiting for final status");
                JobWaiter waiter = new JobWaiter(JobWaiter.STARTED_OR_STARTING, JobWaiter.FAILED_STATE_ONLY);
                waiter.waitForAfterJobNotification(EndOfJobNotificationListener.class);
                waiter.pollForFinalState(mostRecentExecutionID);
            } catch (Exception e) {
            	BonusPayoutUtils.throwIllegalStateExc("Exception waiting for final status");
            }
            // For each restart
            for (int restartNum = 1; restartNum <= numRestarts; restartNum++) {
                try {
                    mostRecentExecutionID = jobOperator.restart(mostRecentExecutionID, jobParameters);
                    logger.fine((new StringBuilder()).append("In BonusPayout, restart execution # ").append(restartNum).append(" is done, now waiting for final status").toString());
                    JobWaiter waiter = null;
                    // All restarts except the last will end in failure.  
                    if (restartNum < numRestarts) {
                        waiter = new JobWaiter(JobWaiter.STARTED_OR_STARTING, JobWaiter.FAILED_STATE_ONLY);
                        // The last will complete successfully
                    } else {
                        waiter = new JobWaiter();
                    }
                    waiter.waitForAfterJobNotification(EndOfJobNotificationListener.class);
                    waiter.pollForFinalState(mostRecentExecutionID);
                } catch (Exception e) {
                	BonusPayoutUtils.throwIllegalStateExc("Exception waiting for final status");
                }
            }
        }
    }

    /**
     * @param props
     */
    private String extractJSLName(Properties props) {
        boolean partitioned = Boolean.parseBoolean(props.getProperty("junit.partitioned", "false"));
        boolean cursorhold = Boolean.parseBoolean(props.getProperty("junit.cursorhold", "false"));

        if (partitioned && cursorhold) {
            return "BonusPayoutJob.partitioned.cursorhold";
        } else if (!partitioned && cursorhold) {
            return "BonusPayoutJob.cursorhold";
        } else if (partitioned && !cursorhold) {
            return "BonusPayoutJob.partitioned";
        } else { // if (!partitioned && !cursorhold) {
            return "BonusPayoutJob";
        }
    }

    private int extractNumRestarts(Properties props) {
        int retVal = Integer.parseInt(props.getProperty("junit.numRestarts", "0"));
        return retVal;
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