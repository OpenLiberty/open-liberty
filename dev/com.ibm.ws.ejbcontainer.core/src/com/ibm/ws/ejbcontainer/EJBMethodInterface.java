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

public enum EJBMethodInterface
{
    HOME(InternalConstants.METHOD_INTF_HOME, "Home"),
    REMOTE(InternalConstants.METHOD_INTF_REMOTE, "Remote"),
    LOCAL_HOME(InternalConstants.METHOD_INTF_LOCAL_HOME, "LocalHome"),
    LOCAL(InternalConstants.METHOD_INTF_LOCAL, "Local"),

    SERVICE_ENDPOINT(InternalConstants.METHOD_INTF_SERVICE_ENDPOINT, "ServiceEndpoint"),
    TIMER(InternalConstants.METHOD_INTF_TIMER, "Timer"),
    MESSAGE_ENDPOINT(InternalConstants.METHOD_INTF_MESSAGE_ENDPOINT, "MessageEndpoint"),

    /**
     * Method interface for EJBs that support transaction or security attributes
     * for lifecycle interceptors. Request collaborators are not notified for
     * this method interface type.
     */
    LIFECYCLE_INTERCEPTOR(InternalConstants.METHOD_INTF_LIFECYCLE_INTERCEPTOR, "LifecycleInterceptor");

    private static final EJBMethodInterface[] FOR_VALUE;

    static
    {
        EJBMethodInterface[] values = EJBMethodInterface.values();

        int maxValue = 0;
        for (EJBMethodInterface type : values)
        {
            maxValue = Math.max(maxValue, type.value());
        }

        FOR_VALUE = new EJBMethodInterface[maxValue + 1];

        for (EJBMethodInterface type : values)
        {
            FOR_VALUE[type.value()] = type;
        }
    }

    public static EJBMethodInterface forValue(int value)
    {
        return FOR_VALUE[value];
    }

    private final int ivValue;

    /**
     * String representation of each legal <code>EJBMethodInterface</code>.
     * The value would be consumed by JACC.
     **/
    private final String ivName;

    private EJBMethodInterface(int value, String name) {
        ivValue = value;
        ivName = name;
    }

    public int value()
    {
        return ivValue;
    }

    public String specName()
    {
        return ivName;
    }

    public boolean isHome()
    {
        return this == HOME ||
               this == LOCAL_HOME;
    }

    public boolean isRemote()
    {
        return this == REMOTE ||
               this == HOME;
    }

    public boolean isLocal()
    {
        return this == LOCAL ||
               this == LOCAL_HOME;
    }

}
