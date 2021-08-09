/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2;

import java.io.Serializable;

import javax.enterprise.context.Dependent;

/**
 * CDI Testing: Type for dependent scoped field injection.
 */
@Dependent
public class DependentFieldBean extends FieldBean implements Serializable {
    /** Default serialization ID */
    private static final long serialVersionUID = 1L;

    /**
     * Override: An application field bean has application scope, {@link CDICaseScope#Application}.
     *
     * @return The bean scope.
     */
    @Override
    public CDICaseScope getScope() {
        return CDICaseScope.Dependent;
    }

}
