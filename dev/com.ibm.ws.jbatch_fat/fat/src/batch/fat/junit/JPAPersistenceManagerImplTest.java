/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package batch.fat.junit;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JPAPersistenceManagerImplTest extends BatchFATHelper {

    public final static String DEFAULT_DDL = "IdPersistence/batch-jpa-default.ddl";
    public final static String NEGATIVE_INSTANCE_DDL = "IdPersistence/batch-jpa-negative-instance.ddl";
    public final static String ZERO_INSTANCE_DDL = "IdPersistence/batch-jpa-zero-instance.ddl";
    public final static String NEGATIVE_EXECUTION_DDL = "IdPersistence/batch-jpa-negative-execution.ddl";
    public final static String ZERO_EXECUTION_DDL = "IdPersistence/batch-jpa-zero-execution.ddl";
    public final static String NEGATIVE_STEPEXECUTION_DDL = "IdPersistence/batch-jpa-negative-stepexecution.ddl";
    public final static String ZERO_STEPEXECUTION_DDL = "IdPersistence/batch-jpa-zero-stepexecution.ddl";

    public final static String DFLT_SERVER_XML = "IdPersistence/server.xml";

    @BeforeClass
    public static void setup() throws Exception {

        BatchFATHelper.setConfig(DFLT_SERVER_XML, JPAPersistenceManagerImplTest.class);

        BatchFATHelper.startServer(server, JPAPersistenceManagerImplTest.class);

        FatUtils.waitForSmarterPlanet(server);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKY0011W", "CWWKY0036E", "CWWKY0037E", "CWWKY0038E");
        }
    }

    /*
     * Note: This test should FAIL early because of invalid job instance id.
     */
    @Test(expected = IOException.class)
    @ExpectedFFDC("com.ibm.jbatch.container.exception.PersistenceException")
    public void testNegativePersistedInstanceIdFails() throws Exception {
        createRuntimeTables(NEGATIVE_INSTANCE_DDL);
        test("Basic", "jslName=BasicPersistence");
    }

    /*
     * Note: This test should FAIL early because of invalid job instance id.
     */
    @Test(expected = IOException.class)
    @ExpectedFFDC("com.ibm.jbatch.container.exception.PersistenceException")
    public void testZeroPersistedInstanceIdFails() throws Exception {
        createRuntimeTables(ZERO_INSTANCE_DDL);
        test("Basic", "jslName=BasicPersistence");
    }

    /*
     * Note: This test should FAIL early because of invalid execution id.
     */
    @Test(expected = IOException.class)
    @ExpectedFFDC("com.ibm.jbatch.container.exception.PersistenceException")
    public void testPositivePersistedInstanceIdNegativeExecutionIdFails() throws Exception {
        createRuntimeTables(NEGATIVE_EXECUTION_DDL);
        test("Basic", "jslName=BasicPersistence");
    }

    /*
     * Note: This test should FAIL early because of invalid execution id.
     */
    @Test(expected = IOException.class)
    @ExpectedFFDC("com.ibm.jbatch.container.exception.PersistenceException")
    public void testPositivePersistedInstanceIdZeroExecutionIdFails() throws Exception {
        createRuntimeTables(ZERO_EXECUTION_DDL);
        test("Basic", "jslName=BasicPersistence");
    }

    /*
     * Note: This test will reach job execution, but should FAIL out because of invalid step execution id.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.PersistenceException", "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testPositivePersistedInstanceIdPositiveExecutionIdNegativeStepThreadExecutionId() throws Exception {
        createRuntimeTables(NEGATIVE_STEPEXECUTION_DDL);
        test("Basic", "jslName=BasicPersistence", FAILED_MESSAGE);
    }

    /*
     * Note: This test will reach job execution, but should FAIL out because of invalid step execution id.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.PersistenceException", "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testPositivePersistedInstanceIdPositiveExecutionIdZeroStepThreadExecutionId() throws Exception {
        createRuntimeTables(ZERO_STEPEXECUTION_DDL);
        test("Basic", "jslName=BasicPersistence", FAILED_MESSAGE);
    }

    /*
     * Note: This test should pass because database generated positive job, execution, and step id.
     */
    @Test
    public void testPositivePersistedInstanceIds() throws Exception {
        createRuntimeTables(DEFAULT_DDL);
        test("Basic", "jslName=BasicPersistence");
    }

    protected static void createRuntimeTables(String ddl) throws Exception {
        loadAndExecuteSql(DFLT_PERSISTENCE_JNDI,
                          ddl,
                          DFLT_PERSISTENCE_SCHEMA,
                          DFLT_TABLE_PREFIX);
    }

}
