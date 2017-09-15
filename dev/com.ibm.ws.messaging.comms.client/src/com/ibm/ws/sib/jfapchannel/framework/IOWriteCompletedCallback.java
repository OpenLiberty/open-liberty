/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.framework;

import java.io.IOException;

/**
 * A callback used to provide notification that a write request has completed.
 * Typically, a user of this package will use the write method of a
 * IOWriteRequestContext implementation to schedule a write request.  One of
 * the arguments to this method is an implementation of this interface which 
 * the user must provide.
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext 
 */
public interface IOWriteCompletedCallback
{
   /**
    * Notification that the write operation completed successfully
    * (that is to say that no errors were detected - not that the data has
    * been received by the peer).
    * @param networkConnection the network connection for which the write
    * operation was requested.
    * @param writeRequestContext the write context which was used to
    * request the write operation.
    */
   public void complete(NetworkConnection networkConnection, 
                        IOWriteRequestContext writeRequestContext);
   
   /**
    * Notification that the write operation completed - but not
    * successfully.
    * @param networkConnection the network connection for which the
    * write operation was requested.
    * @param writeRequestContext the write context which was used to
    * request the write operation.
    * @param ioException an exception which provides information about
    * why the write operation did not complete successfully.
    */
   public void error(NetworkConnection networkConnection,
                     IOWriteRequestContext writeRequestContext,  
                     IOException ioException);
}
