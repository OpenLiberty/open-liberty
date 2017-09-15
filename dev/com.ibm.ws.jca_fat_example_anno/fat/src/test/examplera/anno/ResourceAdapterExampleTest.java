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
package test.examplera.anno;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class ResourceAdapterExampleTest extends FATServletClient {

    public static final String APP_NAME = "ExampleApp";

    @Server("jca.fat.example.anno")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "web");
        ShrinkHelper.exportToServer(server, "dropins", app);
        server.addInstalledAppForValidation(APP_NAME);

        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "ExampleRA.rar")
                        .addAsLibraries(ShrinkWrap.create(JavaArchive.class)
                                        .addPackage("com.ibm.example.jca.anno"));
        ShrinkHelper.exportToServer(server, "dropins", rar);

        server.startServer();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        server.stopServer();
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
                "Successfully performed FIND with output: {area=86939, capital=Saint Paul, population=5379139, state=Minnesota}");
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
                "Successfully performed REMOVE with output: {city=Stewartville, population=5916, state=Minnesota}");

        // attempt removal of something that doesn't exist
        runTest("?functionName=REMOVE&city=Stewartville",
                "Did not REMOVE any entries.");
    }

    @Test
    public void testMessageDrivenBean() throws Exception {
        server.setMarkToEndOfLog();

        runTest("?functionName=ADD&county=Olmsted&state=Minnesota&population=147066&area=654.5",
                "Successfully performed ADD with output: {area=654.5, county=Olmsted, population=147066, state=Minnesota}");

        // search messages log for MDB output
        server.waitForStringInLog("ExampleMessageDrivenBean.onMessage record = {area=654.5, county=Olmsted, population=147066, state=Minnesota}");
    }
}
