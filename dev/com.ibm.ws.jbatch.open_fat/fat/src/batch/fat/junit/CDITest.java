package batch.fat.junit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.FatUtils;
import com.ibm.ws.jbatch.test.BatchAppUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.custom.junit.runner.FATRunner;

/**
 *
 */
@RunWith(FATRunner.class)
public class CDITest extends BatchFATHelper {

    private static final Class testClass = CDITest.class;

    @BeforeClass
    public static void setup() throws Exception {

        // Just happens to be a config that works we could reuse.  Could rename.
        BatchFATHelper.setConfig("BonusPayoutViaJobOperator/server.xml", testClass);

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

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
