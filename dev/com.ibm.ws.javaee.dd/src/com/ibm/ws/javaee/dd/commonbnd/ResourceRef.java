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
import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIFlatten;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIRefElement;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;resource-ref>.
 */
@DDIdAttribute
public interface ResourceRef {

    @DDAttribute(name = "name", type = DDAttributeType.String)
    @DDXMIRefElement(name = "bindingResourceRef", referentType = com.ibm.ws.javaee.dd.common.ResourceRef.class, getter = "getName")
    String getName();

    @DDAttribute(name = "binding-name", type = DDAttributeType.String)
    @DDXMIAttribute(name = "jndiName")
    String getBindingName();

    @DDElement(name = "authentication-alias")
    AuthenticationAlias getAuthenticationAlias();

    @DDElement(name = "custom-login-configuration")
    @DDXMIFlatten
    CustomLoginConfiguration getCustomLoginConfiguration();

    /* Default-auth is for backward compatibility and should not be used. */
    @LibertyNotInUse
    @DDAttribute(name = "userid", elementName = "default-auth", type = DDAttributeType.String)
    @DDXMIAttribute(name = "userId", elementName = "defaultAuth",
                    elementXMITypeNamespace = "commonbnd.xmi", elementXMIType = "BasicAuthData")
    String getDefaultAuthUserid();

    @LibertyNotInUse
    @DDAttribute(name = "password", elementName = "default-auth", type = DDAttributeType.String)
    @DDXMIAttribute(name = "password", elementName = "defaultAuth",
                    elementXMITypeNamespace = "commonbnd.xmi", elementXMIType = "BasicAuthData")
    String getDefaultAuthPassword();
}
