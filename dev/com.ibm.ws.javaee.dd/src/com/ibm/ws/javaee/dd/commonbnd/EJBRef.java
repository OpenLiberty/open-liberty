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
package com.ibm.ws.javaee.dd.commonbnd;

import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIRefElement;

/**
 * Represents &lt;ejb-ref>.
 */
@DDIdAttribute
public interface EJBRef {

    @DDAttribute(name = "name", type = DDAttributeType.String)
    @DDXMIRefElement(name = "bindingEjbRef", referentType = com.ibm.ws.javaee.dd.common.EJBRef.class, getter = "getName")
    String getName();

    @DDAttribute(name = "binding-name", type = DDAttributeType.String)
    @DDXMIAttribute(name = "jndiName")
    String getBindingName();

}
