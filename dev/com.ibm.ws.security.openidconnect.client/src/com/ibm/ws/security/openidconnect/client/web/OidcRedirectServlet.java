/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.google.gson.JsonObject;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.oauth20.web.WebUtils;
import com.ibm.ws.security.openidconnect.client.internal.OidcClientConfigImpl;
import com.ibm.ws.security.openidconnect.client.internal.OidcClientImpl;
import com.ibm.ws.security.openidconnect.client.internal.TraceConstants;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.HashUtils;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil;
import com.ibm.ws.security.openidconnect.clients.common.OidcUtil;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.webcontainer.security.openidconnect.OidcClient;

/**
 * Servlet for The OpenID Connect client / relying party
 */
public class OidcRedirectServlet extends HttpServlet {

    private static TraceComponent tc = Tr.register(OidcRedirectServlet.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    private static final long serialVersionUID = 1L;
    public static final String METHOD_GET = "GET";

    private transient ServletContext servletContext = null;
    private transient BundleContext bundleContext = null;
    private transient ServiceReference<OidcClient> OidcClientRef = null;
    private transient OidcClient oidcClient = null;

    public static OidcClientImpl activatedOidcClientImpl = null; // this will be initialized when the feature starts

    /**
     * @param activatedOidcClientImpl
     *            the activatedOidcClientImpl to set
     *            (called by the oidcClientImpl on activation)
     */
    public static void setActivatedOidcClientImpl(OidcClientImpl activatedOidcClientImpl) {
        OidcRedirectServlet.activatedOidcClientImpl = activatedOidcClientImpl;
    }

    @Override
    public void init() {
        servletContext = getServletContext();
        bundleContext = (BundleContext) servletContext.getAttribute("osgi-bundlecontext");
        OidcClientRef = bundleContext.getServiceReference(OidcClient.class);
    }

    private synchronized OidcClient getOidcClient() throws ServletException {

        if (OidcClientRef == null) {
            throw new ServletException();
        } else {
            oidcClient = bundleContext.getService(OidcClientRef);
        }
        return oidcClient;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        if (request.getParameter(Constants.STATE) == null && METHOD_GET.equalsIgnoreCase(request.getMethod())) {
            // discourage inappropriate snooping around with a browser.
            if (!getOidcClient().isValidRedirectUrl(request)) {
                String errorMsg = Tr.formatMessage(tc, "OIDC_CLIENT_BAD_GET_REQUEST", request.getRequestURL()); // CWWKS1748E
                Tr.error(tc, errorMsg);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); //will cause PublicFacingErrorServlet to run
                return;
            }
            getTokenFromFragment(request, response); //using javascript, send the browser to the redirect url registered for the client.
        } else { // state has been set by OP, continue processing.
            this.doPost(request, response);
        }
    }

    /*
     * this method gets entered twice during routine processing.
     * First time, browser has been redirected to OP and is coming back with auth code or id token.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String state = request.getParameter(Constants.STATE);
        if (state == null || state.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "OIDC_CLIENT_BAD_REQUEST_NO_STATE", request.getRequestURL()); // CWWKS1749E
            Tr.error(tc, errorMsg);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); //will cause PublicFacingErrorServlet to run
            return;
        }
        //  when OIDCClientAuthenticatorUtil.doClientSideRedirect initially redirected the browser,
        //  this cookie was set to hold the original URL.
        //  Now it's time to get it back.
        String cookieName = ClientConstants.WAS_REQ_URL_OIDC + HashUtils.getStrHashCode(state);
        String requestUrl = OidcClientUtil.getReferrerURLCookieHandler().getReferrerURLFromCookies(request, cookieName);
        // 240540
        //CookieHelper.clearCookie(request, response, cookieName, cookies); //clear the WAS_REQ_URL_OIDC cookie
        OidcClientUtil.invalidateReferrerURLCookie(request, response, cookieName);
        if (tc.isDebugEnabled() && requestUrl != null) {
            Tr.debug(tc, "requestUrl: " + requestUrl);
        }

        if (requestUrl == null || requestUrl.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "OIDC_CLIENT_BAD_REQUEST_NO_COOKIE", request.getRequestURL()); // CWWKS1520E
            Tr.error(tc, errorMsg);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); //will cause PublicFacingErrorServlet to run
            return;
        }

        if (isRedirectionUrlValid(request, requestUrl) == false) {
            String errorMsg = Tr.formatMessage(tc, "OIDC_CLIENT_BAD_REQUEST_MALFORMED_URL_IN_COOKIE", request.getRequestURL(), (new URL(requestUrl)).getHost()); // CWWKS152XE
            Tr.error(tc, errorMsg);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); //will cause PublicFacingErrorServlet to run
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "requestURL is not null or empty");
        }

        String clientId = null;
        int iPrefix = request.getRequestURI().lastIndexOf("/");
        if (iPrefix > -1) {
            clientId = request.getRequestURI().substring(iPrefix + 1);
        }
        String oidcClientId = null;
        if (state.length() > OidcUtil.STATEVALUE_LENGTH) {
            oidcClientId = state.substring(OidcUtil.STATEVALUE_LENGTH);
        }

        String code = request.getParameter(Constants.CODE);
        String idToken = request.getParameter(Constants.ID_TOKEN);

        if (code != null || idToken != null) {
            sendToRedirectUrl(request, response, requestUrl, state, clientId, oidcClientId, idToken);
        } else {
            sendError(request, response);
        }
    }

    public boolean isRedirectionUrlValid(HttpServletRequest request, @Sensitive String requestUrl) {
        return OidcClientUtil.isReferrerHostValid(request, requestUrl);
    }

    // todo: converge w social in oidcutils class?
    private void sendToRedirectUrl(HttpServletRequest request, HttpServletResponse response, String requestUrl, String state, String clientId, String oidcClientId, String id_token) throws IOException {
        String sessionState = request.getParameter(Constants.SESSION_STATE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Request info: state: " + state + " session_state: " + sessionState);
        }
        boolean isHttpsRequest = requestUrl.toLowerCase().startsWith("https");
        OidcClientConfigImpl clientCfg = activatedOidcClientImpl.getOidcClientConfig(request, clientId);
        // store all request params in digested cookie
        new OidcClientUtil().setCookieForRequestParameter(request, response, clientId, state, isHttpsRequest, clientCfg);

        if ((oidcClientId != null && !oidcClientId.isEmpty()) || id_token != null) {
            postToWASReqURL(request, response, requestUrl, oidcClientId); //  implicit flow???
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "OIDC _SSO RP Servlet redirecting to [" + requestUrl + "]");
            }
            response.sendRedirect(requestUrl); // send back to protected resource
        }
    }

    private void sendError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String error = request.getParameter(Constants.ERROR);
        if (error != null && OAuth20Exception.ACCESS_DENIED.equals(error)) {
            // User likely canceled the request
            String errorMsg = Tr.formatMessage(tc, "OAUTH_REQUEST_ACCESS_DENIED");
            Tr.error(tc, errorMsg);
            errorMsg = Tr.formatMessage(tc, "OAUTH_REQUEST_ACCESS_DENIED_ENDUSER");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, errorMsg);
            return;
        }

        StringBuilder query = new StringBuilder();
        if (error != null && OAuth20Exception.INVALID_SCOPE.equals(error)) {
            query.append(Constants.ERROR + "=" + OAuth20Exception.INVALID_SCOPE);
        } else {
            query.append(Constants.ERROR + "=" + OAuth20Exception.ACCESS_DENIED);
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, query.toString());
    }

    // todo: call oidcClientUtil version of this method instead.
    void setCookieForRequestParameter(HttpServletRequest request, HttpServletResponse response, String id, String state, boolean isHttpsRequest) {
        OidcClientConfigImpl clientCfg = activatedOidcClientImpl.getOidcClientConfig(request, id);
        Map<String, String[]> map = request.getParameterMap(); // at least it gets state parameter
        JsonObject jsonObject = new JsonObject();
        Set<Map.Entry<String, String[]>> entries = map.entrySet();
        for (Map.Entry<String, String[]> entry : entries) {
            String key = entry.getKey();
            if (Constants.ACCESS_TOKEN.equals(key) || Constants.ID_TOKEN.equals(key)) {
                continue;
            }
            String[] strs = entry.getValue();
            if (strs != null && strs.length > 0) {
                jsonObject.addProperty(key, strs[0]);
            }
        }
        String requestParameters = jsonObject.toString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "requestParameters:" + requestParameters);
        }
        // digest with the client_secret value
        String digestValue = HashUtils.digest(requestParameters + clientCfg.getClientSecret());
        String hashReqParams = requestParameters + digestValue;
        String encodedReqParams = null;
        try {
            encodedReqParams = Base64Coder.toString(Base64Coder.base64Encode(hashReqParams.getBytes(ClientConstants.CHARSET)));
        } catch (UnsupportedEncodingException e) {
            //This should not happen, we are using UTF-8
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "get unexpected exception", e);
            }
        }

        Cookie c = OidcClientUtil.createCookie(ClientConstants.WAS_OIDC_CODE, encodedReqParams, request);
        if (clientCfg.isHttpsRequired() && isHttpsRequest) {
            c.setSecure(true);
        }
        response.addCookie(c);
    }

    /**
     *
     * @param request
     * @param reqsponse
     * @throws IOException
     */
    public void getTokenFromFragment(HttpServletRequest request, HttpServletResponse response) throws IOException {

        StringBuffer sb = new StringBuffer();

        sb.append("<HTML xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\"><HEAD><title>Submit This Form</title></HEAD>");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
        sb.append("<BODY onload=\"javascript:document.forms[0].submit()\">");

        String requestUrl = getOidcRedirectUrl(request);
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
            Tr.debug(tc, "OIDC _SSO RP redirecting (\"" + "POST" + "\")\n" + sb.toString());
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

    private String getOidcRedirectUrl(HttpServletRequest request) {

        String redirectUrl = null;
        if (activatedOidcClientImpl != null) {

            String clientId = null;
            int iPrefix = request.getRequestURI().lastIndexOf("/");
            if (iPrefix > -1) {
                clientId = request.getRequestURI().substring(iPrefix + 1);
            }
            if (clientId != null && !clientId.isEmpty()) {
                OidcClientConfigImpl clientCfg = activatedOidcClientImpl.getOidcClientConfig(request, clientId);
                if (clientCfg != null) {
                    redirectUrl = clientCfg.getRedirectUrlFromServerToClient();
                }
            }
        }
        return redirectUrl;
    }

    protected void postToWASReqURL(HttpServletRequest req, HttpServletResponse resp, String requestUrl, String oidcClientId) throws IOException {
        String access_token = req.getParameter(Constants.ACCESS_TOKEN);
        String id_token = req.getParameter(Constants.ID_TOKEN);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "id_token:" + id_token);
        }

        StringBuffer sb = new StringBuffer("");
        // HTTP 1.1.
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private, max-age=0");
        // HTTP 1.0.
        resp.setHeader("Pragma", "no-cache");
        // Proxies.
        resp.setDateHeader("Expires", 0);
        resp.setContentType("text/html");

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
            Tr.debug(tc, "OIDC _SSO RP redirecting\n" +
                    sb.toString());
        }

        PrintWriter out = resp.getWriter();
        out.println(sb.toString());
        out.flush();
    }

}