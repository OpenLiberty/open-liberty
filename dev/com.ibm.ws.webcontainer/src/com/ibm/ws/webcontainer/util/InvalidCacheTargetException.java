/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;


public class InvalidCacheTargetException extends NullPointerException
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3618421535787135542L;
	private static InvalidCacheTargetException inst;
	
	private InvalidCacheTargetException()
	{
		super();
	}
	
	public static InvalidCacheTargetException instance()
	{
		if (inst == null)
			inst = new InvalidCacheTargetException();
			
		return inst;
	}
}
