/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.uow;

/**
 * A piece of logic to be performed under a particular type of unit of work. The work
 * is performed by invoking <code>UOWManager.runUnderUOW</code> and providing this
 * action as a parameter.
 * 
 * @see UOWManager#runUnderUOW(int, boolean, UOWAction)
 *
 * @ibm-spi
 */
public interface UOWAction
{
    /**
     * Invoked as a result of an invocation of <code>UOWManager.runUnderUOW</code> 
     * once the requested UOW has been established on the thread.
     * 
     * @throws Exception Thrown if an exceptional event occurs. Note that throwing
     * a checked exception will not result in the current UOW being marked rollback
     * only or being rolled back. Throwing a RuntimeException will result in the
     * effective UOW being marked rollback only.
     * 
     * @see UOWManager#runUnderUOW(int, boolean, UOWAction)
     */
    public void run() throws Exception;
}
