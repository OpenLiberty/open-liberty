package batch.fat.junit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.FatUtils;
import com.ibm.ws.jbatch.test.BatchAppUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 *
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class CDITestCheckpoint extends BatchFATHelper {

    private static final Class testClass = CDITestCheckpoint.class;

    @BeforeClass
    public static void setup() throws Exception {

        server = LibertyServerFactory.getLibertyServer("checkpointbatchFAT");
        // Just happens to be a config that works we could reuse.  Could rename.
        BatchFATHelper.setConfig("CDITestCheckpoint/server.xml", testClass);

        BatchAppUtils.addDropinsBatchFATWar(server);
        
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true, null);
        BatchFATHelper.startServer(server, testClass);
        FatUtils.waitForSmarterPlanet(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKY0041W");
        }
    }

    @Test
    public void testSelfValidatingJobWithCDIJobListener() throws Exception {
        // Self-validating (if job completes, test passes).
        test("Basic", "jslName=CDIJobListener");
    }
}
