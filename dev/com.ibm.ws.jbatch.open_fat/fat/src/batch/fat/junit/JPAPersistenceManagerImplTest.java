/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.BatchRestUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

@Mode(TestMode.FULL)
public class JPAPersistenceManagerImplTest extends BatchFATHelper {

    public final static String DEFAULT_DDL = "IdPersistence/batch-jpa-default.ddl";
    public final static String NEGATIVE_INSTANCE_DDL = "IdPersistence/batch-jpa-negative-instance.ddl";
    public final static String ZERO_INSTANCE_DDL = "IdPersistence/batch-jpa-zero-instance.ddl";
    public final static String NEGATIVE_EXECUTION_DDL = "IdPersistence/batch-jpa-negative-execution.ddl";
    public final static String ZERO_EXECUTION_DDL = "IdPersistence/batch-jpa-zero-execution.ddl";
    public final static String NEGATIVE_STEPEXECUTION_DDL = "IdPersistence/batch-jpa-negative-stepexecution.ddl";
    public final static String ZERO_STEPEXECUTION_DDL = "IdPersistence/batch-jpa-zero-stepexecution.ddl";

    //public final static String DFLT_SERVER_XML = "IdPersistence/server.xml";

    public static String BATCH_V4_URL = "/ibm/api/batch/v4/";

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.fat.jpa.ddl.test");
    private static final BatchRestUtils batchRestUtils = new BatchRestUtils(server);

    protected final static String ADMIN_NAME = "bob";
    protected final static String ADMIN_PASSWORD = "bobpwd";
    private final static String USER_NAME = "jane";
    private final static String USER_PASSWORD = "janepwd";

    private final Map<String, String> adminHeaderMap, headerMap;

    public JPAPersistenceManagerImplTest() {
        adminHeaderMap = Collections.singletonMap("Authorization", "Basic " + Base64Coder.base64Encode(ADMIN_NAME + ":" + ADMIN_PASSWORD));
        headerMap = Collections.singletonMap("Authorization", "Basic " + Base64Coder.base64Encode(USER_NAME + ":" + USER_PASSWORD));
    }

    @BeforeClass
    public static void setup() throws Exception {

        //server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.fat.jpa.persistence");

        //BatchFATHelper.setConfig(DFLT_SERVER_XML, JPAPersistenceManagerImplTest.class);

        HttpUtils.trustAllCertificates();
        
        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        BatchFATHelper.startServer(server, JPAPersistenceManagerImplTest.class);

        //FatUtils.waitForSmarterPlanet(server);
        FatUtils.waitForStartupAndSsl(server);
        FatUtils.waitForLTPA(server);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKY0038E", "CWWKY0011W", "CWWKY0037E", "CWWKY0036E", "WTRN0074E");
        }
    }

    // These used to be in separate tests, but they were running into oddball issues where the tests would fail if the tables couldn't be modified due to timing
    // So just do these here one by one so we can control when things happen to avoid system timing issues
    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.PersistenceException",
                    "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testInstanceAndExecutionIdVariants() throws Exception {

        List<String> failedTests = new ArrayList<String>();

        // *****************************************
        // Test 1: Default test with valid id values
        // *****************************************
        log("testInstanceAndExecutionIdVariants", "====TESTING DEFAULT====");
        createRuntimeTables(DEFAULT_DDL);
        JsonObject response = batchRestUtils.submitJobAndWaitUntilFinished("batchFAT", "BasicPersistence", BATCH_V4_URL);
        long jobInstanceId = response.getJsonNumber("instanceId").longValue();
        log("testInstanceAndExecutionIdVariants", " Instance id = " + jobInstanceId);
        log("testInstanceAndExecutionIdVariants", "batchStatus=" + response.getString("batchStatus"));

        if (!response.getString("batchStatus").equals("COMPLETED")) {
            log("testInstanceAndExecutionIdVariants", "testPositivePersistedInstanceIds FAILED: batchStatus=" + response.getString("batchStatus"));
            failedTests.add("testPositivePersistedInstanceIds");
        } else {
            log("testInstanceAndExecutionIdVariants", "testPositivePersistedInstanceIds PASSED: batchStatus=" + response.getString("batchStatus"));
        }

        // ************************************
        // Test 2: Zero step execution id value
        // ************************************
        log("testInstanceAndExecutionIdVariants", "====TESTING ZERO STEP EXECUTION====");
        loadAndExecuteSql(DFLT_PERSISTENCE_JNDI,
                          "IdPersistence/alter-stepexecution-zero.ddl",
                          DFLT_PERSISTENCE_SCHEMA,
                          DFLT_TABLE_PREFIX);

        response = batchRestUtils.submitJobAndWaitUntilFinished("batchFAT", "BasicPersistence", BATCH_V4_URL);

        List<String> errors = server.findStringsInLogs("CWWKY0038E: Invalid step execution id value 0");

        if (errors != null && !errors.isEmpty() && errors.get(0) != null) {
            log("testInstanceAndExecutionIdVariants",
                "testPositivePersistedInstanceIdPositiveExecutionIdZeroStepThreadExecutionId PASSED: Found expected error for zero step execution id");
        } else {
            log("testInstanceAndExecutionIdVariants",
                "testPositivePersistedInstanceIdPositiveExecutionIdZeroStepThreadExecutionId FAILED: Did not find expected error for zero step execution id");
            failedTests.add("testPositivePersistedInstanceIdPositiveExecutionIdZeroStepThreadExecutionId,");
        }

        // ****************************************
        // Test 3: Negative step execution id value
        // ****************************************
        log("testInstanceAndExecutionIdVariants", "====TESTING NEGATIVE STEP EXECUTION====");
        loadAndExecuteSql(DFLT_PERSISTENCE_JNDI,
                          "IdPersistence/alter-stepexecution-negative.ddl",
                          DFLT_PERSISTENCE_SCHEMA,
                          DFLT_TABLE_PREFIX);

        response = batchRestUtils.submitJobAndWaitUntilFinished("batchFAT", "BasicPersistence", BATCH_V4_URL);

        errors = server.findStringsInLogs("CWWKY0038E: Invalid step execution id value -1");

        if (errors != null && !errors.isEmpty() && errors.get(0) != null) {
            log("testInstanceAndExecutionIdVariants",
                "testPositivePersistedInstanceIdPositiveExecutionIdNegativeStepThreadExecutionId PASSED: Found expected error for negative step execution id");
        } else {
            log("testInstanceAndExecutionIdVariants",
                "testPositivePersistedInstanceIdPositiveExecutionIdNegativeStepThreadExecutionId FAILED: Did not find expected error for negative step execution id");
            failedTests.add("testPositivePersistedInstanceIdPositiveExecutionIdNegativeStepThreadExecutionId");
        }

        // Reset values back to valid starting points
        loadAndExecuteSql(DFLT_PERSISTENCE_JNDI,
                          "IdPersistence/alter-tables-default.ddl",
                          DFLT_PERSISTENCE_SCHEMA,
                          DFLT_TABLE_PREFIX);

        // ***********************************
        // Test 4: Zero job execution id value
        // ***********************************
        log("testInstanceAndExecutionIdVariants", "====TESTING ZERO EXECUTION ID====");
        loadAndExecuteSql(DFLT_PERSISTENCE_JNDI,
                          "IdPersistence/alter-jobexecution-zero.ddl",
                          DFLT_PERSISTENCE_SCHEMA,
                          DFLT_TABLE_PREFIX);

        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder().add("applicationName",
                                                                          "batchFAT").add("jobXMLName", "BasicPersistence");

        HttpURLConnection con = HttpUtils.getHttpConnection(batchRestUtils.buildURL(BATCH_V4_URL + "jobinstances"),
                                                            HttpURLConnection.HTTP_INTERNAL_ERROR, // Expecting HTTP 500 here
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.POST,
                                                            BatchRestUtils.buildHeaderMap(ADMIN_NAME, ADMIN_PASSWORD),
                                                            new ByteArrayInputStream(payloadBuilder.build().toString().getBytes("UTF-8")));

        errors = server.findStringsInLogs("CWWKY0037E: Invalid job execution id value 0");

        if (errors != null && !errors.isEmpty() && errors.get(0) != null) {
            log("testInstanceAndExecutionIdVariants",
                "testPositivePersistedInstanceIdZeroExecutionIdFails PASSED: Found expected error for zero job execution id");
        } else {
            log("testInstanceAndExecutionIdVariants",
                "testPositivePersistedInstanceIdZeroExecutionIdFails FAILED: Did not find expected error for zero job execution id");
            failedTests.add("testPositivePersistedInstanceIdZeroExecutionIdFails");
        }

        // ***************************************
        // Test 5: Negative job execution id value
        // ****************************************
        log("testInstanceAndExecutionIdVariants", "====TESTING NEGATIVE EXECUTION ID====");
        loadAndExecuteSql(DFLT_PERSISTENCE_JNDI,
                          "IdPersistence/alter-jobexecution-negative.ddl",
                          DFLT_PERSISTENCE_SCHEMA,
                          DFLT_TABLE_PREFIX);

        payloadBuilder = Json.createObjectBuilder().add("applicationName",
                                                        "batchFAT").add("jobXMLName", "BasicPersistence");

        con = HttpUtils.getHttpConnection(batchRestUtils.buildURL(BATCH_V4_URL + "jobinstances"),
                                          HttpURLConnection.HTTP_INTERNAL_ERROR, // expecting http 500
                                          new int[0],
                                          10 * 1000,
                                          HTTPRequestMethod.POST,
                                          BatchRestUtils.buildHeaderMap(ADMIN_NAME, ADMIN_PASSWORD),
                                          new ByteArrayInputStream(payloadBuilder.build().toString().getBytes("UTF-8")));

        errors = server.findStringsInLogs("CWWKY0037E: Invalid job execution id value -1");

        if (errors != null && !errors.isEmpty() && errors.get(0) != null) {
            log("testInstanceAndExecutionIdVariants",
                "testPositivePersistedInstanceIdNegativeExecutionIdFails PASSED: Found expected error for negative job execution id");
        } else {
            log("testInstanceAndExecutionIdVariants",
                "testPositivePersistedInstanceIdNegativeExecutionIdFails FAILED: Did not find expected error for negative job execution id");
            failedTests.add("testPositivePersistedInstanceIdNegativeExecutionIdFails");
        }

        // Reset values back to valid starting points
        loadAndExecuteSql(DFLT_PERSISTENCE_JNDI,
                          "IdPersistence/alter-tables-default.ddl",
                          DFLT_PERSISTENCE_SCHEMA,
                          DFLT_TABLE_PREFIX);

        // **********************************
        // Test 6: Zero job instance id value
        // **********************************
        log("testInstanceAndExecutionIdVariants", "====TESTING ZERO INSTANCE ID====");
        loadAndExecuteSql(DFLT_PERSISTENCE_JNDI,
                          "IdPersistence/alter-instance-zero.ddl",
                          DFLT_PERSISTENCE_SCHEMA,
                          DFLT_TABLE_PREFIX);
        payloadBuilder = Json.createObjectBuilder().add("applicationName",
                                                        "batchFAT").add("jobXMLName", "BasicPersistence");

        con = HttpUtils.getHttpConnection(batchRestUtils.buildURL(BATCH_V4_URL + "jobinstances"),
                                          HttpURLConnection.HTTP_INTERNAL_ERROR, // expecting http 500
                                          new int[0],
                                          10 * 1000,
                                          HTTPRequestMethod.POST,
                                          BatchRestUtils.buildHeaderMap(ADMIN_NAME, ADMIN_PASSWORD),
                                          new ByteArrayInputStream(payloadBuilder.build().toString().getBytes("UTF-8")));

        errors = server.findStringsInLogs("CWWKY0036E: Invalid job instance id value 0");

        if (errors != null && !errors.isEmpty() && errors.get(0) != null) {
            log("testInstanceAndExecutionIdVariants",
                "testZeroPersistedInstanceIdFails PASSED: Found expected error for zero job instance id");
        } else {
            log("testInstanceAndExecutionIdVariants",
                "testZeroPersistedInstanceIdFails FAILED: Did not find expected error for zero job instance id");
            failedTests.add("testZeroPersistedInstanceIdFails");
        }

        // **********************************
        // Test 7: Negative job instance id value
        // **********************************
        log("testInstanceAndExecutionIdVariants", "====TESTING NEGATIVE INSTANCE ID====");
        loadAndExecuteSql(DFLT_PERSISTENCE_JNDI,
                          "IdPersistence/alter-instance-negative.ddl",
                          DFLT_PERSISTENCE_SCHEMA,
                          DFLT_TABLE_PREFIX);

        payloadBuilder = Json.createObjectBuilder().add("applicationName",
                                                        "batchFAT").add("jobXMLName", "BasicPersistence");

        con = HttpUtils.getHttpConnection(batchRestUtils.buildURL(BATCH_V4_URL + "jobinstances"),
                                          HttpURLConnection.HTTP_INTERNAL_ERROR, // expecting http 500
                                          new int[0],
                                          10 * 1000,
                                          HTTPRequestMethod.POST,
                                          BatchRestUtils.buildHeaderMap(ADMIN_NAME, ADMIN_PASSWORD),
                                          new ByteArrayInputStream(payloadBuilder.build().toString().getBytes("UTF-8")));

        errors = server.findStringsInLogs("CWWKY0036E: Invalid job instance id value -1");

        if (errors != null && !errors.isEmpty() && errors.get(0) != null) {
            log("testInstanceAndExecutionIdVariants",
                "testNegativePersistedInstanceIdFails PASSED: Found expected error for negative job instance id");
        } else {
            log("testInstanceAndExecutionIdVariants",
                "testNegativePersistedInstanceIdFails FAILED: Did not find expected error for negative job instance id");
            failedTests.add("testNegativePersistedInstanceIdFails");
        }

        if (failedTests.size() != 0) {
            String msg = "JPAPersistenceManagerImplTest has failures: ";
            for (int i = 0; i < failedTests.size(); i++) {
                msg += failedTests.get(i) + ", ";
            }
            throw new Exception(msg);
        }

    }

    /*
     * Note: This test should FAIL early because of invalid job instance id.
     */
    //@Test(expected = IOException.class)
    //@ExpectedFFDC("com.ibm.jbatch.container.exception.PersistenceException")
    public void testNegativePersistedInstanceIdFails() throws Exception {
        createRuntimeTables(NEGATIVE_INSTANCE_DDL);
        test("Basic", "jslName=BasicPersistence");
    }

    /*
     * Note: This test should FAIL early because of invalid job instance id.
     */
    //@Test(expected = IOException.class)
    //@ExpectedFFDC("com.ibm.jbatch.container.exception.PersistenceException")
    public void testZeroPersistedInstanceIdFails() throws Exception {
        createRuntimeTables(ZERO_INSTANCE_DDL);
        test("Basic", "jslName=BasicPersistence");
    }

    /*
     * Note: This test should FAIL early because of invalid execution id.
     */
    //@Test(expected = IOException.class)
    //@ExpectedFFDC("com.ibm.jbatch.container.exception.PersistenceException")
    public void testPositivePersistedInstanceIdNegativeExecutionIdFails() throws Exception {
        createRuntimeTables(NEGATIVE_EXECUTION_DDL);
        test("Basic", "jslName=BasicPersistence");
    }

    /*
     * Note: This test should FAIL early because of invalid execution id.
     */
    //@Test(expected = IOException.class)
    //@ExpectedFFDC("com.ibm.jbatch.container.exception.PersistenceException")
    public void testPositivePersistedInstanceIdZeroExecutionIdFails() throws Exception {
        createRuntimeTables(ZERO_EXECUTION_DDL);
        test("Basic", "jslName=BasicPersistence");
    }

    /*
     * Note: This test will reach job execution, but should FAIL out because of invalid step execution id.
     */
    //@Test
    //@ExpectedFFDC({ "com.ibm.jbatch.container.exception.PersistenceException", "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testPositivePersistedInstanceIdPositiveExecutionIdNegativeStepThreadExecutionId() throws Exception {
        createRuntimeTables(NEGATIVE_STEPEXECUTION_DDL);
        test("Basic", "jslName=BasicPersistence", FAILED_MESSAGE);
    }

    /*
     * Note: This test will reach job execution, but should FAIL out because of invalid step execution id.
     */
    //@Test
    //@ExpectedFFDC({ "com.ibm.jbatch.container.exception.PersistenceException", "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testPositivePersistedInstanceIdPositiveExecutionIdZeroStepThreadExecutionId() throws Exception {
        createRuntimeTables(ZERO_STEPEXECUTION_DDL);
        test("Basic", "jslName=BasicPersistence", FAILED_MESSAGE);
    }

    /*
     * Note: This test should pass because database generated positive job, execution, and step id.
     */
    // @Test
    public void testPositivePersistedInstanceIds() throws Exception {
        createRuntimeTables(DEFAULT_DDL);
        JsonObject response = batchRestUtils.submitJobAndWaitUntilFinished("batchFAT", "BasicPersistence", BATCH_V4_URL);
        long jobInstanceId = response.getJsonNumber("instanceId").longValue();
        log("testRESTGetStepExecutionPartitionData", " Instance id = " + jobInstanceId);
        assertEquals("COMPLETED", response.getString("batchStatus"));
        //test("Basic", "jslName=BasicPersistence");
    }

    /*
     * helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(JPAPersistenceManagerImplTest.class, method, msg);
    }

    protected static void createRuntimeTables(String ddl) throws Exception {
        loadAndExecuteSql(DFLT_PERSISTENCE_JNDI,
                          ddl,
                          DFLT_PERSISTENCE_SCHEMA,
                          DFLT_TABLE_PREFIX);
    }

}
