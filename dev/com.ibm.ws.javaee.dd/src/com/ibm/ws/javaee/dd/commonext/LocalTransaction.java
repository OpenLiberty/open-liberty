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
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;local-transaction>.
 */
@DDIdAttribute
public interface LocalTransaction {
    enum BoundaryEnum {
        @LibertyNotInUse
        @DDXMIEnumConstant(name = "ActivitySession")
        ACTIVITY_SESSION,
        @DDXMIEnumConstant(name = "BeanMethod")
        BEAN_METHOD
    }

    enum ResolverEnum {
        @DDXMIEnumConstant(name = "Application")
        APPLICATION,
        @DDXMIEnumConstant(name = "ContainerAtBoundary")
        CONTAINER_AT_BOUNDARY
    }

    enum UnresolvedActionEnum {
        @DDXMIEnumConstant(name = "Rollback")
        ROLLBACK,
        @DDXMIEnumConstant(name = "Commit")
        COMMIT
    }

    boolean isSetBoundary();

    @LibertyNotInUse
    @DDAttribute(name = "boundary", type = DDAttributeType.Enum)
    @DDXMIAttribute(name = "boundary")
    BoundaryEnum getBoundary();

    boolean isSetResolver();

    @DDAttribute(name = "resolver", type = DDAttributeType.Enum)
    @DDXMIAttribute(name = "resolver")
    ResolverEnum getResolver();

    boolean isSetUnresolvedAction();

    @DDAttribute(name = "unresolved-action", type = DDAttributeType.Enum)
    @DDXMIAttribute(name = "unresolvedAction")
    UnresolvedActionEnum getUnresolvedAction();

    boolean isSetShareable();

    @DDAttribute(name = "shareable", type = DDAttributeType.Boolean)
    @DDXMIAttribute(name = "shareable")
    boolean isShareable();
}
