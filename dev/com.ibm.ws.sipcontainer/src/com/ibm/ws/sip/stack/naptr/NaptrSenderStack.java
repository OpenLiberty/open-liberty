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
package com.ibm.ws.sip.stack.naptr;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.SipURL;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.transport.BackupMessageSenderBase;
import com.ibm.ws.sip.stack.transaction.transport.SIPTransportException;
import com.ibm.ws.sip.stack.transaction.transport.TransportCommLayerMgr;
import com.ibm.ws.sip.stack.util.SipStackUtil;

/**
 * The sender in charge of sending messages to backup detsination using the
 * Naptr.
 * 
 * @author nogat
 * 
 */
public class NaptrSenderStack extends BackupMessageSenderBase implements INaptrSender {

	/** Class Logger. */
	private static final LogMgr c_logger = Log.get(NaptrSenderStack.class);

	/**
	 * Holds a reference to the naptr handler
	 */
	private final NaptrHandler _naptrHandler;

	/**
	 * Holds a reference to the message
	 */
	private MessageContext _messageContext;

	/**
	 * Ctor
	 * 
	 * @param target
	 * @param client
	 */
	public NaptrSenderStack() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "NaptrSender", "New NaptrSender created  = " + toString());
		}
		_naptrHandler = new NaptrHandler(this);
		_messageContext = null;
	}

	/**
	 * Method that used to clean the NaptrSender object before is inserted back
	 * to the NaptrSender pool
	 * 
	 */
	public void cleanItself() {
		_naptrHandler.cleanSelf();
		_messageContext = null;
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "cleanItself", " Clean NaptrSender = " + toString());
		}
	}

	/**
	 * @throws SipParseException
	 * @throws IllegalArgumentException
	 * 
	 */
	public void sendMessageToBackup(MessageContext messageContext) {
		_messageContext = messageContext;
		sendToNextDestination();
	}

	/**
	 * @throws SipParseException
	 * @throws IllegalArgumentException
	 * @throws SIPTransportException
	 * @see com.ibm.ws.sip.container.naptr.ISendingProcessor#sendRequest(com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest,
	 *      com.ibm.ws.sip.container.naptr.ISenderListener)
	 */
	private void sendToNextDestination() {
		SipURL target = null;
		try {
			target = SipStackUtil.createTargetFromMessage(_messageContext.getSipMessage());
			_naptrHandler.sendToNextDestination(_messageContext, target);
		} catch (Exception e) {
			// report error
			error(_messageContext);
		}
	}

	/**
	 * @see com.ibm.ws.sip.stack.naptr.INaptrSender#error(jain.protocol.ip.sip.message.Message)
	 */
	public void error(MessageContext messageContext) {
		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer();
			buff.append("Failed to send response to any destination for message: ");
			buff.append(_messageContext);
			c_logger.traceDebug(this, "error", buff.toString());
		}
		MessageContext.doneWithContext(messageContext);
	}

	/*
	 * @see com.ibm.ws.sip.stack.naptr.INaptrSender#sendMessage(MessageContext,String)
	 */
	public void sendMessage(MessageContext messageContext, String transport) {

		try {

			// Sending the request to sipProvider. If IOException will be
			// received - the request will be sent to next hop if present.
			// If not - _response (if exist )will be forwarded to the
			// application.
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer();
				buff.append("Sending message to the next destination = ");
				buff.append(_naptrHandler.getLastUsedDestination());
				c_logger.traceDebug(this, "sendRequestDownstream", buff.toString());
			}

			SipProvider provider = StackProperties.getInstance().getProvider(transport);
			TransportCommLayerMgr.instance().sendMessage(messageContext, provider, null);

		} catch (Exception e) {
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer();
				buff.append("Exception when sending message. try next destination for message: ");
				buff.append(messageContext);
				c_logger.traceDebug(this, "sendToNextDestination", buff.toString());
			}
			try {
				sendToNextDestination();
			} catch (Exception e1) {
				if (messageContext!= null){
					messageContext.writeError(e1);
				}
				if (c_logger.isTraceDebugEnabled()) {
					StringBuffer buff = new StringBuffer();
					buff.append("IOException when trying to send message to next destination for message: ");
					buff.append(messageContext);
					c_logger.traceDebug(this, "sendMessage", buff.toString());
				}
			}
		}
	}

}
