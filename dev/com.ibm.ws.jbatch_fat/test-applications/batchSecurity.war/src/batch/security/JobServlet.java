package batch.security;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.PrivilegedExceptionAction;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.operations.JobSecurityException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.naming.InitialContext;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;

/*import com.ibm.websphere.security.auth.WSSubject;
 import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
 import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;*/

/*import com.ibm.websphere.security.auth.WSSubject;
 import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
 import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;*/

/**
 * This servlet starts/stops/restarts/gets status/etc of batch
 * jobs via BatchRuntime JobOperator.
 * 
 */
@WebServlet(urlPatterns = { "/jobservlet" })
@ServletSecurity(value = @HttpConstraint(transportGuarantee = ServletSecurity.TransportGuarantee.CONFIDENTIAL),
                 httpMethodConstraints = { @HttpMethodConstraint(value = "POST", emptyRoleSemantic = ServletSecurity.EmptyRoleSemantic.PERMIT),
                                          @HttpMethodConstraint(value = "GET", emptyRoleSemantic = ServletSecurity.EmptyRoleSemantic.PERMIT),
                                          @HttpMethodConstraint(value = "PUT", emptyRoleSemantic = ServletSecurity.EmptyRoleSemantic.PERMIT) })
public class JobServlet extends HttpServlet {

    protected final static Logger logger = Logger.getLogger(JobServlet.class.getName());

    public static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";

    /**
     * Logging helper.
     */
    protected static void log(String method, Object msg) {
        System.out.println("JobServlet: " + method + ": " + String.valueOf(msg));
        // logger.info("JobServlet: " + method + ": " + String.valueOf(msg));
    }

    /**
     * Request entry point.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        log("doGet", "URL: " + request.getRequestURL() + "?" + request.getQueryString());

        String action = request.getParameter("action");

        UserTransaction ut = beginTran(request);

        try {
            if ("abandon".equalsIgnoreCase(action)) {
                abandon(request, response);
            } else if ("start".equalsIgnoreCase(action)) {
                start(request, response, getRunAsSubject(request));
            } else if ("startAs".equalsIgnoreCase(action)) {
                startAs(request, response);
            } else if ("startUnderUserTran".equalsIgnoreCase(action)) {
                startUnderUserTran(request, response);
            } else if ("getJobExecutions".equalsIgnoreCase(action)) {
                getJobExecutions(request, response, getRunAsSubject(request));
            } else if ("getJobExecution".equalsIgnoreCase(action)) {
                getJobExecution(request, response, getRunAsSubject(request));
            } else if ("getJobInstance".equalsIgnoreCase(action)) {
                getJobInstance(request, response);
            } else if ("getJobInstanceCount".equalsIgnoreCase(action)) {
                getJobInstanceCount(request, response);
            } else if ("getJobInstances".equalsIgnoreCase(action)) {
                getJobInstances(request, response);
            } else if ("getJobNames".equalsIgnoreCase(action)) {
                getJobNames(request, response);
            } else if ("getParameters".equalsIgnoreCase(action)) {
                getParameters(request, response);
            } else if ("getRunningJobExecutions".equalsIgnoreCase(action)) {
                getRunningJobExecutions(request, response);
            } else if ("getStepExecutions".equalsIgnoreCase(action)) {
                getStepExecutions(request, response);
            } else if ("stop".equalsIgnoreCase(action)) {
                stop(request, response);
            } else if ("restart".equalsIgnoreCase(action)) {
                restart(request, response);
            } else {
                throw new IOException("action not recognized: " + action);
            }
        } catch (JobSecurityException e) {
            response.sendError(HttpURLConnection.HTTP_UNAUTHORIZED, e.getMessage());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof JobSecurityException) {
                response.sendError(HttpURLConnection.HTTP_UNAUTHORIZED, e.getCause().getMessage());
            } else {
                throw new ServletException(e);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            endTran(ut);
        }
    }

    /**
     * Begin a UserTransaction, if requested.
     * 
     * @return the UserTransaction, or null if not requested.
     */
    protected UserTransaction beginTran(HttpServletRequest request) throws ServletException {

        if (request.getParameter("beginTran") == null) {
            return null;
        }

        try {
            log("beginTran", "Looking up UserTransaction...");
            UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

            log("beginTran", "Beginning UserTransaction...");
            ut.begin();

            return ut;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Commit the given tran (if not null).
     */
    protected void endTran(UserTransaction ut) throws ServletException {

        if (ut == null) {
            return;
        }

        try {
            log("endTran", "Committing UserTransaction...");
            ut.commit();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Start the job.
     * 
     * Sends back the JobInstance record as JSON.
     */
    protected void start(HttpServletRequest request, HttpServletResponse response, Subject subject) throws Exception {

        JobOperator jobOperator = BatchRuntime.getJobOperator();

        final String jobXMLName = request.getParameter("jobXMLName");

        final Properties jobParams = new Properties();
        String forceFailure = request.getParameter("force.failure");
        if (forceFailure != null) {
            jobParams.setProperty("force.failure", forceFailure);
        }

        JobInstance jobInstance = (JobInstance) WSSubject.doAs(subject, new PrivilegedExceptionAction<JobInstance>() {
            @Override
            public JobInstance run() throws Exception {
                long execId = BatchRuntime.getJobOperator().start(jobXMLName, jobParams);

                return BatchRuntime.getJobOperator().getJobInstance(execId);
            }
        });

        log("start", "jobInstance: " + jobInstance);

        sendJsonResponse(response, toJsonObject(jobInstance));
    }

    /**
     * Start the job as the user indicated in the request.
     * 
     * Sends back the JobInstance record as JSON.
     */
    protected void startAs(HttpServletRequest request, HttpServletResponse response) throws Exception {

        start(request,
              response,
              login(request.getParameter("user"), request.getParameter("password")));
    }

    /**
     * Start a UserTransaction, then start the job.
     */
    protected void startUnderUserTran(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log("startUnderUserTran", "Looking up UserTransaction...");
        UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        log("startUnderUserTran", "Beginning UserTransaction...");
        ut.begin();

        start(request, response, getRunAsSubject(request));

        log("startUnderUserTran", "Committing UserTransaction...");
        ut.commit();
    }

    /**
     * Send back a JSON array of all job executions for the given instanceId.
     */
    protected void getJobExecutions(HttpServletRequest request, HttpServletResponse response, Subject subject) throws Exception {

        final long instanceId = getLongParm(request, "instanceId");

        List<JobExecution> jobExecutions = (List<JobExecution>) WSSubject.doAs(subject, new PrivilegedExceptionAction<List<JobExecution>>() {
            @Override
            public List<JobExecution> run() throws Exception {

                return BatchRuntime.getJobOperator().getJobExecutions(new JobInstance() {
                    @Override
                    public long getInstanceId() {
                        return instanceId;
                    }

                    @Override
                    public String getJobName() {
                        return null; // Don't know, don't care.
                    }

                });
            }
        });

        sendJsonResponse(response, toJsonArray(jobExecutions));
    }

    /**
     * Send back a JSON array of all runningjob executions for the given instanceId.
     */
    protected void getRunningJobExecutions(HttpServletRequest request, HttpServletResponse response) throws IOException {

        final String jobName = getStringParm(request, "jobName");

        List<Long> jobExecutions = BatchRuntime.getJobOperator().getRunningExecutions(jobName);

        sendJsonResponse(response, toJsonArray(jobExecutions));
    }

    /**
     * Send back a JSON array of job parameters
     */
    protected void getParameters(HttpServletRequest request, HttpServletResponse response) throws IOException {

        final Long executionId = getLongParm(request, "executionId");

        Properties parameters = BatchRuntime.getJobOperator().getParameters(executionId);

        sendJsonResponse(response, toJsonObject(parameters));
    }

    /**
     * Send back a JSON array of all job names. Will be filtered if batch security is on.
     */
    protected void getJobNames(HttpServletRequest request, HttpServletResponse response) throws IOException {

        Set<String> jobNames = BatchRuntime.getJobOperator().getJobNames();

        sendJsonResponse(response, toJsonArray(jobNames));
    }

    /**
     * 
     * Sends back the requested job execution record.
     */
    protected void getJobExecution(HttpServletRequest request, HttpServletResponse response, Subject subject) throws Exception {

        final long execId = getLongParm(request, "executionId");

        JobExecution jobExecution = (JobExecution) WSSubject.doAs(subject, new PrivilegedExceptionAction<JobExecution>() {
            @Override
            public JobExecution run() throws Exception {
                return BatchRuntime.getJobOperator().getJobExecution(execId);
            }
        });

        sendJsonResponse(response, toJsonObject(jobExecution));
    }

    /**
     * 
     * Sends back the requested job execution record.
     */
    protected void getStepExecutions(HttpServletRequest request, HttpServletResponse response) throws IOException {

        long execId = getLongParm(request, "executionId");

        List<StepExecution> stepExecutions = BatchRuntime.getJobOperator().getStepExecutions(execId);

        sendJsonResponse(response, toJsonArrayOfSteps(stepExecutions));
    }

    /**
     * 
     * Sends back the requested job instance record.
     */
    protected void getJobInstance(HttpServletRequest request, HttpServletResponse response) throws IOException {

        long execId = getLongParm(request, "executionId");

        JobInstance jobInstance = BatchRuntime.getJobOperator().getJobInstance(execId);

        sendJsonResponse(response, toJsonObject(jobInstance));
    }

    /**
     * 
     * Sends back the requested job instance record.
     */
    protected void getJobInstances(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String jobName = getStringParm(request, "jobName");

        List<JobInstance> jobInstances = BatchRuntime.getJobOperator().getJobInstances(jobName, 0, 1);

        sendJsonResponse(response, toJsonArrayOfJobInstances(jobInstances));
    }

    /**
     * 
     * Sends back the requested job instance record.
     */
    protected void getJobInstanceCount(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String jobName = getStringParm(request, "jobName");

        int count = BatchRuntime.getJobOperator().getJobInstanceCount(jobName);

        sendJsonResponse(response, toJsonObject(count));
    }

    /**
     * Stop the job identified by the 'executionId' query parm.
     * 
     * Sends back the stopping/stopped job execution record.
     */
    protected void stop(HttpServletRequest request, HttpServletResponse response) throws IOException {

        long execId = getLongParm(request, "executionId");

        BatchRuntime.getJobOperator().stop(execId);

        JobExecution jobExecution = BatchRuntime.getJobOperator().getJobExecution(execId);

        sendJsonResponse(response, toJsonObject(jobExecution));
    }

    /**
     * Abandon the job identified by the 'executionId' query parm.
     * 
     * Sends back the stopping/stopped job execution record.
     */
    protected void abandon(HttpServletRequest request, HttpServletResponse response) throws IOException {

        long execId = getLongParm(request, "executionId");

        BatchRuntime.getJobOperator().abandon(execId);

        JobExecution jobExecution = BatchRuntime.getJobOperator().getJobExecution(execId);

        sendJsonResponse(response, toJsonObject(jobExecution));
    }

    /**
     * Restart the job identified by the 'executionId' query parm.
     * 
     * Sends back the newly started job execution record.
     */
    protected void restart(HttpServletRequest request, HttpServletResponse response) throws IOException {

        long execId = getLongParm(request, "executionId");

        Properties jobParams = null;
        String forceFailure = request.getParameter("force.failure");
        if (forceFailure != null) {
            jobParams = new Properties();
            jobParams.setProperty("force.failure", forceFailure);
        }

        long newExecId = BatchRuntime.getJobOperator().restart(execId, jobParams);

        JobExecution jobExecution = BatchRuntime.getJobOperator().getJobExecution(newExecId);

        sendJsonResponse(response, toJsonObject(jobExecution));
    }

    /**
     * Send the given jsonStructure as a JSON response over the given response object.
     * 
     * @param response
     * @param jsonObject
     * 
     */
    private void sendJsonResponse(HttpServletResponse response, JsonStructure jsonStructure) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MEDIA_TYPE_APPLICATION_JSON);

        JsonWriter jsonWriter = Json.createWriter(response.getOutputStream());
        jsonWriter.write(jsonStructure);
        jsonWriter.close();
    }

    /**
     * @return the value for the given query parm as a long.
     * 
     * @throws IOException if the parm is not defined.
     */
    protected long getLongParm(HttpServletRequest request, String parmName) throws IOException {

        String parmVal = request.getParameter(parmName);
        if (parmVal == null || parmVal.isEmpty()) {
            throw new IOException("Must specify parameter '" + parmName + "' in the query string");
        }

        return Long.parseLong(parmVal);
    }

    /**
     * @return the value for the given query parm as a String.
     * 
     * @throws IOException if the parm is not defined.
     */
    protected String getStringParm(HttpServletRequest request, String parmName) throws IOException {

        String parmVal = request.getParameter(parmName);
        if (parmVal == null || parmVal.isEmpty()) {
            throw new IOException("Must specify parameter '" + parmName + "' in the query string");
        }

        return parmVal;
    }

    /**
     * @return a Json version of the jobInstance record.
     */
    protected JsonObject toJsonObject(JobInstance jobInstance) {
        return Json.createObjectBuilder()
                        .add("jobName", jobInstance.getJobName())
                        .add("instanceId", jobInstance.getInstanceId())
                        .build();
    }

    /**
     * @return a Json version of the jobExecution record.
     */
    protected JsonObject toJsonObject(JobExecution jobExecution) {
        return Json.createObjectBuilder()
                        .add("jobName", jobExecution.getJobName())
                        .add("executionId", jobExecution.getExecutionId())
                        .add("batchStatus", jobExecution.getBatchStatus().name())
                        .add("exitStatus", defaultValue(jobExecution.getExitStatus(), ""))
                        .add("createTime", formatDate(jobExecution.getCreateTime()))
                        .add("endTime", formatDate(jobExecution.getEndTime()))
                        .add("lastUpdatedTime", formatDate(jobExecution.getLastUpdatedTime()))
                        .add("startTime", formatDate(jobExecution.getStartTime()))
                        .add("jobParameters", convertMapToJsonObject(jobExecution.getJobParameters()))
                        .build();
    }

    /**
     * @return a Json version of the jobExecution record.
     */
    protected JsonObject toJsonObject(int number) {
        return Json.createObjectBuilder()
                        .add("count", number)
                        .build();
    }

    /**
     * @return a Json array with the given jobExecution records.
     */
    protected JsonArray toJsonArray(Collection<JobExecution> jobExecutions) {

        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (JobExecution jobExecution : jobExecutions) {
            builder.add(toJsonObject(jobExecution));
        }

        return builder.build();
    }

    /**
     * @return a Json array with the given jobExecution records.
     */
    protected JsonArray toJsonArrayOfSteps(Collection<StepExecution> stepExecutions) {

        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (StepExecution stepExec : stepExecutions) {
            builder.add(stepExec.getStepName());
        }

        return builder.build();
    }

    /**
     * @return a Json object with the given Properties object
     */
    protected JsonObject toJsonObject(Properties props) {

        JsonObjectBuilder builder = Json.createObjectBuilder();

        if (props != null) {
            for (String key : props.stringPropertyNames()) {
                builder.add(key, props.getProperty(key));
            }
        }

        return builder.build();
    }

    /**
     * @return a Json array with the given job names.
     */
    protected JsonArray toJsonArray(Set<String> jobNames) {

        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (String jobName : jobNames) {
            builder.add(jobName);
        }

        return builder.build();
    }

    /**
     * @return a Json array with the given jobExecution records.
     */
    protected JsonArray toJsonArray(List<Long> jobExecutions) {

        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (Long jobExecution : jobExecutions) {
            builder.add(jobExecution);
        }

        return builder.build();
    }

    /**
     * @return a Json array of job ids with the given jobinstance records.
     */
    protected JsonArray toJsonArrayOfJobInstances(List<JobInstance> jobInstances) {

        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (JobInstance jobInstance : jobInstances) {
            builder.add(jobInstance.getInstanceId());
        }

        return builder.build();
    }

    /**
     * @return t1 if not null; otherwise t2
     */
    private static <T> T defaultValue(T t1, T t2) {
        return (t1 != null) ? t1 : t2;
    }

    /**
     * @return the given date formatted as a string, or "" if the date is null.
     */
    private static String formatDate(Date d) {
        return (d != null) ? new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS Z").format(d) : "";
    }

    /**
     * 
     * @return a JsonObject for the given map
     */
    public static JsonObject convertMapToJsonObject(Map map) {

        JsonObjectBuilder builder = Json.createObjectBuilder();

        for (Map.Entry entry : (Set<Map.Entry>) ((map != null) ? map.entrySet() : Collections.EMPTY_SET)) {
            builder.add((String) entry.getKey(), (String) entry.getValue());
        }

        return builder.build();
    }

    /**
     * Login via the security sub-system.
     * 
     * @return An authenticated Subject for the user/pass in the request, or null if no user/pass was supplied.
     */
    private Subject getRunAsSubject(HttpServletRequest request) throws LoginException, WSSecurityException {

        String user = request.getParameter("user");
        String pass = request.getParameter("password");

        return (user == null || user.isEmpty())
                        ? WSSubject.getRunAsSubject()
                        : login(user, pass);
    }

    /**
     * Login via the security sub-system.
     * 
     * @return An authenticated Subject for the given username and password.
     */
    private Subject login(String username, String password) throws LoginException {

        LoginContext lc = new LoginContext(JaasLoginConfigConstants.APPLICATION_WSLOGIN, new WSCallbackHandlerImpl(username, password));
        lc.login();
        Subject subj = lc.getSubject();

        log("login", "Subject: " + subj);
        return subj;
    }

}
