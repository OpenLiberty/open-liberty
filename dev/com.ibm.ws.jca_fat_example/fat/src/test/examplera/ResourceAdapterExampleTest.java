/*******************************************************************************
 * Copyright (c) 2012, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.examplera;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@MinimumJavaLevel(javaLevel = 11)
@RunWith(FATRunner.class)
public class ResourceAdapterExampleTest extends FATServletClient {

    @Server("com.ibm.ws.jca.fat.example")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ExampleWeb.war")
                        .addPackages(true, "web")
                        .addAsWebInfResource(new File("test-applications/ExampleWeb/resources/WEB-INF/web.xml"));
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ExampleApp.ear")
                        .addAsModule(war)
                        .addAsManifestResource(new File("test-applications/ExampleApp/resources/META-INF/application.xml"));
        ShrinkHelper.exportDropinAppToServer(server, ear);

        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "ExampleRA.rar")
                        .addAsLibraries(ShrinkWrap.create(JavaArchive.class)
                                        .addPackage("com.ibm.example.jca.adapter"))
                        .addAsManifestResource(new File("test-resourceadapters/ExampleRA/resources/META-INF/ra.xml"));
        ShrinkHelper.exportDropinAppToServer(server, rar, DeployOptions.DISABLE_VALIDATION);

        server.startServer();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        server.stopServer("CWWKG0033W"); // ported BVT test produced warning because and was not checking server logs for it
    }

    @Test
    public void testAddAndFind() throws Exception {
        // attempt find for an entry that isn't in the table
        HttpUtils.findStringInReadyUrl(server,
                                       "/ExampleApp?functionName=FIND&capital=Saint%20Paul",
                                       "Did not FIND any entries");

        // add
        HttpUtils.findStringInReadyUrl(server,
                                       "/ExampleApp?functionName=ADD&state=Iowa&population=3074186&area=56272&capital=Des%20Moines",
                                       "Successfully performed ADD with output: {area=56272, capital=Des Moines, population=3074186, state=Iowa}");

        HttpUtils.findStringInReadyUrl(server,
                                       "/ExampleApp?functionName=ADD&state=Minnesota&population=5379139&area=86939&capital=Saint%20Paul",
                                       "Successfully performed ADD with output: {area=86939, capital=Saint Paul, population=5379139, state=Minnesota}");

        // find
        HttpUtils.findStringInReadyUrl(server,
                                       "/ExampleApp?functionName=FIND&capital=Saint%20Paul",
                                       "Successfully performed FIND with output: {area=86939, capital=Saint Paul, population=5379139, state=Minnesota}");
    }

    @Test
    public void testAddAndRemove() throws Exception {

        // add
        HttpUtils.findStringInReadyUrl(server,
                                       "/ExampleApp?functionName=ADD&city=Rochester&state=Minnesota&population=106769",
                                       "Successfully performed ADD with output: {city=Rochester, population=106769, state=Minnesota}");

        HttpUtils.findStringInReadyUrl(server,
                                       "/ExampleApp?functionName=ADD&city=Stewartville&state=Minnesota&population=5916",
                                       "Successfully performed ADD with output: {city=Stewartville, population=5916, state=Minnesota}");

        HttpUtils.findStringInReadyUrl(server,
                                       "/ExampleApp?functionName=ADD&city=Byron&state=Minnesota&population=4914",
                                       "Successfully performed ADD with output: {city=Byron, population=4914, state=Minnesota}");

        // remove
        HttpUtils.findStringInReadyUrl(server,
                                       "/ExampleApp?functionName=REMOVE&city=Stewartville",
                                       "Successfully performed REMOVE with output: {city=Stewartville, population=5916, state=Minnesota}");

        // attempt removal of something that doesn't exist
        HttpUtils.findStringInReadyUrl(server,
                                       "/ExampleApp?functionName=REMOVE&city=Stewartville",
                                       "Did not REMOVE any entries");
    }

    @Test
    public void testMessageDrivenBean() throws Exception {
        HttpUtils.findStringInReadyUrl(server,
                                       "/ExampleApp?functionName=ADD&county=Olmsted&state=Minnesota&population=147066&area=654.5",
                                       "Successfully performed ADD with output: {area=654.5, county=Olmsted, population=147066, state=Minnesota}");

        // search messages log for MDB output
        List<String> found = server.findStringsInLogs("ExampleMessageDrivenBean\\.onMessage record = \\{area=654\\.5, county=Olmsted, population=147066, state=Minnesota\\}");
        assertEquals(found.toString(), 1, found.size());
    }

}
