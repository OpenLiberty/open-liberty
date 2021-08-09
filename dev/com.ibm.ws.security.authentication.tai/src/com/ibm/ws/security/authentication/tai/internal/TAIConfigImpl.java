/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.tai.internal;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.tai.TAIConfig;

/**
 * Represents security configurable options for trustAssociation element
 */
public class TAIConfigImpl implements TAIConfig {
    private static final TraceComponent tc = Tr.register(TAIConfigImpl.class);
    // trustAssociation element attributes
    static final String KEY_INVOKE_FOR_UNPROTECTED_URI = "invokeForUnprotectedURI";
    static final String KEY_INVOKE_FOR_FORM_LOGIN = "invokeForFormLogin";
    static final String KEY_FAIL_OVER_TO_APP_AUTH_TYPE = "failOverToAppAuthType";
    public static final String KEY_DISABLE_LTPA_COOKIE = "disableLtpaCookie";
    public static final String KEY_INITIALIZE_AT_FIRST_REQUEST = "initializeAtFirstRequest";

    private boolean failOverToAppAuthType = false;
    private boolean invokeForUnprotectedURI = false;
    private boolean invokeForFormLogin = false;
    private boolean disableLtpaCookie = false;
    private boolean initializeAtFirstRequest = false;
    private String id = null;
    static final String KEY_ID = "id";

    public TAIConfigImpl(Map<String, Object> props) {
        processConfig(props);
    }

    /**
     * @param props
     */
    void processConfig(Map<String, Object> props) {
        if (props == null)
            return;
        id = (String) props.get(KEY_ID);
        invokeForUnprotectedURI = (Boolean) props.get(KEY_INVOKE_FOR_UNPROTECTED_URI);
        invokeForFormLogin = (Boolean) props.get(KEY_INVOKE_FOR_FORM_LOGIN);
        failOverToAppAuthType = (Boolean) props.get(KEY_FAIL_OVER_TO_APP_AUTH_TYPE);
        disableLtpaCookie = (Boolean) props.get(KEY_DISABLE_LTPA_COOKIE);
        initializeAtFirstRequest = (Boolean) props.get(KEY_INITIALIZE_AT_FIRST_REQUEST);
        printTaiConfig();
    }

    /**
     *
     */
    public void printTaiConfig() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "TAI configuration ID: " + id);
            Tr.debug(tc, "  invokeForUnprotectedURI=" + invokeForUnprotectedURI + " invokeForFormLogin=" + invokeForFormLogin + " failOverToAppAuthType=" + failOverToAppAuthType
                         + " disableLtpaCookie=" + disableLtpaCookie);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInvokeForFormLogin() {
        return invokeForFormLogin;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFailOverToAppAuthType() {
        return failOverToAppAuthType;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInvokeForUnprotectedURI() {
        return invokeForUnprotectedURI;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDisableLtpaCookie() {
        return disableLtpaCookie;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInitializeAtFirstRequest() {
        return initializeAtFirstRequest;
    }

}
