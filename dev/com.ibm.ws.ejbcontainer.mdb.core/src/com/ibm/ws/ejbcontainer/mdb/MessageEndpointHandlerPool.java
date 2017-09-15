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
package com.ibm.ws.ejbcontainer.mdb;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.util.PoolDiscardStrategy;

/**
 * This class implements the PoolDiscardStrategy interface to handle
 */
public class MessageEndpointHandlerPool implements PoolDiscardStrategy
{
    private BaseMessageEndpointFactory ivMessageEnpointHandlerFactory = null;

    private static final TraceComponent tc =
                    Tr.register(MessageEndpointHandlerPool.class,
                                "EJBContainer",
                                "com.ibm.ejs.container.container");

    /**
     * Default Constructor for a MessageEndpointHandlerPool object.
     * The initialize method must be called to initialize the message
     * endpoint handler factory object to handle discard.
     */
    public MessageEndpointHandlerPool(BaseMessageEndpointFactory messageEnpointHandlerFactory)
    {
        ivMessageEnpointHandlerFactory = messageEnpointHandlerFactory;
    }

    /**
     * If the PoolManager for ivInvocationHandlerPool discards
     * an instance from free pool since it has not been used for
     * some period of time, then this method will be called to
     * indicate an instance was discarded. This method is called
     * for each instance that is discarded.
     */
    //LI2110.56 - added entire method.
    @Override
    public void discard(Object o)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "MEF.discard");

        ivMessageEnpointHandlerFactory.discard();

        MessageEndpointBase meh = (MessageEndpointBase) o;
        MessageEndpointBase.discard(meh);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "MEF.discard");

    }

}
