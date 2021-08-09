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
package com.ibm.ws.sib.transactions.mpspecific;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.SIXAResource;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

/**
 * Interfaced, used internally, to obtain the Message Store
 * XA Resource for a SICoreConnection.  This is used as part of
 * the recovery process if the following conditions are met:
 * <ul>
 * <li>Subordinate transactions are being used (i.e. the XA
 *     Resource returned from the SICoreConnection.getSIXAResource
 *     method is not the XA Resource of the Message Store)</li>
 * <li>The Transaction Manager fails over into a different application
 *     server than a messaging engine with in-doubt transaction
 *     branches (i.e. remote XA recovery of transaction branches
 *     in which the Message Store participated must take place).
 * </ul>
 * Implementations of SICoreConnection may also implement
 * this interface if they can be used to obtain a reference to the
 * Message Store's XA Resource.
 */
public interface MSSIXAResourceProvider
{
   /** 
    * @return an XA resource implementation for the Message
    * Store.
    */
   SIXAResource getMSSIXAResource()
   throws SIConnectionDroppedException, 
   		 SIConnectionUnavailableException, 
   		 SIConnectionLostException, 
   		 SIResourceException, 
   		 SIErrorException;
}
