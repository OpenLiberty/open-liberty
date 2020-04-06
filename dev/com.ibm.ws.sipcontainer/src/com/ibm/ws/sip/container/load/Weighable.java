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
package com.ibm.ws.sip.container.load;

/**
 * @author anat
 *
 * Interface that should be implemented by each class that represents source
 * affects on the server weight.
 *
 */
public interface Weighable {
	
	/**
	 * Returns the current weight of implementor's class.
	 * @return TODO
	 *
	 */
	public int getWeight();
	
	/**
	 * Notifies the counter about new timerEvent=
	 *
	 */
	public void calculateWeight();
	
	/**
	 * Set counter to new counter
	 * @param counter
	 */
	public void setCounter(long counter);
	
	/**
	 * Increment the counter
	 *
	 */
	public void increment();
	
	/**
	 * Dectement the counter
	 *
	 */
	public void decrement();
	
	/**
	 * Returns the id of the counter
	 * @return
	 */
	public int getCounterID();
	
	/**
	 * Returns current state of the counter
	 * @return TODO
	 * @return
	 */
	public String getCurrentState();
	 
	/**
	 * Returns the last load of Weighable object.
	 * @return
	 */
	public long getCurrentLoad();
	
	/**
	 * Returns the load that was used in the last call to calculateWeight, and NOT
	 * the current load.
	 * @return
	 */
	public long getLoadUsedForLastWeightCalc();
	
}
