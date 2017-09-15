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
 * A callback interface used to provide completion notification for requests to 
 * read data from the network.  Typically a user of this package would obtain
 * a IOReadRequestContext implementation (via a IOConnectionContext, via a
 * NetworkConnection) and invoke the read method supplying an instance of this
 * interface.  When the read operation completes the appropriate notification 
 * event is delivered to the implementation of this interface.
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext
 */
public interface IOReadCompletedCallback
{
   /**
    * Notification that the network read operation completed succesfully. 
    * @param networkConnection the network connection that data was read
    * from.
    * @param readContext the context object used to request the read
    * operation.
    */
   public void complete(NetworkConnection networkConnection, 
                        IOReadRequestContext readContext);
   
   /**
    * Notification that the network read operation completed but was
    * not successful.
    * @param networkConnection the network connection for which the
    * read operation was attempted.
    * @param readContext the context object used to request the read
    * operation.
    * @param ioException an exception which provides information 
    * about why the operation did not complete successfully.
    */
   public void error(NetworkConnection networkConnection, 
                     IOReadRequestContext readContext,
                     IOException ioException);
}
