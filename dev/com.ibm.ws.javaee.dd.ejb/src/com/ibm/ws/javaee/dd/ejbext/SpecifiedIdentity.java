/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ejbext;

import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents the &lt;specified-identity> element of run-as-mode.
 */
@LibertyNotInUse
public interface SpecifiedIdentity {

    /**
     * @return role="..." attribute value -- use is required!
     */
    @DDAttribute(name = "role", type = DDAttributeType.String, required = true)
    @DDXMIAttribute(name = "roleName")
    String getRole();

    /**
     * @return description="..." attribute value -- return null if not specified.
     */
    @DDAttribute(name = "description", type = DDAttributeType.String)
    @DDXMIAttribute(name = "description")
    String getDescription();

}
