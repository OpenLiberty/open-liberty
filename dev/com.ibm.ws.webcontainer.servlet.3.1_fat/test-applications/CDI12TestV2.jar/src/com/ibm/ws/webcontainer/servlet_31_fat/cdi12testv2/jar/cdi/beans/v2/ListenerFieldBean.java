/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2;

import javax.enterprise.context.Dependent;
import javax.inject.Named;

/**
 * CDI Testing: Type for listener field injection.
 */
// Cannot be session or request scoped: There is no active session or request context in a listener.
@Dependent
@Named
@ListenerType
public class ListenerFieldBean extends FieldBean {
    /**
     * Override: A listener field bean has dependent scope, {@link CDICaseScope#Dependent}.
     *
     * @return The bean scope.
     */
    @Override
    public CDICaseScope getScope() {
        return CDICaseScope.Dependent;
    }
}
