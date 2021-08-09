/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel;

/**
 * Implementers of this class can use it to be notified when a physical connection (that backs a
 * conversation) closes. The callback will only be called once and may be called due to a timeout, 
 * user intervention or error.
 * 
 * @author Gareth Matthews
 */
public interface ConnectionClosedListener
{
   /**
    * Driven when the connection is closed.
    * 
    * @param connectionReference A reference to the connection to which was closed.
    */
   void connectionClosed(Object connectionReference);
}
