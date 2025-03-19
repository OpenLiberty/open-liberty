/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxws.security.internal;

import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.security.JaxWsSecurityConfigurationService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 * The implementation of JaxWsSecurityConfigurationService, internal use only
 */
@Component(service = { JaxWsSecurityConfigurationService.class }, configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = false, property = { "service.vendor=IBM" })
public class JaxWsSecurityConfigurationServiceImpl implements JaxWsSecurityConfigurationService {
    private static final TraceComponent tc = Tr.register(JaxWsSecurityConfigurationServiceImpl.class);

    private final AtomicServiceReference<SSLSupport> sslSupportSR = new AtomicServiceReference<SSLSupport>("SSLSupportService");

    @Activate
    protected void activate(ComponentContext cCtx) {
        sslSupportSR.activate(cCtx);
    }

    @Deactivate
    protected void deactivate(ComponentContext cCtx) {
        sslSupportSR.deactivate(cCtx);
    }

    @Reference(name = "SSLSupportService", service = SSLSupport.class, cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setSSLSupportService(ServiceReference<SSLSupport> serviceRef) {
        sslSupportSR.setReference(serviceRef);
        JaxWsSSLManager.init(sslSupportSR);
    }

    protected void unsetSSLSupportService(ServiceReference<SSLSupport> serviceRef) {
        sslSupportSR.unsetReference(serviceRef);
    }

    @Override
    public void configBasicAuth(Conduit conduit, String userName, ProtectedString password) {
        if (null == userName || null == password) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The userName or the password is empty");
            }
            return;
        }

        if (conduit instanceof HTTPConduit) {
            HTTPConduit httpConduit = (HTTPConduit) conduit;

            AuthorizationPolicy authPolicy = httpConduit.getAuthorization();
            if (null == authPolicy) {
                authPolicy = new AuthorizationPolicy();
            }

            String decodedPwd = PasswordUtil.passwordDecode(new String(password.getChars()));
            authPolicy.setUserName(userName);
            authPolicy.setPassword(decodedPwd);

            httpConduit.setAuthorization(authPolicy);
        }
        // TODO Other type of conduit can configure here
    }

    @Override
    public void configClientSSL(Conduit conduit, String sslRef, String certAlias) {
        Map<String, Object> overrideProps = new HashMap<String, Object>();
        if (null != certAlias) {
            overrideProps.put(JaxWsSecurityConstants.CLIENT_KEY_STORE_ALIAS, certAlias);
        }

        if (conduit instanceof HTTPConduit) {
            HTTPConduit httpConduit = (HTTPConduit) conduit;

            TLSClientParameters tlsClientParams = retriveHTTPTLSClientParametersUsingSSLRef(httpConduit, sslRef, overrideProps);
            if (null != tlsClientParams) {
                httpConduit.setTlsClientParameters(tlsClientParams);
            }
        }
        // TODO Other type of conduit can configure here

    }

    private TLSClientParameters retriveHTTPTLSClientParametersUsingSSLRef(HTTPConduit httpConduit, String sslRef, Map<String, Object> overrideProps) {
        TLSClientParameters tlsClientParams = httpConduit.getTlsClientParameters();

        SSLSocketFactory sslSocketFactory = null;

        if (!StringUtils.isEmpty(sslRef)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Use the sslRef = " + sslRef + " to create the SSLSocketFactory.");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Get the Liberty default SSLSocketFactory.");
            }
        }
        sslSocketFactory = JaxWsSSLManager.getProxySSLSocketFactoryBySSLRef(sslRef, overrideProps);

        if (null != sslSocketFactory) {
            if (null == tlsClientParams) {
                tlsClientParams = new TLSClientParameters();
            }
            tlsClientParams.setSSLSocketFactory(sslSocketFactory);
        }

        return tlsClientParams;
    }

}
