/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.common;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.wsspi.security.auth.callback.WSCallbackHandlerFactory;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class JAASServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private boolean isFactoryTest = false;

    public JAASServlet() {
        super("JAASServlet");
    }

    @Override
    protected void performTask(HttpServletRequest req,
                               HttpServletResponse resp, StringBuffer sb) throws ServletException, IOException {
        // Values before any method call
        writeLine(sb, "Start initial values");
        printProgrammaticApiValues(req, sb);
        writeLine(sb, "End initial values");

        // Get parameters from URL link
        String loginUser = req.getParameter("user");
        String loginPassword = req.getParameter("password");
        String testMethod = req.getParameter("testMethod");
        String realm = req.getParameter("realm");
        if (loginUser == null || loginPassword == null || testMethod == null) {
            writeLine(sb, "Usage: ?testMethod=<method>&user=<user>&password=<password>&realm=<realm>");
        }
        writeLine(sb, "Passed in from URL: testMethod:[" + testMethod
                      + "] user:[" + loginUser + "] password:["
                      + loginPassword + "] realm:[" + realm + "]");

        if (testMethod != null) {
            String[] method = testMethod.split(",");
            for (int i = 0; i < method.length; i++) {
                writeLine(sb, "STARTTEST" + (i + 1));
                writeLine(sb, "method: " + method[i]);
                try {
                    callMethod(req, resp, sb, method[i], realm);
                } catch (ServletException e) {
                    writeLine(sb, "ServletException: " + e.getMessage());
                }
                writeLine(sb, "ENDTEST" + (i + 1));
            }
        }

    }

    private void callMethod(HttpServletRequest req, HttpServletResponse resp,
                            StringBuffer sb, String testMethod, String realm) throws ServletException, IOException {

        String user = req.getParameter("user");
        String password = req.getParameter("password");
        String assertId = req.getParameter("assertId");

        if (testMethod.contains(JaasLoginConfigConstants.APPLICATION_WSLOGIN) || testMethod.contains(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)
            || testMethod.contains("CustomIdentityAssertion"))
            isFactoryTest = false;
        else if (testMethod.contains("callback"))
            isFactoryTest = true;
        invokeJAASLoginMethod(req, resp, user, password, assertId, testMethod, realm, sb);

    }

    // for method = WSLogin or system.WEB_INBOUND.  Login using id/pw.
    // This method uses WSCallbackHandlerImpl() to initiate a CallbackHandler
    private void invokeJAASLoginMethod(HttpServletRequest req,
                                       HttpServletResponse resp, String user, String passwd, String assertId, String inputMethod, String realm,
                                       StringBuffer sb) throws ServletException, IOException {
        CallbackHandler wscbh = null;
        String loginMethod = null;
        Subject subject = null;

        if (isFactoryTest) { // will use WSCallbackHandlerFactoryImpl

            // use WSLogin as the method to log in for testing callbacks
            loginMethod = JaasLoginConfigConstants.APPLICATION_WSLOGIN;

            // decide on how to call getCallbackHandler
            WSCallbackHandlerFactory wsCallbackHandlerFactory = WSCallbackHandlerFactory.getInstance();

            if (inputMethod.contains("idpw")) {
                wscbh = wsCallbackHandlerFactory.getCallbackHandler(user, passwd);
            } else if (inputMethod.contains("realm")) {
                String realmName = realm;
                wscbh = wsCallbackHandlerFactory.getCallbackHandler(user, realmName, passwd);
            }
        } else { // will use WSCallbackHandlerImpl

            if (inputMethod.contains(JaasLoginConfigConstants.APPLICATION_WSLOGIN)) {

                //use WSLogin as the method to log in
                loginMethod = JaasLoginConfigConstants.APPLICATION_WSLOGIN;

                // decide on how to instantiate WSCallbackHandlerImpl
                if (inputMethod.equals(JaasLoginConfigConstants.APPLICATION_WSLOGIN)) {
                    wscbh = new WSCallbackHandlerImpl(user, passwd);

                } else if (inputMethod.contains("realm")) {
                    String realmName = realm;
                    wscbh = new WSCallbackHandlerImpl(user, realmName, passwd);

                }
            } else if (inputMethod.contains("CustomIdentityAssertion")) {
                loginMethod = "CustomIdentityAssertion";
                final Subject finalSubject = new Subject();
                final Map<String, Object> hashtable = new Hashtable<String, Object>();
                hashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, assertId);
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        @Override
                        public Object run() throws Exception {
                            finalSubject.getPublicCredentials().add(hashtable);
                            return null;
                        }
                    });
                } catch (PrivilegedActionException e) {
                    throw new ServletException(e.getMessage());
                }
                subject = finalSubject;
                // Set the callback handler
                wscbh = new WSCallbackHandlerImpl(user, passwd);

            } else {
                // use WEB_INBOUND as the method to log in
                loginMethod = JaasLoginConfigConstants.SYSTEM_WEB_INBOUND;
                wscbh = new WSCallbackHandlerImpl(user, passwd);
            }

        }

        // now we try to log in
        try {
            LoginContext ctx;
            if (subject != null) {
                ctx = new LoginContext(loginMethod, subject, wscbh);
            } else {
                ctx = new LoginContext(loginMethod, wscbh);
            }

            doLogin(loginMethod, ctx, wscbh, sb);

            // clean up (log out) for next test
            doLogout(req, resp, loginMethod, ctx, wscbh, sb);

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e.getMessage());
        }

    }

    private void doLogin(String method, LoginContext ctx, CallbackHandler wscbh, StringBuffer sb) throws ServletException, IOException {

        try {
            writeLine(sb, "STARTCTXLOGIN ");

            // this will always log you in, even if you already logged in to the servlet
            ctx.login();

            Subject subj = ctx.getSubject();
            writeLine(sb, "callerSubject: " + subj);

            String cacheKey = getCacheKeyFromSubject(subj);
            writeLine(sb, "cacheKey: " + cacheKey);

        } catch (SecurityException se) {
            writeLine(sb, "Failed to login. SecurityException message: " + se.getMessage());
            throw se;
        } catch (LoginException le) {
            writeLine(sb, "Failed to login. LoginException message: " + le.getMessage());
        } catch (Exception e) {
            writeLine(sb, "Unexpected exception, " + e.getMessage());
        } catch (Error er) {
            writeLine(sb, "Unexpected error, " + er.getMessage());
        } finally {
            writeLine(sb, "ENDCTXLOGIN");
        }
    }

    private void doLogout(HttpServletRequest req, HttpServletResponse resp, String method, LoginContext ctx, CallbackHandler wscbh,
                          StringBuffer sb) throws ServletException, IOException {

        try {
            writeLine(sb, "STARTCTXLOGOUT ");
            ctx.logout();

            Subject subj = ctx.getSubject();
            writeLine(sb, "callerSubject: " + subj);

        } catch (LoginException le) {
            writeLine(sb, "Failed to logout. LoginException message: " + le.getMessage());
        } catch (Exception e) {
            writeLine(sb, "Unexpected exception, " + e.getMessage());
        } finally {
            writeLine(sb, "ENDCTXLOGOUT");
        }

    }

    private static String getCacheKeyFromSubject(Subject subject) {

        String cacheKey = null;
        Set<Object> credentials = subject.getPublicCredentials();
        Hashtable<String, String> publicAttributes = getPublicAttributes(credentials);
        if (publicAttributes != null) {
            cacheKey = publicAttributes.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
        }
        return cacheKey;

    }

    private static Hashtable<String, String> getPublicAttributes(Set<Object> credentials) {
        Hashtable<String, String> attributes = null;
        Iterator<Object> iCredentials = credentials.iterator();
        while (iCredentials.hasNext()) {
            Object credential = iCredentials.next();
            if (credential instanceof java.util.Hashtable) {
                attributes = (Hashtable<String, String>) credential;
                break;
            }
        }
        return attributes;
    }
}
