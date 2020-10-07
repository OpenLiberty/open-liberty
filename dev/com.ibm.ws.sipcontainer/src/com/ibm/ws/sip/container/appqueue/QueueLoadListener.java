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
package com.ibm.ws.sip.container.appqueue;

/**
 * 
 * @author anat
 * 
 * Interface which is implemented by MessageDispatcher.
 * It allows to the MessageDispatcher get notification when queue changed
 *
 */
public interface QueueLoadListener {
	
	/**
	 * Will be called each time when queue changed.
	 * @param queueId - id of queue
	 */
	public void queueChanged();
}
