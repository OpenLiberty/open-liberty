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
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.listener.AbstractChunkListener;
import javax.batch.api.listener.AbstractStepListener;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.Metric.MetricType;
import javax.batch.runtime.StepExecution;
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
import batch.fat.util.PartitionSetChecker;

@WebServlet(name = "PartitionMetrics", urlPatterns = { "/PartitionMetrics" })
public class PartitionMetricsServlet extends HttpServlet {

    private final static Logger logger = Logger.getLogger("test");

    private static JobOperator jobOp = BatchRuntime.getJobOperator();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("In batch test servlet, called with URL: " + request.getRequestURL() + "?" + request.getQueryString());
        response.setContentType("text/plain");
        PrintWriter pw = response.getWriter();
        String testName = request.getParameter("testName");
        executeJob(pw, testName);
    }

    private void executeJob(PrintWriter pw, String testName) {
        Properties origParams = new Properties();
        JobWaiter waiter = null;

        //Default jsl, will change below if another is needed
        String jslName = "partitionMetrics";

        if (testName.equals("testMetricsRerunStep")) {
            origParams.setProperty("step1Size", "10");
            origParams.setProperty("step2Size", "5");
            origParams.setProperty("stepListener.forceFailure", "true");
            waiter = new JobWaiter(JobWaiter.FAILED_STATE_ONLY);
        } else if (testName.equals("testPartitionMetrics")) {
            origParams.setProperty("step1Size", "15");
            origParams.setProperty("step2Size", "20");
            waiter = new JobWaiter();
        } else if (testName.equals("testNestedSplitFlowPartitionMetrics")) {
            origParams.setProperty("step1Size", "15");
            origParams.setProperty("step2Size", "20");
            waiter = new JobWaiter();
            jslName = "partitionSplitFlowMetrics";
        } else if (testName.equals("testPartitionedRollbackMetric")) {
            origParams.setProperty("step1Size", "15");
            origParams.setProperty("step2Size", "20");
            origParams.setProperty("chunkListener.forceFailure", "true");
            waiter = new JobWaiter(JobWaiter.STARTED_OR_STARTING, JobWaiter.COMPLETED_OR_FAILED_STATES);
        }

        long execID = jobOp.start(jslName, origParams);

        try {
            // All tests expect COMPLETED status
            JobExecution jobExec = waiter.waitForAfterJobNotificationThenFinalState(EndOfJobNotificationListener.class, execID);

            if (testName.equals("testMetricsRerunStep")) {
                if (!jobExec.getBatchStatus().equals(BatchStatus.FAILED)) {
                    throw new TestFailureException("Didn't fail as expected, returned a Status of: " + jobExec.getBatchStatus());
                }

                // Now run again, since we failed on step 2 and allow-restart-if-complete = true, we'll get a different execution for
                // step 1.
                Properties restartParams = new Properties();
                restartParams.setProperty("step1Size", "25");
                restartParams.setProperty("step2Size", "6");
                restartParams.setProperty("stepListener.forceFailure", "false");

                long restartExecId = jobOp.restart(execID, restartParams);
                waiter = new JobWaiter();
                jobExec = waiter.waitForAfterJobNotificationThenFinalState(EndOfJobNotificationListener.class, restartExecId);

                StepExecution step1Exec1 = null;
                StepExecution step1Exec2 = null;

                for (StepExecution se : jobOp.getStepExecutions(execID)) {
                    if (se.getStepName().equals("step1")) {
                        step1Exec1 = se;
                    }
                }

                for (StepExecution se : jobOp.getStepExecutions(restartExecId)) {
                    if (se.getStepName().equals("step1")) {
                        step1Exec2 = se;
                    }
                }

                Metric[] metrics = step1Exec1.getMetrics();

                // 3 partitions of 10 elements - for each partition, 6 will be written and 4 will be filtered, this will be 2 chunks (item-count=5) + 1 zero-item chunk
                if (getMetricVal(metrics, Metric.MetricType.COMMIT_COUNT) != 9) {
                    throw new TestFailureException("commit count was not equal to 9");
                }
                if (getMetricVal(metrics, Metric.MetricType.FILTER_COUNT) != 12) {
                    throw new TestFailureException("filter count was not equal to 12");
                }
                if (getMetricVal(metrics, Metric.MetricType.READ_COUNT) != 30) {
                    throw new TestFailureException("read count was not equal to 30");
                }
                if (getMetricVal(metrics, Metric.MetricType.WRITE_COUNT) != 18) {
                    throw new TestFailureException("write count was not equal to 18");
                }

                Metric[] metrics2 = step1Exec2.getMetrics();

                // 3 partitions of 25 elements - for each partition, 15 will be written and 10 will be filtered, this will be 5 chunks (item-count=5) + 1 zero-item chunk
                if (getMetricVal(metrics2, Metric.MetricType.COMMIT_COUNT) != 18) {
                    throw new TestFailureException("commit count was not equal to 18");
                }
                if (getMetricVal(metrics2, Metric.MetricType.FILTER_COUNT) != 30) {
                    throw new TestFailureException("filter count was not equal to 30");
                }
                if (getMetricVal(metrics2, Metric.MetricType.READ_COUNT) != 75) {
                    throw new TestFailureException("read count was not equal to 75");
                }
                if (getMetricVal(metrics2, Metric.MetricType.WRITE_COUNT) != 45) {
                    throw new TestFailureException("write count was not equal to 45");
                }

            } else if (testName.equals("testPartitionMetrics")) {
                testPartitionedMetrics(jobExec, execID);
            } else if (testName.equals("testNestedSplitFlowPartitionMetrics")) {
                testPartitionedMetrics(jobExec, execID);
            } else if (testName.equals("testPartitionedRollbackMetric")) {
                testPartitionedRollbackMetric(jobExec, execID);
            } else {
                throw new IllegalArgumentException("Not expecting testName = " + testName);
            }

            // For simple "screen scraping" parsing
            pw.println(BatchFATHelper.SUCCESS_MESSAGE);

        } catch (Exception e) {
            // Key signifying error.
            pw.println("ERROR: " + e.getMessage());
        }
    }

    private void testPartitionedRollbackMetric(JobExecution jobExec, long execID) throws Exception {

        if (!jobExec.getBatchStatus().equals(BatchStatus.FAILED)) {
            throw new TestFailureException("Didn't fail as expected successfully, returned a Status of: " + jobExec.getBatchStatus());
        }

        StepExecution step1Exec = null;
        StepExecution step2Exec = null;

        for (StepExecution se : jobOp.getStepExecutions(execID)) {
            if (se.getStepName().equals("step1")) {
                step1Exec = se;
            } else if (se.getStepName().equals("step2")) {
                step2Exec = se;
            }
        }

        Metric[] metrics = step1Exec.getMetrics();
        // Temp to see what values are in this array
        for (Metric m : metrics) {
            logger.fine("All Metric values: " + m.getType() + " = " + m.getValue());
        }

        // 3 partitions - this confirms that the read, filter, write counts get rolled back
        if (getMetricVal(metrics, Metric.MetricType.COMMIT_COUNT) != 0) {
            logger.fine(Metric.MetricType.COMMIT_COUNT + " was not equal to 0");
            throw new TestFailureException("commit count was not equal to 0");
        }
        if (getMetricVal(metrics, Metric.MetricType.FILTER_COUNT) != 0) {
            logger.fine(Metric.MetricType.FILTER_COUNT + " was not equal to 0");
            throw new TestFailureException("filter count was not equal to 0");
        }
        if (getMetricVal(metrics, Metric.MetricType.READ_COUNT) != 0) {
            logger.fine(Metric.MetricType.READ_COUNT + " was not equal to 0");
            throw new TestFailureException("read count was not equal to 0");
        }
        if (getMetricVal(metrics, Metric.MetricType.WRITE_COUNT) != 0) {
            logger.fine(Metric.MetricType.WRITE_COUNT + " was not equal to 0");
            throw new TestFailureException("write count was not equal to 0");
        }
        if (getMetricVal(metrics, Metric.MetricType.ROLLBACK_COUNT) != 3) {
            logger.fine(Metric.MetricType.ROLLBACK_COUNT + " was not equal to 3");
            throw new TestFailureException("rollback count was not equal to 3");
        }
    }

    private void testPartitionedMetrics(JobExecution jobExec, long execID) throws Exception {

        if (!jobExec.getBatchStatus().equals(BatchStatus.COMPLETED)) {
            throw new TestFailureException("Didn't complete successfully, returned a Status of: " + jobExec.getBatchStatus());
        }

        StepExecution step1Exec = null;
        StepExecution step2Exec = null;
        for (StepExecution se : jobOp.getStepExecutions(execID)) {
            if (se.getStepName().equals("step1")) {
                step1Exec = se;
            } else if (se.getStepName().equals("step2")) {
                step2Exec = se;
            }
        }
        Metric[] metrics = step1Exec.getMetrics();

        // 3 partitions of 15 elements - for each partition, 9 will be written and 6 will be filtered,
        // this will be 3 chunks (item-count=5) + 1 zero-item chunk
        if (getMetricVal(metrics, Metric.MetricType.COMMIT_COUNT) != 12) {
            throw new TestFailureException("commit count was not equal to 12");
        }
        if (getMetricVal(metrics, Metric.MetricType.FILTER_COUNT) != 18) {
            throw new TestFailureException("filter count was not equal to 18");
        }
        if (getMetricVal(metrics, Metric.MetricType.READ_COUNT) != 45) {
            throw new TestFailureException("read count was not equal to 45");
        }
        if (getMetricVal(metrics, Metric.MetricType.WRITE_COUNT) != 27) {
            throw new TestFailureException("write count was not equal to 27");
        }

        Metric[] metrics2 = step2Exec.getMetrics();

        // 3 partitions of 20 elements - for each partition, 12 will be written and 8 will be filtered,
        // this will be 4 chunks (item-count=5) + 1 zero-item chunk
        if (getMetricVal(metrics2, Metric.MetricType.COMMIT_COUNT) != 15) {
            throw new TestFailureException("commit count was not equal to 15");
        }
        if (getMetricVal(metrics2, Metric.MetricType.FILTER_COUNT) != 24) {
            throw new TestFailureException("filter count was not equal to 24");
        }
        if (getMetricVal(metrics2, Metric.MetricType.READ_COUNT) != 60) {
            throw new TestFailureException("read count was not equal to 60");
        }
        if (getMetricVal(metrics2, Metric.MetricType.WRITE_COUNT) != 36) {
            throw new TestFailureException("write count was not equal to 36");
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        PrintWriter pw = response.getWriter();
        pw.print("use GET method");
        response.setStatus(200);
    }

    private long getMetricVal(Metric[] metrics, MetricType type) {
        long retVal = 0L;
        for (Metric m : metrics) {
            if (m.getType().equals(type)) {
                logger.info("Metric values: " + m.getType() + " = " + m.getValue());
                retVal = m.getValue();
            }
        }
        return retVal;
    }

    public static class Reader extends AbstractItemReader {

        @BatchProperty
        String numToRead;

        int i = 0;

        @Override
        public Object readItem() {
            if (i++ <= Integer.parseInt(numToRead) - 1) {
                return i;
            } else {
                return null;
            }
        }
    }

    public static class Processor implements ItemProcessor {

        @Inject
        @BatchProperty
        String OK;

        @Override
        public Object processItem(Object item) {

            if (OK != null) {
                processorPropChecker.add(OK);
            }

            Integer i = (Integer) item;
            if (i % 5 == 1 || i % 5 == 3) {
                return null;
            } else {
                return i;
            }
        }
    }

    public static class Writer extends AbstractItemWriter {
        @Override
        public void writeItems(List<Object> items) {
            StringBuilder sb = new StringBuilder("Next chunk is: ");
            for (Object o : items) {
                sb.append(o.toString()).append(",");
            }
            String chunkStr = sb.toString();
            System.out.println(chunkStr.substring(0, chunkStr.length() - 1) + "\n");
        }
    }

    public static class ChunkListener extends AbstractChunkListener {

        @Inject
        @BatchProperty
        String forceFailure;

        @Inject
        StepContext stepCtx;

        @Override
        public void afterChunk() throws Exception {
            if (Boolean.parseBoolean(forceFailure) == true) {
                throw new RuntimeException("Forcing failure for step: " + stepCtx.getStepName());
            }
        }
    }

    public static class StepListener extends AbstractStepListener {

        @Inject
        @BatchProperty
        String forceFailure;

        @Inject
        StepContext stepCtx;

        @Override
        public void afterStep() throws Exception {
            if (Boolean.parseBoolean(forceFailure) == true) {
                throw new RuntimeException("Forcing failure for step: " + stepCtx.getStepName());
            }
        }
    }

    private static String[] expectedProcessorPropertyVals = { "OK.0", "OK.1", "OK.2" };
    private static PartitionSetChecker processorPropChecker = new PartitionSetChecker(expectedProcessorPropertyVals);

    public static class ProcessorPropValueCheckerStepListener extends AbstractStepListener {

        @Override
        public void afterStep() throws Exception {
            processorPropChecker.assertExpected();
        }

    }

    public static class NoOpBatchlet extends AbstractBatchlet {
        @Override
        public String process() {
            return "true";
        }
    }

}
