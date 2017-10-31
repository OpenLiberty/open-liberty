/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.beans;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Priority;
import javax.el.ELProcessor;
import javax.enterprise.inject.Instance;
<<<<<<< HEAD
import javax.enterprise.inject.spi.CDI;
=======
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
>>>>>>> support multiple modules
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.security.auth.message.MessageInfo;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.genericbnf.PasswordNullifier;
import com.ibm.ws.security.jaspi.JaspiConstants;
import com.ibm.ws.security.javaeesec.CDIHelper;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.metadata.FormLoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.FormLoginConfigurationImpl;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.LoginConfigurationImpl;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.util.WebConfigUtils;

/**
 *
 */
@LoginToContinue
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 220)
public class LoginToContinueInterceptor {
    private static final String METHOD_TO_INTERCEPT = "validateRequest";
    private static final String CUSTOM_FORM_CLASS = "com.ibm.ws.security.javaeesec.cdi.beans.CustomFormAuthenticationMechanism";
    private static final TraceComponent tc = Tr.register(LoginToContinueInterceptor.class);
    @Inject
    Instance<ModulePropertiesProvider> mppInstance;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {

        Object result = null;
        if (isMethodToIntercept(ic)) {
            // The method signature of validateRequest is as follows:
            // public AuthenticationStatus validateRequest(HttpServletRequest request,
            //                                    HttpServletResponse response,
            //                                    HttpMessageContext httpMessageContext) throws AuthenticationException {

            ModulePropertiesProvider mpp = null;
            if (mppInstance != null && !mppInstance.isUnsatisfied() && !mppInstance.isAmbiguous()) {
                mpp = mppInstance.get();
                if (mpp != null) {
                    result = ic.proceed();
                    Object[] params = ic.getParameters();
                    HttpServletRequest req = (HttpServletRequest) params[0];
                    HttpServletResponse res = (HttpServletResponse) params[1];
                    if (result.equals(AuthenticationStatus.SEND_CONTINUE)) {
                        // need to redirect.
                        HttpMessageContext mc = (HttpMessageContext) params[2];
                        result = gotoLoginPage(mpp.getAuthMechProperties(ic.getTarget().getClass().getSuperclass()), req, res, mc);
                    } else if (result.equals(AuthenticationStatus.SUCCESS)) {
                        boolean isCustom = isCustomForm(ic);
                        // redirect to the original url.
                        postLoginProcess(req, res, isCustom);
                    }
                } else {
                    // return error since properties are not available.
                    Tr.error(tc, "JAVAEESEC_CDI_ERROR_LOGIN_TO_CONTINUE_PROPERTIES_DOES_NOT_EXIST");
                    result = AuthenticationStatus.SEND_FAILURE;
                }
            } else {
                Tr.error(tc, "JAVAEESEC_CDI_ERROR_LOGIN_TO_CONTINUE_PROPERTIES_DOES_NOT_EXIST");
                result = AuthenticationStatus.SEND_FAILURE;
            }
        } else {
            result = ic.proceed();
        }
        return result;
    }

    protected AuthenticationStatus gotoLoginPage(Properties props, HttpServletRequest req, HttpServletResponse res, HttpMessageContext httpMessageContext) throws IOException {
        String loginPage = (String) props.get(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE);
        String errorPage = (String) props.get(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE);
        boolean useForwardToLogin = getUseForwardToLogin(props);
        AuthenticationStatus status = AuthenticationStatus.SEND_CONTINUE;
        // update security metadata.
        MessageInfo msgInfo = httpMessageContext.getMessageInfo();
        WebRequest webRequest = (WebRequest) msgInfo.getMap().get(JaspiConstants.SECURITY_WEB_REQUEST);
        updateFormLoginConfiguration(loginPage, errorPage, webRequest.getSecurityMetadata());
        // set wasrequrl cookie and postparam cookie.
        setCookies(req, res);

        if (useForwardToLogin) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The request will be forwarded to the login page.");
            }
            RequestDispatcher rd = req.getRequestDispatcher(loginPage);
            try {
                rd.forward(req, res);
            } catch (Exception e) {
                status = AuthenticationStatus.SEND_FAILURE;
            }
        } else {
            res.setStatus(HttpServletResponse.SC_FOUND);
            String loginUrl = getLoginUrl(req, loginPage);
            res.sendRedirect(res.encodeURL(loginUrl));
        }
        return status;
    }

    private boolean getUseForwardToLogin(Properties props) {
        String useForwardExpression = (String) props.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION);
        Boolean result = true;
        if (useForwardExpression != null && !useForwardExpression.isEmpty()) {
            ELProcessor elProcessor = getELProcessorWithAppModuleBeanManagerELResolver();
            result = (Boolean) elProcessor.eval(useForwardExpression);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "result of elProcessor.eval : " + result);
            
        } else {
            Boolean value = (Boolean) props.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN);
            if (value != null) {
                result = value;
            }
        }
        return result.booleanValue();
    }

    private void updateFormLoginConfiguration(String loginPage, String errorPage, SecurityMetadata securityMetadata) {
        if (loginPage != null && errorPage != null) {
            FormLoginConfiguration flc = new FormLoginConfigurationImpl(loginPage, errorPage);
            LoginConfiguration lc = new LoginConfigurationImpl(LoginConfiguration.FORM, null, flc);
            securityMetadata.setLoginConfiguration(lc);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "LoginConfiguration was updated. " + lc);
        }
    }

    protected void postLoginProcess(HttpServletRequest req, HttpServletResponse res, boolean isCustom) throws IOException, RuntimeException {
        String storedReq = null;
        WebAppSecurityConfig webAppSecConfig = getWebSAppSeurityConfig();
        ReferrerURLCookieHandler referrerURLHandler = webAppSecConfig.createReferrerURLCookieHandler();
        storedReq = getStoredReq(req, referrerURLHandler);
        // If storedReq(WASReqURL) is bad, RuntimeExceptions are thrown in isReferrerHostValid. These exceptions are not caught here. If we return here, WASReqURL is good.
        if (storedReq != null && storedReq.length() > 0) {
            ReferrerURLCookieHandler.isReferrerHostValid(PasswordNullifier.nullifyParams(req.getRequestURL().toString()), PasswordNullifier.nullifyParams(storedReq),
                                                         webAppSecConfig.getWASReqURLRedirectDomainNames());
        }
        if (isCustom) {
            // webcontainer.security code will invalidate the WASReqURL cookie for the regular form login.
            referrerURLHandler.invalidateReferrerURLCookie(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
        }
        res.setHeader("Location", res.encodeURL(storedReq));
        res.setStatus(HttpServletResponse.SC_FOUND);
    }

    /**
     * Always be redirecting to a stored req with the web app... strip any initial slash
     *
     * @param req
     * @param referrerURLHandler
     * @return storedReq
     */
    @Sensitive
    private String getStoredReq(HttpServletRequest req, ReferrerURLCookieHandler referrerURLHandler) {
        String storedReq = referrerURLHandler.getReferrerURLFromCookies(req, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
        if (storedReq != null) {
            if (storedReq.equals("/"))
                storedReq = "";
            else if (storedReq.startsWith("/"))
                storedReq = storedReq.substring(1);
        } else {
            storedReq = "";
        }
        return storedReq;
    }

    private void setCookies(HttpServletRequest req, HttpServletResponse res) {
        WebAppSecurityConfig webAppSecConfig = getWebSAppSeurityConfig();
        if (allowToAddCookieToResponse(webAppSecConfig, req)) {
            AuthenticationResult authResult = new AuthenticationResult(AuthResult.REDIRECT, "dummy");
            if ("POST".equalsIgnoreCase(req.getMethod())) {
                PostParameterHelper postParameterHelper = new PostParameterHelper(webAppSecConfig);
                postParameterHelper.save(req, res, authResult, true);
            }
            ReferrerURLCookieHandler referrerURLHandler = getWebSAppSeurityConfig().createReferrerURLCookieHandler();
            String query = req.getQueryString();
            String originalURL = req.getRequestURL().append(query != null ? "?" + query : "").toString();
            referrerURLHandler.setReferrerURLCookie(req, authResult, originalURL);
            List<Cookie> cookies = authResult.getCookies();
            for (Cookie c : cookies) {
                res.addCookie(c);
            }
        }
    }

    private String getLoginUrl(HttpServletRequest req, String loginPage) {
        StringBuilder builder = new StringBuilder(req.getRequestURL());
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getFormURL : loginPage : " + loginPage, ", requestURL : " + builder);
        int hostIndex = builder.indexOf("//");
        int contextIndex = builder.indexOf("/", hostIndex + 2);
        builder.replace(contextIndex, builder.length(), normalizeURL(loginPage, req.getContextPath()));
        return builder.toString();
    }

    private String normalizeURL(String url, String contextPath) {
        if (contextPath.equals("/"))
            contextPath = "";
        if (!url.startsWith("/"))
            url = "/" + url;
        return contextPath + url;
    }

    /**
     * This method checks the following conditions:
     * 1) If SSO requires SSL is true and NOT HTTPs request, returns false.
     * 2) Otherwise returns true.
     *
     * @param req
     * @return
     */
    private boolean allowToAddCookieToResponse(WebAppSecurityConfig webAppSecConfig, HttpServletRequest req) {
        boolean secureRequest = req.isSecure();
        if (webAppSecConfig.getSSORequiresSSL() && !secureRequest) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "SSO requires SSL. The cookie will not be sent back because the request is not over https.");
            }
            return false;
        }
        return true;
    }

    protected boolean isMethodToIntercept(InvocationContext ic) {
        String methodName = ic.getMethod().getName();
        return METHOD_TO_INTERCEPT.equals(methodName);
    }
    
    protected boolean isCustomForm(InvocationContext ic) {
        String className = ic.getMethod().getDeclaringClass().getName();
        return CUSTOM_FORM_CLASS.equals(className);
    }

    protected void setMPPInstance(Instance<ModulePropertiesProvider> mppInstance) {
        this.mppInstance = mppInstance;
    }

    protected WebAppSecurityConfig getWebSAppSeurityConfig() {
        return WebConfigUtils.getWebAppSecurityConfig();
    }

    protected ELProcessor getELProcessorWithAppModuleBeanManagerELResolver() {
        return CDIHelper.getELProcessor();
    }
}
