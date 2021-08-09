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

package com.ibm.ws.repository.common.enums;

public enum ResourceType {
    /**
     * Samples that only use product code
     */
    PRODUCTSAMPLE(ResourceTypeLabel.PRODUCTSAMPLE, "com.ibm.websphere.ProductSample", "samples"),
    /**
     * Samples that also use open source code
     */
    OPENSOURCE(ResourceTypeLabel.OPENSOURCE, "com.ibm.websphere.OpenSource", "opensource"),
    /**
     * A liberty install jar file
     */
    INSTALL(ResourceTypeLabel.INSTALL, "com.ibm.websphere.Install", "runtimes"),
    /**
     * Extended functionality for the install
     */
    ADDON(ResourceTypeLabel.ADDON, "com.ibm.websphere.Addon", "addons"),
    /**
     * ESA's representing features
     */
    FEATURE(ResourceTypeLabel.FEATURE, "com.ibm.websphere.Feature", "features"),
    /**
     * Ifixes
     */
    IFIX(ResourceTypeLabel.IFIX, "com.ibm.websphere.Ifix", "ifixes"),
    /**
     * AdminScripts
     */
    ADMINSCRIPT(ResourceTypeLabel.ADMINSCRIPT, "com.ibm.websphere.AdminScript", "scripts"),
    /**
     * Config snippets
     */
    CONFIGSNIPPET(ResourceTypeLabel.CONFIGSNIPPET, "com.ibm.websphere.ConfigSnippet", "snippets"),
    /**
     * Tools
     */
    TOOL(ResourceTypeLabel.TOOL, "com.ibm.websphere.Tool", "tools");

    private final ResourceTypeLabel _typeLabel;
    private final String _type;
    private final String _nameForUrl;

    private ResourceType(ResourceTypeLabel label, String type, String nameForUrl) {
        _typeLabel = label;
        _type = type;
        _nameForUrl = nameForUrl;
    }

    public ResourceTypeLabel getTypeLabel() {
        return _typeLabel;
    }

    public String getURLForType() {
        return _nameForUrl;
    }

    // get the long name of a the type ie for FEATURE returns "com.ibm.websphere.Feature"
    public String getValue() {
        return _type;
    }

    public static ResourceType forValue(String value) {
        for (ResourceType ty : ResourceType.values()) {
            if (ty.getValue().equals(value)) {
                return ty;
            }
        }
        return null;
    }
}
