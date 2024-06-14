package batch.fat.junit;

import org.jboss.shrinkwrap.api.Archive;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import batch.fat.util.BatchFATHelper;

import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class BasicJDBCPersistenceTest extends BatchFATHelper {

    @BeforeClass
    public static void setup() throws Exception {

        BatchFATHelper.setConfig(DFLT_SERVER_XML, BasicJDBCPersistenceTest.class);

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        server.startServer();
        FatUtils.waitForSmarterPlanet(server);

        //wait for the security keys get generated.
        FatUtils.waitForLTPA(server);

        createDefaultRuntimeTables();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testBasicJDBCPersistence() throws Exception {
        test("Basic", "jslName=BasicPersistence");
    }

}
