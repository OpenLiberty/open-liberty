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

public enum ResourceTypeLabel {
    PRODUCTSAMPLE("Product Sample"),
    OPENSOURCE("Open Source Integration"),
    INSTALL("Product"),
    ADDON("Addon"),
    FEATURE("Feature"),
    IFIX("iFix"),
    ADMINSCRIPT("Admin Script"),
    CONFIGSNIPPET("Config Snippet"),
    TOOL("Tool");

    private final String _label;

    ResourceTypeLabel(String label) {
        _label = label;
    }

    public String getValue() {
        return _label;
    }

    public static ResourceTypeLabel forValue(String value) {
        for (ResourceTypeLabel lab : ResourceTypeLabel.values()) {
            if (lab.getValue().equals(value)) {
                return lab;
            }
        }
        return null;
    }
}
