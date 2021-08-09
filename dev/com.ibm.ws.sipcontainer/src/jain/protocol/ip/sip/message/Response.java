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
package jain.protocol.ip.sip.message;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.ProxyAuthenticateHeader;
import jain.protocol.ip.sip.header.ServerHeader;

import java.util.List;

/**
 * <p>
 * This interface represents a SIP Response message.
 * After receiving and interpreting a Request, the recipient
 * responds with a Response. As well as Headers and a possible body,
 * Responses also contain a Status-Code and a Reason-Phrase.
 * </p><p>
 * The Status-Code is a 3-digit integer result code that indicates the
 * outcome of the attempt to understand and satisfy the Request. The
 * Reason-Phrase is intended to give a short textual description of the
 * Status-Code. The Status-Code is intended for use by automata, whereas
 * the Reason-Phrase is intended for the human user. The client is not
 * required to examine or display the Reason-Phrase.
 * </p><p>
 * SIP/2.0 Defines status codes in the following classes:
 * <LI><b>1xx: Informational</b> -- Indicates that the server or proxy contacted
 * is performing some further action and does not yet have a definitive
 * Response. The client should wait for a further Response from the
 * server, and the server should send such a Response without further
 * prompting. A server should send an Informational Response if it expects to take
 * more than 200 ms to obtain a final Response. A server may issue zero
 * or more Informational Responses, with no restriction on their ordering or
 * uniqueness. Note that Informational Responses are not transmitted reliably,
 * that is, they do not cause the client to send an AckMessage. Servers are
 * free to retransmit Informational Responses and clients can inquire
 * about the current state of call processing by re-sending the Request.</LI>
 * <LI><b>2xx: Success</b> -- The Request was successful and must terminate a search.</LI>
 * <LI><b>3xx: Redirection</b> -- Gives information about the user's new location, or
 * about alternative services that might be able to satisfy the call.
 * They should terminate an existing search, and may cause the initiator
 * to begin a new search if appropriate. Any Redirection Response must
 * not suggest any of the addresses in the ViaHeaders or the ContactHeader
 * of the Request. (Addresses match if their host and port number match.)
 * To avoid forwarding loops, a user agent client or proxy must check
 * whether the address returned by a redirect server equals an address
 * tried earlier.</LI>
 * <LI><b>4xx: Client Error</b> -- These are definite failure Responses from a particular
 * server.  The client should not retry the same Request without
 * modification (e.g., adding appropriate authorization). However, the
 * same Request to a different server might be successful.</LI>
 * <LI><b>5xx: Server Error</b> -- These are failure Responses given when a server itself has
 * erred. They are not definitive failures, and must not terminate a
 * search if other possible locations remain untried.</LI>
 * <LI><b>6xx: Global Failure</b> -- Indicates that a server has definitive information about
 * a particular user, not just the particular instance indicated in the
 * Request-URI. All further searches for this user are doomed to failure
 * and pending searches should be terminated.</LI>
 * <p>
 * SIP status codes are extensible. SIP applications are not required
 * to understand the meaning of all registered response codes, though
 * such understanding is obviously desirable. However, applications must
 * understand the class of any status code, as indicated by the first
 * digit, and treat any unrecognized status code as being equivalent to the
 * x00 status code of that class, with the exception that an
 * unrecognized status code must not be cached. For example, if a client
 * receives an unrecognized status code of 431, it can safely assume
 * that there was something wrong with its request and treat the
 * Response as if it had received a BAD_REQUEST(400) status code. In
 * such cases, user agents should present to the user the message body
 * returned with the Response, since that message body is likely to
 * include human-readable information which will explain the unusual
 * status.
 * </p><p>
 * Here is the list of currently defined status codes grouped by class
 * (x00 codes are in bold) :
 * </p>
 * <table BORDER WIDTH="75%" >
 * <tr>
 * <td><b>Class</b></td>
 * <td><b>Code</b></td>
 * </tr>
 * <tr>
 * <td>INFORMATIONAL (1xx)</td>
 * <td>
 * <LI><b>TRYING</b></LI>
 * <LI>RINGING</LI>
 * <LI>CALL_IS_BEING_FORWARDED</LI>
 * <LI>QUEUED</LI>
 * </td>
 * </tr>
 * <tr>
 * <td>SUCCESS (2xx)</td>
 * <td>
 * <LI><b>OK</b></LI>
 * </td>
 * </tr>
 * <tr>
 * <td>REDIRECTION (3xx)</td>
 * <td>
 * <LI><b>MULTIPLE_CHOICES</b></LI>
 * <LI>MOVED_PERMANENTLY</LI>
 * <LI>MOVED_TEMPORARILY</LI>
 * <LI>SEE_OTHER</LI>
 * <LI>USE_PROXY</LI>
 * <LI>ALTERNATIVE_SERVICE</LI>
 * </td>
 * </tr>
 * <tr>
 * <td>CLIENT_ERROR (4xx)</td>
 * <td>
 * <LI><b>BAD_REQUEST</b></LI>
 * <LI>UNAUTHORIZED</LI>
 * <LI>PAYMENT_REQUIRED</LI>
 * <LI>FORBIDDEN</LI>
 * <LI>NOT_FOUND</LI>
 * <LI>METHOD_NOT_ALLOWED</LI>
 * <LI>NOT_ACCEPTABLE</LI>
 * <LI>PROXY_AUTHENTICATION_REQUIRED</LI>
 * <LI>REQUEST_TIMEOUT</LI>
 * <LI>CONFLICT</LI>
 * <LI>GONE</LI>
 * <LI>LENGTH_REQUIRED</LI>
 * <LI>ENTITY_TOO_LARGE</LI>
 * <LI>URI_TOO_LARGE</LI>
 * <LI>UNSUPPORTED_MEDIA_TYPE</LI>
 * <LI>BAD_EXTENSION</LI>
 * <LI>TEMPORARILY_NOT_AVAILABLE</LI>
 * <LI>CALL_LEG_OR_TRANSACTION_DOES_NOT_EXIST</LI>
 * <LI>LOOP_DETECTED</LI>
 * <LI>TOO_MANY_HOPS</LI>
 * <LI>ADDRESS_INCOMPLETE</LI>
 * <LI>AMBIGUOUS</LI>
 * <LI>BUSY_HERE</LI>
 * </td>
 * </tr>
 * <tr>
 * <td>SERVER_ERROR (5xx)</td>
 * <td>
 * <LI><b>INTERNAL_SERVER_ERROR</b></LI>
 * <LI>NOT_IMPLEMENTED</LI>
 * <LI>BAD_GATEWAY</LI>
 * <LI>SERVICE_UNAVAILABLE</LI>
 * <LI>GATEWAY_TIME_OUT</LI>
 * <LI>SIP_VERSION_NOT_SUPPORTED</LI>
 * </td>
 * </tr>
 * <tr>
 * <td>GLOBAL_ERROR (6xx)</td>
 * <td>
 * <LI><b>BUSY_EVERYWHERE</b></LI>
 * <LI>DECLINE</LI>
 * <LI>DOES_NOT_EXIST_ANYWHERE</LI>
 * <LI>NOT_ACCEPTABLE_ANYWHERE</LI>
 * </td>
 * </tr>
 * </table>
 *
 * @version 1.0
 *
 */
public interface Response extends Message
{
    
    /**
     * Gets HeaderIterator of AllowHeaders of Response.
     * (Returns null if no AllowHeaders exist)
     * @return HeaderIterator of AllowHeaders of Response
     */
    public HeaderIterator getAllowHeaders();
    
    /**
     * Gets boolean value to indicate if Response
     * has AllowHeaders
     * @return boolean value to indicate if Response
     * has AllowHeaders
     */
    public boolean hasAllowHeaders();
    
    /**
     * Sets AllowHeaders of Response.
     * @param <var>allowHeaders</var> List of AllowHeaders to set
     * @throws IllegalArgumentException if allowHeaders is null, empty, contains
     * any elements that are null or not AllowHeaders from the same
     * JAIN SIP implementation
     */
    public void setAllowHeaders(List allowHeaders)
                 throws IllegalArgumentException;
    
    /**
     * Gets boolean value to indicate if Response
     * has UnsupportedHeaders
     * @return boolean value to indicate if Response
     * has UnsupportedHeaders
     */
    public boolean hasUnsupportedHeaders();
    
    /**
     * Sets UnsupportedHeaders of Response.
     * @param <var>unsupportedHeaders</var> List of UnsupportedHeaders to set
     * @throws IllegalArgumentException if unsupportedHeaders is null, empty, contains
     * any elements that are null or not UnsupportedHeaders from the same
     * JAIN SIP implementation
     */
    public void setUnsupportedHeaders(List unsupportedHeaders)
                 throws IllegalArgumentException;
    
    /**
     * Sets WarningHeaders of Response.
     * @param <var>warningHeaders</var> List of WarningHeaders to set
     * @throws IllegalArgumentException if warningHeaders is null, empty, contains
     * any elements that are null or not WarningHeaders from the same
     * JAIN SIP implementation
     */
    public void setWarningHeaders(List warningHeaders)
                 throws IllegalArgumentException;
    
    /**
     * Sets reason phrase of Response.
     * @param <var>reasonPhrase</var> reason phrase to set
     * @throws IllegalArgumentException if reasonPhrase is null
     * @throws SipParseException if reasonPhrase is not accepted by implementation
     */
    public void setReasonPhrase(String reasonPhrase)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Sets ServerHeader of Response.
     * @param <var>serverHeader</var> ServerHeader to set
     * @throws IllegalArgumentException if serverHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setServerHeader(ServerHeader serverHeader)
                 throws IllegalArgumentException;
    
    /**
     * Removes AllowHeaders from Response (if any exist)
     */
    public void removeAllowHeaders();
    
    /**
     * Gets boolean value to indicate if Response
     * has WarningHeaders
     * @return boolean value to indicate if Response
     * has WarningHeaders
     */
    public boolean hasWarningHeaders();
    
    /**
     * Gets ProxyAuthenticateHeader of Response.
     * (Returns null if no ProxyAuthenticateHeader exists)
     * @return HideHeader of Response
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public ProxyAuthenticateHeader getProxyAuthenticateHeader()
                                    throws HeaderParseException;
    
    /**
     * Sets status code of Response.
     * @param <var>statusCode</var> status code to set
     * @throws SipParseException if statusCode is not accepted by implementation
     */
    public void setStatusCode(int statusCode)
                 throws SipParseException;
    
    /**
     * Gets boolean value to indicate if Response
     * has ProxyAuthenticateHeader
     * @return boolean value to indicate if Response
     * has ProxyAuthenticateHeader
     */
    public boolean hasProxyAuthenticateHeader();
    
    /**
     * Removes ServerHeader from Response (if it exists)
     */
    public void removeServerHeader();
    
    /**
     * Removes ProxyAuthenticateHeader from Response (if it exists)
     */
    public void removeProxyAuthenticateHeader();
    
    /**
     * Gets HeaderIterator of UnsupportedHeaders of Response.
     * (Returns null if no UnsupportedHeaders exist)
     * @return HeaderIterator of UnsupportedHeaders of Response
     */
    public HeaderIterator getUnsupportedHeaders();
    
    /**
     * Sets ProxyAuthenticateHeader of Response.
     * @param <var>ProxyAuthenticateHeader</var> ProxyAuthenticateHeader to set
     * @throws IllegalArgumentException if proxyAuthenticateHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setProxyAuthenticateHeader(ProxyAuthenticateHeader proxyAuthenticateHeader)
                 throws IllegalArgumentException;
    
    /**
     * Removes UnsupportedHeaders from Response (if any exist)
     */
    public void removeUnsupportedHeaders();
    
    /**
     * Gets HeaderIterator of WWWAuthenticateHeaders of Response.
     * (Returns null if no WWWAuthenticateHeaders exist)
     * @return HeaderIterator of WWWAuthenticateHeaders of Response
     */
    public HeaderIterator getWWWAuthenticateHeaders();
    
    /**
     * Gets HeaderIterator of WarningHeaders of Response.
     * (Returns null if no WarningHeaders exist)
     * @return HeaderIterator of WarningHeaders of Response
     */
    public HeaderIterator getWarningHeaders();
    
    /**
     * Gets boolean value to indicate if Response
     * has WWWAuthenticateHeaders
     * @return boolean value to indicate if Response
     * has WWWAuthenticateHeaders
     */
    public boolean hasWWWAuthenticateHeaders();
    
    /**
     * Removes WarningHeaders from Response (if any exist)
     */
    public void removeWarningHeaders();
    
    /**
     * Removes WWWAuthenticateHeaders from Response (if any exist)
     */
    public void removeWWWAuthenticateHeaders();
    
    /**
     * Gets status code of Response.
     * @return status code of Response
     * @throws SipParseException if implementation cannot parse status code
     */
    public int getStatusCode()
                throws SipParseException;
    
    /**
     * Sets WWWAuthenticateHeaders of Response.
     * @param <var>wwwAuthenticateHeaders</var> List of WWWAuthenticateHeaders to set
     * @throws IllegalArgumentException if wwwAuthenticateHeaders is null, empty, contains
     * any elements that are null or not WWWAuthenticateHeaders from the same
     * JAIN SIP implementation
     */
    public void setWWWAuthenticateHeaders(List wwwAuthenticateHeaders)
                 throws IllegalArgumentException;
    
    /**
     * Gets reason phrase of Response.
     * @return reason phrase of Response
     * @throws SipParseException if implementation cannot parse reason phrase
     */
    public String getReasonPhrase()
                   throws SipParseException;
    
    /**
     * Gets ServerHeader of Response.
     * (Returns null if no ServerHeader exists)
     * @return ServerHeader of Response
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public ServerHeader getServerHeader()
                         throws HeaderParseException;
    
    /**
     * Gets boolean value to indicate if Response
     * has ServerHeader
     * @return boolean value to indicate if Response
     * has ServerHeader
     */
    public boolean hasServerHeader();
    
    /**
     * Removes first ViaHeader from Response's ViaHeaders.
     */
    public void removeViaHeader();
    
    /**
     * The called user agent has located a possible location where the user
     * has registered recently and is trying to alert the user.
     */
    public static final int RINGING = 180;
    
    /**
     * A proxy server may use this status code to indicate that the call is
     * being forwarded to a different set of destinations.
     */
    public static final int CALL_IS_BEING_FORWARDED = 181;
    
    /**
     * The called party is temporarily unavailable, but the callee has
     * decided to queue the call rather than reject it. When the callee
     * becomes available, it will return the appropriate final status
     * Response. The reason phrase may give further details about the status
     * of the call, e.g., "5 calls queued; expected waiting time is 15
     * minutes". The server may issue several QUEUED Responses to update the
     * caller about the status of the queued call.
     */
    public static final int QUEUED = 182;
    
    /**
     * The server, while acting as a gateway or proxy, received an invalid
     * Response from the downstream server it accessed in attempting to
     * fulfill the Request.
     */
    public static final int BAD_GATEWAY = 502;
    
    /**
     * The callee's end system was contacted successfully but the callee is
     * busy and does not wish to take the call at this time. The Response
     * may indicate a better time to call in the RetryAfterHeader. If the
     * callee does not wish to reveal the reason for declining the call, the
     * callee uses status code DECLINE instead. This status code
     * is returned only if the client knows that no other end point (such as
     * a voice mail system) will answer the Request. Otherwise, BUSY_HERE
     * should be returned.
     */
    public static final int BUSY_EVERYWHERE = 600;
    
    /**
     * The server is refusing to service the Request because the Request-URI
     * is longer than the server is willing to interpret.
     */
    public static final int REQUEST_URI_TOO_LARGE = 414;
    
    /**
     * This status is returned under two conditions: The server received a
     * ByeMessage that does not match any existing call leg or the server
     * received a CancelMessage that does not match any existing
     * transaction. (A server simply discards an AckMessage referring to an unknown
     * transaction.)
     */
    public static final int CALL_LEG_OR_TRANSACTION_DOES_NOT_EXIST = 481;
    
    /**
     * The callee address provided in the Request was ambiguous. The
     * Response may contain a listing of possible unambiguous addresses in
     * ContactHeaders. Revealing alternatives can infringe on privacy concerns of the user
     * or the organization. It must be possible to configure a server to
     * respond with status NOT_FOUND or to suppress the listing of
     * possible choices if the Request address was ambiguous.
     * Some email and voice mail systems provide this
     * functionality. A status code separate from Redirect status codes is used
     * since the semantics are different: for MULTIPLE_CHOICES, it is assumed
     * that the same person or service will be reached by the
     * choices provided. While an automated choice or sequential
     * search makes sense for a Redirect Response, user intervention is
     * required for an AMBIGUOUS response.
     */
    public static final int AMBIGUOUS = 485;
    
    /**
     * The Request has succeeded. The information returned with the Response
     * depends on the method used in the Request, for example:
     * <LI>BYE: The call has been terminated. The message body is empty.</LI>
     * <LI>CANCEL: The search has been cancelled. The message body is empty.</LI>
     * <LI>INVITE: The callee has agreed to participate; the message body
     * indicates the callee's capabilities.</LI>
     * <LI>OPTIONS: The callee has agreed to share its capabilities, included in
     * the message body.</LI>
     * <LI>REGISTER: The registration has succeeded. The client treats the
     * message body according to the ContentTypeHeader.</LI>
     */
    public static final int OK = 200;
    
    /**
     * The server refuses to accept the Request without a defined Content-
     * Length. The client may repeat the Request if it adds a valid
     * ContentLengthHeader containing the length of the message-body
     * in the Request.
     */
    public static final int LENGTH_REQUIRED = 411;
    
    /**
     * The address in the Request resolved to several choices, each with its
     * own specific location, and the user (or user agent) can select a
     * preferred communication end point and redirect its Request to that
     * location. The Response should include an entity containing a list of resource
     * characteristics and location(s) from which the user or user agent can
     * choose the one most appropriate, if allowed by the AcceptHeader.
     * The entity format is specified by the media type given in the
     * ContentTypeHeader. The choices should also be listed as
     * ContactHeaders.  Unlike HTTP, the SIP Response may
     * contain several ContactHeaders. User agents may use the ContactHeader
     * values for automatic redirection or may ask the user to confirm a choice.
     * However, the SIP specification does not define any standard for such
     * automatic selection. This status code is appropriate if the callee can be
     * reached at several different locations and the server
     * cannot or prefers not to proxy the Request.
     */
    public static final int MULTIPLE_CHOICES = 300;
    
    /**
     * The server did not understand the protocol extension specified in a
     * RequireHeader.
     */
    public static final int BAD_EXTENSION = 420;
    
    /**
     * The user can no longer be found at the address in the Request-URI and
     * the requesting client should retry at the new address(es) given by the
     * ContactHeader(s). The caller should update any local directories, address
     * books and user location caches with this new value and redirect future
     * Requests to the address(es) listed.
     */
    public static final int MOVED_PERMANENTLY = 301;
    
    /**
     * The server received a Request that contains more ViaHeaders
     * than allowed by the MaxForwardsHeader.
     */
    public static final int TOO_MANY_HOPS = 483;
    
    /**
     * The user's agent was contacted successfully but some aspects of the
     * session description such as the requested media, bandwidth, or
     * addressing style were not acceptable. A SESSION_NOT_ACCEPTABLE response
     * means that the user wishes to communicate, but cannot adequately support
     * the session described. The SESSION_NOT_ACCEPTABLE Response may
     * contain a list of reasons in a WarningHeader describing why the session
     * described cannot be supported. It is hoped that negotiation will not
     * frequently be needed, and when a new user is being invited to join an
     * already existing conference, negotiation may not be possible. It is up
     * to the invitation initiator to decide whether or not to act on a
     * SESSION_NOT_ACCEPTABLE Response.
     */
    public static final int SESSION_NOT_ACCEPTABLE = 606;
    
    /**
     * The server encountered an unexpected condition that prevented it from
     * fulfilling the Request. The client may display the specific error
     * condition, and may retry the Request after several seconds.
     */
    public static final int INTERNAL_SERVER_ERROR = 500;
    
    /**
     * See other.
     */
    public static final int SEE_OTHER = 303;
    
    /**
     * The server, while acting as a gateway, did not receive a timely
     * Response from the server (e.g., a location server) it accessed in
     * attempting to complete the Request.
     */
    public static final int GATEWAY_TIME_OUT = 504;
    
    /**
     * The requested resource must be accessed through the proxy given by
     * the ContactHeader. The ContactHeader gives the URI of the proxy. The
     * recipient is expected to repeat this single Request via the proxy.
     * USE_PROXY Responses must only be generated by user agent servers.
     */
    public static final int USE_PROXY = 305;
    
    /**
     * The server has authoritative information that the user indicated in
     * the ToHeader of the Request does not exist anywhere. Searching for the user
     * elsewhere will not yield any results.
     */
    public static final int DOES_NOT_EXIST_ANYWHERE = 604;
    
    /**
     * The call was not successful, but alternative services are possible.
     * The alternative services are described in the message body of the
     * Response. Formats for such bodies are not defined in RFC 2543,
     * and may be he subject of future standardization.
     */
    public static final int ALTERNATIVE_SERVICE = 380;
    
    /**
     * The server is refusing to process a Request because the Request
     * entity is larger than the server is willing or able to process. The
     * server may close the connection to prevent the client from continuing
     * the Request. If the condition is temporary, the server should include
     * a RetryAfterHeader to indicate that it is temporary and after what
     * time the client may try again.
     */
    public static final int ENTITY_TOO_LARGE = 413;
    
    /**
     * The Request could not be understood due to malformed syntax.
     */
    public static final int BAD_REQUEST = 400;
    
    /**
     * The server is refusing to service the Request because the message
     * body of the Request is in a format not supported by the requested
     * resource for the requested method. The server should return a list of
     * acceptable formats using AcceptHeaders, AcceptEncodingHeaders and AcceptLanguageHeaders.
     */
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;
    
    /**
     * The Request requires user authentication.
     */
    public static final int UNAUTHORIZED = 401;
    
    /**
     * The callee's end system was contacted successfully but the callee is
     * currently unavailable (e.g., not logged in or logged in in such a
     * manner as to preclude communication with the callee). The Response
     * may indicate a better time to call in the RetryAfterHeader. The
     * user could also be available elsewhere (unbeknownst to this host),
     * thus, this Response does not terminate any searches. The reason
     * phrase should indicate a more precise cause as to why the callee is
     * unavailable. This value should be setable by the user agent. Status
     * BUSY_HERE may be used to more precisely indicate a particular
     * reason for the call failure. This status is also returned by a redirect
     * server that recognizes the user identified by the Request-URI, but does
     * not currently have a valid forwarding location for that user.
     */
    public static final int TEMPORARILY_NOT_AVAILABLE = 480;
    
    /**
     * Reserved for future use.
     */
    public static final int PAYMENT_REQUIRED = 402;
    
    /**
     * The server received a Request with a ViaHeader containing itself.
     */
    public static final int LOOP_DETECTED = 482;
    
    /**
     * The server understood the Request, but is refusing to fulfill it.
     * Authorization will not help, and the Request should not be repeated.
     */
    public static final int FORBIDDEN = 403;
    
    /**
     * The server received a Request with a ToHeader address or
     * Request-URI that was incomplete. Additional information should be
     * provided. This status code allows overlapped dialing. With overlapped
     * dialing, the client does not know the length of the dialing
     * string. It sends strings of increasing lengths, prompting
     * the user for more input, until it no longer receives an ADDRESS_INCOMPLETE
     * status response.
     */
    public static final int ADDRESS_INCOMPLETE = 484;
    
    /**
     * The server has definitive information that the user does not exist at
     * the domain specified in the Request-URI. This status is also returned
     * if the domain in the Request-URI does not match any of the domains
     * handled by the recipient of the Request.
     */
    public static final int NOT_FOUND = 404;
    
    /**
     * The callee's end system was contacted successfully but the callee is
     * currently not willing or able to take additional calls. The Response
     * may indicate a better time to call in the RetryAfterHeader. The
     * user could also be available elsewhere, such as through a voice mail
     * service, thus, this Response does not terminate any searches.  Status
     * BUSY_EVERYWHERE should be used if the server knows that no
     * other end system will be able to accept this call.
     */
    public static final int BUSY_HERE = 486;
    
    /**
     * The method specified in the Request is not allowed for the
     * address identified by the Request-URI. The Response must include
     * AllowHeaders containing a valid methods for the indicated address.
     */
    public static final int METHOD_NOT_ALLOWED = 405;
    
    /**
     * The server does not support the functionality required to fulfill the
     * Request. This is the appropriate Response when the server does not
     * recognize the Request method and is not capable of supporting it for
     * any user.
     */
    public static final int NOT_IMPLEMENTED = 501;
    
    /**
     * The resource identified by the Request is only capable of generating
     * Response entities which have content characteristics not acceptable
     * according to the AcceptHeaders sent in the Request.
     */
    public static final int NOT_ACCEPTABLE = 406;
    
    /**
     * The server is currently unable to handle the Request due to a
     * temporary overloading or maintenance of the server. The implication
     * is that this is a temporary condition which will be alleviated after
     * some delay. If known, the length of the delay may be indicated in a
     * RetryAfterHeader. If no RetryAfterHeader is given, the client must
     * handle the response as it would for an INTERNAL_SERVER_ERROR Response.
     * Note: The existence of the SERVICE_UNAVAILABLE status code does not imply that a
     * server has to use it when becoming overloaded. Some servers may wish
     * to simply refuse the connection.
     */
    public static final int SERVICE_UNAVAILABLE = 503;
    
    /**
     * This code is similar to UNAUTHORIZED, but indicates that the
     * client must first authenticate itself with the proxy. The proxy must
     * return a ProxyAuthenticateHeader containing a challenge applicable to
     * the proxy for the requested resource. The client may repeat the Request
     * with a suitable ProxyAuthorizationHeader. This status code is used for
     * applications where access to the communication channel (e.g., a telephony
     * gateway) rather than the callee requires authentication.
     */
    public static final int PROXY_AUTHENTICATION_REQUIRED = 407;
    
    /**
     * The server does not support, or refuses to support, the SIP protocol
     * version that was used in the Request. The server is
     * indicating that it is unable or unwilling to complete the Request
     * using the same major version as the client, other than with this
     * error message. The Response may contain an entity describing why that
     * version is not supported and what other protocols are supported by
     * that server. The format for such an entity is not defined in RFC 2543
     * and may be the subject of future standardization.
     */
    public static final int SIP_VERSION_NOT_SUPPORTED = 505;
    
    /**
     * The server could not produce a Response, e.g., a user location,
     * within the time indicated in the ExpiresHeader of the Request. The
     * client may repeat the Request without modifications at any later
     * time.
     */
    public static final int REQUEST_TIMEOUT = 408;
    
    /**
     * The callee's machine was successfully contacted but the user
     * explicitly does not wish to or cannot participate. The Response may
     * indicate a better time to call in the RetryAfterHeader.
     */
    public static final int DECLINE = 603;
    
    /**
     * The Request could not be completed due to a conflict with the current
     * state of the resource. This status code is returned if the action
     * parameter in a RegisterMessage conflicts with existing registrations.
     */
    public static final int CONFLICT = 409;
    
    /**
     * Some unspecified action is being taken on behalf of this call (e.g.,
     * a database is being consulted), but the user has not yet been
     * located.
     */
    public static final int TRYING = 100;
    
    /**
     * The requested resource is no longer available at the server and no
     * forwarding address is known. This condition is expected to be
     * considered permanent. If the server does not know, or has no facility
     * to determine, whether or not the condition is permanent, the status
     * code NOT_FOUND should be used instead.
     */
    public static final int GONE = 410;
    
    /**
     * The requesting client should retry the Request at the new address(es)
     * given by the ContactHeader(s).  The duration of the redirection can be
     * indicated through an ExpiresHeader. If there is no explicit expiration time,
     * the address is only valid for this call and must not be cached for future calls.
     */
    public static final int MOVED_TEMPORARILY = 302;
}
