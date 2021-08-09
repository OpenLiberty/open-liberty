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
package com.ibm.ws.jaxws.ejbwscontext.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.ejbwscontext.EchoInfoI;
import com.ibm.ws.jaxws.ejbwscontext.EchoInfoService;

/**
 *
 */
@WebServlet("/EJBWSContextTestServlet")
public class EJBWSContextTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @WebServiceRef(value = EchoInfoService.class, wsdlLocation = "WEB-INF/wsdl/EJBWSContext.wsdl")
    EchoInfoI echoInfo = null;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String action = req.getParameter("action");
        if (action == null || action.isEmpty()) {
            throw new ServletException("Unable to detect the parameter \"action\" from the request");
        }

        String user = req.getParameter("user");
        String pwd = req.getParameter("pwd");

        if (user == null || pwd == null)
            throw new ServletException("Unable to detect the parameter \"user\" or \"pwd\" from the request");

        if ("PRIN".equals(action)) {
            testSecPrin(req, resp, user, pwd);

        } else if ("ROLE".equals(action)) {

            testSecRole(req, resp, user, pwd);
        } else if ("MCKEYSIZE".equals(action)) {

            testMsgContextKeySize(req, resp, user, pwd);
        } else if ("MCFIELDS".equals(action)) {
            testMsgContextFieldGetter(req, resp, user, pwd);
        }

    }

    /**
     * @param req
     * @param resp
     * @param user
     * @param pwd
     * @throws IOException
     */
    private void testMsgContextFieldGetter(HttpServletRequest req, HttpServletResponse resp, String user, String pwd) throws IOException {
        PrintWriter writer = resp.getWriter();
        setEndpointAddress((BindingProvider) echoInfo, req, "EJBWSContext/EchoInfoService", user, pwd);
        String result = echoInfo.getInfo("MCFIELDS");
        writer.write(result);
    }

    /**
     * testMsgContextKeySize
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    private void testMsgContextKeySize(HttpServletRequest req, HttpServletResponse resp, String user, String pwd) throws IOException {

        PrintWriter writer = resp.getWriter();
        setEndpointAddress((BindingProvider) echoInfo, req, "EJBWSContext/EchoInfoService", user, pwd);
        String result = echoInfo.getInfo("MCKEYSIZE");
        writer.write(result);
    }

    /**
     * testSecRole
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    private void testSecRole(HttpServletRequest req, HttpServletResponse resp, String user, String pwd) throws IOException {
        PrintWriter writer = resp.getWriter();
        setEndpointAddress((BindingProvider) echoInfo, req, "EJBWSContext/EchoInfoService", user, pwd);
        String result = echoInfo.getInfo("ROLE");
        writer.write(result);
    }

    /**
     * testSecPrin
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    private void testSecPrin(HttpServletRequest req, HttpServletResponse resp, String user, String pwd) throws IOException {

        PrintWriter writer = resp.getWriter();
        setEndpointAddress((BindingProvider) echoInfo, req, "EJBWSContext/EchoInfoService", user, pwd);
        String result = echoInfo.getInfo("PRIN");
        writer.write(result);
    }

    /**
     * setEndpointAddress
     *
     * @param bindingProvider
     * @param request
     * @param endpointPath
     */
    protected void setEndpointAddress(BindingProvider bindingProvider, HttpServletRequest request, String endpointPath, String user, String pwd) {
        Map<String, Object> reqCtx = bindingProvider.getRequestContext();
        reqCtx.put(BindingProvider.USERNAME_PROPERTY, user);
        reqCtx.put(BindingProvider.PASSWORD_PROPERTY, pwd);
        reqCtx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                   "http://" + request.getServerName() + ":" + request.getServerPort() + "/" + endpointPath);
    }

}
