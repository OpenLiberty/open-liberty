/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.csiv2.server.config.tss;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.omg.CSIIOP.TransportAddress;
import org.omg.GSSUP.GSSUPMechOID;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.CommonCfg;
import com.ibm.ws.security.csiv2.server.TraceConstants;
import com.ibm.ws.security.csiv2.trust.TrustedIDEvaluatorImpl;
import com.ibm.ws.security.csiv2.util.SecurityServices;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.ws.transport.iiop.security.config.tss.OptionsKey;
import com.ibm.ws.transport.iiop.security.config.tss.TSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechListConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSITTAbsent;
import com.ibm.ws.transport.iiop.security.config.tss.TSSITTAnonymous;
import com.ibm.ws.transport.iiop.security.config.tss.TSSITTDistinguishedName;
import com.ibm.ws.transport.iiop.security.config.tss.TSSITTPrincipalNameGSSUP;
import com.ibm.ws.transport.iiop.security.config.tss.TSSITTX509CertChain;
import com.ibm.ws.transport.iiop.security.config.tss.TSSNULLASMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSNULLTransportConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSSSLTransportConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSTransportMechConfig;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;

/**
 * Server config helper class reads the iiopServerPolicy configuration from server.xml and maps to TSSConfig.
 */
public class ServerConfigHelper extends CommonCfg {

    private static TraceComponent tc = Tr.register(ServerConfigHelper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    public static final String KEY_POLICY = "serverPolicy";
    private static final String TYPE = "com.ibm.ws.security.csiv2.serverPolicyCSIV2";
    public static final String KEY_TRUSTED_IDENTITIES = "trustedIdentities";

    private final Authenticator authenticator;
    private final TokenManager tokenManager;
    private final UnauthenticatedSubjectService unauthenticatedSubjectService;
    private final String targetName;

    /**
     * @param authenticator
     * @param tokenManager
     * @param defaultAlias TODO
     * @param unuathenticatedSubjectService
     */
    public ServerConfigHelper(Authenticator authenticator, TokenManager tokenManager, UnauthenticatedSubjectService unauthenticatedSubjectService, String targetName,
                              String defaultAlias) {
        super(defaultAlias);
        this.authenticator = authenticator;
        this.targetName = targetName;
        this.tokenManager = tokenManager;
        this.unauthenticatedSubjectService = unauthenticatedSubjectService;
    }

    /*
     * The TSSConfig object is the modal representation of <iiopServerPolicy> configuration, the user specify
     * in server.xml. An example of <iiopServerPolicy> entry is shown here for quick reference.
     * <iiopServerPolicy id="jaykay">
     * --<csiv2 stateful="false">
     * ----<layers>
     * ------<attributeLayer identityAssertionEnabled="true" trustedIdentities="MyHost"/>
     * ------<authenticationLayer establishTrustInClient="Supported" mechanisms="GSSUP,LTPA"/>
     * ------<transportLayer establishTrustInClient="Supported"/>
     * ----</layers>
     * --</csiv2>
     * --<iiopEnpoint host="localhost" iiopPort="123" iiopsPort="321">
     * ----<sslOptions sessionTimeout="1"></sslOptions>
     * --</iiopEnpoint>
     * </iiopServerPolicy>
     */
    public TSSConfig getTSSConfig(Map<String, Object> properties, Map<String, List<TransportAddress>> addrMap) throws Exception {
        TSSConfig tssConfig = new TSSConfig();
        printTrace("IIOP Server Policy", null, 0);

        PolicyData policyData = extractPolicyData(properties, KEY_POLICY, TYPE);

        if (policyData != null) {
            printTrace("CSIV2", null, 1);

            TSSCompoundSecMechListConfig mechListConfig = tssConfig.getMechListConfig();
            mechListConfig.setStateful(policyData.stateful);
            printTrace("Stateful", mechListConfig.isStateful(), 2);

            populateSecMechList(mechListConfig, policyData.layersData, addrMap);
        }
        return tssConfig;
    }

    /*
     * Iterate through each of the "layers" elements to add compound sec mechs to the compound sec mech list.
     */
    private void populateSecMechList(TSSCompoundSecMechListConfig mechListConfig, List<LayersData> layersList,
                                     Map<String, List<TransportAddress>> addrMap) throws Exception {
        for (LayersData mech : layersList) {
            Map<String, TSSCompoundSecMechConfig> tssCompoundSecMechConfigs = extractCompoundSecMech(mech, addrMap);
            Iterator<Entry<String, TSSCompoundSecMechConfig>> entries = tssCompoundSecMechConfigs.entrySet().iterator();
            while (entries.hasNext()) {
                Entry<String, TSSCompoundSecMechConfig> entry = entries.next();
                mechListConfig.add(entry.getValue(), entry.getKey());
            }
        }
    }

    /*
     * The compound security mechanism structure in CORBA specification, looks like
     * 
     * struct CompoundSecMech {
     * ----AssociationOptions target_requires;
     * ----IOP::TaggedComponent transport_mech;
     * ----AS_ContextSec as_context_mech;
     * ----SAS_ContextSec sas_context_mech;
     * };
     * 
     * The sub elements under <layers> corresponds one of transport_mech, AS_ContextSec or SAS_ContextSec.
     * This method extracts the respective security mechanisms.
     */
    protected Map<String, TSSCompoundSecMechConfig> extractCompoundSecMech(LayersData mechInfo, Map<String, List<TransportAddress>> addrMap) throws Exception {
        printTrace("Layers", null, 1);
        /* Get the entries corresponding to attributeLayer, authenticationLayer and transportLayer */
        // Map<String, List<Map<String, Object>>> mechInfo = Nester.nest(mechInfo, KEY_LAYER_TRANSPORT, KEY_LAYER_AUTHENTICATION, KEY_LAYER_ATTRIBUTE);
        Map<String, TSSCompoundSecMechConfig> compoundSecMechConfigs = new LinkedHashMap<String, TSSCompoundSecMechConfig>();
        // TODO: Make temporal coupling explicit.  Setting authentication layer will create the compoundSecMechConfigs that the other layers need to fill up.
        setAuthenticationLayerConfig(compoundSecMechConfigs, mechInfo);
        setTransportLayerConfig(compoundSecMechConfigs, mechInfo, addrMap);
        setAttributeLayerConfig(compoundSecMechConfigs, mechInfo);
        return compoundSecMechConfigs;
    }

    /*
     * This method populates the transport_mech structure. Referring to the CORBA spec on transport_mech, you can see that
     * it could be one of TAG_NULL_TAG, TAG_TLS_SEC_TRANS or TAG_SECIOP_SEC_TRANS. Out of this we support only TAG_NULL_TAG and
     * TAG_TLS_SEC_TRANS (for now).
     * We check the presence of <transportLayer>, and assume that, the contents are meant for TAG_TLS_SEC_TRANS (ie. for TLS/SSL transport)
     * If the <transportLayer> is not present, we default that to TAG_NULL_TAG
     */
    private void setTransportLayerConfig(Map<String, TSSCompoundSecMechConfig> compoundSecMechConfigs, LayersData mechInfo,
                                         Map<String, List<TransportAddress>> addrMap) throws SSLException {
        Map<String, Object> transportLayerProperties = mechInfo.transportLayer;
        TSSTransportMechConfig tssTransportMechConfig;

        if (transportLayerProperties != null) {
            printTrace("Transport Layer", null, 2);
            boolean sslEnabled = (Boolean) transportLayerProperties.get(KEY_SSL_ENABLED);
            String sslAliasName = (String) transportLayerProperties.get(KEY_SSL_REF);
            if (sslEnabled && "".equals(sslAliasName) == false) {
                tssTransportMechConfig = extractSSLTransport(transportLayerProperties, addrMap);
            } else {
                tssTransportMechConfig = new TSSNULLTransportConfig();
            }
        } else {
            /* If no configuration for CSIv2 transport layer. Continue with default or NULL transport configuration. */
            tssTransportMechConfig = new TSSNULLTransportConfig();
        }

        Iterator<Entry<String, TSSCompoundSecMechConfig>> entries = compoundSecMechConfigs.entrySet().iterator();
        while (entries.hasNext()) {
            TSSCompoundSecMechConfig compoundSecMechConfig = entries.next().getValue();
            compoundSecMechConfig.setTransport_mech(tssTransportMechConfig);
        }
    }

    /*
     * This method extract the fields corresponding to TAG_TLS_SEC_TRANS. Referring to CORBA spec, this corresponds to a
     * TaggedComponent, and have structure as below.
     * 
     * struct TLS_SEC_TRANS {
     * ----AssociationOptions target_supports;
     * ----AssociationOptions target_requires;
     * ----TransportAddressList addresses;
     * };
     * 
     * and
     * 
     * struct TransportAddress {
     * ----string host_name;
     * ----unsigned short port;
     * };
     */
    private TSSTransportMechConfig extractSSLTransport(Map<String, Object> transportLayerProperties, Map<String, List<TransportAddress>> addrMap) throws SSLException {
        String sslAliasName = (String) transportLayerProperties.get(KEY_SSL_REF);
        if (sslAliasName == null)
            sslAliasName = defaultAlias;
        OptionsKey options = SecurityServices.getSSLConfig().getAssociationOptions(sslAliasName);
        TSSSSLTransportConfig transportLayerConfig = new TSSSSLTransportConfig(authenticator);
        transportLayerConfig.setSupports(options.supports);
        transportLayerConfig.setRequires(options.requires);

        List<TransportAddress> addresses = addrMap.get(sslAliasName);

        if (addresses == null) {
            if (addrMap.size() == 1) {
                String messageFromBundle = Tr.formatMessage(tc, "CSIv2_SERVER_TRANSPORT_NO_SSL_CONFIGS_IN_IIOP_ENDPOINT", sslAliasName);
                throw new IllegalStateException(messageFromBundle);
            } else {
                String messageFromBundle = Tr.formatMessage(tc, "CSIv2_SERVER_TRANSPORT_MISMATCHED_SSL_CONFIG", sslAliasName);
                throw new IllegalStateException(messageFromBundle);
            }
        }
        transportLayerConfig.setTransportAddresses(addresses);
        return transportLayerConfig;
    }

    private void setAuthenticationLayerConfig(Map<String, TSSCompoundSecMechConfig> compoundSecMechConfigs, LayersData mechInfo) {
        Map<String, Object> authenticationLayerProperties = mechInfo.authenticationLayer;

        if (authenticationLayerProperties != null) {
            printTrace("Authentication Layer", null, 2);
            Map<String, TSSASMechConfig> tssasMechConfigs = extractASMech(authenticationLayerProperties);

            Iterator<Entry<String, TSSASMechConfig>> entries = tssasMechConfigs.entrySet().iterator();
            while (entries.hasNext()) {
                Entry<String, TSSASMechConfig> entry = entries.next();
                TSSCompoundSecMechConfig compoundSecMechConfig = new TSSCompoundSecMechConfig();
                compoundSecMechConfig.setAs_mech(entry.getValue());
                compoundSecMechConfigs.put(entry.getKey(), compoundSecMechConfig);
            }
        } else {
            /* If no configuration for CSIv2 authentication layer, then continue with default or NULL authentication configuration. */
            TSSCompoundSecMechConfig compoundSecMechConfig = new TSSCompoundSecMechConfig();
            compoundSecMechConfig.setAs_mech(new TSSNULLASMechConfig());
            compoundSecMechConfigs.put(TSSNULLASMechConfig.NULL_OID, compoundSecMechConfig);
        }
    }

    /*
     * This method creates a collection of AuthenticationServices configuration, as List<TSSASMechConfig>
     * A typical configuration will look like
     * <authenticationLayer mechanisms="GSSUP,LTPA" establishTrustInClient="Supported"/>
     * One TSSASMechConfig object is created for each of the supported mechanisms. In this example, a List<> is created
     * which contains two TSSASMechConfig objects, one corresponding to GSSUP and the other one corresponding to LTPA.
     */
    private Map<String, TSSASMechConfig> extractASMech(Map<String, Object> authenticationLayerProperties) {
        Map<String, TSSASMechConfig> tssasMechConfigs = new LinkedHashMap<String, TSSASMechConfig>();
        List<String> configuredMechanisms = new ArrayList<String>();

        String establishTrustInClient = (String) authenticationLayerProperties.get(KEY_ESTABLISH_TRUST_IN_CLIENT);
        boolean required = false;
        if (OPTION_REQUIRED.equals(establishTrustInClient)) {
            required = true;
        } else if (OPTION_NEVER.equals(establishTrustInClient)) {
            Tr.warning(tc, "CSIv2_COMMON_AUTH_LAYER_DISABLED", establishTrustInClient);
            tssasMechConfigs.put(TSSNULLASMechConfig.NULL_OID, new TSSNULLASMechConfig());
            return tssasMechConfigs;
        }

        List<String> mechanisms = getAsMechanisms(authenticationLayerProperties);
        if (mechanisms.isEmpty()) {
            Tr.warning(tc, "CSIv2_SERVER_AUTH_MECHANISMS_NULL");
            tssasMechConfigs.put(TSSNULLASMechConfig.NULL_OID, new TSSNULLASMechConfig());
        } else {
            for (String mech : mechanisms) {
                if (!configuredMechanisms.contains(mech.toUpperCase())) {
                    if (mech.equalsIgnoreCase(AUTHENTICATION_MECHANISM_LTPA)) {
                        tssasMechConfigs.put(ServerLTPAMechConfig.LTPA_OID, new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required));
                    } else if (mech.equalsIgnoreCase(AUTHENTICATION_MECHANISM_GSSUP)) {
                        tssasMechConfigs.put(GSSUPMechOID.value, new TSSGSSUPMechConfig(authenticator, targetName, required));
                    } else {
                        Tr.warning(tc, "CSIv2_SERVER_AUTH_MECHANISM_INVALID", mech);
                    }
                    configuredMechanisms.add(mech.toUpperCase());
                }
            }
            if (tssasMechConfigs.isEmpty()) {
                tssasMechConfigs.put(TSSNULLASMechConfig.NULL_OID, new TSSNULLASMechConfig());
            }
        }

        return tssasMechConfigs;
    }

    private void setAttributeLayerConfig(Map<String, TSSCompoundSecMechConfig> compoundSecMechConfigs, LayersData mechInfo) {
        TSSSASMechConfig sasMechConfig = null;
        Map<String, Object> attributeLayerProperties = mechInfo.attributeLayer;

        if (attributeLayerProperties != null) {
            printTrace("Attribute Layer", null, 2);
            sasMechConfig = extractSASMech(attributeLayerProperties);
        } else {
            /* If no configuration for CSIv2 attribute layer, then continue with default ITTAbsent configuration. */
            sasMechConfig = new TSSSASMechConfig(new TrustedIDEvaluatorImpl());
            sasMechConfig.addIdentityToken(new TSSITTAbsent());
        }

        Iterator<Entry<String, TSSCompoundSecMechConfig>> entries = compoundSecMechConfigs.entrySet().iterator();
        while (entries.hasNext()) {
            TSSCompoundSecMechConfig compoundSecMechConfig = entries.next().getValue();
            compoundSecMechConfig.setSas_mech(sasMechConfig);
        }
    }

    private TSSSASMechConfig extractSASMech(Map<String, Object> attributeLayerProperties) {
        boolean identityAssertionEnabled = (Boolean) attributeLayerProperties.get(KEY_IDENTITY_ASSERTION_ENABLED);
        String[] identityAssertionTypes = (String[]) attributeLayerProperties.get(KEY_IDENTITY_ASSERTION_TYPES);
        String pipeSeparatedTrustedIdentities = (String) attributeLayerProperties.get(KEY_TRUSTED_IDENTITIES);
        TrustedIDEvaluator trustedIDEvaluator = new TrustedIDEvaluatorImpl(pipeSeparatedTrustedIdentities);
        TSSSASMechConfig sasMechConfig = new TSSSASMechConfig(trustedIDEvaluator);

        // ITTAbsent must be supported always.
        sasMechConfig.addIdentityToken(new TSSITTAbsent());

        if (identityAssertionEnabled) {
            for (String assertionType : identityAssertionTypes) {
                if (TSSSASMechConfig.TYPE_ITTAnonymous.equals(assertionType)) {
                    sasMechConfig.addIdentityToken(new TSSITTAnonymous(unauthenticatedSubjectService));
                } else if (TSSSASMechConfig.TYPE_ITTPrincipalName.equals(assertionType)) {
                    sasMechConfig.addIdentityToken(new TSSITTPrincipalNameGSSUP(authenticator, targetName));
                } else if (TSSSASMechConfig.TYPE_ITTX509CertChain.equals(assertionType)) {
                    sasMechConfig.addIdentityToken(new TSSITTX509CertChain(authenticator));
                } else if (TSSSASMechConfig.TYPE_ITTDistinguishedName.equals(assertionType)) {
                    sasMechConfig.addIdentityToken(new TSSITTDistinguishedName(authenticator));
                }
            }
        }

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

}
