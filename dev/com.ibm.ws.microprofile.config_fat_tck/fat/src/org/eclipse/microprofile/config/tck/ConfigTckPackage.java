/**
 *
 */
package org.eclipse.microprofile.config.tck;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.PortType;

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class ConfigTckPackage {

    @Server("FATServer")
    public static LibertyServer server;
    private String className;
    private String packageName;
    private File home;
    private String wlp;
    private File tckRunnerDir;
    private boolean init;
    private String mvnCliRoot[];
    private String[] mvnCliMethodRoot;
    private String[] mvnCliClassRoot;
    private String[] mvnCliPackageRoot;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testCustomConfigSourceProvider() throws Exception {
        samePacakgeInTck();
    }

    /**
     * @param methodName
     * @return
     * @throws Exception
     */
    private int samePacakgeInTck() throws Exception {
        if (!init) {
            init();
        }
        File mvnOutput = new File(home, "mvnOut_" + packageName);
        int rc = runCmd(mvnCliPackageRoot, tckRunnerDir, mvnOutput);
        return rc;
    }

    /**
     * @param cmd
     * @param workingDirectory TODO
     * @param outputFile TODO
     * @return
     * @throws Exception
     */
    private int runCmd(String[] cmd, File workingDirectory, File outputFile) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(cmd);
        System.out.println("GDH cmd is:" + Arrays.asList(cmd));
        pb.directory(workingDirectory);
        pb.redirectOutput(outputFile);
        Process p = pb.start();
        int exitCode = p.waitFor();
        return exitCode;
    }

    public String[] concatStringArray(String[] a, String[] b) {
        Stream<String> streamA = Arrays.stream(a);
        Stream<String> streamB = Arrays.stream(b);
        return Stream.concat(streamA, streamB).toArray(String[]::new);
    }

    public void init() throws Exception {
        className = this.getClass().getName();
        packageName = this.getClass().getPackage().getName();
        home = new File(System.getProperty("user.dir"));
        // wlpHome = System.getProperty("wlp.install.dir"); //System.getProperty("liberty.location");
        wlp = server.getInstallRoot();
        mvnCliRoot = new String[] { "mvn", "clean", "test", "-Dwlp=" + wlp, "-Dtck_server=" + server.getServerName(),
                                    "-Dtck_port=" + server.getPort(PortType.WC_defaulthost), "-DpackageName=" + packageName, "-DclassName=" + className };
        mvnCliMethodRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=method.xml" });
        mvnCliClassRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=class.xml" });
        mvnCliPackageRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=package.xml" });
        tckRunnerDir = new File("publish/tckRunner");
        init = true;
    }

}
