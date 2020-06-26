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
import static org.junit.Assert.assertTrue;

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

import com.ibm.testapp.g3store.restProducer.client.ProducerServiceRestClient;
import com.ibm.testapp.g3store.restProducer.model.AppStructure;
import com.ibm.testapp.g3store.restProducer.model.AppStructure.GenreType;
import com.ibm.testapp.g3store.restProducer.model.AppStructure.SecurityType;
import com.ibm.testapp.g3store.restProducer.model.Creator;
import com.ibm.testapp.g3store.restProducer.model.MultiAppStructues;
import com.ibm.testapp.g3store.restProducer.model.Price;
import com.ibm.testapp.g3store.restProducer.model.Price.PurchaseType;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet(urlPatterns = "/ConsumerEndpointFATServlet")
public class ConsumerEndpointFATServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;
    Logger _log = Logger.getLogger(ConsumerEndpointFATServlet.class.getName());

    private RestClientBuilder builderConsumer;
    private RestClientBuilder builderProducer;

    private static String getSysProp(String key) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
    }

    /**
     *
     */
    @Override
    public void init() throws ServletException {

        // The baseURL URL of the remote endpoint of Producer Wrapper application
        String baseUrlStrProducer = "http://" + "localhost:" + getSysProp("bvt.prop.member_1.http") + "/StoreConsumerApp/v1P/";

        _log.info("baseUrlStrProducer = " + baseUrlStrProducer);

        URL baseUrlProducer;
        try {
            baseUrlProducer = new URL(baseUrlStrProducer);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
        builderProducer = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUrl(baseUrlProducer);

        _log.info("builderProducer = " + builderProducer.toString());

        // The baseURL URL of the remote endpoint of Consumer Wrapper application
        String baseUrlStr = "http://" + "localhost:" + getSysProp("bvt.prop.member_1.http") + "/StoreConsumerApp/v1C/";

        _log.info("baseUrl = " + baseUrlStr);

        URL baseUrl;
        try {
            baseUrl = new URL(baseUrlStr);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
        builderConsumer = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUrl(baseUrl);

        _log.info("builderConsumer = " + builderConsumer.toString());
    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testGetAppInfo(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testGetAppInfo";

        // create
        // query name
        // get app info with the name
        // get Price list for the app
        // delete
        ProducerServiceRestClient service = null;
        String appName = "myApp";
        boolean isValidResponse = false;

        try {
            service = builderProducer.build(ProducerServiceRestClient.class);
        } catch (Exception e) {
            _log.severe("Check ProducerServiceRestClient proxy");
            throw e;
        }

        try {

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking producer rest client to create app: " + appName);
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

            // Now get the data using Consumer
            ConsumerServiceRestClient client = builderConsumer
                            .property("com.ibm.ws.jaxrs.client.keepalive.connection", "close")
                            .build(ConsumerServiceRestClient.class);

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking consumer rest client for available app names");

            r = client.getAllAppNames();

            assertEquals(200, r.getStatus());

            String entityName = r.readEntity(String.class);
            _log.info(m + " entityName: " + entityName);
            // Expected output in logs
            //entityName: ["myApp"]

            isValidResponse = entityName.contains(appName);
            assertTrue(isValidResponse);

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking consumer rest client to get app info: " + appName);

            r = client.getAppInfo(appName);

            assertEquals(200, r.getStatus());

            String entityAppInfo = r.readEntity(String.class);
            _log.info(m + " entityAppInfo: " + entityAppInfo);

            // Expected output in logs
//            entityAppInfo: {
//                "name": "myApp",
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
            _log.info(m + " ----- invoking consumer rest client to get price for the app to test bidi grpc streaming: " + appName);

            List<String> appNames = Arrays.asList(appName);

            r = client.getPrices(appNames);

            assertEquals(200, r.getStatus());

            String entityAppPrice = r.readEntity(String.class);
            _log.info(m + " entityAppPrice: " + entityAppPrice);
            // entityAppPrice: {"appNameswPrice":[{"appName":"myApp","prices":[{"purchaseType":"BLUEPOINTS","sellingPrice":200.0}]}]}
            isValidResponse = entityAppPrice.contains("BLUEPOINTS");
            assertTrue(isValidResponse);

        } finally {

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking producer rest client to delete app: " + appName);
            // call Remote REST service to delete
            Response r2 = service.deleteApp(appName);

            // check response
            int status = r2.getStatus();
            _log.info(m + ": delete status: " + status);

            assertEquals(200, status);

            String entity = r2.readEntity(String.class);
            _log.info(m + ": delete entity: " + entity);

            isValidResponse = entity.contains("The app [myApp] has been removed from the server");

            assertTrue(isValidResponse);

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ------------------------------------------------------------");

        }

    }

    /**
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testGetMultiAppsInfo(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        final String m = "getMultiAppsInfo";
        // create
        // query name
        // get app info with the name
        // get Price list for the app
        // delete
        ProducerServiceRestClient service = null;
        ConsumerServiceRestClient client = null;
        boolean isValidResponse = false;

        try {
            service = builderProducer.build(ProducerServiceRestClient.class);
        } catch (Exception e) {
            _log.severe(m + " , Error creating ProducerServiceRestClient proxy");
            throw e;
        }

        try {

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking producer rest client to create apps to test client streaming  ----- ");

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

            // call Remote REST service

            Response r = service.createMultiApps(multiApp);

            // check response
            int status = r.getStatus();
            _log.info(m + ": create status: " + status);
            assertEquals(200, status);

            String entity = r.readEntity(String.class);
            _log.info(m + ": create entity: " + entity);

            // expected output
//            getMultiAppsInfo: create entity: Store has successfully added the app [myApp1] with id [ 8bd3de2d-b274-4a7c-b289-29e187257d73 ]
//                            Store has successfully added the app [myApp2] with id [ a56d79e7-2731-45c5-9e18-bfaf4bf31f82 ]
//                            Store has successfully added the app [myApp3] with id [ 25f2934e-9dd2-47c7-bd8a-bf7e82e6436b ]
//                            Store has successfully added the app [myApp4] with id [ 68e73541-9183-4cd5-84b4-490ac3331602 ]

            assertTrue(entity.contains("Store has successfully added the app [myApp1]"));
            assertTrue(entity.contains("Store has successfully added the app [myApp2]"));
            assertTrue(entity.contains("Store has successfully added the app [myApp3]"));
            assertTrue(entity.contains("Store has successfully added the app [myApp4]"));

            // Now get the app names using Consumer
            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking consumer rest client for available app names ----- ");

            try {
                client = builderConsumer
                                .property("com.ibm.ws.jaxrs.client.keepalive.connection", "close")
                                .build(ConsumerServiceRestClient.class);
            } catch (Exception e) {
                _log.severe(m + " , Error creating ConsumerServiceRestClient proxy");
                throw e;
            }

            r = client.getAllAppNames();

            assertEquals(200, r.getStatus());

            String entityName = r.readEntity(String.class);
            _log.info(m + " entityName: " + entityName);
            // Expected output in logs
            //entityName: ["myApp1","myApp2","myApp3","myApp4"]

            isValidResponse = entityName.contains("[\"myApp1\",\"myApp2\",\"myApp3\",\"myApp4\"]");
            assertTrue(isValidResponse);

            // Now get the data using Consumer
            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking consumer rest client to get app info: " + "myApp3");

            String appName = "myApp3";
            r = client.getAppInfo(appName);

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

            isValidResponse = entityAppInfo.contains(appName);
            assertTrue(isValidResponse);

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking Consumer Rest client to get prices for all the apps to test bidi grpc streaming ----- ");

            List<String> appNames = Arrays.asList("myApp1", "myApp2", "myApp3", "myApp4");

            r = client.getPrices(appNames);

            assertEquals(200, r.getStatus());

            String entityAppPrice = r.readEntity(String.class);
            _log.info(m + " entityAppPrice: " + entityAppPrice);

            isValidResponse = entityAppPrice.contains("CREDITCARD");
            assertTrue(isValidResponse);

        } finally {

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ----- invoking Producer Rest client to delete apps to test grpc server streaming ----- ");
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

            isValidResponse = entity.contains("The app [myApp1] has been removed from the Store. "
                                              + "The app [myApp2] has been removed from the Store. "
                                              + "The app [myApp3] has been removed from the Store. "
                                              + "The app [myApp4] has been removed from the Store.");

            assertTrue(isValidResponse);

            _log.info(m + " ------------------------------------------------------------");
            _log.info(m + " ------------------------------------------------------------");

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

}
