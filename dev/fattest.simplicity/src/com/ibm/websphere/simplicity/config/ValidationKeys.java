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
 * Represents the <validationKeys> element which is embedded inside <ltpa> element in server.xml
 */

public class ValidationKeys extends ConfigElement {

    // Atrribute to specify the name of the validation key file
    @XmlAttribute
    public String fileName;

    // Atrribute to specify the password for the validation key file
    @XmlAttribute
    public String password;

    // Atrribute to specify the expiration value for the validation key file
    @XmlAttribute
    public String validUntilDate;

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // Attributes
        buf.append("fileName=").append(fileName);
        buf.append(", password=").append(password);
        buf.append(", validUntilDate=").append(validUntilDate);
        buf.append('}');
        return buf.toString();
    }
    
}
