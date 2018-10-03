/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.fat.common.apps.formlogin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * Base servlet which all of our test servlets extend.
 */
public abstract class BaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String servletName;

    protected BaseServlet(String servletName) {
        this.servletName = servletName;
    }

    protected void updateServletName(String servletName) {
        this.servletName = servletName;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if ("CUSTOM".equalsIgnoreCase(req.getMethod()))
            doCustom(req, res);
        else
            super.service(req, res);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    private void doCustom(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        writer.println("ServletName: " + servletName);

        StringBuffer sb = new StringBuffer();
        try {
            performTask(req, resp, sb);
            boolean dologout = req.getParameter("logout") != null ? true : false;
            if (dologout) {
                System.out.println("Test application class BaseServlet is logging out");
                req.logout();
                writeLine(sb, "Test Application class BaseServlet logged out\n");
                // req.getSession().invalidate();
            }
        } catch (Throwable t) {
            t.printStackTrace(writer);
        }

        writer.write(sb.toString());
        writer.flush();
        writer.close();
    }

    protected void performTask(HttpServletRequest req, HttpServletResponse resp, StringBuffer sb) throws ServletException, IOException {
        printProgrammaticApiValues(req, sb);
    }

    protected void printProgrammaticApiValues(HttpServletRequest req, StringBuffer sb) {
        printBasicInfo(req, sb);
        printUserRoleInfo(req, sb);
        printCookies(req, sb);
        try {
            Subject callerSubject = WSSubject.getCallerSubject();
            printCallerSubjectInfo(callerSubject, sb);
            printCustomCacheKey(callerSubject, sb);
        } catch (NoClassDefFoundError ne) {
            // For OSGI App testing (EBA file), we expect this exception for all packages that are not public
            writeLine(sb, "NoClassDefFoundError for SubjectManager: " + ne);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected void printBasicInfo(HttpServletRequest req, StringBuffer sb) {
        writeLine(sb, "getRequestURL: " + req.getRequestURL().toString());
        writeLine(sb, "getAuthType: " + req.getAuthType());
        writeLine(sb, "getRemoteUser: " + req.getRemoteUser());
        writeLine(sb, "getUserPrincipal: " + req.getUserPrincipal());
        if (req.getUserPrincipal() != null) {
            writeLine(sb, "getUserPrincipal().getName(): " + req.getUserPrincipal().getName());
        }
    }

    protected void printUserRoleInfo(HttpServletRequest req, StringBuffer sb) {
        writeLine(sb, "isUserInRole(Employee): " + req.isUserInRole("Employee"));
        writeLine(sb, "isUserInRole(Manager): " + req.isUserInRole("Manager"));
        String role = req.getParameter("role");
        if (role == null) {
            writeLine(sb, "You can customize the isUserInRole call with the follow paramter: ?role=name");
        }
        writeLine(sb, "isUserInRole(" + role + "): " + req.isUserInRole(role));
    }

    protected void printCookies(HttpServletRequest req, StringBuffer sb) {
        Cookie[] cookies = req.getCookies();
        writeLine(sb, "Getting cookies");
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                writeLine(sb, "cookie: " + cookie.getName() + " value: " + cookie.getValue());
            }
        }
    }

    protected void printCallerSubjectInfo(Subject callerSubject, StringBuffer sb) throws WSSecurityException {
        writeLine(sb, "callerSubject: " + callerSubject);
        // Get the public credential from the CallerSubject
        if (callerSubject != null) {
            printSubjectCredentials(callerSubject, sb);
        } else {
            writeLine(sb, "callerCredential: null");
        }

        // getInvocationSubject for RunAs tests
        Subject runAsSubject = WSSubject.getRunAsSubject();
        writeLine(sb, "RunAs subject: " + runAsSubject);
    }

    protected void printSubjectCredentials(Subject callerSubject, StringBuffer sb) {
        printPublicCredentials(callerSubject, sb);
        printPrivateCredentials(callerSubject, sb);
    }

    protected void printPublicCredentials(Subject callerSubject, StringBuffer sb) {
        WSCredential callerCredential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
        if (callerCredential != null) {
            writeLine(sb, "callerCredential: " + callerCredential);
        } else {
            writeLine(sb, "callerCredential: null");
        }
    }

    /**
     * Prints the various private credentials of the subject. Can be overridden by extending classes to print specific types of
     * private credentials (JWTs, UserProfile, etc.).
     */
    protected void printPrivateCredentials(Subject callerSubject, StringBuffer sb) {
        // To be overridden, if desired
    }

    protected void printCustomCacheKey(Subject callerSubject, StringBuffer sb) {
        // Check for cache key for hashtable login test. Will return null otherwise
        String customCacheKey = null;
        if (callerSubject != null) {
            customCacheKey = getCustomCacheKeyFromSubjectCustomProps(callerSubject);
            if (customCacheKey == null) {
                customCacheKey = getCustomCacheKeyFromSsoToken(callerSubject, sb);
            }
        }
        writeLine(sb, "customCacheKey: " + customCacheKey);
    }

    protected String getCustomCacheKeyFromSubjectCustomProps(Subject callerSubject) {
        String[] properties = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };
        SubjectHelper subjectHelper = new SubjectHelper();
        Hashtable<String, ?> customProperties = subjectHelper.getHashtableFromSubject(callerSubject, properties);
        if (customProperties != null) {
            return (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
        }
        return null;
    }

    protected String getCustomCacheKeyFromSsoToken(Subject callerSubject, StringBuffer sb) {
        SingleSignonToken ssoToken = getSsoToken(callerSubject, sb);
        if (ssoToken != null) {
            String[] attrs = ssoToken.getAttributes(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
            if (attrs != null && attrs.length > 0) {
                return attrs[0];
            }
        }
        return null;
    }

    /**
     * Gets the SSO token from the subject.
     *
     * @param subject
     *            {@code null} is not supported.
     */
    protected SingleSignonToken getSsoToken(Subject subject, StringBuffer sb) {
        SingleSignonToken ssoToken = null;
        Set<SingleSignonToken> ssoTokens = subject.getPrivateCredentials(SingleSignonToken.class);
        writeLine(sb, "Number of SSO token: " + ssoTokens.size());
        Iterator<SingleSignonToken> ssoTokensIterator = ssoTokens.iterator();
        if (ssoTokensIterator.hasNext()) {
            ssoToken = ssoTokensIterator.next();
        }
        return ssoToken;
    }

    /**
     * "Writes" the msg out to the client. This actually appends the msg
     * and a line delimiters to the running StringBuffer. This is necessary
     * because if too much data is written to the PrintWriter before the
     * logic is done, a flush() may get called and lock out changes to the
     * response.
     *
     * @param sb
     *            Running StringBuffer
     * @param msg
     *            Message to write
     */
    protected void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + "\n");
    }

}
