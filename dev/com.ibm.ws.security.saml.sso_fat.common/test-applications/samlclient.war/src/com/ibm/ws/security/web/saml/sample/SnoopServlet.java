/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.web.saml.sample;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.websphere.security.saml2.Saml20Token;

public class SnoopServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public SnoopServlet() {
        super();
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        doGet(req, res);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        PrintWriter out;

        res.setContentType("text/html");
        out = res.getWriter();

        out.println("<HTML><HEAD><TITLE>Snoop Servlet</TITLE></HEAD><BODY  BGCOLOR=\"WHITE\">");
        out.println("<h1>Snoop Servlet - Request/Client Information</h1>");
        out.println("<h2>Requested URL:</h2>");
        out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
        out.println("<tr><td>" + req.getRequestURL().toString() + "</td></tr></table><BR><BR>");

        out.println("<h2>Servlet Name:</h2>");
        out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
        out.println("<tr><td>" + escapeChar(getServletConfig().getServletName()) + "</td></tr></table><BR><BR>");

        Enumeration vEnum = getServletConfig().getInitParameterNames();
        if (vEnum != null && vEnum.hasMoreElements())
        {
            boolean first = true;
            while (vEnum.hasMoreElements())
            {
                if (first)
                {
                    out.println("<h2>Servlet Initialization Parameters</h2>");
                    out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
                    first = false;
                }
                String param = (String) vEnum.nextElement();
                out.println("<tr><td>" + escapeChar(param) + "</td><td>" + escapeChar(getInitParameter(param)) + "</td></tr>");
            }
            out.println("</table><BR><BR>");
        }

        vEnum = getServletConfig().getServletContext().getInitParameterNames();
        if (vEnum != null && vEnum.hasMoreElements())
        {
            boolean first = true;
            while (vEnum.hasMoreElements())
            {
                if (first)
                {
                    out.println("<h2>Servlet Context Initialization Parameters</h2>");
                    out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
                    first = false;
                }
                String param = (String) vEnum.nextElement();
                out.println("<tr><td>" + escapeChar(param) + "</td><td>" + escapeChar(getServletConfig().getServletContext().getInitParameter(param)) + "</td></tr>");
            }
            out.println("</table><BR><BR>");
        }

        out.println("<h2>Request Information:</h2>");
        out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
        print(out, "Request method", req.getMethod());
        print(out, "Request URI", req.getRequestURI());
        print(out, "Request protocol", req.getProtocol());
        print(out, "Servlet path", req.getServletPath());
        print(out, "Path info", req.getPathInfo());
        print(out, "Path translated", req.getPathTranslated());
        print(out, "Character encoding", req.getCharacterEncoding());
        print(out, "Query string", req.getQueryString());
        print(out, "Content length", req.getContentLength());
        print(out, "Content type", req.getContentType());
        print(out, "Server name", req.getServerName());
        print(out, "Server port", req.getServerPort());
        print(out, "Remote user", req.getRemoteUser());
        print(out, "Remote address", req.getRemoteAddr());
        print(out, "Remote host", req.getRemoteHost());
        print(out, "Remote port", req.getRemotePort());
        print(out, "Local address", req.getLocalAddr());
        print(out, "Local host", req.getLocalName());
        print(out, "Local port", req.getLocalPort());
        print(out, "Authorization scheme", req.getAuthType());
        if (req.getLocale() != null)
        {
            print(out, "Preferred Client Locale", req.getLocale().toString());
        }
        else
        {
            print(out, "Preferred Client Locale", "none");
        }
        Enumeration ee = req.getLocales();
        while (ee.hasMoreElements())
        {
            Locale cLocale = (Locale) ee.nextElement();
            if (cLocale != null)
            {
                print(out, "All Client Locales", cLocale.toString());
            }
            else
            {
                print(out, "All Client Locales", "none");
            }
        }
        print(out, "Context Path", escapeChar(req.getContextPath()));
        if (req.getUserPrincipal() != null)
        {
            print(out, "User Principal", escapeChar(req.getUserPrincipal().getName()));
        }
        else
        {
            print(out, "User Principal", "none");
        }
        try {
            String RealmName = getRealm();
            print(out, "Realm Name", RealmName);
            Subject subject = WSSubject.getRunAsSubject();
            String[] group = getGroups();
            if (group != null && group.length != 0) {
                out.println("<tr><td>Groups</td><td>");
                for (int i = 0; i < group.length; i++) {
                    String groupUser = group[i];
                    if (groupUser.indexOf(RealmName) > 0) {
                        groupUser = groupUser.substring((groupUser
                                .indexOf(RealmName) + RealmName.length()) + 1);
                        out.println(groupUser + "<br>");
                    }
                }
                out.println("</td></tr>");
            }

        } catch (WSSecurityException e1) {
            e1.printStackTrace();
        }

        out.println("</table><BR><BR>");

        Enumeration e = req.getHeaderNames();
        if (e.hasMoreElements())
        {
            out.println("<h2>Request Headers:</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
            String Value = null;
            while (e.hasMoreElements())
            {
                String name = (String) e.nextElement();
                out.println("<tr><td>" + escapeChar(name) + "</td>");
                out.println("<td>");
                int start = 0;
                Value = escapeChar(req.getHeader(name));
                if (Value.length() > 70)
                {
                    for (int j = 70; j <= Value.length(); j = j + 70)
                    {
                        out.println(Value.substring(start, j) + "<br>");
                        start = j;
                    }
                    if (start <= Value.length())
                    {
                        out.println(Value.substring(start));
                    }
                }
                else {
                    out.println(Value + "<br>");
                }
                out.println("</td></tr>");
            }
            out.println("</table><BR><BR>");
        }

        e = req.getParameterNames();
        if (e.hasMoreElements())
        {
            out.println("<h2>Servlet Parameters (Single Value style):</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
            while (e.hasMoreElements())
            {
                String name = (String) e.nextElement();
                out.println("<tr><td>" + escapeChar(name) + "</td><td>" + escapeChar(req.getParameter(name)) + "</td></tr>");
            }
            out.println("</table><BR><BR>");
        }

        e = req.getParameterNames();
        if (e.hasMoreElements())
        {
            out.println("<h2>Servlet Parameters (Multiple Value style):</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
            while (e.hasMoreElements())
            {
                String name = (String) e.nextElement();
                String vals[] = (String[]) req.getParameterValues(name);
                if (vals != null)
                {

                    out.print("<tr><td>" + escapeChar(name) + "</td><td>");
                    out.print(escapeChar(vals[0]));
                    for (int i = 1; i < vals.length; i++)
                        out.print(", " + escapeChar(vals[i]));
                    out.println("</td></tr>");
                }
            }
            out.println("</table><BR><BR>");
        }

        String cipherSuite = (String) req.getAttribute("javax.net.ssl.cipher_suite");
        if (cipherSuite != null)
        {
            X509Certificate certChain[] = (X509Certificate[]) req.getAttribute("javax.net.ssl.peer_certificates");

            out.println("<h2>HTTPS Information:</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
            out.println("<tr><td>Cipher Suite</td><td>" + escapeChar(cipherSuite) + "</td></tr>");

            if (certChain != null)
            {
                for (int i = 0; i < certChain.length; i++)
                {
                    out.println("client cert chain [" + i + "] = " + escapeChar(certChain[i].toString()));
                }
            }
            out.println("</table><BR><BR>");
        }

        Cookie[] cookies = req.getCookies();
        if (cookies != null && cookies.length > 0)
        {
            out.println("<H2>Client Cookies:</H2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
            String Value = null;
            for (int i = 0; i < cookies.length; i++)
            {
                out.println("<tr><td>" + escapeChar(cookies[i].getName()) + "</td>");
                out.println("<td>");
                Value = cookies[i].getValue();
                if (Value != null && Value.length() > 0) {
                    int start = 0;
                    if (Value.length() > 70)
                    {
                        for (int j = 70; j <= Value.length(); j = j + 70)
                        {
                            out.println(Value.substring(start, j) + "<br>");
                            start = j;
                        }
                        if (start <= Value.length())
                        {
                            out.println(Value.substring(start));
                        }
                    }
                    else {
                        out.println(escapeChar(Value));
                    }
                } else {
                    out.println("<tr><td>Cookie Value MISSING</td>");
                }
                out.println("</td></tr>");
            }
            out.println("</table><BR><BR>");
        }

        e = req.getAttributeNames();
        if (e.hasMoreElements())
        {
            out.println("<h2>Request Attributes:</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
            while (e.hasMoreElements())
            {
                String name = (String) e.nextElement();
                out.println("<tr><td>" + escapeChar(name) + "</td><td>" + escapeChar(req.getAttribute(name).toString()) + "</td></tr>");
            }
            out.println("</table><BR><BR>");
        }

        e = getServletContext().getAttributeNames();

        HttpSession session = req.getSession(false);
        if (session != null)
        {
            out.println("<h2>Session information:</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
            print(out, "Session ID", session.getId());
            print(out, "Last accessed time", new Date(session.getLastAccessedTime()).toString());
            print(out, "Creation time", new Date(session.getCreationTime()).toString());
            String mechanism = "unknown";
            if (req.isRequestedSessionIdFromCookie())
            {
                mechanism = "cookie";
            }
            else if (req.isRequestedSessionIdFromURL())
            {
                mechanism = "url-encoding";
            }
            print(out, "Session-tracking mechanism", mechanism);
            out.println("</table><BR><BR>");

            Enumeration vals = session.getAttributeNames();
            if (vals.hasMoreElements())
            {
                out.println("<h2>Session values</h2>");
                out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");

                while (vals.hasMoreElements())
                {
                    String name = (String) vals.nextElement();
                    String charname = escapeChar(name);
                    out.println("<tr><td>" + charname + "</td><td>" + escapeChar(session.getAttribute(name).toString()) + "</td></tr>");
                }
                out.println("</table><BR><BR>");
            }
        }

        out.println("<h2>SAML 2.0 Assertions:</h2>");
        out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
        out.println("<tr><td>");
        Saml20Token samlToken = getSAMLTokenFromRunAsSubject(out);
        if (samlToken != null) {
            out.println("<H2>SAMLToken (saml:Assertion only, since it's verified by acs handler)</H2>");
            //Element elem = samlToken.getDOM();	
            //traverse(elem, 0, out );
            String domString = samlToken.getSAMLAsString();
            out.println(domString);
        } else {
            out.println("<H2>No SAMLToken found</H2>");
        }
        out.println("</tr></td>");
        out.println("</table><BR><BR>");
        // end SAMLToken

        out.println("</table><BR><BR>");
        out.println("</body></html>");
    }

    /************************************************************************************************************************
     * Methods
     ************************************************************************************************************************/
    //prints SAML Response in a table with indentation
    private void showSAML(String SAML, PrintWriter out)
    {
        String seperator = null;
        out.println("<h2>SAML 2.0 Assertions:</h2>");
        out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
        out.println("<tr><td>");
        while (SAML.indexOf('\n') != -1)
        {
            seperator = SAML.substring(0, SAML.indexOf('\n'));
            out.print(seperator + "<br>");
            SAML = SAML.substring(SAML.indexOf('\n') + 1, SAML.length());
        }
        out.println(SAML);
        out.println("</tr></td>");
        out.println("</table><BR><BR>");
    }

    private void print(PrintWriter out, String name, String value) {
        out.println("<tr><td>" + name + "</td><td>"
                + (value == null ? "&lt;none&gt;" : escapeChar(value))
                + "</td></tr>");
    }

    /****************************************************************************************************************************************************/
    private void print(PrintWriter out, String name, int value) {
        out.print("<tr><td>" + name + "</td><td>");
        if (value == -1) {
            out.print("&lt;none&gt;");
        } else {
            out.print(value);
        }
        out.println("</td></tr>");
    }

    /****************************************************************************************************************************************************/
    private String escapeChar(String str) {
        char src[] = str.toCharArray();
        int len = src.length;
        for (int i = 0; i < src.length; i++) {
            switch (src[i]) {
            case '<': // to "&lt;"
                len += 3;
                break;
            case '>': // to "&gt;"
                len += 3;
                break;
            case '&': // to "&amp;"
                len += 4;
                break;
            }
        }
        char ret[] = new char[len];
        int j = 0;
        for (int i = 0; i < src.length; i++) {
            switch (src[i]) {
            case '<': // to "&lt;"
                ret[j++] = '&';
                ret[j++] = 'l';
                ret[j++] = 't';
                ret[j++] = ';';
                break;
            case '>': // to "&gt;"
                ret[j++] = '&';
                ret[j++] = 'g';
                ret[j++] = 't';
                ret[j++] = ';';
                break;
            case '&': // to "&amp;"
                ret[j++] = '&';
                ret[j++] = 'a';
                ret[j++] = 'm';
                ret[j++] = 'p';
                ret[j++] = ';';
                break;
            default:
                ret[j++] = src[i];
                break;
            }
        }
        return new String(ret);
    }

    /****************************************************************************************************************************************************/
    public Saml20Token getSAMLTokenFromRunAsSubject(PrintWriter out) {
        Saml20Token samlToken = null;
        try {
            Subject subject = WSSubject.getRunAsSubject();

            if (subject != null) {
                java.util.Set tokens = subject
                        .getPrivateCredentials(Saml20Token.class);

                if (tokens != null && tokens.size() > 0) {
                    java.util.Iterator samlIterator = tokens.iterator();

                    while (samlIterator.hasNext()) {
                        Object cred = samlIterator.next();

                        if (cred != null && cred instanceof Saml20Token) {
                            samlToken = (Saml20Token) cred;
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return samlToken;
    }

    /****************************************************************************************************************************************************/
    private static String[] getGroups() {
        String[] grps = null;
        try {
            WSCredential credential = null;
            Subject subject = null;
            ArrayList groups = new ArrayList();
            try {
                subject = WSSubject.getCallerSubject();
            } catch (Exception ex) {
            }
            if (subject != null) {
                java.util.Set publicCreds = subject.getPublicCredentials();

                if (publicCreds != null && publicCreds.size() > 0) {
                    java.util.Iterator publicCredIterator = publicCreds
                            .iterator();

                    while (publicCredIterator.hasNext()) {
                        Object cred = publicCredIterator.next();

                        if (cred != null && cred instanceof WSCredential) {
                            credential = (WSCredential) cred;
                        }
                    }
                }
            }

            if (credential != null) {
                groups = credential.getGroupIds();
            }
            if (groups != null && groups.size() > 0) {
                grps = new String[groups.size()];
                groups.toArray(grps);
            }
        } catch (Exception e) {
        }
        return grps;
    }

    /****************************************************************************************************************************************************/
    private static String getRealm() {
        String realm = null;
        try {
            WSCredential name = null;
            Subject subject = null;
            try {
                subject = WSSubject.getCallerSubject();
            } catch (Exception ex) {
            }
            if (subject != null) {
                java.util.Set publicCreds = subject.getPublicCredentials();

                if (publicCreds != null && publicCreds.size() > 0) {
                    java.util.Iterator publicCredIterator = publicCreds
                            .iterator();

                    while (publicCredIterator.hasNext()) {
                        Object cred = publicCredIterator.next();

                        if (cred != null && cred instanceof WSCredential) {
                            name = (WSCredential) cred;
                        }
                    }
                }
            }

            if (name != null) {
                realm = name.getRealmName();
            }
        } catch (Exception e) {
        }
        return realm;
    }

    /****************************************************************************************************************************************************/
    public static String forXML(String xml2String) {
        final StringBuilder xml = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(
                xml2String);
        char letter = iterator.current();
        while (letter != CharacterIterator.DONE) {
            if (letter == '<') {
                xml.append("&lt;");
            } else if (letter == '>') {
                xml.append("&gt;");
            } else if (letter == '\"') {
                xml.append("&quot;");
            } else if ((letter == '\'') && (letter != '\n')) {
                xml.append("&#039;");
            } else if (letter == '&') {
                xml.append("&amp;");
            } else if (letter == '\t') {
                xml.append("&nbsp;");
            } else {
                xml.append(letter);
            }
            letter = iterator.next();
        }
        return xml.toString();
    }

    /****************************************************************************************************************************************************/
    public static String xmlIndent(String xmlString, PrintWriter out) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int indent = 0, i = 0;
        char bytes[] = xmlString.toCharArray();
        while (i < bytes.length) {
            if ((bytes[i] == '<') && (bytes[i + 1] == '/')) {
                stream.write('\n');
                indentString(stream, --indent);
            } else if (bytes[i] == '<') {
                if (i > 0) {
                    stream.write('\n');
                }
                indentString(stream, indent++);
            } else if (bytes[i] == '/' && bytes[i + 1] == '>') {
                indent--;
            }
            stream.write(bytes[i++]);
        }
        return stream.toString();
    }

    /****************************************************************************************************************************************************/
    private static void indentString(ByteArrayOutputStream stream, int indent) {
        for (int i = 0; i < indent; i++) {
            stream.write('\t');
            stream.write('\t');
            stream.write('\t');
            stream.write('\t');
            stream.write('\t');
            stream.write('\t');
        }
    }

    /*************************************
     * print out from XML Node
     */
    static char[] _cIndent = new char[100];
    static int _iIndent = 3;

    private static void traverse(Node node, int iLevel, PrintWriter out)
    {
        int iIndent = iLevel * _iIndent;
        printIndent(iIndent, out);

        String strN = node.getNodeName();
        out.print("&lt;" + strN);

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
                    out.print("&nbsp;" + str +
                            "=&quot;" + ((Attr) attr).getValue() + "&quot");
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
                    out.print("<BR>");
                    printIndent(iAttribIndent, out);
                    String str = ((Attr) attr).getName();
                    out.print(str +
                            "=&quot;" + ((Attr) attr).getValue() + "&quot;");
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
                    out.print("&gt;<BR>");
                }
                traverse(item, iLevel + 1, out);
            }
            else
            {
                if (item instanceof Attr)
                {
                    Attr attr = (Attr) item;
                    out.print("???ERR " + attr.getName() + "=&quot;" + attr.getValue() + "&quot;");
                }
                else if (item instanceof Text)
                {
                    if (!bClose)
                    {
                        bClose = true;
                        out.print("&gt;<BR>");
                    }
                    printText((Text) item, iIndent, out);
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
                            out.print("/&gt;<BR>");
                        }
                        else
                        {
                            bClose = true;
                            out.print("&gt;<BR>");
                        }
                    }
                    // Skip all the text
                    printComment((Comment) item, iIndent, out);
                }
                else
                {
                    out.print("???  " + item.getClass().getName() +
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
                printIndent(iIndent, out);
                out.print("&lt;/" + strN + "&gt<BR>");
            }
            else
            {
                out.print("/&gt;<BR>");
            }
        }
    }

    private static void printIndent(int iIndent, PrintWriter out)
    {
        for (int iI = 0; iI < iIndent; iI++)
        {
            out.print("&nbsp;");
        }

    }

    private static void printComment(Comment comment, int iIndent, PrintWriter out)
    {
        iIndent += _iIndent;
        printIndent(iIndent, out);
        out.print("&lt;!&ndash;&ndash;<BR>");

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
            printIndent(iIndent + 4, out);
            out.print(token + "<BR>");
        }

        printIndent(iIndent, out);
        out.print("--&gt;<br>");
    }

    private static void printText(Text comment, int iIndent, PrintWriter out)
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
            printIndent(iIndent, out);
            out.print(token + "<br>");
        }
    }

}