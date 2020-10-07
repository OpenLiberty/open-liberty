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
package com.ibm.testapp.g3store.servletConsumer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.testapp.g3store.exception.InvalidArgException;
import com.ibm.testapp.g3store.exception.UnauthException;
import com.ibm.testapp.g3store.grpcConsumer.api.ConsumerGrpcServiceClientImpl;
import com.ibm.testapp.g3store.restConsumer.model.AppNamewPriceListPOJO;
import com.ibm.testapp.g3store.restConsumer.model.PriceModel;
import com.ibm.testapp.g3store.restConsumer.model.PriceModel.PurchaseType;
import com.ibm.testapp.g3store.utilsConsumer.ConsumerUtils;

/**
 * Servlet implementation class ConsumerServlet
 */
@WebServlet("/ConsumerServlet")
public class ConsumerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected static final Class<?> c = ConsumerServlet.class;

    private static final Logger log = Logger.getLogger(c.getName());

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsumerServlet() {
        super();
    }

    ConsumerGrpcServiceClientImpl consumerhelper = null;

    @Override
    public void init() throws ServletException {
        consumerhelper = new ConsumerGrpcServiceClientImpl();
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
                        .append("                       <title>Store Consumer Client</title>\r\n")
                        .append("               </head>\r\n")
                        .append("               <body>\r\n")
                        .append("                       <h3>gRPC Store Consumer client</h3>\r\n")
                        .append("                       <form action=\"ConsumerServlet\" method=\"POST\" name=\"form1\">\r\n")
                        .append("                               Enter the test Name: \r\n")
                        .append("                               <input type=\"text\" name=\"testName\" />\r\n\r\n")
                        .append("                               <br/>")
                        .append("                               Enter the app name: \r\n")
                        .append("                               <input type=\"text\" value=\"defaultApp\" name=\"appName\" />\r\n\r\n")
                        .append("                               <br/>")
                        .append("                               <br/>")
                        .append("                               <input type=\"submit\" value=\"Submit\" name=\"submit\" />\r\n")
                        .append("                       </form>\r\n")
                        .append("               </body>\r\n")
                        .append("</html>\r\n");
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // get the values from the request

        String testToInvoke = request.getParameter("testName");

        if ("getAppInfo".equalsIgnoreCase(testToInvoke)) {
            String m = testToInvoke;
            try {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-START-----------------------");
                String appName = request.getParameter("appName");
                this.getAppInfo(consumerhelper, response, appName, testToInvoke);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-FINISH-----------------------");
            }

        }

        else if ("getAppPrice".equalsIgnoreCase(testToInvoke)) {
            String m = testToInvoke;
            try {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-START-----------------------");
                String appName = request.getParameter("appName");
                this.getAppPrice(consumerhelper, response, appName, testToInvoke);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                log.info(m + " ----------------------------------------------------------------");
                log.info(m + " ------------" + m + "-FINISH-----------------------");
            }

        }

    }

    /**
     * @param consumerhelper2
     * @param response
     * @param appName
     * @param testToInvoke
     */
    private void getAppPrice(ConsumerGrpcServiceClientImpl consumerhelper2, HttpServletResponse response, String appName, String testToInvoke) throws Exception {
        final String m = testToInvoke;
        try {

            // create grpc client
            consumerhelper2.startService_AsyncStub(ConsumerUtils.getStoreServerHost(), ConsumerUtils.getStoreServerPort());

            log.info(m + " ------------------------------------------------------------");
            log.info(m + " ----- get price for the app to test bidi grpc streaming: " + appName);

            //call the gRPC API and get response
            List<AppNamewPriceListPOJO> listOfAppNames_w_PriceList = consumerhelper2.getAppswPrices(Arrays.asList(appName));
            String priceType = null;

            if (listOfAppNames_w_PriceList != null) {
                for (int i = 0; i < listOfAppNames_w_PriceList.size(); i++) {
                    List<PriceModel> pricelist = listOfAppNames_w_PriceList.get(i).getPrices();
                    if (pricelist != null) {
                        for (int j = 0; j < pricelist.size(); j++) {
                            PurchaseType ptype = pricelist.get(j).getPurchaseType();
                            log.info(m + " -----ptype=" + ptype);
                            if (ptype.toString() == "BLUEPOINTS") {
                                priceType = "BLUEPOINTS";
                            }
                        }
                    }
                }
            }

            // create HTML response
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            writer.append("<!DOCTYPE html>\r\n")
                            .append("<html>\r\n")
                            .append("               <head>\r\n")
                            .append("                       <title>Get App response message</title>\r\n")
                            .append("               </head>\r\n")
                            .append("               <body>\r\n");
            if (priceType != null) {

                writer.append("<h3>getAppInfo: </h3>\r\n");
                writer.append(priceType);
            }

        } catch (Exception e) {
            log.info(m + " " + e.getMessage());
            throw e;

        } finally {
            // stop the grpc service
            consumerhelper2.stopService();
        }

    }

    /**
     * @param consumerhelper2
     * @param response
     * @throws IOException
     */
    private void getAppInfo(ConsumerGrpcServiceClientImpl consumerhelper2, HttpServletResponse response, String appNameInput, String testToInvoke) throws Exception {

        String appInfo_JSONString = null;
        try {
            // create grpc client
            consumerhelper2.startService_BlockingStub(ConsumerUtils.getStoreServerHost(), ConsumerUtils.getStoreServerPort());

            appInfo_JSONString = consumerhelper2.getAppJSONStructure(appNameInput, testToInvoke);

            log.info(testToInvoke + ": request to get appInfo has been completed by Consumer Servlet " + appInfo_JSONString);
        } catch (InvalidArgException e) {
            throw e;
        } catch (UnauthException e) {
            appInfo_JSONString = e.getMessage();
        } finally {
            // stop the grpc service
            consumerhelper2.stopService();
        }

        // create HTML response
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.append("<!DOCTYPE html>\r\n")
                        .append("<html>\r\n")
                        .append("               <head>\r\n")
                        .append("                       <title>Get App response message</title>\r\n")
                        .append("               </head>\r\n")
                        .append("               <body>\r\n");
        if (appInfo_JSONString != null) {

            writer.append("<h3>getAppInfo: </h3>\r\n");
            writer.append(appInfo_JSONString);
        } else {
            writer.append("<h3>FAILED </h3>\r\n");
        }

    }

}
