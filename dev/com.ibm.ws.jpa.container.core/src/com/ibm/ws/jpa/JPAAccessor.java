/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Provide accessor function to retrieve the JPAComponent object.
 */
public abstract class JPAAccessor
{
    private static final TraceComponent tc = Tr.register(JPAAccessor.class, "JPA", null); // d406994.2 d416151.3.11

    private static JPAComponent jpaComponent;

    /**
     * Return the default JPAComponent object in the application server.
     */
    public static JPAComponent getJPAComponent()
    {
        return jpaComponent;
    }

    // 416151.3.5 Begins
    /**
     * Return the default JPAComponent object in the application server.
     */
    public static void setJPAComponent(JPAComponent instance)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setJPAComponent", instance);

        jpaComponent = instance;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setJPAComponent");
    }
    // 416151.3.5 Ends
}
