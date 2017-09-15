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
import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;run-as-mode>.
 */
@DDIdAttribute
@LibertyNotInUse
public interface RunAsModeBase {

    enum ModeTypeEnum {
        CALLER_IDENTITY,
        SPECIFIED_IDENTITY,
        SYSTEM_IDENTITY
    }

    /**
     * @return &lt;specified-identity> return null if not specified.
     *         If Mode is SPECIFIED_IDENTITY then this is required.
     */
    @DDElement(name = "specified-identity")
    SpecifiedIdentity getSpecifiedIdentity();

    /**
     * @return mode="..." attribute value, one of the mode type enums, required
     */
    @DDAttribute(name = "mode", type = DDAttributeType.Enum, required = true)
    ModeTypeEnum getModeType();

    /**
     * @return description="..." attribute value, return null if not specified
     */
    @DDAttribute(name = "description", type = DDAttributeType.String)
    @DDXMIAttribute(name = "description")
    String getDescription();

}
