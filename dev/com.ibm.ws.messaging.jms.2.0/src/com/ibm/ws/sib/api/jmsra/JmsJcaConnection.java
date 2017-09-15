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
package com.ibm.ws.sib.api.jmsra;

//Sanjay Liberty Changes
//import javax.resource.ResourceException;
//import javax.resource.spi.IllegalStateException;

import javax.resource.ResourceException;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

/**
 * Manages the lifecycle of an authenticated core connection and provides a 
 * factory method for the creation of <code>JmsJcaSession</code> objects.
 * There will be a one-to-one relationship between JMS connections and objects
 * implementing this interface.  This class is thread safe.
 */
public interface JmsJcaConnection {
   
   /**
    * Creates a <code>JmsJcaSession</code> that shares the core connection
    * from this connection.
    * 
    * @param transacted
    *            a flag indicating whether, in the absence of a global or
    *            container local transaction, work should be performed inside
    *            an application local transaction
    * @return the session
    * @throws ResourceException
    *             if the JCA runtime fails to allocate a managed connection
    * @throws IllegalStateException
    *             if this connection has been closed
    * @throws SIException
    *             if the core connection cannot be cloned
    * @throws SIErrorException
    *             if the core connection cannot be cloned
    */
    public JmsJcaSession createSession(boolean transacted)
            throws ResourceException, IllegalStateException, SIException,
            SIErrorException;
   
   /**
    * Returns the core connection created for, and associated with, this
    * connection.
    * 
    * @return the core connection
    * @throws IllegalStateException
    *             if this connection has been closed
    */
   public SICoreConnection getSICoreConnection() throws IllegalStateException;
   
   /**
    * Closes this connection, any open sessions created from it and its
    * associated core connection.
    * 
    * @throws SIErrorException
    *             if the associated core connection failed to close
    * @throws SIResourceException
    *             if the associated core connection failed to close
    * @throws SIIncorrectCallException
    *             if the associated core connection failed to close
    * @throws SIConnectionLostException
    *             if the associated core connection failed to close
    * @throws SIConnectionDroppedException
    *             if the associated core connection failed to close
    * @throws SIConnectionUnavailableException
    *             if the associated core connection failed to close
    */
   public void close() throws SIConnectionLostException,
            SIIncorrectCallException, SIResourceException, SIErrorException,
            SIConnectionDroppedException, SIConnectionUnavailableException;

}
