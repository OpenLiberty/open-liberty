/**
 *
 */
package org.eclipse.microprofile.config.tck;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.junit.AfterClass;
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

//    <systemPath>${api.stable}com.ibm.websphere.org.eclipse.microprofile.config.${mpconfig.version}_${mpconfig.bundle.version}.${version.qualifier}.jar</systemPath>
//    <systemPath>${lib}com.ibm.ws.microprofile.config_${liberty.version}.jar</systemPath>
//    <systemPath>${lib}com.ibm.ws.microprofile.config.cdi_${liberty.version}.jar</systemPath>

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
        System.out.print("GDH FIND");
        int rc = Utils.runCmd(Utils.mvnCliTckRoot, Utils.tckRunnerDir, mvnOutput);
    }

}
