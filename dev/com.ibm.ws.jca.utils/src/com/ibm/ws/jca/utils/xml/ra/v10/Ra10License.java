/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.xml.ra.v10;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 */
@XmlType(name = "licenseType", propOrder = { "description", "licenseRequired" })
public class Ra10License {

    @XmlElement(name = "description")
    private String description;
    @XmlElement(name = "license-required", required = true)
    private String licenseRequired;

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the licenseRequired
     */
    public String getLicenseRequired() {
        return licenseRequired;
    }

}
