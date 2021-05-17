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
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.jwt.JwtHeaderInjecter;
import com.ibm.websphere.security.openidconnect.PropagationHelper;
import com.ibm.websphere.security.openidconnect.token.IdToken;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;

/**
 * Servlet implementation class CxfSamlSvcClient
 */
public class JaxRSClient extends HttpServlet {

    private static final int EXP_TIME_TOLERANCE_SEC = 10;

    String accessTokenFromSubject = null;
    //String jwtFromSubject = null;
    String issuedJwtFromSubject = null;
    String tokenTypeFromSubject = null;
    String scopesFromSubject = null;
    String accessTokenExpirationTimeFromSubject = null;
    String idTokenFromSubject = null;

    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public JaxRSClient() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @SuppressWarnings("rawtypes")
    protected void doWorker(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // get and log parms
        System.out.println("Got into the svc client");
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
        String headerName = request.getParameter("headerName");
        System.out.println("Header name: " + headerName);
        String jwtBuilder = request.getParameter("jwtBuilder");
        System.out.println("JWT Builder name: " + jwtBuilder);

        Enumeration<String> v = request.getParameterNames();
        while (v.hasMoreElements()) {
            System.out.println("Parm: " + v.nextElement().toString());
        }

        String access_token = null;
        String token_type = null;

        try {
            printSubjectParts("appStart-", response);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Caught an exception retrieving values from the Subject. " + e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
            return;
        }

        PrintWriter pw = response.getWriter();
        pw.print("\r\n");
        pw.print("*******************  Start of JaxRSClient output  ******************* \r\n");

        // Tests for api's - if context is not set, the api's should NOT throw an NPE, returning null is fine!
        try {
            if (contextSet.equals("false")) {
                // If we're in an unprotected app, none of the values should be set
                if (accessTokenFromSubject == null && tokenTypeFromSubject == null && scopesFromSubject == null
                        && accessTokenExpirationTimeFromSubject == null && idTokenFromSubject == null) {
                    System.out.println("All values in subject are null as they should be" + "\n");
                    // make sure api's all work with no context - they should return null (which we'll check
                    // in the junit test class.
                    String[] apiList = new String[] { "getAccessToken", "getAccessTokenType", "getAccessTokenExpirationTime", "getScopes", "getIdToken" };

                    for (String api : apiList) {
                        runApi(pw, response, api);
                    }
                    return;
                } else {
                    String printString = "one of: accessToken, tokenType, scopes, expiration time, or id_token, was NOT null in the subject and should have been";
                    System.out.println(printString + "\n");
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, printString);
                    return;
                }
            } else {
                // we should have a subject, so make sure that the api's return the values that we manually find in the subject
                if (!accessTokenFromSubject.equals(runApi(pw, response, "getAccessToken"))) {
                    String printString = "getAccessToken did NOT Match what test app found in the subject";
                    System.out.println(printString);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, printString);
                    return;
                }
                if (!tokenTypeFromSubject.equals(runApi(pw, response, "getAccessTokenType"))) {
                    String printString = "getAccessTokenType did NOT Match what test app found in the subject";
                    System.out.println(printString);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, printString);
                    return;
                }
                if (!isExpirationWithinLimit(accessTokenExpirationTimeFromSubject, runApi(pw, response, "getAccessTokenExpirationTime"), EXP_TIME_TOLERANCE_SEC)) {
                    String printString = "getAccessTokenExpirationTime did NOT Match what test app found in the subject";
                    System.out.println(printString);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, printString);
                    return;
                }
                if (!scopesFromSubject.equals(runApi(pw, response, "getScopes"))) {
                    String printString = "getScopes did NOT Match what test app found in the subject";
                    System.out.println(printString);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, printString);
                    return;
                }
                if (!idTokenFromSubject.equals(runApi(pw, response, "getIdToken"))) {
                    String printString = "getIdToken did NOT Match what test app found in the subject";
                    System.out.println(printString);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, printString);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Caught an exception calling a PropagationHelper API. " + e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
            return;
        }

        // decide which values we'll pass on the call to the protected app
        if (tokenContent.equals("currentValue")) {
            access_token = accessTokenFromSubject;
            token_type = tokenTypeFromSubject;
        } else {
            if (tokenContent.equals("apiValue")) {
                access_token = PropagationHelper.getAccessToken();
                token_type = PropagationHelper.getAccessTokenType();
            } else {
                token_type = "Bearer";
                if (tokenContent.equals("null")) {
                    access_token = null;
                } else {
                    if (tokenContent.equals("")) {
                        access_token = "My cat has fleas";
                    } else {
                        access_token = tokenContent;
                    }
                }
            }
        }
        System.out.println("access_token: " + access_token);
        System.out.println("token_type: " + token_type);

        // now, we're going to try to use the access token to get to our protected app on the rs server
        // pass the access token in the header or with parms based on caller's request
        try {
            String localResponse = null;
            Client client = ClientBuilder.newClient();
            switch (where) {
                case Constants.PROPAGATE_TOKEN_STRING_TRUE:
                    System.out.println("Set the propagation handler property (" + Constants.OAUTH_HANDLER + ") - string true");
                    client.property(Constants.OAUTH_HANDLER, "true");
                    break;
                case Constants.PROPAGATE_TOKEN_STRING_FALSE:
                    System.out.println("Set the propagation handler property (" + Constants.OAUTH_HANDLER + ") - string false");
                    client.property(Constants.OAUTH_HANDLER, "false");
                    break;
                case Constants.PROPAGATE_TOKEN_BOOLEAN_TRUE:
                    System.out.println("Set the propagation handler property (" + Constants.OAUTH_HANDLER + ") - boolean true");
                    client.property(Constants.OAUTH_HANDLER, true);
                    break;
                case Constants.PROPAGATE_TOKEN_BOOLEAN_FALSE:
                    System.out.println("Set the propagation handler property (" + Constants.OAUTH_HANDLER + ") - boolean false");
                    client.property(Constants.OAUTH_HANDLER, false);
                    break;
                case Constants.PROPAGATE_JWT_TOKEN_STRING_TRUE:
                    System.out.println("Set the propagation handler property (" + Constants.JWT_HANDLER + ") - string true");
                    client.property(Constants.JWT_HANDLER, "true");
                    break;
                case Constants.PROPAGATE_JWT_TOKEN_STRING_FALSE:
                    System.out.println("Set the propagation handler property (" + Constants.JWT_HANDLER + ") - string false");
                    client.property(Constants.JWT_HANDLER, "false");
                    break;
                case Constants.PROPAGATE_JWT_TOKEN_BOOLEAN_TRUE:
                    System.out.println("Set the propagation handler property (" + Constants.JWT_HANDLER + ") - boolean true");
                    client.property(Constants.JWT_HANDLER, true);
                    break;
                case Constants.PROPAGATE_JWT_TOKEN_BOOLEAN_FALSE:
                    System.out.println("Set the propagation handler property (" + Constants.JWT_HANDLER + ") - boolean false");
                    client.property(Constants.JWT_HANDLER, false);
                    break;
            }

            //String beforeJwtFromSubject = jwtFromSubject;
            String beforeIssuedJwtFromSubject = issuedJwtFromSubject;
            //pw.println("JWT from Subject: " + beforeJwtFromSubject);
            pw.println("Issued JWT from Subject: " + beforeIssuedJwtFromSubject);

            if (where.contains("Injection")) {
                System.out.println("Using injection");
                try {
                    // do we all then to pass the header name in the test code?  Need to sync with RS - I think there's a token name attr that we can set
                    // assign it here, or use the default of "Authorization"???
                    String headerToInject = "Authorization";
                    // TODO - may need to bypass assigning the "default" - in lueue of testing null
                    if (headerName != null) {
                        if (headerName.equals("null")) {
                            headerToInject = null;
                        } else {
                            if (headerName.equals("empty")) {
                                headerToInject = "";
                            } else {
                                headerToInject = headerName;
                            }
                        }
                    }

                    //String headerToInject = "custom";
                    if (where.contains("1")) {
                        System.out.println("JwtHeaderInjector 1");
                        client.register(new JwtHeaderInjecter());
                    }
                    if (where.contains("2")) {
                        System.out.println("JwtHeaderInjector 2");
                        client.register(new JwtHeaderInjecter(headerToInject));
                    }
                    if (where.contains("3")) {
                        // add parm to pass builder
                        System.out.println("JwtHeaderInjector 3");
                        String formattedBuilderName = jwtBuilder;
                        if (jwtBuilder != null) {
                            if (jwtBuilder.equals("null")) {
                                formattedBuilderName = null;
                            } else {
                                if (jwtBuilder.equals("empty")) {
                                    formattedBuilderName = "";
                                }
                            }
                        }
                        // on fast machines, we end up issuing the same token if:
                        //  the same builder is used, and
                        //  the commands run so fast that the iat and exp are the same
                        // so sleep to force a unique time
                        Thread.sleep(1000);
                        client.register(new JwtHeaderInjecter(headerToInject, formattedBuilderName));
                    }
                    printSubjectParts("updated-", response);
                    //String afterJwtFromSubject = jwtFromSubject;
                    String afterIssuedJwtFromSubject = issuedJwtFromSubject;
//                    if (beforeJwtFromSubject == null && afterJwtFromSubject == null) {
//                        pw.println("RP's JWT in subject is the same before and after the Injecter was invoked.");
//                        pw.println("RP's JWT in subject check: passed");
//                    } else {
//                        if (beforeJwtFromSubject == null || afterJwtFromSubject == null) {
//                            pw.println("RP's JWT in subject is NOT the same before and after the Injecter was invoked.");
//                            pw.println("RP's JWT in subject check: failed");
//                        } else {
//                            if (beforeJwtFromSubject.equals(afterJwtFromSubject)) {
//                                pw.println("RP's JWT in subject is the same before and after the Injecter was invoked.");
//                                pw.println("RP's JWT in subject check: passed");
//                            } else {
//                                pw.println("RP's JWT in subject is NOT the same before and after the Injecter was invoked.");
//                                pw.println("RP's JWT in subject check: failed");
//                            }
//                        }
//                    }

                    if (beforeIssuedJwtFromSubject == null && afterIssuedJwtFromSubject == null) {
                        pw.println("RP's Issued JWT in subject is the same before and after the Injecter was invoked.");
                        pw.println("RP's Issued JWT in subject check: passed");
                    } else {
                        if (beforeIssuedJwtFromSubject == null || afterIssuedJwtFromSubject == null) {
                            pw.println("RP's Issued JWT in subject is NOT the same before and after the Injecter was invoked.");
                            pw.println("RP's Issued JWT in subject check: failed");
                        } else {
                            if (beforeIssuedJwtFromSubject.equals(afterIssuedJwtFromSubject)) {
                                pw.println("RP's Issued JWT in subject is the same before and after the Injecter was invoked.");
                                pw.println("RP's Issued JWT in subject check: passed");
                            } else {
                                pw.println("RP's Issued JWT in subject is NOT the same before and after the Injecter was invoked.");
                                pw.println("RP's Issued JWT in subject check: failed");
                            }
                        }
                    }

                    System.out.println("JwtHeaderInjector registered successfully");

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Caught an exception while registering the JwtHeaderInjecter : " + e.getLocalizedMessage());
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    return;
                }

                WebTarget myResource = client.target(appToCall).queryParam("targetApp", appToCall).queryParam("where", where).queryParam("tokenContent", tokenContent).queryParam("contextSet", contextSet).queryParam("headerName", headerName).queryParam("jwtBuilder", jwtBuilder);
                //                WebTarget myResource = client.target(appToCall).queryParam("targetApp", appToCall);
                Builder builder = myResource.request(MediaType.TEXT_PLAIN);
                localResponse = builder.get(String.class);
                //                if (localResponse != null) {
                //                    if (beforeJwtFromSubject != null) {
                //                        if (localResponse.contains(beforeJwtFromSubject)) {
                //                            pw.println("RP and RS JWT's match");
                //                        } else {
                //                            pw.println("RP and RS JWT's DO NOT match");
                //                        }
                //                    } else {
                //                        pw.println("RP and RS JWT's DO NOT match");
                //                    }
                //                }
            } else {
                System.out.println("Not using injection");
                if (where.contains("propagate_token") || where.contains("propagate_jwt_token")) {
                    WebTarget myResource = client.target(appToCall).queryParam("targetApp", appToCall).queryParam("where", where).queryParam("tokenContent", tokenContent).queryParam("contextSet", contextSet);
                    localResponse = myResource.request(MediaType.TEXT_PLAIN).get(String.class);
                } else {
                    if (where.equals("header")) {
                        String headerValue = token_type + " " + access_token;
                        WebTarget myResource = client.target(appToCall);
                        System.out.println("Adding: Authorization=" + headerValue + "   to the HEADER");
                        localResponse = myResource.request(MediaType.TEXT_PLAIN).header("Authorization", headerValue).get(String.class);
                    } else {
                        WebTarget myResource = client.target(appToCall);
                        //.queryParam("access_token", access_token).queryParam("targetApp", appToCall).queryParam("where", where).queryParam("tokenContent", tokenContent).queryParam("contextSet", contextSet);
                        Form form = new Form();
                        System.out.println("Adding: access_token=" + access_token + "   as a parm");
                        System.out.println("Adding: targetApp=" + appToCall + "   as a parm - for parm testing of servlet output");
                        form.param("access_token", access_token);
                        form.param("targetApp", appToCall);
                        form.param("where", where);
                        form.param("tokenContent", tokenContent);
                        form.param("contextSet", contextSet);
                        localResponse = myResource.request(MediaType.TEXT_PLAIN).post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);

                    }
                }
            }

            if (localResponse != null) {
                if (beforeIssuedJwtFromSubject != null) {
                    if (localResponse.contains(beforeIssuedJwtFromSubject)) {
                        pw.println("RP and RS Issued JWT's match");
                    } else {
                        pw.println("RP and RS Issued JWT's DO NOT match");
                    }
                } else {
                    pw.println("RP and RS Issued JWT's DO NOT match");
                }
//                } else if(beforeJwtFromSubject != null) {
//                	if (localResponse.contains(beforeJwtFromSubject)) {
//                        pw.println("RP and RS JWT's match");
//                    } else {
//                        pw.println("RP and RS JWT's DO NOT match");
//                    }
//                }
//                else {
//                	pw.println("RP and RS Issued JWT's DO NOT match");
//                    pw.println("RP and RS JWT's DO NOT match");
//                }
            }

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
     * Attemps to parse each expiration time value as a long and returns whether the values differ by more than the
     * specified secTolerance value.
     *
     * @param accessTokenExpirationTimeFromSubject
     * @param apiExpirationTime
     * @param secTolerance
     * @return
     * @throws NumberFormatException
     */
    private boolean isExpirationWithinLimit(String accessTokenExpirationTimeFromSubject, String apiExpirationTime, int secTolerance) throws NumberFormatException {
        if (accessTokenExpirationTimeFromSubject == null || apiExpirationTime == null) {
            System.out.println((apiExpirationTime == null ? "Expiration time from API" : "Expiration time from access token") + " was null.");
            return false;
        }
        long expSubject, expApi = 0L;
        try {
            expSubject = Long.parseLong(accessTokenExpirationTimeFromSubject);
            expApi = Long.parseLong(apiExpirationTime);
        } catch (NumberFormatException nfe) {
            System.out.println("Either the expiration time from the access token or the time obtained from the API were not valid numbers.");
            throw nfe;
        }
        // Difference between the two values must be within the specified tolerance (in seconds)
        long diff = Math.abs(expApi - expSubject);
        if (diff <= secTolerance) {
            return true;
        }
        System.out.println("Difference in expiration time (" + diff + " seconds) was outside allowed tolerance of " + secTolerance + " seconds");
        return false;
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

    protected void printSubjectParts(String msgPrefix, HttpServletResponse response) throws Exception {

        // get the actual values out of the subject
        accessTokenFromSubject = null;
        //jwtFromSubject = null;
        issuedJwtFromSubject = null;
        tokenTypeFromSubject = null;
        scopesFromSubject = null;
        accessTokenExpirationTimeFromSubject = null;
        idTokenFromSubject = null;

        Subject runAsSubject = WSSubject.getRunAsSubject();
        if (runAsSubject != null) {
            Set<Hashtable> privateHashtableCreds = runAsSubject.getPrivateCredentials(Hashtable.class);

            if (privateHashtableCreds != null) {
                //there could be many.. we'll just take the one with access_token.
                Hashtable theChosenOne = null;
                for (Hashtable test : privateHashtableCreds) {
                    if (test.containsKey("access_token")) { // use "jwt" for jwt token
                        theChosenOne = test;
                    }
                }

                if (theChosenOne != null) {
                    //now we have found the credentials holding the current access_token
                    //we will cache it locally so we can invoke the RS with it.
                    System.out.println("theChosenOne: " + theChosenOne.toString());
                    accessTokenFromSubject = getAValue(theChosenOne, "access_token");
                    //jwtFromSubject = getAValue(theChosenOne, "jwt");
                    issuedJwtFromSubject = getAValue(theChosenOne, "issuedJwt");

                    tokenTypeFromSubject = getAValue(theChosenOne, "token_type");
                    scopesFromSubject = getAValue(theChosenOne, "scope");
                    IdToken tmpIdToken = null;

                    Set credset = runAsSubject.getPrivateCredentials();
                    Iterator it = credset.iterator();
                    while (it.hasNext()) {
                        Object o = it.next();
                        System.out.println("*** private credential object: " + o.getClass().getName() + " " + o);
                    }

                    Set<IdToken> privateIdTokens = runAsSubject.getPrivateCredentials(IdToken.class);
                    System.out.println("*** size of id token set is " + privateIdTokens.size());
                    for (IdToken idTokenTmp : privateIdTokens) {
                        tmpIdToken = idTokenTmp;
                        break;
                    }
                    if (tmpIdToken != null) {
                        idTokenFromSubject = tmpIdToken.toString();
                        accessTokenExpirationTimeFromSubject = String.valueOf(tmpIdToken.getExpirationTimeSeconds());
                    }
                }
            }
        }
        System.out.println(msgPrefix + "accessTokenFromSubject: " + accessTokenFromSubject);
        //System.out.println(msgPrefix + "jwtFromSubject: " + jwtFromSubject);
        System.out.println(msgPrefix + "issuedJwtFromSubject: " + issuedJwtFromSubject);
        System.out.println(msgPrefix + "tokenTypeFromSubject: " + tokenTypeFromSubject);
        System.out.println(msgPrefix + "scopesFromSubject: " + scopesFromSubject);
        System.out.println(msgPrefix + "accessTokenExpirationTimeFromSubject: " + accessTokenExpirationTimeFromSubject);
        System.out.println(msgPrefix + "idTokenFromSubject: " + idTokenFromSubject);

    }

}
