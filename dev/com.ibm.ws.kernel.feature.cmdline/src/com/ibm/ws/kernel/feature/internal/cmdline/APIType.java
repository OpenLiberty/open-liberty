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
package com.ibm.ws.kernel.feature.internal.cmdline;

import com.ibm.ws.kernel.feature.provisioning.FeatureResource;

public enum APIType {
    API("dev/api", "apiJar"),
    SPI("dev/spi", "spiJar");

    public final String prefix;
    public final String attribute;

    APIType(String prefix, String attribute) {
        this.prefix = prefix;
        this.attribute = attribute;
    }

    public boolean matches(FeatureResource resource) {
        String attrValue = resource.getAttributes().get(attribute);
        return attrValue == null || Boolean.parseBoolean(attrValue);
    }

    public String getElementName() {
        // The feature list element name is the same as the feature manifest
        // attribute name.
        return attribute;
    }

    public static APIType getAPIType(FeatureResource resource) {
        String location = resource.getLocation();
        if (location != null) {
            location = location.trim();

            for (APIType apiType : APIType.values()) {
                if (location.startsWith(apiType.prefix)) {
                    return apiType.matches(resource) ? apiType : null;
                }
            }
        }

        return null;
    }
}
