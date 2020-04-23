/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.bval;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class BeanValidationTest extends FATServletClient {
    private static final String BVAL_APP = "jca-bval";
    private static final String BVAL_RAR = "BValRA";

    @Server("com.ibm.ws.jca.fat.bval")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create applications
        ShrinkHelper.defaultApp(server, BVAL_APP, "web", "web.mdb");
        ShrinkHelper.defaultRar(server, BVAL_RAR, "com.ibm.bval.jca.adapter");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }

    /**
     * Utility method to run a test on RABValServlet.
     *
     * @param query query string for the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    private StringBuilder runInServlet(String queryString) throws Exception {
        return runTestWithResponse(server, BVAL_APP, "testBeanValidation&" + queryString);
    }

    private void restartWithNewConfig(String fileName) throws Exception {
        if (server.isStarted())
            server.stopServer();

        server.setServerConfigurationFile(fileName);
        server.startServer(testName.getMethodName() + ".log");
    }

    @Test
    public void testAddAndFind() throws Exception {
        // attempt find for an entry that isn't in the table
        StringBuilder output = runInServlet("functionName=FIND&capital=Saint%20Paul");
        if (output.indexOf("Did not FIND any entries") < 0)
            throw new Exception("Entry should not have been found. Output: " + output);

        // add
        output = runInServlet("functionName=ADD&state=Iowa&population=30741869&area=56272&capital=Des%20Moines");
        output = runInServlet("functionName=ADD&state=Minnesota&population=5379139&area=86939&capital=Saint%20Paul");

        // find
        output = runInServlet("functionName=FIND&capital=Saint%20Paul");
        if (output.indexOf("Successfully performed FIND with output: {area=86939, capital=Saint Paul, population=5379139, state=Minnesota}") < 0)
            throw new Exception("Did not find entry. Output: " + output);
    }

    @Test
    public void testAddAndRemove() throws Exception {
        // add
        StringBuilder output = runInServlet("functionName=ADD&city=Rochester&state=Minnesota&population=106769");
        output = runInServlet("functionName=ADD&city=Stewartville&state=Minnesota&population=5916");
        output = runInServlet("functionName=ADD&city=Byron&state=Minnesota&population=4914");

        // remove
        output = runInServlet("functionName=REMOVE&city=Stewartville");
        if (output.indexOf("Successfully performed REMOVE with output: {city=Stewartville, population=5916, state=Minnesota}") < 0)
            throw new Exception("Did not report entry removed. Output: " + output);

        // attempt removal of something that doesn't exist
        output = runInServlet("functionName=REMOVE&city=Stewartville");
        if (output.indexOf("Successfully performed REMOVE") >= 0)
            throw new Exception("Entry should not have been present to remove. Output: " + output);
    }

    @Test
    @Mode(FULL)
    public void testMessageDrivenBean() throws Exception {

        restartWithNewConfig("server-with-mdb.xml");

        StringBuilder output = runInServlet("functionName=ADD&county=Olmsted&state=Minnesota&population=147066&area=654.5");
        if (output.indexOf("Successfully performed ADD with output: {area=654.5, county=Olmsted, population=147066, state=Minnesota}") < 0)
            throw new Exception("Did not report entry added. Output: " + output);

        // search messages log for MDB output
        final String expectedMsg = "BValMessageDrivenBean.onMessage record = \\{area=654.5, county=Olmsted, population=147066, state=Minnesota\\}";
        String msg = server.waitForStringInLog(expectedMsg);
        assertNotNull("Expected to find message: " + expectedMsg, msg);
    }

    @Test
    @ExpectedFFDC({ "javax.validation.ConstraintViolationException",
                    "com.ibm.wsspi.injectionengine.InjectionException",
                    "javax.servlet.UnavailableException"
    })
    @Mode(FULL)
    public void testInvalidActivationSpec() throws Exception {

        restartWithNewConfig("server-invalid-interaction-spec.xml");

        try {
            runInServlet("functionName=ADD&county=Olmsted&state=Minnesota&population=147066&area=654.5");
            fail("Unexpected success in error path, expecting servlet call to fail and get a J2CA0238E");
        } catch (Exception e) {

            if (server.waitForStringInLog("J2CA0238E") == null) {
                fail("Didn't find expected message: J2CA0238E");
            }
            //Else pass
        }

        server.stopServer("(?s)J2CA0238E.*com.ibm.bval.jca.adapter.InteractionSpecImpl", // multiline match because some translations split this message into multiple lines
                          "SRVE0319E(?=.*eis/iSpec_ADD)(?=.*javax.resource.cci.InteractionSpec)",
                          "CWNEN1006E(?=.*eis/iSpec_ADD)(?=.*javax.resource.cci.InteractionSpec)");
    }

    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.injectionengine.InjectionException",
                    "javax.servlet.UnavailableException"
    })
    @Mode(FULL)
    public void testInvalidConnectionFactory() throws Exception {

        restartWithNewConfig("server-invalid-cf.xml");

        try {
            runInServlet("functionName=ADD&city=Rochester&state=Minnesota&population=106769");
            fail("Unexpected success in error path, expecting servlet call to fail and get a J2CA0238E");
        } catch (Exception e) {
            if (server.waitForStringInLog("J2CA0238E") == null) {
                fail("Didn't find expected message: J2CA0238E");
            }
        }

        server.stopServer("(?s)J2CA0238E.*com.ibm.bval.jca.adapter.ManagedConnectionFactoryImpl", // multiline match because some translations split this message into multiple lines
                          "CWNEN1006E(?=.*eis/conFactory)(?=.*javax.resource.cci.ConnectionFactory)");
    }

    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.injectionengine.InjectionException",
                    "javax.servlet.UnavailableException"
    })
    @Mode(FULL)
    public void testInvalidResourceAdapter() throws Exception {

        restartWithNewConfig("server-invalid-ra.xml");

        try {
            runInServlet("functionName=FIND&capital=Saint%20Paul");
            fail("Unexpected success in error path, expecting servlet call to fail and get a J2CA0238E");
        } catch (FileNotFoundException e) {
            if (server.waitForStringInLog("J2CA0238E") == null) {
                fail("Didn't find expected message: J2CA0238E");
            }
        }

        server.stopServer("(?s)J2CA0238E.*com.ibm.bval.jca.adapter.ResourceAdapterImpl", // multiline match because some translations split this message into multiple lines
                          "CWWKE0701E",
                          "CWWKE0700W",
                          "CWNEN1006E(?=.*eis/conFactory)(?=.*javax.resource.cci.ConnectionFactory)");
    }
}
