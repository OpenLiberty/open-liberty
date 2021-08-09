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
package jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;

/**
 * <p>
 * This interface represents the Max-Forwards request-header.
 * The MaxForwardsHeader may be used with any SIP method
 * to limit the number of proxies or gateways that can forward the
 * Request to the next downstream server. This can also be useful when
 * the client is attempting to trace a request chain which appears to be
 * failing or looping in mid-chain.
 * </p><p>
 * The MaxForwards value is an integer indicating the remaining
 * number of times this Request is allowed to be forwarded.
 * Each proxy or gateway recipient of a Request containing a
 * MaxForwardsHeader must check and update its value prior to
 * forwarding the request. If the received value is zero (0), the
 * recipient must not forward the Request. Instead, for
 * OPTIONS and REGISTER Requests, it must respond as the
 * final recipient. For all other methods, the server returns a
 * TOO_MANY_HOPS Response.
 * </p><p>
 * If the received MaxForwards value is greater than zero, then the
 * forwarded Request must contain an updated MaxForwardsHeader with a
 * value decremented by one (1).
 * </p>
 *
 * @version 1.0
 *
 */
public interface MaxForwardsHeader extends Header
{
    
    /**
     * Decrements the number of max-forwards by one
     * @throws SipException if implementation cannot decrement max-fowards i.e.
     * max-forwards has reached zero
     */
    public void decrementMaxForwards()
                 throws SipException;
    
    /**
     * Gets max-forwards of MaxForwardsHeader
     * @return max-forwards of MaxForwardsHeader
     */
    public int getMaxForwards();
    
    /**
     * Sets max-forwards of MaxForwardsHeader
     * @param <var>maxForwards</var> number of max-forwards
     * @throws SipParseException if maxForwards is not accepted by implementation
     */
    public void setMaxForwards(int maxForwards)
                 throws SipParseException;
    
    ////////////////////////////////////////////////////////////
    
    /**
     * Name of MaxForwardsHeader
     */
    public final static String name = "Max-Forwards";
}
