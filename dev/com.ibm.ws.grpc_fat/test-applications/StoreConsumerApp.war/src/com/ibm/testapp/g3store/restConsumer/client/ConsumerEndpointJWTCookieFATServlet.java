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
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
 *         This class is only required due to feature jwtSso-1.0
 *
 */
@WebServlet(urlPatterns = "/ConsumerEndpointJWTCookieFATServlet")
public class ConsumerEndpointJWTCookieFATServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;
    Logger _log = Logger.getLogger(ConsumerEndpointJWTCookieFATServlet.class.getName());

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
     * This test will send a valid JWT token in Cookie header added via ClientRequestFilter
     * The Cookie header will be propagated using GrpcTarget.
     *
     * This test will sent grpc requests to create data, getAppName with JWT token Cookie header , delete data.
     * The test passes when correct "appName" is asserted in response.
     *
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testGetApp_JWTCookie_GrpcTarget(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        this.getAppName_CookieAuth_GrpcTarget(req, resp);
    }

    /**
     * This test will send a valid JWT token in Cookie header added via ClientRequestFilter
     * The Cookie header will be propagated using GrpcTarget.
     *
     * This test will sent grpc requests to create data, getAppName with JWT token Cookie header , delete data.
     * But the server side will have bad RolesAllowed set,
     *
     * The test passes when "Expected auth failure" is asserted in response.
     *
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testGetApp_BadRole_JWTCookie_GrpcTarget(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        this.getAppName_BadRole_CookieAuth_GrpcTarget(req, resp);
    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    private void getAppName_CookieAuth_GrpcTarget(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final String m = "testGetAppName_CookieAuth_GrpcTarget";

        // create
        // secure query app name
        // delete
        ProducerServiceRestClient service = null;
        String appName = "myAppConsumerJWTCookie";
        boolean isValidResponse = false;

        try {

            _log.info(m + " ------------------ " + m + "--START ---------------------");

            service = assertCreateSingleAppData(m, appName);

            _log.info(m + " ----- invoking consumer rest client for available app names");

            ConsumerServiceRestClient client = null;
            // Now get the data using Consumer
            try {
                client = this.add_AuthCookieHeader_Filter(m, req, resp);
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
     * @throws Exception
     */
    private void getAppName_BadRole_CookieAuth_GrpcTarget(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final String m = "testGetAppName_BadRole_CookieAuth_GrpcTarget";

        // create
        // secure query app name
        // delete
        ProducerServiceRestClient service = null;
        String appName = "myAppConsumerBadRoleJWTCookie";
        boolean isValidResponse = false;

        try {

            _log.info(m + " ------------------ " + m + "--START ---------------------");

            service = assertCreateSingleAppData(m, appName);

            _log.info(m + " ----- invoking consumer rest client for available app names");

            ConsumerServiceRestClient client = null;
            // Now get the data using Consumer
            try {
                client = this.add_AuthCookieHeader_Filter(m, req, resp);
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
     * @param req
     * @param resp
     * @return
     * @throws Exception
     */
    private ConsumerServiceRestClient add_AuthCookieHeader_Filter(String m, HttpServletRequest req, HttpServletResponse resp) throws Exception {

        builderConsumer = createConsumerRestClient(m);

        ConsumerServiceRestClient client = builderConsumer
                        .property("com.ibm.ws.jaxrs.client.keepalive.connection", "close")
                        .register(
                                  (ClientRequestFilter) requestContext -> {
                                      MultivaluedMap<String, Object> headers = requestContext.getHeaders();
                                      try {
                                          _log.info(m + " ----------- Add Cookie JWT Token to the request --------------");

                                          if (headers.containsKey("Cookie")) {

                                              List<Object> cookieValues = headers.get("Cookie");

                                              String cookieString = cookieValues.toString();

                                              _log.info(m + " read existing Cookies " + cookieString);

                                              cookieString = cookieString + "; " + "myjwtCookie" + "=" + getToken(req, resp);

                                              List<Object> myList = new ArrayList<Object>(Arrays.asList(cookieString.split("; ")));

                                              headers.put("Cookie", myList);

                                          } else {
                                              String cookieString = "myjwtCookie" + "=" + getToken(req, resp);
                                              _log.info(m + " add new Cookie header " + cookieString);
                                              headers.put("Cookie", new ArrayList<Object>(Arrays.asList(cookieString)));
                                          }

                                          for (Entry<String, List<Object>> entry : headers.entrySet()) {
                                              _log.info(m + "Request headers: " + entry.getKey() + ", set to: " + entry.getValue());
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

}
