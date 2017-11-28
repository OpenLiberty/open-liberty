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

import org.junit.AfterClass;
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
public class CustomConfigSourceProvider {

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
        sameTestClassInTck();
    }

    /**
     * @param methodName
     * @return
     * @throws Exception
     */
    private int sameTestClassInTck() throws Exception {
        if (!init) {
            init();
        }
        File mvnOutput = new File(home, "mvnOut_" + className);
        int rc = runCmd(mvnCliClassRoot, tckRunnerDir, mvnOutput);
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
