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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import batch.fat.artifacts.EndOfJobNotificationListener;
import batch.fat.common.util.JobWaiter;
import batch.fat.common.util.TestFailureException;
import batch.fat.util.BatchFATHelper;

/**
 *
 */
@WebServlet(name = "StartUpRecovery", urlPatterns = { "/StartUpRecovery" })
public class StartUpRecoveryServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 4039165686736404172L;
    private final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    private final List<Long> jobInstances = new ArrayList<Long>();
    private final List<Long> executionInstances = new ArrayList<Long>();
    protected final static String jslParmsAttr = "jslParmsAttr";

    // We don't want to override an app set exit status
    private static final String APP_SET_EXIT_STATUS = "APP SET EXIT STATUS";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {

        logger.info("In StartUpRecovery, called with URL: " + req.getRequestURL() + "?" + req.getQueryString());

        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        String testName = req.getParameter("testName");
        String serverName = req.getParameter("serverName");
        String hostName = req.getParameter("hostName");
        String userDir = req.getParameter("userDir");
        String executionId = req.getParameter("executionId");
        String expectedBatchStatus = req.getParameter("expectedStatus");

        try {
            if (testName.equals("setUpDB")) {
                String entries = setUpDBForTest(hostName, userDir, serverName);
                //return list of execution ids back to the caller
                pw.println(entries);
            } else {
                verifyResult(pw, testName, Long.parseLong(executionId), expectedBatchStatus);
                pw.println(BatchFATHelper.SUCCESS_MESSAGE);
            }
        } catch (Exception e) {
            pw.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @param useJpa
     * @throws NamingException
     * @throws SQLException
     * @return string in format jobInstances=[1 2 3],executionInstances=[
     */
    private String setUpDBForTest(String hostName, String userDir, String serverName) throws Exception {

        long jobExecId = BatchRuntime.getJobOperator().start("NullPropOnJobExec", null);
        // A hack to get any old stepExecId which we can use as the top-level stepExecId in 
        // the row we're going to create.
        JobWaiter waiter = new JobWaiter();
        waiter.waitForAfterJobNotification(EndOfJobNotificationListener.class);
        waiter.pollForFinalState(jobExecId);

        long dummyTopLevelStepExecId = BatchRuntime.getJobOperator().getStepExecutions(jobExecId).get(0).getStepExecutionId();

        jobInstances.add(generateJobInstanceEntry(1));
        jobInstances.add(generateJobInstanceEntry(2));
        jobInstances.add(generateJobInstanceEntry(3));
        jobInstances.add(generateJobInstanceEntry(4));
        jobInstances.add(generateJobInstanceEntry(5));

        executionInstances.add(generateJobExecutionEntry(jobInstances.get(0), BatchStatus.STARTING, null, hostName, userDir, serverName));
        executionInstances.add(generateJobExecutionEntry(jobInstances.get(1), BatchStatus.STARTED, null, hostName, userDir, serverName));
        executionInstances.add(generateJobExecutionEntry(jobInstances.get(2), BatchStatus.STARTED, APP_SET_EXIT_STATUS, hostName, userDir, serverName));
        executionInstances.add(generateJobExecutionEntry(jobInstances.get(3), BatchStatus.STOPPING, null, hostName, userDir, serverName));
        executionInstances.add(generateJobExecutionEntry(jobInstances.get(4), BatchStatus.STOPPED, BatchStatus.STOPPED.name(), hostName, userDir, serverName));

        generateStepExecutionEntry(executionInstances.get(0), dummyTopLevelStepExecId, BatchStatus.STARTING, null);
        generateStepExecutionEntry(executionInstances.get(1), dummyTopLevelStepExecId, BatchStatus.STARTED, null);
        generateStepExecutionEntry(executionInstances.get(2), dummyTopLevelStepExecId, BatchStatus.STARTED, APP_SET_EXIT_STATUS);
        generateStepExecutionEntry(executionInstances.get(3), dummyTopLevelStepExecId, BatchStatus.STOPPING, null);
        generateStepExecutionEntry(executionInstances.get(4), dummyTopLevelStepExecId, BatchStatus.STOPPED, BatchStatus.STOPPED.name());

        //not able to create the jobstatus and step status entry because
        //we store them as blob.  Servlet does not have access to the internal JobStatus and StepStatus class
        //to create object.

        //we will test the externally visible status such as from JobExecution and StepExecution

        StringBuffer buffer = new StringBuffer();
        buffer.append(Arrays.toString(executionInstances.toArray()));
        logger.info("dbString = " + buffer.toString());

        return buffer.toString();

    }

    /**
     * Insert a new entry for step execution to db
     * 
     * @param long1
     * @param starting
     */
    private void generateStepExecutionEntry(Long executionId, Long dummyTopLevelStepExecId, BatchStatus batchStatus, String exitStatus) throws Exception {
        long stepExecutionId = -1L;

        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        long time = System.currentTimeMillis();
        Timestamp timestamp = new Timestamp(time);

        // I suppose we're not buying ourselves much by enforcing that the FK_TOPLVL_STEPEXECID can never be null
        String query = "INSERT INTO JBATCH.STEPTHREADEXECUTION (FK_JOBEXECID, batchstatus, exitstatus, stepname, M_READ, "
                       + "M_WRITE, M_COMMIT, M_ROLLBACK, M_READSKIP, M_PROCESSSKIP, M_WRITESKIP, M_FILTER, STARTTIME, ENDTIME, FK_TOPLVL_STEPEXECID, INTERNALSTATUS, PARTNUM, THREADTYPE) "
                       + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, -1, 'T' )"; // 'T' => (T)op-level.

        try {
            conn = getDataSourceConnection();
            statement = conn.prepareStatement(query, new String[] { "STEPEXECID" });

            statement.setLong(1, executionId);
            statement.setInt(2, batchStatus.ordinal());
            statement.setString(3, exitStatus);
            statement.setString(4, "stepName_" + executionId);
            statement.setLong(5, 0);
            statement.setLong(6, 0);
            statement.setLong(7, 0);
            statement.setLong(8, 0);
            statement.setLong(9, 0);
            statement.setLong(10, 0);
            statement.setLong(11, 0);
            statement.setLong(12, 0);
            statement.setTimestamp(13, timestamp);
            statement.setTimestamp(14, timestamp);
            statement.setLong(15, dummyTopLevelStepExecId);

            statement.executeUpdate();
            rs = statement.getGeneratedKeys();
            if (rs.next()) {
                stepExecutionId = rs.getLong(1);
            }
        } catch (Exception ex) {
            logger.info("exception creating execution instance: " + ex.toString());
            throw new TestFailureException(ex.getMessage());
        } finally {
            cleanupConnection(conn, rs, statement);
        }
        String msg = "generateStepExecutionEntry return execution instance:[" + stepExecutionId + "]";
        logger.info(msg);
    }

    /**
     * Insert a new entry to EXECUTIONINSTANCEDATA table
     * 
     * @param i
     * @return
     */
    private Long generateJobExecutionEntry(long jobInstance, BatchStatus status, String exitStatus, String hostName, String userDir, String serverName) throws Exception {
        String sql = "INSERT INTO JBATCH.JOBEXECUTION (FK_JOBINSTANCEID, createtime, updatetime, batchstatus, exitstatus, jobparameters, serverId, EXECNUM, RESTURL) VALUES(?, ?, ?, ?, ?, ?, ?, 0, 'NOTSET')";
        PreparedStatement statement = null;
        ResultSet rs = null;
        Connection conn = null;
        long time = System.currentTimeMillis();
        Timestamp timestamp = new Timestamp(time);
        long newJobExecutionId = 0;
        String serverId = hostName + "/" + userDir + "/" + serverName;
        logger.info("generateJobExecutionEntry jobinstanceid=" + jobInstance + ",serverId=" + serverId);
        try {
            conn = getDataSourceConnection();
            statement = conn.prepareStatement(sql, new String[] { "JOBEXECID" });
            statement.setLong(1, jobInstance);
            statement.setTimestamp(2, timestamp);
            statement.setTimestamp(3, timestamp);
            statement.setInt(4, status.ordinal());
            statement.setString(5, exitStatus);
            statement.setObject(6, serializeObject(new Properties()));
            statement.setString(7, serverId);
            int rc = statement.executeUpdate();
            logger.info("generateJobExecutionEntry: for jobinstance=" + jobInstance + ", rc=" + rc);
            rs = statement.getGeneratedKeys();
            if (rs.next()) {
                newJobExecutionId = rs.getLong(1);
            }

        } catch (Exception ex) {
            logger.info("exception creating execution instance: " + ex.toString());
            throw new TestFailureException(ex.getMessage());
        } finally {
            cleanupConnection(conn, rs, statement);
        }
        String msg = "generateJobExecutionEntry return execution instance:[" + newJobExecutionId + "]";
        logger.info(msg);
        return newJobExecutionId;
    }

    /**
     * This method is used to serialized an object saved into a table BLOB field.
     * 
     * @param theObject the object to be serialized
     * @return a object byte array
     * @throws IOException
     */
    private byte[] serializeObject(Serializable theObject) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(baos);
        oout.writeObject(theObject);
        byte[] data = baos.toByteArray();
        baos.close();
        oout.close();

        return data;
    }

    /**
     * @param i
     */
    private long generateJobInstanceEntry(int i) throws Exception {
        String methodName = "generateJobInstanceEntry";

        String sql_1 = "INSERT INTO JBATCH.JOBINSTANCE (JOBNAME, SUBMITTER, AMCNAME, BATCHSTATUS, CREATETIME, INSTANCESTATE, NUMEXECS) VALUES(?, ?, ?, ?, ?, 1, 1)";

        logger.info(methodName + ",sql=" + sql_1);

        long jobInstanceID = 0;

        PreparedStatement statement = null;
        ResultSet rs = null;
        Connection conn = null;
        try {
            conn = getDataSourceConnection();
            statement = conn.prepareStatement(sql_1, new String[] { "JOBINSTANCEID" });
            statement.setString(1, "name_" + i);
            statement.setString(2, "submitter_" + i);
            statement.setString(3, "appNameToInsert_" + i);
            statement.setInt(4, BatchStatus.STARTING.ordinal());
            statement.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            statement.executeUpdate();
            rs = statement.getGeneratedKeys();
            if (rs.next()) {
                jobInstanceID = rs.getLong(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TestFailureException(ex.getMessage());
        } finally {
            cleanupConnection(conn, rs, statement);
        }

        logger.info(methodName + ",return job instance:[" + jobInstanceID + "]");
        return jobInstanceID;
    }

    /**
     * closes connection, result set and statement
     * 
     * @param conn - connection object to close
     * @param rs - result set object to close
     * @param statement - statement object to close
     */
    private void cleanupConnection(Connection conn, ResultSet rs, PreparedStatement statement) {

        try {
            if (statement != null) {
                statement.close();
            }

            if (rs != null) {
                rs.close();
            }

            if (conn != null) {
                conn.commit();
            }
        } catch (SQLException e) {

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {

                }
            }
        }

    }

    /**
     * @return a connection to the dataSource described by the request parms.
     */
    protected Connection getDataSourceConnection() throws SQLException, NamingException {
        log("getDataSourceConnection: from dataSourceJndi: jdbc/batch");

        return getDataSource("jdbc/batch").getConnection();
    }

    /**
     * @return the DataSource with the given jndi name.
     */
    protected DataSource getDataSource(String dataSourceJndi) throws NamingException {
        log("getDataSource: from jndi: " + dataSourceJndi);
        return DataSource.class.cast(new InitialContext().lookup(dataSourceJndi));
    }

    private void verifyResult(PrintWriter pw, String testName, long executionId, String expectedStatus) throws TestFailureException {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        if (testName.startsWith("recoverLocalJobs")) {
            try {
                JobExecution executionInst = jobOperator.getJobExecution(executionId);
                String exitStatus = executionInst.getExitStatus();
                String batchStatus = executionInst.getBatchStatus().name();
                logger.info("verifyResult for " + testName + ", executionId=" + executionId + ",status from DB=" + batchStatus);
                assertEquals(expectedStatus, batchStatus);
                if (testName.equals("recoverLocalJobsInStartedWithExitStatusSetTest")) {
                    assertEquals(APP_SET_EXIT_STATUS, exitStatus);
                } else {
                    assertEquals(expectedStatus, exitStatus);
                }
            } catch (Exception e) {
                String msg = "ERROR: " + e.getMessage();
                throw new TestFailureException(msg);
            }
        } else if (testName.startsWith("recoverStepExecution")) {
            try {
                List<StepExecution> stepExecutions = jobOperator.getStepExecutions(executionId);
                assertEquals("Didn't find exactly one StepExecution for jobExec = " + executionId, 1, stepExecutions.size());
                for (StepExecution stepExe : stepExecutions) {
                    String exitStatus = stepExe.getExitStatus();
                    String batchStatus = stepExe.getBatchStatus().name();
                    assertEquals(expectedStatus, batchStatus);
                    if (testName.equals("recoverStepExecutionInStartedWithExitStatusSetTest")) {
                        assertEquals(APP_SET_EXIT_STATUS, exitStatus);
                    } else {
                        assertEquals(expectedStatus, exitStatus);
                    }
                }
            } catch (Exception e) {
                String msg = "ERROR: " + e.getMessage();
                throw new TestFailureException(msg);
            }
        }
    }
}
