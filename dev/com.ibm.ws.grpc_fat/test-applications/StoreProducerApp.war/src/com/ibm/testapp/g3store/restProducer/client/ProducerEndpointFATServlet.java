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
package com.ibm.testapp.g3store.restProducer.client;

import static com.ibm.ws.fat.grpc.monitoring.GrpcMetricsTestUtils.getMetric;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import com.ibm.testapp.g3store.restProducer.model.AppStructure;
import com.ibm.testapp.g3store.restProducer.model.AppStructure.GenreType;
import com.ibm.testapp.g3store.restProducer.model.AppStructure.SecurityType;
import com.ibm.testapp.g3store.restProducer.model.Creator;
import com.ibm.testapp.g3store.restProducer.model.MultiAppStructues;
import com.ibm.testapp.g3store.restProducer.model.Price;
import com.ibm.testapp.g3store.restProducer.model.Price.PurchaseType;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * @author anupag
 *
 *         This class is JAX-RS Resource as proxy to the Remote Endpoint:
 *         ProducerServiceEndpoint
 *
 */
@WebServlet(urlPatterns = "/ProducerEndpointFATServlet")
public class ProducerEndpointFATServlet extends FATServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // ProducerRESTEndpoint has 4 APIs
//    "create";
//    "delete";
//    "createMulti";
//    "deleteAll";

    Logger LOG = Logger.getLogger(ProducerEndpointFATServlet.class.getName());

    private static String getSysProp(String key) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
    }

    private static String hostname = getSysProp("testing.ProducerServer.hostname");

    private RestClientBuilder builder;

    @Override
    public void init() throws ServletException {

        // The baseURL URL of the remote endpoint
        String baseUrlStr = "http://" + hostname + ":" + getSysProp("bvt.prop.HTTP_secondary") + "/StoreProducerApp/v1P/";

        LOG.info("baseUrl = " + baseUrlStr);

        URL baseUrl;
        try {
            baseUrl = new URL(baseUrlStr);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
        builder = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUrl(baseUrl);

        LOG.info("builder = " + builder.toString());
    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testCreateDeleteMyApp(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String m = "testCreateDeleteMyApp";
        LOG.info(m + " ----------------------------------------------------------------");
        LOG.info(m + " ------------testCreateDeleteMyApp--START-----------------------");

        ProducerServiceRestClient service = builder.build(ProducerServiceRestClient.class);
        String appName = "myApp";
        try {
            // create input data
            AppStructure reqPOJO = createAppData("myApp", "Famous myApp", true,
                                                 AppStructure.SecurityType.NO_SECURITY, AppStructure.GenreType.GAME,
                                                 createPriceList(Price.PurchaseType.BLUEPOINTS, 200, null, 100), "ABC", "abc@comp");

            LOG.info("testCreateDeleteMyApp: service = " + service.toString());

            // call Remote REST service
            LOG.info(m + " ------------------------------------------------------------");
            LOG.info(m + " ----- invoking producer rest client to create app: " + appName);
            Response r = service.createApp(reqPOJO);

            // check response
            int status = r.getStatus();
            LOG.info(m + ": create status: " + status);

            assertEquals(200, status);

            String entity = r.readEntity(String.class);
            LOG.info(m + ": create entity: " + entity);

            // testCreate: entity: {"createResult":"18bd8277-efa5-4444-ba78-bf4aa5d3ad50"}

            boolean isValidResponse = entity.contains("createResult");

            assertTrue(isValidResponse);

        } catch (Exception e) {
            LOG.info(m + " " + e.getMessage());
            throw e;
        } finally {
            LOG.info(m + " ------------------------------------------------------------");
            LOG.info(m + " ----- invoking producer rest client to delete app: " + appName);
            // call Remote REST service
            Response r = service.deleteApp(appName);

            // check response
            int status = r.getStatus();
            LOG.info(m + ": delete status: " + status);

            assertEquals(200, status);

            String entity = r.readEntity(String.class);
            LOG.info(m + ": delete entity: " + entity);

            boolean isValidResponse = entity.contains("The app [myApp] has been removed from the server");

            assertTrue(isValidResponse);

            LOG.info(m + " ------------testCreateDeleteMyApp--FINISH-----------------------");
            LOG.info(m + " ----------------------------------------------------------------");

        }
    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testCreateDeleteMultiApp(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String m = "testCreateMulitDeleteAllApp";

        LOG.info(m + " -----------------------------------------------------------------");
        LOG.info(m + " ---------------testCreateDeleteMultiApp---START---------------");

        String app1 = "myApp1";
        String app2 = "myApp2";
        String app3 = "myApp3";
        String app4 = "myApp4";

        ProducerServiceRestClient service = builder.build(ProducerServiceRestClient.class);

        try {

            LOG.info(m + " ------------------------------------------------------------");
            LOG.info(m + " ----- invoking Producer rest client to create apps to test client streaming  ----- ");

            // create input data
            AppStructure reqPOJO1 = createAppData(app1, "Famous myApp1", true,
                                                  AppStructure.SecurityType.NO_SECURITY, AppStructure.GenreType.GAME,
                                                  createPriceList(Price.PurchaseType.BLUEPOINTS, 200, null, 100), "ABC", "abc@comp");

            AppStructure reqPOJO2 = createAppData(app2, "Famous myApp2", true, AppStructure.SecurityType.BASIC,
                                                  AppStructure.GenreType.NEWS, createPriceList(Price.PurchaseType.CREDITCARD, 400, null, 100),
                                                  "ABC", "abc@comp");

            AppStructure reqPOJO3 = createAppData(app3, "Famous myApp3", true,
                                                  AppStructure.SecurityType.TOKEN_JWT, AppStructure.GenreType.SOCIAL,
                                                  createPriceList(Price.PurchaseType.PAYAPL, 2000, null, 100), "ABC", "abc@comp");

            AppStructure reqPOJO4 = createAppData(app4, "Famous myApp4", true,
                                                  AppStructure.SecurityType.TOKEN_OAUTH2, AppStructure.GenreType.GAME,
                                                  createPriceList(Price.PurchaseType.PAYAPL, 20000, Price.PurchaseType.CREDITCARD, 3000), "ABC",
                                                  "abc@comp");

            MultiAppStructues multiApp = new MultiAppStructues();

            multiApp.setStructureList(Arrays.asList(reqPOJO1, reqPOJO2, reqPOJO3, reqPOJO4));

            LOG.info(m + ": create service = " + service.toString());

            // call Remote REST service
            Response r = service.createMultiApps(multiApp);

            // check response
            int status = r.getStatus();
            LOG.info(m + ": create status: " + status);

            String entity = r.readEntity(String.class);
            LOG.info(m + ": create entity: " + entity);

            // but there is no order since it is server streaming, assert them individually

            assertTrue(entity.contains("Store has successfully added the app [myApp1]"));
            assertTrue(entity.contains("Store has successfully added the app [myApp2]"));
            assertTrue(entity.contains("Store has successfully added the app [myApp3]"));
            assertTrue(entity.contains("Store has successfully added the app [myApp4]"));

        } catch (Exception e) {
            LOG.info(m + " " + e.getMessage());
            e.printStackTrace();
            throw e;

        } finally {
            LOG.info(m + " ------------------------------------------------------------");
            LOG.info(m + " ----- invoking Producer rest client to delete all apps to test gRPC server streaming ----- ");

            // call Remote REST service
            Response r = service.deleteAllApps();

            // check response
            int status = r.getStatus();
            LOG.info(m + ": delete status: " + status);

            assertEquals(200, status);

            String entity = r.readEntity(String.class);
            LOG.info(m + ": delete entity: " + entity);

            // Expected output
            // delete entity: The app [myApp1] has been removed from the Store. The app [myApp2] has been removed from the Store.
            //The app [myApp3] has been removed from the Store. The app [myApp4] has been removed from the Store.
            // but there is no order since it is server streaming, assert them individually

            String helpFindFailureMessage = "Check the previous log message for the response and compare with expected : The app [" + app1 + "] has been removed from the Store";

            assertTrue(helpFindFailureMessage, entity.contains("The app [" + app1 + "] has been removed from the Store"));
            assertTrue(entity.contains("The app [" + app2 + "] has been removed from the Store"));
            assertTrue(entity.contains("The app [" + app3 + "] has been removed from the Store"));
            assertTrue(entity.contains("The app [" + app4 + "] has been removed from the Store"));

            LOG.info(m + " ---------------testCreateDeleteMultiApp---FINISH---------");
            LOG.info(m + " ------------------------------------------------------------");

        }
    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testDuplicate_CreateDeleteMyApp(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String m = "testDuplicateCreateDeleteMyApp";

        LOG.info(m + " ------------------------------------------------------------");
        LOG.info(m + " ----- invoking Producer rest client to create app to test client streaming  ----- ");

        ProducerServiceRestClient service = builder.build(ProducerServiceRestClient.class);

        try {
            // create input data
            AppStructure reqPOJO = createAppData("myApp", "Famous myApp", true,
                                                 AppStructure.SecurityType.NO_SECURITY, AppStructure.GenreType.GAME,
                                                 createPriceList(Price.PurchaseType.BLUEPOINTS, 200, null, 100), "ABC", "abc@comp");

            LOG.info(m + ": service = " + service.toString());

            // call Remote REST service
            Response r = service.createApp(reqPOJO);

            // check response
            int status = r.getStatus();
            LOG.info(m + ": create status: " + status);

            assertEquals(200, status);

            LOG.info(m + " ------------------------------------------------------------");
            LOG.info(m + " ----- invoking Producer rest client to create duplicate app to test ALREADY EXIST exception.");

            // call again to create same entry
            r = service.createApp(reqPOJO);

            // check response status if exception is not thrown
            status = r.getStatus();
            LOG.info(m + ": create duplicate status: " + status);

        } catch (javax.ws.rs.WebApplicationException excep) {

            LOG.info(m + ": Expected WAexception message: " + excep.getMessage());

            String excepEntity = excep.getResponse().readEntity(String.class);

            LOG.info(m + ": Expected excepEntity: " + excepEntity);

            boolean isValidResponse = excepEntity.contains("The app already exist in the Store.\n" +
                                                           "First run the ProducerService to delete the app. AppName = myApp");

            assertTrue(isValidResponse);

        } catch (Exception e) {
            LOG.info(m + ": exception message: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // call Remote REST service to delete the app created
            Response r = service.deleteApp("myApp");

            // check response
            assertEquals(200, r.getStatus());

            String entity = r.readEntity(String.class);
            LOG.info(m + ": delete entity: " + entity);

            assertTrue(entity.contains("The app [myApp] has been removed from the server"));

            LOG.info(m + " ------------------------------------------------------------");
            LOG.info(m + " ------------------------------------------------------------");

        }
    }

    /**
     * @param name
     * @param desc
     * @param isfree
     * @param securityType
     * @param genreType
     * @param purchaseType
     * @param sellingPrice
     * @param companyName
     * @param email
     * @return
     */
    private AppStructure createAppData(String name, String desc, Boolean isfree, SecurityType securityType,
                                       GenreType genreType, List<Price> priceList, String companyName, String email) {

        AppStructure appStruct = new AppStructure();

        appStruct.setName(name);
        appStruct.setDesc(desc);
        appStruct.setFree(isfree);
        appStruct.setSecurityType(securityType);
        appStruct.setGenreType(genreType);
        appStruct.setPriceList(priceList);

        Creator cr = new Creator();
        cr.setCompanyName(companyName);
        cr.setEmail(email);

        appStruct.setCreator(cr);

        return appStruct;

    }

    /**
     * @param purchaseType1
     * @param sellingPrice1
     * @param purchaseType2
     * @param sellingPrice2
     * @return
     */
    private List<Price> createPriceList(PurchaseType purchaseType1, double sellingPrice1, PurchaseType purchaseType2,
                                        double sellingPrice2) {

        List<Price> priceList = null;

        Price price1 = new Price();
        price1.setPurchaseType(purchaseType1);
        price1.setSellingPrice(sellingPrice1);

        if (purchaseType2 != null) {
            Price price2 = new Price();
            price2.setPurchaseType(purchaseType2);
            price2.setSellingPrice(sellingPrice2);

            priceList = Arrays.asList(price1, price2);
        } else {
            priceList = Arrays.asList(price1);
        }

        return priceList;
    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testClientStreaming(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String m = "testClientStreaming";
        LOG.info(m + " ----------------------------------------------------------------");
        LOG.info(m + " ------------ testClientStreaming--START -----------------------");

        startlientStreaming(req, resp);

        LOG.info(m + " ------------ testClientStreaming--FINISH -----------------------");
        LOG.info(m + " ----------------------------------------------------------------");
    }

    //@Mode(TestMode.FULL)
    //@Test
    public void testClientStreamingMetrics(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String m = "testClientStreamingMetrics";
        LOG.info(m + " ----------------------------------------------------------------");
        LOG.info(m + " ------------ testClientStreamingMetrics--START -----------------------");

        startlientStreaming(req, resp);

        // retrieve the metrics
        int httpPort = Integer.parseInt(getSysProp("bvt.prop.HTTP_default"));
        String metricValue = getMetric("localhost", httpPort, "/metrics/vendor/grpc.server.receivedMessages.total");
        if (metricValue == null || Integer.parseInt(metricValue) < 200) {
            fail(String.format("Incorrect metric value [%s]. Expected [%s], got [%s]", "grpc.server.receivedMessages.total", ">=200", metricValue));
        }
        LOG.info(m + " ------------ testClientStreamingMetrics--FINISH -----------------------");
        LOG.info(m + " ----------------------------------------------------------------");
    }

    private void startlientStreaming(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String m = "startlientStreaming";

        // before coming here, code in ProducerGrpcServiceClient will have made the GRPC calls
        // to ManagedChannelBuilder.forAddress and newStub to setup the RPC code for client side usage

        ProducerServiceRestClient service = builder.build(ProducerServiceRestClient.class);
        LOG.info("testClientStreamApp: service = " + service.toString());
        try {
            // call Remote REST service
            // tell the rest client to send data to the gRPC client.  gRPC client will then make
            // gRPC calls to the gRPC server.
            LOG.info(m + " ------------------------------------------------------------");
            LOG.info(m + " ----- invoking producer REST client to perform clientStream test: clientStreamApp()");

            Response r = service.clientStreamApp();

            // check response
            String result = r.readEntity(String.class);
            LOG.info(m + ": client stream entity/result: " + result);
            boolean isValidResponse = result.contains("success");
            assertTrue(isValidResponse);
        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        }
    }

    @Test
    public void testServerStreaming(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String m = "testServerStreaming";
        LOG.info(m + " ----------------------------------------------------------------");
        LOG.info(m + " ------------ testServerStreaming--START -----------------------");

        startServerStreaming(req, resp);

        LOG.info(m + " ------------ testServerStreaming--FINISH -----------------------");
        LOG.info(m + " ----------------------------------------------------------------");
    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testServerStreamingMetrics(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String m = "testServerStreamingMetrics";
        LOG.info(m + " ----------------------------------------------------------------");
        LOG.info(m + " ------------ testServerStreamingMetrics--START -----------------------");

        startServerStreaming(req, resp);

        // retrieve the metrics
        int httpPort = Integer.parseInt(getSysProp("bvt.prop.HTTP_secondary"));
        String metricValue = getMetric("localhost", httpPort, "/metrics/vendor/grpc.client.receivedMessages.total");
        if (metricValue == null || Integer.parseInt(metricValue) < 200) {
            fail(String.format("Incorrect metric value [%s]. Expected [%s], got [%s]", "grpc.client.receivedMessages.total", ">=200", metricValue));
        }

        LOG.info(m + " ------------ testServerStreamingMetrics--FINISH -----------------------");
        LOG.info(m + " ----------------------------------------------------------------");
    }

    private void startServerStreaming(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String m = "startServerStreaming";

        // before coming here, code in ProducerGrpcServiceClient will have made the GRPC calls
        // to ManagedChannelBuilder.forAddress and newStub to setup the RPC code for client side usage

        ProducerServiceRestClient service = builder.build(ProducerServiceRestClient.class);
        LOG.info("testServerStreamApp: service = " + service.toString());
        try {
            // call Remote REST service
            // tell the rest client to send data to the gRPC client.  gRPC client will then make
            // gRPC calls to the gRPC server.
            LOG.info(m + " ------------------------------------------------------------");
            LOG.info(m + " ----- invoking producer REST client to perform serverStream test: serverStreamApp()");

            Response r = service.serverStreamApp();

            // check response
            String result = r.readEntity(String.class);
            LOG.info(m + ": client stream entity/result: " + result);
            boolean isValidResponse = result.contains("success");
            assertTrue(isValidResponse);

        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        }
    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testTwoWayStreaming(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String m = "testTwoWayStreaming";
        LOG.info(m + " ----------------------------------------------------------------");
        LOG.info(m + " ------------ testTwoWayStreaming--START -----------------------");

        // before coming here, code in ProducerGrpcServiceClient will have made the GRPC calls
        // to ManagedChannelBuilder.forAddress and newStub to setup the RPC code for client side usage

        ProducerServiceRestClient service = builder.build(ProducerServiceRestClient.class);
        LOG.info("testTwoWayStream: service = " + service.toString());
        try {
            // call Remote REST service
            // tell the rest client to send data to the gRPC client.  gRPC client will then make
            // gRPC calls to the gRPC server.
            LOG.info(m + " ------------------------------------------------------------");
            LOG.info(m + " ----- invoking producer REST client to perform TwoWayStream test: twoWayStreamApp(false)");
            Response r = service.twoWayStreamApp(false);

            // check response
            String result = r.readEntity(String.class);
            LOG.info(m + ": client stream entity/result: " + result);
            boolean isValidResponse = result.contains("success");
            assertTrue(isValidResponse);

        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        } finally {
            LOG.info(m + " ------------ testTwoWayStreaming--FINISH -----------------------");
            LOG.info(m + " ----------------------------------------------------------------");
        }
    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testTwoWayStreamingAsyncThread(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String m = "testTwoWayStreamingAsyncThread";
        LOG.info(m + " ----------------------------------------------------------------");
        LOG.info(m + " ------------ testTwoWayStreamingAsyncThread--START -------------");

        // before coming here, code in ProducerGrpcServiceClient will have made the GRPC calls
        // to ManagedChannelBuilder.forAddress and newStub to setup the RPC code for client side usage

        ProducerServiceRestClient service = builder.build(ProducerServiceRestClient.class);
        LOG.info("testTwoWayStreamAsyncThread: service = " + service.toString());
        try {
            // call Remote REST service
            // tell the rest client to send data to the gRPC client.  gRPC client will then make
            // gRPC calls to the gRPC server.
            LOG.info(m + " ------------------------------------------------------------");
            LOG.info(m + " ----- invoking producer REST client to perform TwoWayStream test: twoWayStreamApp(true)");
            Response r = service.twoWayStreamApp(true);

            // check response
            String result = r.readEntity(String.class);
            LOG.info(m + ": client stream entity/result: " + result);
            boolean isValidResponse = result.contains("success");
            assertTrue(isValidResponse);

        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        } finally {
            LOG.info(m + " ------------ testTwoWayStreamingAsyncThread--FINISH ------------");
            LOG.info(m + " ----------------------------------------------------------------");
        }
    }

}
