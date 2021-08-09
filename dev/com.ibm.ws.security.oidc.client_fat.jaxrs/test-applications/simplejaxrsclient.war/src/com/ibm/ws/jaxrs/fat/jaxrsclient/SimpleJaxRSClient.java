/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.jaxrsclient;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import com.ibm.websphere.security.openidconnect.PropagationHelper;
import com.ibm.websphere.security.openidconnect.token.IdToken;

/**
 * Servlet implementation class simpleJaxrsClient
 */
public class SimpleJaxRSClient extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public SimpleJaxRSClient() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doWorker(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // get and log parms
        System.out.println("Got into the simple svc client");
        String appToCall = request.getParameter("targetApp");
        System.out.println("Target APP: " + appToCall);
        String where = request.getParameter("where");
        System.out.println("Where: " + where);
        String tokenContent = request.getParameter("tokenContent");
        tokenContent = (tokenContent == null) ? "currentValue" : tokenContent;
        System.out.println("Token Content: " + tokenContent);
        String contextSet = request.getParameter("contextSet");
        contextSet = (contextSet == null) ? "true" : contextSet;
        System.out.println("Context Set: " + contextSet);

        Enumeration<String> v = request.getParameterNames();
        while (v.hasMoreElements()) {
            System.out.println("Parm: " + v.nextElement().toString());
        }

        PrintWriter pw = response.getWriter();
        pw.print("\r\n");
        pw.print("*******************  Start of SimpleJaxRSClient output  ******************* \r\n");

        // now, we're going to try to use the access token to get to our protected app on the rs server
        // pass the access token in the header or with parms based on caller's request
        //		try {
        String localResponse = null;
        Client client = ClientBuilder.newClient();

        try {

            client.property("com.ibm.ws.jaxrs.client.oauth.sendToken", "true");
            WebTarget myResource = client.target(appToCall).queryParam("targetApp", appToCall).queryParam("where", where).queryParam("tokenContent", tokenContent).queryParam("contextSet", contextSet);
            localResponse = myResource.request(MediaType.TEXT_PLAIN).get(String.class);

            // just return the output from the called app - junit client will vaidate
            System.out.println("Response: " + localResponse);
            System.out.println("exiting the svc client");
            pw.print("*******************  End of JaxRSClient output  ******************* \r\n");
            pw.print(localResponse + "\r\n");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Caught an exception calling external App." + e.toString());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doWorker(request, response);
        return;
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doWorker(request, response);
        return;
    }

    protected String runApi(PrintWriter pw, HttpServletResponse response, String api) {// throws Exception{
        String valueString;
        if (api.equals("getAccessTokenType")) {
            valueString = PropagationHelper.getAccessTokenType();
        } else if (api.equals("getAccessTokenExpirationTime")) {
            valueString = Long.toString(PropagationHelper.getAccessTokenExpirationTime());
        } else if (api.equals("getAccessToken")) {
            valueString = PropagationHelper.getAccessToken();
        } else if (api.equals("getScopes")) {
            valueString = PropagationHelper.getScopes();
        } else if (api.equals("getIdToken")) {
            IdToken x = PropagationHelper.getIdToken();
            if (x == null) {
                valueString = "null";
            } else {
                valueString = x.toString();
            }
        } else {
            valueString = "something not recognized";
        }
        String printString = "JaxRSClient-" + api + ": " + valueString;
        pw.print(printString + "\r\n");
        System.out.println(printString);
        return valueString;
    }

    @SuppressWarnings("rawtypes")
    protected String getAValue(Hashtable theChosenOne, String token) throws Exception {
        Object theValue = theChosenOne.get(token);
        if (theValue != null) {
            return theValue.toString();
        } else {
            return null;
        }

    }

}
