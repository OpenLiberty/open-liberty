/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package batch.fat.util;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.jbatch.test.FatUtils;
import com.ibm.ws.jbatch.test.dbservlet.DbServletClient;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class BatchFatUtils {

    public static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";

    /**
     * @return http://{server.host}:{server.port}/{contextRoot}{uri}
     */
    public static URL buildUrl(LibertyServer server, String contextRoot, String uri) throws IOException {
        return new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot + uri);
    }

    /**
     * @param jobExecution
     *
     * @return true if jobExecution.batchStatus is any of STOPPED, FAILED, COMPLETED, ABANDONED.
     */
    public static boolean isDone(JsonObject jobExecution) {
        String batchStatus = jobExecution.getString("batchStatus");
        return ("STOPPED".equals(batchStatus) ||
                "FAILED".equals(batchStatus) ||
                "COMPLETED".equals(batchStatus) || "ABANDONED".equals(batchStatus));
    }

    /**
     * Parse job parameters from the request's query parms.
     *
     * @param queryParmNames The query parms to include in the job parameters Properties object
     *
     * @return the given query parms as a Properties object.
     */
    public static Properties getJobParameters(HttpServletRequest request, String... queryParmNames) throws IOException {
        Properties retMe = new Properties();

        for (String queryParmName : queryParmNames) {
            String queryParmValue = request.getParameter(queryParmName);
            if (queryParmValue != null) {
                retMe.setProperty(queryParmName, queryParmValue);
            }
        }

        return retMe;
    }

    public static void restartServer(LibertyServer server) throws Exception {

        if (server != null && server.isStarted()) {
            server.restartServer();
            FatUtils.waitForStartupAndSsl(server);
        }
    }

    public static void stopServer(LibertyServer server) throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    public static void startServer(LibertyServer server) throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer();
            FatUtils.waitForStartupAndSsl(server);
        }
    }

    /**
     * @param stepExecutions
     * @param stepName
     * @param numPartitionsExpected
     * @Param BatchStatus defaults to COMPLETED if not provided
     */
    public static void checkPartitionsSizeAndStatusCompleted(JsonArray stepExecutions, String stepName, int numPartitionsExpected, BatchStatus... batchStatus) {

        for (Object step : stepExecutions.toArray()) {

            if (((JsonObject) step).getString("stepName").equalsIgnoreCase(stepName)) {
                //Casting to JsonObject
                JsonObject stepExecution = (JsonObject) step;
                checkPartitionsSizeAndStatusCompleted(stepExecution, numPartitionsExpected, batchStatus);

                return;
            }
        }

    }

    /**
     * @param stepExecutions
     * @param stepName
     * @param numPartitionsExpected
     * @Param BatchStatus defaults to COMPLETED if not provided
     */
    public static void checkPartitionsSizeAndStatusCompleted(JsonObject stepExecution, int numPartitionsExpected, BatchStatus... batchStatus) {
        BatchStatus status;
        if (batchStatus.length == 0) {
            status = BatchStatus.COMPLETED;
        } else {
            status = batchStatus[0];
        }

        JsonArray partitionExecutions = stepExecution.getJsonArray("partitions");

        assertEquals(numPartitionsExpected, partitionExecutions.size());

        for (Object partitionExecution : partitionExecutions.toArray()) {
            assertEquals(status.toString(), ((JsonObject) partitionExecution).getString("batchStatus"));
        }
    }

    /**
     * check batchStatus of all elements in the given JsonArray against expected batchstatus
     *
     * @param array
     * @param numPartitionsExpected
     * @Param BatchStatus defaults to COMPLETED
     */
    public static void checkExpectedBatchStatus(JsonArray array, int numExpected, BatchStatus... batchStatus) {
        BatchStatus status;
        if (batchStatus.length == 0) {
            status = BatchStatus.COMPLETED;
        } else {
            status = batchStatus[0];
        }

        assertEquals(numExpected, array.size());

        for (Object partitionExecution : array.toArray()) {
            assertEquals(status.toString(), ((JsonObject) partitionExecution).getString("batchStatus"));
        }
    }

    /*
     * Sets mark to end of trace for dispatcher, endpoint and endpoint2
     */
    public static void setMarkToEndOfTraceForAllServers(LibertyServer... servers) throws Exception {

        for (LibertyServer server : servers) {
            server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        }
    }

    public static String executeSqlUpdate(LibertyServer server, String dataSourceJndi, String sql) throws Exception {

        String userName = "user";
        String password = "pass";

        HttpURLConnection conn = new DbServletClient().setDataSourceJndi(dataSourceJndi).setDataSourceUser(userName,
                                                                                                           password).setHostAndPort(server.getHostname(),
                                                                                                                                    server.getHttpDefaultPort()).setSql(sql).executeUpdate();

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String retVal = br.readLine();
        br.close();

        return retVal;
    }

}
