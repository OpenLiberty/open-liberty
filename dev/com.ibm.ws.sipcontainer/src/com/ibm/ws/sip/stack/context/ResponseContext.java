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
package com.ibm.ws.sip.stack.context;

import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.message.Message;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.stack.transaction.transport.BackupMessageSenderFactory;
import com.ibm.ws.sip.stack.transaction.transport.IBackupMessageSender;
import com.ibm.ws.sip.stack.transaction.transport.SIPTransportException;
import com.ibm.ws.sip.stack.transaction.transport.TransportCommLayerMgr;
import com.ibm.ws.sip.stack.util.SipStackUtil;

/**
 * the class holding the response message context.
 * It also has methods that determine the behavior in case of failures  
 * 
 * @author nogat
 */
public class ResponseContext extends MessageContext {

	/** class logger */
	private static final LogMgr s_logger = Log.get(ResponseContext.class);

	/**
	 * flag indicating if message was sent to received address
	 * (from the Via header).
	 */
	private boolean m_sendToReceivedAddress = true;
	
	public ResponseContext(){
		super();
		m_sendToReceivedAddress = true;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.ws.sip.stack.context.MessageContext#handleFailure()
	 */
	public void handleFailure() {
		if (m_sendToReceivedAddress){
			//we get here after trying to send the response to the client
			//on the connection on which it arrived and failing.
			m_sendToReceivedAddress = false;
			
			// make sure we are not failing over a STARTUP or a KEEPALIVE response
			String method = getMethod();
			if (method != null) {
				if (method.equals("STARTUP") ||
					method.equals("KEEPALIVE"))
				{
					if (s_logger.isTraceDebugEnabled()) {
						s_logger.traceDebug(this, "hanleFailure",
							"not applying response-failover to [" + method + ']');
					}
					return;
				}
			}
			
			try {
				//check if a received tag exists in the Via Header
				if (SipStackUtil.topViaHasReceivedTag(sipMessage)){
					//this will cause an attempt to connect to the 
					//ip address from which the request was received
					//(as noted in the received tag added to the Via header by the server)
					TransportCommLayerMgr.instance().sendMessage(this, null, null);
					//we finished for now, we will get back to this method 
					//if this attempt will also fail
					return;
				}
			} catch (SIPTransportException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//we get to this part after trying to send the response to the client
		//on the connection on which it arrived and to connect to the 
		//ip address from which the request was received
		//(as noted in the received tag added to the Via header by the server)
		//at this stage both attemps failed
		
		//if we don't have a sender yet, we get one.
		if (getSender() == null){
			//get the backup sender (could be default or naptr)
			IBackupMessageSender backupMessageSender = BackupMessageSenderFactory.instance().getBackupSender();
			//set the sender in the message context so it can return it to factory when done sending
			setSender(backupMessageSender);
		}
		//send the message through backup
		getSender().sendMessageToBackup(this);
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.ws.sip.stack.context.MessageContext#doneWithContext()
	 */
	protected void doneWithContext() {
		BackupMessageSenderFactory.instance().finishToUseSender(getSender());
	}

	
	public void cleanItself() {
		m_sendToReceivedAddress = true;
		super.cleanItself();
	}

	/**
	 * @return the CSeq method of this response message
	 */
	private String getMethod() {
		Message message = getSipMessage();
		if (message == null) {
			return null;
		}
		CSeqHeader cseq = message.getCSeqHeader();
		if (cseq == null) {
			return null;
		}
		String method = cseq.getMethod();
		return method;
	}
}
