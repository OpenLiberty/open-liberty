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

/**
 * Represents the <ltpa> element in server.xml
 */
public class LTPA extends ConfigElement {

    @XmlAttribute
    public String monitorDirectory;
    @XmlAttribute
    public String monitorInterval;

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // attributes
        buf.append("monitorDirectory=").append(monitorDirectory);
        buf.append(", monitorInterval=").append(monitorInterval);
        buf.append('}');
        return buf.toString();
    }
}
