package com.ibm.ws.jbatch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.KeyStore;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * Utility methods for using the jbatch CLI utility (wlp/bin/batchManager).
 */
public class BatchManagerCliUtils {

	public static final String JOB_NAME_NOT_SET = "";
	public static final int TIMEOUT_SLEEP = 20000;
	
    /**
     * The server object is mainly used for finding and invoking the batchManager CLI.
     */
    private final LibertyServer server;

    /**
     * CTOR.
     */
    public BatchManagerCliUtils(LibertyServer server) {
        this.server = server;
    }

    /**
     * Issues a message if the assumption is NOT true.
     */
    private void myAssumeTrue( String msg, boolean isItTrue ) {
        
        if ( ! isItTrue ) {
            log("myAssumeTrue", "ASSUMPTION FAILED: " + msg);
        }
        
        assumeTrue( isItTrue );
    }
    
    /**
     * Note: runs in the server root/config dir, in case batchManager generates any output
     * files, e.g. when getting joblogs. This way the files will be captured in the autoFVT output
     * (and they won't clutter up the install root dir).
     * 
     * @return ProgramOutput from the given submit command.
     */
    public ProgramOutput executeCommand(String[] args) throws Exception {
        return executeCommand(args, buildEnv(args) );
    }
    
    /**
     * Note: runs in the server root/config dir, in case batchManager generates any output
     * files, e.g. when getting joblogs. This way the files will be captured in the autoFVT output
     * (and they won't clutter up the install root dir).
     * 
     * @return ProgramOutput from the given submit command.
     */
    public ProgramOutput executeCommand(String[] args, Properties env) throws Exception {

        ProgramOutput po = server.getMachine().execute(server.getInstallRoot() + "/bin/batchManager",
                                                       args,
                                                       server.getServerRoot(),
                                                       env);
        
        log("executeCommand", "batchManager args: " + Arrays.asList(args));
        log("executeCommand", "batchManager RC:" + po.getReturnCode());
        log("executeCommand", "batchManager stdout:\n" + po.getStdout());
        log("executeCommand", "batchManager stderr:\n" + po.getStderr());
        
        // Sometimes when the test server is running REALLY slow, the batchManager CLI
        // times out waiting for an HTTP response.  Normally this would cause intermittent
        // test failures and build-break defects.  To avoid them, the following assumption
        // will abort the test (without failing) if a timeout is detected.  None of our
        // tests are specifically trying to cause an HTTP timeout, so they won't be disrupted
        // by this assumption.  And we can safely assume that MOST of the time the tests
        // will NOT hit a timeout, so most of the time the tests will run to completion.
        myAssumeTrue( "executeCommand: Detected SocketTimeoutException", po.getStdout().contains("java.net.SocketTimeoutException") == false );
        
        // Need to give jobs time to finish before executing the next test case during a timeout scenario.
        if(po.getStdout().contains("java.net.SocketTimeoutException")) {
            log("executeCommand", "Timeout detected.  Sleeping for " + TIMEOUT_SLEEP + " seconds to allow existing jobs to finish.");
            Thread.sleep(TIMEOUT_SLEEP);
        }
            

        return po;
    }

    /**
     * @return ProgramOutput from the given submit command.
     */
    public ProgramOutput submitJob(String[] args) throws Exception {

        ProgramOutput po = executeCommand(args);
        boolean contPropsWait = false;
        String lastContPropsArg = null;
        for(String prop : Arrays.asList(args)){//get last prop
	        if(prop.contains("--controlPropertiesFile")){
	        	lastContPropsArg = prop;
	        }
        }
        
        if(lastContPropsArg != null){
        	contPropsWait = contPropsHasWait(lastContPropsArg);
        }

        if (!Arrays.asList(args).contains("--wait") && !contPropsWait) {
           assertEquals(po.getStdout(), 0, po.getReturnCode());
        } else {
            assertTrue(po.getStdout(), po.getStdout().contains("CWWKY0105I:")); // job finished message
            assertTrue(po.getStdout(), po.getStdout().contains("CWWKY0107I:")); // job execution message
        }
        
        if (Arrays.toString(args).contains("--restartTokenFile")) {
        	assertTrue(po.getStdout(), (po.getStdout().contains("CWWKY0101I:") || po.getStdout().contains("CWWKY0102I:")));
        } else {
        	assertTrue(po.getStdout(), po.getStdout().contains("CWWKY0101I:"));
        }
        
        assertTrue(po.getStdout(), po.getStdout().contains("CWWKY0106I:"));

        return po;
    }
    
    static public boolean contPropsHasWait(String prop) throws IOException{
		int idx = prop.indexOf("=");
        String path = prop.substring(idx + 1);
        Properties temp = new Properties();
        String[] files = path.split(",");//Javadoc guarantees order in which they occur
        for(String value : files){
        	if(value.length() > 0){
                File propsFile = (StringUtils.isEmpty(value)) ? null : new File(value);
		        if (propsFile != null) {
		            InputStream is;
		            try {
						is = new FileInputStream(propsFile);
					} catch (FileNotFoundException fe) {
						throw fe;
					}
					try {
			            temp.load(is);
					} catch (IOException ioe){
						throw ioe;
					} finally{
						is.close();
					}
					if(temp.containsKey("--wait")){
						return true;
					}
		        }
            }
        }
    	return false;
    }

    /**
     * @return ProgramOutput from the given submit command.
     */
    public ProgramOutput restartJob(String[] args) throws Exception {

        ProgramOutput po = executeCommand(args);

        if (!Arrays.asList(args).contains("--wait")) {
            assertEquals(po.getStdout(), 0, po.getReturnCode());
        } else {
            assertTrue(po.getStdout(), po.getStdout().contains("CWWKY0105I:")); // job finished message
            assertTrue(po.getStdout(), po.getStdout().contains("CWWKY0107I:")); // job execution message
        }

        assertTrue(po.getStdout(), po.getStdout().contains("CWWKY0102I:"));
        assertTrue(po.getStdout(), po.getStdout().contains("CWWKY0106I:"));

        return po;
    }

    /**
     * Send STOP request for given job.
     * 
     * @return ProgramOutput
     */
    public ProgramOutput stopJob(String[] args) throws Exception {
        ProgramOutput po = executeCommand(args);

        // See defect 141403 for an explanation of this line.
        myAssumeTrue("stopJob: job no longer running", !po.getStdout().contains("JobExecutionNotRunningException"));

        // Due to timing, the job may have already stopped by the time the request is sent.
        myAssumeTrue("stopJob: job no longer running", !po.getStdout().contains("is not currently running"));

        if (!Arrays.asList(args).contains("--wait")) {
            assertEquals(po.getStdout(), 0, po.getReturnCode());
        } else {
            assertTrue(po.getStdout(), po.getStdout().contains("CWWKY0103I:")); // job stopped message
            assertTrue(po.getStdout(), po.getStdout().contains("CWWKY0107I:")); // job execution message
        }

        assertTrue(po.getStdout(), po.getStdout().contains("CWWKY0104I:"));

        return po;
    }
    
    /**
     * @return The createDate parsed from the status message in YYYY-MM-DD format
     */
    public static String getJobCreateDateFromStatus(String stdout) {
    	final Matcher matcher = Pattern.compile("createTime.*?([0-9][0-9][0-9][0-9]\\/[0-9][0-9]\\/[0-9][0-9])").matcher(stdout);
        assertTrue(stdout, matcher.find());
        
        return matcher.group(1).replace("/", "-");
    }

    /**
     * @return JobInstance object parsed from "CWWKY0101I" message.
     */
    public static JobInstance parseJobInstanceFromSubmitMessages(String stdout) {
        final Matcher matcher = Pattern.compile("CWWKY0101I: Job (|\\S+) with instance ID (\\d+) has been submitted[.]").matcher(stdout);
        assertTrue(stdout, matcher.find());

        return new JobInstance() {

            @Override
            public long getInstanceId() {
                return Long.parseLong(matcher.group(2));
            }

            @Override
            public String getJobName() {
                return matcher.group(1);
            }

        };
    }

    /**
     * INFO: CWWKY0107I: JobExecution:{"jobName":"test_batchlet_stepCtx","executionId":47,"batchStatus":"COMPLETED","exitStatus":"JOB: VERY GOOD INVOCATION","createTime":
     * "2015/03/04 15:21:10.877 +0000"
     * ,"endTime":"2015/03/04 15:21:12.967 +0000","lastUpdatedTime":"2015/03/04 15:21:12.967 +0000","startTime":"2015/03/04 15:21:11.380 +0000","jobParameters":{}}
     */
    public static JobExecution parseJobExecutionMessage(String stdout) {
        final Matcher matcher = Pattern.compile("CWWKY0107I: JobExecution:(.*)").matcher(stdout);
        assertTrue(stdout, matcher.find());

        return new JobExecutionModel(JsonHelper.readJsonObject(new StringReader(matcher.group(1))));
    }

    /**
     * CWWKY0106I:
     * JobInstance:{"jobName":"test_batchlet_stepCtx","instanceId":1,"appName":"SimpleBatchJob#SimpleBatchJob.war","submitter":"bob","batchStatus":"STARTING","jobXMLName"
     * :"test_batchlet_stepCtx"}
     */
    public static JobInstanceModel parseJobInstanceMessage(String stdout) {
        final Matcher matcher = Pattern.compile("CWWKY0106I: JobInstance:(.*)").matcher(stdout);
        assertTrue(stdout, matcher.find());

        return new JobInstanceModel(JsonHelper.readJsonObject(new StringReader(matcher.group(1))));
    }
    
    public ProgramOutput getStatus(long jobInstanceId) throws Exception {
        ProgramOutput po = executeCommand(new String[] { "status",
                                                        "--batchManager=" + getHostAndPort(),
                                                        "--user=bob",
                                                        "--password=bobpwd",
                                                        "--jobInstanceId=" + jobInstanceId
        });

        assertTrue("Actual status rc: " + po.getReturnCode() + "; " + po.getStdout(), 
                   po.getReturnCode() >= 0 && po.getReturnCode() < 40);

        return po;
    }
    
    /**
     * 
     */
    public ProgramOutput getStatus(JobInstance jobInstance, int port) throws Exception {
        ProgramOutput po = executeCommand(new String[] { "status",
                                                        "--batchManager=localhost:"+port,
                                                        "--user=bob",
                                                        "--password=bobpwd",
                                                        "--jobInstanceId=" + jobInstance.getInstanceId()
        });

        assertTrue("Actual status rc: " + po.getReturnCode() + "; " + po.getStdout(), 
                   po.getReturnCode() >= 0 && po.getReturnCode() < 40);

        return po;
    }

    /**
     * 
     */
    public ProgramOutput getStatus(JobInstance jobInstance) throws Exception {
        ProgramOutput po = executeCommand(new String[] { "status",
                                                        "--batchManager=" + getHostAndPort(),
                                                        "--user=bob",
                                                        "--password=bobpwd",
                                                        "--jobInstanceId=" + jobInstance.getInstanceId()
        });

        assertTrue("Actual status rc: " + po.getReturnCode() + "; " + po.getStdout(), 
                   po.getReturnCode() >= 0 && po.getReturnCode() < 40);

        return po;
    }
    
    /**
     * Poll status every second for a max timeout_s seconds waiting for job to get to one
     * of the given statuses
     * 
     * @return the latest jobexecution record, or null if the desired status was never achieved.
     */
    public JobExecution waitForStatus(int timeout_s,
                                      JobInstance jobInstance,
                                      int port,
                                      BatchStatus... batchStatuses) throws Exception {
        for (int i = 0; i < timeout_s; ++i) {

            ProgramOutput po = getStatus(jobInstance, port);

            if (isStatus(po.getReturnCode(), batchStatuses)) {
                return parseJobExecutionMessage(po.getStdout());
            } else {
                Thread.sleep(1 * 1000);
            }
        }

        return null;
    }

    /**
     * Poll status every second for a max timeout_s seconds waiting for job to get to one
     * of the given statuses
     * 
     * @return the latest jobexecution record, or null if the desired status was never achieved.
     */
    public JobExecution waitForStatus(int timeout_s,
                                      JobInstance jobInstance,
                                      BatchStatus... batchStatuses) throws Exception {
        for (int i = 0; i < timeout_s; ++i) {

            ProgramOutput po = getStatus(jobInstance);

            if (isStatus(po.getReturnCode(), batchStatuses)) {
                return parseJobExecutionMessage(po.getStdout());
            } else {
                Thread.sleep(1 * 1000);
            }
        }

        return null;
    }

    /**
     * @return true if JobInstance has any one of the given batchStatuses.
     */
    public boolean isStatus(JobInstance jobInstance, BatchStatus... batchStatuses) throws Exception {
        ProgramOutput po = getStatus(jobInstance);
        return isStatus(po.getReturnCode(), batchStatuses);
    }

    /**
     * @return true if the given batchManager status RC matches one of the given statuses.
     */
    public static boolean isStatus(int batchManagerStatusRC, BatchStatus... batchStatuses) {
        for (BatchStatus batchStatus : batchStatuses) {
            if (batchManagerStatusRC == getReturnCodeForBatchStatus(batchStatus)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if compareMe is equal to one of toUs.
     */
    public static boolean isBatchStatusEqual(BatchStatus compareMe, BatchStatus... toUs) {
        for (BatchStatus batchStatus : toUs) {
            if (compareMe == batchStatus) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return 30 + batchStatus.ordinal()
     */
    public static int getReturnCodeForBatchStatus(BatchStatus batchStatus) {
        return batchStatus.ordinal() + 30;
    }

    /**
     * helper for simple logging.
     */
    public static void log(String method, String msg) {
        Log.info(BatchManagerCliUtils.class, method, msg);
    }
    
    /**
     * @return the shell environment (JVM_ARGS, JAVA_HOME) for invoking the utility.
     */
    public Properties buildEnv() {
        return buildEnv( new String[] {} );
    }

    /**
     * @param args[] the command args. if --trustSslCertificates is specified, then trustStore is NOT set.
     * 
     * @return the shell environment (JVM_ARGS, JAVA_HOME) for invoking the utility.
     */
    public Properties buildEnv(String[] args) {

        Properties retMe = (Arrays.asList(args).contains("--trustSslCertificates"))
                        ? new Properties()
                        : BatchManagerCliUtils.buildJvmArgs(Arrays.asList(buildTrustStoreArg(),
                                                                          "-Dsun.security.ssl.allowUnsafeRenegotiation=true")); // fix for 147811 (hopefully)

        retMe.setProperty("JAVA_HOME", System.getProperty("java.home"));

        log("buildEnv", "properties=" + retMe);
        return retMe;
    }

    /**
     * Build the shell environment jvm args (JVM_ARGS="") for invoking the utility.
     * The given colleciton of args is joined with a ' ' and set into JVM_ARGS
     */
    public static Properties buildJvmArgs(Collection<String> args) {

        // JVM -D args are passed thru the utility's shell script via the JVM_ARGS env var.
        Properties env = new Properties();
        env.setProperty("JVM_ARGS", StringUtils.join(args, " "));

        return env;
    }

    /**
     * @return "-Djavax.net.ssl.trustStore=" + trustStoreFile;
     */
    public String buildTrustStoreArg() {
    	String keystorePath = StringUtils.join(Arrays.asList(server.getServerRoot(),
    			"resources",
    			"security",
    			"key.p12"),
    			File.separator);

    	String keystorePass = "Liberty";

    	return "-Djavax.net.ssl.trustStore=" + keystorePath 
    			+ " -Djavax.net.ssl.trustStorePassword=" + keystorePass
    			+ " -Djavax.net.ssl.trustStoreType=PKCS12";
    }

    /**
     * @return the https host:port of the given server
     */
    public String getHostAndPort() {
        return server.getHostname() + ":" + server.getHttpDefaultSecurePort();
    }

    /**
     * Asserts that actualJobName is equal to one of two things:
     *  1) the expectedJobName parm
     *      or 
     *  2) the not set value of "" (empty string)
     * 
     * @param expectedJobName
     * @param actualJobName
     */
    public void assertJobNamePossiblySet(String expectedJobName, String actualJobName) {
        // I'm sure there's a more elegant JUnit API here.
        if (!actualJobName.equals(JOB_NAME_NOT_SET)) {
            assertEquals(expectedJobName, actualJobName);
        }
    }
    
}
