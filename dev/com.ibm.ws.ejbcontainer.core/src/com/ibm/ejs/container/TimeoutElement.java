/*******************************************************************************
 * Copyright (c) 1998, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.util.Date;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * <code>TimeoutElement</code> is a holder object used by the
 * <code>StatefuleBeanReaper</code> class to retain information on bean ids
 * and their associated timeout value
 */
public class TimeoutElement
{
    private static final TraceComponent tc = Tr.register(TimeoutElement.class,
                                                         "EJBCache",
                                                         "com.ibm.ejs.container.container");

    /**
     * Construct a <code>TimeoutElement</code> object, holding the specified
     * bean ID and the time out value
     * 
     * @param beanId The bean id of the session bean
     * @param timeoutVal The timeout value for the session bean
     */
    TimeoutElement(BeanId beanId, long timeoutVal)
    {
        this.beanId = beanId;
        this.timeout = timeoutVal;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public String toString()
    {
        return "Bean ID = " + beanId + " : Timeout = " + timeout;
    }

    public boolean isTimedOut()
    {
        if (timeout > 0)
        {
            synchronized (this)
            {
                long now = System.currentTimeMillis();
                if (now - lastAccessTime >= timeout)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Session bean timed out",
                                 "Current Time: " + new Date(now) +
                                                 " Last Access Time: " + new Date(lastAccessTime) +
                                                 " Timeout: " + timeout + " ms");

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Bean ID of the session bean
     */
    public final BeanId beanId;

    /**
     * Timeout value for the bean
     */
    public final long timeout;

    /**
     * Last time this bean was accessed
     */
    public volatile long lastAccessTime;

    /**
     * Whether the bean has been passivated or not
     */
    public volatile boolean passivated;
}
