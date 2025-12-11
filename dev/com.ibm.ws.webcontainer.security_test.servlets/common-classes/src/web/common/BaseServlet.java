/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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

package web.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    /**
     * Common logic to handle any of the various requests this servlet supports.
     * The actual business logic can be customized by overriding performTask.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        writer.println("ServletName: " + servletName);

        StringBuffer sb = new StringBuffer();
        try {
            performTask(req, resp, sb);
        } catch (Throwable t) {
            t.printStackTrace(writer);
        }

        writer.write(sb.toString());
        writer.flush();
        writer.close();
    }

    /**
     * Default action for the servlet if not overridden.
     *
     * @param req
     * @param resp
     * @param writer
     * @throws ServletException
     * @throws IOException
     */
    protected void performTask(HttpServletRequest req,
                               HttpServletResponse resp, StringBuffer sb) throws ServletException, IOException {
        printProgrammaticApiValues(req, sb);
    }

    /**
     * Gets the SSO token from the subject.
     *
     * @param subject {@code null} is not supported.
     * @return
     */
    private SingleSignonToken getSSOToken(Subject subject) {
        SingleSignonToken ssoToken = null;
        Set<SingleSignonToken> ssoTokens = subject.getPrivateCredentials(SingleSignonToken.class);
        Iterator<SingleSignonToken> ssoTokensIterator = ssoTokens.iterator();
        if (ssoTokensIterator.hasNext()) {
            ssoToken = ssoTokensIterator.next();
        }
        return ssoToken;
    }

    /**
     * Print the various programmatic API values we care about.
     *
     * @param req
     * @param writer
     */
    protected void printProgrammaticApiValues(HttpServletRequest req,
                                              StringBuffer sb) {
        writeLine(sb, "getAuthType: " + req.getAuthType());
        writeLine(sb, "getRemoteUser: " + req.getRemoteUser());
        writeLine(sb, "getUserPrincipal: " + req.getUserPrincipal());

        if (req.getUserPrincipal() != null) {
            writeLine(sb, "getUserPrincipal().getName(): "
                          + req.getUserPrincipal().getName());
        }
        writeLine(sb, "isUserInRole(Employee): "
                      + req.isUserInRole("Employee"));
        writeLine(sb, "isUserInRole(Manager): " + req.isUserInRole("Manager"));
        String role = req.getParameter("role");
        if (role == null) {
            writeLine(sb, "You can customize the isUserInRole call with the follow paramter: ?role=name");
        }
        writeLine(sb, "isUserInRole(" + role + "): " + req.isUserInRole(role));

        Cookie[] cookies = req.getCookies();
        writeLine(sb, "Getting cookies");
        if (cookies != null && cookies.length > 0) {
            for (int i = 0; i < cookies.length; i++) {
                writeLine(sb, "cookie: " + cookies[i].getName() + " value: "
                              + cookies[i].getValue());
            }
        }
        writeLine(sb, "getRequestURL: " + req.getRequestURL().toString());

        try {
            Class policyContext = Class.forName("javax.security.jacc.PolicyContext");
            Method m = policyContext.getMethod("getHandlerKeys");
            Set<String> handlerKeys = (Set<String>) m.invoke(null);
            for (String key : handlerKeys) {
                writeLine(sb, "handlerKey(" + key + ")=true\n");
            }
        } catch (Exception e) {
            // expected for non JACC scenarios
        }

        try {
            // Get the CallerSubject
            Subject callerSubject = WSSubject.getCallerSubject();
            writeLine(sb, "callerSubject: " + callerSubject);

            // Get the public credential from the CallerSubject
            if (callerSubject != null) {
                WSCredential callerCredential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
                if (callerCredential != null) {
                    writeLine(sb, "callerCredential: " + callerCredential);
                } else {
                    writeLine(sb, "callerCredential: null");
                }
            } else {
                writeLine(sb, "callerCredential: null");
            }

            // getInvocationSubject for RunAs tests
            Subject runAsSubject = WSSubject.getRunAsSubject();
            writeLine(sb, "RunAs subject: " + runAsSubject);

            // Check for cache key for hashtable login test. Will return null otherwise
            String customCacheKey = null;
            if (callerSubject != null) {
                String[] properties = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };
                SubjectHelper subjectHelper = new SubjectHelper();
                Hashtable<String, ?> customProperties = subjectHelper.getHashtableFromSubject(callerSubject, properties);
                if (customProperties != null) {
                    customCacheKey = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
                }
                if (customCacheKey == null) {
                    SingleSignonToken ssoToken = getSSOToken(callerSubject);
                    if (ssoToken != null) {
                        String[] attrs = ssoToken.getAttributes(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
                        if (attrs != null && attrs.length > 0) {
                            customCacheKey = attrs[0];
                        }
                    }
                }
            }
            writeLine(sb, "customCacheKey: " + customCacheKey);

        } catch (NoClassDefFoundError ne) {
            // For OSGI App testing (EBA file), we expect this exception for all packages that are not public
            writeLine(sb, "NoClassDefFoundError for SubjectManager: " + ne);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * "Writes" the msg out to the client. This actually appends the msg
     * and a line delimiters to the running StringBuffer. This is necessary
     * because if too much data is written to the PrintWriter before the
     * logic is done, a flush() may get called and lock out changes to the
     * response.
     *
     * @param sb  Running StringBuffer
     * @param msg Message to write
     */
    void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + "\n");
    }

}
