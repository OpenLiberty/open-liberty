package batch.fat.junit;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class BasicJDBCPersistenceTest extends BatchFATHelper {

    @BeforeClass
    public static void setup() throws Exception {

        BatchFATHelper.setConfig(DFLT_SERVER_XML, BasicJDBCPersistenceTest.class);

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
