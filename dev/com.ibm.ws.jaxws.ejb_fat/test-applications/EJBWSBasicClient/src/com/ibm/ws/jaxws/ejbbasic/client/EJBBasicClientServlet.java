/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejbbasic.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.Addressing;

import com.ibm.ws.jaxws.ejbbasic.view.client.SayHelloInterface;
import com.ibm.ws.jaxws.ejbbasic.view.client.SayHelloService;

/**
 *
 */
@WebServlet("/EJBBasicClientServlet")
public class EJBBasicClientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final long MAX_ASYNC_WAIT_TIME = 30 * 1000;

    @Addressing
    @WebServiceRef(name = "service/UserQueryService", value = UserQueryService.class, wsdlLocation = "WEB-INF/wsdl/UserQueryService.wsdl")
    private UserQuery userQueryService;

    @Addressing
    @WebServiceRef(name = "service/UserQuery", value = UserQueryService.class, wsdlLocation = "WEB-INF/wsdl/UserQueryService.wsdl")
    private UserQuery userQuery;

    @EJB(beanName = "UseQueryEJBBean")
    UserQueryEJBInterface userQueryEJB;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String testMethod = req.getParameter("testMethod");
        if (testMethod == null || testMethod.isEmpty()) {
            throw new ServletException("Unable to detect the testMethod from the request");
        }
        if (testMethod.equals("testQueryUser")) {
            testQueryUser(req, resp);
        } else if (testMethod.equals("testUserNotFoundException")) {
            testUserNotFoundException(req, resp);
        } else if (testMethod.equals("testListUsers")) {
            testListUsers(req, resp);
        } else if (testMethod.equals("testQueryUserBasicAsyncResponse")) {
            testQueryUserBasicAsyncResponse(req, resp);
        } else if (testMethod.equals("testQueryUserBasicAsyncHandler")) {
            testQueryUserBasicAsyncHandler(req, resp);
        } else if (testMethod.equals("testInConsistentNamespace")) {
            testInConsistentNamespace(req, resp);
        } else if (testMethod.equals("testQueryUserBasicAsyncResponse_EJB")) {
            PrintWriter writer = resp.getWriter();
            userQueryEJB.setServerName(req.getServerName());
            userQueryEJB.setServerPort(new Integer(req.getServerPort()).toString());
            String result = userQueryEJB.getUserAsyncResponse("Hakkar");
            writer.write(result);
            writer.flush();
        } else if (testMethod.equals("testQueryUserBasicAsyncHandler_EJB")) {
            PrintWriter writer = resp.getWriter();
            userQueryEJB.setServerName(req.getServerName());
            userQueryEJB.setServerPort(new Integer(req.getServerPort()).toString());
            String result = userQueryEJB.getUserAsyncHandler("Hakkar");
            writer.write(result);
            writer.flush();
        } else {
            throw new ServletException("Unable to recognize the test method " + testMethod);
        }
    }

    private void testUserNotFoundException(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter writer = resp.getWriter();
        try {
            setEndpointAddress((BindingProvider) userQuery, req, "EJBWSBasic/UserQueryService");
            BindingProvider bp = (BindingProvider) userQuery;

            bp.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, "http://ejbbasic.jaxws.ws.ibm.com/UserQuery/getUser");
            userQuery.getUser("none");
            writer.write("FAILED UserNotFoundException is expected");
        } catch (UserNotFoundException_Exception e) {
            String userName = e.getFaultInfo().getUserName();
            if (userName.equals("none")) {
                writer.write("PASS The expected UserNotFoundException is thrown, " + e.getMessage());
            } else {
                writer.write("FAILED User name none not found in the exception message");
            }
        }
    }

    public void testQueryUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter writer = resp.getWriter();
        try {

            setEndpointAddress((BindingProvider) userQueryService, req, "EJBWSBasic/UserQueryService");
            BindingProvider bp = (BindingProvider) userQuery;

            bp.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, "http://ejbbasic.jaxws.ws.ibm.com/UserQuery/getUser");
            UserQuery uq = userQueryService;
            User user = uq.getUser("Illidan Stormrage");
            if (user == null) {
                writer.write("FAILED Expected user instance is not returned");
            } else if (!"Illidan Stormrage".equals(user.getName())) {
                writer.write("FAILED Expected user instance with name Illidan Stormrage is not returned");
            } else {
                writer.write("PASS");
            }
        } catch (UserNotFoundException_Exception e) {
            writer.write("FAILED Unexpected UserNotFoundException is thrown, " + e.getMessage());
        }
    }

    public void testQueryUserBasicAsyncResponse(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        PrintWriter writer = resp.getWriter();
        try {
            setEndpointAddress((BindingProvider) userQuery, req, "EJBWSBasic/UserQueryService");

            BindingProvider bp = (BindingProvider) userQuery;
            bp.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, "http://ejbbasic.jaxws.ws.ibm.com/UserQuery/getUser");
            Response<GetUserResponse> response = userQuery.getUserAsync("Illidan Stormrage");

            long curWaitTime = 0;
            Object lock = new Object();

            while (!response.isDone() && curWaitTime < MAX_ASYNC_WAIT_TIME) {
                synchronized (lock) {
                    try {
                        lock.wait(50L);
                    } catch (InterruptedException e) {
                    }
                }
                curWaitTime += 50;
            }

            if (!response.isDone()) {
                writer.write("FAILED Response is not received after waiting " + MAX_ASYNC_WAIT_TIME);
                return;
            }

            User user;
            try {
                user = response.get().getReturn();
            } catch (InterruptedException e) {
                throw new ServletException(e);
            } catch (ExecutionException e) {
                throw new ServletException(e);
            }

            if (user == null) {
                writer.write("FAILED Expected user instance is not returned");
            } else if (!"Illidan Stormrage".equals(user.getName())) {
                writer.write("FAILED Expected user instance with name Illidan Stormrage is not returned");
            } else {
                writer.write("PASS");
            }
        } catch (UserNotFoundException_Exception e) {
            writer.write("FAILED Unexpected UserNotFoundException is thrown, " + e.getMessage());
        }
    }

    public void testQueryUserBasicAsyncHandler(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final PrintWriter writer = resp.getWriter();
        try {
            setEndpointAddress((BindingProvider) userQuery, req, "EJBWSBasic/UserQueryService");

            BindingProvider bp = (BindingProvider) userQuery;
            bp.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, "http://ejbbasic.jaxws.ws.ibm.com/UserQuery/getUser");
            Future<?> future = userQuery.getUserAsync("Illidan Stormrage", new AsyncHandler<GetUserResponse>() {
                @Override
                public void handleResponse(Response<GetUserResponse> response) {
                    try {
                        User user = response.get().getReturn();
                        if (user == null) {
                            writer.write("FAILED Expected user instance is not returned");
                        } else if (!"Illidan Stormrage".equals(user.getName())) {
                            writer.write("FAILED Expected user instance with name Illidan Stormrage is not returned");
                        } else {
                            writer.write("PASS");
                        }
                    } catch (Exception e) {
                        writer.write("FAILED " + e.getMessage());
                    }
                }
            });

            long curWaitTime = 0;
            Object lock = new Object();

            while (!future.isDone() && curWaitTime < MAX_ASYNC_WAIT_TIME) {
                synchronized (lock) {
                    try {
                        lock.wait(50L);
                    } catch (InterruptedException e) {
                    }
                }
                curWaitTime += 50;
            }

            if (!future.isDone()) {
                writer.write("FAILED the getUser is not returned after the timeout " + MAX_ASYNC_WAIT_TIME);
            }
        } catch (UserNotFoundException_Exception e) {
            writer.write("FAILED Unexpected UserNotFoundException is thrown, " + e.getMessage());
        }
    }

    /**
     * test the inconsistent targetNamespace between SEI and implementation bean
     * both the the SEI and implementation bean has not defined the targetNamespace, default package name is used.
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    public void testInConsistentNamespace(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        PrintWriter writer = resp.getWriter();

        String wsdlURL = "http://" + req.getServerName() + ":" + req.getServerPort() + "/" + "EJBWSBasic/SayHelloService?wsdl";

        URL wsdlLocation = new URL(wsdlURL);
        QName serviceName = new QName("http://ejbbasic.jaxws.ws.ibm.com/", "SayHelloService");
        SayHelloService service = new SayHelloService(wsdlLocation, serviceName);

        SayHelloInterface sayHello = service.getSayHelloPort();
        String response = sayHello.sayHello("simon");

        if (response.equals("hello, simon")) {
            writer.write("PASS");
        } else {
            writer.write("Failed");
        }

    }

    public void testListUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter writer = resp.getWriter();
        setEndpointAddress((BindingProvider) userQuery, req, "EJBWSBasic/UserQueryService");

        BindingProvider bp = (BindingProvider) userQuery;

        bp.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, "http://ejbbasic.jaxws.ws.ibm.com/UserQuery/listUser");
        List<User> users = userQuery.listUsers();
        if (users == null) {
            writer.write("FAILED Expected user instances are not returned");
        } else if (users.size() != 3) {
            writer.write("FAILED Expected three users should be returned");
        } else {
            writer.write("PASS");
        }
    }

    protected void setEndpointAddress(BindingProvider bindingProvider, HttpServletRequest request, String endpointPath) {
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                "http://" + request.getServerName() + ":" + request.getServerPort() + "/" + endpointPath);

        bindingProvider.getRequestContext().put("allowNonMatchingToDefaultSoapAction", true);
    }
}
