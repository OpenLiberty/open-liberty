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

/**
 SITransaction is the parent interface to all Core API transaction objects. 
 SITransaction objects are passed to send and receive calls to group the 
 operations into a single unit of work. 
 <p>
 Objects with type SITransaction include:
 <ul>
 <li> Objects of type SIUncoordinatedTransaction, on which the application calls 
      commit and rollback directly. </li>
 <li> Objects of type SIXAResource, which are used to enlist send and receive 
      calls into externally coordinated transactions. 
      </li>
 </ul>
 <p>
 This class has no security implications.
*/
public interface SITransaction {
}

