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

import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.message.KeepalivePong;
import com.ibm.ws.sip.parser.util.ObjectPool;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransaction;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;

/**
 * the factory making the message contexts to be used.
 * 
 * the factory works with the 2 object pools for holding both types of contexts
 * 
 * @author nogat
 * 
 */
public class MessageContextFactory {

	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(MessageContextFactory.class);

	/**
	 * The singleton instance
	 */
	public static MessageContextFactory s_instance = null;

	/**
	 * pool of request context objects
	 */
	private ObjectPool s_requestContextPool = null;

	/**
	 * pool of response context objects
	 */
	private ObjectPool s_responseContextPool = null;

	/**
	 * pool of keepalive-pong context objects
	 */
	private ObjectPool s_keepalivePongContextPool = null;

	private MessageContextFactory() {
		int maxPoolSize = ApplicationProperties.getProperties().getInt(StackProperties.MAX_MESSAGE_CONTEXT_POOL_SIZE);
		s_requestContextPool = new ObjectPool(RequestContext.class, null, maxPoolSize);
		s_responseContextPool = new ObjectPool(ResponseContext.class, null, maxPoolSize);
		s_keepalivePongContextPool = new ObjectPool(KeepalivePongContext.class, null, maxPoolSize);
	}

	/**
	 * get the message context
	 * 
	 * @param request -
	 *            the request the context will contain
	 * 
	 * @return the message context
	 */
	public MessageContext getMessageContext(Message message) {
		if (message instanceof Request) {
			Request request = (Request) message;
			return getMessageContext(request);
		}
		if (message instanceof Response) {
			Response response = (Response) message;
			return getMessageContext(response);
		}
		if (message instanceof KeepalivePong) {
			KeepalivePong keepalivePong = (KeepalivePong)message;
			return getMessageContext(keepalivePong);
		}
		return null;
	}

	/**
	 * get the message context
	 * 
	 * @param request -
	 *            the request the context will contain
	 * 
	 * @return the message context
	 */
	public MessageContext getMessageContext(Message message, SIPTransaction sipTransaction) {
		if (message instanceof Request) {
			Request request = (Request) message;
			return getMessageContext(request,sipTransaction);
		}
		if (message instanceof Response) {
			Response response = (Response) message;
			return getMessageContext(response,sipTransaction);
		}
		return null;
	}

	/**
	 * get the message context
	 * 
	 * @param request -
	 *            the request the context will contain
	 * 
	 * @return the message context
	 */
	public MessageContext getMessageContext(Request request) {
		MessageContext messageContext = null;
		messageContext = (MessageContext) s_requestContextPool.get();
		if (messageContext == null) {
			return null;
		}
		messageContext.setSipMessage(request);
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getMessageContext", "Message context supplied: " + messageContext);
		}
		return messageContext;
	}

	/**
	 * get the message context
	 * 
	 * @param response -
	 *            the request the context will contain
	 * 
	 * @return the message context
	 */
	public MessageContext getMessageContext(Response response) {
		MessageContext messageContext = null;
		messageContext = (MessageContext) s_responseContextPool.get();
		if (messageContext == null) {
			return null;
		}
		messageContext.setSipMessage(response);
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getMessageContext", "Message context supplied: " + messageContext);
		}
		return messageContext;
	}

	/**
	 * get the message context
	 * 
	 * @param response -
	 *            the request the context will contain
	 * @param sipTransaction -
	 *            the sip transaction
	 * @return the message context
	 */
	public MessageContext getMessageContext(Request request, SIPTransaction sipTransaction) {
		MessageContext messageContext = getMessageContext(request);
		messageContext.setSipTransaction(sipTransaction);
		return messageContext;
	}

	/**
	 * get the message context
	 * 
	 * @param response -
	 *            the request the context will contain
	 * @param sipTransaction -
	 *            the sip transaction
	 * @return the message context
	 */
	public MessageContext getMessageContext(Response response, SIPTransaction sipTransaction) {
		MessageContext messageContext = getMessageContext(response);
		messageContext.setSipTransaction(sipTransaction);
		return messageContext;
	}

	/**
	 * gets a keepalive pong message context from the pool
	 * @param keepalivePong the message the context will contain
	 * @return the message context
	 */
	public MessageContext getMessageContext(KeepalivePong keepalivePong) {
		MessageContext messageContext = (MessageContext)s_keepalivePongContextPool.get();
		if (messageContext == null) {
			return null;
		}
		messageContext.setSipMessage(keepalivePong);
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getMessageContext", "Message context supplied: " + messageContext);
		}
		return messageContext;
	}

	/**
	 * Method that notifies the MessageContextFactory that Client ended to use
	 * the messageContext.
	 */
	public void finishToUseContext(MessageContext messageContext) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "finishToUseContext", "Message context about to be cleaned and returned to Pool: " + messageContext);
		}

		// clean and put back to the queue only if poolabe
		messageContext.cleanItself();

		if (messageContext instanceof RequestContext) {
			s_requestContextPool.putBack(messageContext);
			return;
		}
		if (messageContext instanceof ResponseContext) {
			s_responseContextPool.putBack(messageContext);
			return;
		}
		if (messageContext instanceof KeepalivePongContext) {
			s_keepalivePongContextPool.putBack(messageContext);
			return;
		}
	}

	/**
	 * get the singleton instance
	 * 
	 * @return the singleton instance
	 */
	public static MessageContextFactory instance() {
		if (s_instance == null) {
			synchronized (MessageContextFactory.class) {
				if (s_instance == null) {
					s_instance = new MessageContextFactory();
				}
			}
		}
		return s_instance;
	}

}
