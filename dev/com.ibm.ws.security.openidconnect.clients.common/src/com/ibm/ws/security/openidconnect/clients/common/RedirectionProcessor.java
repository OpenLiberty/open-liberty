/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.web.WebUtils;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.webcontainer.security.CookieHelper;

/**
 * Processes the End-User redirection to the Client by the OP.
 */
public class RedirectionProcessor {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final TraceComponent tc;

    public RedirectionProcessor(HttpServletRequest request, HttpServletResponse response, TraceComponent tc) {
        this.request = request;
        this.response = response;
        this.tc = tc;
    }

    public void processRedirection(RedirectionEntry redirectionEntry) throws IOException {
        String state = request.getParameter(Constants.STATE);

        if (state == null && "GET".equalsIgnoreCase(request.getMethod())) {
            // The validity of the redirect URL must be done by the caller
            getTokenFromFragment(redirectionEntry);
        } else {
            continueWithRedirection(redirectionEntry, state);
        }
    }

    private void getTokenFromFragment(RedirectionEntry redirectionEntry) throws IOException {
        ConvergedClientConfig clientConfig = redirectionEntry.getConvergedClientConfig(request, getClientId());
        StringBuffer sb = new StringBuffer();

        sb.append("<HTML xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\"><HEAD><title>Submit This Form</title></HEAD>");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
        sb.append("<BODY onload=\"javascript:document.forms[0].submit()\">");

        String requestUrl = clientConfig.getRedirectUrlFromServerToClient();
        if (requestUrl == null) {
            requestUrl = request.getRequestURL().toString();
        }
        requestUrl = WebUtils.htmlEncode(requestUrl);

        sb.append("<FORM name=\"redirectform\" id=\"redirectform\" action=\"");
        sb.append(requestUrl);
        sb.append("\" method=\"POST\">");

        sb.append("<script type=\"text/javascript\" language=\"javascript\">");
        sb.append("function createInput(name, value) {");
        sb.append("var input = document.createElement(\"input\");");
        sb.append("input.setAttribute(\"type\", \"hidden\");");
        sb.append("input.setAttribute(\"name\", name);");
        sb.append("input.setAttribute(\"value\", value);");
        sb.append("return input;");
        sb.append("}");

        sb.append("var form=document.forms[0];");
        sb.append("var state=null;");
        sb.append("var params = {}, postBody = location.hash.substring(1),");
        sb.append("regex = /([^&=]+)=([^&]*)/g, m;");
        sb.append("while (m = regex.exec(postBody)){");
        sb.append("form.appendChild( createInput(decodeURIComponent(m[1]), decodeURIComponent(m[2])));");
        sb.append("}");

        sb.append("</script>");
        sb.append("<button type=\"submit\" name=\"redirectform\">Process Form Post</button></FORM></BODY></HTML>");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "... expect to be redirected by the browser (\"" + "POST" + "\")\n" + sb.toString());
        }

        // HTTP 1.1.
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private, max-age=0");
        // HTTP 1.0.
        response.setHeader("Pragma", "no-cache");
        // Proxies.
        response.setDateHeader("Expires", 0);
        response.setContentType("text/html; charset=UTF-8");

        PrintWriter out = response.getWriter();
        out.println(sb.toString());
        out.flush();
    }

    private void continueWithRedirection(RedirectionEntry redirectionEntry, String state) throws IOException {
        if (state == null || state.isEmpty()) {
            redirectionEntry.handleNoState(request, response);
            return;
        }

        String requestUrl = getOriginalRequestUrl(state);

        if (requestUrl == null || requestUrl.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "OIDC_CLIENT_BAD_REQUEST_NO_COOKIE", request.getRequestURL()); // CWWKS1750E
            Tr.error(tc, errorMsg);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        String clientId = getClientId();
        String oidcClientId = getOidcClientId(state);
        String code = request.getParameter(Constants.CODE);
        String idToken = request.getParameter(Constants.ID_TOKEN);

        if (code != null || idToken != null) {
            ConvergedClientConfig clientConfig = redirectionEntry.getConvergedClientConfig(request, clientId);
            sendToOriginalRequestUrl(requestUrl, state, clientId, oidcClientId, idToken, clientConfig);
        } else {
            redirectionEntry.sendError(request, response);
        }
    }

    private String getOriginalRequestUrl(String state) {
        String cookieName = ClientConstants.WAS_REQ_URL_OIDC + HashUtils.getStrHashCode(state);
        Cookie[] cookies = request.getCookies();
        String requestUrl = CookieHelper.getCookieValue(cookies, cookieName);
        OidcClientUtil.invalidateReferrerURLCookie(request, response, cookieName);
        return requestUrl;
    }

    private String getClientId() {
        String clientId = null;
        int iPrefix = request.getRequestURI().lastIndexOf("/");
        if (iPrefix > -1) {
            clientId = request.getRequestURI().substring(iPrefix + 1);
        }
        return clientId;
    }

    private String getOidcClientId(String state) {
        String oidcClientId = null;
        if (state.length() > OidcUtil.STATEVALUE_LENGTH) {
            oidcClientId = state.substring(OidcUtil.STATEVALUE_LENGTH);
        }
        return oidcClientId;
    }

    private void sendToOriginalRequestUrl(String requestUrl, String state, String clientId, String oidcClientId, String id_token, ConvergedClientConfig clientCfg) throws IOException {
        String sessionState = request.getParameter(Constants.SESSION_STATE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Request info: state: " + state + " session_state: " + sessionState);
        }
        boolean isHttpsRequest = requestUrl.toLowerCase().startsWith("https");
        new OidcClientUtil().setCookieForRequestParameter(request, response, clientId, state, isHttpsRequest, clientCfg);
        if ((oidcClientId != null && !oidcClientId.isEmpty()) || id_token != null) {
            postToWASReqURLForImplicitFlow(requestUrl, oidcClientId);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "... expect to be redirected by the browser:" + requestUrl);
            }
            response.sendRedirect(requestUrl);
        }
    }

    private void postToWASReqURLForImplicitFlow(String requestUrl, String oidcClientId) throws IOException {
        String access_token = request.getParameter(Constants.ACCESS_TOKEN);
        String id_token = request.getParameter(Constants.ID_TOKEN);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "id_token:" + id_token);
        }

        StringBuffer sb = new StringBuffer("");
        // HTTP 1.1.
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private, max-age=0");
        // HTTP 1.0.
        response.setHeader("Pragma", "no-cache");
        // Proxies.
        response.setDateHeader("Expires", 0);
        response.setContentType("text/html");

        sb.append("<HTML xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
        sb.append("<HEAD>");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>");
        sb.append("<meta http-equiv=\"Cache-Control\" content=\"no-cache, no-store, must-revalidate\"/>");
        sb.append("<meta http-equiv=\"Pragma\" content=\"no-cache\"/>");
        sb.append("<meta http-equiv=\"Expires\" content=\"0\"/>");
        sb.append("</HEAD>");
        sb.append("<BODY onload=\"document.forms[0].submit()\">");
        sb.append("<FORM name=\"redirectform\" id=\"redirectform\" action=\"");
        sb.append(WebUtils.htmlEncode(requestUrl));
        sb.append("\" method=\"POST\"><div>");

        // add oidc_client
        if (oidcClientId != null) {
            sb.append("<input type=\"hidden\" name=\"oidc_client\" value=\"" + WebUtils.htmlEncode(oidcClientId) + "\"/>");
        }
        if (access_token != null) {
            sb.append("<input type=\"hidden\" name=\"access_token\" value=\"" + WebUtils.htmlEncode(access_token) + "\"/>");
        }
        if (id_token != null) {
            sb.append("<input type=\"hidden\" name=\"id_token\" value=\"" + WebUtils.htmlEncode(id_token) + "\"/>");
        }
        sb.append("</div>");
        sb.append("<noscript><div>");
        sb.append("<button type=\"submit\" name=\"redirectform\">Process request</button>");
        sb.append("</div></noscript>");
        sb.append("</FORM></BODY></HTML>");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "... expect to be redirected by the browser\n" +
                    sb.toString());
        }

        PrintWriter out = response.getWriter();
        out.println(sb.toString());
        out.flush();
    }

}
