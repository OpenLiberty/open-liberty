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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.grpc.fat.beer.service.Beer;
import com.ibm.ws.grpc.fat.beer.service.BeerResponse;
import com.ibm.ws.grpc.fat.beer.service.BeerServiceGrpc;
import com.ibm.ws.grpc.fat.beer.service.BeerServiceGrpc.BeerServiceBlockingStub;

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
public class ServiceConfigTests extends FATServletClient {

    protected static final Class<?> c = ServiceConfigTests.class;

    private static final Logger LOG = Logger.getLogger(c.getName());

    public static final int STARTUP_TIMEOUT = 120 * 1000; // 120 seconds
    public static final int SHORT_TIMEOUT = 500; // .5 second

    private static final String DEFAULT_CONFIG_FILE = "grpc.server.xml";

    private static final String GRPC_BAD_TARGET = "grpc.server.bad.target.xml";
    private static final String GRPC_ELEMENT = "grpc.server.grpc.element.xml";
    private static final String GRPC_INTERCEPTOR = "grpc.server.grpc.interceptor.xml";
    private static final String GRPC_INVALID_MAX_MSG_SIZE = "grpc.server.invalid.max.msg.size.xml";
    private static final String GRPC_MAX_MESSAGE_SPECIFIC_TARGET = "grpc.server.small.max.msg.size.and.specific.target.xml";
    private static final String GRPC_SMALL_MAX_MESSAGE = "grpc.server.small.max.msg.size.xml";

    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    static ManagedChannel worldChannel;
    private static GreeterBlockingStub worldServiceBlockingStub;

    private static String hws = new String("HelloWorldService.war");

    private static final Set<String> appName = Collections.singleton("HelloWorldService");
    private static final Set<String> appNames = new HashSet<>(Arrays.asList("HelloWorldService", "FavoriteBeerService"));

    @Rule
    public TestName name = new TestName();

    @Server("GrpcServerOnly")
    public static LibertyServer grpcServer;

    @BeforeClass
    public static void setUp() throws Exception {
        
        grpcServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        grpcServer.startServer(ServiceConfigTests.class.getSimpleName() + ".log");

        LOG.info("ServiceConfigTests : setUp() : add helloWorldService  app");
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
        // The testInvalidMaxInboundMessageSize() test generates this log message, don't flag it as an error
        // CWWKT0203E: The maxInboundMessageSize -1 is not valid. Sizes must greater than 0.
        // SRVE9015E: This occurs during testInvalidMaxInboundMessageSize where the protocol error prevents the response body being accessed
        grpcServer.stopServer("CWWKT0203E", "SRVE9015E");
    }

    /**
     * Add a new <grpc/> element and make sure it's applied
     * The original server.xml enables the grpc feature, but has no grpc element.
     * Update the server with a server.xml that has a grpc element, make sure no errors, send a request.
     *
     * <grpc target="helloworld.Greeter" />
     *
     * @throws Exception
     *
     **/
    @Test
    public void testAddGrpcElement() throws Exception {

        LOG.info("ServiceConfigTests : testAddGrpcElement() : update the server.xml file to one with a <grpc> element.");

        // Update to a config file with a <grpc> element
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_ELEMENT, appName, LOG);

        // Send a request to the HelloWorld service and check for a response
        HelloRequest person = HelloRequest.newBuilder().setName("Holly").build();
        HelloReply greeting = worldServiceBlockingStub.sayHello(person);
        //Make sure the reply has Holly in it
        assertTrue(greeting.getMessage().contains("Holly"));

    }

    /**
     * Update an existing <grpc> element, eg. add a new param to <gprc>.
     *
     * This test adds a grpc element with a grpc interceptor. Make sure the interceptor does not run
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
    public void testUpdateGrpcParam() throws Exception {

        LOG.info("ServiceConfigTests : testUpdateGrpcParam() : update the server.xml file to one with a </grpc> element with no interceptor");

        // Update to a config file with a <grpc> element with no interceptor
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_ELEMENT, appName, LOG);

        // Send a request to the HelloWorld service and check for a response
        HelloRequest person = HelloRequest.newBuilder().setName("Kevin").build();
        HelloReply greeting = worldServiceBlockingStub.sayHello(person);
        //Make sure the reply has Kevin in it
        assertTrue(greeting.getMessage().contains("Kevin"));
        //Make sure the Interceptor was not called and did not logged a message
        String interceptorHasRun = grpcServer.verifyStringNotInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor has been invoked!",
                                                                            SHORT_TIMEOUT);
        if (interceptorHasRun != null) {
            Assert.fail(c + ": server.xml with <grpc> element plus interceptor ran when it should not have in " + SHORT_TIMEOUT + "ms");
        }

        // Update to a config file with a <grpc> element with Interceptor included
        LOG.info("ServiceConfigTests : testUpdateGrpcParam() : update the server.xml file to one with a </grpc> element with an interceptor");
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_INTERCEPTOR, appName, LOG);

        // Send a request to the HelloWorld service and check for a response
        person = HelloRequest.newBuilder().setName("Millie").build();
        greeting = worldServiceBlockingStub.sayHello(person);
        //Make sure the reply has Millie in it
        assertTrue(greeting.getMessage().contains("Millie"));
        //Make sure the Interceptor was called and logged a message
        interceptorHasRun = grpcServer.waitForStringInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor has been invoked!", STARTUP_TIMEOUT);
        if (interceptorHasRun == null) {
            Assert.fail(c + ": interceptor did not print message to the server log as expected");
        }

    }

    /**
     *
     * Remove an existing <grpc/> element
     * Start with a config file with a too small msg size, look for failure.
     * <grpc target="*" maxInboundMessageSize="1"/>
     *
     * Then update to a config file with no <grpc> element, so msg size should revert to default, look for success.
     * no grpc element in server.xml
     *
     * @throws Exception
     *
     **/
    @Test
    public void testRemoveGrpcElement() throws Exception {

        LOG.info("ServiceConfigTests : testRemoveGrpcElement() : update the server.xml file to one with a small grpc message size of 1");

        // Update to a config file with a <grpc> element with a small message size, send msg, look for failure
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_SMALL_MAX_MESSAGE, appName, LOG);

        boolean testPassed = false;
        try {
            // Send a request to the HelloWorld service and check for a response
            // This should fail because the message is larger than 1
            HelloRequest person = HelloRequest.newBuilder().setName("Kevin").build();
            @SuppressWarnings("unused")
            HelloReply greeting = worldServiceBlockingStub.sayHello(person);
        } catch (io.grpc.StatusRuntimeException e) {
            testPassed = true;
            LOG.info("ServiceConfigTests : testRemoveGrpcElement(): Max message size set to 1 and message is > 1 byte");
        }

        // Update to a config file with no <grpc> element, do same test, it should work
        LOG.info("ServiceConfigTests : testRemoveGrpcElement() : update the server.xml file to one with no grpc message size.");
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, DEFAULT_CONFIG_FILE, appName, LOG);

        // Send a request to the HelloWorld service, this should work
        HelloRequest person = HelloRequest.newBuilder().setName("Holly").build();
        HelloReply greeting = worldServiceBlockingStub.sayHello(person);
        LOG.info("ServiceConfigTests : testRemoveGrpcElement() : greeting:" + greeting);
        assertTrue(greeting.getMessage().contains("Holly") && testPassed);

    }

    /**
     * Validate that * matches all services and apps on the server
     *
     * Start two different services. Load a server.xml with a wildcard target and a small max message size.
     * Verify that both services give errors when a message is sent to them.
     *
     * <grpc target="*" maxInboundMessageSize="1"/>
     *
     * @throws Exception
     *
     **/

    @Test
    public void testServiceTargetWildcard() throws Exception {

        ManagedChannel beerChannel;
        BeerServiceBlockingStub beerServiceBlockingStub;

        LOG.info("ServiceConfigTests : testServiceTargetWildcard() : update the server.xml file to one with </grpc> wildard target.");

        grpcServer.setMarkToEndOfLog();

        // add all classes from com.ibm.ws.grpc.fat.beer.service and com.ibm.ws.grpc.fat.beer
        // to a new app FavoriteBeerService.war; disable validation
        WebArchive fbsApp = ShrinkHelper.buildDefaultApp("FavoriteBeerService",
                                                         "com.ibm.ws.grpc.fat.beer.service",
                                                         "com.ibm.ws.grpc.fat.beer");
        DeployOptions[] options = new DeployOptions[] { DeployOptions.DISABLE_VALIDATION };
        ShrinkHelper.exportDropinAppToServer(grpcServer, fbsApp, options);

        LOG.info("ServiceConfigTests : testServiceTargetWildcard() : dropped the beer app into dropins.");
        // Make sure the beer service has started
        String appStarted = grpcServer.waitForStringInLogUsingMark("CWWKT0201I.*FavoriteBeerService", STARTUP_TIMEOUT);
        if (appStarted == null) {
            Assert.fail(c + ": application " + "FavoriteBeerService" + " failed to start within " + STARTUP_TIMEOUT + "ms");
        }
        beerChannel = ManagedChannelBuilder.forAddress(grpcServer.getHostname(), grpcServer.getHttpDefaultPort()).usePlaintext().build();
        beerServiceBlockingStub = BeerServiceGrpc.newBlockingStub(beerChannel);

        // Update to a config file with a <grpc target="*" maxInboundMessageSize="1"/>
        // This should apply max msg size of 1 to both services
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_SMALL_MAX_MESSAGE, appNames, LOG);

        boolean test1Passed = false;
        try {
            // Send a request to the HelloWorld service and check for a response
            // This should fail because the message is larger than 1
            HelloRequest person = HelloRequest.newBuilder().setName("Kevin").build();
            HelloReply greeting = worldServiceBlockingStub.sayHello(person);
            LOG.info("ServiceConfigTests : testServiceTargetWildcard(): greeter returned " + greeting.getMessage());
        } catch (io.grpc.StatusRuntimeException e) {
            test1Passed = true;
            LOG.info("ServiceConfigTests : testServiceTargetWildcard(): first service test passed");
        }

        // Send a message to the second service, expect error
        boolean test2Passed = false;
        // Send a request to the beer service and check for a response
        // This should fail because the message is larger than 1
        try {
            Beer newBeer1 = Beer.newBuilder().setBeerName("Citraquenchl").setBeerMaker("Heist").setBeerTypeValue(0).setBeerRating((float) 4.3).build();
            BeerResponse rsp = beerServiceBlockingStub.addBeer(newBeer1);
            LOG.info("ServiceConfigTests : testServiceTargetWildcard(): beer rsp returned " + rsp.getDone());
        } catch (Exception e) {
            test2Passed = true;
            LOG.info("ServiceConfigTests : testServiceTargetWildcard(): second service test passed");
        }

        // Stop only the FavoriteBeerService grpc application
        GrpcTestUtils.stopGrpcService(beerChannel);
        LOG.info("ServiceConfigTests : testSingleWarWithGrpcService() : Stop the FavoriteBeerService application and remove it from dropins.");
        grpcServer.removeAndStopDropinsApplications("FavoriteBeerService.war");

        // removeAndStop above actually just renames the file, so really delete so the next tests have a clean slate
        grpcServer.deleteFileFromLibertyServerRoot("/FavoriteBeerService");

        assertTrue(test1Passed & test2Passed);

    }

    /**
     * Set a target that matches no existing service paths
     *
     * <grpc target="foo.Fighter" maxInboundMessageSize="-1"/>
     *
     * @throws Exception
     *
     **/
    @Test
    public void testServiceTargetNoMatch() throws Exception {
        LOG.info("ServiceConfigTests : testServiceTargetNoMatch() : update the server.xml file to one with a </grpc> target that matches nothing.");

        // Update to a config file with a <grpc> element with Foo.fighter target and an invalid msg size
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_BAD_TARGET, appName, LOG);

        // Send a request to a different service, the HelloWorld service, and check for a response
        // This should work, since the invalid msg size is set on the invalid target
        HelloRequest person = HelloRequest.newBuilder().setName("Kevin").build();
        HelloReply greeting = worldServiceBlockingStub.sayHello(person);
        assertTrue(greeting.getMessage().contains("Kevin"));

    }

    /**
     * Test a specific match, eg. that helloworld.Greeter matches the helloworld app
     *
     * Start two different services. Load a server.xml with a helloWorld target and a small max message size.
     * Verify that the specific service target gives an error while the other service works with no errors.
     *
     * <grpc "helloworld.Greeter" maxInboundMessageSize="1"/>
     *
     * @throws Exception
     *
     **/
    @Test
    public void testServiceTargetSpecificMatch() throws Exception {

        LOG.info("ServiceConfigTests : testServiceTargetSpecificMatch() : update the server.xml file to one with a </grpc> specific target.");

        grpcServer.setMarkToEndOfLog();

        ManagedChannel beerChannel;
        BeerServiceBlockingStub beerServiceBlockingStub;

        // The helloworld app is already in dropins, add the beer service app and fire it up
        // add all classes from com.ibm.ws.grpc.fat.beer.service and com.ibm.ws.grpc.fat.beer
        // to a new app FavoriteBeerService.war; disable validation
        WebArchive fbsApp = ShrinkHelper.buildDefaultApp("FavoriteBeerService",
                                                         "com.ibm.ws.grpc.fat.beer.service",
                                                         "com.ibm.ws.grpc.fat.beer");
        DeployOptions[] options = new DeployOptions[] { DeployOptions.DISABLE_VALIDATION };
        ShrinkHelper.exportDropinAppToServer(grpcServer, fbsApp, options);

        LOG.info("ServiceConfigTests : testServiceTargetSpecificMatch() : dropped the beer app into dropins.");
        // Make sure the beer service has started
        String appStarted = grpcServer.waitForStringInLogUsingMark("CWWKT0201I.*FavoriteBeerService", STARTUP_TIMEOUT);
        if (appStarted == null) {
            Assert.fail(c + ": application " + "FavoriteBeerService" + " failed to start within " + STARTUP_TIMEOUT + "ms");
        }
        beerChannel = ManagedChannelBuilder.forAddress(grpcServer.getHostname(), grpcServer.getHttpDefaultPort()).usePlaintext().build();
        beerServiceBlockingStub = BeerServiceGrpc.newBlockingStub(beerChannel);

        // Update to a config file with a <grpc> element that has a specific target
        // make sure to pass in both appNames so that the test will wait until they're both up
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_MAX_MESSAGE_SPECIFIC_TARGET, appNames, LOG);

        // Send a message to the first service, expect error
        boolean test1Passed = false;
        try {
            // Send a request to the HelloWorld service and check for a response
            // This should fail because the message is larger than 1
            HelloRequest person = HelloRequest.newBuilder().setName("Kevin").build();
            @SuppressWarnings("unused")
            HelloReply greeting = worldServiceBlockingStub.sayHello(person);
        } catch (io.grpc.StatusRuntimeException e) {
            test1Passed = true;
            LOG.info("ServiceConfigTests : testServiceTargetSpecificMatch(): first test failed as expected");
        }

        // Send a message to the second service, expect pass
        // Send a request to the beer service and check for a response
        // work because this target has no restrictions
        Beer newBeer1 = Beer.newBuilder().setBeerName("Citraquenchl").setBeerMaker("Heist").setBeerTypeValue(0).setBeerRating((float) 4.3).build();
        BeerResponse rsp = beerServiceBlockingStub.addBeer(newBeer1);

        assertTrue(test1Passed && rsp.getDone());

        // Stop only the FavoriteBeerService grpc application and remove the app from dropins
        GrpcTestUtils.stopGrpcService(beerChannel);
        grpcServer.removeAndStopDropinsApplications("FavoriteBeerService.war");
        // removeAndStop above actually just renames the file, so really delete so the next tests have a clean slate
        grpcServer.deleteFileFromLibertyServerRoot("/FavoriteBeerService");

        assertTrue(test1Passed & rsp.getDone());

    }

    /**
     * Test an invalid setting (less than 0)
     *
     * @throws Exception
     *
     **/
    @Test
    public void testInvalidMaxInboundMessageSize() throws Exception {

        LOG.info("ServiceConfigTests : testInvalidMaxInboundMessageSize() : update the server.xml file to one with an </grpc> with an invalid maxInboundMessageSize.");

        // Update to a config file with a <grpc> element with a max msg size of -1
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_INVALID_MAX_MSG_SIZE, appName, LOG);

        String warningMsg = grpcServer.waitForStringInLogUsingMark("CWWKT0203E: The maxInboundMessageSize -1 is not valid. Sizes must greater than 0.", STARTUP_TIMEOUT);
        if (warningMsg == null) {
            Assert.fail(c + ": Invalid maxInboundMessageSize failed to report an error in " + STARTUP_TIMEOUT + "ms");
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

        LOG.info("ServiceConfigTests : testSmallMaxInboundMessageSize() : update the server.xml file to one with an </grpc> with an very small maxInboundMessageSize.");

        // Update to a config file with a <grpc> element
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcServer, serverConfigurationFile, GRPC_SMALL_MAX_MESSAGE, appName, LOG);

        boolean testPassed = false;
        try {
            // Send a request to the HelloWorld service and check for a response
            // This should fail because the message is larger than 1
            HelloRequest person = HelloRequest.newBuilder().setName("Kevin").build();
            HelloReply greeting = worldServiceBlockingStub.sayHello(person);
            LOG.info("ServiceConfigTests : testSmallMaxInboundMessageSize(): greeting returned " + greeting.getMessage());
            //Make sure the reply has Kevin in it
            //assertTrue(greeting.getMessage().contains("Kevin"));
        } catch (io.grpc.StatusRuntimeException e) {
            testPassed = true;
            LOG.info("ServiceConfigTests : testSmallMaxInboundMessageSize(): Max message size set to 1 and message is > 1 byte");
        }

        assertTrue(testPassed);

    }
}
