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
 * Represents the <kerberos> element in server.xml
 */
public class Kerberos extends ConfigElement {

    @XmlAttribute
    public String keytab;

    @XmlAttribute
    public String configFile;

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // attributes
        buf.append("keytab=").append(keytab).append(", ");
        buf.append("configFile=").append(configFile);

        buf.append('}');
        return buf.toString();
    }
}
