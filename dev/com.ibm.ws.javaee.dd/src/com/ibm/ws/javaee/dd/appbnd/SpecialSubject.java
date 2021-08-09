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
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;special-subject>.
 */
@DDIdAttribute
public interface SpecialSubject {

    public static enum Type {
        EVERYONE,
        ALL_AUTHENTICATED_USERS,
        @LibertyNotInUse
        ALL_AUTHENTICATED_IN_TRUSTED_REALMS,
        /* SERVER should not be used, it is for backward compatibility only */
        @LibertyNotInUse
        SERVER
    }

    /**
     * @return type="..." attribute value
     */
    @DDAttribute(name = "type", type = DDAttributeType.Enum)
    SpecialSubject.Type getType();

}
