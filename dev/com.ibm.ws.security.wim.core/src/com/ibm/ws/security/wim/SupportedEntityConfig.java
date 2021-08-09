/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim;

/**
 *
 */
public class SupportedEntityConfig {

    private final String defaultParent;
    private final String[] rdnProperties;

    /**
     * @param defaultParent
     * @param rdnProperties
     */
    public SupportedEntityConfig(String defaultParent, String[] rdnProperties) {
        this.defaultParent = defaultParent;
        this.rdnProperties = rdnProperties;
    }

    /**
     * @return the defaultParent
     */
    public String getDefaultParent() {
        return defaultParent;
    }

    /**
     * @return the rdnProperties
     */
    public String[] getRdnProperties() {
        return rdnProperties;
    }

}
