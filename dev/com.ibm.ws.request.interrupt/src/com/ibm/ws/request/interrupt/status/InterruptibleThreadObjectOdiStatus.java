/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.interrupt.status;

/**
 * class to hold odi status
 * 
 */
public class InterruptibleThreadObjectOdiStatus {
	int position;
	String name;
	String details;
	boolean queried;
	
	/**
	 * constructor
	 * 
	 * @param position The position that the InterruptObject occupies in the current stack of InterruptObjects for a request. Lower positions occur earlier in the stack, and will be driven first.
	 * @param name     The name of the InterruptObject in the current stack of InterruptObjects for a request. The name is reported by the "getName" method of the InterruptObject.
	 * @param details  The details of the InterruptObject in the current stack of InterruptObjects for a request. The details are reported by the "getDisplayInfo" method of the InterruptObject.
	 * @param queried  The queried flag for the InterruptObject in the current stack of InterruptObjects for a request. The queried flag is set when the InterruptObject was driven n an attempt to interrupt the request.
	 * 
	 */
	public InterruptibleThreadObjectOdiStatus(int position, String name, String details, boolean queried) {
		this.position = position;
		this.name = name;
		this.details = details;
		this.queried = queried;
	}

	/**
	 * Get the name of an InterruptObject
	 * 
	 * @return The name of the InterruptObject.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the position that the InterruptObject occupies.
	 * 
	 * @return The position that the InterruptObject occupies.
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * Get the details of the InterruptObject.
	 * 
	 * @return The details of the InterruptObject.
	 */
	public String getDetails() {
		return details;
	}

	/**
	 * Get the queried flag for the InterruptObject.
	 * 
	 * @return The queried flag for the InterruptObject 
	 */
	public Boolean getQueried() {
		if (queried == true) {
			return Boolean.TRUE;
			
		} else {
			return Boolean.FALSE;
			
		}
	}
}
