/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oauth20.internal.config;

import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;

import io.openliberty.security.common.config.SpecificEndpointSettings;

@SuppressWarnings("restriction")
public class SpecificOAuthEndpointSettings extends SpecificEndpointSettings {

    protected EndpointType endpoint = null;

    public SpecificOAuthEndpointSettings(EndpointType endpointType) {
        super(String.valueOf(endpointType));
        this.endpoint = endpointType;
    }

    public EndpointType getEndpointType() {
        return endpoint;
    }

    @Override
    public String toString() {
        return "[SpecificOAuthEndpointSettings: " + endpoint + ", Supported HTTP methods: " + supportedHttpMethods + "]";
    }

}
