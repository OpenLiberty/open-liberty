package batch.fat.junit;

import java.util.concurrent.Callable;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import batch.fat.util.BatchFATHelper;

import com.ibm.ws.jbatch.test.BatchAppUtils;

import componenttest.custom.junit.runner.FATRunner;

/**
 *
 * Test dynamic updates to JDBC config using various schemas and tablePrefixes.
 *
 */
@RunWith(FATRunner.class)
public class JdbcConfigTest extends BatchFATHelper {

    /**
     * Used with DynamicConfigRule.
     */
    public static Callable<Void> initialSetup = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
            setup();
            return null;
        }
    };

    /**
     * Used with DynamicConfigRule.
     */
    public static Callable<Void> finalTearDown = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
            tearDown();
            return null;
        }
    };

    /**
     * Used with DynamicConfigRule. Called after each iteration.
     */
    public static Callable<Void> afterEach = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
            afterEach();
            return null;
        }
    };

    /**
     * This ClassRule will run all the tests in this class multiple times, against
     * all the given server.xml configuration files.
     *
     * Use setInitialSetup/setFinalTearDown to run logic before/after ALL
     * tests (against all configurations) have run.
     */
    @ClassRule
    public static DynamicConfigRule dynamicConfigRule = new DynamicConfigRule().setServer(server).setInitialSetup(initialSetup).setFinalTearDown(finalTearDown).setAfterEach(afterEach).addServerXml("JDBCPersistence/jdbc.config.myschema1.server.xml").addServerXml("JDBCPersistence/jdbc.config.myschema2.server.xml").addServerXml("JDBCPersistence/jdbc.config.myschema1.tp1.server.xml").addServerXml("JDBCPersistence/jdbc.config.myschema1.tp2.server.xml");

    /**
     * Start the server and setup the DB.
     */
    public static void setup() throws Exception {

        log("setup", "start server and execute DDLs");

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        // Start server
        server.setServerConfigurationFile("JDBCPersistence/jdbc.config.myschema1.server.xml");
        server.startServer("JdbcConfig.log");
        server.waitForStringInLog("CWWKF0011I", 20000);

        // Setup chunk test data
        executeSql("jdbc/batch", getChunkInTableSql());
        executeSql("jdbc/batch", getChunkOutTableSql("APP.OUT4"));

        executeSql("jdbc/myds", getChunkInTableSql());
        executeSql("jdbc/myds", getChunkOutTableSql("APP.OUT1"));
        executeSql("jdbc/myds", getChunkOutTableSql("APP.OUT2"));

        executeSql("jdbc/mydsNonTran", getChunkInTableSql());
        executeSql("jdbc/mydsNonTran", getChunkOutTableSql("APP.OUT3"));
    }

    /**
     * Stop the server.
     */
    public static void tearDown() throws Exception {
        log("tearDown", "stopping server");
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Clear out the OUT4 table used by the chunk tests.
     */
    public static void afterEach() throws Exception {
        log("afterEach", "");
        executeSql(DFLT_PERSISTENCE_JNDI, "DELETE FROM APP.OUT4;");
    }

    /**
     * Test a simple batchlet.
     */
    @Test
    public void testBasicJDBCPersistence() throws Exception {
        test("Basic", "jslName=BasicPersistence");
    }

    /**
     * Chunk test using the same datasource as the batch tables.
     */
    @Test
    public void testSharedResourceMultiChunk() throws Exception {
        test("Chunk", "jslName=ChunkTestMultipleCheckpoint&writeTable=APP.OUT4&sharedDB=true");
    }

    /**
     * Log helper.
     */
    public static void log(String method, String msg) {
        Log.info(JdbcConfigTest.class, method, msg);
    }

}
