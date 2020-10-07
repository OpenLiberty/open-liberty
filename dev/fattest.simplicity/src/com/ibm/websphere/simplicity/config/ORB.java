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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * ORB configuration element:
 * com.ibm.ws.transport.iiop/resources/OSGI-INF/metatype/metatype.xml
 */
public class ORB extends ConfigElement {

    private String orbSSLInitTimeout;

    /**
     * @return the orbSSLInitTimeout
     */
    public String getOrbSSLInitTimeout() {
        return orbSSLInitTimeout;
    }

    /**
     * @param orbSSLInitTimeout the orbSSLInitTimeout to set
     */
    @XmlAttribute(name = "orbSSLInitTimeout")
    public void setOrbSSLInitTimeout(String orbSSLInitTimeout) {
        this.orbSSLInitTimeout = orbSSLInitTimeout;
    }

}
