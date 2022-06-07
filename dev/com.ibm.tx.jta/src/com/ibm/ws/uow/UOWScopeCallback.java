/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.uow;

import com.ibm.ws.Transaction.UOWCallback;

public interface UOWScopeCallback
{
    /** 
     * A context change type indicating that a new
     * UOWScope is about to begin. <code>contextChange</code>
     * will receive a <code>null</code> UOWScope reference.
     */
    public static final int PRE_BEGIN  = UOWCallback.PRE_BEGIN;
    
    /**
     * A context change type indicating that a new
     * UOWScope has begun. <code>contextChange</code>
     * will receive a reference to the new scope.
     */
    public static final int POST_BEGIN = UOWCallback.POST_BEGIN;
   
    /**
     * A context change type indicating that a 
     * UOWScope is about to end. <code>contextChange</code>
     * will receive a reference to the ending scope.
     */   
    public static final int PRE_END    = UOWCallback.PRE_END;
    
    /** 
     * A context change type indicating that a
     * UOWScope has ended. <code>contextChange</code>
     * will receive a <code>null<code> UOWScope reference.
     */
    public static final int POST_END   = UOWCallback.POST_END;
    
    /** 
     * Invoked when a unit of work context change is occuring.
     * 
     * @param changeType The type of change that is occuring
     * @param uowScope The UOWScope to which the change relates
     * @throws IllegalStateException
     */
    public void contextChange(int changeType, UOWScope uowScope) throws IllegalStateException;
}
