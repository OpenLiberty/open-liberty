package com.ibm.websphere.jtaextensions;
/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


/**
 * The exception is thrown by the transaction manager if an attempt is
 * made to unregister a <code>SynchronizationCallback</code> that is not
 * registered with the <code>ExtendedJTATransaction</code>.
 *
 * @ibm-api
 * @ibm-was-base
 * 
 */
public final class CallbackNotRegisteredException extends java.lang.Exception 
{
    private static final long serialVersionUID = -8757030444545754993L;
}
