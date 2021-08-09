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
package com.ibm.ws.jbatch.joblog.internal.impl;

public class JobLogEntryDetail {
	
	private String firstField; // Step name (partitioned step) or split name (split flow)
	private String secondField; // Partition number (partitioned step) or flow name (split flow)
	
	public JobLogEntryDetail(String first, String second) {
		this.firstField = first;
		this.secondField = second;
	}
	
	public String toString() {
		return firstField + " " + secondField;
	}

}
