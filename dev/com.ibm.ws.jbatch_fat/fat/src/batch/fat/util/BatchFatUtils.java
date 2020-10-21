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

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.batch.runtime.StepExecution;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;

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

    /**
     * Converts List<StepExecution> to map, with key equal to "step name (id)"
     * 
     * @param stepExecutions
     * @return
     */
    public static Map<String, StepExecution> getStepExecutionMap(List<StepExecution> stepExecutions) {
        Map<String, StepExecution> map = new HashMap<String, StepExecution>();
        for (StepExecution step : stepExecutions) {
            map.put(step.getStepName(), step);
        }

        return Collections.unmodifiableMap(map);
    }

}
