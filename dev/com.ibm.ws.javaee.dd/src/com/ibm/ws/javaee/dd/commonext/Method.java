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
package com.ibm.ws.javaee.dd.commonext;

import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIEnumConstant;

/**
 * Represents &lt;method>.
 */
@DDIdAttribute
public interface Method {

    enum MethodTypeEnum {
        @DDXMIEnumConstant(name = "Unspecified")
        UNSPECIFIED,
        @DDXMIEnumConstant(name = "Remote")
        REMOTE,
        @DDXMIEnumConstant(name = "Home")
        HOME,
        @DDXMIEnumConstant(name = "Local")
        LOCAL,
        @DDXMIEnumConstant(name = "LocalHome")
        LOCAL_HOME,
        @DDXMIEnumConstant(name = "ServiceEndpoint")
        SERVICE_ENDPOINT
    }

    @DDAttribute(name = "name", type = DDAttributeType.String)
    @DDXMIAttribute(name = "name")
    String getName();

    @DDAttribute(name = "params", type = DDAttributeType.String)
    @DDXMIAttribute(name = "parms")
    String getParams();

    @DDAttribute(name = "type", type = DDAttributeType.Enum)
    @DDXMIAttribute(name = "type")
    MethodTypeEnum getType();

}
