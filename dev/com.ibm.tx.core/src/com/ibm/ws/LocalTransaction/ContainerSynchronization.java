package com.ibm.ws.LocalTransaction;
/*******************************************************************************
 * Copyright (c) 2002, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * This interface is provided for the EJB container and
 * only the EJB container to use. Synchronizations enlisted
 * with the LTC and ActivitySesion by the EJB container
 * should implement this interface.
 *
 * <p> This interface is private to WAS.
 * Any use of this interface outside the WAS Express/ND codebase 
 * is not supported.
 *
 */
public interface ContainerSynchronization
    extends javax.transaction.Synchronization
{
    /**
     * Set whether the LTC is being driven mid-ActivitySession
     * or at ActivitySession completion.
     * 
     * @param isCompleting
     *               <UL>
     *               <LI>true - ActivitySession is completing.</LI>
     *               <LI>false - ActivitySession is executing a mid session checkpoint/reset.</LI>
     *               </UL>
     */
    public void setCompleting(boolean isCompleting);    
}
