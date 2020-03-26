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

import com.ibm.ws.security.authentication.tai.TAIConfig;

/**
 * Represents security configurable options for trustAssociation element
 */
public class TAIConfigImpl implements TAIConfig {
    // trustAssociation element attributes
    static final String KEY_INVOKE_FOR_UNPROTECTED_URI = "invokeForUnprotectedURI";
    static final String KEY_INVOKE_FOR_FORM_LOGIN = "invokeForFormLogin";
    static final String KEY_FAIL_OVER_TO_APP_AUTH_TYPE = "failOverToAppAuthType";
    static final String KEY_DISABLE_LTPA_COOKIE = "disableLtpaCookie";

    private boolean failOverToAppAuthType = false;
    private boolean invokeForUnprotectedURI = false;
    private boolean invokeForFormLogin = false;
    private boolean disableLtpaCookie = false;

    public TAIConfigImpl(Map<String, Object> props) {
        processConfig(props);
    }

    /**
     * @param props
     */
    void processConfig(Map<String, Object> props) {
        if (props == null)
            return;
        invokeForUnprotectedURI = (Boolean) props.get(KEY_INVOKE_FOR_UNPROTECTED_URI);
        invokeForFormLogin = (Boolean) props.get(KEY_INVOKE_FOR_FORM_LOGIN);
        failOverToAppAuthType = (Boolean) props.get(KEY_FAIL_OVER_TO_APP_AUTH_TYPE);
        disableLtpaCookie = (Boolean) props.get(KEY_DISABLE_LTPA_COOKIE);
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
}
