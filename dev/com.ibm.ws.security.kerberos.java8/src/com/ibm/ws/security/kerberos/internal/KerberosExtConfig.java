/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.kerberos.internal;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Represents security configurable options for SPNEGO web.
 */
public class KerberosExtConfig {

    private static final TraceComponent tc = Tr.register(KerberosExtConfig.class);

    public static final String KEY_ID = "id";
    public static final String KEY_S4U2SELF_ENABLED = "s4U2selfEnabled";
    public static final String KEY_S4U2PROXY_ENABLED = "s4U2proxyEnabled";

    private String id;
    private boolean s4U2selfEnabled;
    private boolean s4U2proxyEnabled;

    /**
     * @param props
     */
    public KerberosExtConfig(Map<String, Object> props) {
        if (props == null || props.isEmpty())
            return;
        id = (String) props.get(KEY_ID);
        s4U2selfEnabled = (Boolean) props.get(KEY_S4U2SELF_ENABLED);
        s4U2proxyEnabled = (Boolean) props.get(KEY_S4U2PROXY_ENABLED);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "id: " + id);
            Tr.debug(tc, "s4U2selfEnabled: " + s4U2selfEnabled);
            Tr.debug(tc, "s4U2proxyEnabled: " + s4U2proxyEnabled);
        }
    }

    public boolean isS4U2selfEnable() {
        return s4U2selfEnabled;
    }

    public boolean isS4U2proxyEnable() {
        return s4U2proxyEnabled;
    }
}
