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
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;bean-cache>.
 */
@DDIdAttribute
public interface BeanCache {

    enum ActivationPolicyTypeEnum {
        ONCE,
        @LibertyNotInUse // Activity sessions are not supported in Liberty
        ACTIVITY_SESSION,
        TRANSACTION
    }

    /**
     * @return activation-policy="" return one of the ENUM values, return null is not specified.
     */
    @DDAttribute(name = "activation-policy", type = DDAttributeType.Enum)
    @DDXMIAttribute(name = "activateAt")
    ActivationPolicyTypeEnum getActivationPolicy();

}
