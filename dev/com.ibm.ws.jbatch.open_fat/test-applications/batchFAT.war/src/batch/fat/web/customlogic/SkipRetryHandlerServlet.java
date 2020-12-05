/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.listener.RetryReadListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.listener.AbstractStepListener;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.Metric.MetricType;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import batch.fat.artifacts.EndOfJobNotificationListener;
import batch.fat.common.MyChildException;
import batch.fat.common.MyGrandchildException;
import batch.fat.common.MyParentException;
import batch.fat.common.util.JobWaiter;
import batch.fat.util.BatchFATHelper;

/**
 *
 */
@WebServlet(name = "SkipRetryHandler", urlPatterns = { "/SkipRetryHandler/*" })
public class SkipRetryHandlerServlet extends HttpServlet {

    private final static Logger logger = Logger.getLogger("test");
    private static JobOperator jobOp = BatchRuntime.getJobOperator();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.info("In batch test servlet, called with URL: " + request.getRequestURL() + "?" + request.getQueryString());
        response.setContentType("text/plain");
        PrintWriter pw = response.getWriter();
        String testName = request.getParameter("testName");
        String jslName = request.getParameter("jslName");
        executeJob(pw, testName, jslName);
    }

    private void executeJob(PrintWriter pw, String testName, String jslName) {
        // 1. Start job
        Properties props = new Properties();
        JobWaiter waiter = null;

        //used to differentiate between the tests to ensure a proper test name and execution number
        boolean containsSkip = testName.toLowerCase().contains("testskip");
        boolean containsRetry = testName.toLowerCase().contains("testretry");
        boolean containsRollback = testName.toLowerCase().contains("rollback");
        String executionNumber = null;

        //all jobs are going to have the same array size for easy manipulation
        props.put("app.arraysize", "30");

        //set up the properties for the tests that should pass
        if (testName.toLowerCase().contains("pass")) {
            if (containsSkip)
                executionNumber = "1";
            else if (containsRetry)
                executionNumber = containsRollback ? "7" : "4";
            props.put("execution.number", executionNumber);
            props.put("readrecord.fail", "4,13");
            waiter = new JobWaiter(JobWaiter.COMPLETED_STATE_ONLY);
        }
        //set up the properties for the tests that should fail due to hitting the retry/skip limits 
        //There is 1 exception from the process, 4 from read, and 1 from write which is 6 total to exceed the limit 
        else if (testName.toLowerCase().contains("toomanyexceptions")) {
            if (containsSkip)
                executionNumber = "2";
            else if (containsRetry)
                executionNumber = containsRollback ? "8" : "5";
            props.put("execution.number", executionNumber);
            props.put("readrecord.fail", "6,12,18,23,28");
            waiter = new JobWaiter(JobWaiter.FAILED_STATE_ONLY);
        }
        //set up the properties for the tests that should fail due to an excluded exception being hit 
        else if (testName.toLowerCase().contains("excludedexception")) {
            if (containsSkip)
                executionNumber = "3";
            else if (containsRetry)
                executionNumber = containsRollback ? "9" : "6";
            props.put("execution.number", executionNumber);
            props.put("readrecord.fail", "5,9,20");
            waiter = new JobWaiter(JobWaiter.FAILED_STATE_ONLY);
        }
        logger.info("JobXML: " + jslName + " for Test: " + testName);
        long execID = jobOp.start(jslName, props);

        // 2. Wait for completion
        try {
            //Some test are expected back in Failed state and others in Completed
            JobExecution jobExec = waiter.waitForAfterJobNotificationThenFinalState(EndOfJobNotificationListener.class, execID);

            // 3. Now that all tests complete do further verification based on the specific variation.
            if (containsSkip || containsRetry) {
                testSkipRetryHandler(jobExec, execID, executionNumber, testName);
            } else {
                throw new IllegalArgumentException("Not expecting testName = " + testName);
            }

            // 4a. For simple "screen scraping" parsing
            pw.println(BatchFATHelper.SUCCESS_MESSAGE);

        } catch (Exception e) {
            // 4b. Key signifying error.
            pw.println("ERROR: " + e.getMessage());
        }
    }

    /**
     * @param jobExec
     * @param execID
     * @param testName
     */
    private void testSkipRetryHandler(JobExecution jobExec, long execID, String execNum, String testName) throws Exception {

        StepExecution step1Exec1 = null;

        for (StepExecution se : jobOp.getStepExecutions(execID)) {
            if (se.getStepName().equals("step1")) {
                step1Exec1 = se;
            }
        }

        //Get the job metrics
        Metric[] metrics = step1Exec1.getMetrics();
        //Get the total skip count for the job (reader + writer + processor = total)
        long skipCount = getMetricVal(metrics, Metric.MetricType.READ_SKIP_COUNT) + getMetricVal(metrics, Metric.MetricType.WRITE_SKIP_COUNT)
                         + getMetricVal(metrics, Metric.MetricType.PROCESS_SKIP_COUNT);
        //Get the rollback count for the job (A failing job always performs an additional rollback at the very end)
        long rollbackCount = getMetricVal(metrics, Metric.MetricType.ROLLBACK_COUNT);

        //Verify jobs have the proper skip count (only jobs 1-3 will perform skips) 
        //and rollback count (all failing jobs will have at least one)
        if (execNum.equals("1")) {
            assertEquals(4, skipCount);
            assertEquals(0, rollbackCount);
        } else if (execNum.equals("2")) {
            assertEquals(6, skipCount);
            assertEquals(1, rollbackCount);
        } else if (execNum.equals("3")) {
            assertEquals(4, skipCount);
            assertEquals(1, rollbackCount);
        } else if (execNum.equals("4")) {
            assertEquals(0, skipCount);
            assertEquals(0, rollbackCount);
        } else if (execNum.equals("5")) {
            assertEquals(0, skipCount);
            assertEquals(1, rollbackCount);
        } else if (execNum.equals("6")) {
            assertEquals(0, skipCount);
            assertEquals(1, rollbackCount);
        } else if (execNum.equals("7")) {
            assertEquals(0, skipCount);
            assertEquals(4, rollbackCount);
        } else if (execNum.equals("8")) {
            assertEquals(0, skipCount);
            assertEquals(7, rollbackCount);
        } else if (execNum.equals("9")) {
            assertEquals(0, skipCount);
            assertEquals(5, rollbackCount);
        }
        //check that tests that should pass do and tests that should fail due
        if (testName.toLowerCase().contains("pass")) {
            assertEquals(BatchStatus.COMPLETED.toString(), jobExec.getBatchStatus().toString());
        } else if (testName.toLowerCase().contains("fail")) {
            assertEquals(BatchStatus.FAILED.toString(), jobExec.getBatchStatus().toString());
        }
        //if a test does not contain the keyword pass or fail then it is not a proper test name for this FAT
        else {
            throw new IllegalArgumentException("Not expecting testName = " + testName);
        }
        //makes sure that the skip/retry read listeners recorded an instance of the proper exception (MyParentException)
        assertTrue(MyRetryReadListener.GOOD_EXIT_STATUS.toString().equals(jobExec.getExitStatus().toString())
                   || MySkipReadListener.GOOD_EXIT_STATUS.toString().equals(jobExec.getExitStatus().toString()));
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

    @javax.inject.Named("readRecord")
    public static class ReadRecord {

        private int count = 0;

        public ReadRecord() {

        }

        public ReadRecord(int in) {
            count = in;
        }

        public int getCount() {
            return count;
        }

        public void setRecord(int i) {
            count = i;
        }
    }

    @javax.inject.Named("processor")
    public static class Processor implements ItemProcessor {

        private int update = 10;
        int processIteration = 0;

        @Inject
        StepContext stepCtx = null;

        @Override
        public ReadRecord processItem(Object record) throws Exception {

            //Throw an included exception to ensure the processor path is tested properly
            if (processIteration == 0) {
                processIteration++;
                //set dirty flag here in processor
                stepCtx.setTransientUserData(Boolean.FALSE);
                throw new MyGrandchildException("fail on purpose with MyGrandchildException in process");
            }
            ReadRecord processedRecord = null;
            processedRecord = (ReadRecord) record;
            processedRecord.setRecord(update);
            update = update + 1;
            processIteration++;
            return processedRecord;
        }
    }

    @javax.inject.Named("reader")
    public static class Reader extends AbstractItemReader {

        private final static Logger logger = Logger.getLogger(Reader.class.getName());

        private int exceptionCount = 0;
        private int idx;
        private int execnum;
        private int arraysize;
        private int checkpointValue = 0;

        //The read iterations to fail on
        @Inject
        @BatchProperty(name = "readrecord.fail")
        String readrecordfailNumberString = null;

        @Inject
        @BatchProperty(name = "execution.number")
        String executionNumberString;

        @Inject
        @BatchProperty(name = "app.arraysize")
        String appArraySizeString;

        @Inject
        StepContext stepCtx = null;

        Set<Integer> failnum = new HashSet<Integer>();

        public Reader() {

        }

        //The checkpoint value is not used
        @Override
        public void open(Serializable chkptValue) throws Exception {

            if (!(readrecordfailNumberString == null)) {
                String[] readFailPointsStrArr = readrecordfailNumberString.split(",");
                for (int i = 0; i < readFailPointsStrArr.length; i++) {
                    failnum.add(Integer.parseInt(readFailPointsStrArr[i]));
                }
            } else {
                failnum.add(-1);
            }

            execnum = Integer.parseInt(executionNumberString);
            arraysize = Integer.parseInt(appArraySizeString);

            //if checkpoint value is 0 it means it has not been initialized yet so the value passed into open will be null
            if (checkpointValue == 0) {
                assertEquals(null, chkptValue);
            }
            //otherwise the local checkpoint value should patch the argument passed into open
            else {
                assertEquals(checkpointValue, chkptValue);
            }

            //The first time through will always be the initial open which will have a null flag.
            //On subsequent passes through assert the flag is dirty to show a retry with rollbacks occurred
            if (execnum == 7 || execnum == 8 || execnum == 9) {
                //verify the flag was dirty and then unset it here to ensure rollback happened
                if (stepCtx.getTransientUserData() != null)
                    assertTrue(stepCtx.getTransientUserData() == Boolean.FALSE);
                stepCtx.setTransientUserData(Boolean.TRUE);
            }

        }

        //used to verify that a rollback was processed
        @Override
        public Serializable checkpointInfo() {
            checkpointValue++;
            return checkpointValue;
        }

        @Override
        public ReadRecord readItem() throws Exception {

            int i = idx;

            if (i == arraysize) {
                return null;
            }

            //if the index is in the failnum integer array then throw an exception based on 
            //execution number and total read exceptions thrown
            if (isFailnum(idx)) {
                logger.fine("READ: got the fail num..." + failnum.toString());
                if (execnum == 7 || execnum == 8 || execnum == 9) {
                    //Ensure the flag is clean and make it dirty here to ensure a rollback completed before returning
                    assertTrue(stepCtx.getTransientUserData() == Boolean.TRUE);
                    stepCtx.setTransientUserData(Boolean.FALSE);
                }
                idx++;
                exceptionCount++;
                if (exceptionCount == 1) {
                    throw new MyParentException("fail on purpose with MyParentException in read");
                } else if (exceptionCount == 2 || execnum == 2 || execnum == 5 || execnum == 8) {
                    //Don't want to throw MyChildException when testing for hitting the skip/retry limit since it's excluded
                    throw new MyGrandchildException("fail on purpose with MyGrandchildException in read");
                }
                else {
                    throw new MyChildException("fail on purpose with MyChildException in read");
                }
            }
            idx++;;
            return new ReadRecord(i);
        }

        private boolean isFailnum(int idxIn) {

            boolean ans = false;
            for (int i = 0; i < failnum.size(); i++) {
                if (failnum.contains(idxIn)) {
                    ans = true;
                }
            }
            return ans;
        }
    }

    @javax.inject.Named("writer")
    public static class Writer extends AbstractItemWriter {

        private final static Logger logger = Logger.getLogger(Writer.class.getName());
        int writerIteration = 0;

        @Inject
        StepContext stepCtx = null;

        @Override
        public void writeItems(List<Object> myData) throws Exception {

            //Throw an included error after to ensure the write path is tested properly
            if (writerIteration == 0) {
                writerIteration++;
                //set dirty flag here for a write
                stepCtx.setTransientUserData(Boolean.FALSE);
                throw new MyGrandchildException("fail on purpose with MyGrandchildException in write");
            }
            logger.fine("writeMyData receives chunk size=" + myData.size());
            StringBuilder sb = new StringBuilder("Next chunk is: ");
            for (Object o : myData) {
                sb.append(o.toString()).append(",");
            }
            String chunkStr = sb.toString();
            System.out.println(chunkStr.substring(0, chunkStr.length() - 1) + "\n");
            writerIteration++;

        }
    }

    public static class StepListener extends AbstractStepListener {

        @Inject
        @BatchProperty(name = "execution.number")
        String executionNumberString;

        @Inject
        StepContext stepCtx;

        @Override
        public void afterStep() throws Exception {
            //Execution number 7 which will have a clean flag since it passed
            if (executionNumberString.equals("7")) {
                assertTrue(stepCtx.getTransientUserData() == Boolean.TRUE);
            }//Execution number 8 and 9 will have a dirty flag since they failed
            else if (executionNumberString.equals("8") || executionNumberString.equals("9"))
                assertTrue(stepCtx.getTransientUserData() == Boolean.FALSE);
        }
    }

    @javax.inject.Named("mySkipReadListener")
    public static class MySkipReadListener implements SkipReadListener {

        @Inject
        JobContext jobCtx;

        private final static String sourceClass = MySkipReadListener.class.getName();
        private final static Logger logger = Logger.getLogger(sourceClass);

        public static final String GOOD_EXIT_STATUS = "MySkipReadListener: GOOD STATUS";
        public static final String BAD_EXIT_STATUS = "MySkipReadListener: BAD STATUS";

        @Override
        public void onSkipReadItem(Exception ex) {
            logger.fine("In readItem" + ex);

            if (ex instanceof MyParentException) {
                logger.fine("SKIPLISTENER: readItem, exception is an instance of: MyParentException");
                jobCtx.setExitStatus(GOOD_EXIT_STATUS);
            } else {
                logger.fine("SKIPLISTENER: readItem, exception is NOT an instance of: MyParentException");
                jobCtx.setExitStatus(BAD_EXIT_STATUS);
            }
        }
    }

    @javax.inject.Named("myRetryReadListener")
    public static class MyRetryReadListener implements RetryReadListener {

        @Inject
        JobContext jobCtx;

        private final static String sourceClass = MyRetryReadListener.class.getName();
        private final static Logger logger = Logger.getLogger(sourceClass);

        public static final String GOOD_EXIT_STATUS = "MyRetryReadListener: GOOD STATUS";
        public static final String BAD_EXIT_STATUS = "MyRetryReadListener: BAD STATUS";

        @Override
        public void onRetryReadException(Exception ex) throws Exception {
            logger.fine("In readItem" + ex);

            if (ex instanceof MyParentException) {
                logger.fine("RETRYLISTENER: readItem, exception is an instance of: MyParentException");
                jobCtx.setExitStatus(GOOD_EXIT_STATUS);
            } else {
                logger.fine("RETRYLISTENER: readItem, exception is NOT an instance of: MyParentException");
                jobCtx.setExitStatus(BAD_EXIT_STATUS);
            }
        }
    }
}
