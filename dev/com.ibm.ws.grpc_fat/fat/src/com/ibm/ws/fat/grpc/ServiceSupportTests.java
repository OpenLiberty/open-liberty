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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.grpc.fat.beer.service.Beer;
import com.ibm.ws.grpc.fat.beer.service.BeerResponse;
import com.ibm.ws.grpc.fat.beer.service.BeerServiceGrpc;
import com.ibm.ws.grpc.fat.beer.service.BeerServiceGrpc.BeerServiceBlockingStub;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.GreeterGrpc.GreeterBlockingStub;

//Beer newBeer1 = new Beer("Citraquenchl","Heist",NEWENGLANDIPA, 4.3);
//Beer newBeer2 = new Beer("Snozzberry","Green Man Brewery",AMERICANWILDALE, 4.2);
//Beer newBeer3 = new Beer("Red Angel","Wicked Weed Brewing",AMERICANWILDALE, 4.4);
//Beer newBeer4 = new Beer("The Event Horizon","Olde Hickory Brewery",AMERICANIMPERIALSTOUT, 4.2);
//Beer newBeer5 = new Beer("Blurred Is The Word","Heist",NEWENGLANDIPA, 4.2);
//Beer newBeer6 = new Beer("Drunken Vigils Breakfast Stout","Southern Pines Brewing Company",AMERICANIMPERIALSTOUT, 4.4);

@RunWith(FATRunner.class)
public class ServiceSupportTests extends FATServletClient {

    protected static final Class<?> c = ServiceSupportTests.class;
    private static final long serialVersionUID = 1L;

    ManagedChannel beerChannel;
    private BeerServiceBlockingStub beerServiceBlockingStub;
    //private final BeerServiceStub asyncStub;
    ManagedChannel worldChannel;
    private GreeterBlockingStub worldServiceBlockingStub;

    @Rule
    public TestName name = new TestName();

    @Server("BeerServer")
    public static LibertyServer beerServer;

    @BeforeClass
    public static void setUp() throws Exception {
        // add all classes from com.ibm.ws.grpc.fat.beer.service and com.ibm.ws.grpc.fat.beer
        // to a new app FavoriteBeerService.war
        ShrinkHelper.defaultDropinApp(beerServer, "FavoriteBeerService.war",
                                      "com.ibm.ws.grpc.fat.beer.service",
                                      "com.ibm.ws.grpc.fat.beer");

        beerServer.startServer(ServiceSupportTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        beerServer.stopServer();
    }

    private void startBeerService(String address, int port) {
        System.out.println("Connecting to beerService gRPC service at " + address + ":" + port);
        beerChannel = ManagedChannelBuilder.forAddress(address, port).usePlaintext().build();
        beerServiceBlockingStub = BeerServiceGrpc.newBlockingStub(beerChannel);
    }

    private void stopBeerService() {
        beerChannel.shutdownNow();
    }

    private void startHelloWorldService(String address, int port) {
        System.out.println("Connecting to helloWorld gRPC service at " + address + ":" + port);
        worldChannel = ManagedChannelBuilder.forAddress(address, port).usePlaintext().build();
        worldServiceBlockingStub = GreeterGrpc.newBlockingStub(worldChannel);
    }

    private void stopHelloworldService() {
        worldChannel.shutdownNow();
    }

    /**
     * Tests validate the two grpc features, server and client
     *
     * @throws Exception
     *
     **/
    @Test
    public void featuresEnabled() throws Exception {
        Set<String> features = beerServer.getServerConfiguration().getFeatureManager().getFeatures();

        assertTrue("Expected the grpc feature 'grpc-1.0' to be enabled but was not: " + features,
                   features.contains("grpc-1.0"));

        assertTrue("Expected the grpc feature 'grpcClient-1.0' to be enabled but was not: " + features,
                   features.contains("grpcClient-1.0"));

    }

    /**
     * Tests a single grpc service
     *
     * @throws Exception
     *
     **/
    @Test
    public void testSingleWarWithGrpcService() throws Exception {

        // TODO Change this to check for the actual grpc service started message, once it's implemented
        // Check to make sure the service has started
        // CWWKZ0001I: Application FavoriteBeerService started in 0.802 seconds.
        assertNotNull(beerServer.waitForStringInLog("CWWKZ0001I"));

        // Start up the client connection and send a request
        startBeerService(beerServer.getHostname(), beerServer.getHttpDefaultPort());

        // Send a request
        // //"Citraquenchl", "Heist", NEWENGLANDIPA, 4.3);
        Beer newBeer1 = Beer.newBuilder().setBeerName("Citraquenchl").setBeerMaker("Heist").setBeerTypeValue(0).setBeerRating((float) 4.3).build();
        BeerResponse rsp = beerServiceBlockingStub.addBeer(newBeer1);
        assertTrue(rsp.getDone());
        stopBeerService();

    }

    /**
     * Tests starting and stopping multiple grpc services
     *
     * @throws Exception
     *
     **/
    //@Test
    //public void testMultipleGrpcServiceWars() throws Exception {
    //    // Start two services
    //    startBeerService(beerServer.getHostname(), beerServer.getHttpDefaultPort());
    //    startHelloWorldService(beerServer.getHostname(), beerServer.getHttpDefaultPort());

    // Send a request to the Beer service
    //    Beer newBeer1 = Beer.newBuilder().setBeerName("Citraquenchl").setBeerMaker("Heist").setBeerTypeValue(0).setBeerRating((float) 4.3).build();
    //    BeerResponse rsp = beerServiceBlockingStub.addBeer(newBeer1);
    //    assertTrue(rsp.getDone());

    // Send a request to the HelloWorld service
    //    HelloRequest person = HelloRequest.newBuilder().setName("Scarlett").build();
    //    HelloReply greeting = worldServiceBlockingStub.sayHello(person);

    //Make sure the reply has Scarlett in it

    //   stopBeerService();
    //  stopHelloworldService();

    //}

    /**
     * Tests single war uninstall
     *
     * @throws Exception
     *
     **/
    // @Test
    // public void testSingleWarUninstall() throws Exception {

    //     startBeerService(beerServer.getHostname(), beerServer.getHttpDefaultPort());

    //     stopBeerService();

    //}

    /**
     * Tests multiple war uninstall
     *
     * @throws Exception
     *
     **/
    //@Test
    //public void testMultipleWarUninstall() throws Exception {

    //    startBeerService(beerServer.getHostname(), beerServer.getHttpDefaultPort());
    //    startHelloWorldService(beerServer.getHostname(), beerServer.getHttpDefaultPort());

    //    stopBeerService();
    //    stopHelloworldService();

    //}

    /**
     * Tests single war update
     *
     * @throws Exception
     *
     **/
    //@Test
    //public void testSingleWarUpdate() throws Exception {

    //    startBeerService(beerServer.getHostname(), beerServer.getHttpDefaultPort());

    //    stopBeerService();

    //}

    /**
     * Tests invalid grpc service
     *
     * @throws Exception
     *
     **/
//    @Test
//    public void testInvalidService() throws Exception {

//        startBeerService(beerServer.getHostname(), beerServer.getHttpDefaultPort());
//        startHelloWorldService(beerServer.getHostname(), beerServer.getHttpDefaultPort());

//        stopBeerService();
//        stopHelloworldService();

//    }

    /**
     * Tests duplicate grpc service
     *
     * @throws Exception
     *
     **/
    //@Test
    //public void testDuplicateService() throws Exception {

    //    startBeerService(beerServer.getHostname(), beerServer.getHttpDefaultPort());
    //    startBeerService(beerServer.getHostname(), beerServer.getHttpDefaultPort());

    //    stopBeerService();

    //}
}
