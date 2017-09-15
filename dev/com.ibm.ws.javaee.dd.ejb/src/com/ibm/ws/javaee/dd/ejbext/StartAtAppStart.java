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

/**
 * Represents &lt;start-at-app-start>.
 */
@DDIdAttribute
public interface StartAtAppStart {

    /**
     * @return value="true|false" attribute value -- use is required!
     */
    @DDAttribute(name = "value", type = DDAttributeType.Boolean, required = true)
    @DDXMIAttribute(name = "startEJBAtApplicationStart")
    boolean getValue();

}
