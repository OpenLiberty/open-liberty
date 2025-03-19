/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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

package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.GreeterGrpc.GreeterBlockingStub;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ServiceInterceptorTests extends FATServletClient {

    protected static final Class<?> c = ServiceInterceptorTests.class;

    private static final Logger LOG = Logger.getLogger(c.getName());

    public static final int STARTUP_TIMEOUT = 120 * 1000; // 120 seconds
    public static final int SHORT_TIMEOUT = 500; // .5 seconds

    private static final String DEFAULT_CONFIG_FILE = "grpc.server.xml";
    private static final String GRPC_ELEMENT = "grpc.server.grpc.element.xml";
    private static final String GRPC_INTERCEPTOR = "grpc.server.grpc.interceptor.xml";
    private static final String GRPC_MULTIPLE_INTERCEPTOR = "grpc.server.multiple.interceptor.xml";
    private static final String GRPC_INVALID_INTERCEPTOR = "grpc.server.invalid.interceptor.xml";

    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    static ManagedChannel worldChannel;
    private static GreeterBlockingStub worldServiceBlockingStub;

    private static String hws = new String("HelloWorldService.war");

    private static final Set<String> appName = Collections.singleton("HelloWorldService");

    @Rule
    public TestName name = new TestName();

    @Server("GrpcServerOnly")
    public static LibertyServer grpcServer;

    @BeforeClass
    public static void setUp() throws Exception {

        grpcServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        grpcServer.startServer(ServiceInterceptorTests.class.getSimpleName() + ".log");

        LOG.info("ServiceInterceptorTests : setUp() : add helloWorldService  app");
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
        // Setting serverConfigurationFile to null forces a server.xml update (when GrpcTestUtils.setServerConfiguration() is first called) on the repeat run
        // If not set to null, test failures may occur (since the incorrect server.xml could be used)
        serverConfigurationFile = null;

        GrpcTestUtils.stopGrpcService(worldChannel);
        grpcServer.stopServer("CWWKT0202W");
    }

    /**
     * Test a single grpc service interceptor
     *
     * This test adds a grpc element without a grpc interceptor and then updates
     * to a grpc element with a grpc interceptor. Make sure the interceptor does not run
     * when not configured, and make sure it runs when configured.
     *
     * <grpc target="helloworld.Greeter" serverInterceptors="com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor"/>
     *
     * The simple interceptor prints a message to the log.
     *
     * @throws Exception
     *
     **/
    @Test
    public void testSingleServerInterceptor() throws Exception {

        // Update to a config file with a <grpc> element with no interceptor
        LOG.info("ServiceInterceptorTests : testSingleServerInterceptor() : update the server.xml file to one with a </grpc> element with no interceptor");
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_ELEMENT, appName, LOG);

        // Send a request to the HelloWorld service and check for a response
        HelloRequest person = HelloRequest.newBuilder().setName("Leigh").build();
        HelloReply greeting = worldServiceBlockingStub.sayHello(person);

        //Make sure the reply has Leigh in it
        assertTrue(greeting.getMessage().contains("Leigh"));
        //Make sure the Interceptor was not called and did not log a message
        String interceptorHasRun = grpcServer.verifyStringNotInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor has been invoked!",
                                                                            SHORT_TIMEOUT);
        if (interceptorHasRun != null) {
            Assert.fail(c + ": server.xml with <grpc> element no interceptor ran when it should not have in " + SHORT_TIMEOUT + "ms");
        }

        // Update to a config file with a <grpc> element with Interceptor included
        LOG.info("ServiceInterceptorTests : testSingleServerInterceptor() : update the server.xml file to one with a </grpc> element with an interceptor");
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_INTERCEPTOR, appName, LOG);

        // Send a request to the HelloWorld service and check for a response
        person = HelloRequest.newBuilder().setName("Lisa").build();
        greeting = worldServiceBlockingStub.sayHello(person);
        //Make sure the reply has Lisa in it
        assertTrue(greeting.getMessage().contains("Lisa"));

        //Make sure the Interceptor was called and logged a message
        interceptorHasRun = grpcServer.waitForStringInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor has been invoked!", STARTUP_TIMEOUT);
        if (interceptorHasRun == null) {
            Assert.fail(c + " testSingleServerInterceptor() : interceptor did not print message to the server log as expected");
        }

    }

    /**
     * Test a multiple grpc service interceptors
     *
     * This test adds a grpc element with multiple grpc interceptors. Make sure they both run.
     *
     * <grpc target="helloworld.Greeter" serverInterceptors="com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor"/>
     *
     * These simple interceptors prints a message to the log.
     *
     * @throws Exception
     *
     **/
    @Test
    public void testMultipleServerInterceptors() throws Exception {

        // Update to a config file with a <grpc> element with multiple interceptors
        LOG.info("ServiceInterceptorTests : testMultipleServerInterceptors() : update the server.xml file to one with a </grpc> element with multiple interceptors");
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_MULTIPLE_INTERCEPTOR, appName, LOG);

        // Send a request to the HelloWorld service and check for a response
        HelloRequest person = HelloRequest.newBuilder().setName("Lynnette").build();
        HelloReply greeting = worldServiceBlockingStub.sayHello(person);
        //Make sure the reply has Lynnette in it
        assertTrue(greeting.getMessage().contains("Lynnette"));
        //Make sure the Interceptor 1 and Interceptor 2 were both called and logged a message
        String interceptor1HasRun = grpcServer.waitForStringInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor has been invoked!",
                                                                           STARTUP_TIMEOUT);
        String interceptor2HasRun = grpcServer.waitForStringInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor2 has been invoked!",
                                                                           STARTUP_TIMEOUT);

        if (interceptor1HasRun == null || interceptor2HasRun == null) {
            Assert.fail(c + ": server.xml with muitiple service interceptors failed.");
        }
    }

    /**
     * Test an invalid grpc service interceptor
     *
     * This test adds a grpc element with a non-existent grpc interceptor. Check for a log error.
     *
     * <grpc target="helloworld.Greeter" serverInterceptors="com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor"/>
     *
     * @throws Exception
     *
     **/
    @Test
    @ExpectedFFDC({ "java.lang.ClassNotFoundException" })
    public void testInvalidServerInterceptor() throws Exception {

        LOG.info("ServiceInterceptorTests : testInvalidServerInterceptor() : update the server.xml file to one with a </grpc> element with an invalid interceptor");

        // Update to a config file with a <grpc> element with an invalid interceptor classname; skip validation
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_INVALID_INTERCEPTOR, appName, LOG, false);

        String interceptorError = grpcServer.waitForStringInLogUsingMark("CWWKT0202W: Could not load gRPC interceptor",
                                                                         STARTUP_TIMEOUT);

        if (interceptorError == null) {
            Assert.fail(c + ": Did not log grpc interceptor error msg as expected in " + STARTUP_TIMEOUT + "ms");
        }
    }

    /**
     * Test that interceptors defined in @GrpcService work correctly.
     *
     * @GrpcService(interceptors = { HelloWorldServerAnnotationInterceptor.class,
     *                           HelloWorldServerAnnotationInterceptor2.class })
     * @throws Exception
     *
     **/
    @Test
    public void testServerInterceptorAnnotations() throws Exception {

        // Update to a config file with a <grpc> element with no interceptor
        LOG.info("ServiceInterceptorTests : testServerInterceptorAnnotations() : update the server.xml file to one with a </grpc> element with no interceptor");
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_ELEMENT, appName, LOG);

        // Send a request to the HelloWorld service and check for a response
        HelloRequest person = HelloRequest.newBuilder().setName("Annotated").build();
        HelloReply greeting = worldServiceBlockingStub.sayHello(person);

        //Make sure the reply has Leigh in it
        assertTrue(greeting.getMessage().contains("Annotated"));

        //Make sure the Interceptor was called and logged a message
        String interceptor1HasRun = grpcServer.waitForStringInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerAnnotationInterceptor has been invoked!",
                                                                           STARTUP_TIMEOUT);
        String interceptor2HasRun = grpcServer.waitForStringInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerAnnotationInterceptor2 has been invoked!",
                                                                           STARTUP_TIMEOUT);
        if (interceptor1HasRun == null || interceptor2HasRun == null) {
            Assert.fail(c + " testServerInterceptorAnnotations() : interceptors did not print message to the server log as expected");
        }
    }

    /**
     * Test that interceptors defined in @GrpcService work correctly in conjunction with interceptors defined in server.xml
     *
     * @GrpcService(interceptors = { HelloWorldServerAnnotationInterceptor.class, HelloWorldServerAnnotationInterceptor2.class })
     *
     *                           <grpc target="helloworld.Greeter" serverInterceptors="com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor"/>
     * @throws Exception
     *
     **/
    @Test
    public void testMultipleServerInterceptorsAndAnnotations() throws Exception {

        // Update to a config file with a <grpc> element with multiple interceptors
        LOG.info("ServiceInterceptorTests : testMultipleServerInterceptorsAndAnnotations() : update the server.xml file to one with a </grpc> element with multiple interceptors");
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_MULTIPLE_INTERCEPTOR, appName, LOG);

        // Send a request to the HelloWorld service and check for a response
        HelloRequest person = HelloRequest.newBuilder().setName("Annotated").build();
        HelloReply greeting = worldServiceBlockingStub.sayHello(person);

        //Make sure the reply has Leigh in it
        assertTrue(greeting.getMessage().contains("Annotated"));

        //Make sure the Interceptor was called and logged a message
        String interceptor1HasRun = grpcServer.waitForStringInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerAnnotationInterceptor has been invoked!",
                                                                           STARTUP_TIMEOUT);
        String interceptor2HasRun = grpcServer.waitForStringInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerAnnotationInterceptor2 has been invoked!",
                                                                           STARTUP_TIMEOUT);
        //Make sure the Interceptor 1 and Interceptor 2 were both called and logged a message
        String interceptor3HasRun = grpcServer.waitForStringInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor has been invoked!",
                                                                           STARTUP_TIMEOUT);
        String interceptor4HasRun = grpcServer.waitForStringInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor2 has been invoked!",
                                                                           STARTUP_TIMEOUT);

        if (interceptor1HasRun == null || interceptor2HasRun == null || interceptor3HasRun == null || interceptor4HasRun == null) {
            Assert.fail(c + " testServerInterceptorAnnotations() : interceptors did not print message to the server log as expected");
        }
    }
}
