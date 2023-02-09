/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.workcontext;

// added
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class ResourceAdapterExampleTest extends FATServletClient {

    public static final String APP_NAME = "WorkContextJCApp";
    // see under publish-servers
    @Server("jca.fat.RaExampleServer")
    public static LibertyServer server;
    // LBH
    private static String ServletURL;

    @BeforeClass
    public static void setup() throws Exception {
        server = LibertyServerFactory.getLibertyServer("jca.fat.RaExampleServer");
        try {
            server.installSystemFeature("threadingTestFeature-1.0");
            //server.installSystemBundle("test.bundle.threading_1.0.0");
            server.installSystemBundle("test.bundle.threading");
        } catch (Exception e) {
            e.printStackTrace();
        }

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "web");
        ShrinkHelper.exportToServer(server, "dropins", app);
        server.addInstalledAppForValidation(APP_NAME);
// see under bin and test-resourceadapter
        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "WorkContextJCAppRA.rar")
                        .addAsLibraries(ShrinkWrap.create(JavaArchive.class)
                                        .addPackage("com.ibm.workcontext.jca"));
        ShrinkHelper.exportToServer(server, "dropins", rar);

        server.startServer();
        // LBH
        ServletURL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/WorkContextJCApp/RAExampleServlet";

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        server.stopServer("CNTR4015W");
    }

    private void runTest(String queryString, String... toFind) throws Exception {
        if (toFind == null || toFind.length == 0)
            toFind = new String[] { "" };
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + queryString, toFind);
    }

    @Test
    public void testAddAndFind() throws Exception {
        // attempt find for an entry that isn't in the table
        runTest("?functionName=FIND&capital=Saint%20Paul", "Did not FIND any entries");

        // add
        runTest("?functionName=ADD&state=Iowa&population=30741869&area=56272&capital=Des%20Moines");
        //output = runInServlet("functionName=ADD&state=Iowa&population=30741869&area=56272&capital=Des%20Moines");
        runTest("?functionName=ADD&state=Minnesota&population=5379139&area=86939&capital=Saint%20Paul");
        //output = runInServlet("functionName=ADD&state=Minnesota&population=5379139&area=86939&capital=Saint%20Paul");

        // find
        runTest("?functionName=FIND&capital=Saint%20Paul",
                "Successfully performed FIND with output: [area=86939, capital=Saint Paul, population=5379139, state=Minnesota]");
//        output = runInServlet("functionName=FIND&capital=Saint%20Paul");
//        if (output.indexOf("Successfully performed FIND with output: {area=86939, capital=Saint Paul, population=5379139, state=Minnesota}") < 0)
//            throw new Exception("Did not find entry. Output: " + output);
    }

    @Test
    public void testAddAndRemove() throws Exception {
        // add
        runTest("?functionName=ADD&city=Rochester&state=Minnesota&population=106769");
        runTest("?functionName=ADD&city=Stewartville&state=Minnesota&population=5916");
        runTest("?functionName=ADD&city=Byron&state=Minnesota&population=4914");

        // remove
        runTest("?functionName=REMOVE&city=Stewartville",
                "Successfully performed REMOVE with output: [city=Stewartville, population=5916, state=Minnesota]");

        // attempt removal of something that doesn't exist
        runTest("?functionName=REMOVE&city=Stewartville",
                "Did not REMOVE any entries.");
    }

    @Test
    public void testMessageDrivenBean() throws Exception {
        server.setMarkToEndOfLog();

        runTest("?functionName=ADD&county=Olmsted&state=Minnesota&population=147066&area=654.5",
                "Successfully performed ADD with output: [area=654.5, county=Olmsted, population=147066, state=Minnesota]");

        // search messages log for MDB output
        server.waitForStringInLog("ExampleMessageDrivenBean.onMessage record = [area=654.5, county=Olmsted, population=147066, state=Minnesota]");
    }

    // JCA code here LBH
    /**
     * Verifies that the workContextService is correctly intercepting JCAMDB workContext results
     * The interceptor scans incoming callable's and runnables for workContext and prints JCMADB contexts
     * in messages.log using System.out.println
     */
    @Test
    public void testTaskWorkContext() throws Exception {
        final String method = "testTaskWorkContext";
        server.setMarkToEndOfLog();
        invokeURL(ServletURL).readLine();

        //runTest("?functionName=ADD&city=JCACity&state=JCAState&population=10");
        runTest("?functionName=ADD&county=JCA&state=NC&population=1333&area=354.3",
                "Successfully performed ADD with output: [area=354.3, county=JCA, population=1333, state=NC]");
        System.out.println(" Start check for JCA --- " + method);
        // verify that the task intercepter captured the work context by looking for the System.out.printlns it puts in the server log
        assertTrue("Did not find 'This runnable has work context. The type is JCA.' in log file",
                   server.findStringsInLogs("This runnable has work context. The type is JCA.").size() > 0);
        System.out.println(" End check for JCA in msg --- " + method);
        // search messages log for MDB output
        server.waitForStringInLog("ExampleMessageDrivenBean.onMessage record = [area=354.3, county=JCA, population=1333, state=NC]");

    }

    /**
     * Invokes the specified URL and returns a BufferedReader to the returned content.
     *
     * @param urlString The URL to invoke
     * @return a BufferedReader to the content returned from the invoked URL
     * @throws Exception
     */
    private BufferedReader invokeURL(String urlString) throws Exception {
        final String method = "invokeURL";
        System.out.println(" -- BufferReader --- " + method);
        HttpURLConnection con = (HttpURLConnection) new URL(urlString).openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

        return br;
    }
    // JCA code ends here

}
