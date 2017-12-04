/**
 *
 */
package org.eclipse.microprofile.config.tck;

import java.io.File;
import java.lang.invoke.MethodHandles;

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
 *
 */
@RunWith(FATRunner.class)
public class ConfigTckPackageTest {

    @Server("FATServer")
    public static LibertyServer server;

    String[] jarsUsed = { "com.ibm.websphere.org.eclipse.microprofile.config",
                          "com.ibm.ws.microprofile.config",
                          "com.ibm.ws.microprofile.config.cdi" };

    private static String className;
    private static String packageName;
    static {
        Class<?> clazz = MethodHandles.lookup().lookupClass();
        className = clazz.getName();
        packageName = clazz.getPackage().getName();
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E");
    }

    @Test
    @AllowedFFDC
    public void testTck() throws Exception {
        if (!Utils.init) {
            Utils.init(server);
        }
        File mvnOutput = new File(Utils.home, "mvnOut_TCK");
        int rc = Utils.runCmd(Utils.mvnCliTckRoot, Utils.tckRunnerDir, mvnOutput);
        // mvn returns 0 is all surefire tests and pass 1 on failure
        Assert.assertTrue("TCK has returned non-zero return code of: " + rc +
                          "This idicates test failure, see: ...autoFVT/mvnOut_TCK " +
                          "and ...autoFVT/publish/tckRunner/tck/target/surefire-reports/index.html", rc == 0);
    }

}
