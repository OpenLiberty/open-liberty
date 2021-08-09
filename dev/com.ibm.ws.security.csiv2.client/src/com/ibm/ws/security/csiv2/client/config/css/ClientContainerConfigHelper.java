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
package com.ibm.ws.security.csiv2.client.config.css;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.client.TraceConstants;
import com.ibm.ws.security.csiv2.config.css.CommonClientCfg;
import com.ibm.ws.transport.iiop.security.config.css.CSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSGSSUPMechConfigDynamic;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * Client config helper class reads the iiopClientPolicy configuration from server.xml and maps to CSSConfig.
 */
public class ClientContainerConfigHelper extends CommonClientCfg {

    private static TraceComponent tc = Tr.register(ClientContainerConfigHelper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final String TYPE = "com.ibm.ws.security.csiv2.clientContainer.clientPolicyCSIV2";

    public ClientContainerConfigHelper(String defaultAlias) {
        super(null, null, defaultAlias, TYPE);
    }

    public CSSASMechConfig handleASMech(String mech, Authenticator authenticator, String domain, boolean required, Map<String, Object> props) {
        CSSASMechConfig config = null;
        if (mech.equalsIgnoreCase(AUTHENTICATION_MECHANISM_GSSUP)) {
            String realm = (String) props.get(KEY_AUTHENTICATION_REALM);
            String user = (String) props.get(KEY_AUTHENTICATION_USER);
            SerializableProtectedString password = (SerializableProtectedString) props.get(KEY_AUTHENTICATION_PASSWORD);
            if (user == null || password == null) {
                String missing = ((user == null) ? (KEY_AUTHENTICATION_USER + " ") : "")
                                 + ((password == null) ? KEY_AUTHENTICATION_PASSWORD : "");
                Tr.warning(tc, "CSIv2_CLIENT_MISSING_AUTH_MECH_PROPERTIES", missing);
            }
            config = new CSSGSSUPMechConfigDynamic(user, password, realm, required);

        }
        return config;
    }

    public void logWarning (String messageKey, Object... objs) {
        Tr.warning(tc, messageKey, objs);
    }

    public Map<String, Object> getAttributeLayerProperties(LayersData mechInfo) {
        return null;
    }
}