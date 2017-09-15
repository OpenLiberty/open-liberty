/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.persistent.htod;

/******************************************************************************
*	Get and release a lock.
******************************************************************************/
public class Semaphore {

	private Thread activeThread = null;
	
/******************************************************************************
*	Get the lock.  Caller will wait until the lock is available.  This initial
*	implementation oes not control which thread gets the lock when it is released.  
*   This could cause starvation if not used carefully.
******************************************************************************/
	public synchronized void p() 
	{
		while (activeThread != null) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		activeThread = Thread.currentThread();
	}
	
/******************************************************************************
*	Release the lock.
******************************************************************************/
	public synchronized void v() 
	{
        activeThread = null;
        notify();
	}
	
}
