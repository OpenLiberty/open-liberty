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
import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class PartitionMetricsTest extends BatchFATHelper {

    private static final Class testClass = PartitionMetricsTest.class;

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
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "java.lang.RuntimeException"
    })
    public void testMetricsRerunStep() throws Exception {
        test("PartitionMetrics", "testName=testMetricsRerunStep");
    }

    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "java.lang.RuntimeException"
    })
    public void testPartitionMetrics() throws Exception {
        test("PartitionMetrics", "testName=testPartitionMetrics");
    }

    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "java.lang.RuntimeException"
    })
    public void testNestedSplitFlowPartitionMetrics() throws Exception {
        test("PartitionMetrics", "testName=testNestedSplitFlowPartitionMetrics");
    }

    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "java.lang.RuntimeException"
    })
    public void testPartitionedRollbackMetric() throws Exception {
        test("PartitionMetrics", "testName=testPartitionedRollbackMetric");
    }

}
