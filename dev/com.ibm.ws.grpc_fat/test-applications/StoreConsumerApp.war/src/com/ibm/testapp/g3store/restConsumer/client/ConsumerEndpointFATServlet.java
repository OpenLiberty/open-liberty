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
package com.ibm.testapp.g3store.restConsumer.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import com.ibm.testapp.g3store.restProducer.client.ProducerServiceRestClient;
import com.ibm.testapp.g3store.restProducer.model.AppStructure;
import com.ibm.testapp.g3store.restProducer.model.AppStructure.GenreType;
import com.ibm.testapp.g3store.restProducer.model.AppStructure.SecurityType;
import com.ibm.testapp.g3store.restProducer.model.Creator;
import com.ibm.testapp.g3store.restProducer.model.MultiAppStructues;
import com.ibm.testapp.g3store.restProducer.model.Price;
import com.ibm.testapp.g3store.restProducer.model.Price.PurchaseType;
import com.ibm.testapp.g3store.utilsConsumer.ConsumerUtils;

import componenttest.app.FATServlet;

/**
 * @author anupag
 *
 *         This class is JAX-RS Resource as proxy to the Remote Endpoint:
 *         ConsumerRestEndpoint
 *
 */
@WebServlet(urlPatterns = "/ConsumerEndpointFATServlet")
public class ConsumerEndpointFATServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;
    Logger _log = Logger.getLogger(ConsumerEndpointFATServlet.class.getName());

    private RestClientBuilder builderConsumer;
    private RestClientBuilder builderProducer;

    private static String consumerServerBaseURL = "http://" + ConsumerUtils.getConsumerServerHost() + ":" + ConsumerUtils.getConsumerServerPort();

    // The baseURL URL of the remote endpoint of Consumer Wrapper application
    private static String consumerUrlStr = consumerServerBaseURL + "/StoreConsumerApp/v1C/";

    // The baseURL URL of the remote endpoint of Producer Wrapper application
    private static String producerUrlStr = consumerServerBaseURL + "/StoreConsumerApp/v1P/";

    /**
     *
     */
    @Override
    public void init() throws ServletException {

        String m = "ConsumerEndpoint.init";
        _log.info(m + ": baseUrlStrProducer = " + producerUrlStr);

        URL baseUrlProducer;
        try {
            baseUrlProducer = new URL(producerUrlStr);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
        builderProducer = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUrl(baseUrlProducer);

        _log.info(m + ": builderProducer = " + builderProducer.toString());

    }

    /**
     * This test will send a valid Basic Authorization header created in ConsumerServiceRestClient
     * The Authorization header will be propagated using grpcClient.
     *
     * This test will sent grpc requests to create data, getAppInfo with Auth header , delete data.
     * The test passes when correct "appName" is asserted in response.
     *
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testGetAppInfo_Valid_BasicAuth(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        this.getAppInfo(req, resp);
    }

    /**
     * This test will send a Bad Basic Authorization header created in ConsumerServiceRestClient
     * The Authorization header will be propagated using grpcClient.
     *
     * This test will sent grpc requests to create data, getAppInfo with Bad Auth header , delete data.
     * The test passes when "Expected auth failure" is asserted in response.
     *
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testGetAppInfo_Bad_BasicAuth(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        this.getAppInfo_BadBasicAuth(req, resp);
    }

    /**
     * This test will send a valid JWT token in Authorization header added via ClientRequestFilter
     * The Authorization header will be propagated using grpcClient.
     *
     * This test will sent grpc requests to create data, getAppName with JWT token Auth header , delete data.
     * The test passes when correct "appName" is asserted in response.
     *
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testGetAppName_JWTAuth_GrpcClient(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        this.getAppName_Auth_grpcClient(req, resp);
    }

    /**
     * This test will send a null JWT token in Authorization header added via ClientRequestFilter
     * The Authorization header will be propagated using grpcClient.
     *
     * This test will sent grpc requests to create data, getAppName with null JWT token Auth header , delete data.
     *
     * The test passes when "Expected auth failure" is asserted in response.
     *
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testGetAppName_NullJWTAuth_grpcClient(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        this.getAppName_NullJWTAuth_grpcClient(req, resp);
    }

    /**
     * This test will send a good JWT token in Authorization header added via ClientRequestFilter
     * The Authorization header will be propagated using grpcClient.
     *
     * This test will sent grpc requests to create data, getAppName with good JWT token Auth header
     * But the server side will have bad RolesAllowed set,
     *
     * The test passes when "Expected auth failure" is asserted in response.
     *
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testGetAppName_BadServerRoles_grpcClient(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        this.getAppName_BadServerRoles_grpcClient(req, resp);
    }

    /**
     * This test will send a valid JWT token in Authorization header added via ClientRequestFilter
     * The Authorization header will be propagated using CallCredentials API.
     *
     * This test will sent grpc requests to create data, getAppName with JWT token Auth header , delete data.
     * The test passes when correct "appName" is asserted in response.
     *
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testGetAppName_Auth_CallCred(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        this.getAppName_Auth_CallCred(req, resp);
    }

    @Test
    public void testGetAppPrice(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        this.getAppPrice(req, resp);
    }

    @Test
    public void testGetMultiAppsInfo(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        this.getMultiAppsInfo(req, resp);
    }

    @Test
    public void testGetMultiAppNames(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        this.getMultiAppNames(req, resp);
    }

    @Test
    public void testGetMultiAppPrices(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        this.getMultiAppPrices(req, resp);
    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    private void getAppName_Auth_CallCred(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final String m = "testGetAppName_Auth_CallCred";

        // create
        // query name
        // get app info with the name
        // delete

        ProducerServiceRestClient service = null;
        String appName = "myAppConsumerAuthCC";
        boolean isValidResponse = false;

        try {

            _log.info(m + " ------------------ " + m + "--START ---------------------");

            service = assertCreateSingleAppData(m, appName);

            _log.info(m + " ----- invoking consumer rest client for available app names");
            ConsumerServiceRestClient client = null;
            // Now get the data using Consumer
            try {
                client = this.add_AuthHeader_Filter(m, req, resp);
            } catch (Exception e) {
                _log.severe(m + " , Error creating ConsumerServiceRestClient proxy");
                throw e;
            }

            _log.info(m + " ----- invoking consumer rest client: " + client);

            Response r = client.getAllAppNames_Auth_CallCred();

            assertEquals(200, r.getStatus());

            String entityName = r.readEntity(String.class);
            _log.info(m + " entityName: " + entityName);
            // Expected output in logs
            //entityName: ["myApp"]

            isValidResponse = entityName.contains(appName);
            assertTrue(isValidResponse);

            _log.info(m + " ------------------------------------------------------------");

        } finally {

            assertDeleteSingleAppData(m, service, appName);

            _log.info(m + " ---------------- " + m + "--FINISH -------------------");
            _log.info(m + " ------------------------------------------------------------");

        }
    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    private void getAppName_NullJWTAuth_grpcClient(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final String m = "getAppName_NullJWTAuth_grpcClient";

        // create
        // query name
        // get app info with the name

        // delete
        ProducerServiceRestClient service = null;
        String appName = "myAppConsumerNullJWT";
        boolean isValidResponse = false;

        try {

            _log.info(m + " ------------------ " + m + "--START ---------------------");

            service = assertCreateSingleAppData(m, appName);

            _log.info(m + " ----- invoking consumer rest client for available app names");

            // Now get the data using Consumer

            ConsumerServiceRestClient client = this.add_BadAuthHeader_Filter(appName, req, resp);

            _log.info(m + " ----- invoking consumer rest client: " + client);

            Response r = client.getAllAppNames(m);
            // Always returned 200 from grpc
            assertEquals(200, r.getStatus());

            String entityName = r.readEntity(String.class);
            _log.info(m + " entityName: " + entityName);

            isValidResponse = entityName.contains("Expected auth failure");
            assertTrue(isValidResponse);

            _log.info(m + " ------------------------------------------------------------");

        } finally {

            assertDeleteSingleAppData(m, service, appName);

            _log.info(m + " ---------------- " + m + "--FINISH -------------------");
            _log.info(m + " ------------------------------------------------------------");

        }

    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    private void getAppName_Auth_grpcClient(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final String m = "testGetAppName_Auth_grpcClient";

        // create
        // query name
        // get app info with the name
        // get Price list for the app
        // delete
        ProducerServiceRestClient service = null;
        String appName = "myAppConsumerAuth";
        boolean isValidResponse = false;

        try {

            _log.info(m + " ------------------ " + m + "--START ---------------------");

            service = assertCreateSingleAppData(m, appName);

            _log.info(m + " ----- invoking consumer rest client for available app names");

            ConsumerServiceRestClient client = null;
            // Now get the data using Consumer
            try {
                client = this.add_AuthHeader_Filter(m, req, resp);
            } catch (Exception e) {
                _log.severe(m + " , Error creating ConsumerServiceRestClient proxy");
                throw e;
            }

            _log.info(m + " ----- invoking consumer rest client: " + client);

            Response r = client.getAllAppNames(m);

            assertEquals(200, r.getStatus());

            String entityName = r.readEntity(String.class);
            _log.info(m + " entityName: " + entityName);
            // Expected output in logs
            //entityName: ["myApp"]

            isValidResponse = entityName.contains(appName);
            assertTrue(isValidResponse);

            _log.info(m + " ------------------------------------------------------------");

        } finally {

            assertDeleteSingleAppData(m, service, appName);

            _log.info(m + " ---------------- " + m + "--FINISH -------------------");
            _log.info(m + " ------------------------------------------------------------");

        }

    }

    /**
     * @param req
     * @param resp
     */
    private void getAppName_BadServerRoles_grpcClient(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testGetAppName_BadServerRoles_grpcClient";

        // create
        // get app name
        // delete
        ProducerServiceRestClient service = null;
        String appName = "myAppConsumerBadServerRole";
        boolean isValidResponse = false;

        try {

            _log.info(m + " ------------------ " + m + "--START ---------------------");

            service = assertCreateSingleAppData(m, appName);

            _log.info(m + " ----- invoking consumer rest client for available app names");

            ConsumerServiceRestClient client = null;
            // Now get the data using Consumer
            try {
                client = this.add_AuthHeader_Filter(m, req, resp);
            } catch (Exception e) {
                _log.severe(m + " , Error creating ConsumerServiceRestClient proxy");
                throw e;
            }

            _log.info(m + " ----- invoking consumer rest client: " + client);

            Response r = client.getAllAppNames(m);

            assertEquals(200, r.getStatus());

            String entityName = r.readEntity(String.class);
            _log.info(m + " entityName: " + entityName);

            isValidResponse = entityName.contains("Expected auth failure");
            assertTrue(isValidResponse);

            _log.info(m + " ------------------------------------------------------------");

        } finally {

            assertDeleteSingleAppData(m, service, appName);

            _log.info(m + " ---------------- " + m + "--FINISH -------------------");
            _log.info(m + " ------------------------------------------------------------");

        }

    }

    /**
     *
     * Since the filter is added to add token authentication
     * we need to create this client each time, so that filter does not add on other API requests
     *
     * @param m
     * @return
     * @throws Exception
     */
    private RestClientBuilder createConsumerRestClient(String m) throws Exception {

        _log.info(m + ": baseUrlConsumer = " + consumerUrlStr);

        URL baseUrl;
        try {
            baseUrl = new URL(consumerUrlStr);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
        builderConsumer = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUrl(baseUrl);

        _log.info(m + ": builderConsumer = " + builderConsumer.toString());

        return builderConsumer;

    }

    /**
     * @param m
     * @param appName
     * @throws Exception
     */
    private ProducerServiceRestClient assertCreateSingleAppData(String m, String appName) throws Exception {

        ProducerServiceRestClient service = null;
        try {
            service = builderProducer.build(ProducerServiceRestClient.class);
        } catch (Exception e) {
            _log.severe("Check ProducerServiceRestClient proxy");
            throw e;
        }

        _log.info(m + " ----- invoking producer rest client to create data: " + appName);
        // create input data
        AppStructure reqPOJO = createAppData(appName, "Famous myApp", true,
                                             AppStructure.SecurityType.NO_SECURITY, AppStructure.GenreType.GAME,
                                             createPriceList(Price.PurchaseType.BLUEPOINTS, 200, null, 100), "ABC", "abc@comp");

        // call Remote REST service to create
        Response r = service.createApp(reqPOJO);

        // check response
        assertEquals(200, r.getStatus());

        String entityCreate = r.readEntity(String.class);
        _log.info(m + " entityCreate: " + entityCreate);
        // Expected output in logs

        _log.info(m + " ------ data created -----");

        return service;
    }

    /**
     * @param m
     * @param service
     * @param appName
     * @throws Exception
     */
    private void assertDeleteSingleAppData(String m, ProducerServiceRestClient service, String appName) throws Exception {

        boolean isValidResponse = false;

        _log.info(m + " ------------------ delete data ------------------");
        _log.info(m + " ----- invoking producer rest client to delete data: " + appName);
        // call Remote REST service to delete
        Response r2 = service.deleteApp(appName);

        // check response
        int status = r2.getStatus();
        _log.info(m + ": delete status: " + status);

        assertEquals(200, status);

        String entity = r2.readEntity(String.class);
        _log.info(m + ": delete entity: " + entity);

        isValidResponse = entity.contains("The app [" + appName + "] has been removed from the server");

        assertTrue(isValidResponse);
        _log.info(m + " ------ data deleted -----");
    }

    /**
     * @param m
     * @param app1
     * @param app2
     * @param app3
     * @param app4
     * @return
     * @throws Exception
     */
    private ProducerServiceRestClient assertCreateMultipleAppData(String m, String app1, String app2, String app3, String app4) throws Exception {

        _log.info(m + " ----- invoking producer rest client to create apps to test client streaming  ----- ");

        ProducerServiceRestClient service = null;

        try {
            service = builderProducer.build(ProducerServiceRestClient.class);
        } catch (Exception e) {
            _log.severe(m + " , Error creating ProducerServiceRestClient proxy");
            throw e;
        }

        // create input data
        AppStructure reqPOJO1 = createAppData(app1, "Famous myApp1", true,
                                              AppStructure.SecurityType.NO_SECURITY, AppStructure.GenreType.GAME,
                                              createPriceList(Price.PurchaseType.BLUEPOINTS, 200, null, 100), "ABC", "abc@comp");

        AppStructure reqPOJO2 = createAppData(app2, "Famous myApp2", false, AppStructure.SecurityType.BASIC,
                                              AppStructure.GenreType.NEWS, createPriceList(Price.PurchaseType.CREDITCARD, 400, null, 100),
                                              "ABC", "abc@comp");

        AppStructure reqPOJO3 = createAppData(app3, "Famous myApp3", true,
                                              AppStructure.SecurityType.TOKEN_JWT, AppStructure.GenreType.SOCIAL,
                                              createPriceList(Price.PurchaseType.PAYAPL, 2000, null, 100), "ABC", "abc@comp");

        AppStructure reqPOJO4 = createAppData(app4, "Famous myApp4", false,
                                              AppStructure.SecurityType.TOKEN_OAUTH2, AppStructure.GenreType.GAME,
                                              createPriceList(Price.PurchaseType.PAYAPL, 20000, Price.PurchaseType.CREDITCARD, 3000), "ABC",
                                              "abc@comp");

        MultiAppStructues multiApp = new MultiAppStructues();

        multiApp.setStructureList(Arrays.asList(reqPOJO1, reqPOJO2, reqPOJO3, reqPOJO4));

        // call Remote REST service

        Response r = service.createMultiApps(multiApp);

        // check response
        int status = r.getStatus();
        _log.info(m + ": create status: " + status);
        assertEquals(200, status);

        String entity = r.readEntity(String.class);
        _log.info(m + ": create entity: " + entity);

        // expected output, but there is no order since it is client streaming, assert them individually
//        getMultiAppsInfo: create entity: Store has successfully added the app [myAppConsumer1] with id [ 8bd3de2d-b274-4a7c-b289-29e187257d73 ]
//                        Store has successfully added the app [myAppConsumer2] with id [ a56d79e7-2731-45c5-9e18-bfaf4bf31f82 ]
//                        Store has successfully added the app [myAppConsumer3] with id [ 25f2934e-9dd2-47c7-bd8a-bf7e82e6436b ]
//                        Store has successfully added the app [myAppConsumer4] with id [ 68e73541-9183-4cd5-84b4-490ac3331602 ]

        assertTrue(entity.contains("Store has successfully added the app [" + app1 + "]"));
        assertTrue(entity.contains("Store has successfully added the app [" + app2 + "]"));
        assertTrue(entity.contains("Store has successfully added the app [" + app3 + "]"));
        assertTrue(entity.contains("Store has successfully added the app [" + app4 + "]"));

        _log.info(m + " ------ multi app data created -----");

        return service;
    }

    /**
     * @param m
     * @param service
     * @param app1
     * @param app2
     * @param app3
     * @param app4
     * @throws Exception
     */
    private void assertDeleteMultipleAppData(String m, ProducerServiceRestClient service, String app1, String app2, String app3, String app4) throws Exception {

        _log.info(m + " ----- invoking Producer Rest client to delete apps to test grpc server streaming ----- ");

        if (service == null) {
            try {
                service = builderProducer.build(ProducerServiceRestClient.class);
            } catch (Exception e) {
                _log.severe(m + " , Error creating ProducerServiceRestClient proxy");
                throw e;
            }
        }
        // call Remote REST service to delete
        Response r2 = service.deleteAllApps();

        // check response
        int status = r2.getStatus();
        _log.info(m + ": delete status: " + status);

        assertEquals(200, status);

        String entity = r2.readEntity(String.class);
        _log.info(m + ": delete entity: " + entity);

        // Expected output
        // delete entity: The app [myApp1] has been removed from the Store. The app [myApp2] has been removed from the Store.
        //The app [myApp3] has been removed from the Store. The app [myApp4] has been removed from the Store.
        // expected output, but there is no order since it is server streaming, assert them individually
        String helpFindFailureMessage = "Check the previous log message for the response and compare with expected : The app [" + app1 + "] has been removed from the Store";
        assertTrue(helpFindFailureMessage, entity.contains("The app [" + app1 + "] has been removed from the Store"));
        assertTrue(entity.contains("The app [" + app2 + "] has been removed from the Store"));
        assertTrue(entity.contains("The app [" + app3 + "] has been removed from the Store"));
        assertTrue(entity.contains("The app [" + app4 + "] has been removed from the Store"));

        _log.info(m + " ------ multi data deleted -----");

    }

    /**
     * @param m
     * @param req
     * @param resp
     * @return
     * @throws Exception
     */
    private ConsumerServiceRestClient add_AuthHeader_Filter(String m, HttpServletRequest req, HttpServletResponse resp) throws Exception {

        builderConsumer = createConsumerRestClient(m);

        ConsumerServiceRestClient client = builderConsumer
                        .property("com.ibm.ws.jaxrs.client.keepalive.connection", "close")
                        .register(
                                  (ClientRequestFilter) requestContext -> {
                                      MultivaluedMap<String, Object> headers = requestContext.getHeaders();
                                      try {
                                          _log.info(m + " ----------- Add Bearer Token to the request --------------");
                                          headers.add("Authorization", "Bearer " + getToken(req, resp));

                                          for (Entry<String, List<Object>> entry : headers.entrySet()) {
                                              _log.info("Request headers Bearer: " + entry.getKey() + ", set to: " + entry.getValue());
                                          }

                                      } catch (Exception e) {
                                          _log.info(m + " -------------- Failed --------------");
                                          e.printStackTrace();
                                      }
                                  })
                        .build(ConsumerServiceRestClient.class);

        return client;

    }

    /**
     *
     * This will fail getting the token from the token endpoint,
     * resulting in a null value in
     * "Authorization" "Bearer " token
     *
     * @param m
     * @param req
     * @param resp
     * @return
     * @throws Exception
     */
    private ConsumerServiceRestClient add_BadAuthHeader_Filter(String m, HttpServletRequest req, HttpServletResponse resp) throws Exception {

        builderConsumer = createConsumerRestClient(m);

        ConsumerServiceRestClient client = builderConsumer
                        .property("com.ibm.ws.jaxrs.client.keepalive.connection", "close")
                        .register(
                                  (ClientRequestFilter) requestContext -> {
                                      MultivaluedMap<String, Object> headers = requestContext.getHeaders();
                                      try {
                                          _log.info(m + " ----------- Add Bearer Token to the request --------------");
                                          headers.add("Authorization", "Bearer " + getBadToken(req, resp));

                                          for (Entry<String, List<Object>> entry : headers.entrySet()) {
                                              _log.info(m + "Request headers Bearer: " + entry.getKey() + ", set to: " + entry.getValue());
                                          }

                                      } catch (Exception e) {
                                          _log.info(m + " -------------- Failed --------------");
                                          e.printStackTrace();
                                      }
                                  })
                        .build(ConsumerServiceRestClient.class);

        return client;

    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    private void getAppInfo(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testGetAppInfo";

        // create

        // get app info with the name

        // delete
        ProducerServiceRestClient service = null;
        String appName = "myAppConsumer1";
        boolean isValidResponse = false;

        try {

            _log.info(m + " ------------------ testGetAppInfo--START ---------------------");

            service = assertCreateSingleAppData(m, appName);

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking consumer rest client to get app info: " + appName);

            builderConsumer = createConsumerRestClient(m);

            ConsumerServiceRestClient client2 = builderConsumer
                            .property("com.ibm.ws.jaxrs.client.keepalive.connection", "close")
                            .build(ConsumerServiceRestClient.class);

            _log.info(m + " ----- invoking consumer rest client: " + client2);

            Response r = client2.getAppInfo(appName);

            assertEquals(200, r.getStatus());

            String entityAppInfo = r.readEntity(String.class);
            _log.info(m + " entityAppInfo: " + entityAppInfo);

            // Expected output in logs
//            entityAppInfo: {
//                "name": "myAppConsumer",
//                "desc": "Famous myApp",
//                "id": "8317f96c-b26a-453c-870e-7f73108d23be",
//                "free": true,
//                "genreType": "GAME",
//                "securityType": "NO_SECURITY",
//                "prices": [{
//                  "purchaseType": "BLUEPOINTS",
//                  "sellingPrice": 200.0
//                }],
//                "creator": {
//                  "companyName": "ABC",
//                  "email": "abc@comp"
//                }
//              }

            isValidResponse = entityAppInfo.contains(appName);
            assertTrue(isValidResponse);

            _log.info(m + " ------------------------------------------------------------");

        } finally {

            assertDeleteSingleAppData(m, service, appName);
            builderConsumer = null;

            _log.info(m + " ----------------testGetAppInfo--FINISH -------------------");
            _log.info(m + " ------------------------------------------------------------");

        }

    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    private void getAppInfo_BadBasicAuth(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testGetAppInfo_BadBasicAuth";

        // create

        // get app info with the name

        // delete
        ProducerServiceRestClient service = null;
        String appName = "myAppConsumerBadBasicAuth";
        boolean isValidResponse = false;

        try {

            _log.info(m + " ------------------ testGetAppInfo_BadBasicAuth--START ---------------------");

            service = assertCreateSingleAppData(m, appName);

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking consumer rest client to get app info: " + appName);

            builderConsumer = createConsumerRestClient(m);

            ConsumerServiceRestClient client2 = builderConsumer
                            .property("com.ibm.ws.jaxrs.client.keepalive.connection", "close")
                            .build(ConsumerServiceRestClient.class);

            _log.info(m + " ----- invoking consumer rest client: " + client2);

            Response r = client2.getAppInfoBadAuth(appName);

            assertEquals(200, r.getStatus());

            String entityAppInfo = r.readEntity(String.class);
            _log.info(m + " entityAppInfo: " + entityAppInfo);

            // since we are sending bad auth , we should not recv any value back
            // once server side is fixed, fix this

            isValidResponse = entityAppInfo.contains("Expected auth failure");
            assertTrue(isValidResponse);

            _log.info(m + " ------------------------------------------------------------");

        } finally {

            assertDeleteSingleAppData(m, service, appName);
            builderConsumer = null;

            _log.info(m + " ----------------testGetAppInfo_BadBasicAuth--FINISH -------------------");
            _log.info(m + " ------------------------------------------------------------");

        }

    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    private void getAppPrice(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testGetAppPrice";

        // create

        // get Price list for the app

        // delete

        ProducerServiceRestClient service = null;
        String appName = "myAppConsumer2";
        boolean isValidResponse = false;

        try {

            _log.info(m + " ------------------ testGetAppPrice--START ---------------------");
            service = assertCreateSingleAppData(m, appName);

            builderConsumer = createConsumerRestClient(m);

            ConsumerServiceRestClient client2 = builderConsumer
                            .property("com.ibm.ws.jaxrs.client.keepalive.connection", "close")
                            .build(ConsumerServiceRestClient.class);

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking consumer rest client to get price for the app to test bidi grpc streaming: " + appName);

            _log.info(m + " ----- invoking consumer rest client: " + client2);

            Response r = client2.getPrices(Arrays.asList(appName));

            assertEquals(200, r.getStatus());

            String entityAppPrice = r.readEntity(String.class);
            _log.info(m + " entityAppPrice: " + entityAppPrice);
            // entityAppPrice: {"appNameswPrice":[{"appName":"myAppConsumer","prices":[{"purchaseType":"BLUEPOINTS","sellingPrice":200.0}]}]}
            isValidResponse = entityAppPrice.contains("BLUEPOINTS");
            assertTrue(isValidResponse);

        } finally {

            assertDeleteSingleAppData(m, service, appName);

            _log.info(m + " ----------------testGetAppPrice--FINISH -------------------");
            _log.info(m + " ------------------------------------------------------------");

        }

    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    private void getMultiAppPrices(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final String m = "testGetMultiAppPrices";
        // create
        // get Price list for the app
        // delete
        ProducerServiceRestClient service = null;
        ConsumerServiceRestClient client = null;
        boolean isValidResponse = false;

        String app1 = "myAppConsumerMultiAppPrices1";
        String app2 = "myAppConsumerMultiAppPrices2";
        String app3 = "myAppConsumerMultiAppPrices3";
        String app4 = "myAppConsumerMultiAppPrices4";

        try {

            _log.info(m + " ----------------testGetMultiAppPrices--START -----------------------");

            service = this.assertCreateMultipleAppData(m, app1, app2, app3, app4);
            // Now get the app names using Consumer
            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking consumer rest client for available app names ----- ");

            try {

                builderConsumer = createConsumerRestClient(m);

                client = builderConsumer
                                .property("com.ibm.ws.jaxrs.client.keepalive.connection", "close")
                                .build(ConsumerServiceRestClient.class);
            } catch (Exception e) {
                _log.severe(m + " , Error creating ConsumerServiceRestClient proxy");
                throw e;
            }

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking Consumer Rest client to get prices for all the apps to test bidi grpc streaming ----- ");

            List<String> appNames = Arrays.asList(app1, app2, app3, app4);

            Response r = client.getPrices(appNames);

            assertEquals(200, r.getStatus());

            String entityAppPrice = r.readEntity(String.class);
            _log.info(m + " entityAppPrice: " + entityAppPrice);

            isValidResponse = entityAppPrice.contains("CREDITCARD");
            assertTrue(isValidResponse);

        } finally {

            _log.info(m + " ------------------------------------------------------------");

            this.assertDeleteMultipleAppData(m, service, app1, app2, app3, app4);
            _log.info(m + " ----------------testGetMultiAppPrices--FINISH -----------------------");
            _log.info(m + " ------------------------------------------------------------");

        }

    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */

    private void getMultiAppNames(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final String m = "testGetMultiAppNames";
        // create
        // query names
        // delete
        ProducerServiceRestClient service = null;

        ConsumerServiceRestClient client = null;

        String app1 = "myAppConsumerMultiAppNames1";
        String app2 = "myAppConsumerMultiAppNames2";
        String app3 = "myAppConsumerMultiAppNames3";
        String app4 = "myAppConsumerMultiAppNames4";

        try {

            _log.info(m + " ----------------testGetMultiAppNames--START -----------------------");

            service = this.assertCreateMultipleAppData(m, app1, app2, app3, app4);

            // Now get the app names using Consumer
            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking consumer rest client for available app names ----- ");

            try {

                client = this.add_AuthHeader_Filter(m, req, resp);

            } catch (Exception e) {
                _log.severe(m + " , Error creating ConsumerServiceRestClient proxy");
                throw e;
            }

            Response r = client.getAllAppNames(m);

            assertEquals(200, r.getStatus());

            String entityName = r.readEntity(String.class);
            _log.info(m + " entityName: " + entityName);
            // expected output, but there is no order since it is client streaming, assert them individually, examples
            //entityName: ["myApp1","myApp2","myApp3","myApp4"]
            //entityName: ["myAppConsumer4","myAppConsumer3","myAppConsumer2","myAppConsumer1"]

            assertTrue(entityName.contains(app1));
            assertTrue(entityName.contains(app2));
            assertTrue(entityName.contains(app3));
            assertTrue(entityName.contains(app4));

        } finally {

            _log.info(m + " ------------------------------------------------------------");
            this.assertDeleteMultipleAppData(m, service, app1, app2, app3, app4);

            _log.info(m + " ----------------testGetMultiAppNames--FINISH--------------");
            _log.info(m + " ------------------------------------------------------------");

        }

    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    private void getMultiAppsInfo(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final String m = "testGetMultiAppsInfo";
        // create

        // get app info with the name

        // delete
        ProducerServiceRestClient service = null;
        ConsumerServiceRestClient client = null;
        boolean isValidResponse = false;

        String app1 = "myAppConsumer_MultiAppsInfo1";
        String app2 = "myAppConsumer_MultiAppsInfo2";
        String app3 = "myAppConsumer_MultiAppsInfo3";
        String app4 = "myAppConsumer_MultiAppsInfo4";

        try {

            _log.info(m + " -----------------testGetMultiAppsInfo --- START------------------------");

            service = this.assertCreateMultipleAppData(m, app1, app2, app3, app4);

            // Now get the app names using Consumer
            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking consumer rest client for available app names ----- ");

            try {

                builderConsumer = createConsumerRestClient(m);

                client = builderConsumer
                                .property("com.ibm.ws.jaxrs.client.keepalive.connection", "close")
                                .build(ConsumerServiceRestClient.class);
            } catch (Exception e) {
                _log.severe(m + " , Error creating ConsumerServiceRestClient proxy");
                throw e;
            }

            Response r = client.getAppInfo(app3);

            assertEquals(200, r.getStatus());

            String entityAppInfo = r.readEntity(String.class);
            _log.info(m + " entityAppInfo: " + entityAppInfo);

            // Expected output in logs
//            entityAppInfo: {
//                "name": "myApp3",
//                "desc": "Famous myApp3",
//                "id": "25f2934e-9dd2-47c7-bd8a-bf7e82e6436b",
//                "free": true,
//                "genreType": "SOCIAL",
//                "securityType": "TOKEN_JWT",
//                "prices": [{
//                  "purchaseType": "PAYAPL",
//                  "sellingPrice": 2000.0
//                }],
//                "creator": {
//                  "companyName": "ABC",
//                  "email": "abc@comp"
//                }
//              }

            isValidResponse = entityAppInfo.contains(app3);
            assertTrue(isValidResponse);

        } finally {

            _log.info(m + " ------------------------------------------------------------");

            this.assertDeleteMultipleAppData(m, service, app1, app2, app3, app4);

            _log.info(m + " ---------------testGetMultiAppsInfo --- FINISH -----------------");
            _log.info(m + " ------------------------------------------------------------");

        }

    }

    //unused TODO
    /**
     * @param numberofApps
     * @return
     */
    private MultiAppStructues createMultiAppStructure(String numberofApps) {

        // create input data
        AppStructure reqPOJO1 = createAppData("myApp1", "Famous myApp1", true,
                                              AppStructure.SecurityType.NO_SECURITY, AppStructure.GenreType.GAME,
                                              createPriceList(Price.PurchaseType.BLUEPOINTS, 200, null, 100), "ABC", "abc@comp");

        AppStructure reqPOJO2 = createAppData("myApp2", "Famous myApp2", false, AppStructure.SecurityType.BASIC,
                                              AppStructure.GenreType.NEWS, createPriceList(Price.PurchaseType.CREDITCARD, 400, null, 100),
                                              "ABC", "abc@comp");

        AppStructure reqPOJO3 = createAppData("myApp3", "Famous myApp3", true,
                                              AppStructure.SecurityType.TOKEN_JWT, AppStructure.GenreType.SOCIAL,
                                              createPriceList(Price.PurchaseType.PAYAPL, 2000, null, 100), "ABC", "abc@comp");

        AppStructure reqPOJO4 = createAppData("myApp4", "Famous myApp4", false,
                                              AppStructure.SecurityType.TOKEN_OAUTH2, AppStructure.GenreType.GAME,
                                              createPriceList(Price.PurchaseType.PAYAPL, 20000, Price.PurchaseType.CREDITCARD, 3000), "ABC",
                                              "abc@comp");

        MultiAppStructues multiApp = new MultiAppStructues();

        multiApp.setStructureList(Arrays.asList(reqPOJO1, reqPOJO2, reqPOJO3, reqPOJO4));

        return multiApp;
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

    // create token , return token
    /**
     * @param req
     * @param resp
     * @return
     * @throws Exception
     */
    private String getToken(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String builtToken = ConsumerUtils.getJwtFromTokenEndpoint("https://" + req.getServerName() + ":" + ConsumerUtils.getSysProp("bvt.prop.member_1.https") + "/", "defaultJWT",
                                                                  "dev",
                                                                  "hello");

        _log.info("getToken ------------------------------------------------------------ " + builtToken);

        assertNotNull(builtToken);

        return builtToken;
    }

    /**
     * @param req
     * @param resp
     * @return
     * @throws Exception
     */
    private String getBadToken(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        //server.xml has "dev", "hello", send something different
        String builtToken = null;
        try {
            builtToken = ConsumerUtils.getJwtFromTokenEndpoint("https://" + req.getServerName() + ":" + ConsumerUtils.getSysProp("bvt.prop.member_1.https") + "/", "defaultBadJWT",
                                                               "devBad",
                                                               "hello");

            _log.info("getBadToken ------------------------------------------------------------ " + builtToken);
        } catch (Exception e) {
            // expected so eat it.
        }

        assertNull(builtToken);

        return builtToken;
    }

}
