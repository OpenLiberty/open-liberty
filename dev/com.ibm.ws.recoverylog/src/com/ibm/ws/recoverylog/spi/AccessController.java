/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

//------------------------------------------------------------------------------
// Interface : AccessController
//------------------------------------------------------------------------------
/**
 * Interface to represent the abstraction of functions required by individual services
 * of the recovery log component.
 * Each product specific recoverylog service should implement this interface and make it available
 * to the services.
 */
public interface AccessController
{
   /**
    * Called to perform java2 security security manager (if one is available) function.
    * May throw PrivilegedActionException or PrivilegedExceptionAction.
    * @param action contains the code to run under the security manager control.
    */
   public Object doPrivileged(PrivilegedExceptionAction action) throws PrivilegedActionException;


}
