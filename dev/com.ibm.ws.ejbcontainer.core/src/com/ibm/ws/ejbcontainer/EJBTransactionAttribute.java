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

public enum EJBTransactionAttribute
{
    BEAN_MANAGED(InternalConstants.TX_BEAN_MANAGED),

    NOT_SUPPORTED(InternalConstants.TX_NOT_SUPPORTED),
    SUPPORTS(InternalConstants.TX_SUPPORTS),
    REQUIRED(InternalConstants.TX_REQUIRED),
    REQUIRES_NEW(InternalConstants.TX_REQUIRES_NEW),
    MANDATORY(InternalConstants.TX_MANDATORY),
    NEVER(InternalConstants.TX_NEVER);

    private static final EJBTransactionAttribute[] FOR_VALUE;

    static
    {
        EJBTransactionAttribute[] values = EJBTransactionAttribute.values();

        int maxValue = 0;
        for (EJBTransactionAttribute type : values)
        {
            maxValue = Math.max(maxValue, type.value());
        }

        FOR_VALUE = new EJBTransactionAttribute[maxValue + 1];

        for (EJBTransactionAttribute type : values)
        {
            FOR_VALUE[type.value()] = type;
        }
    }

    public static EJBTransactionAttribute forValue(int value)
    {
        return FOR_VALUE[value];
    }

    private final int ivValue;

    EJBTransactionAttribute(int value)
    {
        ivValue = value;
    }

    public int value()
    {
        return ivValue;
    }
}
