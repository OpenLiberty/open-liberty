/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.IncludeArg;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * <p>This class contains utilities for working with minified servers. It lets you both minify and expand the server then run against the expanded server. There are two valid use
 * case scenarios:</p>
 * <code>
 * MinifiedServerTestUtils minifyUtils = new MinifiedServerTestUtils();<br/>
 * try {<br/>
 * &nbsp;&nbsp;LibertyServer someServer = LibertyServerFactory.getServer("someServer");<br/>
 * &nbsp;&nbsp;minifyUtils.setupAndStartMinifiedServer(this.getClass().getName(), "someServer", someServer);<br/>
 * &nbsp;&nbsp;// do some tests<br/>
 * &nbsp;&nbsp;minifyUtils.testViaHttpGet(new URL("http://someurl"));<br/>
 * } finally {<br/>
 * &nbsp;&nbsp;minifyUtils.tearDown();<br/>
 * }<br/>
 * </code>
 * <p>Or if you want to re-use a single minified server multiple times:</p>
 * <code>
 * try {<br/>
 * &nbsp;&nbsp;LibertyServer someServer = LibertyServerFactory.getServer("someServer");<br/>
 * &nbsp;&nbsp;minifyUtils.setup(this.getClass().getName(), "someServer", someServer);<br/>
 * &nbsp;&nbsp;RemoteFile packagedServer = minifyUtils.minifyServer();</br>
 * &nbsp;&nbsp;LibertyServer minifiedServer = minifyUtils.useMinifiedServer(packagedServer);</br>
 * &nbsp;&nbsp;//Do some tests on this server</br>
 * &nbsp;&nbsp;minifyUtils.deletedNestedServer();</br>
 * &nbsp;&nbsp;//Repeat</br>
 * &nbsp;&nbsp;minifiedServer = minifyUtils.useMinifiedServer(packagedServer);</br>
 * &nbsp;&nbsp;//Do some tests on this server</br>
 * &nbsp;&nbsp;minifyUtils.deletedNestedServer();</br>
 * } finally {<br/>
 * &nbsp;&nbsp;minifyUtils.tearDown();<br/>
 * }<br/>
 * </code>
 *
 */
public class MinifiedServerTestUtils {
    LibertyServer server;
    String testServerName;
    String testClassName;
    RemoteFile libExtract;
    RemoteFile manifest;
    RemoteFile nestedServerRoot;
    RemoteFile libExtractMetaInf;
    RemoteFile laFiles;
    RemoteFile templates;
    boolean createdManifest = false;
    boolean createdLibExtract = false;
    boolean createdLaFiles = false;
    boolean createdTemplates = false;
    private LibertyServer parentServer;

    /**
     * This sets up the class ready to be used but does not create the minified server.
     *
     * @param className The name of the test class
     * @param serverName The name of the server being used
     * @param lserver The server to be minified
     * @throws Exception
     */
    public void setup(String className, String serverName, LibertyServer lserver) throws Exception {
        Log.info(MinifiedServerTestUtils.class, "setup", "**** Begin Minify test setup for " + className + " " + serverName);

        //remember this for teardown later..
        testClassName = className;
        testServerName = serverName;
        server = lserver;
        //reset the vars
        createdManifest = false;
        createdLibExtract = false;
        createdLaFiles = false;
        createdTemplates = false;
        //tidy up any servers we made last time..
        LibertyServerFactory.tidyAllKnownServers(MinifiedServerTestUtils.class.getName());

        parentServer = server;
    }

    /**
     * This minifies a server and then extracts the minified server and starts it.
     *
     * @param className The name of the test class
     * @param serverName The name of the server being used
     * @param lserver The server to be minified
     * @throws Exception
     */
    public void setupAndStartMinifiedServer(String className, String serverName, LibertyServer lserver) throws Exception {
        setup(className, serverName, lserver);

        //sadly the tricks performed below would likely not work well on zOS..
        // a) the default extension for an archive on zos is not .zip
        // b) if we forced the extension to .zip, the permissions on the sub install would be incorrect, and it's not clear how to correct them
        // c) if we allowed the extension to be .pax, we'd need a way to extract the pax archive.
        //when we resolve these issues, we can implement the zOS case correctly.
        if (server.getMachine().getOperatingSystem().compareTo(OperatingSystem.ZOS) == 0) {
            Log.info(MinifiedServerTestUtils.class, "setup", "Tests for minify are not active currently on zOS");
            server.startServer();
        } else {

            RemoteFile packageZip = minifyServer();

            // Remember the dependency of the external test files, these will not be minified, and will be copied
            // in afterward. We must remember them now, because we use server.getServerRoot to find them, and that will change
            // once we swap to the minified image.
            RemoteFile fatTestCommon = server.getMachine().getFile(server.getServerRoot() + "/../fatTestCommon.xml");
            RemoteFile fatTestPorts = server.getMachine().getFile(server.getServerRoot() + "/../fatTestPorts.xml");
            RemoteFile testPortsProps = server.getMachine().getFile(server.getServerRoot() + "/../testports.properties");
            RemoteFile serverEnv = server.getMachine().getFile(server.getInstallRoot() + "/etc/server.env");

            useMinifiedServer(packageZip);

            // Put the required test files into the right place for new server..
            fatTestCommon.copyToDest(new RemoteFile(server.getMachine(), server.getServerRoot() + "/../fatTestCommon.xml"));
            fatTestPorts.copyToDest(new RemoteFile(server.getMachine(), server.getServerRoot() + "/../fatTestPorts.xml"));
            testPortsProps.copyToDest(new RemoteFile(server.getMachine(), server.getServerRoot() + "/../testports.properties"));
            if (serverEnv.exists())
                serverEnv.copyToDest(new RemoteFile(server.getMachine(), server.getInstallRoot() + "/etc/server.env"));

            Log.info(MinifiedServerTestUtils.class, "setup", "minified Install Root : " + server.getInstallRoot());
            Log.info(MinifiedServerTestUtils.class, "setup", "minified  Server Root : " + server.getServerRoot());

            //finally launch server so tests can run.
            server.startServer();
        }
        Log.info(MinifiedServerTestUtils.class, "setup", "started? " + server.isStarted());
    }

    /**
     * This method will extract a minified server and swap this instance to use it as it's server
     *
     *
     * @param packageZip
     * @return returns the minified server
     * @throws Exception
     * @throws IOException
     */
    public LibertyServer useMinifiedServer(RemoteFile packageZip) throws Exception, IOException {
        // Unzip the packaged server
        extractMinifiedImage(packageZip);

        // swap to using the minfied server..
        swapToMinifiedServer();

        nestedServerRoot = server.getMachine().getFile(server.getInstallRoot());
        return server;
    }

    /**
     * Minifies the current server and returns a remote file to the minified server zip.
     *
     * @return The minified server or <code>null</code> if on z/os
     * @throws Exception
     * @throws IOException
     */
    public RemoteFile minifyServer() throws Exception, IOException {
        if (server.getMachine().getOperatingSystem().compareTo(OperatingSystem.ZOS) == 0) {
            return null;
        }

        //get the image into a state where it can be minified..
        prepareForMinify();

        // run minify against our server: DO NOT CLEAN START.. let's try to reuse the cache files so these
        // tests don't take forever.
        server.packageServer(IncludeArg.MINIFY, null);
        RemoteFile packageZip = server.getMachine().getFile(server.getServerRoot() + "/" + server.getServerName() + ".zip");
        // try to move the packaged zip file to the servers directory
        RemoteFile serversDir = packageZip.getParentFile().getParentFile();
        if (packageZip.copyToDest(serversDir)) {
            packageZip.delete();
            packageZip = server.getMachine().getFile(server.getServerRoot() + "/../" + server.getServerName() + ".zip");
        }

        ZipArchiveInputStream zipIn = null;
        try {
            InputStream in = packageZip.openForReading();

            zipIn = new ZipArchiveInputStream(in);

            for (ZipArchiveEntry entry = zipIn.getNextZipEntry(); entry != null; entry = zipIn.getNextZipEntry()) {
                if ("wlp/bin/server".equals(entry.getName())) {
                    int unixMode = entry.getUnixMode();
                    if ((unixMode ^ 256) != 256) {
                        throw new Exception("unix mode not set with user execute. UnixMode was " + unixMode);
                    }
                }
            }
        } finally {
            if (zipIn != null) {
                zipIn.close();
            }
        }

        //verify that server created a logs dir where we expected
        //minify is using the embedded api to launch, which led to 96988, this test checks that doesn't happen again.
        RemoteFile logDir = server.getMachine().getFile(server.getServerRoot() + "/logs");
        if (!logDir.exists()) {
            throw new Exception("ERROR: minify did not create logs where expected..");
        }
        return packageZip;
    }

    /**
     * @throws Exception
     */
    private void swapToMinifiedServer() throws Exception {
        //before we can get a new server, we need to tidy up the one we minified, as it shares the same name..
        //and if we request the same name again, it'll give us the cached one. tidyAllKnownServer should remove
        //the old server from the cache, and let us build a new one with our special bootstrap
        //we use the testClassName, as it was the class that originally obtained the server..
        LibertyServerFactory.tidyAllKnownServers(testClassName);

        Bootstrap minifiedBootstrap = createMinifiedBootstrapForFramework();

        //swap the server instance variable for our minified server.
        server = LibertyServerFactory.getLibertyServer(testServerName, minifiedBootstrap, true);
    }

    private Bootstrap createMinifiedBootstrapForFramework() throws Exception {
        //now we need to carefully obtain a new server instance over our new expanded dir..
        Bootstrap b = Bootstrap.getInstance();
        File bootStrapFile = b.getFile();
        Log.info(MinifiedServerTestUtils.class, "setup", "Bootstrap path : " + bootStrapFile.getAbsolutePath());

        //read the old bootstrapping.properties (using whichever one was read to create the 'Bootstrap')
        //and rewrite it as a new file, with liberty root pointing at our new expanded dir.
        File minifiedBootstrapFile = new File(bootStrapFile.getAbsolutePath() + ".minified");
        BufferedReader fr = new BufferedReader(new FileReader(bootStrapFile));
        BufferedWriter bw = new BufferedWriter(new FileWriter(minifiedBootstrapFile, false));
        while (fr.ready()) {
            String line = fr.readLine();
            if (line.startsWith("libertyInstallPath=")) {
                Log.info(MinifiedServerTestUtils.class, "setup", "install path : " + line);
                line = "libertyInstallPath=" + server.getServerRoot() + "/wlp"; //use of '/' tested on win & unix.
                Log.info(MinifiedServerTestUtils.class, "setup", "install path 2 : " + line);
            }
            bw.write(line + "\n");
        }
        fr.close();
        bw.close();
        //special instance returns us one based on the file, rather than the cached static instance.
        //it also won't cache the instance so we don't break other tests.
        return Bootstrap.getSpecialInstance(minifiedBootstrapFile);
    }

    /**
     * @param packageZip
     * @throws Exception
     * @throws IOException
     */
    private void extractMinifiedImage(RemoteFile packageZip) throws Exception, IOException {
        // Can't use unzip because it isn't on all machines so need to use jar
        String command = server.getMachine().getFile(server.getMachineJavaJarCommandPath()).getAbsolutePath();
        String[] parameters = { "xf", "\"" + packageZip.getAbsolutePath() + "\"" };
        ProgramOutput unzipPackagedServerOutput = server.getMachine().execute(command, parameters, server.getServerRoot());
        if (unzipPackagedServerOutput.getReturnCode() != 0) {
            throw new IOException(unzipPackagedServerOutput.getCommand() + " reported; retcode'" + unzipPackagedServerOutput.getReturnCode() + "' stdout'"
                                  + unzipPackagedServerOutput.getStdout() + "' stderr'"
                                  + unzipPackagedServerOutput.getStderr() + "'");
        }

        //fudge the permissions on the wlp/bin/server script..
        if (server.getMachine().getOperatingSystem().compareTo(OperatingSystem.WINDOWS) != 0) {
            //try to exec this on all non windows platforms..
            server.getMachine().execute("/bin/chmod", //POSIX says chmod lives here...
                                        new String[] { "755", server.getServerRoot() + File.separator + "wlp" + File.separator + "bin" + File.separator + "server" });
            server.getMachine().execute("/bin/chmod", //POSIX says chmod lives here...
                                        new String[] { "755", server.getServerRoot() + File.separator + "wlp" + File.separator + "bin" + File.separator + "featureManager" });
            server.getMachine().execute("/bin/chmod", //POSIX says chmod lives here...
                                        new String[] { "755", server.getServerRoot() + File.separator + "wlp" + File.separator + "bin" + File.separator + "productInfo" });
        }
    }

    /**
     * @throws Exception
     * @throws IOException
     */
    private void prepareForMinify() throws Exception, IOException {
        //first, we need to check that some parts of liberty that WILL be there on a customer install, are here as part of our FAT
        //they may not be, which caused 95749
        //lib/extract should hold the self-extractor code, which we wont use anyways, as we're building a zip in the testcase.
        libExtract = server.getMachine().getFile(server.getInstallRoot() + "/lib/extract");
        if (!libExtract.exists()) {
            libExtract.mkdir();
            createdLibExtract = true;
        }
        libExtractMetaInf = server.getMachine().getFile(server.getInstallRoot() + "/lib/extract/META-INF");
        if (!libExtractMetaInf.exists()) {
            libExtractMetaInf.mkdir();
            manifest = server.getMachine().getFile(libExtractMetaInf, "MANIFEST.MF");
            java.io.OutputStream os = manifest.openForWriting(false);
            os.write("Dummy manifest written during minify test".getBytes());
            os.flush();
            os.close();
            createdManifest = true;
        }
        laFiles = server.getMachine().getFile(server.getInstallRoot() + "/lafiles");
        if (!laFiles.exists()) {
            laFiles.mkdir();
            createdLaFiles = true;
        }
        templates = server.getMachine().getFile(server.getInstallRoot() + "/templates");
        if (!templates.exists()) {
            templates.mkdir();
            createdTemplates = true;
        }
    }

    public void tearDown() throws Exception {
        Log.info(MinifiedServerTestUtils.class, "tearDown", "issuing stop to server at " + server.getInstallRoot());

        server.stopServer();

        Log.info(MinifiedServerTestUtils.class, "tearDown", "after stop of server at " + server.getInstallRoot());

        //tidy up those extra dirs we built.. if we built them..
        if (createdLibExtract) {
            if (!libExtract.delete()) {
                Log.info(MinifiedServerTestUtils.class, "tearDown", "Failed to cleanup " + libExtract.getAbsolutePath());
            }
        } else if (createdManifest) {
            if (!libExtractMetaInf.delete()) {
                Log.info(MinifiedServerTestUtils.class, "tearDown", "Failed to cleanup " + libExtractMetaInf.getAbsolutePath());
            }
        }
        if (createdLaFiles) {
            if (!laFiles.delete()) {
                Log.info(MinifiedServerTestUtils.class, "tearDown", "Failed to cleanup " + laFiles.getAbsolutePath());
            }
        }
        if (createdTemplates) {
            if (!templates.delete()) {
                Log.info(MinifiedServerTestUtils.class, "tearDown", "Failed to cleanup " + templates.getAbsolutePath());
            }
        }

        deletedNestedServer();

        Log.info(MinifiedServerTestUtils.class, "tearDown", "tidying up our server at " + server.getInstallRoot());
        //also tidy up our servers, the nature of our tests, and the arrangement of our classes
        //doesnt work well with the way the framework expects it..
        LibertyServerFactory.tidyAllKnownServers(testClassName);
        LibertyServerFactory.tidyAllKnownServers(MinifiedServerTestUtils.class.getName());
    }

    /**
     * This deletes the nested server if one was created by extracting a minified server. Also sets the active server back to the parent server.
     *
     * @return <code>true</code> if the nested server is deleted
     * @throws Exception
     */
    public boolean deletedNestedServer() throws Exception {
        server = parentServer;
        if (nestedServerRoot != null) {
            if (!nestedServerRoot.delete()) {
                Log.info(MinifiedServerTestUtils.class, "tearDown", "Failed to cleanup " + nestedServerRoot.getAbsolutePath());
                return false;
            } else {
                nestedServerRoot = null;
            }
        }

        return true;
    }

    protected static final String staticUrlPrefix = "http://localhost:" + System.getProperty("HTTP_default", "8000") + "/static";
    protected static final String servletUrlPrefix = "http://localhost:" + System.getProperty("HTTP_default", "8000") + "/ServletTest";

    public void testViaHttpGet(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        try {
            // read the page contents
            String line = br.readLine();
            List<String> lines = new ArrayList<String>();
            while (line != null) {
                lines.add(line);
                line = br.readLine();
            }
            con.disconnect();

            // log the output lines so we can debug
            Log.info(MinifiedServerTestUtils.class, "testViaHttpGet", "start - response from url " + url);
            for (String msg : lines) {
                Log.info(MinifiedServerTestUtils.class, "testViaHttpGet", msg);
            }
            Log.info(MinifiedServerTestUtils.class, "testViaHttpGet", "end - response from url " + url);

            // check the first line to be sure we at least got to the right place.
            assertEquals("Servlet content was incorrect", "This is Mini-WOPR. Welcome Dr Falken.", lines.get(0));

            boolean foundPass = false;

            //Pass criteria:
            // - No FAIL: lines
            // - at least one PASS line
            for (String msg : lines) {
                if (msg.startsWith("FAIL: ")) {
                    // When there is a fail log the whole output
                    StringBuilder builder = new StringBuilder();
                    for (String lineForMessage : lines) {
                        builder.append(lineForMessage);
                        builder.append("\n");
                    }
                    fail(builder.toString());
                }
                if (msg.startsWith("PASS")) {
                    foundPass = true;
                }
            }
            if (!foundPass) {
                fail("Did not see PASS from servlet invocation at " + url);
            }
        } finally {
            br.close();
        }
    }

}