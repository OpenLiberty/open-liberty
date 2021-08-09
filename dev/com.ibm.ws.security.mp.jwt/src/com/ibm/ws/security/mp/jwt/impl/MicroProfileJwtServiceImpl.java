/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.impl;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.mp.jwt.MicroProfileJwtService;
import com.ibm.ws.security.mp.jwt.TraceConstants;
import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.mp.jwt.osgi.MpJwtRuntimeVersion;

@Component(service = MicroProfileJwtService.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM", name = "microProfileJwtService")
public class MicroProfileJwtServiceImpl implements MicroProfileJwtService {

    public static final TraceComponent tc = Tr.register(MicroProfileJwtServiceImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static final String KEY_UNIQUE_ID = "id";

    private final String uniqueId = "MicroProfileJwtService";

    //private ConfigurationAdmin configAdmin = null;

    public static final String KEY_SSL_SUPPORT = "sslSupport";
    protected AtomicServiceReference<SSLSupport> sslSupportRef = new AtomicServiceReference<SSLSupport>(KEY_SSL_SUPPORT);
    public static final String KEY_KEYSTORE_SERVICE = "keyStoreService";
    private final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = new AtomicServiceReference<KeyStoreService>(KEY_KEYSTORE_SERVICE);
    public static final String KEY_MP_JWT_RUNTIME_VERSION_SERVICE = "mpJwtRuntimeVersionService";
    private final AtomicServiceReference<MpJwtRuntimeVersion> mpJwtRuntimeVersionServiceRef = new AtomicServiceReference<MpJwtRuntimeVersion>(KEY_MP_JWT_RUNTIME_VERSION_SERVICE);

    SSLSupport sslSupport = null;

    //    protected void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
    //        this.configAdmin = configAdmin;
    //    }
    //
    //    protected void updateConfigurationAdmin(ConfigurationAdmin configAdmin) {
    //        this.configAdmin = configAdmin;
    //    }
    //
    //    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
    //        this.configAdmin = null;
    //    }

    @Reference(service = SSLSupport.class, name = KEY_SSL_SUPPORT, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    protected void setSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setSslSupport service.pid:" + ref.getProperty("service.pid"));
        }
    }

    protected void updatedSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "updatedtSslSupport service.pid:" + ref.getProperty("service.pid"));
        }
    }

    protected void unsetSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.unsetReference(ref);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "unsetSslSupport service.pid:" + ref.getProperty("service.pid"));
        }
    }

    @Reference(service = KeyStoreService.class, name = KEY_KEYSTORE_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    protected void setKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.setReference(ref);
    }

    protected void unsetKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.unsetReference(ref);
    }

    @Reference(service = MpJwtRuntimeVersion.class, name = KEY_MP_JWT_RUNTIME_VERSION_SERVICE, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setMpJwtRuntimeVersion(ServiceReference<MpJwtRuntimeVersion> reference) {
        mpJwtRuntimeVersionServiceRef.setReference(reference);
    }

    protected void unsetMpJwtRuntimeVersion(ServiceReference<MpJwtRuntimeVersion> reference) {
        mpJwtRuntimeVersionServiceRef.unsetReference(reference);
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) throws MpJwtProcessingException {
        sslSupportRef.activate(cc);
        keyStoreServiceRef.activate(cc);
        this.sslSupport = sslSupportRef.getService();
        mpJwtRuntimeVersionServiceRef.activate(cc);
        Tr.info(tc, "MPJWT_CONFIG_PROCESSED", uniqueId);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) throws MpJwtProcessingException {
        this.sslSupport = sslSupportRef.getService();
        Tr.info(tc, "MPJWT_CONFIG_MODIFIED", uniqueId);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        sslSupportRef.deactivate(cc);
        keyStoreServiceRef.deactivate(cc);
        mpJwtRuntimeVersionServiceRef.deactivate(cc);
        Tr.info(tc, "MPJWT_CONFIG_DEACTIVATED", uniqueId);
    }

    // This method is for unittesting.
    //    ConfigurationAdmin getConfigurationAdmin() {
    //        return configAdmin;
    //    }

    /**
     * @return the sslSupportRef
     */
    @Override
    public AtomicServiceReference<SSLSupport> getSslSupportRef() {
        return sslSupportRef;
    }

    /**
     * @return the configAdmin
     */
    //    @Override
    //    public ConfigurationAdmin getConfigAdmin() {
    //        return configAdmin;
    //    }

    /**
     * @return the sslSupport
     */
    @Override
    public SSLSupport getSslSupport() {
        return sslSupport;
    }

    /**
     * @return the keyStoreServiceRef
     */
    @Override
    public AtomicServiceReference<KeyStoreService> getKeyStoreServiceRef() {
        return keyStoreServiceRef;
    }

    @Override
    public MpJwtRuntimeVersion getMpJwtRuntimeVersion() {
        return mpJwtRuntimeVersionServiceRef.getService();
    }

}
