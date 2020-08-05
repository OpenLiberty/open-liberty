package wlp.lib.extract;

/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class ExternalDependencyDownloadTest {
    private static LibertyServer hostingServer = LibertyServerFactory.getLibertyServer("dependencyHostServer");
    private static final String HOST_APP_NAME = "dependencyHost";
    private static final File FILES_DIR = new File("lib/LibertyFATTestFiles/");
    private static final File SERVERS_DIR = new File("publish/servers");

    @BeforeClass
    public static void setupClass() throws Exception {
        ShrinkHelper.defaultDropinApp(hostingServer, HOST_APP_NAME, "web");

        // Start the server which hosts the dependency URLs
        // This runs for the whole test suite while we test installing samples which reference it
        hostingServer.startServer();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (hostingServer != null && hostingServer.isStarted()) {
            hostingServer.stopServer();
        }
    }

//    @Mode(TestMode.QUARANTINE)
//    @Test
//    Test should not be hitting an external site.  Need to find a better implemenation before re-enabling.
    public void testHTTP2HTTPSRedirect() throws Exception {
        String serverName = "http2httpsRedirectGood";
        String dependencyTargetFile = "shared/lib/" + serverName + ".testfile.jar";

        File tmpJar = new File(FILES_DIR, serverName + ".jar");
        if (tmpJar.exists()) {
            tmpJar.delete();
        }

        String dependencyUrl = "http://search.maven.org/remotecontent?filepath=com/datastax/cassandra/cassandra-driver-core/2.0.4/cassandra-driver-core-2.0.4.jar";
        String dependencyTargetPath = "servers/" + serverName + "/" + dependencyTargetFile;
        createExternalDependencySample(tmpJar, serverName, dependencyUrl, dependencyTargetPath);
        tmpJar.deleteOnExit();

        LibertyServer installServer = createEmptyServer(serverName);
        installServer.installSampleWithExternalDependencies(serverName);

        assertTrue(installServer.fileExistsInLibertyServerRoot(dependencyTargetFile));
    }

    @Test
    public void testInstallDependency() throws Exception {
        assertGoodInstall("/good", "depGood");
    }

    @Test
    public void testInstallNotFound() throws Exception {
        assertBadInstall("/notfound", "depNotFound");
    }

    @Test
    public void testInstallBadRequest() throws Exception {
        assertBadInstall("/badrequest", "depBadRequest");
    }

    @Test
    public void testInstallForbidden() throws Exception {
        assertBadInstall("/forbidden", "depForbidden");
    }

    @Test
    public void testInstallServerError() throws Exception {
        assertBadInstall("/serverError", "depServerError");
    }

    @Test
    public void testRedirect301() throws Exception {
        assertGoodInstall("/redirect301", "dep301");
    }

    @Test
    public void testRedirect302() throws Exception {
        assertGoodInstall("/redirect302", "dep302");
    }

    @Test
    public void testRedirect303() throws Exception {
        assertGoodInstall("/redirect303", "dep303");
    }

    @Test
    public void testRedirect307() throws Exception {
        assertGoodInstall("/redirect307", "dep307");
    }

    @Test
    public void testRedirectNotFound() throws Exception {
        assertBadInstall("/redirectnotfound", "depRedirectNotFound");
    }

    @Test
    public void testInstallProtocolRedirect() throws Exception {
        assertGoodInstall("/protocolchange", "depProtoChange");
    }

    /**
     * Generate a sample with the given dependency and server name and ensure that installing it fails.
     *
     * @param dependencyPath the path to download the dependency from, relative to the root of the dependency hosting app
     * @param serverName the server name that should be included in the sample
     * @throws Exception
     */
    private void assertGoodInstall(String dependencyPath, String serverName) throws Exception {
        String dependencyTargetFile = serverName + ".testfile";

        prepareSampleJar(dependencyPath, serverName);
        LibertyServer installServer = createEmptyServer(serverName);

        boolean serveravailable = hostingServer.isStarted();
        boolean fileavailable = hostingServer.fileExistsInLibertyInstallRoot("/usr/servers/" + serverName);

        Log.info(ExternalDependencyDownloadTest.class, "serveravailable", Boolean.toString(serveravailable));
        Log.info(ExternalDependencyDownloadTest.class, "fileavailable", Boolean.toString(fileavailable));
        Log.info(ExternalDependencyDownloadTest.class, "getServerFolderFiles", listFiles(hostingServer.getUserDir() + "/servers"));
        Log.info(ExternalDependencyDownloadTest.class, "getServerFiles", listFiles(hostingServer.getUserDir() + "/servers/" + serverName));

        // This method uses the sample jar created above
        installServer.installSampleWithExternalDependencies(serverName);

        assertTrue(installServer.fileExistsInLibertyServerRoot(dependencyTargetFile));
    }

    /**
     * Generate a sample with the given dependency and server name and ensure that installing it fails.
     *
     * @param dependencyPath the path to download the dependency from, relative to the root of the dependency hosting app
     * @param serverName the server name that should be included in the sample
     * @throws Exception
     */
    private void assertBadInstall(String dependencyPath, String serverName) throws Exception {
        String dependencyTargetFile = serverName + ".testfile";

        prepareSampleJar(dependencyPath, serverName);
        LibertyServer installServer = createEmptyServer(serverName);

        try {
            // This method uses the sample jar created above
            installServer.installSampleWithExternalDependencies(serverName);
            fail("Expected sample installation to fail but it did not");
        } catch (Exception e) {
            // Expected exception, do nothing
        }

        assertFalse(installServer.fileExistsInLibertyServerRoot(dependencyTargetFile));
    }

    /**
     * Generate a simple sample with a dependency
     * <p>
     * The sample has one external dependency which points to a path under the depedencyHost app.
     *
     * @param dependencyPath the path to download the dependency from, relative to the root of the dependency hosting app
     * @param serverName the server name that should be included in the sample
     * @throws Exception
     */
    private void prepareSampleJar(String dependencyPath, String serverName) throws Exception {
        File tmpJar = new File(FILES_DIR, serverName + ".jar");
        if (tmpJar.exists()) {
            tmpJar.delete();
        }

        String dependencyUrl = "http://" + hostingServer.getHostname() + ":" + hostingServer.getHttpDefaultPort() + "/" + HOST_APP_NAME + dependencyPath;
        String dependencyTargetFile = serverName + ".testfile";
        String dependencyTargetPath = "servers/" + serverName + "/" + dependencyTargetFile;
        createExternalDependencySample(tmpJar, serverName, dependencyUrl, dependencyTargetPath);
        tmpJar.deleteOnExit();
    }

    /**
     * Ensure the server directory exists and create a new LibertyServer.
     *
     * @param serverName the name for the new server
     * @return the newly created server
     */
    private LibertyServer createEmptyServer(String serverName) {
        File serverDir = new File(SERVERS_DIR, serverName);
        serverDir.mkdirs();

        return LibertyServerFactory.getLibertyServer(serverName);
    }

    /**
     * Create a new minimal sample file with the given external dependency.
     * <p>
     * The generated sample includes
     * <ul>
     * <li>The wlp.lib.extract classes</li>
     * <li>A server directory with the given name</li>
     * <li>A basic server.xml from publish/files/sampleServer.xml</li>
     * <li>A generated externaldependencies.xml</li>
     * </li>
     *
     * @param outSample the jar file to write the new sample to
     * @param serverName the name of the server to include in the sample jar
     * @param dependencyUrl the URL which the externaldependencies.xml file should point to
     * @param dependencyTargetPath the path which the external dependency should be downloaded to
     */
    private static void createExternalDependencySample(File outSample, String serverName, String dependencyUrl, String dependencyTargetPath) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outSample));

        writeDir(out, "META-INF");
        writeFile(out, "META-INF/MANIFEST.MF", new File(FILES_DIR, "MANIFEST.MF"));

        writeExternalDependencies(out, dependencyUrl, dependencyTargetPath);

        copyArchive(out, new File(FILES_DIR, "wlp.lib.extract.zip"));

        writeDir(out, "wlp/usr");
        writeDir(out, "wlp/usr/servers");
        writeDir(out, "wlp/usr/servers/" + serverName);
        writeFile(out, "wlp/usr/servers/" + serverName + "/server.xml", new File(FILES_DIR, "sampleServer.xml"));

        out.close();
        assertTrue(outSample.getAbsolutePath() + " was not created", outSample.exists());
    }

    /**
     * Write a directory to an archive
     *
     * @param out
     * @param path
     * @throws IOException
     */
    private static void writeDir(ZipOutputStream out, String path) throws IOException {
        if (!path.endsWith("/")) {
            path += "/";
        }
        out.putNextEntry(new ZipEntry(path));
    }

    /**
     * Read a file from disk and write it into an archive
     *
     * @param out
     * @param path
     * @param file
     * @throws IOException
     */
    private static void writeFile(ZipOutputStream out, String path, File file) throws IOException {
        if (path.endsWith("/")) {
            throw new IOException("File paths may not end with a slash");
        }
        out.putNextEntry(new ZipEntry(path));
        InputStream in = new FileInputStream(file);
        copyStream(out, in);
        in.close();
        out.closeEntry();
    }

    /**
     * Copy the contents of one archive into another archive
     *
     * @param out
     * @param archive
     * @throws IOException
     */
    private static void copyArchive(ZipOutputStream out, File archive) throws IOException {
        ZipInputStream in = new ZipInputStream(new FileInputStream(archive));
        ZipEntry e;
        while ((e = in.getNextEntry()) != null) {
            out.putNextEntry(new ZipEntry(e.getName()));
            if (!e.isDirectory()) {
                copyStream(out, in);
            }
            out.closeEntry();
        }
        in.close();
    }

    /**
     * Copy from an input stream into an output stream until the end of the input stream is reached.
     * <p>
     * Does not close either stream.
     *
     * @param out
     * @param in
     * @throws IOException
     */
    private static void copyStream(OutputStream out, InputStream in) throws IOException {
        byte[] buf = new byte[1024];
        int count;
        while ((count = in.read(buf)) != -1) {
            out.write(buf, 0, count);
        }
    }

    /**
     * Write a simple externaldependencies.xml file entry into a ZipOutputStream.
     * <p>
     * The generated file specifies one dependency with the given url and target path.
     *
     * @param out the ZipOutputStream to write to
     * @param dependencyUrl the URL hosting the dependency
     * @param dependencyTargetPath the destination path for the dependency
     * @throws IOException
     */
    private static void writeExternalDependencies(ZipOutputStream out, String dependencyUrl, String dependencyTargetPath) throws IOException {
        out.putNextEntry(new ZipEntry("externaldependencies.xml"));
        OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
        writer.append("<externalDependencies description=\"Test Dependencies\">\n");

        writer.append("    <dependency url=\"");
        writer.append(dependencyUrl);
        writer.append("\" targetpath=\"");
        writer.append(dependencyTargetPath);
        writer.append("\" />\n");

        writer.append("</externalDependencies>\n");
        writer.flush();
        out.closeEntry();
    }

    public static String listFiles(String path) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        String filenames = "Files: ";
        String dirnames = "Directories: ";
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                filenames += listOfFiles[i].getName() + " ";
            } else if (listOfFiles[i].isDirectory()) {
                dirnames += listOfFiles[i].getName() + " ";
            }
        }

        filenames += "\n";

        return (filenames + dirnames);

    }

}