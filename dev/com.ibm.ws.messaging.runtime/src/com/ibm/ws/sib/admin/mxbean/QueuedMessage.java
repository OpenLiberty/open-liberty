/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin.mxbean;

import java.beans.ConstructorProperties;

public class QueuedMessage {
	String id = null;
	String name = null;
	int approximateLength = 0;
	String state;
	String transactionId;
	String type;
	String systemMessageId;

	@ConstructorProperties({ "id", "name", "approximateLength", "state",
			"transactionId", "type", "systemMessageId" })
	public QueuedMessage(String id, String name, int approximateLength,
			String state, String transactionId, String type,
			String systemMesageId) {

		this.id = id;
		this.name = name;
		this.approximateLength = approximateLength;
		this.state = state;
		this.transactionId = transactionId;
		this.type = type;
		this.systemMessageId = systemMesageId;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getApproximateLength() throws Exception {
		// TODO Auto-generated method stub
		return approximateLength;
	}

	public String getState() {
		return state;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public String getType() throws Exception {
		return type;
	}

	public String getSystemMessageId() {
		return systemMessageId;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer(" SIBQUEUEDMessage :\n ");
		buffer.append("Id = " + id + " : ");
		buffer.append("State= " + state + " : ");
		buffer.append("Transaction Id= " + transactionId + " : ");
		buffer.append("System Message Id= " + systemMessageId + " : ");
		buffer.append("Type= " + type + " : ");

		return buffer.toString();
	}
}
