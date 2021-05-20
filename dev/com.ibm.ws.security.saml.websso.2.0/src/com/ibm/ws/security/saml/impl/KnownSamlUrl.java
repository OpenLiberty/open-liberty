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
package com.ibm.ws.security.saml.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.TraceConstants;

/**
 *
 */
public class KnownSamlUrl {
    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(KnownSamlUrl.class,
                                                         TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);

    public static final String SAML_CONTEXT_PATH = "/ibm/saml20";
    public static final String IBM_JMX_CONNECTOR_REST = "/IBMJMXConnectorREST";
    public static final String REGEX_COMPONENT_ID = "/([\\w-]+)/";
    public static final String PATH_KNOWN_SAML_URL = "(acs|samlmetadata|slo)";
    public static final String SLASH_PATH_KNWON_SAML_URL = "/" + PATH_KNOWN_SAML_URL;
    private static final Pattern PATTERN_KNOWN_SAML_URL = Pattern.compile("^" + REGEX_COMPONENT_ID + PATH_KNOWN_SAML_URL + "$");

    // path cannot be null
    public static Matcher matchKnownSamlUrl(String path) {
        synchronized (PATTERN_KNOWN_SAML_URL) {
            return PATTERN_KNOWN_SAML_URL.matcher(path);
        }
    }
}
