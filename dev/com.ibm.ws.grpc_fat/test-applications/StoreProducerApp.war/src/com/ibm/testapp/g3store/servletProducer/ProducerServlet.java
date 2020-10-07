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
package com.ibm.testapp.g3store.servletProducer;

import static com.ibm.ws.fat.grpc.monitoring.GrpcMetricsTestUtils.getMetric;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.testapp.g3store.exception.AlreadyExistException;
import com.ibm.testapp.g3store.exception.HandleExceptionsAsyncgRPCService;
import com.ibm.testapp.g3store.exception.InvalidArgException;
import com.ibm.testapp.g3store.grpcProducer.api.ProducerGrpcServiceClientImpl;
import com.ibm.testapp.g3store.restProducer.model.AppStructure;
import com.ibm.testapp.g3store.restProducer.model.DeleteAllRestResponse;
import com.ibm.testapp.g3store.restProducer.model.MultiAppStructues;
import com.ibm.testapp.g3store.restProducer.model.Price;
import com.ibm.testapp.g3store.restProducer.model.ProducerRestResponse;
import com.ibm.testapp.g3store.utilsProducer.ProducerUtils;

/**
 * @author anupag
 *
 *         This class is servlet client implementation of producer APIs. Each
 *         API will further create a gRPC server connection to StoreApp and send
 *         gRPC requests.
 *
 *         This class API will be called from
 *
 */
@WebServlet(urlPatterns = "/ProducerServlet")
public class ProducerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger(ProducerServlet.class.getName());

    ProducerGrpcServiceClientImpl helper = null;

    @Override
    public void init() throws ServletException {
        helper = new ProducerGrpcServiceClientImpl();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // set response headers
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        // create HTML form
        PrintWriter writer = response.getWriter();
        writer.append("<!DOCTYPE html>\r\n")
                        .append("<html>\r\n")
                        .append("               <head>\r\n")
                        .append("                       <title>Store Producer Client</title>\r\n")
                        .append("               </head>\r\n")
                        .append("               <body>\r\n")
                        .append("                       <h3>gRPC Store Producer client</h3>\r\n")
                        .append("                       <form action=\"ProducerServlet\" method=\"POST\" name=\"form1\">\r\n")
                        .append("                               Enter the test Name: \r\n")
                        .append("                               <input type=\"text\" name=\"testName\" />\r\n\r\n")
                        .append("                               <br/>")
                        .append("                               Enter the app name: \r\n")
                        .append("                               <input type=\"text\" value=\"defaultApp\" name=\"appName\" />\r\n\r\n")
                        .append("                               <br/>")
                        .append("                               <br/>")
                        .append("                               <br/>")
                        .append("                               <input type=\"submit\" value=\"Submit\" name=\"submit\" />\r\n")
                        .append("                       </form>\r\n")
                        .append("               </body>\r\n")
                        .append("</html>\r\n");
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String testToInvoke = request.getParameter("testName");

        if ("createApp".equalsIgnoreCase(testToInvoke)) {
            String m = testToInvoke;
            try {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-START-----------------------");
                String appName = request.getParameter("appName");
                this.createApp(helper, response, appName);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-FINISH-----------------------");
            }

        }

        else if ("createMultiApps".equalsIgnoreCase(testToInvoke)) {
            String m = testToInvoke;
            try {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-START-----------------------");
                createMultiApps(helper, response);

            } catch (Exception e) {

                e.printStackTrace();
            } finally {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-FINISH-----------------------");
            }

        }

        else if ("deleteApp".equalsIgnoreCase(testToInvoke)) {
            String m = testToInvoke;
            try {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-START-----------------------");
                String appName = request.getParameter("appName");
                this.deleteApp(helper, response, appName);

            } catch (Exception e) {

                e.printStackTrace();
            } finally {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-FINISH-----------------------");
            }

        }

        else if ("deleteMultiApps".equalsIgnoreCase(testToInvoke)) {
            String m = testToInvoke;
            try {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-START-----------------------");
                deleteMultiApps(helper, response);

            } catch (Exception e) {

                e.printStackTrace();
            } finally {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-FINISH-----------------------");
            }

        }

        else if ("clientStreaming".equalsIgnoreCase(testToInvoke)) {
            String m = testToInvoke;
            try {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-START-----------------------");
                clientStreaming(helper, response);

            } catch (Exception e) {

                e.printStackTrace();
            } finally {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-FINISH-----------------------");
            }

        }

        else if ("clientStreamingMetrics".equalsIgnoreCase(testToInvoke)) {
            String m = testToInvoke;
            try {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-START-----------------------");
                clientStreamingMetrics(helper, response);

            } catch (Exception e) {

                e.printStackTrace();
            } finally {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-FINISH-----------------------");
            }

        }

        else if ("serverStreaming".equalsIgnoreCase(testToInvoke)) {
            String m = testToInvoke;
            try {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-START-----------------------");
                serverStreaming(helper, response);

            } catch (Exception e) {

                e.printStackTrace();
            } finally {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-FINISH-----------------------");
            }

        }

        else if ("serverStreamingMetrics".equalsIgnoreCase(testToInvoke)) {
            String m = testToInvoke;
            try {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-START-----------------------");
                serverStreamingMetrics(helper, response);

            } catch (Exception e) {

                e.printStackTrace();
            } finally {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-FINISH-----------------------");
            }

        }

        else if ("twoWayStreamAppAsyncFlagTrue".equalsIgnoreCase(testToInvoke)) {
            String m = testToInvoke;
            try {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-START-----------------------");
                twoWayStreamApp(helper, response, true);

            } catch (Exception e) {

                e.printStackTrace();
            }

        }

        else if ("twoWayStreamAppAsyncFlagFalse".equalsIgnoreCase(testToInvoke)) {
            String m = testToInvoke;
            try {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-START-----------------------");
                twoWayStreamApp(helper, response, false);

            } catch (Exception e) {

                e.printStackTrace();
            } finally {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-FINISH-----------------------");
            }

        }

    }

    /**
     * @param helper2
     * @param response
     * @throws IOException
     */
    private void serverStreamingMetrics(ProducerGrpcServiceClientImpl helper2, HttpServletResponse response) throws IOException {

        // retrieve the metrics
        int httpPort = Integer.parseInt(ProducerUtils.getSysProp("bvt.prop.HTTP_secondary"));
        String metricValue = getMetric("localhost", httpPort, "/metrics/vendor/grpc.client.receivedMessages.total");

        // create HTML response
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.append("<!DOCTYPE html>\r\n")
                        .append("<html>\r\n")
                        .append("               <head>\r\n")
//                        .append("                       <title>serverStreaming metric response message</title>\r\n")
                        .append("               </head>\r\n")
                        .append("               <body>\r\n");
        if (metricValue != null) {

            writer.append(metricValue);
        }

    }

    /**
     * @param helper2
     * @param response
     * @throws IOException
     */
    private void clientStreamingMetrics(ProducerGrpcServiceClientImpl helper2, HttpServletResponse response) throws IOException {

        // retrieve the metrics
        int httpPort = Integer.parseInt(ProducerUtils.getSysProp("bvt.prop.HTTP_default"));
        String metricValue = getMetric("localhost", httpPort, "/metrics/vendor/grpc.server.receivedMessages.total");

        // create HTML response
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.append("<!DOCTYPE html>\r\n")
                        .append("<html>\r\n")
                        .append("               <head>\r\n")
//                        .append("                       <title>clientStreaming metric response message</title>\r\n")
                        .append("               </head>\r\n")
                        .append("               <body>\r\n");
        if (metricValue != null) {

            writer.append(metricValue);
        }

    }

    // ----------------------------------------------------------------------------------------

    public final static int CLIENT_STREAM_MAX_STRESS_CONNECTIONS = 100;
    public final static int CLIENT_STREAM_SLEEP_BETWEEN_STARTING_CONNECTIONS_MSEC = 100;
    public final static int CLIENT_STREAM_TIMEOUT_WAITING_FOR_TEST_COMPLETE_SEC = 60;
    public final static int CLIENT_STREAM_NUMBER_OF_CONCURRENT_CONNECTIONS = 1;

    public static CountDownLatch stressLatch = null;

    /**
     * @param helper
     * @param response
     * @throws Exception
     */
    private void clientStreaming(ProducerGrpcServiceClientImpl helper, HttpServletResponse response) throws Exception {

        int numOfConnections = CLIENT_STREAM_NUMBER_OF_CONCURRENT_CONNECTIONS;
        ClientStreamThread[] ta = new ClientStreamThread[CLIENT_STREAM_NUMBER_OF_CONCURRENT_CONNECTIONS];
        int countConnectionsSuccess = 0;
        int countConnectionsFailed = 0;

        if (numOfConnections > CLIENT_STREAM_MAX_STRESS_CONNECTIONS) {
            numOfConnections = CLIENT_STREAM_MAX_STRESS_CONNECTIONS;
        }
        stressLatch = new CountDownLatch(numOfConnections);

        // stuff to call code the will be the grpc client logic

        // Get the input parameters from the REST request
        // Each parameter value will have to be transferred to the grpc request object
        log.info("clientStreamAppStress(): request to run clientStreamAppStress with count: " + numOfConnections);

        // create grpc client
        helper.startService_AsyncStub(ProducerUtils.getStoreServerHost(), ProducerUtils.getStoreServerPort());

        try {
            for (int i = 0; i < numOfConnections; i++) {
                ta[i] = new ClientStreamThread(i);
                Thread t = new Thread(ta[i]);
                t.start();
                if (CLIENT_STREAM_SLEEP_BETWEEN_STARTING_CONNECTIONS_MSEC > 0) {
                    Thread.sleep(CLIENT_STREAM_SLEEP_BETWEEN_STARTING_CONNECTIONS_MSEC);
                }
            }

            try {
                stressLatch.await(CLIENT_STREAM_TIMEOUT_WAITING_FOR_TEST_COMPLETE_SEC, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }

            for (int j = 0; j < numOfConnections; j++) {
                if (ta[j].getResult().indexOf("success") != -1) {
                    countConnectionsSuccess++;
                } else {
                    countConnectionsFailed++;
                }
            }

            log.info("clientStreamApp: Success Count: " + countConnectionsSuccess + " Failed Count: " + countConnectionsFailed);

            if (stressLatch.getCount() != 0) {
                log.info("clientStreamApp: not all threads completed on time.  outstanding count: " + stressLatch.getCount());
            }

            String resultString = "NotSet";
            if (countConnectionsSuccess == numOfConnections) {
                resultString = "success. number of connections " + countConnectionsSuccess;
            } else {
                resultString = "Failed. TotalCount " + numOfConnections + " Worked " + countConnectionsSuccess;
            }

            // create HTML response
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            writer.append("<!DOCTYPE html>\r\n")
                            .append("<html>\r\n")
                            .append("               <head>\r\n")
                            .append("                       <title>clientStreaming response message</title>\r\n")
                            .append("               </head>\r\n")
                            .append("               <body>\r\n");
            if (resultString != null) {

                writer.append(resultString);
            }
        } finally {
            // stop the grpc service
            helper.stopService();
        }

    }

    class ClientStreamThread implements Runnable {

        int id = -1;
        String result = "NotDone";

        public ClientStreamThread(int in_id) {
            id = in_id;
        }

        @Override
        public void run() {
            try {
                log.info("clientStreamThread: " + id + " started at:   " + System.currentTimeMillis());
                result = helper.grpcClientStreamApp();
                log.info("clientStreamThread: " + id + " completed at: " + System.currentTimeMillis() + " with result: " + result);
            } finally {
                stressLatch.countDown();
            }
        }

        public String getResult() {
            return result;
        }
    }

    // ----------------------------------------------------------------------------------------

    public final static int SERVER_STREAM_MAX_STRESS_CONNECTIONS = 100;
    public final static int SERVER_STREAM_SLEEP_BETWEEN_STARTING_CONNECTIONS_MSEC = 100;
    public final static int SERVER_STREAM_TIMEOUT_WAITING_FOR_TEST_COMPLETE_SEC = 60;
    public final static int SERVER_STREAM_NUMBER_OF_CONCURRENT_CONNECTIONS = 1;

    /**
     * @param helper
     * @param response
     * @throws Exception
     */
    private void serverStreaming(ProducerGrpcServiceClientImpl helper, HttpServletResponse response) throws Exception {
        int numOfConnections = SERVER_STREAM_NUMBER_OF_CONCURRENT_CONNECTIONS;
        ServerStreamThread[] ta = new ServerStreamThread[SERVER_STREAM_NUMBER_OF_CONCURRENT_CONNECTIONS];
        int countConnectionsSuccess = 0;
        int countConnectionsFailed = 0;

        if (numOfConnections > SERVER_STREAM_MAX_STRESS_CONNECTIONS) {
            numOfConnections = SERVER_STREAM_MAX_STRESS_CONNECTIONS;
        }
        stressLatch = new CountDownLatch(numOfConnections);

        // Get the input parameters from the REST request
        // Each parameter value will have to be transferred to the grpc request object
        log.info("serverStreamApp(): request to run serverStreamApp test received by ProducerRestEndpoint ");

        // create grpc client
        helper.startService_AsyncStub(ProducerUtils.getStoreServerHost(), ProducerUtils.getStoreServerPort());

        try {
            for (int i = 0; i < numOfConnections; i++) {
                ta[i] = new ServerStreamThread(i);
                Thread t = new Thread(ta[i]);
                t.start();
                if (SERVER_STREAM_SLEEP_BETWEEN_STARTING_CONNECTIONS_MSEC > 0) {
                    Thread.sleep(SERVER_STREAM_SLEEP_BETWEEN_STARTING_CONNECTIONS_MSEC);
                }
            }

            try {
                stressLatch.await(SERVER_STREAM_TIMEOUT_WAITING_FOR_TEST_COMPLETE_SEC, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }

            for (int j = 0; j < numOfConnections; j++) {
                if (ta[j].getResult().indexOf("success") != -1) {
                    countConnectionsSuccess++;
                } else {
                    countConnectionsFailed++;
                }
            }

            log.info("serverStreamApp: Success Count: " + countConnectionsSuccess + " Failed Count: " + countConnectionsFailed);

            String resultString = "NotSet";
            if (countConnectionsSuccess == numOfConnections) {
                resultString = "success. number of connections " + countConnectionsSuccess;
            } else {
                resultString = "Failed. TotalCount " + numOfConnections + " Worked " + countConnectionsSuccess;
            }

            // create HTML response
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            writer.append("<!DOCTYPE html>\r\n")
                            .append("<html>\r\n")
                            .append("               <head>\r\n")
                            .append("                       <title>serverStreaming response message</title>\r\n")
                            .append("               </head>\r\n")
                            .append("               <body>\r\n");
            if (resultString != null) {
                writer.append(resultString);
            }

        } catch (InvalidArgException e) {
            e.printStackTrace();

        } finally {
            // stop the grpc service
            helper.stopService();
        }
    }

    class ServerStreamThread implements Runnable {

        int id = -1;
        String result = "NotDone";

        public ServerStreamThread(int in_id) {
            id = in_id;
        }

        @Override
        public void run() {
            try {
                log.info("serverStreamThread: " + id + " started at:   " + System.currentTimeMillis());
                result = helper.grpcServerStreamApp();
                log.info("serverStreamThread: " + id + " completed at: " + System.currentTimeMillis() + " with result: " + result);
            } finally {
                stressLatch.countDown();
            }
        }

        public String getResult() {
            return result;
        }
    }

    // ----------------------------------------------------------------------------------------

    /**
     * @param helper
     * @param response
     * @throws Exception
     */
    private void deleteMultiApps(ProducerGrpcServiceClientImpl helper, HttpServletResponse response) throws Exception {

        String deleteSuccess = null;
        try {
            try {
                // create grpc client
                helper.startService_BlockingStub(ProducerUtils.getStoreServerHost(), ProducerUtils.getStoreServerPort());

                DeleteAllRestResponse deleteResponse = helper.deleteMultiAppsinStore();

                deleteSuccess = deleteResponse.getDeleteResult();
            } catch (Exception e) {

                e.printStackTrace();

            } finally {
                // stop the grpc service
                helper.stopService();
            }

            log.info("deleteAllApps, request to delete apps has been completed by ProducerRestEndpoint, result =  " + deleteSuccess);

            // create HTML response
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            writer.append("<!DOCTYPE html>\r\n")
                            .append("<html>\r\n")
                            .append("               <head>\r\n")
                            .append("                       <title>Delete multi App response message</title>\r\n")
                            .append("               </head>\r\n")
                            .append("               <body>\r\n");
            if (deleteSuccess != null) {

                writer.append("<h3>deleteResult: </h3>\r\n");
                writer.append(deleteSuccess);
            } else {
                writer.append("<h3>FAILED </h3>\r\n");
            }

        } catch (Exception e) {
            //"getWriter failed"
            e.printStackTrace();

        }
    }

    // ----------------------------------------------------------------------------------------

    private void deleteApp(ProducerGrpcServiceClientImpl helper, HttpServletResponse response, String appNameInput) throws Exception {
        String appName = appNameInput;
        String deleteSuccess = null;
        try {
            try {
                // create grpc client
                helper.startService_BlockingStub(ProducerUtils.getStoreServerHost(), ProducerUtils.getStoreServerPort());

                deleteSuccess = helper.deleteSingleAppinStore(appName);
                log.info("deleteApp, request to delete app has been completed by ProducerRestEndpoint, result =  "
                         + deleteSuccess);
            } catch (Exception e) {

                e.printStackTrace();

            } finally {
                // stop the grpc service
                helper.stopService();
            }

            // create HTML response
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            writer.append("<!DOCTYPE html>\r\n")
                            .append("<html>\r\n")
                            .append("               <head>\r\n")
                            .append("                       <title>Delete App response message</title>\r\n")
                            .append("               </head>\r\n")
                            .append("               <body>\r\n");
            if (deleteSuccess != null) {

                writer.append("<h3>deleteResult: </h3>\r\n");
                writer.append(deleteSuccess);
            } else {
                writer.append("<h3>FAILED </h3>\r\n");
            }

        } catch (Exception e) {
            //"getWriter failed"
            e.printStackTrace();

        }

    }

    // ----------------------------------------------------------------------------------------

    /**
     * @param helper
     * @param response
     * @throws Exception
     */
    private void createMultiApps(ProducerGrpcServiceClientImpl helper, HttpServletResponse response) throws Exception {
        String m = "testCreateMulitDeleteAllApp";

        log.info(m + " -----------------------------------------------------------------");
        log.info(m + " ---------------testCreateDeleteMultiApp---START---------------");

        String app1 = "myApp1";
        String app2 = "myApp2";
        String app3 = "myApp3";
        String app4 = "myApp4";

        try {

            log.info(m + " ------------------------------------------------------------");
            log.info(m + " ----- invoking Producer rest client to create apps to test client streaming  ----- ");

            // create input data
            AppStructure reqPOJO1 = ProducerUtils.createAppData(app1, "Famous myApp1", true,
                                                                AppStructure.SecurityType.NO_SECURITY, AppStructure.GenreType.GAME,
                                                                ProducerUtils.createPriceList(Price.PurchaseType.BLUEPOINTS, 200, null, 100), "ABC", "abc@comp");

            AppStructure reqPOJO2 = ProducerUtils.createAppData(app2, "Famous myApp2", true, AppStructure.SecurityType.BASIC,
                                                                AppStructure.GenreType.NEWS, ProducerUtils.createPriceList(Price.PurchaseType.CREDITCARD, 400, null, 100),
                                                                "ABC", "abc@comp");

            AppStructure reqPOJO3 = ProducerUtils.createAppData(app3, "Famous myApp3", true,
                                                                AppStructure.SecurityType.TOKEN_JWT, AppStructure.GenreType.SOCIAL,
                                                                ProducerUtils.createPriceList(Price.PurchaseType.PAYAPL, 2000, null, 100), "ABC", "abc@comp");

            AppStructure reqPOJO4 = ProducerUtils.createAppData(app4, "Famous myApp4", true,
                                                                AppStructure.SecurityType.TOKEN_OAUTH2, AppStructure.GenreType.GAME,
                                                                ProducerUtils.createPriceList(Price.PurchaseType.PAYAPL, 20000, Price.PurchaseType.CREDITCARD, 3000), "ABC",
                                                                "abc@comp");

            MultiAppStructues multiApp = new MultiAppStructues();

            multiApp.setStructureList(Arrays.asList(reqPOJO1, reqPOJO2, reqPOJO3, reqPOJO4));

            // create grpc client
            helper.startService_AsyncStub(ProducerUtils.getStoreServerHost(), ProducerUtils.getStoreServerPort());
            HandleExceptionsAsyncgRPCService handleException = new HandleExceptionsAsyncgRPCService();
            String successResponse = null;
            String errorStringResponse = null;
            try {
                ProducerRestResponse restResponse = helper.createMultiAppsinStore(multiApp, handleException);
                // check if there was any exception thrown
                if ((handleException.getAlExException() != null) || (handleException.getArgException() != null)) {

                    ProducerRestResponse errorResponse = new ProducerRestResponse();

                    if (handleException.getAlExException() != null) {
                        errorResponse.setCreateResult(handleException.getAlExException().getMessage());
                    }
                    if ((handleException.getArgException() != null)) {
                        errorResponse.setCreateResult(handleException.getArgException().getMessage());
                    }

                    log.severe("createMultiApps: ProducerRestEndpoint request to create apps has errors = " + errorResponse.getCreateResult());
                    errorStringResponse = errorResponse.getCreateResult();
                    //return Response.status(Status.BAD_REQUEST).entity(errorResponse.getCreateResult()).build();
                } else {
                    log.info("createMultiApps: request to create apps has been completed by ProducerRestEndpoint = " + restResponse.getCreateResult());
                    successResponse = restResponse.getCreateResult();
                    //return Response.ok().entity(response.getCreateResult()).build();
                }

            } finally {
                // stop the grpc service
                helper.stopService();
            }

            // create HTML response
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            writer.append("<!DOCTYPE html>\r\n")
                            .append("<html>\r\n")
                            .append("               <head>\r\n")
                            .append("                       <title>Create multi App response message</title>\r\n")
                            .append("               </head>\r\n")
                            .append("               <body>\r\n");
            if (successResponse != null) {

                writer.append("<h3>createResult: </h3>\r\n");
                writer.append(successResponse);
            } else {
                writer.append("<h3>FAILED </h3>\r\n");
                writer.append(errorStringResponse);
            }

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

        }

    }

    // ----------------------------------------------------------------------------------------

    private void createApp(ProducerGrpcServiceClientImpl helper, HttpServletResponse response, String appNameInput) throws Exception {
        String id = null;
        String appName = appNameInput;
        // create input data
        AppStructure reqPOJO = ProducerUtils.createAppData(appName, "Famous myApp", true,
                                                           AppStructure.SecurityType.NO_SECURITY, AppStructure.GenreType.GAME,
                                                           ProducerUtils.createPriceList(Price.PurchaseType.BLUEPOINTS, 200, null, 100), "ABC", "abc@comp");

        try {
            // create grpc client
            helper.startService_BlockingStub(ProducerUtils.getStoreServerHost(), ProducerUtils.getStoreServerPort());

            id = helper.createSingleAppinStore(reqPOJO);
            log.info("createApp: request to create app has been completed by ProducerRestEndpoint " + id);

        } catch (AlreadyExistException ae) {
            log.info("createApp: expected AlreadyExistException is caught for " + appName);

            String excepMessage = ae.getMessage();
            boolean isValidResponse = excepMessage.contains("The app already exist in the Store.\n" +
                                                            "First run the ProducerService to delete the app. AppName = " + appName);

            assertTrue(isValidResponse);
            id = "Expected AlreadyExistException recieved.";
        } finally {
            // stop the grpc service
            helper.stopService();
        }

        // create HTML response
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.append("<!DOCTYPE html>\r\n")
                        .append("<html>\r\n")
                        .append("               <head>\r\n")
                        .append("                       <title>Create App response message</title>\r\n")
                        .append("               </head>\r\n")
                        .append("               <body>\r\n");
        if (id != null) {

            writer.append("<h3>createResult: </h3>\r\n");
            writer.append(id);
        } else {
            writer.append("<h3>FAILED </h3>\r\n");
        }

    }

    // ----------------------------------------------------------------------------------------

    public final static int TWOWAY_STREAM_MAX_STRESS_CONNECTIONS = 100;
    public final static int TWOWAY_STREAM_SLEEP_BETWEEN_STARTING_CONNECTIONS_MSEC = 100;
    public final static int TWOWAY_STREAM_TIMEOUT_WAITING_FOR_TEST_COMPLETE_SEC = 60;
    public final static int TWOWAY_STREAM_NUMBER_OF_CONCURRENT_CONNECTIONS = 1;

    /**
     * @param helper2
     * @param response
     * @param asyncThread
     * @throws Exception
     */
    private void twoWayStreamApp(ProducerGrpcServiceClientImpl helper2, HttpServletResponse response, boolean asyncThread) throws Exception {

        log.info("twoWayStreamApp(): request to run twoWayStreamApp test received by ProducerRestEndpoint.  asyncThread:  " + asyncThread);
        int numOfConnections = TWOWAY_STREAM_NUMBER_OF_CONCURRENT_CONNECTIONS;
        TwoWayStreamThread[] ta = new TwoWayStreamThread[TWOWAY_STREAM_NUMBER_OF_CONCURRENT_CONNECTIONS];
        int countConnectionsSuccess = 0;
        int countConnectionsFailed = 0;

        if (numOfConnections > TWOWAY_STREAM_MAX_STRESS_CONNECTIONS) {
            numOfConnections = TWOWAY_STREAM_MAX_STRESS_CONNECTIONS;
        }
        stressLatch = new CountDownLatch(numOfConnections);

        // create grpc client
        helper.startService_AsyncStub(ProducerUtils.getStoreServerHost(), ProducerUtils.getStoreServerPort());

        try {
            for (int i = 0; i < numOfConnections; i++) {
                ta[i] = new TwoWayStreamThread(i, asyncThread);
                Thread t = new Thread(ta[i]);
                t.start();
                if (TWOWAY_STREAM_SLEEP_BETWEEN_STARTING_CONNECTIONS_MSEC > 0) {
                    Thread.sleep(TWOWAY_STREAM_SLEEP_BETWEEN_STARTING_CONNECTIONS_MSEC);
                }
            }

            try {
                stressLatch.await(TWOWAY_STREAM_TIMEOUT_WAITING_FOR_TEST_COMPLETE_SEC, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }

            for (int j = 0; j < numOfConnections; j++) {
                if (ta[j].getResult().indexOf("success") != -1) {
                    countConnectionsSuccess++;
                } else {
                    countConnectionsFailed++;
                }
            }

            log.info("twoWayStreamApp: Success Count: " + countConnectionsSuccess + " Failed Count: " + countConnectionsFailed);

            String resultString = "NotSet";
            if (countConnectionsSuccess == numOfConnections) {
                resultString = "success. number of connections " + countConnectionsSuccess;
            } else {
                resultString = "Failed. TotalCount " + numOfConnections + " Worked " + countConnectionsSuccess;
            }
            // create HTML response
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            writer.append("<!DOCTYPE html>\r\n")
                            .append("<html>\r\n")
                            .append("               <head>\r\n")
                            .append("                       <title>serverStreaming response message</title>\r\n")
                            .append("               </head>\r\n")
                            .append("               <body>\r\n");
            if (resultString != null) {
                writer.append(resultString);
            }

        } catch (InvalidArgException e) {
            throw e;
        }

        finally {
            // stop this grpc service
            helper.stopService();
        }
    }

    class TwoWayStreamThread implements Runnable {

        int id = -1;
        String result = "NotDone";
        boolean asyncFlag = false;

        public TwoWayStreamThread(int in_id, boolean af) {
            id = in_id;
            asyncFlag = af;
        }

        @Override
        public void run() {
            try {
                log.info("TwoWayStreamThread: " + id + " started at:   " + System.currentTimeMillis());
                result = helper.grpcTwoWayStreamApp(asyncFlag);
                log.info("TwoWayStreamThread: " + id + " completed at: " + System.currentTimeMillis() + " with result: " + result);
            } finally {
                stressLatch.countDown();
            }
        }

        public String getResult() {
            return result;
        }
    }

    // ----------------------------------------------------------------------------------------

}
