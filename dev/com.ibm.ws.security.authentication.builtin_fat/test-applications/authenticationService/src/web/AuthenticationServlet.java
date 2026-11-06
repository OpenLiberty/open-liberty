/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.wsspi.security.token.AttributeNameConstants;

@SuppressWarnings("serial")
public class AuthenticationServlet extends HttpServlet {

    private static final String ACTION_AUTHENTICATE = "authenticate";
    private static final String ACTION_JAAS_LOGIN = "jaasLogin";
    private static final String ACTION_VALIDATECACHE = "validateCache";

    /**
     * {@inheritDoc} Given the expected input parameters, perform an authentication attempt.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        AuthenticationService authenticationService = getAuthenticationService(writer);

        String action = request.getParameter("action");
        String username = request.getParameter("user");
        String password = request.getParameter("password");
        String jaasEntryName = request.getParameter("jaasEntryName");
        if (action == null || username == null || jaasEntryName == null) {
            writer.println("Usage: url?action=ACTION&user=NAME&password=PASSWD&jaasEntryName=JAASENTRYNAME)");
        } else {
            if (authenticationService == null) {
                writer.println("service is null");
            } else if (ACTION_AUTHENTICATE.equals(action)) {
                authenticate(authenticationService, username, password, jaasEntryName, writer);
            } else if (ACTION_VALIDATECACHE.equals(action)) {
                validateCache(authenticationService, username, password, jaasEntryName, writer);
            } else if (ACTION_JAAS_LOGIN.equals(action)) {
                validateCacheJAASLogin(username, password, writer);
            }
        }
        response.setStatus(200);

        writer.println("authenticationService = " + authenticationService);
        writer.flush();
        writer.close();
    }

    private void authenticate(AuthenticationService authenticationService, String username, String password, String jaasEntryName, PrintWriter writer) {
        try {
            Subject authenticatedSubject = getAuthenticatedSubject(authenticationService, username, password, jaasEntryName);
            writer.println("true");
            writer.println(authenticatedSubject);
        } catch (AuthenticationException e) {
            writer.println("false");
            e.printStackTrace(writer);
        }
    }

    private void validateCache(AuthenticationService authenticationService, String username, String password, String jaasEntryName, PrintWriter writer) {
        try {
            boolean areSubjectsTheSame = false;
            Subject firstAuthenticatedSubject = getAuthenticatedSubject(authenticationService, username, password, jaasEntryName);
            Subject secondAuthenticatedSubject = getAuthenticatedSubject(authenticationService, username, password, jaasEntryName);
            areSubjectsTheSame = (firstAuthenticatedSubject.equals(secondAuthenticatedSubject));

            writer.println(areSubjectsTheSame);
            writer.println("firstAuthenticatedSubject = " + firstAuthenticatedSubject);
            writer.println("secondAuthenticatedSubject = " + secondAuthenticatedSubject);
        } catch (AuthenticationException e) {
            writer.println("false");
            e.printStackTrace(writer);
        }
    }

    private void validateCacheJAASLogin(String username,
                                        String password,
                                        PrintWriter writer) {
        try {
            CallbackHandler wscbh = new WSCallbackHandlerImpl(username, password);
            LoginContext ctx = new LoginContext(JaasLoginConfigConstants.APPLICATION_WSLOGIN, wscbh);
            ctx.login();
            Subject firstAuthenticatedSubject = ctx.getSubject();
            System.out.println("after 1st login");
            wscbh = new WSCallbackHandlerImpl(username, password);
            ctx = new LoginContext(JaasLoginConfigConstants.APPLICATION_WSLOGIN, wscbh);
            ctx.login();
            Subject secondAuthenticatedSubject = ctx.getSubject();
            System.out.println("after 2nd login");
            boolean areSubjectsTheSame = (firstAuthenticatedSubject.equals(secondAuthenticatedSubject));
            writer.println(areSubjectsTheSame);
            writer.println("firstAuthenticatedSubject = " + firstAuthenticatedSubject);
            writer.println("secondAuthenticatedSubject = " + secondAuthenticatedSubject);
        } catch (Exception e) {
            writer.println("false");
            writer.println("Unexpected exception in validateCacheJAASLogin, " + e.getMessage());
            e.printStackTrace(writer);
        }
    }

    private Subject getAuthenticatedSubject(AuthenticationService authenticationService, String username, String password, String jaasEntryName) throws AuthenticationException {
        Subject authenticatedSubject = null;
        if (password != null) {
            AuthenticationData authenticationData = createAuthenticationData(username, password);
            authenticatedSubject = authenticationService.authenticate(jaasEntryName, authenticationData, null);
        } else {
            Subject partialSubject = createPartialSubject(username);
            authenticatedSubject = authenticationService.authenticate(jaasEntryName, partialSubject);
        }
        return authenticatedSubject;
    }

    private Subject createPartialSubject(String username) {
        Subject partialSubject = new Subject();
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, username);
        hashtable.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        partialSubject.getPublicCredentials().add(hashtable);
        return partialSubject;
    }

    /**
     * Determines the AuthenticationService, will return null if this determination
     * can not be made.
     * 
     * @param writer
     * @return null if this determination can not be made.
     */
    private AuthenticationService getAuthenticationService(PrintWriter writer) {
        BundleContext bundleContext = getBundleContext(writer);
        if (bundleContext == null) {
            writer.println("Unable to determine bundle context");
            return null;
        }

        ServiceReference<AuthenticationService> authenticationServiceReference = bundleContext.getServiceReference(AuthenticationService.class);
        if (authenticationServiceReference == null) {
            writer.println("Unable to locate ServiceReference<AuthenticationService>");
            return null;
        }

        AuthenticationService authenticationService = bundleContext.getService(authenticationServiceReference);
        if (authenticationService == null) {
            writer.println("Unable to locate AuthenticationService");
            return null;
        }
        return authenticationService;
    }

    /**
     * Determines the BundleContext, will return null if this determination
     * can not be made.
     * 
     * @param writer
     * @return null if this determination can not be made.
     */
    private BundleContext getBundleContext(PrintWriter writer) {
        Bundle bundle = FrameworkUtil.getBundle(Servlet.class);
        if (bundle == null) {
            writer.println("Unable to determine bundle");
            return null;
        }
        return bundle.getBundleContext();
    }

    /**
     * Creates an AuthenticationData object.
     * 
     * @param username
     * @param password
     * @return An AuthenticationData
     */
    private AuthenticationData createAuthenticationData(String username, String password) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, username);
        authenticationData.set(AuthenticationData.PASSWORD, password.toCharArray());
        return authenticationData;
    }
}
