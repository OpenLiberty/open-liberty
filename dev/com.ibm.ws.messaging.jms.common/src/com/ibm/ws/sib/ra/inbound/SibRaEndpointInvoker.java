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

package com.ibm.ws.sib.ra.inbound;

import java.lang.reflect.Method;

//Sanjay Liberty Changes
//import javax.resource.spi.ResourceAdapterInternalException;
//import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpoint;
import com.ibm.wsspi.sib.core.AbstractConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SITransaction;

/**
 * Interface implemented by resource adapters for invocation of message
 * endpoints.
 */
public interface SibRaEndpointInvoker {

    /**
     * Returns the method on the endpoint which will be invoked.
     * 
     * @return the method
     * @throws ResourceAdapterInternalException
     *             if the method cannot be obtained
     */
    Method getEndpointMethod() throws ResourceAdapterInternalException;

    /**
     * Invokes an endpoint with the given message.
     * 
     * @param endpoint
     *            the endpoint to invoke
     * @param message
     *            the message to invoke it with
     * @param session
     *            the session from which the message was consumed
     * @param transaction
     *            the transactin under which the message was consumed
     * @param debugMEName
     * 			  this is always null unless the debug system parameter is switched on
     * @return a flag indicating whether the message was delivered successfully
     * @throws ResourceAdapterInternalException
     *             if the endpoint does not implement the required interface
     */
    boolean invokeEndpoint(MessageEndpoint endpoint, SIBusMessage message,
            AbstractConsumerSession session, SITransaction transaction, String debugMEName)
            throws ResourceAdapterInternalException;

}
