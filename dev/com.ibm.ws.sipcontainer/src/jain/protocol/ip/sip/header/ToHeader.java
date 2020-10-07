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

/**
 * <p>
 * This interface represents the To general-header.
 * The ToHeader specifies the recipient of the Request,
 * with the same NameAddress rules as the FromHeader.
 * </p><p>
 * Requests and Responses must contain a ToHeader,
 * indicating the desired recipient of the Request using the
 * NameAddress. The optional display-name of the NameAddress
 * is meant to be rendered by a human-user interface.
 * The UAS or redirect server copies the ToHeader into its
 * Response, and must add a tag if the Request
 * contained more than one ViaHeader.
 * </p><p>
 * If there was more than one ViaHeader, the Request
 * was handled by at least one proxy server. Since the
 * receiver cannot know whether any of the proxy servers
 * forked the Request, it is safest to assume that they might
 * have.
 * </p><p>
 * The SipURL of the NameAddress must not contain the transport,
 * maddr, ttl, or headers elements. A server that receives a
 * SipURL with these elements removes them before further processing.
 * </p><p>
 * The tag serves as a general mechanism to distinguish
 * multiple instances of a user identified by a single SipURL.
 * As proxies can fork Requests, the same Request
 * can reach multiple instances of a user (mobile and home phones,
 * for example). As each can respond, there needs to be a means to
 * distinguish the Responses from each other at the caller.
 * The situation also arises with multicast Requests. The
 * tag in the ToHeader serves to distinguish Responses at
 * the UAC. It must be placed in the ToHeader of the Response
 * by each instance when there is a possibility that the
 * Request was forked at an intermediate proxy. The tag
 * must be added by UAS, registrars and redirect servers, but
 * must not be inserted into Responses forwarded upstream
 * by proxies. The tag is added for all definitive Responses
 * for all Requests, and may be added for informational
 * Responses from a UAS or redirect server. All subsequent
 * transactions between two entities must include the tag.
 * </p><p>
 * The tag in ToHeaders is ignored when matching Responses
 * to Requests that did not contain a tag in their ToHeader.
 * </p><p>
 * A SIP server returns a BAD_REQUEST Response if it receives a
 * Request with a ToHeader containing a URI with a scheme it does
 * not recognize.
 * </p><p>
 * CallIdHeader, ToHeader and FromHeader are needed to identify a call leg.
 * The distinction between call and call leg matters in calls
 * with multiple Responses from a forked Request.
 * The tag is added to the ToHeader in the Response to allow
 * forking of future Requests for the same call by proxies,
 * while addressing only one of the possibly several
 * responding user agent servers. It also allows several
 * instances of the callee to send Requests that can be
 * distinguished.
 *
 * @version 1.0
 *
 */
public interface ToHeader extends EndPointHeader
{
    
    /**
     * Name of ToHeader
     */
    public final static String name = "To";
}
