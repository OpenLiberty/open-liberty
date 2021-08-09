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
package com.ibm.ws.sib.processor.matching;

import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.matching.MessageProcessorMatching;
import com.ibm.ws.sib.utils.SIBUuid12;

public class TopicAuthorization {
	
	MessageProcessor _messageProcessor = null;
	
	boolean isSIBSecure = false;
	
	MessageProcessorMatching _messageProcessorMatching = null;

	public TopicAuthorization(MessageProcessor messageProcessor) {
		_messageProcessor = messageProcessor;
		_messageProcessorMatching = new MessageProcessorMatching(_messageProcessor);
	}

	public int getAclRefreshVersion() {
		
		return 0;
	}

	public void addTopicAcl(SIBUuid12 destinationUuid, String topicName, int i,
			MPPrincipal mpPrincipal) {
		// TODO Auto-generated method stub
		
	}

	public void prepareToRefresh() {
		// TODO Auto-generated method stub
		
	}

	public boolean isBusSecure() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean checkPermissionToSubscribe(DestinationHandler topicSpace,
			String topic, String userid,
			TopicAclTraversalResults topicAclTraversalResults) {
		// TODO Auto-generated method stub
		return false;
	}
}
