/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticateApi;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.util.WebContainerSystemProps;
import com.ibm.wsspi.webcontainer.osgi.extension.WebExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public class FormLogoutExtensionProcessor extends WebExtensionProcessor {
    private static final TraceComponent tc = Tr.register(FormLogoutExtensionProcessor.class);
    protected static final String DEFAULT_LOGOUT_MSG = "<!DOCTYPE HTML PUBLIC \"-//W3C/DTD HTML 4.0 Transitional//EN\">" +
                                                       "<HTML><TITLE>Default Logout Exit Page</TITLE>" +
                                                       "<BODY><H2>Successful Logout</H2></BODY></HTML>";
    private static String ABSOLUTE_URI = "com.ibm.websphere.security.web.absoluteUri";
    private boolean absoluteUri = false;
    private final WebAppSecurityConfig webAppSecurityConfig;
    AuthenticateApi authenticateApi = null;

    public FormLogoutExtensionProcessor(IServletContext webapp,
                                        WebAppSecurityConfig webAppSecConfig,
                                        AuthenticateApi authenticateApi) {
        super(webapp);
        this.authenticateApi = authenticateApi;
        webAppSecurityConfig = webAppSecConfig;
        String absUri = System.getProperty(ABSOLUTE_URI);
        if (absUri != null && absUri.equalsIgnoreCase("true"))
            absoluteUri = true;
    }

    @Override
    public void handleRequest(ServletRequest req, ServletResponse res) throws Exception {
        if (req instanceof HttpServletRequest && res instanceof HttpServletResponse) {
            final HttpServletRequest request = (HttpServletRequest) req;
            final HttpServletResponse response = (HttpServletResponse) res;

            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws ServletException, IOException {
                    formLogout(request, response);
                    return null;
                }
            });
        }
    }

    /**
     * Log the user out by clearing the LTPA cookie if LTPA and SSO are enabled. Must also invalidate
     * the http session since it contains user id and password.
     * Finally, if the user specified an exit page with a form parameter of logoutExitPage,
     * redirect to the specified page.
     *
     * This is a special hidden servlet which is always loaded by the servlet engine. Logout is
     * achieved by having a html, jsp, or other servlet which specifies ibm_security_logout
     * HTTP post action.
     *
     * @param req The http request object
     * @param res The http response object.
     * @exception ServletException
     * @exception IOException
     */
    private void formLogout(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        try {
            // if we have a valid custom logout page, set an attribute so SAML SLO knows about it.
            String exitPage = getValidLogoutExitPage(req);
            if (exitPage != null) {
                req.setAttribute("FormLogoutExitPage", exitPage);
            }
            authenticateApi.logout(req, res, webAppSecurityConfig);
            String str = null;

            // if SAML SLO is in use, it will write the audit record and take care of the logoutExitPage redirection
            if (req.getAttribute("SpSLOInProgress") == null) {
                AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, str);
                authResult.setAuditLogoutSubject(authenticateApi.returnSubjectOnLogout());
                authResult.setAuditCredType("FORM");
                authResult.setAuditOutcome(AuditEvent.OUTCOME_SUCCESS);
                authResult.setTargetRealm(authResult.realm);
                Audit.audit(Audit.EventID.SECURITY_AUTHN_TERMINATE_01, req, authResult, Integer.valueOf(res.getStatus()));
                redirectLogoutExitPage(req, res);
            }
        } catch (ServletException se) {
            String str = "ServletException: " + se.getMessage();

            AuthenticationResult authResult = new AuthenticationResult(AuthResult.FAILURE, str);
            authResult.setAuditCredType("FORM");
            authResult.setAuditOutcome(AuditEvent.OUTCOME_FAILURE);
            authResult.setTargetRealm(authResult.realm);
            Audit.audit(Audit.EventID.SECURITY_AUTHN_TERMINATE_01, req, authResult, Integer.valueOf(res.getStatus()));

            throw se;
        } catch (IOException ie) {
            String str = "IOException: " + ie.getMessage();

            AuthenticationResult authResult = new AuthenticationResult(AuthResult.FAILURE, str);
            authResult.setAuditCredType("FORM");
            authResult.setAuditOutcome(AuditEvent.OUTCOME_FAILURE);
            authResult.setTargetRealm(authResult.realm);
            Audit.audit(Audit.EventID.SECURITY_AUTHN_TERMINATE_01, req, authResult, Integer.valueOf(res.getStatus()));

            throw ie;

        }

    }

    /**
     * return a logoutExitPage string suitable for redirection if one is defined and valid.
     * else return null
     */
    private String getValidLogoutExitPage(HttpServletRequest req) {

        boolean valid = false;
        String exitPage = req.getParameter("logoutExitPage");
        if (exitPage != null && exitPage.length() != 0) {
            boolean logoutExitURLaccepted = verifyLogoutURL(req, exitPage);
            if (logoutExitURLaccepted) {
                exitPage = removeFirstSlash(exitPage);
                exitPage = compatibilityExitPage(req, exitPage);
                valid = true;
            }
        }
        return valid == true ? exitPage : null;
    }

    /**
     * @param req
     * @param res
     * @throws IOException
     */
    private void redirectLogoutExitPage(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String exitPage = getValidLogoutExitPage(req);
        if (exitPage != null) {
            res.sendRedirect(res.encodeURL(exitPage));
        } else {
            useDefaultLogoutMsg(res);
        }
    }

    /**
     * Display the default logout message.
     *
     * @param res
     */
    private void useDefaultLogoutMsg(HttpServletResponse res) {
        try {
            PrintWriter pw = res.getWriter();
            pw.println(DEFAULT_LOGOUT_MSG);
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, e.getMessage());
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "No logoutExitPage specified");
    }

    /**
     * @param res
     * @param exitPage
     * @return
     */
    private String compatibilityExitPage(HttpServletRequest req, String exitPage) {
        if (!WebContainerSystemProps.getSendRedirectCompatibilty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Compatibility=false (default) redirect mode");
            if (absoluteUri) {
                if (exitPage.equals("/"))
                    exitPage = "";
                else if (exitPage.startsWith("/"))
                    exitPage = exitPage.substring(1);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Logout exit page is not relative to Context Root.");
            } else if (exitPage.startsWith("/")) {
                StringBuffer sb = new StringBuffer();
                String contextPath = req.getContextPath();
                if (contextPath != null && contextPath.endsWith("/")) {
                    int i = contextPath.lastIndexOf("/");
                    contextPath = contextPath.substring(0, i);
                }
                sb.append(contextPath);
                sb.append(exitPage);
                exitPage = sb.toString();
            }
        }
        return exitPage;
    }

    /**
     * @param exitPage
     * @return
     */
    private String removeFirstSlash(String exitPage) {
        //If the URI begins with "//" it needs to to have the first "/" removed the admin application (which is in error by using a URI with a "//")
        if (exitPage.startsWith("//")) {
            exitPage = exitPage.substring(1);
        }
        return exitPage;
    }

    /**
     * Verify the logout URL
     *
     * @param req
     * @param exitPage
     * @return
     */
    @FFDCIgnore({ UnknownHostException.class })
    protected boolean verifyLogoutURL(HttpServletRequest req, String exitPage) {
        boolean acceptURL = false;
        boolean allowLogoutPageRedirectToAnyHost = webAppSecurityConfig.getAllowLogoutPageRedirectToAnyHost();
        if (exitPage != null && !exitPage.equals("logon.jsp") && !allowLogoutPageRedirectToAnyHost) {
            String logoutURLHost = null;
            try {
                InetAddress thisHost = InetAddress.getLocalHost();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "return from getLocalHost: " + thisHost);
                String shortName = thisHost.getHostName();
                String ipAddress = thisHost.getHostAddress();
                String hostFullName = (ipAddress == null ? shortName : InetAddress.getByName(ipAddress).getHostName());
                try {
                    URI logoutURL = new URI(exitPage);
                    logoutURLHost = logoutURL.getHost();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "domain for exitPage url: " + logoutURLHost);
                    if (!logoutURL.isAbsolute()) {
                        /*
                         * If exitPage starts with "//" , it is a NetworkPath. We need to valid its target host.
                         * While the specification for network paths is preceded by exactly 2 slashes("//"), browsers will
                         * generally redirect network paths with 2 or more preceding slashes. Thus, we need to try to obtain the hostname
                         * utilizing only 2 preceding slashes.
                         */
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "URI " + exitPage + " is not absolute.");
                        String exitPageTrimmed = exitPage.trim();
                        if (exitPageTrimmed.startsWith("//")) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "URI " + exitPageTrimmed + " will be processed as a Network-Path." +
                                             " SendRedirect() defines a Network-Path as starting with // ");
                            char[] exitPageCharArr = exitPageTrimmed.toCharArray();
                            for (int i = 0; i < exitPageCharArr.length; i++) {
                                if (exitPageCharArr[i] != '/') {
                                    URI uri = new URI(exitPageTrimmed.substring(i - 2));
                                    //Set hostname for further checks.
                                    logoutURLHost = uri.getHost();
                                    if (logoutURLHost == null) {
                                        if (tc.isDebugEnabled())
                                            Tr.debug(tc, "SDK indicates " + exitPageTrimmed + " Network-Path does not contain a valid hostname.");
                                    } else {
                                        if (tc.isDebugEnabled())
                                            Tr.debug(tc, "SDK indicates " + exitPageTrimmed + " Network-Path contains a valid hostname," + logoutURLHost);
                                    }
                                    break;
                                }
                            }
                        } else {
                            //accept a relative URIs that are not Network-Path's
                            acceptURL = true;
                        }
                    }
                } catch (URISyntaxException urise) {
                    //The URI is invalid and will not be redirected to.
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "caught URISyntaxException getting urI for exitPage: " + urise.getMessage());
                    acceptURL = false;
                }
                if (logoutURLHost != null && acceptURL == false) {
                    acceptURL = isRedirectHostTheSameAsLocalHost(exitPage, logoutURLHost, hostFullName, shortName, ipAddress);
                    if (acceptURL)
                        return acceptURL;

                    acceptURL = isLogoutPageMatchDomainNameList(exitPage, logoutURLHost, webAppSecurityConfig.getLogoutPageRedirectDomainList());
                }
            } catch (UnknownHostException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "caught an unknown exception: " + e.getMessage());
                acceptURL = false;
            }
            if (!acceptURL && logoutURLHost != null)
                acceptURL = isRequestURLEqualsExitPageHost(req, logoutURLHost);
        } else {
            if (exitPage != null) {
                acceptURL = true;
            }
        }
        if (!acceptURL) {
            Tr.error(tc, "SEC_FORM_LOGOUTEXITPAGE_INVALID", new Object[] { req.getRequestURL(), req.getParameter("logoutExitPage") });
        }
        return acceptURL;
    }

    /**
     * Check the logout URL host name with various combination of shortName, full name and ipAddress.
     *
     * @param logoutURLhost
     * @param hostFullName
     * @param shortName
     * @param ipAddress
     */
    private boolean isRedirectHostTheSameAsLocalHost(String exitPage, String logoutURLhost, String hostFullName, String shortName, String ipAddress) {
        String localHostIpAddress = "127.0.0.1";
        boolean acceptURL = false;
        if (logoutURLhost.equalsIgnoreCase("localhost") || logoutURLhost.equals(localHostIpAddress) ||
            (hostFullName != null && logoutURLhost.equalsIgnoreCase(hostFullName)) ||
            (shortName != null && logoutURLhost.equalsIgnoreCase(shortName)) ||
            (ipAddress != null && logoutURLhost.equals(ipAddress))) {

            acceptURL = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "exitPage points to this host: all ok");
        }
        return acceptURL;
    }

    /**
     * Attempt to match the request URL's host with the URL for the exitPage this might be
     * the case of a proxy URL that is used in the request.
     *
     * @param req
     * @param acceptURL
     * @return
     */
    private boolean isRequestURLEqualsExitPageHost(HttpServletRequest req, String logoutURLhost) {
        boolean acceptURL = false;
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "about to attempt matching the logout exit url with the domain of the request.");
            StringBuffer requestURLString = req.getRequestURL();
            URL requestURL = new URL(new String(requestURLString));
            String requestURLhost = requestURL.getHost();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, " host of the request url is: " + requestURLhost + " and the host of the logout URL is: " + logoutURLhost);
            if (logoutURLhost != null && requestURLhost != null && logoutURLhost.equalsIgnoreCase(requestURLhost)) {
                acceptURL = true;
            }
        } catch (MalformedURLException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "caught Exception trying to form request URL object: " + e.getMessage());
            }
        }
        return acceptURL;
    }

    /**
     * @param exitPage
     * @param logoutURLhost
     * @param logoutPageRedirectDomainList
     * @return
     */
    boolean isLogoutPageMatchDomainNameList(String exitPage, String logoutURLhost, List<String> logoutPageRedirectDomainList) {
        boolean acceptURL = false;
        if (logoutPageRedirectDomainList != null && !logoutPageRedirectDomainList.isEmpty()) {
            for (Iterator<String> itr = logoutPageRedirectDomainList.iterator(); itr.hasNext();) {
                String domain = itr.next();
                if (logoutURLhost.endsWith(domain) || exitPage.endsWith(domain)) {
                    acceptURL = true;
                    break;
                }
            }
        }
        return acceptURL;
    }
}
