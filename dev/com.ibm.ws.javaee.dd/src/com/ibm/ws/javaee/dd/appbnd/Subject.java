/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.appbnd;

import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;

/**
 * Represents common behavior of &lt;user> and &lt;group>.
 */
@DDIdAttribute
public interface Subject {

    /**
     * @return name="..." attribute value
     */
    @DDAttribute(name = "name", type = DDAttributeType.String)
    @DDXMIAttribute(name = "name")
    String getName();

    /**
     * @return access-id="..." attribute value
     */
    @DDAttribute(name = "access-id", type = DDAttributeType.String)
    @DDXMIAttribute(name = "accessId")
    String getAccessId();

}
