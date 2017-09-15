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
package com.ibm.ws.security.csiv2.server.config.css;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.config.css.CommonClientCfg;
import com.ibm.ws.security.csiv2.server.TraceConstants;
import com.ibm.ws.transport.iiop.security.config.css.CSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSGSSUPMechConfigDynamic;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASITTAbsent;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASITTAnonymous;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASITTPrincipalNameDynamic;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSSSLTransportConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSTransportMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.OptionsKey;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * Client config helper class reads the iiopClientPolicy configuration from server.xml and maps to CSSConfig.
 */
public class ClientConfigHelper extends CommonClientCfg {

    private static TraceComponent tc = Tr.register(ClientConfigHelper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final String TYPE = "com.ibm.ws.security.csiv2.clientPolicyCSIV2";

    public ClientConfigHelper(Authenticator authenticator, String domain, String defaultAlias) {
        super(authenticator, domain, defaultAlias, TYPE);
    }

    @Override
    protected CSSTransportMechConfig extractSSLTransport(Map<String, Object> transportLayerProperties) throws SSLException {
        String sslRef = (String) transportLayerProperties.get(KEY_SSL_REF);
        CSSSSLTransportConfig transportLayerConfig = new CSSSSLTransportConfig();
        if (sslRef != null) {
            OptionsKey options = sslConfig.getAssociationOptions(sslRef);
            transportLayerConfig.setSupports(options.supports);
            transportLayerConfig.setRequires(options.requires);
            transportLayerConfig.setSslConfigName(sslRef);
        } else {
            transportLayerConfig.setOutboundSSLReference();
        }
        return transportLayerConfig;
    }

    @Override
    public CSSASMechConfig handleASMech(String mech, Authenticator authenticator, String domain, boolean required, Map<String, Object> props) {
        CSSASMechConfig config = null;
        if (mech.equalsIgnoreCase(AUTHENTICATION_MECHANISM_LTPA)) {
            config = new ClientLTPAMechConfig(authenticator, domain, required);
        } else if (mech.equalsIgnoreCase(AUTHENTICATION_MECHANISM_GSSUP)) {
            config = new CSSGSSUPMechConfigDynamic(domain, required);
        }
        return config;
    }

    @Override
    public void logWarning(String messageKey, Object... objs) {
        Tr.warning(tc, messageKey, objs);
    }

    @Override
    public Map<String, Object> getAttributeLayerProperties(LayersData mechInfo) {
        return mechInfo.attributeLayer;
    }

    @Override
    protected CSSSASMechConfig extractSASMech(Map<String, Object> attributeLayerProperties) {
        CSSSASMechConfig sasMechConfig = new CSSSASMechConfig();
        // ITTAbsent must be supported always.
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());

        boolean identityAssertionEnabled = (Boolean) attributeLayerProperties.get(KEY_IDENTITY_ASSERTION_ENABLED);
        String[] identityAssertionTypes = (String[]) attributeLayerProperties.get(KEY_IDENTITY_ASSERTION_TYPES);
        String trustedIdentity = (String) attributeLayerProperties.get(KEY_TRUSTED_IDENTITY);
        SerializableProtectedString trustedPassword = (SerializableProtectedString) attributeLayerProperties.get(KEY_TRUSTED_PASSWORD);

        printTrace("IdentityAssertionEnabled", identityAssertionEnabled, 3);
        printTrace("TrustedIdentity", trustedIdentity, 3);

        if (identityAssertionEnabled) {
            /*
             * The order of checking the type is Anonymous, Principal Name, Cert chain, Distinguished name. This order
             * is maintained to be consistent with the CSIv2 spec.
             */
            for (String assertionType : identityAssertionTypes) {
                if (CSSSASMechConfig.TYPE_ITTAnonymous.equals(assertionType)) {
                    sasMechConfig.addIdentityToken(new CSSSASITTAnonymous());
                } else if (CSSSASMechConfig.TYPE_ITTPrincipalName.equals(assertionType)) {
                    sasMechConfig.addIdentityToken(new CSSSASITTPrincipalNameDynamic(null, domain));
                } else if (CSSSASMechConfig.TYPE_ITTX509CertChain.equals(assertionType)) {
                    sasMechConfig.addIdentityToken(new ClientSASITTX509CertChain(null, domain));
                } else if (CSSSASMechConfig.TYPE_ITTDistinguishedName.equals(assertionType)) {
                    sasMechConfig.addIdentityToken(new ClientSASITTDistinguishedName());
                }
            }

            sasMechConfig.setTrustedIdentity(trustedIdentity);
            sasMechConfig.setTrustedPassword(trustedPassword);
        }

        return sasMechConfig;
    }

}
