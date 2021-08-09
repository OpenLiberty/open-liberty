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
@XmlType(name = "securityPermissionType", propOrder = { "description", "securityPermissionSpec" })
public class Ra10SecurityPermission {

    @XmlElement(name = "description")
    private String description;
    @XmlElement(name = "security-permission-spec", required = true)
    private String securityPermissionSpec;

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the securityPermissionSpec
     */
    public String getSecurityPermissionSpec() {
        return securityPermissionSpec;
    }

}
