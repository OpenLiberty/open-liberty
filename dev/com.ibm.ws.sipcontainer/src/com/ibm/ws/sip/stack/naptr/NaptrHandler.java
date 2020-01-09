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
import jain.protocol.ip.sip.address.AddressFactory;
import jain.protocol.ip.sip.address.SipURL;

import java.io.IOException;
import java.util.List;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.SipJainFactories;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.internalapi.NaptrRequestListener;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.util.SipStackUtil;
import com.ibm.wsspi.sip.channel.resolver.SIPUri;

public class NaptrHandler implements NaptrRequestListener {
	/** Class Logger. */
	private static final LogMgr c_logger = Log.get(NaptrHandler.class);

	/**
	 * Holds a reference to the messageSender
	 */
	private final INaptrSender _sender;

	/**
	 * Holds the results of the NAPTR request.
	 */
	private List<SIPUri> _naptrResults = null;

	/**
	 * Flag that defines if URI was already resolved by NAPTR or not.
	 */
	private boolean _naptrUsed = false;

	/**
	 * Holds a reference to the schema of the target
	 */
	private SipURL _target = null;

	/**
	 * Holds a reference to the latest destination where the request was sent
	 */
	private SipURL _latestDestination = null;

	/**
	 * Holds a reference to message context
	 */
	private MessageContext _messageContext = null;

	/**
	 * Ctor
	 * 
	 * @param sender -
	 *            the naptr sender that will be used for the actual message
	 *            sending
	 */
	public NaptrHandler(INaptrSender sender) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "NaptrHandler", "New NaptrSender created  = " + toString());
		}
		_sender = sender;
	}

	/**
	 * Method that used to clean the NaptrHandler object
	 */
	public void cleanSelf() {
		_naptrResults = null;
		_latestDestination = null;
		_naptrUsed = false;
		_target = null;
		_messageContext = null;
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "cleanItself", " Clean NaptrHandler = " + toString());
		}
	}

	/**
	 * @see com.ibm.ws.sip.stack.internalapi.NaptrRequestListener#error(java.lang.Exception)
	 */
	public void error(Exception e) {
		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer();
			buff.append("Error was received from NAPTR for SIPUrl = ");
			buff.append(_target);
			buff.append(" Exception ");
			buff.append(e);
			c_logger.traceDebug(this, "error", buff.toString());
		}
		reportError(_messageContext);
	}

	/**
	 * return the last used destination
	 * 
	 * @return the last used destination
	 */
	public SipURL getLastUsedDestination() {
		return _latestDestination;
	}

	/**
	 * @see com.ibm.ws.sip.stack.internalapi.NaptrRequestListener#handleNAPTRResults(java.util.Vector)
	 */
	 public void handleResolve(List<SIPUri> results) {
		if (_naptrResults == null) {
			_naptrResults = results;

			if (c_logger.isTraceDebugEnabled()) {
				SIPUri result = null;
				StringBuffer buff = new StringBuffer();
				for (int i = 0; i < results.size(); i++) {
					result = results.get(i);
					buff.append("\n\r");
					buff.append("Destination =  ");
					buff.append(result.getScheme());
					buff.append(":");
					buff.append(result.getUserInfo());
					buff.append("@");
					buff.append(result.getHost());
					buff.append(":");
					buff.append(result.getPort());
				}
				c_logger.traceDebug(this, "handleNAPTRResults", "Got Results " + buff.toString());
			}
		} else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "handleNAPTRResults", "We already received NAPTR results for " + _target);
			}
		}
		sendToNextDestination(_messageContext, _target);
	}

	/**
	 * Method that is sending the request to the top result from NAPTR query.
	 * 
	 * @throws IOException
	 */
	public void sendToNextDestination(MessageContext messageContext, SipURL target) {

		try {
			setMessage(messageContext);
			setTarget(target);
		} catch (SipParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer();
				buff.append("Error ocurred while preparing message for NAPTR for SIPUrl = ");
				buff.append(target);
				buff.append(" Exception ");
				buff.append(e);
				c_logger.traceDebug(this, "error", buff.toString());
			}
			reportError(messageContext);
			return;
		}

		// we reach this stage if all parameters were legal

		if (_naptrUsed) {
			// If NAPTR resolve used try to send to the next hop
			if (_naptrResults != null) {
				boolean sendSuccessful = sendToNextHop();
				if (!sendSuccessful) {
					reportError(messageContext);
				}
			} else {
				// if no destinations are left
				// we must report an error to sender
				reportError(messageContext);
			}
		} else {
			// NAPTR was not used yet - use it.
			useNaptr();
		}
	}

	/**
	 * send to the next hop, according to naptr results
	 * 
	 * @return false if an error was encountered, otherwise false
	 */
	private boolean sendToNextHop() {
		
		if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceEntry(this, "sendToNextHop");
		}
		
		
		boolean isNewDestination = false;
		SIPUri nextDestination = null;
		
		//we loop the results till we find a destination different from previous one
		//or till no result is found 
		while (_naptrResults.size() > 0 && !isNewDestination) {
			// get the next destination
			nextDestination = _naptrResults.remove(0);
			//checking if the next destination is new 
			isNewDestination = checkIfNewDestination(nextDestination, _messageContext.getSipConnection());
		}
		// if no next destination was found, we report the error
		if (nextDestination == null) {
			return false;
		}
		// Set in the target received information about
		// host, port and transport
		try {
			_latestDestination.setHost(nextDestination.getHost());
			_latestDestination.setPort(nextDestination.getPortInt());
			String transport = nextDestination.getTransport();
			if (transport != null) {
				_latestDestination.setTransport(transport);
			}
			try {
				
				if (c_logger.isTraceDebugEnabled()) {
		            c_logger.traceDebug("calling static method setDestinationHeader to -  " + _latestDestination);
				}
				
				
				SipStackUtil.setDestinationHeader(_latestDestination, _messageContext.getSipMessage());
			} catch (SipParseException e) {
				if (c_logger.isTraceDebugEnabled()) {
					StringBuffer buff = new StringBuffer();
					buff.append("error setting destination " + "header for message ");
					buff.append(_messageContext);
					c_logger.traceDebug(this, "sendToNextDestination", buff.toString(), e);
				}
				// if we failed to set the correct destination
				// we must report an error to sender
				return false;
			}

			// return the message to the sender so the message will be sent
			_sender.sendMessage(_messageContext, _target.getTransport());
			
		} catch (SipParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "sendToNextDestination", "error in URI [" + nextDestination + ']', e);
			}
			return false;
		}
		return true;
	}
	
	/**
	 * send the request out without doing a real naptr lookup
	 * this is used for loopback messages
	 * 
	 * @param messageContext
	 * @param target
	 * @return
	 */
	public boolean sendWithoutLookup(MessageContext messageContext, SipURL target){
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "sendWithoutLookup", target);
		}
		
		try {
			setMessage(messageContext);
			setTarget(target);
		} catch (SipParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer();
				buff.append("Error ocurred while preparing message for NAPTR for SIPUrl = ");
				buff.append(target);
				buff.append(" Exception ");
				buff.append(e);
				c_logger.traceDebug(this, "error", buff.toString());
			}
			reportError(messageContext);
			return false;
		}
		
		// return the message to the sender so the message will be sent
		_sender.sendMessage(_messageContext, _target.getTransport());
		
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "sendWithoutLookup", true);
		}
		
		return true;
	}

	private boolean checkIfNewDestination(SIPUri nextDestination, SIPConnection sipConnection) {
		// if there was no connection, we cannot send to the same one again,
		// this is new
		if (sipConnection == null) {
			return true;
		}
		// if there was no destination, it cannot be new
		if (nextDestination == null) {
			return false;
		}
		boolean sameHost = false;
		boolean samePort = false;

		// get host & port of previous destination (from the previous
		// connection)
		String previousConnectionHost = sipConnection.getRemoteHost();
		int previousConnectionPort = sipConnection.getRemotePort();

		// get host & port of next destination
		String nextDestHost = nextDestination.getHost();
		int nextDestPort = nextDestination.getPortInt();

		// compare hosts
		if (previousConnectionHost == null && nextDestHost == null) {
			sameHost = true;
		} else if (previousConnectionHost != null) {
			sameHost = previousConnectionHost.equals(nextDestHost);
		}

		// compare ports
		if (previousConnectionPort == nextDestPort) {
			samePort = true;
		}

		// only if both the host and port are equal we consider this same
		// destination
		if (sameHost && samePort) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "checkIfNewDestination", "the destination returned: " + nextDestination 
						+ " is not new. An attempt to send the message to this destination was already made.");
			}

			return false;
		}
		return true;
	}

	private void reportError(MessageContext messageContext) {
		_sender.error(messageContext);
	}

	/**
	 * This method called to the DomainStorage to resolve the target using
	 * NAPTR.
	 */
	private void useNaptr() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "useNaptr", "Call to NAPTR resolve the targetUrl = " + _target);
		}
		// this is to avoid loops if we got here after sending to a message
		// conext
		// for which a Naptr Request was already sent
		if (_messageContext.isNaptrCalled()) {
			return;
		}

		_naptrUsed = true;
		_messageContext.setNaptrCalled(true);
		
		//Create and com.ibm.wsspi.sip.channel.protocol.SIPUri object which is 
		// used by sipChannel resolver.
		SIPUri suri = SIPUri.createSIPUri(_target.toString());
		suri.setHost(_target.getHost());
		suri.setScheme(_target.getScheme());
		suri.setPortInt(_target.getPort());
		suri.setTransport(_target.getTransport());
		
		SipContainerComponent.getDomainResolverService().lookupDestination(suri, this);
	}

	/**
	 * set the message
	 * 
	 * @param messageContext -
	 *            the message context to set
	 */
	private void setMessage(MessageContext messageContext) {
		this._messageContext = messageContext;
	}

	/**
	 * set the target, mainly take the scheme.
	 * 
	 * @param target -
	 *            the target to set
	 * @throws SipParseException
	 * @throws IllegalArgumentException
	 */
	private void setTarget(SipURL target) throws IllegalArgumentException, SipParseException {
		AddressFactory addressFactory = SipJainFactories.getInstance().getAddressFactory();
		_latestDestination = addressFactory.createSipURL("", "");
		// Save the Scheme of the _latestDestination to be the same as
		// in _target URI.
		_latestDestination.setScheme(target.getScheme());
		
		_target = target;
	}
	
	

}
