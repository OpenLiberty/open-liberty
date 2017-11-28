/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.microprofile.config.tck;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PortType;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that no @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
public class ConfigProviderTest {

    //public static final String APP_NAME = "app1";

    @Server("FATServer")
    //@TestServlet(servlet = TestServletA.class, contextRoot = APP_NAME)
    public static LibertyServer server;
    private String className;
    private String packageName;
    private File home;
    private String wlp;
    private File tckRunnerDir;
    private File mvnOutput;
    private boolean init;
    private String mvnCliRoot[];
    private Object mvnCliMethodRoot;
    private String[] mvnCliPackageRoot;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        //ShrinkHelper.defaultApp(server, APP_NAME, "app1.web");
        server.startServer();
    }
//
//    @AfterClass
//    public static void tearDown() throws Exception {
//        server.stopServer();
//    }
//
//    @Test
//    public void verifyArtifactoryDependency() throws Exception {
//        // Confirm that the example Artifactory dependency was download and is available on the classpath
//        org.apache.derby.drda.NetworkServerControl.class.getName();
//    }

//    @BeforeClass
//    public static void test() throws Exception {
//        File home = new File(System.getProperty("user.dir"));
//        // /libertyGit/open-libertydev/com.ibm.ws.microprofile.config_fat_tck/build/libs/autoFVT
//        File mvnOutput = new File(home, "mvnResults");
//        ProcessBuilder pb = new ProcessBuilder("mvn", "--version");
//        File tckRunnerDir = new File("publish/tckRunner");
//        pb.redirectOutput(mvnOutput);
//        pb.directory(tckRunnerDir);
//        Process p = pb.start();
//        int exitCode = p.waitFor();
//    }

//    @Test
//    public void testBuildTckRunner() throws Exception {
//        File home = new File(System.getProperty("user.dir"));
//        // /libertyGit/open-libertydev/com.ibm.ws.microprofile.config_fat_tck/build/libs/autoFVT
//
//        // Now do the below in the surefire config in the pom.xml file
//        String wlpHome = System.getProperty("wlp.install.dir");
//        System.out.println("GDH wlp.install.dir is: " + wlpHome);
//        ProcessBuilder pb = new ProcessBuilder("mvn", "test", "-DwlpHome=" + wlpHome, "-Dtck_server=" + server.getServerName(), "-Dtck_port="
//                                                                                                                                + server.getPort(PortType.WC_defaulthost));
//
//        File tckRunnerDir = new File("publish/tckRunner");
//        pb.directory(tckRunnerDir);
//
//        File mvnOutput = new File(home, "mvnTestResults");
//        pb.redirectOutput(mvnOutput);
//        Process p = pb.start();
//        int exitCode = p.waitFor();
//    }

//    @Test
//    public void testDynamicValueInPropertyConfigSourceLong() throws Exception {
//        String className = this.getClass().getName();
//        String packageName = this.getClass().getPackage().getName();
//
//        File home = new File(System.getProperty("user.dir"));
//        wlpHome = server.getInstallRoot();
//        System.out.println("GDH X wlpHome is:" + wlpHome);
//
//        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
//        //String[] cmd = (String[]) ArrayUtils.addAll(mvnCliRoot, new String[] { "-DmethodName=" + methodName });
//        ProcessBuilder pb = new ProcessBuilder("mvn", "test", "-DWLP=" + wlpHome, "-DsuiteXmlFile=method.xml", "-Dtck_server=" + server.getServerName(), "-Dtck_port=" + server
//                        .getPort(PortType.WC_defaulthost), "-DpackageName=" + packageName, "-DclassName=" + className, "-DmethodName=" + methodName);
//
//        File tckRunnerDir = new File("publish/tckRunner");
//        pb.directory(tckRunnerDir);
//
//        File mvnOutput = new File(home, "mvnTestResults");
//        pb.redirectOutput(mvnOutput);
//        Process p = pb.start();
//        int exitCode = p.waitFor();
//    }

    @Test
    public void testDynamicValueInPropertyConfigSource() throws Exception {
        sameTestMethodInTck(new Object() {}.getClass().getEnclosingMethod().getName());
    }

    /**
     * @param methodName
     * @throws Exception
     */
    private void sameTestMethodInTck(String methodName) throws Exception {
        if (!init) {
            init();
        }
        String[] methodParm = new String[] { "-DmethodName=" + methodName };
        String[] cmd = concatStringArray(mvnCliRoot, methodParm);
        int rc = runCmd(cmd, tckRunnerDir, mvnOutput);
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
        mvnCliPackageRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=package.xml" });
        tckRunnerDir = new File("publish/tckRunner");
        mvnOutput = new File(home, "mvnTestResults");
        init = true;
    }

//    @BeforeClass
//    public void verifyJavaCommandLine() throws Exception {
////        Runtime runtime = Runtime.getRuntime();
//        //      Process pr = runtime.exec("echo Hello World From Java CLI");
//        try {
//            Runtime rt = Runtime.getRuntime();
//            //Process pr = rt.exec("cmd /c dir");
//            Process pr = rt.exec("echo GDH");
//
//            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
//
//            String line = null;
//
//            while ((line = input.readLine()) != null) {
//                System.out.println(line);
//            }
//
//            int exitVal = pr.waitFor();
//            System.out.println("GDH Exited with error code " + exitVal);
//
//        } catch (Exception e) {
//            System.out.println(e.toString());
//            e.printStackTrace();
//        }
//
//    }
//
//    @AfterClass
//    public void verifyMavenAccessible() throws Exception {
//        Runtime runtime = Runtime.getRuntime();
//        Process pr = runtime.exec("mvn -version");
//    }

//    @Test
//    public void verifyStdOut() throws Exception {
//        try {
////            Runtime rt = Runtime.getRuntime();
////            //Process pr = rt.exec("cmd /c dir");
////            Process pr = rt.exec("");
//
//            File home = new File(System.getProperty("user.dir"));
//            // /libertyGit/open-liberty/dev/com.ibm.ws.microprofile.config_fat_tck/build/libs/autoFVT
//            String s = "echo GDH > " + home.toString();
//            Process p = Runtime.getRuntime().exec(s);
//            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
//
//            String line = null;
//
//            while ((line = input.readLine()) != null) {
//                System.out.println(line);
//            }
//
//            int exitVal = p.waitFor();
//            System.out.println("GDH Exited with error code " + exitVal);
//
//        } catch (Exception e) {
//            System.out.println(e.toString());
//            e.printStackTrace();
//        }
//
//    }
}
