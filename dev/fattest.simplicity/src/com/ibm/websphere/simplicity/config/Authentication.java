/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Represents the <authentication> element in server.xml
 */
public class Authentication extends ConfigElement {
    // Atrribute to specify the id type for authentication
    @XmlAttribute
    public String id;

    // Atrribute to specify the cacheEnabled value for authentication
    @XmlAttribute
    public String cacheEnabled;

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // Attributes
        buf.append("id=").append(id);
        buf.append(", cacheEnabled=").append(cacheEnabled);

        return buf.toString();
    }
}


