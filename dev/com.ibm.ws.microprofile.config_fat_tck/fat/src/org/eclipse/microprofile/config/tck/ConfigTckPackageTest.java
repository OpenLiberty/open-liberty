/**
 *
 */
package org.eclipse.microprofile.config.tck;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.microprofile.tck.Utils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class ConfigTckPackageTest {

    @Server("FATServer")
    public static LibertyServer server;

    // These are the jar names subsets that are devoid of specific version numbers
    String[] jarsUsed = { "com.ibm.websphere.org.eclipse.microprofile.config",
                          "com.ibm.ws.microprofile.config",
                          "com.ibm.ws.microprofile.config.cdi" };

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E");
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void testTck() throws Exception {
        if (!Utils.init) {
            Utils.init(server);
        }
        // Everything under autoFVT/results is collected from the child build machine
        File mvnOutput = new File(Utils.home, "results/mvnOutput_TCK");
        int rc = Utils.runCmd(Utils.mvnCliTckRoot, Utils.tckRunnerDir, mvnOutput);
        // mvn returns 0 is all surefire tests and pass 1 on failure
        Assert.assertTrue("com.ibm.ws.microprofile.config_fat_tck:org.eclipse.microprofile.config.tck.ConfigTckPackageTest:testTck:TCK has returned non-zero return code of: " + rc
                          +
                          " This indicates test failure, see: ...autoFVT/results/mvn* " +
                          "and ...autoFVT/results/tck/surefire-reports/index.html", rc == 0);
    }

}
