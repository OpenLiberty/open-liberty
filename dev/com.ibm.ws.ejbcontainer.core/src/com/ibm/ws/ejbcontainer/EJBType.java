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
package com.ibm.ws.ejbcontainer;

public enum EJBType
{
    SINGLETON_SESSION(InternalConstants.TYPE_SINGLETON_SESSION),
    STATEFUL_SESSION(InternalConstants.TYPE_STATEFUL_SESSION),
    STATELESS_SESSION(InternalConstants.TYPE_STATELESS_SESSION),

    BEAN_MANAGED_ENTITY(InternalConstants.TYPE_BEAN_MANAGED_ENTITY),
    CONTAINER_MANAGED_ENTITY(InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY),

    MESSAGE_DRIVEN(InternalConstants.TYPE_MESSAGE_DRIVEN);

    private static final EJBType[] FOR_VALUE;

    static
    {
        EJBType[] values = values();

        int maxValue = 0;
        for (EJBType type : values)
        {
            maxValue = Math.max(maxValue, type.value());
        }

        FOR_VALUE = new EJBType[maxValue + 1];

        for (EJBType type : values)
        {
            FOR_VALUE[type.value()] = type;
        }
    }

    public static EJBType forValue(int value)
    {
        return FOR_VALUE[value];
    }

    private final int ivValue;

    EJBType(int value)
    {
        ivValue = value;
    }

    public int value()
    {
        return ivValue;
    }

    public boolean isSession()
    {
        return this == SINGLETON_SESSION ||
               this == STATEFUL_SESSION ||
               this == STATELESS_SESSION;
    }

    public boolean isEntity()
    {
        return this == BEAN_MANAGED_ENTITY ||
               this == CONTAINER_MANAGED_ENTITY;
    }

    public boolean isMessageDriven()
    {
        return this == MESSAGE_DRIVEN;
    }
}
