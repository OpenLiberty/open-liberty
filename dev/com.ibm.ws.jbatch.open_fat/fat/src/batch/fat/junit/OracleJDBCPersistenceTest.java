package batch.fat.junit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import batch.fat.util.BatchFATHelper;

import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Note: this test is currently not included in the FATSuite. Before it can
 * be added permanently we need to have a permanent oracle DB we can use.
 * Until then, this class and its associated server config (oracle.server.xml)
 * serve as examples of configuring and testing against oracle.
 */
@RunWith(FATRunner.class)
public class OracleJDBCPersistenceTest extends BatchFATHelper {

    @BeforeClass
    public static void setup() throws Exception {
        BatchFATHelper.setConfig("JDBCPersistence/oracle.server.xml", OracleJDBCPersistenceTest.class);

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        BatchFATHelper.startServer(server, OracleJDBCPersistenceTest.class);
        FatUtils.waitForSmarterPlanet(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test a simple batchlet.
     */
    @Test
    public void testBasicJDBCPersistence() throws Exception {
        test("Basic", "jslName=BasicPersistence");
    }

    /**
     * Chunk test, one checkpoint.
     */
    @Test
    public void testChunkTestOneCheckpoint() throws Exception {
        test("Chunk", "jslName=ChunkTestOneCheckpoint&writeTable=OUT1");
    }

    /**
     * Chunk test, multiple checkpoints.
     */
    @Test
    public void testChunkGlobalTranMultiChunk() throws Exception {
        test("Chunk", "jslName=ChunkTestMultipleCheckpoint&writeTable=OUT2");
    }

    /**
     * 
     */
    @Test
    public void testChunkGlobalTranCursorHold() throws Exception {
        test("Chunk", "jslName=ChunkTestGlobalTranCursorHold&writeTable=OUT3&dataSourceJndi=jdbc/mydsNonTran");
    }

    /**
     *
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException",
                   "javax.resource.ResourceException",
                   "javax.transaction.RollbackException",
                   "com.ibm.jbatch.container.exception.PersistenceException",
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "com.ibm.jbatch.container.exception.BatchContainerServiceException" })
    public void testJSLTransactionTimeoutStep1Fail() throws Exception {
        test("TranTimeout", "jslName=ChunkTranTimeout&variation=1"); // Doesn't use jslName actually
    }

    /**
     * 
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException",
                   "javax.resource.ResourceException",
                   "javax.transaction.RollbackException",
                   "com.ibm.jbatch.container.exception.PersistenceException",
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "com.ibm.jbatch.container.exception.BatchContainerServiceException" })
    public void testJSLTransactionTimeoutStep2Fail() throws Exception {
        test("TranTimeout", "jslName=ChunkTranTimeout&variation=2"); // Doesn't use jslName actually
    }

    /**
     * 
     */
    @Test
    public void testJSLTransactionTimeoutComplete() throws Exception {
        test("TranTimeout", "jslName=ChunkTranTimeout&variation=3"); // Doesn't use jslName actually
    }

}
