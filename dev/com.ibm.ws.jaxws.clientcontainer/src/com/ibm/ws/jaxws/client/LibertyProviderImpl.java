/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.client;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.spi.ServiceDelegate;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws22.spi.ProviderImpl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.metadata.JaxWsClientMetaData;
import com.ibm.ws.jaxws.metadata.WebServiceRefInfo;
import com.ibm.ws.jaxws.security.JaxWsSecurityConfigurationService;
import com.ibm.ws.jaxws.support.JaxWsMetaDataManager;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * The class provides an extension implementation for javax.xml.ws.spi.Provider based the CXF's from cxf-rt-frontend-jaxws.
 */
public class LibertyProviderImpl extends ProviderImpl {

    private static final TraceComponent tc = Tr.register(LibertyProviderImpl.class);

    private static final ThreadLocal<List<WebServiceFeature>> wsFeatures = new ThreadLocal<List<WebServiceFeature>>();

    private static final ThreadLocal<WebServiceRefInfo> wsRefInfo = new ThreadLocal<WebServiceRefInfo>();

    private static final AtomicReference<AtomicServiceReference<JaxWsSecurityConfigurationService>> securityConfigSR = new AtomicReference<AtomicServiceReference<JaxWsSecurityConfigurationService>>();

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.ws.spi.Provider#createServiceDelegate(java.net.URL, javax.xml.namespace.QName, java.lang.Class)
     */
    @Override
    public ServiceDelegate createServiceDelegate(URL url, QName qname,
                                                 @SuppressWarnings("rawtypes") Class cls) {
        Bus bus = null;
        JaxWsClientMetaData clientMetaData = JaxWsMetaDataManager.getJaxWsClientMetaData();
        if (clientMetaData != null) {
            bus = clientMetaData.getClientBus();
        }
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No client  bus is found, the thread context default bus " + bus.getId() + " is used");
            }
        }

        WebServiceRefInfo wsrInfo = wsRefInfo.get();

        List<WebServiceFeature> serviceFeatures = wsFeatures.get();
        final List<WebServiceFeature> fServiceFeatures = serviceFeatures;

        AtomicServiceReference<JaxWsSecurityConfigurationService> secConfigSR = securityConfigSR.get();
        JaxWsSecurityConfigurationService securityConfigService = secConfigSR == null ? null : secConfigSR.getService();

        // @TJJ create final vars in order to call a doPriv when creating the LibertyServiceImpl as required by java 2 security
        final JaxWsSecurityConfigurationService scs = securityConfigService;
        final WebServiceRefInfo wi = wsrInfo;
        final Bus b = bus;
        final URL u = url;
        final QName qn = qname;
        final Class c = cls;
        final List<WebServiceFeature> sf = serviceFeatures;

        if (serviceFeatures != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Thread context features are configured with " + serviceFeatures);
            }
            LibertyServiceImpl lsl = AccessController.doPrivileged(new PrivilegedAction<LibertyServiceImpl>() {
                @Override
                public LibertyServiceImpl run() {
                    return new LibertyServiceImpl(scs, wi, b, u, qn, c, sf.toArray(new WebServiceFeature[sf.size()]));
                }
            });
            return lsl;

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Thread context features are not set");
            }

            LibertyServiceImpl lsl = AccessController.doPrivileged(new PrivilegedAction<LibertyServiceImpl>() {
                @Override
                public LibertyServiceImpl run() {
                    return new LibertyServiceImpl(scs, wi, b, u, qn, c);
                }
            });
            return lsl;

        }
    }

    /**
     * set the feature list for the current thread
     *
     * @param features
     */
    public static void setWebServiceFeatures(List<WebServiceFeature> features) {
        wsFeatures.set(features);
    }

    /**
     * return the feature list for the current thread
     *
     * @return
     */
    public static List<WebServiceFeature> getWebServiceFeatures() {
        return wsFeatures.get();
    }

    /**
     * clear the feature list for the current thread
     */
    public static void clearWebServiceFeatures() {
        wsFeatures.remove();
    }

    /**
     * set the serviceRefInfo for the current thread
     *
     * @param wsrInfo
     */
    public static void setWebServiceRefInfo(WebServiceRefInfo wsrInfo) {
        wsRefInfo.set(wsrInfo);
    }

    /**
     * clear the serviceRefInfo for the current thread
     */
    public static void clearWebServiceRefInfo() {
        wsRefInfo.remove();
    }

    /**
     * return the serviceREfInfo for the current thread
     *
     * @return
     */
    public static WebServiceRefInfo getWebServiceRefInfo() {
        return wsRefInfo.get();
    }

    /**
     * Set the security configuration service
     *
     * @param securityConfigService the securityConfigService to set
     */
    public static void setSecurityConfigService(AtomicServiceReference<JaxWsSecurityConfigurationService> serviceRefer) {
        securityConfigSR.set(serviceRefer);
    }
}
