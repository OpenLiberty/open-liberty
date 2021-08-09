/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;

/**
 * Represents SIP responses. Instances of this class are passed to servlets
 * when the container receives incoming SIP responses and also, servlets
 * acting as UA servers or proxies generates SIP responses of their own by
 * creating <code>SipServletResponses</code>.
 * 
 * <p>SIP responses has a three-digit <em>status code</em> that indicates
 * the outcome of the corresponding request. Responses with a status code
 * in the range of 100-199 (1xx's) are called <em>provisional</em> or
 * <em>informational</em> and indicate progress in processing of the request.
 * Any response with a status code of 200 or higher is a <em>final</em>
 * response. A UA server may send only one final response per request
 * but this may be preceeded by any number of provisional responses.
 * 
 * <p>2xx responses indicate a successful outcome while 3xx-6xx indicate a
 * variety of non-success conditions.
 * 
 * <p>The summary of status codes is [RFC 3261]:
 * <ul>
 * <li><b>1xx:</b> Informational -- request received, continuing to process
 * the request
 * <li><b>2xx:</b> Success -- the action was successfully received,
 * understood, and accepted
 * <li><b>3xx:</b> Redirection -- further action needs to be taken in
 * order to complete the request
 * <li><b>4xx:</b> Client Error -- the request contains bad syntax or
 * cannot be fulfilled at this server
 * <li><b>5xx:</b> Server Error -- the server failed to fulfill an
 * apparently valid request
 * <li><b>6xx:</b> Global Failure -- the request cannot be fulfilled at any
 * server
 * </ul>
 */
public interface SipServletResponse extends ServletResponse, SipServletMessage {
    
    ///////////////////////////////////////////////////////////////////
    // Informational status codes - 1xx

    /**
     * Status code (202) indicating that the request has been accepted for
     * processing, but the processing has not been completed. The request
     * might or might not eventually be acted upon, as it might be disallowed
     * when processing actually takes place. There is no facility for
     * re-sending a status code from an asynchronous operation such as this.
     */
    static final int SC_ACCEPTED = 202;
    
    /**
     * Status code (484) indicating that the server received a request
     * with a To (Section 6.37) address or Request-URI that was 
     * incomplete.  Additional information SHOULD be provided.
     *
     * <p>Note: This status code allows overlapped dialing. With overlapped
     * dialing, the client does not know the length of the dialing
     * string.  It sends strings of increasing lengths, prompting the 
     * user for more input, until it no longer receives a 484 status 
     * response.
     */
    static final int SC_ADDRESS_INCOMPLETE = 484;

    /**
     * Status code (380) indicating alternative service.
     */
    static final int SC_ALTERNATIVE_SERVICE = 380;

    /**
     * Status code (485) indicating that the callee address provided
     * in the request was ambiguous.  The response MAY contain a 
     * listing of possible unambiguous addresses in Contact headers.
     *
     * <p>Revealing alternatives can infringe on privacy concerns of the
     * user or the organization. It MUST be possible to configure a 
     * server to respond with status 404 (Not Found) or to suppress
     * the listing of possible choices if the request with the URL
     * lee@example.com.
     *
     * <pre>
     * 485 Ambiguous SIP/2.0
     * Contact: Carol Lee &lt;sip:carol.lee@example.com&gt;
     * Contact: Ping Lee &lt;sip:p.lee@example.com&gt;
     * Contact: Lee M. Foote &lt;sip:lee.foote@example.com&gt;
     * </pre>
     *
     * <p>Some email and voice mail systems provide this functionality. A 
     * status code separate from 3xx is used since the semantics are
     * different: for 300, it is assumed that the same person or sevice
     * will be reached by the choices provided. While an automated choice
     * or sequential search makes sense for a 3xx response, user 
     * intervention is required for a 485 response.
     */
    static final int SC_AMBIGUOUS = 485;

    /**
     * Status code (489) indicating that the server did not understand the event 
     * package specified in a "Event" header field. 
     */
    static final int SC_BAD_EVENT = 489;
    
    /**
     * Status code (420) indicating that the server did not understand
     * the protocol extension specified in a Require (Section 6.30) 
     * header field.
     */
    static final int SC_BAD_EXTENSION = 420;
    

    /**
     * Status code (502) indicating that the server, while acting as
     * a gateway or proxy, received an invalid response from the
     * downstream server it accessed in attempting to fulfill the
     * request.
     */
    static final int SC_BAD_GATEWAY = 502;

    /**
     * Status code (436) indicating that the Identity-Info header contains a 
     * URI that cannot be dereferenced by the verifier (either the URI scheme 
     * is unsupported by the verifier, or the resource designated by the URI is 
     * otherwise unavailable). 
     */
    static final int SC_BAD_IDENTITY_INFO = 436;
    
    
    /**
     * Status code (400) indicating Bad Request.
     */
    static final int SC_BAD_REQUEST = 400;


    ///////////////////////////////////////////////////////////////////
    // Redirection status codes - 3xx

    /**
     * Status code (600) indicating that the callee's end system was
     * contacted successfully but the callee is busy and does not 
     * wish to take the call at this time.
     */
    static final int SC_BUSY_EVERYWHERE = 600;

    /**
     * Status code (486) indicating that the callee's end system was
     * contacted successfully but the callee is curently not willing
     * or able to take additional call.
     */
    static final int SC_BUSY_HERE = 486;

    /**
     * Status code (181) indicating the call is being forwarded.
     */
    static final int SC_CALL_BEING_FORWARDED = 181;

    /**
     * Status code (481) indicating Call Leg/Transaction does not exist.
     *
     * <p>This status is returned under two conditions: The server received
     * a BYE request that does not match any existing call leg or the
     * server received a CANCEL request that does not match any existing
     * transaction. (A server simply discards an ACK referring to an 
     * unknown transaction.)
     */
    static final int SC_CALL_LEG_DONE = 481;

    /**
     * Status code (182) indicating the call is queued.
     */
    static final int SC_CALL_QUEUED = 182;

    /**
     * Status code (412) indicating that the precondition given for 
     * the request has failed. 
     */
    static final int SC_CONDITIONAL_REQUEST_FAILED = 412;

    /**
     * Status code (603) indicating that the callee's machine was
     * successfully contacted but the user explicily does not wish
     * to or cannot participate. The response MAY indicate a better
     * time to call in the Retry-After header.
     */
    static final int SC_DECLINE = 603;

    /**
     * Status code (604) indicating that the server has authoritative 
     * information that the user indicated in the To request field
     * does not exist anywhere. Searching for the user elsewhere will
     * not yield an results.
     */
    static final int SC_DOES_NOT_EXIT_ANYWHERE = 604;

    /**
     * Status code (421) indicating that the UAS needs a particular
     * extension to process the request, but this extension is not
     * listed in a Supported header field in the request.
     */
    static final int SC_EXTENSION_REQUIRED = 421;

    /**
     * Status code (403) indicating that the caller is forbidden to make
     * such requests.
     */
    static final int SC_FORBIDDEN = 403;

    /**
     * Status code (410) indicating that the requested resource is no
     * longer available at the server an no forwarding address is known.
     * This condition is expected to be considered permanent. If the 
     * server does not know, or has no facility to determine, whether or
     * not the codition is permanent, the status code 404 (Not Found)
     * SHOULD be used instead.
     */
    static final int SC_GONE = 410;

    /**
     * Status code (423) indicating that the server is rejecting the
     * request because the expiration time of the resource refreshed
     * by the request is too short.
     */
    static final int SC_INTERVAL_TOO_BRIEF = 423;
    
    /**
     * Status code (438) indicating that the verifier receives a message 
     * with an Identity signature that does not correspond to the digest-string 
     * calculated by the verifier.
     */ 
    static final int SC_INVALID_IDENTITY_HEADER = 438;

    /**
     * Status code (482) indicating that the server received a request
     * with a Via (Section 6.40) path containing itself.
     */
    static final int SC_LOOP_DETECTED = 482;

    /**
     * Status code (513) indicating that the server was unable to process
     * the request since the message length exceeded its capabilities.
     */
    static final int SC_MESSAGE_TOO_LARGE = 513;

    /**
     * Status code (405) indicating that the method specified in the
     * Request-Line is not allowed for the address identified byt the
     * Request-URI. The response MUST include an Allow header field
     * containing a list of valid methods for the indicated address.
     */
    static final int SC_METHOD_NOT_ALLOWED = 405;

    /**
     * Status code (301) indicating that the callee has moved permanantly.
     */
    static final int SC_MOVED_PERMANENTLY = 301;

    /**
     * Status code (302) indicating that the callee has moved temporarily.
     */
    static final int SC_MOVED_TEMPORARILY = 302;

    /**
     * Status code (300) indicating Multiple Choices. i.e., user may be
     * reached at multiple locations.
     */
    static final int SC_MULTIPLE_CHOICES = 300;

    /**
     * Status code (406) indicating the the resource identified by the
     * request is only capable of generating response entities which
     * have content characteristics not acceptable according to the 
     * accept headers sent in the request.
     */
    static final int SC_NOT_ACCEPTABLE = 406;
    
    /**
     * Status code (606) indicating that the user's agent was contacted
     * successfully but some aspects of the session description such as
     * the requested media, bandwidth, or addressing style were not
     * acceptable.
     */
    static final int SC_NOT_ACCEPTABLE_ANYWHERE = 606;
    
    /**
     * Status code (488) indicating that the response has the same
     * meaning as 606 (Not Acceptable), but only applies to the
     * specific resource addressed by the Request-URI and the request
     * may succeed elsewhere.
     */
    static final int SC_NOT_ACCEPTABLE_HERE = 488;

    /**
     * Status code (404) indicating that the server had definitive 
     * information that the user does not exist at the domain 
     * specified in the Request-URI. This status is also returned if 
     * the domain in the Request-URI does not match any of the domains 
     * handled by the recipent of the request.
     */
    static final int SC_NOT_FOUND = 404;

    /**
     * Status code (501) indicating that the server does not support
     * the functionality required to fulfill the request.
     */
    static final int SC_NOT_IMPLEMENTED = 501;

    /**
     * Status code (200) indicating the request succeeded normally.
     */
    static final int SC_OK = 200;

    /**
     * Status code (402) indicating that the caller needs to make a payment.
     */
    static final int SC_PAYMENT_REQUIRED = 402;
    
    /**
     * Status code (580) indicating failure to meet certain preconditions. 
     */
    static final int SC_PRECONDITION_FAILURE = 580;
    
    /**
     * Status code (429) indicating that the referee must 
     * provide a valid Referred-By token. 
     */
    static final int SC_PROVIDE_REFERER_IDENTITY = 429;

    /**
     * Status code (407) indicating that the client MUST first 
     * authenticate itself with the proxy. The proxy MUST return a 
     * Proxy-Authenticate header field (section 6.26) containing a 
     * challenge applicable to the proxy for the requested resource.
     * The client MAY repeat the request with a suitable 
     * Proxy-Authorization header field (section 6.27). SIP access
     * authorization is explained in section 13.2 and 14.
     *
     * <p>This status code is used for applications where access to the
     * communication channel (e.g., a telephony gateway) rather than
     * the callee requires authentication.
     */
    static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;

    /**
     * Status code (413) indicating that the server si refusing to process
     * a request becaus the request entity is larger than the server is
     * willing or able to process. The server MAY close the connection to
     * prevent the client from continuing the request.
     *
     * <p>If the condition is temporary, teh server SHOULD include a 
     * Retry-After header field to indicate that it is temporary and after
     * what time the client MAY try again.
     */
    static final int SC_REQUEST_ENTITY_TOO_LARGE = 413;

    /**
     * Status code (491) indicating that the request was received by
     * a UAS that had a pending request within the same dialog.
     */
    static final int SC_REQUEST_PENDING = 491;

    /**
     * Status code (487) indicating that the request was terminated by
     * a BYE or CANCEL request.
     */
    static final int SC_REQUEST_TERMINATED = 487;

    /**
     * Status code (408) indicating that the server could not produce
     * a response, e.g., a user location, within the time indicated in
     * the Expires request-header field. The client MAY repeat the
     * request without modifications at any later time.
     */
    static final int SC_REQUEST_TIMEOUT = 408;
    
    /**
     * Status code (414) indicating that the server if refusing to
     * service the request because the Request-URI is longer than the
     * server is willing to interpret.
     */
    static final int SC_REQUEST_URI_TOO_LONG = 414;
    
    /**
     * Status code (180) indicating the server has located the callee,
     * and callee user agent is Ringing the callee.
     */
    static final int SC_RINGING = 180;
    
    /**
     * Status code (494) indicating that the client must initiate a 
     * security mechanism as defined in RFC 3329. 
     */
    static final int SC_SECURITY_AGREEMENT_REQUIRED = 494;
    
    /**
     * Status code (500) indicating that the server encountered an 
     * unexpected condition that prevented it from fulfilling the
     * request.
     */
    static final int SC_SERVER_INTERNAL_ERROR = 500;

    /**
     * Status code (504) indicating that the server did not receive
     * a timely response from an external server it accessed in
     * attempting to process the request.
     */
    static final int SC_SERVER_TIMEOUT = 504;



    /**
     * Status code (503) indicating that the server is currently 
     * unable to handle the request due to a temporary overloading
     * or maintenance of the server.
     */
    static final int SC_SERVICE_UNAVAILABLE = 503;
    
    /**
     * Status code (422) indicating that a request contained a Session-Expires 
     * header field with a duration below the minimum timer for the server. 
     */
    static final int SC_SESSION_INTERVAL_TOO_SMALL = 422;
    
    

    /**
     * Status code (183) carries miscellaneous call progress information.
     * The Reason-Phrase may convey more details about the call progress.
     */
    static final int SC_SESSION_PROGRESS = 183;

    /**
     * Status code (480) indicating that the callee's end system was
     * contacted successfully but the callee is currently unavailable
     * (e.g., not logged in or logged in such a manner as to preclude 
     * communication with the callee). The response MAY indicate a 
     * better time to call in the Retry-After header. The user could
     * also be available elsewhere (unbeknownst to this host), thus, 
     * this response does not terminate any searches.  The reason 
     * phrase SHOULD be setable by the user agent. Status 486 (Busy Here)
     * MAY be used to more precisely indicate a particular reason for
     * the call failure.
     *
     * <p>This status is also returned by a redirect server that recognizes
     * the user identified by the Request-URI, but does not currently
     * have a valide forwarding location for that user.
     */
    static final int SC_TEMPORARLY_UNAVAILABLE = 480;

    /**
     * Status code (483) indicating that the server received a request
     * that contains more Via entries (hops) (Section 6.40) than allowed
     * by the Max-Forwards (Section 6.23) header field.
     */
    static final int SC_TOO_MANY_HOPS = 483;

    /**
     * Status code (100) indicating the server is trying
     * to locate the callee.
     */
    static final int SC_TRYING = 100;

    /**
     * Status code (401) indicating that the caller is unauthorized to
     * make this request.
     */
    static final int SC_UNAUTHORIZED = 401;

    /**
     * Status code (493) indicating that the request was received by
     * a UAS that contained an encrypted MIME body for which the recipient
     * does not possess or will not provide an appropriate decryption key.
     */
    static final int SC_UNDECIPHERABLE = 493;


    /**
     * Status code (415) indicating that the server is refusing to 
     * service the request because the message body of the  request is
     * in a format not supported by the requested resource for the 
     * requested method. The server SHOULD return a list of acceptable 
     * formats using the Accept, Accept-Encoding and Accept-Language
     * header fields.
     */
    static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;

    /**
     * Status code (416) indicating that the server cannot process the
     * request because the scheme of the URI in the Request-URI is unknown
     * to the server.
     */
    static final int SC_UNSUPPORTED_URI_SCHEME = 416;

    /**
     * Status code (428) indicating that the request should 
     * be re-sent with an Identity header. 
     */
    static final int SC_USE_IDENTITY_HEADER = 428;
    
    /**
     * Status code (305) indicating that he call can be better
     * handled by the specified proxy server.
     */
    static final int SC_USE_PROXY = 305;

    /**
     * Status code (505) indicating that the server does not support,
     * the SIP protocol version that was used in the request message.
     */
    static final int SC_VERSION_NOT_SUPPORTED = 505;
    
    /**
     * Status code (409) indicating that the user already registered.
     */
    static final int SC_CONFLICT = 409;
    
    /**
     * Status code (417) indicating that it was unknown Resource-Priority.
     */
    static final int SC_UNKNOWN_RESOURCE_PRIORITY = 417;
    
    /**
     * Status code (424) indicating location information that was malformed 
     * or not satisfactory for the recipient's purpose or could not be dereferenced.
     */
    static final int SC_BAD_LOCATION_INFORMATION = 424;
    
    /** 
     * Status code (433) indicates that the server refused to fulfill the
     *  request because the requestor was anonymous.
     */
    static final int SC_ANONYMILY_DISALLOWED = 433;
    
    /** 
     * Status code (437) indicating that certificate cannot be validated
     */
    static final int SC_UNSUPPORTED_CERTIFICATE = 437;
    
    /**
     * The 204 (No Notification) response code indicates that the request 
     * was successful, but the notification associated with the request will
     * not be sent.  It is valid only in response to a SUBSCRIBE message
     * sent within an established dialog.
     */
    static final int SC_NO_NOTIFICATION = 204;
    
    /**
     * Returns an ACK request object corresponding to this response.
     * This method is used by servlets acting as UACs in order to 
     * acknowledge 2xx final responses to INVITE requests.
     * 
     * <p>Please note that applications do <em>not</em> generate ACKs for
     * non-2xx responses, as this is done by the container itself.
     * 
     * @return ACK request corresponding to this response
     * 
     * @throws IllegalStateException if the transaction state is such that
     *      it doesn't allow an ACK to be sent now, e.g. if the original
     *      request was not an INVITE, if this response is provisional only,
     *      or if an ACK has already been generated
     */
    SipServletRequest createAck() throws IllegalStateException;
    
    
    /**
     * Creates a PRACK request object corresponding to this response. 
     * This method is used by servlets acting as UACs in order to acknowledge 
     * reliable provisional responses to INVITE requests with PRACK (RFC 3262). 
     * 
     * @return PRACK request corresponding to this response 
     * 
     * @throws IllegalStateException - if the transaction state is such that it 
     * 				doesn't allow a PRACK to be sent now, 
     * 				e.g. if a PRACK has already been generated. 
     * @throws Rel100Exception - if the response is not a reliable provisional 
     * 				response or if the original request was not an INVITE.
     */
    SipServletRequest createPrack() throws Rel100Exception, IllegalStateException;
    
    /**
     * Returns an <code>Iterator</code> over all the realms associated with this
     * challenge response.
     *
     * @return <code>Iterator</code> over all the realms associated with this
     * challenge response.
     */
    Iterator<String> getChallengeRealms();
    
    /**
     * Always returns null. SIP is not a content transfer protocol and
     * having stream based content accessors is of little utility.
     * 
     * <p>Message content can be set using the {@link SipServletMessage#setContent(Object, String)}
     * method.
     * 
     * @throws IOException
     * 
     * @return null
     */
    ServletOutputStream getOutputStream() throws IOException;
    
    /**
     * Returns the <code>Proxy</code> object associated with the
     * transaction of this SIP response object. Such a Proxy object exists
     * if this is a response for a previously proxied request. Otherwise,
     * a <code>Proxy</code> object does not exist, and null is returned.
     * 
     * <p>Note that the container must return the <em>same</em>
     * <code>Proxy</code> instance whenever a servlet invokes
     * <code>getProxy</code> on messages belonging to the same transaction.
     * In particular, a response to a proxied request is associated with
     * the same <code>Proxy</code> object as is the original request.
     * 
     * @return the <code>Proxy</code> object associated with this response's
     *     transaction, or null if this response was not received for a
     *     previously proxied request
     */
    Proxy getProxy();
    
    /**
     * Retruns the ProxyBranch object associated with the transaction of this SIP 
     * response object. Such a ProxyBranch object exists if this is a response for 
     * a previously proxied request. Otherwise, a ProxyBranch  object does not exist, 
     * and null is returned.
     *
     * Note that the container must return the same ProxyBranch instance whenever a 
     * servlet invokes getProxyBranch on messages belonging to the same transaction. 
     * In particular, a response to a proxied request is associated with the same 
     * ProxyBranch object as was the request sent on that branch.
     *  
     * @return the ProxyBranch object associated with this response's transaction, or null 
     * 		   if this response was not received for a previously proxied request.
     */
    ProxyBranch getProxyBranch();
    
    /**
     * Returns the reason phrase for this response object.
     * 
     * @return reason phrase for this response
     */
    String getReasonPhrase();
    
    /**
     * Returns the request associated with this response. For responses
     * received for proxied requests, this method returns a request object
     * that represents the request as it was sent downstream.
     * 
     * <p>Proxying applications can use the request URI obtained from the
     * request object to correlate an incoming response to one of the several
     * destinations it has been proxied to.
     * 
     * @return request for which this response was generated
     */
    SipServletRequest getRequest();
    
    /**
     * Returns the status code of this response object.
     * 
     * <p>The Status-Code is a 3-digit integer result code that indicates
     * the outcome of the attempt to understand and satisfy the request.
     * 
     * @return status code of this response
     */
    int getStatus();

    /**
     * Always returns null. SIP is not a content transfer protocol and
     * having stream based content accessors is of little utility.
     * 
     * <p>Message content can be set using the {@link SipServletMessage#setContent(Object, String)}
     * method.
     * 
     * @return null
     * 
     * @throws IOException
     */
    PrintWriter getWriter() throws IOException;
    
    /**
     * Returns true if this is an intermediate final response that arrived 
     * on a ProxyBranch.
     * This method is used by SipServlet.doResponse() to delegate handling 
     * of any intermediate final responses received on the ProxyBranch to the 
     * SipServlet.doBranchResponse() method.
     *  
     * @return true if the response arrived on a ProxyBranch, false otherwise. 
     *         The method will also return false for a best final response chosen by the Proxy.
     * 
     * @see  SipServlet#doResponse(javax.servlet.sip.SipServletResponse)
     * 
     */
    boolean isBranchResponse();
    
    /**
     * Causes this response to be sent. This is used by servlets acting as
     * UASs to send provisional and final responses, and by proxies when
     * generating provisional responses.
     * 
     * @throws IOException if a transport error occurs when trying to
     *     send this response
     * @throws IllegalStateException if this response was received from
     *     downstream or if it has already been sent
     */
    void send() throws IOException;

    /**
     * Requests that this response be sent reliably using the 100rel
     * extension defined in RFC 3262. This method must only be invoked
     * for 1xx response other than 100, and only if the UAC indicated
     * support for the 100rel extension in the request and the container
     * supports it.
     * 
     * <p>Applications can test whether the container supports the 100rel
     * extension by checking whether an attribute with name
     * "javax.servlet.sip.100rel" exists in the <code>ServletContext</code>
     * and has a value which equals Boolean.TRUE.
     *
     * @throws Rel100Exception if one of the conditions for using the
     *     100rel extension is not satisfied.
     *     
     * @see <a href="http://www.ietf.org/rfc/rfc3262.txt">RFC 3262, Reliability of 
     * Provisional Responses in the Session Initiation Protocol (SIP)</a>
     */
    void sendReliably() throws Rel100Exception;
    
    /**
     * Sets the status code of this response object.
     * 
     * @param statusCode status code of this response
     */
    void setStatus(int statusCode);

    /**
     * Sets the status code and reason phrase of this response object.
     * 
     * @param statusCode status code of this response
     * @param reasonPhrase short textual description of the status code
     */
    void setStatus(int statusCode, String reasonPhrase);
}
