/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe;

public class RequestId {

	long sequenceNumber;
	String id;
	
	public RequestId(long sequenceNumber, String id) {
		this.sequenceNumber = sequenceNumber;
		this.id = id;
	}
	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public String getId() {
		return id;
	}

	/**
	 * Override default behavior of Object.toString() method to return the
	 * getRequestContext.getId() Whenever the toString() is called from
	 * RequestContext Object.
	 */
	@Override
	public String toString() {
		return this.getId();
	}
	
}
