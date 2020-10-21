/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package batch.fat.junit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 7)
public class TranTimeoutTest extends BatchFATHelper {

    private static final Class testClass = TranTimeoutTest.class;

    @BeforeClass
    public static void setup() throws Exception {
        BatchFATHelper.setConfig(DFLT_SERVER_XML, testClass);
        BatchFATHelper.startServer(server, testClass);
        FatUtils.waitForSmarterPlanet(server);

        createDefaultRuntimeTables();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKY0011W");
        }
    }

    @Test
    @ExpectedFFDC({ "javax.persistence.PersistenceException", "javax.transaction.RollbackException",
                    "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testJSLTransactionTimeoutStep1Fail() throws Exception {
        test("TranTimeout", "jslName=ChunkTranTimeout&variation=1"); // Doesn't use jslName actually
    }

    @Test
    @ExpectedFFDC({ "javax.persistence.PersistenceException", "javax.transaction.RollbackException",
                    "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testJSLTransactionTimeoutStep2Fail() throws Exception {
        test("TranTimeout", "jslName=ChunkTranTimeout&variation=2"); // Doesn't use jslName actually
    }

    @Test
    public void testJSLTransactionTimeoutComplete() throws Exception {
        test("TranTimeout", "jslName=ChunkTranTimeout&variation=3"); // Doesn't use jslName actually
    }
}
