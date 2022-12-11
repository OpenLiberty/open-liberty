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

import static batch.fat.common.util.JobWaiter.FAILED_STATE_ONLY;
import static batch.fat.common.util.JobWaiter.STARTED_OR_STARTING;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.Metric.MetricType;
import javax.batch.runtime.StepExecution;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import batch.fat.artifacts.EndOfJobNotificationListener;
import batch.fat.common.util.JobWaiter;
import batch.fat.util.BatchFATHelper;
import batch.fat.util.StringUtils;

@WebServlet(name = "BatchTransactionalMisc", urlPatterns = { "/BatchTransactionalMisc" })
public class BatchTransactionalMiscServlet extends HttpServlet {

    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

    public class TestHelper {

        String testName;
        PrintWriter pw;

        public TestHelper(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            testName = req.getParameter("testName");
            pw = resp.getWriter();
        }

        private void execute() {

            // 1. Start job
            Properties jobParams = new Properties();
            JobWaiter waiter = null;
            long expectedReadMetric = 0;
            long expectedWriteMetric = 0;
            long expectedRollbackMetric = 0;
            int expectedUpdateCount = 0;
            String expectedExitStatus = null;

            // Common props
            jobParams.setProperty("dsjndi", BatchFATHelper.DFLT_PERSISTENCE_JNDI);
            jobParams.setProperty("writeTable", BatchFATHelper.APP_OUT1);

            String[] readDataArr = { "AAA", "BB", "CC", "DDDD", "EX", "FF", "GGGGGG", "HHH", "IIIII", "J", "KKK", "L", "MMMMM", "NOO", "OPOPSOS" };
            String readDataParm = StringUtils.join(Arrays.asList(readDataArr), ",");
            jobParams.setProperty("readData", readDataParm);

            /*
             * Note the default values for throwOn is in the JSL itself.
             */
            if (testName.equals("testRetryableExceptionWithRollbackButExceptionOnReaderClose")) {

                // Complete first chunk, throw retry-rollback exc, but blow up on reader close
                jobParams.setProperty("item-count", "5");
                jobParams.setProperty("throwExcOnReaderClose", "true");
                jobParams.setProperty("throwOn", "7");
                waiter = new JobWaiter(STARTED_OR_STARTING, FAILED_STATE_ONLY);
                expectedReadMetric = 5;
                expectedWriteMetric = 5;
                expectedRollbackMetric = 1;
                expectedUpdateCount = 5;
                expectedExitStatus = "5";

            } else if (testName.equals("testRetryableExceptionWithRollbackSuccessfulCompletion")) {

                // Complete first chunk, throw retry-rollback exc, successfully process chunk 1-item-at-a-time,
                // then process the last chunk as a whole
                jobParams.setProperty("item-count", "5");
                jobParams.setProperty("throwOn", "7");
                waiter = new JobWaiter();
                expectedReadMetric = 15;
                expectedWriteMetric = 15;
                expectedRollbackMetric = 1;
                expectedUpdateCount = readDataArr.length;
                expectedExitStatus = "5,1,1,5,3";

            } else if (testName.equals("testUncaughtExceptionThenExceptionOnReaderClose")) {

                // Complete first chunk, throw retry-rollback exc but with a retry-limit of '0', 
                // then blow up on reader close.
                jobParams.setProperty("item-count", "5");
                jobParams.setProperty("retry-limit", "0");
                jobParams.setProperty("throwExcOnReaderClose", "true");
                jobParams.setProperty("throwOn", "7");
                waiter = new JobWaiter(STARTED_OR_STARTING, FAILED_STATE_ONLY);
                expectedReadMetric = 5;
                expectedWriteMetric = 5;
                expectedRollbackMetric = 1;
                expectedUpdateCount = 5;
                expectedExitStatus = "5";

            } else if (testName.equals("testRetryableExceptionWithRollbackThenMultipleChunksSuccessfulCompletion")) {

                // Complete first two chunks, throw retry-rollback exc, successfully process chunk 1-item-at-a-time.
                jobParams.setProperty("item-count", "5");
                jobParams.setProperty("throwOn", "12");
                waiter = new JobWaiter();
                expectedReadMetric = 15;
                expectedWriteMetric = 15;
                expectedRollbackMetric = 1;
                expectedUpdateCount = readDataArr.length;
                expectedExitStatus = "5,5,1,1,3";
            }

            long execID = jobOperator.start(jslName, jobParams);

            boolean failTest = false;

            try {
                JobExecution jobExec = waiter.waitForAfterJobNotificationThenFinalState(EndOfJobNotificationListener.class, execID);
            } catch (Exception e) {
                failTest = true;
                pw.println("ERROR: " + e.getMessage());
            }

            ////
            // 
            // 3.  Now that it's done verify that it failed at the right point.
            //
            // A bit convoluted I know... I'm just trying to see and log the update count even in the cases
            // where the test fails, to help in debugging.
            // 
            // The count is interesting to confirm the rollback happened as expected.
            ////
            try {
                int cnt = getUpdateCount(execID);
                logger.info("Update count in BatchTransactionalMiscServlet = " + cnt);

                if (cnt != expectedUpdateCount) {
                    failTest = true;
                    pw.println("ERROR: Test failed. Expecting update count of " + expectedUpdateCount + ", but found: " + cnt);
                }
            } catch (Exception e) {
                failTest = true;
                pw.println("ERROR: " + e.getMessage());
            }

            // 4. Check step read metric
            try {
                long readMetric = getMetric(execID, MetricType.READ_COUNT);
                if (expectedReadMetric != readMetric) {
                    failTest = true;
                    pw.println("ERROR: Test failed. Expecting read metric of " + expectedReadMetric + ", but found: " + readMetric);
                }
            } catch (Exception e) {
                failTest = true;
                pw.println("ERROR: " + e.getMessage());
            }

            // 5. Check step write metric
            try {
                long writeMetric = getMetric(execID, MetricType.WRITE_COUNT);
                if (expectedWriteMetric != writeMetric) {
                    failTest = true;
                    pw.println("ERROR: Test failed. Expecting write metric of " + expectedWriteMetric + ", but found: " + writeMetric);
                }
            } catch (Exception e) {
                failTest = true;
                pw.println("ERROR: " + e.getMessage());
            }

            // 6. Check step rollback metric
            try {
                long rollbackMetric = getMetric(execID, MetricType.ROLLBACK_COUNT);
                if (expectedRollbackMetric != rollbackMetric) {
                    failTest = true;
                    pw.println("ERROR: Test failed. Expecting rollback metric of " + expectedRollbackMetric + ", but found: " + rollbackMetric);
                }
            } catch (Exception e) {
                failTest = true;
                pw.println("ERROR: " + e.getMessage());
            }

            // 7. Check step exit status
            try {
                String stepExitStatus = getExitStatus(execID);
                if (!expectedExitStatus.equals(stepExitStatus)) {
                    failTest = true;
                    pw.println("ERROR: Test failed. Expecting exit status of " + expectedExitStatus + ", but found: " + stepExitStatus);
                }
            } catch (Exception e) {
                failTest = true;
                pw.println("ERROR: " + e.getMessage());
            }

            // 6. Declare success
            if (!failTest) {
                pw.println(BatchFATHelper.SUCCESS_MESSAGE);
            }
        }
    }

    private final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");
    protected final static String jslName = "BatchTransactionalMisc";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        logger.info("In batch test servlet, called with URL: " + req.getRequestURL() + "?" + req.getQueryString());
        resp.setContentType("text/plain");
        TestHelper helper = new TestHelper(req, resp);
        helper.execute();
    }

    /**
     * @param job execID
     * @return step exit status (for 1-step job like this, we know which one)
     */
    private String getExitStatus(long execID) {
        StepExecution se = jobOperator.getStepExecutions(execID).get(0);
        return se.getExitStatus();
    }

    /**
     * @param job execID
     * @return step exit status (for 1-step job like this, we know which one)
     */
    private long getMetric(long execID, MetricType type) {
        StepExecution se = jobOperator.getStepExecutions(execID).get(0);
        for (Metric m : se.getMetrics()) {
            if (m.getType().equals(type)) {
                return m.getValue();
            }
        }
        throw new IllegalStateException("Didn't find rollback metric");
    }

    private int getUpdateCount(long execId) throws Exception {
        DataSource ds = (DataSource) (new InitialContext().lookup(BatchFATHelper.DFLT_PERSISTENCE_JNDI));
        Connection conn = ds.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT COUNT(name) FROM " + BatchFATHelper.APP_OUT1 +
                                                     " WHERE name like '" +
                                                     Long.toString(execId) + "%'");

        ResultSet rs = ps.executeQuery();
        rs.next();
        int cnt = rs.getInt(1);

        rs.close();
        ps.close();
        conn.close();

        return cnt;
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
