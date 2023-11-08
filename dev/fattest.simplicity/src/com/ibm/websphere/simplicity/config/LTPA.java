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
 * Represents the <ltpa> element in server.xml
 */
public class LTPA extends ConfigElement {

    // Atrribute to specify the name of the file that contains the primary keys
    @XmlAttribute
    public String keysFileName;

    // Atrribute to specify the expiration value for the primary keys
    @XmlAttribute
    public String expiration;

    // Atrribute to specify the password for the primary keys
    @XmlAttribute
    public String password;

    // Atrribute to toggle monitoring of the keys within the directory
    @XmlAttribute
    public String monitorValidationKeysDir;

    // Atrribute to specify the interval for monitoring the keys within the directory
    @XmlAttribute
    public String monitorInterval;

    // Atrribute to specify the update trigger for the keys within the directory
    @XmlAttribute
    public String updateTrigger;

    // Inherit ValidationKeys
    @XmlElement(name = "validationKeys")
    private ConfigElementList<ValidationKeys> validationKeys;

    /**
     * Retrieves the validation keys for this ltpa configuration
     *
     * @return the validation keys for this ltpa configuration
     */
    public ConfigElementList<ValidationKeys> getValidationKeys() {
        if (this.validationKeys == null) {
            this.validationKeys = new ConfigElementList<ValidationKeys>();
        }
        return this.validationKeys;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // Attributes
        buf.append("keysFileName=").append(keysFileName);
        buf.append(", expiration=").append(expiration);
        buf.append(", password=").append(password);
        buf.append(", monitorValidationKeysDir=").append(monitorValidationKeysDir);
        buf.append(", monitorInterval=").append(monitorInterval);
        buf.append(", updateTrigger=").append(updateTrigger);

        // Elements
        if (this.validationKeys != null)
            for (ValidationKeys validationKey : this.validationKeys)
                buf.append(validationKey.toString() + ",");
        buf.append('}');

        return buf.toString();
    }
}
