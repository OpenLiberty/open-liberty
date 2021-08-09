/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.parser.util;

/**
 * A listener to the objects pool, used to perform custom clean up operations 
 * on an object that is returned to the pool.
 * 
 * @author Assaf Azaria
 */
public interface ObjectPoolListener 
{
	/**
	 * The given object was returned to the pool. Perform any clean up 
	 * operations that are needed to reset its state.
	 * 
	 * @param obj The object that was returned to the pool.
	 */
	public void objectReturned(Object obj);

}
