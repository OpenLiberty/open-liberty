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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.testapp.g3store.exception.InvalidArgException;
import com.ibm.testapp.g3store.exception.NotFoundException;
import com.ibm.testapp.g3store.exception.UnauthException;
import com.ibm.testapp.g3store.grpcConsumer.api.ConsumerGrpcServiceClientImpl;

/**
 * Servlet implementation class ConsumerServlet
 */
@WebServlet("/ConsumerServlet")
public class ConsumerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(ConsumerServlet.class.getName());

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsumerServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // get the values from the request

        String address = request.getParameter("address");
        String port = request.getParameter("port");

        // call the API which will further call gRPC

        List<String> list = getAllAppNames(address, Integer.parseInt(port));

        // or

        String appName = request.getParameter("appName");

        String structure = getAppInfo(appName, address, Integer.parseInt(port));

        // TODO Auto-generated method stub
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        doGet(request, response);
    }

    /**
     * @param address
     * @param port
     * @return
     */
    private List<String> getAllAppNames(String address, int port) {

        String m = "getAllAppNames";

        if (log.isLoggable(Level.FINE)) {
            log.finest("ConsumerServlet: getAllAppNames: Received request to get AppNames");
        }

        ConsumerGrpcServiceClientImpl helper = new ConsumerGrpcServiceClientImpl();

        // start service
        helper.startService_BlockingStub(address, port);

        // get the value from RPC
        List<String> nameList = null;
        try {
            nameList = helper.getAllAppNameList(m);
        } catch (NotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        } catch (UnauthException e) {
            e.printStackTrace();
        }
        // stop service
        helper.stopService();

        return nameList;

    }

    /**
     * @param appName
     * @param address
     * @param port
     * @return
     */
    private String getAppInfo(String appName, String address, int port) {

        if (log.isLoggable(Level.FINE)) {
            log.finest("ConsumerServlet: getAppInfo: Received request to get app info");
        }

        ConsumerGrpcServiceClientImpl helper = new ConsumerGrpcServiceClientImpl();

        // start service
        helper.startService_BlockingStub(address, port);

        String appStruct = null;
        try {
            appStruct = helper.getAppJSONStructure(appName, "getAppInfo");
        } catch (InvalidArgException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnauthException e) {
            e.printStackTrace();
        }

        // this String is in JSON format

        return appStruct;

    }

}
