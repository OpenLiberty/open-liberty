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

package com.ibm.wsspi.sib.core;

import javax.transaction.xa.XAResource;

/**
 SIXAResource may be used to enlist a Jetstream Messaging Engine into XA 
 transactions. A started XAResource can be passed on send and receive methods to 
 enlist the underlying operations into the transaction.
 <p>
 This class has no security implications.
*/
public interface SIXAResource extends XAResource, SITransaction {

  /**
   Returns true if the SIXAResource object is currently enlisted in a 
   transaction.
   
   @return true if the object is currently enlisted in a transaction
  */
  public boolean isEnlisted();
	
}

