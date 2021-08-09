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
package com.ibm.ws.security.sso.common;

import java.util.Map;

public interface SsoService {
    public static final String KEY_TYPE = "type";
    public static final String TYPE_WSS_SAML = "wssSaml";
    public static final String TYPE_WSSECURITY = "wssecurity";
    public static final String TYPE_SAML20 = "saml20";

    // Defined in wssecurity
    public static final String WSSEC_SAML_ASSERTION = "wssecurity-samlassertion";
    public static final String SAML_SSO_TOKEN = "samlssotoken";

    public Map<String, Object> handleRequest(String requestName,
                                             Map<String, Object> requestContext) throws Exception;

}
