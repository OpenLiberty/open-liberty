package batch.fat.junit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import batch.fat.util.BatchFATHelper;
import componenttest.custom.junit.runner.FATRunner;

import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.FatUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class ChunkTest extends BatchFATHelper {

    private static final Class testClass = ChunkTest.class;

    @BeforeClass
    public static void setup() throws Exception {
        BatchFATHelper.setConfig("ChunkTest/server.xml", testClass);

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        BatchFATHelper.startServer(server, testClass);
        FatUtils.waitForSmarterPlanet(server);

        // Setup batch tables.
        createDefaultRuntimeTables();

        // Setup chunk test data
        executeSql("jdbc/batch", getChunkInTableSql());
        executeSql("jdbc/batch", getChunkOutTableSql("APP.OUT4"));

        executeSql("jdbc/myds", getChunkInTableSql());
        executeSql("jdbc/myds", getChunkOutTableSql("APP.OUT1"));
        executeSql("jdbc/myds", getChunkOutTableSql("APP.OUT2"));

        executeSql("jdbc/mydsNonTran", getChunkInTableSql());
        executeSql("jdbc/mydsNonTran", getChunkOutTableSql("APP.OUT3"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Chunk test using the same datasource as the batch tables.
     */
    @Test
    public void testSharedResourceMultiChunk() throws Exception {
        test("Chunk", "jslName=ChunkTestMultipleCheckpoint&writeTable=APP.OUT4&sharedDB=true");
    }

    /**
     * Chunk test, one checkpoint.
     */
    @Test
    public void testChunkTestOneCheckpoint() throws Exception {
        test("Chunk", "jslName=ChunkTestOneCheckpoint&writeTable=APP.OUT1");
    }

    /**
     * Chunk test, multiple checkpoints.
     */
    @Test
    public void testChunkGlobalTranMultiChunk() throws Exception {
        test("Chunk", "jslName=ChunkTestMultipleCheckpoint&writeTable=APP.OUT2");
    }

    /**
     *
     */
    @Test
    public void testChunkGlobalTranCursorHold() throws Exception {
        test("Chunk", "jslName=ChunkTestGlobalTranCursorHold&writeTable=APP.OUT3&dataSourceJndi=jdbc/mydsNonTran");
    }

    /**
    *
    */
    @Test
    public void testChunkStop() throws Exception {
        test("ChunkStopServlet", "jslName=chunkStopTest&testName=chunkStop");
    }

}
