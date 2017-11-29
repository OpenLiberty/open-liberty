/**
 *
 */
package com.ibm.ws.microprofile.tck;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import com.ibm.websphere.simplicity.PortType;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class Utils {

    public static File home;
    public static String wlp;
    public static File tckRunnerDir;
    public static boolean init;
    public static String mvnCliRoot[];
    public static String[] mvnCliTckRoot;
    public static String[] mvnCliMethodRoot;
    public static String[] mvnCliClassRoot;
    public static String[] mvnCliPackageRoot;

    public static void init(LibertyServer server) throws Exception {
        // wlpHome = System.getProperty("wlp.install.dir"); //System.getProperty("liberty.location");
        wlp = server.getInstallRoot();
        home = new File(System.getProperty("user.dir"));
        mvnCliRoot = new String[] { "mvn", "clean", "test", "-Dwlp=" + wlp, "-Dtck_server=" + server.getServerName(),
                                    "-Dtck_port=" + server.getPort(PortType.WC_defaulthost) };
        mvnCliTckRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=tck-suite.xml" });
        mvnCliMethodRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=method.xml" });
        mvnCliClassRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=class.xml" });
        mvnCliPackageRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=package.xml" });
        tckRunnerDir = new File("publish/tckRunner");
        init = true;
    }

    public static String[] concatStringArray(String[] a, String[] b) {
        Stream<String> streamA = Arrays.stream(a);
        Stream<String> streamB = Arrays.stream(b);
        return Stream.concat(streamA, streamB).toArray(String[]::new);
    }

    /**
     * @param cmd
     * @param workingDirectory TODO
     * @param outputFile TODO
     * @return
     * @throws Exception
     */
    public static int runCmd(String[] cmd, File workingDirectory, File outputFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workingDirectory);
        pb.redirectOutput(outputFile);
        Process p = pb.start();
        int exitCode = p.waitFor();
        return exitCode;
    }
}