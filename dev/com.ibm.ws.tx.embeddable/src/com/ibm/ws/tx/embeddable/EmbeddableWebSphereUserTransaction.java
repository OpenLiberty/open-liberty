package com.ibm.ws.tx.embeddable;
/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.UserTransaction;

import com.ibm.ws.uow.UOWScopeCallback;

/**
 * <code>EmbeddableWebSphereUserTransaction</code> defines some Websphere extensions to
 * the UserTransaction interface.
 *
 * <p> This interface is private to WAS.
 * Any use of this interface outside the WAS Express/ND codebase 
 * is not supported.
 *
 */
public interface EmbeddableWebSphereUserTransaction extends UserTransaction
{
    /**
     *
     *  Register users who want notification on UserTransaction Begin and End
     *
     */

    public void registerCallback(UOWScopeCallback callback); // Defect 130321

    /**
     *
     *  Unregister users who want notification on UserTransaction Begin and End
     *
     */
    public void unregisterCallback(UOWScopeCallback callback);
}