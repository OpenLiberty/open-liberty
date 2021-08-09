/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.csiv2.config.css;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.CommonCfg;
import com.ibm.ws.security.csiv2.TraceConstants;
import com.ibm.ws.security.csiv2.config.ssl.SSLConfig;
import com.ibm.ws.security.csiv2.util.SecurityServices;
import com.ibm.ws.transport.iiop.security.config.css.CSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSCompoundSecMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSCompoundSecMechListConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSNULLASMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSNULLTransportConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASITTAbsent;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSSSLTransportConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSTransportMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.OptionsKey;

/**
 * Client config helper class reads the iiopClientPolicy configuration from server.xml and maps to CSSConfig.
 */
public abstract class CommonClientCfg extends CommonCfg {

    private static TraceComponent tc = Tr.register(CommonClientCfg.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_POLICY = "clientPolicy";
    public static final String KEY_TRUSTED_IDENTITY = "trustedIdentity";
    public static final String KEY_TRUSTED_PASSWORD = "trustedPassword";
    public static final String KEY_AUTHENTICATION_USER = "user";
    public static final String KEY_AUTHENTICATION_PASSWORD = "password";
    public static final String KEY_AUTHENTICATION_REALM = "realm";

    protected final String domain;
    private final String TYPE;
    private final Authenticator authenticator;
    protected final SSLConfig sslConfig;

    /* ctor */
    public CommonClientCfg(Authenticator authenticator, String domain, String defaultAlias, String type) {
        super(defaultAlias);
        this.TYPE = type;
        this.authenticator = authenticator;
        this.domain = domain;
        this.sslConfig = SecurityServices.getSSLConfig();
    }

    public CSSConfig getCSSConfig(Map<String, Object> properties) throws Exception {
        CSSConfig cssConfig = new CSSConfig();
        printTrace("IIOP Client Policy", null, 0);

        PolicyData policyData = extractPolicyData(properties, KEY_POLICY, TYPE);

        if (policyData != null) {
            printTrace("CSIV2", null, 1);

            CSSCompoundSecMechListConfig mechListConfig = cssConfig.getMechList();
            mechListConfig.setStateful(policyData.stateful);
            printTrace("Stateful", mechListConfig.isStateful(), 2);

            populateSecMechList(mechListConfig, policyData.layersData);
        }
        return cssConfig;
    }

    /*
     * Iterate through each of the "layers" elements to add compound sec mechs to the compound sec mech list.
     */
    private void populateSecMechList(CSSCompoundSecMechListConfig mechListConfig, List<LayersData> layersList) throws Exception {
        for (LayersData mech : layersList) {
            for (CSSCompoundSecMechConfig cssCompoundSecMechConfig : extractCompoundSecMech(mech)) {
                mechListConfig.add(cssCompoundSecMechConfig);
            }
        }
    }

    protected List<CSSCompoundSecMechConfig> extractCompoundSecMech(LayersData mechInfo) throws Exception {
        printTrace("Layers", null, 1);
        List<CSSCompoundSecMechConfig> compoundSecMechConfigs = new ArrayList<CSSCompoundSecMechConfig>();

        setAuthenticationLayerConfig(compoundSecMechConfigs, mechInfo);
        setTransportLayerConfig(compoundSecMechConfigs, mechInfo);
        setAttributeLayerConfig(compoundSecMechConfigs, mechInfo);
        return compoundSecMechConfigs;
    }

    protected void setTransportLayerConfig(List<CSSCompoundSecMechConfig> compoundSecMechs, LayersData mechInfo) throws SSLException {
        Map<String, Object> transportLayerProperties = mechInfo.transportLayer;
        CSSTransportMechConfig cssTransportMechConfig;
        if (transportLayerProperties != null) {
            printTrace("Transport Layer", null, 2);
            boolean sslEnabled = (Boolean) transportLayerProperties.get(KEY_SSL_ENABLED);
            if (sslEnabled) {
                cssTransportMechConfig = extractSSLTransport(transportLayerProperties);
            } else {
                cssTransportMechConfig = new CSSNULLTransportConfig();
            }
        } else {
            /* If no configuration for CSIv2 transport layer. Continue with default or NULL transport configuration. */
            cssTransportMechConfig = new CSSNULLTransportConfig();
        }
        for (CSSCompoundSecMechConfig cssCompoundSecMechConfig : compoundSecMechs) {
            cssCompoundSecMechConfig.setTransport_mech(cssTransportMechConfig);
        }
    }

    protected CSSTransportMechConfig extractSSLTransport(Map<String, Object> transportLayerProperties) throws SSLException {
        String sslRef = (String) transportLayerProperties.get(KEY_SSL_REF);
        if (sslRef == null) {
            sslRef = sslConfig.getSSLAlias();
        }
        OptionsKey options = sslConfig.getAssociationOptions(sslRef);
        CSSSSLTransportConfig transportLayerConfig = new CSSSSLTransportConfig();
        transportLayerConfig.setSupports(options.supports);
        transportLayerConfig.setRequires(options.requires);
        transportLayerConfig.setSslConfigName(sslRef);
        return transportLayerConfig;
    }

    private void setAuthenticationLayerConfig(List<CSSCompoundSecMechConfig> compoundSecMechs, LayersData mechInfo) {
        Map<String, Object> authenticationLayerProperties = mechInfo.authenticationLayer;

        if (authenticationLayerProperties != null) {
            printTrace("Authentication Layer", null, 2);
            for (CSSASMechConfig cssasMechConfig : extractASMech(authenticationLayerProperties)) {
                CSSCompoundSecMechConfig compoundSecMechConfig = new CSSCompoundSecMechConfig();
                compoundSecMechConfig.setAs_mech(cssasMechConfig);
                compoundSecMechs.add(compoundSecMechConfig);
            }
        } else {
            /* If no configuration for CSIv2 authentication layer, then continue with default or NULL authentication configuration. */
            CSSCompoundSecMechConfig compoundSecMechConfig = new CSSCompoundSecMechConfig();
            compoundSecMechConfig.setAs_mech(new CSSNULLASMechConfig());
            compoundSecMechs.add(compoundSecMechConfig);
        }
    }

    private List<CSSASMechConfig> extractASMech(Map<String, Object> authenticationLayerProperties) {
        List<CSSASMechConfig> cssasMechConfigs = new ArrayList<CSSASMechConfig>();
        List<String> configuredMechanisms = new ArrayList<String>();

        String establishTrustInClient = (String) authenticationLayerProperties.get(KEY_ESTABLISH_TRUST_IN_CLIENT);
        printTrace("EstablishTrustInClient", establishTrustInClient, 3);
        boolean required = false;
        if (OPTION_REQUIRED.equals(establishTrustInClient)) {
            required = true;
        } else if (OPTION_NEVER.equals(establishTrustInClient)) {
            logWarning("CSIv2_COMMON_AUTH_LAYER_DISABLED", establishTrustInClient);
            cssasMechConfigs.add(new CSSNULLASMechConfig());
            return cssasMechConfigs;
        }

        List<String> mechanisms = getAsMechanisms(authenticationLayerProperties);
        if (mechanisms.isEmpty()) {
            logWarning("CSIv2_CLIENT_AUTH_MECHANISMS_NULL");
            cssasMechConfigs.add(new CSSNULLASMechConfig());
        } else {
            for (String mech : mechanisms) {
                if (!configuredMechanisms.contains(mech.toUpperCase())) {
                    CSSASMechConfig camConfig = handleASMech(mech, authenticator, domain, required, authenticationLayerProperties);
                    if (camConfig != null) {
                        cssasMechConfigs.add(camConfig);
                    } else {
                        logWarning("CSIv2_CLIENT_AUTH_MECHANISM_INVALID");
                    }
                    configuredMechanisms.add(mech.toUpperCase());
                }
            }
            if (cssasMechConfigs.isEmpty()) {
                cssasMechConfigs.add(new CSSNULLASMechConfig());
            }
        }

        return cssasMechConfigs;
    }

    private void setAttributeLayerConfig(List<CSSCompoundSecMechConfig> compoundSecMechs, LayersData mechInfo) {
        CSSSASMechConfig sasMechConfig = null;
        Map<String, Object> attributeLayerProperties = null;
        attributeLayerProperties = getAttributeLayerProperties(mechInfo);

        if (attributeLayerProperties != null) {
            printTrace("Attribute Layer", null, 2);
            sasMechConfig = extractSASMech(attributeLayerProperties);
        } else {
            /* If no configuration for CSIv2 attribute layer, or client container, then continue with default ITTAbsent configuration. */
            sasMechConfig = new CSSSASMechConfig();
            sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        }
        for (CSSCompoundSecMechConfig compoundSecMechConfig : compoundSecMechs) {
            compoundSecMechConfig.setSas_mech(sasMechConfig);
        }
    }

    protected CSSSASMechConfig extractSASMech(Map<String, Object> attributeLayerProperties) {
        CSSSASMechConfig sasMechConfig = new CSSSASMechConfig();
        // ITTAbsent must be supported always.
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        return sasMechConfig;
    }

    /**
     * @param properties
     * @return
     */
    public Set<String> extractSslRefs(Map<String, Object> properties) {
        Set<String> sslRefs = extractSslRefs(properties, KEY_POLICY, TYPE);
        return sslRefs;
    }

    /**
     * Process ASMech value.
     *
     * @param mech authentication mechanism name.
     * @param authenticator authenticator object, null is acceptable.
     * @param domain the domain name. null is acceptable.
     * @param required the required value.
     * @param props the authentication layer property values.
     * @return CSSASMechConfig object if it is processed, null otherwise.
     */
    public abstract CSSASMechConfig handleASMech(String mech, Authenticator authenticator, String domain, boolean required, Map<String, Object> props);

    /**
     * Process warning message by the implementation class.
     *
     * @param messageKey message key.
     * @param objs objects which are included in the message.
     */
    public abstract void logWarning(String messageKey, Object... objs);

    /**
     * Returns AttributeLayerProperties from LayersData.
     *
     * @param mechInfo all layers properties.
     * @return attribute layer properties. null if there is no attribute layer properties.
     */
    public abstract Map<String, Object> getAttributeLayerProperties(LayersData mechInfo);

}
