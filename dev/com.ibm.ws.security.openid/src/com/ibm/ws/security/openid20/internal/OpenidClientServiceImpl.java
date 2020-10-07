/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openid20.internal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.net.ssl.SSLContext;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.openid20.OpenidClientAuthenticator;
import com.ibm.ws.security.openid20.OpenidConstants;
import com.ibm.ws.security.openid20.OpenidClientConfig;

import com.ibm.ws.security.openid20.consumer.OpenidClientAuthenticatorImpl;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.openid20.OpenidClientService;
import com.ibm.ws.security.openid20.TraceConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.ssl.SSLConfiguration;

/**
 * This class is the OSGI service that is invoked from the FormLoginExtensionProcessor
 * to handle OpenID Authentication of incoming web requests.
 * The absence of this service reference indicates that OpenID20 is not enabled.
 */
public class OpenidClientServiceImpl implements OpenidClientService {
    private static final TraceComponent tc = Tr.register(OpenidClientServiceImpl.class);
    public static final String KEY_OPENID_CLIENT_CONFIG = "openidClientConfig";
    public static final String KEY_SSL_SUPPORT = "sslSupport";
    public static final String KEY_SSL_CONFIG = "sslConfig";

    static final String CFG_ID = "id";

    protected final AtomicServiceReference<OpenidClientConfig> openidClientConfigRef = new AtomicServiceReference<OpenidClientConfig>(KEY_OPENID_CLIENT_CONFIG);
    protected final AtomicServiceReference<SSLSupport> sslSupportRef = new AtomicServiceReference<SSLSupport>(KEY_SSL_SUPPORT);
    protected final ConcurrentServiceReferenceMap<String, SSLConfiguration> sslConfigRef = new ConcurrentServiceReferenceMap<String, SSLConfiguration>(KEY_SSL_CONFIG);
    private OpenidClientConfig openidClientConfig = null;

    private OpenidClientAuthenticator openidAuthenticator;
    private String defaultSslConfig = null;
    private boolean lazyInitOpenidAuth = false;

    protected void setOpenidAuthenticator(OpenidClientAuthenticator openidAuthenticator) {
        this.openidAuthenticator = openidAuthenticator;
    }

    protected void setOpenidClientConfig(ServiceReference<OpenidClientConfig> ref) {
        openidClientConfigRef.setReference(ref);
        openidClientConfig = openidClientConfigRef.getService();
    }

    protected void updatedOpenidClientConfig(ServiceReference<OpenidClientConfig> ref) {
        openidClientConfigRef.setReference(ref);
        openidClientConfig = openidClientConfigRef.getService();
        lazyInitOpenidAuth = true;
    }

    protected void unsetOpenidClientConfig(ServiceReference<OpenidClientConfig> ref) {
        openidClientConfigRef.unsetReference(ref);
        openidClientConfig = null;
    }

    protected void setSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);

        defaultSslConfig = (String) ref.getProperty(OpenidClientConfigImpl.CFG_KEY_SSL_REF);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "defaultSslConfig: " + defaultSslConfig);
        }
        lazyInitOpenidAuth = true;
    }

    protected void updatedSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);

        defaultSslConfig = (String) ref.getProperty(OpenidClientConfigImpl.CFG_KEY_SSL_REF);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "defaultSslConfig: " + defaultSslConfig);
        }
        lazyInitOpenidAuth = true;
    }

    protected void unsetSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.unsetReference(ref);
        defaultSslConfig = null;
        lazyInitOpenidAuth = true;
    }

    protected void updatedSslConfig(ServiceReference<SSLConfiguration> ref) {
        String id = (String) ref.getProperty(CFG_ID);
        sslConfigRef.putReference(id, ref);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sslConfig:" + id);
        }
        initOpenidAuthIfNeeded(id);
    }

    protected void setSslConfig(ServiceReference<SSLConfiguration> ref) {
        String id = (String) ref.getProperty(CFG_ID);
        sslConfigRef.putReference(id, ref);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sslConfig: " + id);
        }
        initOpenidAuthIfNeeded(id);
    }

    protected void unsetSslConfig(ServiceReference<SSLConfiguration> ref) {
        String id = (String) ref.getProperty(CFG_ID);
        sslConfigRef.removeReference(id, ref);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sslConfig: " + id);
        }
        initOpenidAuthIfNeeded(id);
    }

    protected synchronized void activate(ComponentContext cc) {
        openidClientConfigRef.activate(cc);
        sslSupportRef.activate(cc);
        sslConfigRef.activate(cc);
        openidAuthenticator = new OpenidClientAuthenticatorImpl();
        lazyInitOpenidAuth = true;
    }

    protected synchronized void modify(Map<String, Object> properties) {}

    protected synchronized void deactivate(ComponentContext cc) {
        openidClientConfigRef.deactivate(cc);
        sslSupportRef.deactivate(cc);
        sslConfigRef.deactivate(cc);
        openidAuthenticator = null;
    }

    public String getOpenIdIdentifier(HttpServletRequest req) {
        return req.getParameter(OpenidConstants.OPENID_IDENTIFIER);
    }

    public void createAuthRequest(HttpServletRequest request,
                                  HttpServletResponse response)
                    throws Exception {
        if (lazyInitOpenidAuth) {
            SSLContext sslContext = null;
            try {
                sslContext = getSSLContext();
            } catch (SSLException e) {
                throw new Exception(e.getMessage());
            }
            openidAuthenticator.initialize(openidClientConfig, sslContext);
            lazyInitOpenidAuth = false;
        }

        if (request.getCharacterEncoding() == null) {
            try {
                request.setCharacterEncoding(openidClientConfig.getCharacterEncoding());
            } catch (UnsupportedEncodingException e) {
                if (tc.isWarningEnabled()) {
                    Tr.warning(tc, e.getMessage());
                }
            }
        }

        openidAuthenticator.createAuthRequest(request, response);
    }

    public String getRpRequestIdentifier(HttpServletRequest req, HttpServletResponse res) {
        return req.getParameter(OpenidConstants.RP_REQUEST_IDENTIFIER);
    }

    public ProviderAuthenticationResult verifyOpResponse(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ProviderAuthenticationResult result = openidAuthenticator.verifyResponse(request);
        return result;
    }

    public boolean isMapIdentityToRegistryUser() {
        return openidClientConfig == null ? false : openidClientConfig.isMapIdentityToRegistryUser();
    }

    protected SSLContext getSSLContext() throws SSLException {
        SSLContext sslContext = null;
        openidClientConfig = openidClientConfigRef.getService();
        JSSEHelper jsseHelper = getJSSEHelper(openidClientConfig);
        if (jsseHelper != null) {
            String sslRef = openidClientConfig.getSslRef();
            if (sslRef != null) {
                if (!jsseHelper.doesSSLConfigExist(sslRef)) {
                    Tr.error(tc, "OPENID_RP_CONFIG_INVALID_SSLREF", sslRef);
                    throw new SSLException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                        TraceConstants.MESSAGE_BUNDLE,
                                                                        "OPENID_RP_CONFIG_INVALID_SSLREF",
                                                                        new Object[] { sslRef },
                                                                        "CWWKS1507E: OpenID configuration requires SSL but sslRef {0} does not exist or is blank."));
                }
            }
            //if sslRef is null, use the default ssl config        
            sslContext = jsseHelper.getSSLContext(sslRef, null, null);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "sslContext (" + sslRef + ") get: " + sslContext);
            }
            if (sslContext == null && openidClientConfig.ishttpsRequired()) {
                Tr.error(tc, "OPENID_HTTPS_WITH_SSLCONTEXT_NULL");
                throw new SSLException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                    TraceConstants.MESSAGE_BUNDLE,
                                                                    "OPENID_HTTPS_WITH_SSLCONTEXT_NULL",
                                                                    null,
                                                                    "CWWKS1509E: OpenID configuration requires SSL but SSL is not properly configured."));
            }
        }
        return sslContext;
    }

    protected JSSEHelper getJSSEHelper(OpenidClientConfig openidClientConfig) throws SSLException {
        SSLSupport sslSupport = sslSupportRef.getService();
        if (sslSupport != null) {
            return sslSupport.getJSSEHelper(); // already synchronized in sslSupport
        } else if (openidClientConfig.ishttpsRequired()) {
            Tr.error(tc, "OPENID_HTTPS_WITHOUT_SSL_SERVICE");
            throw new SSLException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                TraceConstants.MESSAGE_BUNDLE,
                                                                "OPENID_HTTPS_NO_SSL_SERVICE",
                                                                null,
                                                                "CWWKS1508E: OpenID configuration requires SSL but SSL service is not available."));
        }
        return null;
    }

    protected boolean initOpenidAuthIfNeeded(String id) {
        //We only need to set the lazyInitConsumer when the sslConfig updated that is used by openId
        OpenidClientConfig openidClientConfig = openidClientConfigRef.getService();
        if (openidClientConfig != null && id.equalsIgnoreCase(openidClientConfig.getSslRef())) {
            lazyInitOpenidAuth = true;
        } else if (defaultSslConfig != null && id.equalsIgnoreCase(defaultSslConfig)) {
            lazyInitOpenidAuth = true;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "lazyInitConsumer:" + lazyInitOpenidAuth);
        }
        return lazyInitOpenidAuth;
    }
}
