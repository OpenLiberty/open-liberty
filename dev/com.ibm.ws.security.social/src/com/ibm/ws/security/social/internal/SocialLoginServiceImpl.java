/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.social.SocialLoginService;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

public class SocialLoginServiceImpl implements SocialLoginService {
    public static final TraceComponent tc = Tr.register(SocialLoginServiceImpl.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    static final String CONFIGURATION_ADMIN = "configurationAdmin";
    static final String KEY_UNIQUE_ID = "id";

    private final String uniqueId = "SocialLoginService";

    private ConfigurationAdmin configAdmin = null;

    public static final String KEY_SSL_SUPPORT = "sslSupport";
    protected AtomicServiceReference<SSLSupport> sslSupportRef = new AtomicServiceReference<SSLSupport>(KEY_SSL_SUPPORT);
    public static final String KEY_KEYSTORE_SERVICE = "keyStoreService";
    private final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = new AtomicServiceReference<KeyStoreService>(KEY_KEYSTORE_SERVICE);

    
    private String bundleLocation;

    protected void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected void updateConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        this.configAdmin = null;
    }

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

    protected void setKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.setReference(ref);
    }

    protected void unsetKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) throws SocialLoginException {
        this.bundleLocation = cc.getBundleContext().getBundle().getLocation();
        sslSupportRef.activate(cc);
        keyStoreServiceRef.activate(cc);
        // this.sslSupport = sslSupportRef.getService();  // this is not good practice.
        Tr.info(tc, "SOCIAL_LOGIN_CONFIG_PROCESSED", uniqueId);  // CWWKS5400I
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) throws SocialLoginException {
        
        Tr.info(tc, "SOCIAL_LOGIN_CONFIG_MODIFIED", uniqueId);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        this.bundleLocation = null;
        sslSupportRef.deactivate(cc);
        keyStoreServiceRef.deactivate(cc);
        Tr.info(tc, "SOCIAL_LOGIN_CONFIG_DEACTIVATED", uniqueId);
    }

    // This method is for unittesting.
    ConfigurationAdmin getConfigurationAdmin() {
        return configAdmin;
    }

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
    @Override
    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    /**
     * @return the sslSupport
     */
    @Override
    public SSLSupport getSslSupport() {       
        return sslSupportRef.getService();
    }

    /**
     * @return the keyStoreServiceRef
     */
    @Override
    public AtomicServiceReference<KeyStoreService> getKeyStoreServiceRef() {
        return keyStoreServiceRef;
    }

    /** {@inheritDoc} */
    @Override
    public String getBundleLocation() {
        return this.bundleLocation;
    }

}
