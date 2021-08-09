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
package com.ibm.ws.sip.container.naptr;

import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.message.Request;

import java.io.IOException;

import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl;
import com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.context.MessageContextFactory;
import com.ibm.ws.sip.stack.naptr.INaptrSender;
import com.ibm.ws.sip.stack.naptr.NaptrHandler;

public class NaptrSenderContainer extends SendProcessor implements INaptrSender{

	/** Class Logger. */
	private static final LogMgr c_logger = Log.get(NaptrSenderContainer.class);

	/**
	 * Reference to the client that sent the request and should receive the 
	 * notification about the response if any
	 */
	protected ISenderListener _client;
		
	/**
	 * Reference to the 503 response that was received by the container
	 * when it tried to send an outgoing request. After this response the 
	 * container will try to send the request to the next hop received from
	 * NAPTR resolve. If there is no another OK response - this response
	 * will be used as a best response and will be forwarded to the listener.
	 */
	private SipServletResponseImpl _503Response;
	
	/**
	 * Holds a reference to the naptr handler
	 */
	private NaptrHandler _naptrHandler;
	/**
	 * Ctor
	 * @param target
	 * @param client
	 */
	public NaptrSenderContainer() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "NaptrSender", 
					"New NaptrSender created  = " + toString());
		}
		_naptrHandler = new NaptrHandler(this);
	}
	
	/**
	 * Method that used to clean the NaptrSender object before is 
	 * inserted back to the NaptrSender pool
	 * 
	 */
	public void cleanItself(){
		_client = null;
		_503Response = null;
		_naptrHandler.cleanSelf();
		super.cleanItself();
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "cleanItself", 
					" Clean NaptrSender = " + toString());
		}
	}

	/**
	 * @see com.ibm.ws.sip.container.naptr.ISendingProcessor#sendRequest(com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest, com.ibm.ws.sip.container.naptr.ISenderListener)
	 */
	public void sendRequest(OutgoingSipServletRequest request, ISenderListener client)
		throws IOException
	{
		if(client != null && _client == null){
			_client = client;
		}
		MessageContext messageContext = MessageContextFactory.instance().getMessageContext(request.getRequest());
		
		if (! ((MessageImpl)request.getRequest()).isLoopback()){
			_naptrHandler.sendToNextDestination(messageContext, _client.getTarget());
		}else{
			//no need to do nptr lookup if this is a loop back request, only applications that sends the request outside of the container
			//needs to do real resolving 
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "sendRequest", "no need to do NPTR resole, this is a loopback request");
			}
			_naptrHandler.sendWithoutLookup(messageContext, _client.getTarget());
		}
		
	}
	
	/**
	 *  @see com.ibm.ws.sip.container.naptr.ISendingProcessor#responseReceived(com.ibm.ws.sip.container.servlets.SipServletResponseImpl)
	 */
	public void responseReceived(SipServletResponseImpl response, ISenderListener client) {
		//check if the request was identified as loopback
		MessageImpl messageImpl  = (MessageImpl) response.getMessage();
		
		//if the response is loopback that means that the request was also a loopback, 
		//there is no need to go thru the NPTR process since no NPTR result was 
		//done in the first place
		if((response.getStatus() == SipServletResponse.SC_SERVICE_UNAVAILABLE) && ! messageImpl.isLoopback()){
			// Save this response and try to send to different destination.
			_503Response = response;
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "responseReceived", "503 response received - try next destination");
			}
			Request request = client.getOutgoingRequest().getRequest(); 
			MessageContext messageContext = MessageContextFactory.instance().getMessageContext(request);
			_naptrHandler.sendToNextDestination(messageContext,_client.getTarget());
		}
		else{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "responseReceived", 
						"Final response received - forward to the client Transaction");
			}
			_client.responseReceived(response);
		}
	}
	
	/**
	 *  @see com.ibm.ws.sip.container.naptr.ISendingProcessor#processTimeout(com.ibm.ws.sip.container.naptr.ISenderListener)
	 */
	public void processTimeout(ISenderListener client) {
		Request request = client.getOutgoingRequest().getRequest();
		MessageContext messageContext = MessageContextFactory.instance().getMessageContext(request);
		_naptrHandler.sendToNextDestination(messageContext,_client.getTarget());
	}

	
	/**
	 * @see com.ibm.ws.sip.stack.naptr.INaptrSender#sendMessage(String)
	 */
	public void error(MessageContext messageContext) {
		MessageContext.doneWithContext(messageContext);
		if(_503Response != null){
			_client.responseReceived(_503Response);
		}
		else{
			_client.failedToSendRequest(true);
		}
	}

	/*
	 * @see com.ibm.ws.sip.stack.naptr.INaptrSender#sendMessage(MessageContext,String)
	 */
	public void sendMessage(MessageContext messageContext,String transport) {
		MessageContext.doneWithContext(messageContext);
		OutgoingSipServletRequest request = _client.getOutgoingRequest();
		try {
			request.updateParamAccordingToDestination();
			
			// Sending the request to sipProvider. If IOException will be 
			// received - the request will be sent to next hop if present.
			// If not - _response (if exist )will be forwarded to the application.
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer();
				buff.append("Sending request to the next destination = ");
				buff.append(_naptrHandler.getLastUsedDestination());		
				c_logger.traceDebug(this, "sendRequestDownstream", buff
						.toString());
			}
			send(request, _client);
		} 
		catch (IOException e) {
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer();
				buff.append("IOException when sending request. try next destination for request: ");
				buff.append(request);
				c_logger.traceDebug(this, "sendToNextDestination",buff.toString());
			}
			MessageContext newMessageContext = MessageContextFactory.instance().getMessageContext(request.getRequest());
			_naptrHandler.sendToNextDestination(newMessageContext,_client.getTarget());
		}
	}

	/**
	 * get the destination that was last used for sending
	 * 
	 * @see com.ibm.ws.sip.container.naptr.ISendingProcessor#getLastUsedDestination(com.ibm.ws.sip.container.naptr.ISenderListener)
	 */
	public SipURL getLastUsedDestination(ISenderListener client) {
		return _naptrHandler.getLastUsedDestination();
	}

}
