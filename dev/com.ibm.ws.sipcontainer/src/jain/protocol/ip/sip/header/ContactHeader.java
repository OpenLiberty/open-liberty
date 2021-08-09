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

import jain.protocol.ip.sip.SipParseException;

import java.util.Date;

/**
 * <p>
 * This interface represents the Contact general-header.
 * A ContactHeader field can appear in INVITE, ACK and
 * REGISTER Requests, and in Responses with any INFORMATIONAL, SUCCESS,
 * REDIRECT, or AMBIGUOUS status codes. In
 * general, it provides a URL where the user can be reached for further
 * communications.
 * </p><p>
 * INVITE and ACK Requests: INVITE and ACK Requests may contain
 * ContactHeaders indicating from which location the request is originating.
 * </p><p>
 * This allows the callee to send future Requests, such as
 * BYE Requests, directly to the caller instead of through a series of
 * proxies.  The ViaHeader is not sufficient since the
 * desired address may be that of a proxy.
 * </p><p>
 * SUCCESS Responses for INVITE Requests: A user agent server sending
 * a definitive, positive Response (SUCCESS) may insert a ContactHeader in the
 * Response indicating the SIP address under which it is reachable
 * most directly for future SIP Requests, such as an ACK Request, within the
 * same Call-ID. The ContactHeader contains the address of
 * the server itself or that of a proxy, e.g., if the host is
 * behind a firewall. The value of this ContactHeader is copied
 * into the Request-URI of subsequent Requests for this call if the
 * response did not also contain a RecordRouteHeader. If the
 * response also contains a RecordRouteHeader, the address
 * in the ContactHeader is added as the last item in the
 * RouteHeader. See RecordRouteHeader for details.
 * </p><p>
 * The ContactHeader should not be cached across calls, as it
 * may not represent the most desirable location for a
 * particular destination address.
 * </p><p>
 * INFORMATIONAL Responses for INVUTE Requests: A server sending an
 * INFORMATIONAL Response may insert a ContactHeader.
 * It has the same semantics in an INFORMATIONAL Response as a SUCCESS
 * Response. Note that CANCEL Requests must not be sent to that address,
 * but rather follow the same path as the original request.
 * </p><p>
 * REGISTER Requests: REGISTER Requests may contain ContactHeaders
 * indicating at which locations the user is reachable. The
 * REGISTER Request defines a wildcard ContactHeader which
 * must only be used with Expires set to 0 to remove all registrations
 * for a particular user. An optional "expires" parameter indicates
 * the desired expiration time of the registration. If a ContactHeader
 * does not have an "expires" parameter, the ExpiresHeader
 * field is used as the default value. If neither of these
 * mechanisms is used, SIP URIs are assumed to expire after one
 * hour. Other URI schemes have no expiration times.
 * </p><p>
 * SUCCESS Responses for REGISTER Requests: A SUCCESS Response for a
 * REGISTER Request may return all locations at which the user is currently
 * reachable.  An optional "expires" parameter indicates the expiration time
 * of the registration. If a ContactHeader does not have an "expires"
 * parameter, the value of the ExpiresHeader indicates the expiration time.
 * If neither mechanism is used, the expiration time specified in the
 * request, explicitly or by default, is used.
 * </p><p>
 * REDIRECTION and AMBIGUOUS Responses: The ContactHeader
 * can be used with a REDIRECTION or AMBIGUOUS Response to
 * indicate one or more alternate addresses to try. It can appear in
 * Responses for BYE, INVITE and OPTIONS Requests.
 * ContactHeaders contain URIs giving the new locations or user names to try,
 * or may simply specify additional transport parameters. A REDIRECTION_MULTIPLE_CHOICES,
 * REDIRECTION_MOVED_PERMANENTLY, REDIRECTION_MOVED_TEMPORARILY, or
 * AMBIGUOUS Response should contain one or more
 * ContactHeaders containing URIs of new addresses to be tried. A
 * REDIRECTION_MOVED_PERMANENTLY or REDIRECTION_MOVED_TEMPORARILY Response
 * may also give the same location and username that was being tried but
 * specify additional transport parameters such as a different server or
 * multicast address to try or a change of SIP transport from UDP to TCP
 * or vice versa. The client copies the "user", "password", "host", "port"
 * and "user-param" elements of the Contact URI into the Request-URI of the
 * redirected request and directs the request to the address
 * specified by the "maddr" and "port" parameters, using the
 * transport protocol given in the "transport" parameter. If
 * "maddr" is a multicast address, the value of "ttl" is used as
 * the time-to-live value.
 * </p><p>
 * Note that the ContactHeader field may also refer to a different
 * entity than the one originally called. For example, a SIP call
 * connected to GSTN gateway may need to deliver a special information
 * announcement such as "The number you have dialed has been changed."
 * </p><p>
 * A ContactHeader in a Response can contain any suitable URI
 * indicating where the called party can be reached, not limited to SIP
 * URLs. For example, it could contain URL's for phones, fax, or irc (if
 * they were defined) or a mailto: URL.
 * </p><p>
 * The following parameters are defined.
 * </p><p>
 * q: The "qvalue" indicates the relative preference among the locations
 * given. "qvalue" values are decimal numbers from 0 to 1, with
 * higher values indicating higher preference.
 * </p><p>
 * action: The "action" parameter is used only when registering with the
 * REGISTER request. It indicates whether the client wishes that
 * the server proxy or redirect future Requests intended for the
 * client. If this parameter is not specified the action taken
 * depends on server configuration. In its Response, the registrar
 * should indicate the mode used. This parameter is ignored for
 * other Requests.
 * </p><p>
 * expires: The "expires" parameter indicates how long the URI is valid.
 * The parameter is either a number indicating seconds or a quoted
 * string containing a SIP-date. If this parameter is not provided,
 * the value of the ExpiresHeader determines how long the
 * URI is valid. Implementations may treat values larger than
 * 2**32-1 (4294967295 seconds or 136 years) as equivalent to
 * 2**32-1.
 * </p>
 *
 * @see NameAddressHeader
 * @see ParametersHeader
 * @see RecordRouteHeader
 *
 * @version 1.0
 *
 */
public interface ContactHeader extends NameAddressHeader, ParametersHeader
{
    
    /**
     * Sets ContactHeader to wild card (replaces NameAddress with "*")
     */
    public void setWildCard();
    
    /**
     * Gets comment of ContactHeader
     * (Returns null if comment does not exist)
     * @return comment of ContactHeader
     */
    public String getComment();
    
    /**
     * Gets boolean value to indicate if ContactHeader
     * has comment
     * @return boolean value to indicate if ContactHeader
     * has comment
     */
    public boolean hasComment();
    
    /**
     * Gets expires as date of ContactHeader
     * (Returns null if expires value does not exist)
     * @return expires as date of ContactHeader
     */
    public Date getExpiresAsDate();
    
    /**
     * Sets action of ContactHeader
     * @param <var>action</var> action
     * @throws IllegalArgumentException if action is null
     * @throws SipParseException if action is not accepted by implementation
     */
    public void setAction(String action)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Removes action from ContactHeader (if it exists)
     */
    public void removeAction();
    
    /**
     * Sets expires of ContactHeader to a number of delta-seconds
     * @param <var>expiryDeltaSeconds</var> number of delta-seconds until expiry
     * @throws SipParseException if expiryDeltaSeconds is not accepted by implementation
     */
    public void setExpires(long expiryDeltaSeconds)
                 throws SipParseException;
    
    /**
     * Gets expires as delta-seconds of ContactHeader
     * (returns negative long if expires does not exist)
     * @return expires as delta-seconds of ContactHeader
     */
    public long getExpiresAsDeltaSeconds();
    
    /**
     * Sets comment of ContactHeader
     * @param <var>comment</var> comment
     * @throws IllegalArgumentException if comment is null
     * @throws SipParseException if comment is not accepted by implementation
     */
    public void setComment(String comment)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Returns boolean value indicating whether ContactHeader is a wild card
     * @return boolean value indicating whether ContactHeader is a wild card
     */
    public boolean isWildCard();
    
    /**
     * Removes comment from ContactHeader (if it exists)
     */
    public void removeComment();
    
    /**
     * Gets action of ContactHeader
     * (Returns null if action does not exist)
     * @return action of ContactHeader
     */
    public String getAction();
    
    /**
     * Gets q-value of ContactHeader
     * (Returns negative float if comment does not exist)
     * @return q-value of ContactHeader
     */
    public float getQValue();
    
    /**
     * Sets expires of ContactHeader to a date
     * @param <var>expiryDate</var> date of expiry
     * @throws IllegalArgumentException if expiryDate is null
     * @throws SipParseException if expiryDate is not accepted by implementation
     */
    public void setExpires(Date expiryDate)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets boolean value to indicate if ContactHeader
     * has q-value
     * @return boolean value to indicate if ContactHeader
     * has q-value
     */
    public boolean hasQValue();
    
    /**
     * Gets boolean value to indicate if ContactHeader
     * has expires
     * @return boolean value to indicate if ContactHeader
     * has expires
     */
    public boolean hasExpires();
    
    /**
     * Sets q-value of ContactHeader
     * @param <var>qValue</var> q-value
     * @throws SipParseException if qValue is not accepted by implementation
     */
    public void setQValue(float qValue)
                 throws SipParseException;
    
    /**
     * Removes expires from ContactHeader (if it exists)
     */
    public void removeExpires();
    
    /**
     * Removes q-value from ContactHeader (if it exists)
     */
    public void removeQValue();
    
    /**
     * Gets boolean value to indicate if ContactHeader
     * has action
     * @return boolean value to indicate if ContactHeader
     * has action
     */
    public boolean hasAction();
    
    /**
     * Redirect action constant
     */
    public static final String ACTION_REDIRECT = "redirect";
    
    /**
     * Name of ContactHeader
     */
    public final static String name = "Contact";
    
    //////////////////////////////////////////////////////////////////
    
    /**
     * Proxy action constant
     */
    public static final String ACTION_PROXY = "proxy";
}
