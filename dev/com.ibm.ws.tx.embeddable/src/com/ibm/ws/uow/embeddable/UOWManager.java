/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.uow.embeddable;

import com.ibm.ws.Transaction.UOWCallback;
import com.ibm.ws.uow.UOWScope;
import com.ibm.ws.uow.UOWScopeCallback;

public interface UOWManager extends com.ibm.wsspi.uow.UOWManager
{    
    /** 
     * Suspends all units of work that are currently active on the calling
     * thread. In the event of an exception being thrown no change will have
     * been made to the state of the calling thread.
     * 
     * @exception SystemException Thrown if an unexpected internal error occurs.
     * 
     * @return A <code>UOWToken</code> that represents the suspended unit(s) of
     * work or null if no units of work were active.
     */
    public UOWToken suspend() throws SystemException;
   
    /**
     * Resumes the unit(s) of work represented by the given <code>UOWToken</code>.
     * In the event of an exception being thrown no change will have been made to
     * the state of the calling thread.
     * 
     * @param uowToken The token that represents the unit(s) of work to be resumed.
     * <code>null</code> is a valid input and will result in the thread's state
     * remaining unchanged.
     * 
     * @exception IllegalThreadStateException Thrown if the calling thread is
     * already associated with a unit of work of the same type as one that is
     * encapsulated in the given <code>UOWToken</code>.
     * 
     * @exception IllegalArgumentException Thrown if the given
     * <code>UOWToken</code> represents on or more units of work that are invalid
     * 
     * @exception SystemException Thrown if an unexpected internal error occurs
     */
    public void resume(UOWToken uowToken) throws IllegalThreadStateException, IllegalArgumentException, SystemException;

    /** 
     * Suspends all units of work that are currently active on the calling
     * thread. In the event of an exception being thrown no change will have
     * been made to the state of the calling thread.
     * In addition to tx, LTC and ActivitySessions, this method also suspends all
     * ActivityService context.  HLSLite context will not be passed on subsequent iiop 
     * requests after this method is called until a corresponding resumeAll call is made
     * 
     * @exception SystemException Thrown if an unexpected internal error occurs.
     * 
     * @return A <code>UOWToken</code> that represents the suspended unit(s) of
     * work or null if no units of work were active.
     */
    public UOWToken suspendAll() throws SystemException;

    /**
     * Resumes the unit(s) of work represented by the given <code>UOWToken</code>.
     * In the event of an exception being thrown no change will have been made to
     * the state of the calling thread.
     * 
     * @param uowToken The token that represents the unit(s) of work to be resumed.
     * This should be obtained via a call to suspendAll.
     * In addition to tx, LTC and ActivitySessions, this method also resumes 
     * ActivityService context.  HLSLite context will resume being passed on iiop 
     * requests after this method call completes.
     * <code>null</code> is a valid input and will result in the thread's state
     * remaining unchanged.
     * 
     * @exception Exception Thrown if an unexpected internal error occurs
     */
    public void resumeAll(UOWToken uowToken) throws Exception;
   
    
    /** 
     * Registers the given UOWScopeCallback for POST_BEGIN of all UOW
     * types, both user and container initiated, and in client and server
     * environments.
     * 
     * @param callback The callback to be registered for POST_BEGIN notification
     */
    public void registerCallback(UOWScopeCallback callback);
    
    /** 
     * Registers the given UOWCallback for POST_BEGIN and POST_END for
     * all UOWs started with com.ibm.wsspi.uow.UOWManager.runUnderUOW()
     * 
     * @param callback The callback to be registered for POST_BEGIN and POST_END notification
     */
    public void registerRunUnderUOWCallback(UOWCallback callback);
    
    /**
     * Returns the UOWScope on the calling thread that is responsible for coordinating
     * enlisted resources. 
     * 
     * @return The currently active UOWScope responsible for coordinating enlisted resources
     * 
     * @exception SystemException Thrown if an unexpected internal error occurs
     */
    public UOWScope getUOWScope() throws SystemException;
            
}
