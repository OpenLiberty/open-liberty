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
 * Servlet implementation class CxfSamlSvcClient
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
    @SuppressWarnings("rawtypes")
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

        String access_token = null;
        String token_type = null;

        //		// get the actual values out of the subject
        //		String accessTokenFromSubject = null ;
        //		String tokenTypeFromSubject = null ;
        //		String scopesFromSubject = null ;
        //		String accessTokenExpirationTimeFromSubject = null ;
        //		String idTokenFromSubject = null  ;
        //		try {
        //
        //			Subject runAsSubject = WSSubject.getRunAsSubject();
        //			if (runAsSubject != null) {
        //				Set<Hashtable> privateHashtableCreds = runAsSubject.getPrivateCredentials(Hashtable.class);
        //
        //				if (privateHashtableCreds != null ) {
        //					//there could be many.. we'll just take the one with access_token.
        //					Hashtable theChosenOne = null;
        //					for(Hashtable test : privateHashtableCreds){
        //						if(test.containsKey("access_token")){
        //							theChosenOne = test;
        //						}
        //					}
        //
        //					if ( theChosenOne != null ) {
        //						//now we have found the credentials holding the current access_token
        //						//we will cache it locally so we can invoke the RS with it.
        //						System.out.println("theChosenOne: " + theChosenOne.toString());
        //						accessTokenFromSubject = getAValue(theChosenOne, "access_token");
        //						tokenTypeFromSubject = getAValue(theChosenOne, "token_type");
        //						scopesFromSubject = getAValue(theChosenOne, "scope");
        //						IdToken tmpIdToken = null;
        //						Set<IdToken> privateIdTokens = runAsSubject.getPrivateCredentials(IdToken.class);
        //						for(IdToken idTokenTmp : privateIdTokens){
        //							tmpIdToken = idTokenTmp;
        //							break;
        //						}
        //						if (tmpIdToken != null) {
        //							idTokenFromSubject = tmpIdToken.toString();
        //							accessTokenExpirationTimeFromSubject = String.valueOf(tmpIdToken.getExpirationTimeSeconds());
        //						}
        //					}
        //				}
        //			}
        //			System.out.println("accessTokenFromSubject: " + accessTokenFromSubject);
        //			System.out.println("tokenTypeFromSubject: " + tokenTypeFromSubject);
        //			System.out.println("scopesFromSubject: " + scopesFromSubject);
        //			System.out.println("accessTokenExpirationTimeFromSubject: " + accessTokenExpirationTimeFromSubject);
        //			System.out.println("idTokenFromSubject: " + idTokenFromSubject);
        //
        //		} catch (Exception e) {
        //			e.printStackTrace();
        //			System.out.println("Caught an exception retrieving values from the Subject. " + e) ;
        //			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
        //			return ;
        //		}

        PrintWriter pw = response.getWriter();
        pw.print("\r\n");
        pw.print("*******************  Start of SimpleJaxRSClient output  ******************* \r\n");

        // Tests for api's - if context is not set, the api's should NOT throw an NPE, returning null is fine!
        //		try {
        //			if (contextSet.equals("false")) {
        //				// If we're in an unprotected app, none of the values should be set
        //				if (accessTokenFromSubject==null && tokenTypeFromSubject==null && scopesFromSubject==null
        //				        && accessTokenExpirationTimeFromSubject==null && idTokenFromSubject==null) {
        //					System.out.println("All values in subject are null as they should be"+ "\n") ;
        //					// make sure api's all work with no context - they should return null (which we'll check 
        //					// in the junit test class.
        //					String[] apiList = new String[]{"getAccessToken", "getAccessTokenType", "getAccessTokenExpirationTime", "getScopes", "getIdToken"};
        //
        //					for (String api: apiList) {
        //						runApi(pw, response, api) ;
        //					}
        //					return ;
        //				} else {
        //					String printString = "one of: accessToken, tokenType, scopes, expiration time, or id_token, was NOT null in the subject and should have been" ; 
        //					System.out.println(printString+ "\n");
        //					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, printString);					
        //				}
        //			} else {
        //				// we should have a subject, so make sure that the api's return the values that we manually find in the subject
        //				if (!accessTokenFromSubject.equals(runApi(pw, response, "getAccessToken"))) {
        //					String printString = "getAccessToken did NOT Match what test app found in the subject" ;
        //					System.out.println(printString) ;
        //					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, printString);
        //				}			
        //				if (!tokenTypeFromSubject.equals(runApi(pw, response, "getAccessTokenType"))) {
        //					String printString = "getAccessTokenType did NOT Match what test app found in the subject" ;
        //					System.out.println(printString) ;
        //					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, printString);
        //				}
        //				if (!isExpirationWithinLimit(accessTokenExpirationTimeFromSubject, runApi(pw, response, "getAccessTokenExpirationTime"), EXP_TIME_TOLERANCE_SEC)) {
        //					String printString = "getAccessTokenExpirationTime did NOT Match what test app found in the subject" ;
        //					System.out.println(printString) ;
        //					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, printString);
        //				}
        //				if (!scopesFromSubject.equals(runApi(pw, response, "getScopes"))) {
        //					String printString = "getScopes did NOT Match what test app found in the subject" ;
        //					System.out.println(printString) ;
        //					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, printString);
        //				}
        //				if (!idTokenFromSubject.equals(runApi(pw, response, "getIdToken"))) {
        //					String printString = "getIdToken did NOT Match what test app found in the subject" ;
        //					System.out.println(printString) ;
        //					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, printString);
        //				}
        //			}
        //		} catch (Exception e) {
        //			e.printStackTrace();
        //			System.out.println("Caught an exception calling a PropagationHelper API. " + e) ;
        //			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
        //		}

        //		// decide which values we'll pass on the call to the protected app
        //		if (tokenContent.equals("currentValue")) {
        //			access_token = accessTokenFromSubject ;
        //			token_type = tokenTypeFromSubject ;
        //		} else {
        //			if (tokenContent.equals("apiValue")) {
        //				access_token = PropagationHelper.getAccessToken();
        //				token_type = PropagationHelper.getAccessTokenType() ;
        //			}else {
        //				token_type = "Bearer" ;
        //				if (tokenContent.equals("null")) {
        //					access_token = null ;
        //				} else {
        //					if (tokenContent.equals("")) {
        //						access_token="My cat has fleas" ;
        //					} else {
        //						access_token = tokenContent ;
        //					}
        //				}
        //			}
        //		}
        //		System.out.println("access_token: " + access_token);
        //		System.out.println("token_type: " + token_type);

        // now, we're going to try to use the access token to get to our protected app on the rs server
        // pass the access token in the header or with parms based on caller's request
        //		try {
        String localResponse = null;
        Client client = ClientBuilder.newClient();
        //			if (where.equals("propagate_token_string_true")) {
        //				System.out.println("Set the propagation handler property - string true") ;
        //				client.property("com.ibm.ws.jaxrs.client.oauth.sendToken", "true");
        //			}
        //			if (where.equals("propagate_token_string_false")) {
        //				System.out.println("Set the propagation handler property - string false") ;
        //				client.property("com.ibm.ws.jaxrs.client.oauth.sendToken", "false");					
        //			} 
        //			if (where.equals("propagate_token_boolean_true")) {
        //				System.out.println("Set the propagation handler property - boolean true") ;
        //				client.property("com.ibm.ws.jaxrs.client.oauth.sendToken", true);
        //			}
        //			if (where.equals("propagate_token_boolean_false")) {
        //				System.out.println("Set the propagation handler property - boolean false") ;
        //				client.property("com.ibm.ws.jaxrs.client.oauth.sendToken", false);					
        //			}
        //
        //			if (where.contains("propagate_token")) {
        //				WebTarget myResource = client.target(appToCall).queryParam("targetApp", appToCall).queryParam("where", where).queryParam("tokenContent", tokenContent).queryParam("contextSet", contextSet);
        //				localResponse = myResource.request(MediaType.TEXT_PLAIN).get(String.class);				
        //			} else {
        //
        //				
        //				if (where.equals("header")) {

        try {

            client.property("com.ibm.ws.jaxrs.client.oauth.sendToken", "true");
            WebTarget myResource = client.target(appToCall).queryParam("targetApp", appToCall).queryParam("where", where).queryParam("tokenContent", tokenContent).queryParam("contextSet", contextSet);
            localResponse = myResource.request(MediaType.TEXT_PLAIN).get(String.class);
            //
            //					String headerValue = token_type + " " + tokenContent  ;
            //					WebTarget myResource = client.target(appToCall);
            //					System.out.println("Adding: Authorization=" + headerValue + "   to the HEADER") ;
            //					localResponse = myResource.request(MediaType.TEXT_PLAIN).header("Authorization", headerValue ).get(String.class);
            //				} else {
            //					WebTarget myResource = client.target(appToCall) ;
            //					//.queryParam("access_token", access_token).queryParam("targetApp", appToCall).queryParam("where", where).queryParam("tokenContent", tokenContent).queryParam("contextSet", contextSet);
            //					Form form = new Form() ;
            //					System.out.println("Adding: access_token=" + access_token + "   as a parm") ;
            //					System.out.println("Adding: targetApp=" + appToCall + "   as a parm - for parm testing of servlet output") ;
            //					form.param("access_token", access_token) ;
            //					form.param("targetApp", appToCall) ;
            //					form.param("where", where);
            //					form.param("tokenContent", tokenContent);
            //					form.param("contextSet", contextSet);
            //					localResponse = (String) myResource.request(MediaType.TEXT_PLAIN).post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);				
            //
            //				}
            //			}

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
