/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.PrivilegedActionException;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.s4u2proxy.SpnegoHelper;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.wsspi.security.token.SpnegoTokenHelper;

/**
 * Servlet implementation class SPNEGOTokenHelperServlet
 */
public class SPNEGOTokenHelperServlet extends HttpServlet {
    private static final String ENV = "ENV";
    private static final String SPN = "SPN";
    private static final String UPN = "UPN";
    private static final String JAAS = "JAAS";
    private static final String TEST = "TEST";
    private static final String DELEGATE_SERVICE_SPN = "DELEGATESPN";
    private static final String KRB5_KEYTAB = "KRB5KEYTAB";
    private static final String KRB5_CONF = "KRB5CONF";
    private static final String ACTIVE_PWD = "ActivePwd";
    private static final String ACTIVE_USERID = "ActiveUserid";
    private static final long serialVersionUID = 1L;
    private String env;
    private String userid;
    private String pwd;
    private String spn;
    private String upn;
    private String jaas;
    private String delegateServiceSpn;
    private String krb5Keytab;
    private String krb5conf;

    private final String servletName = "SPNEGOTokenHelperServlet";
    private static int lifetime = 10000;
    private static boolean delegate = true;
    private String test;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public SPNEGOTokenHelperServlet() {
        super();
    }

    /**
     * Process incoming HTTP DELETE requests
     *
     * @param request  Object that encapsulates the request to the servlet
     * @param response Object that encapsulates the response from the servlet
     */
    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws javax.servlet.ServletException, java.io.IOException {

        super.doDelete(request, response);

        handleRequest(request, response);
    }

    /**
     * Process incoming HTTP GET requests
     *
     * @param request  Object that encapsulates the request to the servlet
     * @param response Object that encapsulates the response from the servlet
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws javax.servlet.ServletException, java.io.IOException {

        handleRequest(request, response);

    }

    /**
     * Process incoming HTTP POST requests
     *
     * @param request  Object that encapsulates the request to the servlet
     * @param response Object that encapsulates the response from the servlet
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws javax.servlet.ServletException, java.io.IOException {

        handleRequest(request, response);

    }

    /**
     * Process incoming HTTP PUT requests
     *
     * @param request  Object that encapsulates the request to the servlet
     * @param response Object that encapsulates the response from the servlet
     */
    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws javax.servlet.ServletException, java.io.IOException {

        super.doPut(request, response);

        handleRequest(request, response);
    }

    /**
     * Initializes the servlet.
     */
    @Override
    public void init() {
        // insert code to initialize the servlet here

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
     * Process incoming requests for information
     * This servlet has two forms.
     * <UL>
     * <LI> JAAS Login form: performs JAAS Login
     * <LI> JAAS form: performs JAAS Login, then invoke the selected EJB.
     * </UL>
     *
     * @param request  Object that encapsulates the request to the servlet
     * @param response Object that encapsulates the response from the servlet
     * @throws IOException
     * @throws ServletException
     */
    public void performTask(HttpServletRequest req,
                            HttpServletResponse resp, StringBuffer sb) throws IOException, ServletException {
        // Insert user code from here.
        try {
            String remoteUser = req.getRemoteUser();
            java.security.Principal principal = req.getUserPrincipal();
            String principalName = null;
            if (principal != null) {
                principalName = principal.getName();
            }

            env = validateParam(req, ENV);
            if (!(("Native".equals(env)) || ("Delegation".equals(env)) || ("NonDelegation".equals(env)) || ("S4U2Self".equals(env))))
                throw new ServletException(ENV + " parameter must be one of Native, Delegation or NonDelegation");

            userid = validateParam(req, ACTIVE_USERID);
            pwd = validateParam(req, ACTIVE_PWD);
            spn = validateParam(req, SPN);
            upn = validateParam(req, UPN);
            jaas = validateParam(req, JAAS);
            test = validateParam(req, TEST);
            boolean valuesRequired = "S4U2Proxy".equals(env) ? true : false;
            delegateServiceSpn = validateParam(req, DELEGATE_SERVICE_SPN, valuesRequired);
            krb5Keytab = validateParam(req, KRB5_KEYTAB, valuesRequired);
            krb5conf = validateParam(req, KRB5_CONF, false);
            writeLine(sb, "Welcome to " + this.getServletName());
            writeLine(sb, "request URI: " + req.getRequestURI());
            writeLine(sb, "remoteUser: " + remoteUser);
            writeLine(sb, "Principal object exist: " + (principal != null));
            writeLine(sb, "Parameters: ");
            writeLine(sb, "principalName: " + principalName);
            writeLine(sb, "ENV: " + env);
            writeLine(sb, "ACTIVE_USERID: " + userid);
            writeLine(sb, "ACTIVE_PWD: " + pwd);
            writeLine(sb, "SPN: " + spn);
            writeLine(sb, "UPN: " + upn);
            writeLine(sb, "JAAS: " + jaas);
            writeLine(sb, "DELEGATE_SERVICE_SPN: " + delegateServiceSpn);
            writeLine(sb, "KRB5KEYTAB: " + krb5Keytab);
            writeLine(sb, "KRB5CONF: " + krb5conf);
            writeLine(sb, "TEST: " + test);

            boolean pass = true;
            if (env != null) {
                if (env.equals("Delegation")) {
                    pass = performDelegationTests(sb);
                } else if (env.equals("Native")) {
                    pass = nativeLoginTest1(sb);
                } else if (env.equals("S4U2Self")) {
                    pass = performS4U2SelfTest(sb);
                }
            }
            if (!pass) {
                throw new ServletException("Some tests failed - refer to logs");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        resp.setContentType("text/html");

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
    private boolean performDelegationTests(StringBuffer sb) throws ServletException, IOException {
        boolean pass = true;

        if (test.equals("fromcallersubject")) {
            pass = fromCallerSubjectTest(sb) & pass;
        } else if (test.equals("fromsubject")) {
            pass = fromSubjectTest(sb) & pass;
        } else if (test.equals("froms4u2proxy")) {
            pass = fromSubjectS4U2ProxyTest(sb) & pass;
        } else if (test.equals("s4u2proxymulti")) {
            pass = s4u2proxyMultipleCallTest(sb) & pass;
        } else if (test.equals("fromuserid")) {
            pass = useridPwdCredsTest(sb) && pass;
        } else if (test.equals("fromupn")) {
            pass = upnCredsTest(sb) && pass;
        } else if (test.equals("negative")) {
            pass = performFailureTests(sb) && pass;
        }

        return pass;
    }

    private String validateParam(HttpServletRequest req, String param) throws ServletException {
        return validateParam(req, param, true);
    }

    private String validateParam(HttpServletRequest req, String param, boolean required) throws ServletException {
        String value = req.getParameter(param);
        if (((null == value) || ("".equals(value))) && required)
            throw new ServletException("Missing parameter " + param);
        return value;
    }

    private boolean performFailureTests(StringBuffer sb) {

        writeLine(sb, "Failure Tests - null params");

        boolean pass = true;
        pass = nullSPNTest1(sb) && pass;
        pass = nullSPNTest2(sb) && pass;
        pass = nullUPNTest1(sb) && pass;
        pass = nullUPNTest2(sb) && pass;
        pass = nullSubjectTest1(sb) && pass;
        pass = nullUseridTest1(sb) && pass;
        pass = nullUseridTest2(sb) && pass;
        pass = nullPasswordTest1(sb) && pass;
        pass = nullPasswordTest2(sb) && pass;

        writeLine(sb, "Failure Tests - Invalid params");

        pass = badUseridTest1(sb) && pass;
        pass = badUseridTest2(sb) && pass;
        pass = badSPNTest1(sb) && pass;
        pass = badSPNTest2(sb) && pass;
        // pass =  badSPNTest3(sb) && pass;   *****  was not throwing exception but got NPE in FFDC like other UPN test

        return pass;
    }

    private boolean nullSPNTest1(StringBuffer sb) {
        boolean pass = false;
        try {
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromCallerSubject(null, lifetime, delegate);
            writeLine(sb, "Null SPN Test #1 failed. Expected exception was not thrown");
        } catch (WSSecurityException e) {
            writeLine(sb, "Null SPN Test #1 failed. Unexpected WSSecurityException thrown " + e.getMessage());
        } catch (GSSException e) {
            int major = e.getMajor();
            if (major == GSSException.BAD_NAME) {
                pass = true;
                writeLine(sb, "Null SPN Test #1 Succeeded - GSSException.major=" + major);
            } else {
                writeLine(sb, "Null SPN Test #1 failed. Unexpected GSSException thrown Major=" + major);

            }
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Null SPN Test #1 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    private boolean nullSPNTest2(StringBuffer sb) {
        boolean pass = false;
        try {
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromCallerSubject("", lifetime, delegate);
            writeLine(sb, "Null SPN Test #2 failed. Expected exception was not thrown");
        } catch (WSSecurityException e) {
            writeLine(sb, "Null SPN Test #2 failed. Unexpected WSSecurityException thrown " + e.getMessage());
        } catch (GSSException e) {
            int major = e.getMajor();
            if (major == GSSException.BAD_NAME) {
                pass = true;
                writeLine(sb, "Null SPN Test #2 Succeeded - GSSException.major=" + major);
            } else {
                writeLine(sb, "Null SPN Test #2 failed. Unexpected GSSException thrown Major=" + major);

            }
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Null SPN Test #2 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    private boolean nullUPNTest1(StringBuffer sb) {
        boolean pass = false;
        try {
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromUpn("HTTP/spn.ibm.net@IBM.NET", new String(), null, lifetime, delegate);
            writeLine(sb, "Null UPN Test #1 failed. Expected exception was not thrown");
        } catch (GSSException e) {
            int major = e.getMajor();
            if (major == GSSException.BAD_NAME) {
                pass = true;
                writeLine(sb, "Null UPN Test #1 Succeeded - GSSException.major=" + major);
            } else {
                writeLine(sb, "Null UPN Test #1 failed. Unexpected GSSException thrown Major=" + major);

            }
        } catch (LoginException e) {
            writeLine(sb, "Null UPN Test #1. LoginException thrown " + e.getMessage());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Null UPN Test #1 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    private boolean nullUPNTest2(StringBuffer sb) {
        boolean pass = false;
        try {
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromUpn("HTTP/spn.ibm.net@IBM.NET", "", null, lifetime, delegate);
            writeLine(sb, "Null UPN Test #2 failed. Expected exception was not thrown");
        } catch (GSSException e) {
            int major = e.getMajor();
            if (major == GSSException.BAD_NAME) {
                pass = true;
                writeLine(sb, "Null UPN Test #2 Succeeded - GSSException.major=" + major);
            } else {
                writeLine(sb, "Null UPN Test #2 failed. Unexpected GSSException thrown Major=" + major);

            }
        } catch (LoginException e) {
            writeLine(sb, "Null UPN Test #2. LoginException thrown " + e.getMessage());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Null UPN Test #2 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    private boolean nullSubjectTest1(StringBuffer sb) {
        boolean pass = false;
        try {
            Subject subject = null;
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromSubject("HTTP/spn.ibm.net@IBM.NET", subject, lifetime, delegate);
            writeLine(sb, "Null Subject Test #1 failed. Expected exception was not thrown");
        } catch (GSSException e) {
            int major = e.getMajor();
            //if (major == GSSException.BAD_NAME) {
            if (major == GSSException.NO_CRED) {
                pass = true;
                writeLine(sb, "Null Subject Test #1 Succeeded - GSSException.major=" + major);
            } else {
                writeLine(sb, "Null Subject Test #1 failed. Unexpected GSSException thrown Major=" + major);

            }
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Null Subject Test #1 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    private boolean nullUseridTest1(StringBuffer sb) {
        boolean pass = false;
        try {
            String userid = null;
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromUseridPassword("HTTP/spn.ibm.net@IBM.NET", userid, "password", lifetime, delegate);
            writeLine(sb, "Null Userid Test #1 failed. Expected exception was not thrown");
        } catch (GSSException e) {
            int major = e.getMajor();
            if (major == GSSException.BAD_NAME) {
                pass = true;
                writeLine(sb, "Null Userid Test #1 Succeeded - GSSException.major=" + major);
            } else {
                writeLine(sb, "Null Userid Test #1 failed. Unexpected GSSException thrown Major=" + major);

            }
        } catch (LoginException e) {
            writeLine(sb, "Null Userid Test #1 failed. Unexpected LoginException thrown " + e.getMessage());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Null Userid Test #1 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    private boolean nullUseridTest2(StringBuffer sb) {
        boolean pass = false;
        try {
            String userid = "";
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromUseridPassword("HTTP/spn.ibm.net@IBM.NET", userid, "password", lifetime, delegate);
            writeLine(sb, "Null Userid Test #2 failed. Expected exception was not thrown");
        } catch (GSSException e) {
            int major = e.getMajor();
            if (major == GSSException.BAD_NAME) {
                pass = true;
                writeLine(sb, "Null Userid Test #2 Succeeded - GSSException.major=" + major);
            } else {
                writeLine(sb, "Null Userid Test #2 failed. Unexpected GSSException thrown Major=" + major);

            }
        } catch (LoginException e) {
            writeLine(sb, "Null Userid Test #2 failed. Unexpected LoginException thrown " + e.getMessage());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Null Userid Test #3 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    private boolean nullPasswordTest1(StringBuffer sb) {
        boolean pass = false;
        try {
            String password = null;
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromUseridPassword("HTTP/spn.ibm.net@IBM.NET", "userid", password, lifetime, delegate);
            writeLine(sb, "Null Password Test #1 failed. Expected exception was not thrown");
        } catch (GSSException e) {
            int major = e.getMajor();
            //if (major == GSSException.BAD_NAME) {
            if (major == GSSException.NO_CRED) {
                pass = true;
                writeLine(sb, "Null Password Test #1 Succeeded - GSSException.major=" + major);
            } else {
                writeLine(sb, "Null Password Test #1 failed. Unexpected GSSException thrown Major=" + major);

            }
        } catch (LoginException e) {
            writeLine(sb, "Null Password Test #1 failed. Unexpected LoginException thrown " + e.getMessage());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Null Password Test #1 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    private boolean nullPasswordTest2(StringBuffer sb) {
        boolean pass = false;
        try {
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromUseridPassword("HTTP/spn.ibm.net@IBM.NET", "userid", "", lifetime, delegate);
            writeLine(sb, "Null Password Test #2 failed. Expected exception was not thrown");
        } catch (GSSException e) {
            int major = e.getMajor();
            //if (major == GSSException.BAD_NAME) {
            if (major == GSSException.NO_CRED) {
                pass = true;
                writeLine(sb, "Null Password Test #2 Succeeded - GSSException.major=" + major);
            } else {
                writeLine(sb, "Null Password Test #2 failed. Unexpected GSSException thrown Major=" + major);

            }
        } catch (LoginException e) {
            writeLine(sb, "Null Password Test #2 failed. Unexpected LoginException thrown " + e.getMessage());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Null Password Test #2 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    private boolean badUseridTest1(StringBuffer sb) {
        boolean pass = false;
        try {
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromUseridPassword(spn, "bogusUser", "bogusPassword", lifetime, delegate);
            writeLine(sb, "Bad Userid Test #1 failed. Expected exception was not thrown");
        } catch (GSSException e) {
            int major = e.getMajor();
            writeLine(sb, "Bad Userid Test #1 failed. Unexpected GSSException thrown Major=" + major);

        } catch (LoginException e) {
            writeLine(sb, "Bad Userid Test #1 Succeeded - LoginException " + e.getMessage());
            pass = true;
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Bad Userid Test #1 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    private boolean badUseridTest2(StringBuffer sb) {
        boolean pass = false;
        try {
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromUseridPassword(spn, userid, "bogusPassword", lifetime, delegate);
            writeLine(sb, "Bad Userid Test #2 failed. Expected exception was not thrown");
        } catch (GSSException e) {
            int major = e.getMajor();
            writeLine(sb, "Bad Userid Test #2 failed. Unexpected GSSException thrown Major=" + major);

        } catch (LoginException e) {
            writeLine(sb, "Bad Userid Test #2 Succeeded - LoginException " + e.getMessage());;
            pass = true;
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Bad Userid Test #2 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    private boolean badSPNTest1(StringBuffer sb) {
        boolean pass = false;
        try {
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromCallerSubject("HTTP/invalidSPN.ibm.net@IBM.NET", lifetime, delegate);
            writeLine(sb, "Bad SPN Test #1 failed. Expected exception was not thrown");
        } catch (WSSecurityException e) {
            writeLine(sb, "Bad SPN Test #1 failed. Unexpected WSSecurityException thrown " + e.getMessage());
        } catch (GSSException e) {
            int major = e.getMajor();
            if (major == GSSException.FAILURE) {
                pass = true;
                writeLine(sb, "Bad SPN Test #1 Succeeded - GSSException major= " + major);
            } else {
                writeLine(sb, "Bad SPN Test #1 failed. Unexpected GSSException thrown Major=" + major);

            }
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Bad SPN Test #1 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    private boolean badSPNTest2(StringBuffer sb) {
        boolean pass = false;
        try {
            @SuppressWarnings("unused")
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromUseridPassword("HTTP/invalidSPN.ibm.net@IBM.NET", userid, pwd, lifetime, delegate);
            writeLine(sb, "Bad SPN Test #2 failed. Expected exception was not thrown");
        } catch (GSSException e) {
            int major = e.getMajor();
            if (major == GSSException.FAILURE) {
                pass = true;
                writeLine(sb, "Bad SPN Test #2 Succeeded - GSSException major= " + major);
            } else {
                writeLine(sb, "Bad SPN Test #2 failed. Unexpected GSSException thrown Major=" + major);

            }
        } catch (LoginException e) {
            writeLine(sb, "Bad SPN Test #2 failed. Unexpected LoginException thrown " + e.getMessage());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Bad SPN Test #2 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }
        return pass;
    }

    /***********************************************************************************************************
     * private boolean badSPNTest3(StringBuffer sb){
     * boolean pass = false;
     * try {
     *
     * @SuppressWarnings("unused")
     * String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromUpn("HTTP/invalidSPN.ibm.net@IBM.NET", upn, jaas, lifetime, delegate);
     * writeLine(sb,"Bad SPN Test #3 failed. Expected exception was not thrown");
     * } catch (GSSException e) {
     * int major = e.getMajor();
     * if (major == GSSException.FAILURE) {
     * pass = true;
     * writeLine(sb,"Bad SPN Test #3 Succeeded - GSSException major= "+ major );
     * } else {
     * writeLine(sb,"Bad SPN Test #3 failed. Unexpected GSSException thrown Major=" + major);
     *
     * }
     * } catch (LoginException e) {
     * writeLine(sb,"Bad SPN Test #3. LoginException thrown "+ e.getMessage());
     * } catch (PrivilegedActionException e) {
     * writeLine(sb,"Bad SPN Test #3 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
     * }
     * return pass;
     * }
     *********************************************************************************************************************/

    private boolean fromCallerSubjectTest(StringBuffer sb) {
        boolean pass = false;
        try {

            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromCallerSubject(spn, lifetime, delegate);
            pass = true;
            writeLine(sb, "Delegate From Caller Subject Test #2 Succeeded  ");
            writeLine(sb, "token:" + token);
        } catch (WSSecurityException e) {
            writeLine(sb, "Delegate From Caller Subject Test #2 failed. Unexpected WSSecurityException thrown " + e.getMessage());
        } catch (GSSException e) {
            writeLine(sb, "Delegate From Caller Subject Test #2 failed. Unexpected GSSException thrown Major=" + e.getMajor());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Delegate From Caller Subject Test #2 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }

        return pass;
    }

    private boolean fromSubjectTest(StringBuffer sb) {
        boolean pass = false;
        try {
            Subject subject = WSSubject.getCallerSubject();
            // set delegate to false for s4u2proxy testing
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromSubject(spn, subject, lifetime, false);
            GSSCredential gssCred = SubjectHelper.getGSSCredentialFromSubject(subject);
            GSSName gssName = gssCred.getName();
            writeLine(sb, "Call to SpnegoTokenHelper.buildSpnegoAuthorizationFromSubject Succeeded  ");
            writeLine(sb, "GSSCredential: " + gssCred);
            writeLine(sb, "GSSCredential name is: " + gssName);
            writeLine(sb, "token:" + token);
            pass = true;
        } catch (WSSecurityException e) {
            writeLine(sb, "Call to SpnegoTokenHelper.buildSpnegoAuthorizationFromSubject failed. Unexpected WSSecurityException thrown " + e.getMessage());
        } catch (GSSException e) {
            writeLine(sb, "Call to SpnegoTokenHelper.buildSpnegoAuthorizationFromSubject failed. Unexpected GSSException thrown Major=" + e.getMajor());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Call to SpnegoTokenHelper.buildSpnegoAuthorizationFromSubject failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        } catch (Exception e) {
            writeLine(sb, "Call to SpnegoTokenHelper.buildSpnegoAuthorizationFromSubject failed. Unexpected Exception thrown " + e.getMessage());
        }

        return pass;
    }

    private boolean s4u2proxyMultipleCallTest(StringBuffer sb) {
        boolean pass = false;
        try {
            Subject subject = WSSubject.getCallerSubject();
            // set delegate to false for s4u2proxy testing
            String token1 = SpnegoHelper.buildS4U2proxyAuthorization(spn, subject, lifetime, false);
            writeLine(sb, "token1: " + token1);
            String token2 = SpnegoHelper.buildS4U2proxyAuthorization(spn, subject, lifetime, false);
            writeLine(sb, "token2: " + token2);
            if (token1.equals(token2)) { // ensure we get 2 distinct tokens
                pass = false;
                return pass;
            }
            GSSCredential gssCred = SubjectHelper.getGSSCredentialFromSubject(subject);
            if (gssCred != null) {
                GSSName gssName = gssCred.getName();
                writeLine(sb, "Call to SpnegoHelper.buildS4U2proxyAuthorization Succeeded  ");
                writeLine(sb, "GSSCredential: " + gssCred);
                writeLine(sb, "GSSCredential name is: " + gssName);
            } else {
                writeLine(sb, "GSSCredential is null");
            }
            pass = true;
        } catch (WSSecurityException e) {
            writeLine(sb, "Call to SpnegoHelper.buildS4U2proxyAuthorization failed. Unexpected WSSecurityException thrown " + e.getMessage());
        } catch (GSSException e) {
            writeLine(sb, "Call to SpnegoHelper.buildS4U2proxyAuthorization failed. Unexpected GSSException thrown Major=" + e.getMajor());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Call to SpnegoHelper.buildS4U2proxyAuthorization failed . Unexpected PrivilegedActionException thrown " + e.getMessage());
        } catch (Exception e) {
            writeLine(sb, "Call to SpnegoHelper.buildS4U2proxyAuthorization failed. Unexpected Exception thrown " + e.getMessage());
        }

        return pass;
    }

    private boolean fromSubjectS4U2ProxyTest(StringBuffer sb) {
        boolean pass = false;
        try {
            Subject subject = WSSubject.getCallerSubject();
            // set delegate to false for s4u2proxy testing
            String token = SpnegoHelper.buildS4U2proxyAuthorization(spn, subject, lifetime, false);
            GSSCredential gssCred = SubjectHelper.getGSSCredentialFromSubject(subject);
            GSSName gssName = gssCred.getName();
            writeLine(sb, "Call to SpnegoHelper.buildS4U2proxyAuthorization Succeeded  ");
            writeLine(sb, "GSSCredential: " + gssCred);
            writeLine(sb, "GSSCredential name is: " + gssName);
            writeLine(sb, "token:" + token);
            pass = true;
        } catch (WSSecurityException e) {
            writeLine(sb, "Call to SpnegoHelper.buildS4U2proxyAuthorization failed. Unexpected WSSecurityException thrown " + e.getMessage());
        } catch (GSSException e) {
            writeLine(sb, "Call to SpnegoHelper.buildS4U2proxyAuthorization failed. Unexpected GSSException thrown Major=" + e.getMajor());
            writeLine(sb, "Call to SpnegoHelper.buildS4U2proxyAuthorization failed. Unexpected GSSException thrown message =" + e.getMessage());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Call to SpnegoHelper.buildS4U2proxyAuthorization failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        } catch (Exception e) {
            writeLine(sb, "Call to SpnegoHelper.buildS4U2proxyAuthorization failed. Unexpected Exception thrown " + e.getMessage());
        }

        return pass;
    }

    private boolean useridPwdCredsTest(StringBuffer sb) {

        boolean pass = false;
        try {
            writeLine(sb, "Delegate UserId Password Test #2 using: ");
            writeLine(sb, "userid: " + userid);
            writeLine(sb, "password: " + pwd);
            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromUseridPassword(spn, userid, pwd, lifetime, delegate);
            pass = true;
            writeLine(sb, "Delegate UserId Password Test #2 Succeeded");
            writeLine(sb, "token:" + token);
        } catch (GSSException e) {
            writeLine(sb, "Delegate UserId Password Test #2 failed. Unexpected GSSException thrown Major=" + e.getMajor());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Delegate UserId Password Test #2 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        } catch (LoginException e) {
            writeLine(sb, "Delegate UserId Password Test #2 failed. Unexpected LoginException thrown " + e.getMessage());
        }

        return pass;
    }

    private boolean upnCredsTest(StringBuffer sb) {

        boolean pass = false;
        try {

            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromUpn(spn, upn, jaas, lifetime, delegate);
            pass = true;
            writeLine(sb, "Delegate UPN Test #2 Succeeded" + token);
        } catch (GSSException e) {
            writeLine(sb, "Delegate UPN Test #2 failed. Unexpected GSSException thrown Major=" + e.getMajor());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Delegate UPN Test #2 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        } catch (LoginException e) {
            writeLine(sb, "Delegate UPN Test #2 failed. Unexpected LoginException thrown " + e.getMessage());
        }

        return pass;
    }

    private boolean nativeLoginTest1(StringBuffer sb) {
        boolean pass = false;

        try {

            String token = SpnegoTokenHelper.buildSpnegoAuthorizationFromNativeCreds(spn, lifetime, delegate);
            pass = true;
            writeLine(sb, "Native Login Test #1 Succeeded" + token);
        } catch (GSSException e) {
            writeLine(sb, "Native Login Test #1 failed. Unexpected GSSException thrown Major=" + e.getMajor());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "Native Login Test #1 failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        }

        return pass;
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
    private boolean performS4U2SelfTest(StringBuffer sb) throws ServletException, IOException {
        boolean pass = true;

        if (test.equals("S4U2SelfTest")) {
            pass = S4U2SelfTest(sb) & pass;
        }
        if (test.equals("S4U2SelfTestCallAPITwice")) {
            pass = S4U2SelfTestCallAPITwice(sb) & pass;
        }

        return pass;
    }

    private boolean S4U2SelfTest(StringBuffer sb) {
        boolean pass = false;
        try {
            if (krb5conf != null && krb5Keytab != null) {
                setSystemProperty("KRB5_KTNAME", krb5Keytab);
                setSystemProperty("java.security.krb5.conf", krb5conf);
            }
            String token = SpnegoHelper.buildS4U2ProxyAuthorizationUsingS4U2Self(upn, spn, lifetime, delegate, delegateServiceSpn, jaas, krb5Keytab);

            writeLine(sb, "We were able to obtain the following " + upn + " spnego token: " + token);
            pass = true;
            writeLine(sb, "S4U2Self Call API Test Succeeded");
            writeLine(sb, "Spnego token:" + token);
        } catch (GSSException e) {
            writeLine(sb, "S4U2Self Call API Test failed. Unexpected GSSException thrown Major=" + e.getMajor());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "S4U2Self Call API Test failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        } catch (Exception e) {
            writeLine(sb, "S4U2Self Call API Test failed. Unexpected Exception thrown " + e.getMessage());
        }

        return pass;
    }

    private boolean S4U2SelfTestCallAPITwice(StringBuffer sb) {
        boolean pass = false;
        try {
            if (krb5conf != null && krb5Keytab != null) {
                setSystemProperty("KRB5_KTNAME", krb5Keytab);
                setSystemProperty("java.security.krb5.conf", krb5conf);
            }
            String token = SpnegoHelper.buildS4U2ProxyAuthorizationUsingS4U2Self(upn, spn, lifetime, delegate, delegateServiceSpn, jaas, krb5Keytab);

            writeLine(sb, "We were able to obtain the following spnego token:" + token);

            String token2 = SpnegoHelper.buildS4U2ProxyAuthorizationUsingS4U2Self(upn, spn, lifetime, delegate, delegateServiceSpn, jaas, krb5Keytab);

            writeLine(sb, "We were able to obtain the following second spnego token:" + token2);

            if (token.compareToIgnoreCase(token2) != 0) {
                writeLine(sb, "Spnego Token 1 and Spnego Token 2 are different" + "\n");
            }

            pass = true;
            writeLine(sb, "S4U2Self Call API Twice Test Succeeded  ");
            writeLine(sb, "token:" + token);
        } catch (GSSException e) {
            writeLine(sb, "S4U2Self Call API Twice Test failed. Unexpected GSSException thrown Major=" + e.getMajor());
        } catch (PrivilegedActionException e) {
            writeLine(sb, "S4U2Self Call API Twice Test failed. Unexpected PrivilegedActionException thrown " + e.getMessage());
        } catch (Exception e) {
            writeLine(sb, "S4U2Self Call API Twice Test failed. Unexpected Exception thrown " + e.getMessage());
        }

        return pass;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setSystemProperty(final String propName, final String propValue) {
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            @Override
            public Object run() {
                return System.setProperty(propName, propValue);
            }
        });
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
