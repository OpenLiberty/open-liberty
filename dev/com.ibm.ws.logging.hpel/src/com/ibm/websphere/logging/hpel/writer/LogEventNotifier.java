/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.writer;

import java.util.Date;

/**
 * Interface for visibility/dependency purposes which logging systems call to when roll or delete events occur and which
 * listeners for these events call to register.  The implementer maintains the collection of listeners and, on event
 * occurrence, notifies all listeners.
 * @ibm-spi
 */
public interface LogEventNotifier {

	/**
	 * set the oldest date based on repository type. This is generally called soon after this object is constructed as the
	 * managers are notified of the object
	 * @param oldestDate oldest date in the repository for that repository type. This may be null if manager is unable to determine oldest date
	 * @param repositoryType type of repository (log/trace)
	 */
	public abstract void setOldestDate(Date oldestDate, String repositoryType);
	
	/**
	 * record that a file action has taken place on a file type, leaving current oldest record as curOldestDate
	 * @param eventType roll or delete
	 * @param repositoryType log or trace 
	 * @param curOldestDate this will be null if it does not change the value or oldest date not determinable
	 */
	public abstract void recordFileAction(String eventType, String repositoryType, Date curOldestDate);

	/**
	 * register a new listener for log events.  This listener will be notified any time a roll or delete event occurs
	 * on a log or trace system. 
	 * @param eventListener implementer of the LogEventListener interface
	 */
	public abstract void registerListener(LogEventListener eventListener);

	/**
	 * deRegister a listener for log events.  Indicates that this listener is no longer interested in receiving
	 * log and trace events
	 * @param eventListener implementer of the LogEventListener interface
	 */
	public abstract void deRegisterListener(LogEventListener eventListener);

	/**
	 * return the oldest record of the current type. For this sample, does not differentiate. In reality later, this will forward
	 * to the appropriate repositoryManager which will calculate oldest record.
	 * @param repositoryType Log vs Trace
	 * @return Date of oldest record in the repository.  Null if manager could not determine this.
	 */
	public abstract Date getOldestLogRecordTime(String repositoryType);

}
