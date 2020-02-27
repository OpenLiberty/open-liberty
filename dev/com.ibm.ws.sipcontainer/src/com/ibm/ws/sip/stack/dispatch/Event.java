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
package com.ibm.ws.sip.stack.dispatch;

/**
 * base class for events executed from the dispatch thread
 * 
 * @author ran
 */
public interface Event 
{
	/**
	 * called from the dispatch thread to execute this event
	 */
	public void onExecute();
}
