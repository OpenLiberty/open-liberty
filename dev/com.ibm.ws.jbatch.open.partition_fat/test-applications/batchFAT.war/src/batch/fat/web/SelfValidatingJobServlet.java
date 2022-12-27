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
package batch.fat.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import batch.fat.artifacts.EndOfJobNotificationListener;
import batch.fat.common.util.JobWaiter;
import batch.fat.util.BatchFATHelper;

/**
 * As indicated by the class name, the idea here is that
 * the test logic can be encapsulated in the simple question of
 * "did the job complete successfully?".
 */
@WebServlet(name = "Basic", urlPatterns = { "/Basic" })
public class SelfValidatingJobServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = -189207824014358889L;

    protected final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");
    protected final static String jslParmsAttr = "jslParmsAttr";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {

        logger.info("In batch test servlet, called with URL: " + req.getRequestURL() + "?" + req.getQueryString());

        PrintWriter pw = resp.getWriter();

        try {
            resp.setContentType("text/plain");

            String jslName = req.getParameter("jslName");
            Properties jslParms = (Properties) req.getAttribute(jslParmsAttr);
            executeJobAndWaitToCompletion(pw, jslName, jslParms);
        } catch (Exception e) {
            logger.info("BatchServlet.doGet: Unexpected exception: " + e);
            e.printStackTrace(pw);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }

    private void executeJobAndWaitToCompletion(PrintWriter pw, String jslName, Properties jslParms) {

        //
        // 1. Start job
        // 
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long execID = jobOperator.start(jslName, jslParms);

        // 2. Wait for terminating batch status to COMPLETED
        JobWaiter waiter = new JobWaiter();
        try {
            JobExecution jobExec = waiter.waitForAfterJobNotificationThenFinalState(EndOfJobNotificationListener.class, execID);

            // 3. Now verify exit status
            String es = jobExec.getExitStatus();
            if ("COMPLETED".equals(es)) {
                pw.println(BatchFATHelper.SUCCESS_MESSAGE);
            } else {
                pw.println("ERROR: Expected exit status COMPLETED but got:" + es);
            }
        } catch (Exception e) {
            pw.println("ERROR: " + e.getMessage());
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