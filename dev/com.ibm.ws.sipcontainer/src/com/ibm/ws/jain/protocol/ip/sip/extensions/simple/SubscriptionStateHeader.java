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

import jain.protocol.ip.sip.header.ParametersHeader;

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
public interface SubscriptionStateHeader extends ParametersHeader
{
	//
	// Constants.
	//
	
	/**
	 * Name of subscription state header.
	 */
	public final static String name = "Subscription-State";
	
	/**
	 * The 'active' substate value.
	 */
	public final static String ACTIVE = "active";
	
	/**
	 * The 'pending' substate value.
	 */
	public final static String PENDING = "pending";
	
	/**
	 * The 'terminated' substate value.
	 */
	public final static String TERMINATED = "terminated";
	
	/**
	 * The 'deactivated' reason code.
	 */
	public final static String DEACTIVATED = "deactivated";
	
	/**
	 * The 'probation' reason code.
	 */
	public final static String PROBATION = "probation";

	/**
	 * The 'rejected' reason code.
	 */
	public final static String REJECTED = "rejected";

	/**
	 * The 'timeout' reason code.
	 */
	public final static String TIMEOUT = "timeout";

	/**
	 * The 'giveup' reason code.
	 */
	public final static String GIVEUP = "giveup";

	/**
	 * The 'noresource' reason code.
	 */
	public final static String NORESOURCE = "noresource";

	/**
	 * The 'expires' parameter.
	 */
	public final static String EXPIRES = "expires";
	
	/**
	 * The 'reason' parameter.
	 */
	public final static String REASON = "reason";
	
	/**
	 * The 'retry-after' parameter.
	 */
	public final static String RETRY_AFTER= "retry-after";
	
	//
	// Operations.
	//
	
	/**
	 * Set the substate value (ACTIVE, PENDING, TERMINATED or extension).
	 * 
	 * @param value the substate value.
	 * @throws IllegalArgumentException in case the value is null or empty.
	 */
	public void setSubstateValue(String value) throws IllegalArgumentException;
	

	/**
	 * Get the substate value.
	 */
	public String getSubstateValue();
	
}
