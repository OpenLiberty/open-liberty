/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.common.config;

import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.security.common.http.SupportedHttpMethodHandler.HttpMethod;

public class SpecificEndpointSettings {

    private static final TraceComponent tc = Tr.register(SpecificEndpointSettings.class);

    protected String endpointName = null;
    protected Set<HttpMethod> supportedHttpMethods = new HashSet<HttpMethod>();

    public SpecificEndpointSettings(String endpointName) {
        this.endpointName = endpointName;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setSupportedHttpMethods(String... supportedHttpMethods) {
        this.supportedHttpMethods = new HashSet<HttpMethod>();
        if (supportedHttpMethods != null) {
            for (String method : supportedHttpMethods) {
                if (method == null) {
                    continue;
                }
                if (isSpecialHttpMethodCase(method)) {
                    addSupportedMethodsForSpecialCase(method);
                    break;
                } else {
                    addStandardHttpMethod(method);
                }
            }
        }
    }

    boolean isSpecialHttpMethodCase(String method) {
        return "all".equalsIgnoreCase(method) || "none".equalsIgnoreCase(method);
    }

    void addSupportedMethodsForSpecialCase(String method) {
        if ("all".equalsIgnoreCase(method)) {
            for (HttpMethod httpMethod : HttpMethod.values()) {
                this.supportedHttpMethods.add(httpMethod);
            }
        } else if ("none".equalsIgnoreCase(method)) {
            // Don't add any supported HTTP methods
            this.supportedHttpMethods = new HashSet<HttpMethod>();
        }
    }

    @FFDCIgnore(IllegalArgumentException.class)
    void addStandardHttpMethod(String method) {
        try {
            HttpMethod convertedMethod = HttpMethod.valueOf(method.toUpperCase());
            this.supportedHttpMethods.add(convertedMethod);
        } catch (IllegalArgumentException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception trying to add supported HTTP method: " + e);
            }
        }
    }

    public Set<HttpMethod> getSupportedHttpMethods() {
        return supportedHttpMethods;
    }

    @Override
    public String toString() {
        return "[Endpoint: " + endpointName + ", Supported HTTP methods: " + supportedHttpMethods + "]";
    }

}
