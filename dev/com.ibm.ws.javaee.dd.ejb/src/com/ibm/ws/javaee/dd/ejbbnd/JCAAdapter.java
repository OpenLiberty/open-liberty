/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ejbbnd;

import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;

/**
 * Represents &lt;jca-adapter>.
 * Schema requires a messageDrivenBean to have either a ListenerPort or a JCAAdapter - but not both.
 * If ListenerPort is null then JCAAdapter must be set.
 */
@DDIdAttribute
public interface JCAAdapter {

    /**
     * @return activation-spec-binding-name="..." attribute value -- use is required!
     */
    @DDAttribute(name = "activation-spec-binding-name", type = DDAttributeType.String, required = true)
    @DDXMIAttribute(name = "activationSpecJndiName")
    String getActivationSpecBindingName();

    /**
     * @return activation-spec-auth-alias="..." attribute value or null if unspecified
     */
    @DDAttribute(name = "activation-spec-auth-alias", type = DDAttributeType.String)
    @DDXMIAttribute(name = "activationSpecAuthAlias")
    String getActivationSpecAuthAlias();

    /**
     * @return destination-binding-name="..." attribute value or null if unspecified
     */
    @DDAttribute(name = "destination-binding-name", type = DDAttributeType.String)
    @DDXMIAttribute(name = "destinationJndiName")
    String getDestinationBindingName();

}
