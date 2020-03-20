/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class PackageLooseConfigDropinsTest extends AbstractLooseConfigTest {

    LibertyServer server;
    private static String ARCHIVE = "DefaultArchive.war";

    @Rule
    public TestName testName = new TestName();

    @Before
    public void before() throws Exception {

        // Delete previous archive file if it exists
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.deleteFileFromLibertyServerRoot(ARCHIVE_PACKAGE);

        System.out.printf("%n%s%n", testName.getMethodName());
    }

    @After
    public void clean() throws Exception {
        server.deleteFileFromLibertyServerRoot(DROPINS_DIR + "/DefaultArchive.war.xml");
    }

    /**
     * Packages a loose application with '--include=all' and verifies that the
     * resulting package contains the expected entries
     */
    @Test
    public void testIncludeAll() throws Exception {
        try {
            String[] cmd = new String[] { "--archive=" + ARCHIVE_PACKAGE, "--include=all",
                                          "--server-root=" + SERVER_ROOT };
            packageWithConfig(server, cmd);
            ZipFile zipFile = new ZipFile(server.getServerRoot() + "/" + ARCHIVE_PACKAGE);
            try {
                String serverPath = SERVER_ROOT + "/usr/servers/" + SERVER_NAME;
                boolean foundServerEntry = false;
                boolean foundWarFileEntry = false;
                boolean foundExpandedEntry = false;
                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                    ZipEntry entry = en.nextElement();

                    foundServerEntry |= entry.getName().matches("^" + serverPath + "/$");
                    foundWarFileEntry |= entry.getName().matches("^" + serverPath + "/dropins/" + ARCHIVE + "$");
                    foundExpandedEntry |= entry.getName().matches("^" + serverPath + "/apps/expanded/" + ARCHIVE + "/.*$");
                }
                assertTrue("The package did not contain " + serverPath + "/ as expected.", foundServerEntry);
                assertTrue("The package did not contain " + serverPath + "/dropins/" + ARCHIVE + " as expected.",
                           foundWarFileEntry);
                assertTrue("The package did not contain " + serverPath + "/apps/expanded/" + ARCHIVE + "/ as expected.",
                           foundExpandedEntry);
            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * Packages a loose application with '--include=runnable' and verifies that the
     * resulting package contains the expected entries
     */
    @Test
    public void testIncludeRunnable() throws Exception {

        try {
            String archivePackage = "MyPackage.jar";
            String[] cmd = new String[] { "--archive=" + archivePackage, "--include=runnable" };
            packageWithConfig(server, cmd);
            JarFile jarFile = new JarFile(server.getServerRoot() + "/" + archivePackage);
            try {
                String serverPath = "wlp/usr/servers/" + SERVER_NAME;
                boolean foundServerEntry = false;
                boolean foundWarFileEntry = false;
                boolean foundExpandedEntry = false;
                for (Enumeration<? extends JarEntry> en = jarFile.entries(); en.hasMoreElements();) {
                    JarEntry entry = en.nextElement();
                    // Uses String.matches() to ensure exact path is found
                    foundServerEntry |= entry.getName().matches("^" + serverPath + "/$");
                    foundWarFileEntry |= entry.getName().matches("^" + serverPath + "/dropins/" + ARCHIVE + "$");
                    foundExpandedEntry |= entry.getName().matches("^" + serverPath + "/apps/expanded/" + ARCHIVE + "/.*$");
                }
                assertTrue("The package did not contain " + serverPath + "/ as expected.", foundServerEntry);
                assertTrue("The package did not contain " + serverPath + "/dropins/" + ARCHIVE + " as expected.",
                           foundWarFileEntry);
                assertTrue("The package did not contain " + serverPath + "/apps/expanded/" + ARCHIVE + "/ as expected.",
                           foundExpandedEntry);
            } finally {
                try {
                    jarFile.close();
                } catch (IOException ex) {
                }
            }
        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * Packages a loose application into a .jar with 'include=runnable' and verifies
     * that it starts up properly after being run with 'java -jar packageName.jar'
     */
    @Test
    public void testCreateAndStartRunnableJar() throws Exception {
        try {
            String archivePackage = "runnablePackage.jar";
            String[] cmd = new String[] { "--archive=" + archivePackage, "--include=runnable" };
            packageWithConfig(server, cmd);

            // Start a separate process to run the jar
            Process proc = Runtime.getRuntime().exec(new String[] { "java", "-jar", server.getServerRoot() + "/" + archivePackage });
            try {
                BufferedReader brOutput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                BufferedReader brError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

                // Timeout after 20 seconds if the server still hasn't started
                long timeStart = System.nanoTime();
                long timeLimit = 20 * (long) Math.pow(10, 9);

                boolean serverDidLaunch = false;
                boolean serverIsReady = false;
                while (!(serverDidLaunch && serverIsReady) && timeLimit - (System.nanoTime() - timeStart) > 0) {

                    // If an error is read fail the test
                    if (brError.ready()) {
                        fail("The server package " + archivePackage + " encountered the following error(s):\n\t"
                             + brError.readLine());
                    }

                    if (brOutput.ready()) {
                        String line = brOutput.readLine();
                        serverDidLaunch |= line.matches("^.* CWWKE0001I: .* " + SERVER_NAME + " .*$");
                        serverIsReady |= line.matches(".* CWWKF0011I: .* " + SERVER_NAME + " .*$");
                    }
                }

                assertTrue("The server package " + archivePackage + " did not launch successfully", serverDidLaunch);
                assertTrue("The server package " + archivePackage + " was not ready to run in time", serverIsReady);
            } finally {
                proc.destroy();
            }
        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    @Override
    public String getAppsTargetDir() {

        return DROPINS_DIR;
    }
}