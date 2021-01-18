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
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.openidconnect.PropagationHelper;
import com.ibm.websphere.security.openidconnect.token.IdToken;
import com.ibm.wsspi.security.oauth20.token.WSOAuth20Token;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * Form Login Servlet
 */
public class manual_IntrospectServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    String servletName = "IntrospectServlet";

    boolean bCallApi = true;

    // getInvocationSubject for RunAs tests
    Subject runAsSubject = null;

    public manual_IntrospectServlet() {
    }

    public void updateServletName(String servletName) {
        this.servletName = servletName;
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if ("CUSTOM".equalsIgnoreCase(req.getMethod())) {
            doCustom(req, res);
        } else {
            super.service(req, res);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest("GET", req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest("POST", req, resp);
    }

    public void doCustom(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest("CUSTOM", req, resp);
    }

    /**
     * Common logic to handle any of the various requests this servlet supports.
     * The actual business logic can be customized by overriding performTask.
     * 
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     * @throws WSSecurityException
     */
    public void handleRequest(String type, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        writer.println("HandleRequest ServletName: " + servletName);
        writer.println("HandleRequest Request type: " + type);

        try {
            runAsSubject = WSSubject.getRunAsSubject();
        } catch (WSSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        writer.println("HandleRequest RunAs subject: " + runAsSubject);

        StringBuffer sb = new StringBuffer();
        try {
            performInstrospect(req, resp, sb);
        } catch (Throwable t) {
            t.printStackTrace(writer);
        }

        writer.write(sb.toString());
        writer.flush();
        writer.close();
    }

    public void performInstrospect(HttpServletRequest req,
            HttpServletResponse resp, StringBuffer sb) {

        sb.append("\n\nPreparing Introspect Data:");
        String clientId = req.getParameter("clientId");
        String clientSecret = req.getParameter("clientSecret");
        sb.append("\n***clientID:" + clientId + " clientSecret:" + clientSecret);
        sb.append("\n***Calling API PropagationHelp:" + bCallApi);
        String encodedOpUrl = req.getParameter("opUrl");
        String opUrl = encodedOpUrl;
        try {
            opUrl = URLDecoder.decode(encodedOpUrl, "UTF8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sb.append("\n***encodedOpUrl:" + encodedOpUrl + "\n  opUrl:" + opUrl);

        Hashtable<String, Object> hashTable = getProperties(runAsSubject, sb);
        // debugHashtable( hashTable, sb);
        String accessToken = bCallApi ? PropagationHelper.getAccessToken() : (String) hashTable.get("access_token");
        String tokenType = bCallApi ? PropagationHelper.getAccessTokenType() : (String) hashTable.get("token_type");
        long expiresAt = bCallApi ? PropagationHelper.getAccessTokenExpirationTime() : (Long) hashTable.get("expires_in");
        sb.append("\n***access_token:" + accessToken + " token_type:" + tokenType + " expires_at/in:" + expiresAt + "***");
        if (bCallApi) {
            IdToken idToken = PropagationHelper.getIdToken();
            sb.append("\n***IdToken-JsonString:" + idToken.getAllClaimsAsJson());
        }

        manual_IntrospectRequester introspectRequester = new manual_IntrospectRequester(req, resp,
                clientId, clientSecret,
                opUrl,
                accessToken, tokenType);
        try {
            introspectRequester.submitIntrospect(sb);
        } catch (WebTrustAssociationFailedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            sb.append("\nGet Exception:" + e);
        }
    }

    public void debugHashtable(Hashtable hashTable, StringBuffer sb) {
        int iCnt = 0;
        Set<Entry<String, Object>> entries = hashTable.entrySet();
        for (Entry<String, Object> entry : entries) {
            iCnt++;
            String key = entry.getKey();
            Object value = entry.getValue();
            sb.append("\nentry(" + iCnt + ") key:" + key + " value:" + value);
        }
    }

    public Hashtable<String, Object> getProperties(Subject subject, StringBuffer sb) {
        Hashtable<String, Object> hashTable = new Hashtable<String, Object>();
        Set<Object> publicCredentials = subject.getPublicCredentials();
        int iCnt = 0;
        for (Object credentialObj : publicCredentials) {
            iCnt++;
            sb.append("\n  **publicCredential(" + iCnt + ") class:" + credentialObj.getClass().getName());
            sb.append("\n  **   content:" + credentialObj);
            if (credentialObj instanceof Map) {
                hashTable.putAll((Map) credentialObj);
            }
        }
        Set<Object> privCredentials = subject.getPrivateCredentials();
        for (Object credentialObj : privCredentials) {
            iCnt++;
            sb.append("\n  **privateCredential(" + iCnt + ") class:" + credentialObj.getClass().getName());
            sb.append("\n  **   content:" + credentialObj);
            if (credentialObj instanceof Map) {
                hashTable.putAll((Map) credentialObj);
            }
        }
        return hashTable;
    }

    /**
     * Gets the SSO token from the subject.
     * 
     * @param subject
     *            {@code null} is not supported.
     * @return
     */
    public SingleSignonToken getSSOToken(StringBuffer sb) {
        Subject subject = runAsSubject;
        SingleSignonToken ssoToken = null;
        Set<SingleSignonToken> ssoTokens = subject.getPrivateCredentials(SingleSignonToken.class);
        Iterator<SingleSignonToken> ssoTokensIterator = ssoTokens.iterator();
        int iCnt = 0;
        while (ssoTokensIterator.hasNext()) {
            ssoToken = ssoTokensIterator.next();
            iCnt++;
            sb.append("\nSSOToken(" + iCnt + ") class:" + ssoToken.getClass().getName());
            sb.append("\n        (" + iCnt + ")      :" + ssoToken);
            sb.append("\n        (" + iCnt + ") name :" + ssoToken.getName() +
                    // " OAuth20Token:" + (ssoToken instanceof OAuth20Token) +
                    // " Saml20Token:" + (ssoToken instanceof Saml20Token) +
                    " WSOAuth20Token:" + (ssoToken instanceof WSOAuth20Token) +
                    "");
        }
        return ssoToken;
    }

    /**
     * Gets the SSO token from the subject.
     * 
     * @param subject
     *            {@code null} is not supported.
     * @return
     */
    public WSOAuth20Token getWSOAuth20Token(StringBuffer sb) {
        Subject subject = runAsSubject;
        WSOAuth20Token wsOAuth20Token = null;
        Set<WSOAuth20Token> wsOAuth20Tokens = subject.getPrivateCredentials(WSOAuth20Token.class);
        Iterator<WSOAuth20Token> wsOAuth20TokensIterator = wsOAuth20Tokens.iterator();
        int iCnt = 0;
        while (wsOAuth20TokensIterator.hasNext()) {
            wsOAuth20Token = wsOAuth20TokensIterator.next();
            iCnt++;
            sb.append("\nWSOAuth20Token(" + iCnt + ") class:" + wsOAuth20Token.getClass().getName());
            sb.append("\n        (" + iCnt + ")      :" + wsOAuth20Token);
            sb.append("\n        (" + iCnt + ") user :" + wsOAuth20Token.getUser() +
                    "\n        (" + iCnt + ") tokenString:" + wsOAuth20Token.getTokenString() +
                    "\n        (" + iCnt + ") expirationTime:" + wsOAuth20Token.getExpirationTime() +
                    "\n        (" + iCnt + ") cacheKey:" + wsOAuth20Token.getCacheKey() +
                    "\n        (" + iCnt + ") scpes:" + arrayToString(wsOAuth20Token.getScope()) +
                    "");
        }
        return wsOAuth20Token;
    }

    String arrayToString(String[] scopes) {
        String result = "";
        if (scopes == null) {
            result = "null";
        } else {
            int iCnt = 0;
            for (String scope : scopes) {
                if (iCnt > 0) {
                    result = result + ", ";
                }
                iCnt++;
                result = result + scope;
            }
        }
        return result;
    }

}
