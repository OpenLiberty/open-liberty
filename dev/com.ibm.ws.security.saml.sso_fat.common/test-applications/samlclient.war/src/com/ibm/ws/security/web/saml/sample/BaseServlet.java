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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.websphere.security.saml2.Saml20Token;

/**
 * Base servlet which all of our test servlets extend.
 */
public abstract class BaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String servletName;

    BaseServlet(String servletName) {
        this.servletName = servletName;
    }

    protected void updateServletName(String servletName) {
        this.servletName = servletName;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        req.getSession().setAttribute("service_class_name", this.getClass().getName());
        if ("CUSTOM".equalsIgnoreCase(req.getMethod()))
            doCustom(req, res);
        else
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

    private void doCustom(HttpServletRequest req, HttpServletResponse resp)
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
            HttpServletResponse resp, StringBuffer sb)
            throws ServletException, IOException {
        printProgrammaticApiValues(req, sb);
    }

    /**
     * Gets the SSO token from the subject.
     * 
     * @param subject
     *            {@code null} is not supported.
     * @return
     */
    private Saml20Token getSSOToken(Subject subject) {
        Saml20Token ssoToken = null;
        Set<Saml20Token> ssoTokens = subject.getPrivateCredentials(Saml20Token.class);
        Iterator<Saml20Token> ssoTokensIterator = ssoTokens.iterator();
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

        // check get/post and parameters
        String method = req.getMethod();
        writeLine(sb, "Servlet method:" + method);
        if ("POST".equals(method)) {
            Enumeration<String> parameterNames = req.getParameterNames();
            while (parameterNames != null && parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();
                String[] values = req.getParameterValues(parameterName);
                if (values != null && values.length > 0) {
                    for (int iI = 0; iI < values.length; iI++) {
                        writeLine(sb, "parameter " + parameterName + ":" + values[iI]);
                    }
                } else {
                    writeLine(sb, "parameter " + parameterName + ":null or empty");
                }
            }
        } else {
            writeLine(sb, "queryString:" + req.getQueryString());
        }

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
                //String[] properties = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };
                //SubjectHelper subjectHelper = new SubjectHelper();
                //Hashtable<String, ?> customProperties = subjectHelper.getHashtableFromSubject(callerSubject, properties);
                //if (customProperties != null) {
                //    customCacheKey = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
                //}
                if (customCacheKey == null) {
                    Saml20Token ssoToken = getSSOToken(callerSubject);
                    if (ssoToken != null) {
                        //String[] attrs = ssoToken.getAttributes(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
                        //if (attrs != null && attrs.length > 0) {
                        customCacheKey = "Saml20Token";
                        //}
                        writeString(sb, "SamlAssertion: ", ssoToken.getSAMLAsString());
                    }
                }
            }

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
     * @param sb
     *            Running StringBuffer
     * @param msg
     *            Message to write
     */
    void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + "\n");
    }

    void writeDom(StringBuffer sb, String name, Element dom) {
        sb.append(name);
        sb.append("\n");
        traverse(dom, 0, sb);
        sb.append("\n");
    }

    void writeString(StringBuffer sb, String name, String domString) {
        sb.append(name);
        sb.append("\n");
        sb.append(domString);
        sb.append("\n");
    }

    static char[] _cIndent = new char[100];
    static int _iIndent = 3;

    private static void traverse(Node node, int iLevel, StringBuffer sb)
    {
        int iIndent = iLevel * _iIndent;
        printIndent(iIndent, sb);

        String strN = node.getNodeName();
        sb.append("<" + strN);

        int iAttribIndent = iIndent + strN.length() + 2;
        // Then print the Attribute
        NamedNodeMap attrlist = node.getAttributes();
        int iLength;
        if (attrlist != null)
        {
            iLength = attrlist.getLength();
            int iI = 0;
            while (iI < iLength)
            {
                Node attr = attrlist.item(iI);
                if (attr instanceof Attr)
                {
                    String str = ((Attr) attr).getName();
                    sb.append(" " + str +
                            "=\"" + ((Attr) attr).getValue() + "\"");
                    iI++;
                    break;
                }
                iI++;
            }

            for (; iI < iLength; iI++)
            {
                Node attr = attrlist.item(iI);
                if (attr instanceof Attr)
                {
                    sb.append("\n");
                    printIndent(iAttribIndent, sb);
                    String str = ((Attr) attr).getName();
                    sb.append(str +
                            "=\"" + ((Attr) attr).getValue() + "\"");
                }
            }
        }

        boolean bClose = false;
        boolean bEnd = false;

        NodeList nodelist = node.getChildNodes();
        // Traverse all the children of the root element
        iLength = nodelist.getLength();
        for (int iI = 0; iI < iLength; iI++)
        {
            Node item = nodelist.item(iI);
            // When child is an element
            if (item instanceof Element)
            {
                if (bClose == false && bEnd == false)
                {
                    bClose = true;
                    sb.append(">\n");
                }
                traverse(item, iLevel + 1, sb);
            }
            else
            {
                if (item instanceof Attr)
                {
                    Attr attr = (Attr) item;
                    sb.append("???ERR " + attr.getName() + "=\"" + attr.getValue() + "\"");
                }
                else if (item instanceof Text)
                {
                    if (!bClose)
                    {
                        bClose = true;
                        sb.append(">\n");
                    }
                    printText((Text) item, iIndent, sb);
                }
                else if (item instanceof Comment)
                {
                    if (!bEnd && !bClose)
                    {
                        boolean bNext = false;
                        for (int iJ = iI + 1; iJ < iLength; iJ++)
                        {
                            if (nodelist.item(iJ) instanceof Element ||
                                    nodelist.item(iJ) instanceof Attr)
                            {
                                bNext = true;
                                break;
                            }
                        }
                        if (bNext == false)
                        {
                            bEnd = true;
                            sb.append(">\n");
                        }
                        else
                        {
                            bClose = true;
                            sb.append(">\n");
                        }
                    }
                    // Skip all the text
                    printComment((Comment) item, iIndent, sb);
                }
                else
                {
                    sb.append("???  " + item.getClass().getName() +
                            "?" + item.getNodeName() +
                            "=" + item.getNodeValue()
                            );
                }
            }
        }
        if (!bEnd)
        {
            bEnd = true;
            if (bClose)
            {
                printIndent(iIndent, sb);
                sb.append("</" + strN + ">\n");
            }
            else
            {
                sb.append("/>\n");
            }
        }
    }

    private static void printIndent(int iIndent, StringBuffer sb)
    {
        for (int iI = 0; iI < iIndent; iI++)
        {
            sb.append(" ");
        }

    }

    private static void printComment(Comment comment, int iIndent, StringBuffer sb)
    {
        iIndent += _iIndent;
        printIndent(iIndent, sb);
        sb.append("<!--\n");

        String str = null;
        try
        {
            str = comment.getData();
        } catch (Exception e)
        {
            e.printStackTrace();
            return;
        }

        StringTokenizer st = new StringTokenizer(str, "\n\r", false);
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            printIndent(iIndent + 4, sb);
            sb.append(token + "\n");
        }

        printIndent(iIndent, sb);
        sb.append("-->\n");
    }

    private static void printText(Text comment, int iIndent, StringBuffer sb)
    {
        iIndent += _iIndent;

        String str = null;
        try
        {
            str = comment.getData();
        } catch (Exception e)
        {
            e.printStackTrace();
            return;
        }

        StringTokenizer st = new StringTokenizer(str, "\n\r", false);
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            printIndent(iIndent, sb);
            sb.append(token + "\n");
        }
    }

}
