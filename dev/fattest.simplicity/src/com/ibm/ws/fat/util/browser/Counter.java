/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.browser;

/**
 * Thread-safe counter
 * 
 * @author Tim Burns
 *
 */
public class Counter {

	protected int count;
	
	/**
	 * Primary Constructor
	 */
	public Counter() {
		this.count = 0;
	}
	
	/**
	 * Increment this instance by one
	 * 
	 * @return
	 */
	public synchronized int next() {
		this.count++;
		return this.count;
	}
}
