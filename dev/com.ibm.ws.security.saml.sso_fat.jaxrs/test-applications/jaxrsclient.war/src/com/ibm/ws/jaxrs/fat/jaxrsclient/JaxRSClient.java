/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.jaxrsclient;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import com.ibm.websphere.security.saml2.PropagationHelper;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestTools;

/**
 * Servlet implementation class CxfSamlSvcClient
 */
public class JaxRSClient extends HttpServlet {

    /**
     * @see HttpServlet#HttpServlet()
     */
    public JaxRSClient() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doWorker(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("Got into the svc client");
        String appToCall = request.getParameter("targetApp");
        if (isUrlEncodedUrl(appToCall)) {
            System.out.println("Raw target app: " + appToCall);
            appToCall = URLDecoder.decode(appToCall, "UTF-8");
        }
        System.out.println("Target APP: " + appToCall);
        String headerFormat = request.getParameter("headerFormat");
        System.out.println("Header Format: " + headerFormat);
        String headerName = request.getParameter("header");
        System.out.println("Header Name: " + headerName);
        String formatTypeToSend = request.getParameter("formatType");
        System.out.println("Format Type: " + formatTypeToSend);
        Enumeration<String> v = request.getParameterNames();
        while (v.hasMoreElements()) {
            System.out.println("Parm: " + v.nextElement());
        }

        //        StringBuffer sb = new StringBuffer();
        //        sb.append(WSSubject.getCallerPrincipal());
        //        System.out.println(sb.toString()) ;

        String mySAML = null;
        if (formatTypeToSend == null) {
            System.out.println("Assertion format: not set - will test invoking all api's");
            testUnsetToken();
            return;
        }
        try {
            if (formatTypeToSend.equals("assertion_encoded")) {
                System.out.println("Assertion format: assertion_encoded");
                mySAML = PropagationHelper.getEncodedSaml20Token(false);
            } else {
                if (formatTypeToSend.equals("assertion_compressed_encoded")) {
                    System.out.println("Assertion format: assertion_compressed_encoded");
                    mySAML = PropagationHelper.getEncodedSaml20Token(true);
                } else {
                    if (formatTypeToSend.equals("assertion_text_only")) {
                        System.out.println("Assertion format: assertion_text_only");
                        Saml20Token myTmpSAML = PropagationHelper.getSaml20Token();
                        if (myTmpSAML != null) {
                            mySAML = SAMLCommonTestTools.trimXML(myTmpSAML.getSAMLAsString());
                        }
                    } else {
                        if (formatTypeToSend.equals("junk")) {
                            System.out.println("Assertion format: junk");
                            mySAML = "my dog has fleas";
                        } else {
                            if (formatTypeToSend.equals("empty")) {
                                System.out.println("Assertion format: junk");
                                mySAML = "";
                            } else {
                                System.out.println("App received an invalid Assertion format request - exiting");
                                return;
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Caught an exception trying to obtain the saml assertion" + e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
            return;
        }

        System.out.println("SAMLAssertion to send: " + mySAML);

        Client client = ClientBuilder.newClient();

        if (headerFormat.equals("propagate_token_string_true")) {
            System.out.println("Set the propagation handler string property - true");
            client.property("com.ibm.ws.jaxrs.client.saml.sendToken", "true");
        }
        if (headerFormat.equals("propagate_token_boolean_true")) {
            System.out.println("Set the propagation handler boolean property - true");
            client.property("com.ibm.ws.jaxrs.client.saml.sendToken", true);
        }
        if (headerFormat.equals("propagate_token_string_false")) {
            System.out.println("Set the propagation handler string property - false");
            client.property("com.ibm.ws.jaxrs.client.saml.sendToken", "false");
        }
        if (headerFormat.equals("propagate_token_boolean_false")) {
            System.out.println("Set the propagation handler boolean property - false");
            client.property("com.ibm.ws.jaxrs.client.saml.sendToken", false);
        }

        WebTarget myResource = client.target(appToCall).queryParam("saml_name", headerName);
        try {
            String Rresponse = null;
            if (!headerFormat.contains("propagate_token")) {
                System.out.println("Pass the header");
                Rresponse = myResource.request(MediaType.TEXT_PLAIN).header(headerName, mySAML).header("saml_name", headerName).get(String.class);
            } else {
                System.out.println("Use the propagation handler");
                Rresponse = myResource.request(MediaType.TEXT_PLAIN).header("saml_name", headerName).get(String.class);
            }

            System.out.println("Response: " + Rresponse);
            System.out.println("exiting the svc client");
            PrintWriter pw = response.getWriter();
            pw.println(Rresponse);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getClass().getName().equals("javax.ws.rs.NotAuthorizedException")) {
                System.out.println("Caught a 401 exception calling external App.");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            } else {
                System.out.println("Caught an exception calling external App.");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }

    }

    /**
     * Checks to see if the provided URL contains a URL-encoded {@code "://"} string. The URL-encoded string equivalent is
     * {@code "%3A%2F%2F"}.
     */
    boolean isUrlEncodedUrl(String url) {
        return (url != null && url.contains("%3A%2F%2F"));
    }

    private void testUnsetToken() {

        try {
            PropagationHelper.getEncodedSaml20Token(false);
        } catch (Exception e) {
            System.out.println("failed on False");
        }
        try {
            PropagationHelper.getEncodedSaml20Token(true);
        } catch (Exception e) {
            System.out.println("failed on true");
        }
        try {
            PropagationHelper.getSaml20Token();
        } catch (Exception e) {
            System.out.println("failed on text");
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doWorker(request, response);
        return;
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doWorker(request, response);
        return;
    }

}
