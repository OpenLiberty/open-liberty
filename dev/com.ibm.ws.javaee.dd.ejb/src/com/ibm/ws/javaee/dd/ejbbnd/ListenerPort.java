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
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;listener-port>.
 * Schema requires a messageDrivenBean to have either a ListenerPort or a JCAAdapter - but not both.
 * If ListenerPort is null then JCAAdapter must be set.
 */
@DDIdAttribute
@LibertyNotInUse
public interface ListenerPort {

    /**
     * @return name="..." attribute value -- use is required!
     */
    @DDAttribute(name = "name", type = DDAttributeType.String, required = true)
    String getName();

}
