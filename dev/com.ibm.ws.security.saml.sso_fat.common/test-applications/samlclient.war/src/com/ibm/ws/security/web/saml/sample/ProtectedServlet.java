/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.web.saml.sample;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.saml.Constants;

import org.opensaml.xml.util.Base64;

/**
 * Base servlet which all of our test servlets extend.
 */
public class ProtectedServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String servletName;

    public ProtectedServlet() {
        this.servletName = "ProtectedServletInSamlcloent.war";
    }

    ProtectedServlet(String servletName) {
        this.servletName = servletName;
    }

    protected void updateServletName(String servletName) {
        this.servletName = servletName;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        req.getSession().setAttribute("service_class_name", this.getClass().getName());
        super.service(req, res);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
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
    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Saml20Token token = getSaml20TokenFromSubject();
        String tokenStr = "null";
        if (token != null) {
            tokenStr = getSamlStringFromToken(token);
            System.out.println("Authorization:" + tokenStr);
        }

        try {
            redirectRequest(req, resp, "Authorization", tokenStr);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void redirectRequest(HttpServletRequest req,
            HttpServletResponse resp,
            String headerName,
            String headerContent) throws Exception {

        StringBuffer sb = new StringBuffer();
        try {
            /*
             * <html>
             * <head>
             * <title>JAXRS SAML/OAuth from ProtectedServlet</title>
             * </head>
             * <body>
             * <h1>JAXRS SAML manual</h1>
             * <form name="authform" id="authform" method="GET" action="<%=formAction%>">
             * <input type="hidden" name="auto" value="true" />
             * <table width=800>
             * <tr><td>httpMethod</td><td><input type="text" name="http_method" value="<%=httpMethod%>" /></td></tr>
             * <tr><td>jaxrsUrl</td><td><input type="text" name="jaxrs_url" value="<%=jaxrsUrl%>" /></td></tr>
             * <tr><td>headerName</td><td><input type="text" name="header_name" value="<%=headerName%>" /></td></tr>
             * <tr><td>headerContent</td><td><input type="text" name="header_content" value="<%=headerContent%>" size="3072"
             * /></td></tr>
             * <tr><td>testCase</td><td><input type="text" name="test_case" value="<%=testCase%>" size="60" /></td></tr>
             * <tr><td>param1</td><td><input type="text" name="param_1" value="<%=param1%>" size="60" /></td></tr>
             * <tr><td>param2</td><td><input type="text" name="param_2" value="<%=param2%>" size="60" /></td></tr>
             * <tr><td colspan="2"><center><button type="submit" name="processAzn" style="width:100%">Process
             * Authorization</button></center></td></tr>
             * </table>
             * </form>
             * </body>
             * </html>
             */
            String requestUrl = "https://" + req.getServerName() +
                    ":" + req.getServerPort() +
                    req.getContextPath() + "/rsSaml.jsp";
            String httpMethod = reqGetParameter(req, "http_method", "GET");
            String jaxrsUrl = reqGetParameter(req, "jaxrs_url", "SimpleServlet");
            headerName = reqGetParameter(req, "header_name", headerName);
            headerContent = reqGetParameter(req, "header_content", headerContent);
            String testCase = reqGetParameter(req, "test_case", this.getClass().getName());
            String param1 = reqGetParameter(req, "param_1", "param1Value");
            String param2 = reqGetParameter(req, "param_2", "param2Value");
            // headerContent =
            sb.append("<HTML xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
            sb.append("<HEAD>");
            sb.append("<title>JAXRS SAML/OAuth from ProtectedServlet</title>");
            sb.append("</HEAD>");
            sb.append("<BODY onload=\"document.forms[0].submit()\">");
            sb.append("<FORM name=\"redirectform\" id=\"redirectform\" action=\"");
            sb.append(requestUrl);
            sb.append("\" method=\"GET\"><div>");

            sb.append("<input type=\"hidden\" name=\"auto\" value=\"true\" />");
            sb.append("<table width=\"800\">");
            sb.append("<tr><td>httpMethod</td><td><input type=\"text\" name=\"http_method\" value=\"" + httpMethod + "\" /></td></tr>");
            sb.append("<tr><td>jaxrsUrl</td><td><input type=\"text\" name=\"jaxrs_url\" value=\"" + jaxrsUrl + "\" /></td></tr>");
            sb.append("<tr><td>headerName</td><td><input type=\"text\" name=\"header_name\" value=\"" + headerName + "\" /></td></tr>");
            byte[] bytes = headerContent.getBytes("UTF-8");
            String encodedHeaderContent = Base64.encodeBytes(bytes);
            String urlContent = URLEncoder.encode(encodedHeaderContent, "UTF-8");
            System.out.println("headerContent:\n" + headerContent);
            System.out.println("encodedHeaderContent:\n" + encodedHeaderContent);
            System.out.println("urlContent:\n" + urlContent);
            sb.append("<tr><td>headerContent</td><td><input type=\"text\" name=\"header_content\" value=\"" + urlContent + "\" size=\"\" /></td></tr>");
            sb.append("<tr><td>testCase</td><td><input type=\"text\" name=\"test_case\" value=\"" + testCase + "\" size=\"60\" /></td></tr>");
            sb.append("<tr><td>param1</td><td><input type=\"text\" name=\"param_1\" value=\"" + param1 + "\" size=\"60\" /></td></tr>");
            sb.append("<tr><td>param2</td><td><input type=\"text\" name=\"param_2\" value=\"" + param2 + "\" size=\"60\" /></td></tr>");
            sb.append("</table>");

            sb.append("</div>");
            sb.append("<noscript><div>");
            sb.append("<button type=\"submit\" name=\"redirectform\">Process request</button>");
            sb.append("</div></noscript>");
            sb.append("</FORM></BODY></HTML>");
        } catch (Exception e) {
            // This should not happen
            throw new Exception(e); // Let SamlException handle the unexpected Exception
        }

        // HTTP 1.1.
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private, max-age=0");
        // HTTP 1.0. 
        resp.setHeader("Pragma", "no-cache");
        // Proxies. 
        resp.setDateHeader("Expires", 0);
        resp.setContentType("text/html");
        System.out.println("\n" + sb.toString());
        try {
            PrintWriter out = resp.getWriter();
            out.println(sb.toString());
            out.flush();
        } catch (IOException e) {
            // Error handling , in case
            throw new Exception(e); // Let the SamlException handle the unexpected Exception
        }
    }

    String reqGetParameter(HttpServletRequest req,
            String parameterName,
            String defaultValue) {
        String value = req.getParameter(parameterName);
        if (value == null || value.length() == 0) {
            value = defaultValue;
        } else {
            try {
                value = URLEncoder.encode(value, Constants.UTF8);
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return value;
    }

    protected String getSamlStringFromToken(Saml20Token token) {
        String samlString = null;
        try {
            samlString = token.getSAMLAsString();
        } catch (Exception e) {
            System.out.println("Exception while extracting SAML element: " + e.getCause());
        }
        return samlString;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Saml20Token getSaml20TokenFromSubject() {
        Saml20Token samlToken = null;
        try {
            final Subject subject = WSSubject.getRunAsSubject();

            samlToken = (Saml20Token) AccessController.doPrivileged(
                    new PrivilegedExceptionAction() {
                        @Override
                        public Object run() throws Exception
                        {
                            final Iterator authIterator = subject.getPrivateCredentials(Saml20Token.class).iterator();
                            if (authIterator.hasNext()) {
                                final Saml20Token token = (Saml20Token) authIterator.next();
                                return token;
                            }
                            return null;
                        }
                    });
        } catch (Exception e) {
            System.out.println("Exception while getting SAML token from subject:" + e.getCause());
        }
        return samlToken;
    }

}
