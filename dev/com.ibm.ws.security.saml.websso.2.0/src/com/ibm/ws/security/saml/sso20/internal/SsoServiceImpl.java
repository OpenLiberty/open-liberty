/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal;

import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.filemonitor.FileBasedActionable;
import com.ibm.ws.security.filemonitor.SecurityFileMonitor;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.UnsolicitedResponseCache;
import com.ibm.ws.security.saml.sso20.rs.RsSamlConfigImpl;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

public class SsoServiceImpl implements SsoSamlService {
    public static final TraceComponent tc = Tr.register(SsoServiceImpl.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);
    public static final String TYPE = "SAMLSso20";
    public static final String VERSION = "v1.0";
    static final String CONFIGURATION_ADMIN = "configurationAdmin";
    static final String KEY_SECURITY_SERVICE = "securityService";
    static final String KEY_SERVICE_PID = "service.pid";
    static final String KEY_PROVIDER_ID = "id";
    static final String KEY_ID = "id";
    static final String KEY_inboundPropagation = "inboundPropagation";

    boolean isSamlInbound = false;

    private String providerId = null;
    //private Map<String, Object> props = null;

    private volatile ConfigurationAdmin configAdmin = null;

    protected AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    public static final String KEY_KEYSTORE_SERVICE = "keyStoreService";
    protected AtomicServiceReference<KeyStoreService> keyStoreServiceRef = new AtomicServiceReference<KeyStoreService>(KEY_KEYSTORE_SERVICE);

    public static final String KEY_SSL_SUPPORT = "sslSupport";
    protected AtomicServiceReference<SSLSupport> sslSupportRef = new AtomicServiceReference<SSLSupport>(KEY_SSL_SUPPORT);

    static final HashMap<String, Cache> acsCookieCacheMap = new HashMap<String, Cache>();

    static final HashMap<String, UnsolicitedResponseCache> replayCacheMap = new HashMap<String, UnsolicitedResponseCache>();

    protected SsoConfig samlConfig = new SsoConfigImpl();

    static HashMap<String, SsoSamlService> samlServiceMap = new HashMap<String, SsoSamlService>();

    private WebProviderAuthenticatorHelper authHelper;

    private SecurityFileMonitor idpMetadataFileMonitor;
    private ServiceRegistration<FileMonitor> idpMetadataFileMonitorRegistration;

    public SsoServiceImpl() {};

    protected void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
        getSamlConfig().setConfigAdmin(configAdmin);
    }

    protected void updateConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
        getSamlConfig().setConfigAdmin(configAdmin);
    }

    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        this.configAdmin = null;
        getSamlConfig().setConfigAdmin(null);
    }

    protected void setSecurityService(ServiceReference<SecurityService> ref) {
        securityServiceRef.setReference(ref);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> ref) {
        securityServiceRef.unsetReference(ref);
    }

    protected void setKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.setReference(ref);
    }

    protected void unsetKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.unsetReference(ref);
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

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        providerId = (String) props.get(KEY_PROVIDER_ID);
        securityServiceRef.activate(cc);
        keyStoreServiceRef.activate(cc);
        sslSupportRef.activate(cc);
        initProps(cc, props);
        Tr.info(tc, "SAML20_CONFIG_PROCESSED", providerId);
    }

    void initProps(ComponentContext cc, Map<String, Object> props) {
        //this.props = props;
        String inboundPropagation = ((String) props.get(KEY_inboundPropagation)).trim();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, providerId + " inboundPropagation:'" + inboundPropagation + "'");
        }
        isSamlInbound = Constants.REQUIRED.equals(inboundPropagation) || Constants.TRUE.equals(inboundPropagation);
        SsoConfig ssoConfig = null;
        if (isSamlInbound) {
            ssoConfig = new RsSamlConfigImpl(cc, props, configAdmin, SAMLRequestTAI.getFilterIdMap(), this);
        } else {
            ssoConfig = new SsoConfigImpl(cc, props, configAdmin, SAMLRequestTAI.getFilterIdMap(), this);
        }
        setSamlConfig(ssoConfig);
        createFileMonitor(getSamlConfig());
        authHelper = new WebProviderAuthenticatorHelper(securityServiceRef);
        if (acsCookieCacheMap.get(providerId) == null) {
            Cache cache = new Cache(0, 0);
            synchronized (acsCookieCacheMap) {
                acsCookieCacheMap.put(providerId, cache); // SsoServiceImpl
            }
        }
        if (replayCacheMap.get(providerId) == null) {
            UnsolicitedResponseCache cache = new UnsolicitedResponseCache(0, 0, getSamlConfig().getClockSkew());
            synchronized (acsCookieCacheMap) {
                replayCacheMap.put(providerId, cache); // SsoServiceImpl
            }
        }
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
        providerId = (String) props.get(KEY_PROVIDER_ID);
        unsetFileMonitorRegistration();
        initProps(cc, props);
        Tr.info(tc, "SAML20_CONFIG_MODIFIED", providerId);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        unsetFileMonitorRegistration();
        securityServiceRef.deactivate(cc);
        keyStoreServiceRef.deactivate(cc);
        setSamlConfig(new SsoConfigImpl());
        sslSupportRef.deactivate(cc);
        synchronized (acsCookieCacheMap) {
            acsCookieCacheMap.remove(providerId);
            replayCacheMap.remove(providerId);
        }
        Tr.info(tc, "SAML20_CONFIG_DEACTIVATED", providerId);
    }

    //This method for unit test
    public void setSamlConfig(SsoConfig samlConfig) {
        this.samlConfig = samlConfig;
    }

    /*
     * The Saml20Config could be changed during the server dynamica changes
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoService#getConfig()
     */
    @Override
    public SsoConfig getConfig() {
        return samlConfig;
    }

    public SsoConfig getSamlConfig() {
        return samlConfig;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoService#getProviderId()
     */
    @Override
    public String getProviderId() {
        return providerId;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SamlService#getSamlVersion()
     */
    @Override
    public Constants.SamlSsoVersion getSamlVersion() {
        return Constants.SamlSsoVersion.SAMLSSO20;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SamlSsoService#getAcsCookieCache(java.lang.String)
     */
    @Override
    public Cache getAcsCookieCache(String providerId) {
        Cache cache = acsCookieCacheMap.get(providerId);
        if (cache == null) {
            cache = new Cache(0, 0);
            synchronized (acsCookieCacheMap) {
                acsCookieCacheMap.put(providerId, cache); //
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "acsCockieCache providerId:" + providerId + " cache:" + cache);
        }
        return cache;
    }

    @Override
    public UnsolicitedResponseCache getUnsolicitedResponseCache(String providerId) {
        UnsolicitedResponseCache cache = replayCacheMap.get(providerId);
        if (cache == null) {
            cache = new UnsolicitedResponseCache(0, 0, this.getSamlConfig().getClockSkew());
            synchronized (replayCacheMap) {
                replayCacheMap.put(providerId, cache); //
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "replayCacheMap providerId:" + providerId + " cache:" + cache);
        }
        return cache;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoService#getAuthHelper()
     */
    @Override
    public WebProviderAuthenticatorHelper getAuthHelper() {
        return authHelper;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoService#getAuthFilterId()
     */
    @Override
    public String getAuthFilterId() {
        return getSamlConfig().getAuthFilterId();
    }

    public @Sensitive String getDefaultKeyStoreProperty(String propKey) {
        String keyStorePropValue = null;
        //config does not specify keystore, so try to get one from servers default ssl config.
        SSLSupport sslSupport = getSslSupportRef().getService();
        JSSEHelper jsseHelper = null;
        if (sslSupport != null) {
            jsseHelper = sslSupport.getJSSEHelper();
        }
        Properties props = null;
        if (jsseHelper != null) {
            try {
                Map<String, Object> connectionInfo = new HashMap<String, Object>();
                connectionInfo.put(com.ibm.websphere.ssl.Constants.CONNECTION_INFO_DIRECTION, com.ibm.websphere.ssl.Constants.DIRECTION_INBOUND);
                props = jsseHelper.getProperties("", connectionInfo, null, true);
            } catch (SSLException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
                //e.printStackTrace();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception getting jssehelper!!!");
                }
            }
            if (props != null) {
                keyStorePropValue = props.getProperty(propKey);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "KeyStore property ( " + propKey + " ) from default ssl config  " );
                }
            }
        }
        return keyStorePropValue;
    }

    /** {@inheritDoc} */
    @Sensitive
    @Override
    @FFDCIgnore({ CertificateException.class, KeyStoreException.class })
    //prevent unnecessary FFDC when we are looking for private key
    public PrivateKey getPrivateKey() throws KeyStoreException, CertificateException {

        String keyStoreName = getSamlConfig().getKeyStoreRef();
        String keyAlias = getSamlConfig().getKeyAlias();

        if (keyStoreName == null) {
            keyStoreName = getDefaultKeyStoreProperty("com.ibm.ssl.keyStoreName");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "KeyStore name from default ssl config  = ", keyStoreName );
            }
        }
        KeyStoreService keyStoreService = getKeyStoreServiceRef().getService();

        PrivateKey privateKey = null;
        if (keyStoreService != null) {
            if (keyAlias == null) {
                // default does not specify key alias
                // try samlsp first and if we don't find it then try one more time to
                // see if there is only one key in the keystore.
                keyAlias = "samlsp";
                try {
                    privateKey = keyStoreService.getPrivateKeyFromKeyStore(keyStoreName, keyAlias,
                                                                           getSamlConfig().getKeyPassword());
                } catch (KeyStoreException kse) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception getting key using default alias.", kse.toString());
                        Tr.debug(tc, "Try getting key one more time to see if there is only one key!!");
                    }
                    privateKey = keyStoreService.getPrivateKeyFromKeyStore(keyStoreName);
                } catch (CertificateException ce) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception getting key using default alias.", ce.toString());
                        Tr.debug(tc, "Try getting key one more time to see if there is only one key!!");
                    }
                    privateKey = keyStoreService.getPrivateKeyFromKeyStore(keyStoreName);
                }

            } else {
                privateKey = keyStoreService.getPrivateKeyFromKeyStore(keyStoreName, keyAlias,
                                                                       getSamlConfig().getKeyPassword());
            }
        }

        return privateKey;
    }

    /** {@inheritDoc} */
    @Sensitive
    @Override
    @FFDCIgnore({ CertificateException.class, KeyStoreException.class })
    public Certificate getSignatureCertificate() throws KeyStoreException, CertificateException {
        String keyStoreName = getSamlConfig().getKeyStoreRef();
        String keyAlias = getSamlConfig().getKeyAlias();

        if (keyStoreName == null) {
            keyStoreName = getDefaultKeyStoreProperty("com.ibm.ssl.keyStoreName");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "KeyStore name from default ssl config  = ", keyStoreName );
            }
        }
        KeyStoreService keyStoreService = getKeyStoreServiceRef().getService();
        Certificate cert = null;
        if (keyAlias == null) {
            keyAlias = "samlsp";
            try {
                cert = keyStoreService.getCertificateFromKeyStore(keyStoreName,
                                                                  keyAlias);
            } catch (KeyStoreException kse) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception getting key using default alias.", kse.toString());
                    Tr.debug(tc, "Try getting key one more time to see if there is only one key!!");

                }
                cert = keyStoreService.getX509CertificateFromKeyStore(keyStoreName);
            } catch (CertificateException ce) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception getting key using default alias.", ce.toString());
                    Tr.debug(tc, "Try getting key one more time to see if there is only one key!!");
                }
                cert = keyStoreService.getX509CertificateFromKeyStore(keyStoreName);
            }
        } else {
            cert = keyStoreService.getCertificateFromKeyStore(keyStoreName, keyAlias);
        }

        return cert;
    }

    /**
     * Handles the creation of the idpMetadata file monitor.
     */
    void createFileMonitor(SsoConfig samlConfig) {
        try {
            if (samlConfig instanceof FileBasedActionable) {
                idpMetadataFileMonitor = new SecurityFileMonitor((FileBasedActionable) samlConfig);
                String idpFileName = samlConfig.getIdpMetadata();
                if (idpFileName != null && !idpFileName.isEmpty()) {
                    setFileMonitorRegistration(idpMetadataFileMonitor.monitorFiles(Arrays.asList(samlConfig.getIdpMetadata()), 2000)); // 2 seconds
                }
            } else {
                if (samlConfig instanceof RsSamlConfigImpl) { // this is OK if rsSamlConfiImpl
                    // this is OK, rsSamlConfig does not need to monitor idpMetadata xml file
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The rsSamlConfig does not need to monitor idp metadata xml file.");
                    }
                } else {
                    // this is not supposed to happen
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "ERROR: The samlConfig is not an FileBasedActionable instance.");
                    }
                }
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception creating the idpMetadata file monitor.", e);
            }
        }
    }

    /**
     * Sets the idpMetadata file monitor registration.
     *
     * @param idpMetadataFileMonitorRegistration
     */
    protected void setFileMonitorRegistration(ServiceRegistration<FileMonitor> idpMetadataFileMonitorRegistration) {
        this.idpMetadataFileMonitorRegistration = idpMetadataFileMonitorRegistration;
    }

    protected void unsetFileMonitorRegistration() {
        if (idpMetadataFileMonitorRegistration != null) {
            idpMetadataFileMonitorRegistration.unregister();
            idpMetadataFileMonitorRegistration = null;
        }
    }

    //This method is for unittesting.
    ConfigurationAdmin getConfigurationAdmin() {
        return configAdmin;
    }

    @Override
    public boolean searchTrustAnchors(Collection<X509Certificate> trustAnchors, String trustAnchorName) throws SamlException {
        if (trustAnchorName == null || trustAnchorName.isEmpty()) {
            trustAnchorName = getDefaultKeyStoreProperty("com.ibm.ssl.trustStoreName");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "TrustStore name from default ssl config  = ", trustAnchorName );
            }
        }

        KeyStoreService keyStoreService = getKeyStoreServiceRef().getService();

        if (keyStoreService == null)
            return false;
        Collection<String> certNames;
        try {
            certNames = keyStoreService.getTrustedCertEntriesInKeyStore(trustAnchorName);
            for (String certName : certNames) {
                X509Certificate cert = keyStoreService.getX509CertificateFromKeyStore(trustAnchorName, certName);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "getCert trustAnchorName:" + trustAnchorName + " certId:" + certName + " cert:" + cert);
                }
                trustAnchors.add(cert);
            }
        } catch (KeyStoreException e) {
            throw new SamlException(e);
        } catch (CertificateException e) {
            throw new SamlException(e);
        }
        return true;
    }

    @Override
    public boolean isEnabled() {
        return getSamlConfig().isEnabled();
    }

    /**
     * @return the keyStoreServiceRef
     */
    public AtomicServiceReference<KeyStoreService> getKeyStoreServiceRef() {
        return keyStoreServiceRef;
    }

    /**
     * @return the sslSupportRef
     */
    public AtomicServiceReference<SSLSupport> getSslSupportRef() {
        return sslSupportRef;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoSamlService#isSamlInbound()
     */
    @Override
    public boolean isInboundPropagation() {
        return isSamlInbound;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.security.saml.SsoSamlService#getDefaultKeyStorePassword()
     */
    @Override
    public @Sensitive String getDefaultKeyStorePassword() {
        // TODO Auto-generated method stub
        return getDefaultKeyStoreProperty("com.ibm.ssl.keyStorePassword");
    }
}
