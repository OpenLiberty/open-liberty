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
package com.ibm.ws.sip.container.pmi;

import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;

public class OutboundQueueMonitoringCounter extends QueueMonitoringCounter {
	
	//We do not want to read the outbound queue size custom property from the stack too early 
	//because it initializes the stack before the custom properties are were properly read. For the outbound queue we will
	//employ lazy initialization
	public OutboundQueueMonitoringCounter() {
		super();
	}
	
	@Override
	public synchronized void updateInTask() {
		if(_queueSize == 0) {
			initOutboundQueueMonitoringCounter();
		}
		super.updateInTask();
	}
	
	private void initOutboundQueueMonitoringCounter() {
		_queueSize = SIPTransactionStack.instance().getConfiguration().getMaxOutboundPendingMessages();
	}
}