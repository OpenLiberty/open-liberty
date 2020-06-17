/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.GreeterGrpc.GreeterBlockingStub;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;

@RunWith(FATRunner.class)
public class ServiceConfigTests extends FATServletClient {

    protected static final Class<?> c = ServiceConfigTests.class;

    private static final Logger LOG = Logger.getLogger(c.getName());

    public static final int STARTUP_TIMEOUT = 120 * 1000; // 120 seconds

    private static final String DEFAULT_CONFIG_FILE = "grpc.server.xml";

    private static final String GRPC_BAD_TARGET = "grpc.server.bad.target.xml";
    private static final String GRPC_ELEMENT = "grpc.server.grpc.element.xml";
    private static final String GRPC_INVALID_MAX_MSG_SIZE = "grpc.server.invalid.max.msg.size.xml";
    private static final String GRPC_MAX_MESSAGE = "grpc.server.max.msg.size.xml";
    private static final String GRPC_SMALL_MAX_MESSAGE = "grpc.server.small.max.msg.size.xml";
    private static final String GRPC_WILDCARD_TARGET = "grpc.server.wildcard.target.xml";

    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    static ManagedChannel worldChannel;
    private static GreeterBlockingStub worldServiceBlockingStub;

    private static String hws = new String("HelloWorldService.war");

    @Rule
    public TestName name = new TestName();

    @Server("GrpcServerOnly")
    public static LibertyServer grpcServer;

    @BeforeClass
    public static void setUp() throws Exception {
        grpcServer.startServer(ServiceSupportTests.class.getSimpleName() + ".log");

        LOG.info("testAddGrpcElement() : add HelloWorldService to the server if not already present.");
        // add all classes from com.ibm.ws.grpc.fat.helloworld.service and io.grpc.examples.helloworld
        // to a new app HelloWorldService.war
        ShrinkHelper.defaultDropinApp(grpcServer, hws,
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        worldChannel = ManagedChannelBuilder.forAddress(grpcServer.getHostname(), grpcServer.getHttpDefaultPort()).usePlaintext().build();
        worldServiceBlockingStub = GreeterGrpc.newBlockingStub(worldChannel);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        worldChannel.shutdownNow();
        grpcServer.stopServer();
    }

    /**
     * This method is used to set the server.xml
     */
    private static void setServerConfiguration(LibertyServer server,
                                               String serverXML) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            // Update server.xml
            Log.info(c, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile(serverXML);
            server.waitForStringInLog("CWWKG0017I");
            serverConfigurationFile = serverXML;
        }
    }

    /**
     * Add a new <grpc/> element and make sure it's applied
     *
     * @throws Exception
     *
     **/
    @Test
    public void testAddGrpcElement() throws Exception {

        LOG.info("testAddGrpcElement() : update the server.xml file to one with a </grpc> element.");

        // Update to a config file with a <grpc> element
        setServerConfiguration(grpcServer, GRPC_ELEMENT);

        String appStarted = grpcServer.waitForStringInLogUsingMark("CWWKG0017I: The server configuration was successfully updated", STARTUP_TIMEOUT);
        if (appStarted == null) {
            Assert.fail(c + ": server.xml with <grpc> element failed to update within " + STARTUP_TIMEOUT + "ms");
        }

    }

    /**
     * Update an existing <grpc> element, eg. add a new param like maxInboundMessageSize
     *
     * @throws Exception
     *
     **/
    @Test
    public void testUpdateGrpcParam() throws Exception {

        LOG.info("testUpdateGrpcParam() : update the server.xml file to one with a </grpc> element with maxMessageSize.");

        // Update to a config file with a <grpc> element with maxMessageSize included
        setServerConfiguration(grpcServer, GRPC_MAX_MESSAGE);

        String appStarted = grpcServer.waitForStringInLogUsingMark("CWWKG0017I: The server configuration was successfully updated", STARTUP_TIMEOUT);
        if (appStarted == null) {
            Assert.fail(c + ": server.xml with <grpc> element plus maxMessageSize failed to update within " + STARTUP_TIMEOUT + "ms");
        }

    }

    /**
     *
     * Remove an existing <grpc/> element
     *
     * @throws Exception
     *
     **/
    @Test
    public void testRemoveGrpcElement() throws Exception {

        LOG.info("testRemoveGrpcElement() : update the server.xml file to one with no </grpc> element.");

        // Update to a config file with a <grpc> element
        setServerConfiguration(grpcServer, DEFAULT_CONFIG_FILE);

        String appStarted = grpcServer.waitForStringInLogUsingMark("CWWKG0017I: The server configuration was successfully updated", STARTUP_TIMEOUT);
        if (appStarted == null) {
            Assert.fail(c + ": server.xml with no <grpc> element failed to update within " + STARTUP_TIMEOUT + "ms");
        }

    }

    /**
     * Validate that * matches all services and apps on the server
     *
     * @throws Exception
     *
     **/
    @Test
    public void testServiceTargetWildcard() throws Exception {
        LOG.info("testServiceTargetWildcard() : update the server.xml file to one with </grpc> wildard target.");

        // Update to a config file with a <grpc> element
        setServerConfiguration(grpcServer, GRPC_WILDCARD_TARGET);

        String appStarted = grpcServer.waitForStringInLogUsingMark("CWWKG0017I: The server configuration was successfully updated", STARTUP_TIMEOUT);
        if (appStarted == null) {
            Assert.fail(c + ": server.xml with <grpc> wildcard target failed to update within " + STARTUP_TIMEOUT + "ms");
        }

        // Send a request to the HelloWorld service and check for a response
        HelloRequest person = HelloRequest.newBuilder().setName("Kevin").build();
        HelloReply greeting = worldServiceBlockingStub.sayHello(person);
        //Make sure the reply has Kevin in it
        assertTrue(greeting.getMessage().contains("Kevin"));

    }

    /**
     * Set a target that matches no existing service paths
     *
     * @throws Exception
     *
     **/
    @Test
    public void testServiceTargetNoMatch() throws Exception {
        LOG.info("testServiceTargetNoMatch() : update the server.xml file to one with a </grpc> target that matches nothing.");

        // Update to a config file with a <grpc> element
        setServerConfiguration(grpcServer, GRPC_BAD_TARGET);

        String appStarted = grpcServer.waitForStringInLogUsingMark("CWWKG0017I: The server configuration was successfully updated", STARTUP_TIMEOUT);
        if (appStarted == null) {
            Assert.fail(c + ": server.xml with a <grpc> that matches nothing failed to update within " + STARTUP_TIMEOUT + "ms");
        }

        try {
            // Send a request to the HelloWorld service and check for a response
            // This should fail
            HelloRequest person = HelloRequest.newBuilder().setName("Kevin").build();
            HelloReply greeting = worldServiceBlockingStub.sayHello(person);
            //Make sure the reply has Kevin in it
            assertTrue(greeting.getMessage().contains("Kevin"));
        } catch (Exception e) {
            LOG.info(e.getStackTrace().toString());
        }

    }

    /**
     * Test a specific match, eg. that helloworld.Greeter matches the helloworld app
     *
     * @throws Exception
     *
     **/
    @Test
    public void testServiceTargetSpecificMatch() throws Exception {

        LOG.info("testServiceTargetSpecificMatch() : update the server.xml file to one with a </grpc> specific target.");

        // Update to a config file with a <grpc> element
        setServerConfiguration(grpcServer, GRPC_ELEMENT);

        String appStarted = grpcServer.waitForStringInLogUsingMark("CWWKG0017I: The server configuration was successfully updated", STARTUP_TIMEOUT);
        if (appStarted == null) {
            Assert.fail(c + ": server.xml with <grpc> specific target failed to update within " + STARTUP_TIMEOUT + "ms");
        }

    }

    /**
     * Test an invalid setting (less than 0)
     *
     * @throws Exception
     *
     **/
    @Test
    public void testInvalidMaxInboundMessageSize() throws Exception {

        LOG.info("testInvalidMaxInboundMessageSize() : update the server.xml file to one with an </grpc> with an invalid maxInboundMessageSize.");

        // Update to a config file with a <grpc> element
        setServerConfiguration(grpcServer, GRPC_INVALID_MAX_MSG_SIZE);

        String appStarted = grpcServer.waitForStringInLogUsingMark("CWWKG0017I: The server configuration was successfully updated", STARTUP_TIMEOUT);
        if (appStarted == null) {
            Assert.fail(c + ": server.xml with <grpc> invalid maxInboundMessageSize failed to update within " + STARTUP_TIMEOUT + "ms");
        }

        String warningMsg = grpcServer.waitForStringInLogUsingMark("grpcTarget maxInboundMessageSize is invalid", STARTUP_TIMEOUT);
        if (warningMsg == null) {
            Assert.fail(c + ": Invalid maxInboundMessageSize failed to report a warning in " + STARTUP_TIMEOUT + "ms");
        }
    }

    /**
     * Test a very small setting, send a gRPC message exceeding the value, and check the server error message
     *
     * @throws Exception
     *
     **/
    @Test
    public void testSmallMaxInboundMessageSize() throws Exception {

        LOG.info("testSmallMaxInboundMessageSize() : update the server.xml file to one with an </grpc> with an very small maxInboundMessageSize.");

        // Update to a config file with a <grpc> element
        setServerConfiguration(grpcServer, GRPC_SMALL_MAX_MESSAGE);

        String appStarted = grpcServer.waitForStringInLogUsingMark("CWWKG0017I: The server configuration was successfully updated", STARTUP_TIMEOUT);
        if (appStarted == null) {
            Assert.fail(c + ": server.xml with <grpc> very small maxInboundMessageSize failed to update within " + STARTUP_TIMEOUT + "ms");
        }

        try {
            // Send a request to the HelloWorld service and check for a response
            // This should fail because the message is larger than 6
            HelloRequest person = HelloRequest.newBuilder().setName("Kevin").build();
            HelloReply greeting = worldServiceBlockingStub.sayHello(person);
            //Make sure the reply has Kevin in it
            assertTrue(greeting.getMessage().contains("Kevin"));
        } catch (Exception e) {
            LOG.info(e.getStackTrace().toString());
        }

    }
}
