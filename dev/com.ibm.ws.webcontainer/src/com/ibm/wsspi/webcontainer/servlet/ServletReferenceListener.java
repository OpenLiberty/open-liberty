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
package com.ibm.wsspi.webcontainer.servlet;

/**
 *
 * A listner that is associated with a single instance of a ServletWrapper (and
 * hence a servlet), and listens for invalidation events.
 * 
 * @ibm-private-in-use
 */
public interface ServletReferenceListener
{
	/**
	 * Signals that the servlet that this listener is associated with has
	 * been invalidated
	 *
	 */
	public void invalidate();
}
