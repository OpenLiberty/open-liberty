/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.extensions.simple;

import jain.protocol.ip.sip.SipParseException;

import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * "Subscription-State" headers indicate the status of a subscription.
 *
 * If the "Subscription-State" header value is "active", it means that
 * the subscription has been accepted and (in general) has been
 * authorized.  If the header also contains an "expires" parameter, the
 * subscriber SHOULD take it as the authoritative subscription duration
 * and adjust accordingly.  The "retry-after" and "reason" parameters
 * have no semantics for "active".
 *
 * If the "Subscription-State" value is "pending", the subscription has
 * been received by the notifier, but there is insufficient policy
 * information to grant or deny the subscription yet.  If the header
 * also contains an "expires" parameter, the subscriber SHOULD take it
 * as the authoritative subscription duration and adjust accordingly.
 * No further action is necessary on the part of the subscriber.  The
 * "retry-after" and "reason" parameters have no semantics for "pending".
 *
 * If the "Subscription-State" value is "terminated", the subscriber
 * should consider the subscription terminated.  The "expires" parameter
 * has no semantics for "terminated".  If a reason code is present, the
 * client should behave as described below.  If no reason code or an
 * unknown reason code is present, the client MAY attempt to re-
 * subscribe at any time (unless a "retry-after" parameter is present,
 * in which case the client SHOULD NOT attempt re-subscription until
 * after the number of seconds specified by the "retry-after"
 * parameter).  The defined reason codes are:
 *
 * deactivated: The subscription has been terminated, but the subscriber
 *    SHOULD retry immediately with a new subscription.  One primary use
 *    of such a status code is to allow migration of subscriptions
 *    between nodes.  The "retry-after" parameter has no semantics for
 *    "deactivated".
 *
 * probation: The subscription has been terminated, but the client
 *    SHOULD retry at some later time.  If a "retry-after" parameter is
 *    also present, the client SHOULD wait at least the number of
 *    seconds specified by that parameter before attempting to re-
 *    subscribe.
 *
 * rejected: The subscription has been terminated due to change in
 *    authorization policy.  Clients SHOULD NOT attempt to re-subscribe.
 *    The "retry-after" parameter has no semantics for "rejected".
 *
 * timeout: The subscription has been terminated because it was not
 *    refreshed before it expired.  Clients MAY re-subscribe
 *    immediately.  The "retry-after" parameter has no semantics for
 *    "timeout".
 *
 * giveup: The subscription has been terminated because the notifier
 *    could not obtain authorization in a timely fashion.  If a "retry-
 *    after" parameter is also present, the client SHOULD wait at least
 *    the number of seconds specified by that parameter before
 *    attempting to re-subscribe; otherwise, the client MAY retry
 *    immediately, but will likely get put back into pending state.
 *
 * noresource: The subscription has been terminated because the resource
 *    state which was being monitored no longer exists.  Clients SHOULD
 *    NOT attempt to re-subscribe.  The "retry-after" parameter has no
 *    semantics for "noresource".
 *
 * @author Assaf Azaria, May 2003.
 */
public class SubscriptionStateHeaderImpl extends ParametersHeaderImpl 
	implements SubscriptionStateHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -1467617513860302944L;

	//
	// Members.
	//
 	
	/**
	 * The substate value.
	 */
	protected String m_substateValue;
 	
	/** 
	 * Construct a new Event header.
	 * @throws SipParseException
	 */
	public SubscriptionStateHeaderImpl() {
		super();
	}

	
	//
	// Operations.
	//
	/**
	 * Set the substate value (ACTIVE, PENDING, TERMINATED or extension).
	 * 
	 * @param value the substate value.
	 * @throws IllegalArgumentException in case the value is null or empty.
	 */
	public void setSubstateValue(String value) throws IllegalArgumentException
	{
		if (value == null || value.equals(""))
		{
			throw new IllegalArgumentException("Sub state header: null or empty type");
		}
		
		m_substateValue = value;
	}
	
	/**
	 * Get the substate value.
	 */
	public String getSubstateValue()
	{
		return m_substateValue;
	}
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
        String value = parser.nextToken(SEMICOLON);
        setSubstateValue(value);

        // parameters
        super.parseValue(parser);
	}

	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
	protected void encodeValue(CharsBuffer ret)
	{
		ret.append(m_substateValue);
		
		// Other params (if exist).
		super.encodeValue(ret);
	}

	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!super.valueEquals(other)){
			return false;
		}
		if (!(other instanceof SubscriptionStateHeaderImpl)) {
			return false;
		}
		SubscriptionStateHeaderImpl o = (SubscriptionStateHeaderImpl)other;
		
		if (m_substateValue == null || m_substateValue.length() == 0) {
			if (o.m_substateValue == null || o.m_substateValue.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_substateValue == null || o.m_substateValue.length() == 0) {
				return false;
			}
			else {
				return m_substateValue.equals(o.m_substateValue);
			}
		}
	}
	
	/**
	 * @return the name of this header 
	 * @see jain.protocol.ip.sip.header.Header#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * Creates and returns a copy of Header
	 * @returns a copy of Header
	 */
	public Object clone()
	{
		// This is required in case someone will inherit 
		// from this class.
		return super.clone(); 
	}

	/**
	 * determines whether or not this header can have nested values
	 */
	public boolean isNested() {
		return false;
	}

	/**
	 * @return the separator preceeding the list of parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getListSeparator()
	 */
	protected char getListSeparator() {
		return SEMICOLON;
	}
	
	/**
	 * @return the separator between parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getParamSeparator()
	 */
	protected char getParamSeparator() {
		return SEMICOLON;
	}

    /**
     * @return true if parameters should be escaped
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#escapeParameters()
     */
    protected boolean escapeParameters() {
    	return true;
    }
}
