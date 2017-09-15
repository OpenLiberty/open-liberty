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
 * made to register a <code>SynchronizationCallback</code> in an environment
 * or at a time when this function is not available.
 *
 * @ibm-api
 * @ibm-was-base
 * 
 */
public final class NotSupportedException extends java.lang.Exception
{
    private static final long serialVersionUID = -799825021772555475L;
}
