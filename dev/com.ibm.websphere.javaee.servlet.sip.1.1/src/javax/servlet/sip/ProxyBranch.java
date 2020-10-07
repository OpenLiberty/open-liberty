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

import java.net.InetSocketAddress;
import java.util.List;

/**
 * A ProxyBranch represents a branch which the Proxy sends out the request on. The ProxyBranch object models the branch as used in RFC3261 while describing a stateful proxy. For example,

 * public void doInvite(SipServletRequest req) {
 * ...
 * Proxy p = req.getProxy();
 * p.setRecordRoute(true);
 * List branches = p.createProxyBranches(targetList);
 * branches.get(0).setProxyBranchTimeout(5);
 * branches.get(1).setProxyBranchTimeout(10);
 * p.startProxy();
 * ...
 * }
 * 
 * @since 1.1
 */

public interface ProxyBranch {
	
	/**
	 * Cancels this branch and all the child branches if recursion is enabled and 
	 * sends a CANCEL to the proxied INVITEs.
	 * 
	 * The effect is similar to {@link Proxy#cancel()} except that it is 
	 * limited to this branch and its children only. 
	 *
	 * @throws IllegalStateException
	 *         if the transaction has already been completed
	 */
	void cancel();

	/**
	 * This overloaded method of {@link ProxyBranch#cancel()} provides a way to specify the
	 * reason for cancelling this branch by including the appropriate Reason headers [RFC 3326].
	 * 
	 * @param protocol describes the source of the 'cause' field in the Reason header field.
	 * @param reasonCode corresponds to the 'cause' field. For eg, if protocol is SIP, the reasonCode
	 * would be the status code of the response which caused the cancel
	 * @param reasonText describes the reason for cancelling the <code>Proxy</code>.
	 * @since 1.1
	 */
	void cancel(String[] protocol, int[] reasonCode, String[] reasonText);
	

	/**
	 * Returns true if subsequent invocations of proxyTo(URI) will add a Path
	 * header to the proxied request, false otherwise.
	 * 
	 * @return value of the "addToPath" flag
	 * 
	 * @since 1.1
	 */
	boolean getAddToPath();
	
	/**
	 * Returns a SipURI that the application can use to add 
	 * parameters to the Path header. This may be used by Path 
	 * header adding proxy applications in order to push state 
	 * to the Registrar and have it returned in subsequent 
	 * requests for the registered UA.
	 * 
	 * Parameters added through a URI returned by this method 
	 * can be retrieved from a subsequent request in the same 
	 * dialog by calling ServletRequest.getParameter(java.lang.String).
	 * 
	 * Note that the URI returned is good only for specifying a 
	 * set of parameters that the application can retrieve when 
	 * invoked to handle subsequent requests Other components of the 
	 * URI are irrelevant and cannot be trusted to reflect the actual 
	 * values that the container will be using when inserting a Path 
	 * header into proxied request.
	 *  
	 * @return SIP URI whose parameters can be modified and then retrieved 
	 * 			by this application when processing subsequent requests for the UA
	 * @throws IllegalStateException
	 *         SIP URI whose parameters can be modified and then retrieved 
	 *         by this application when processing subsequent requests for the UA
	 */
	SipURI getPathURI();
	
	/**
	 * 
	 * @return the associated proxy with this branch
	 */
	Proxy getProxy();
	
	/**
	 * Returns the current value of the search timeout associated 
	 * with this ProxyBranch object. If this value is not explicitly 
	 * set using the {@link ProxyBranch#setProxyBranchTimeout} then the value is 
	 * inherited from the Proxy setting.
	 * 
	 * @return the search timeout value in seconds
	 */
	int getProxyBranchTimeout();
	
	/**
	 * Returns true if subsequent invocations of proxyTo(URI)  
	 * will add a Record-Route header to the proxied request, 
	 * false otherwise.
	 *  
	 * @return - value of the "recordroute" flag
	 */
	boolean getRecordRoute();
	
	/**
	 * Returns a SipURI that the application can use to add parameters to the
	 * Record-Route header. This is used by record-routing proxy applications in
	 * order to push state to the endpoints and have it returned in subsequent
	 * requests belonging to the same dialog.
	 * 
	 * Parameters added through a URI returned by this method can be retrieved
	 * from a subsequent request in the same dialog by calling
	 * {link ServletRequest#getParameter(java.lang.String)}.
	 * 
	 * Note that the URI returned is good only for specifying a set of
	 * parameters that the application can retrieve when invoked to handle
	 * subsequent requests in the same dialog. Other components of the URI are
	 * irrelevant and cannot be trusted to reflect the actual values that the
	 * container will be using when inserting a Record-Route header into proxied
	 * request.
	 * 
	 * @return SIP URI whose parameters can be modified and then retrieved by
	 *         this application when processing subsequent requests in the same
	 *         SIP dialog
	 * 
	 * @throws IllegalStateException -
	 *             if record-routing is not enabled
	 */
	SipURI getRecordRouteURI() throws IllegalStateException;
	
	/**
     * Returns true if this proxy object is set to recurse, or false otherwise.
     *
     * @return true if proxying is enabled, false otherwise
     * 
     * @since 1.1
     */
    boolean getRecurse();
    
	/**
	 * Receipt of a 3xx class redirect response on a branch can result in
	 * recursed branches if the proxy or the branch has recursion enabled. This
	 * can result in several levels of recursed branches in a tree like fashion.
	 * This method returns the top level branches directly below this
	 * ProxyBranch
	 * 
	 * @return the top level branches below this ProxyBranch
	 */
	List<ProxyBranch> getRecursedProxyBranches();
    
	

	
	/**
	 * Returns the request associated with this branch.
	 * @return object representing the request that is or to be proxied.
	 */
	SipServletRequest getRequest();
	
	/**
	 * Returns the last response received on this branch.
	 * 
	 * @return the last SipServletResponse received, or null if no 
	 * 		   response has been received so far.
	 */
	SipServletResponse getResponse();
	
	/** 
	 * The branch can be created using {@link Proxy#createProxyBranches(List)} and may
	 * be started at a later time by using {@link Proxy#startProxy()}. This method tells
	 * if the given branch has been started yet or not. The branches created as
	 * a result of proxyTo are always started on creation.
	 * 
	 * @return whether the branch has been started or not
	 */
	boolean isStarted();
	
    /**
	 * Specifies whether branches initiated in this proxy operation should
	 * include a Path header for the REGISTER request for this servlet container
	 * or not.
	 * 
	 * Path header is used to specify that this Proxy must stay on the signaling
	 * path of subsequent requests sent to the Registered UA from the Home Proxy
	 * in the network. The detailed procedure of Path header handling is defined
	 * in RFC 3327.
	 * 
	 * @param p - if true the container will add Path header
	 * 
	 * @since 1.1
	 */
	void setAddToPath(boolean p);

	
	/**
	 * If multihoming is supported, then this method can be used to select the
	 * outbound interface to when forwarding requests for this proxy branch. The
	 * specified address must be the address of one of the configured outbound
	 * interfaces. The set of SipURI objects which represent the supported
	 * outbound interfaces can be obtained from the servlet context attribute
	 * named javax.servlet.sip.outboundInterfaces.
	 * 
	 * Invocation of this method also impacts the system headers generated by the 
	 * container for the branch, such as the Record-Route header 
	 * {@link ProxyBranch#getRecordRouteURI()} 
	 * and the Via. The supplied IP address is used to construct these system headers. 
	 * 
	 * @param address - the address which represents the outbound interface
	 *  
	 * @throws IllegalStateException    - if this method is called on an invalidated session  
	 * @throws IllegalArgumentException - if the uri is not understood by the container as one of its
	 *             				   outbound interface
	 * @throws NullPointerException     - on null address
	 *               
	 */
	void setOutboundInterface(InetSocketAddress address) throws	IllegalStateException,	
																	IllegalArgumentException,	
																	NullPointerException;
	
	
	/**
	 * If multihoming is supported, then this method can be used to select the
	 * outbound interface to when forwarding requests for this proxy branch. The
	 * specified address must be the address of one of the configured outbound
	 * interfaces. The set of SipURI objects which represent the supported
	 * outbound interfaces can be obtained from the servlet context attribute
	 * named javax.servlet.sip.outboundInterfaces.
	 * 
	 * Invocation of this method also impacts the system headers generated by the 
	 * container for the branch, such as the Record-Route header 
	 * {@link ProxyBranch#getRecordRouteURI()}
	 * and the Via. The supplied IP address is used to construct these system headers. 
	 * 
	 * @param address - the address which represents the outbound interface
	 *  
	 * @throws IllegalStateException    - if this method is called on an invalidated session  
	 * @throws IllegalArgumentException - if the uri is not understood by the container as one of its
	 *             				   outbound interface
	 * @throws NullPointerException     - on null address
	 *               
	 */
	void setOutboundInterface(java.net.InetAddress address) throws	IllegalStateException,	
																	IllegalArgumentException,	
																	NullPointerException;
	
	
	
	/**
	 * Sets the search timeout value for this ProxyBranch object. This is the
	 * amount of time the container waits for a final response when proxying on
	 * this branch. This method can be used to override the default timeout the
	 * branch obtains from the Proxy.setProxyTimeout(int) object. When the timer
	 * expires the container CANCELs this branch and proxies to the next element
	 * in the target set in case the proxy is a sequential proxy. In case the
	 * proxy is a parallel proxy then this can only set the timeout value of
	 * this branch to a value lower than the value in the proxy
	 * Proxy.getProxyTimeout(). The effect of expiry of this timeout in case of
	 * parallel proxy is just to cancel this branch as if an explicit call to
	 * cancel() has been made.
	 * 
	 * @param seconds
	 *            new search timeout in seconds
	 * 
	 * @throws IllegalArgumentException
	 *             if this value cannot be set by the container. Either it is
	 *             too high, too low, negative or greater than the overall proxy
	 *             timeout value in parallel case.
	 */
	void setProxyBranchTimeout(int seconds) throws IllegalArgumentException;

	/**
	 * Specifies whether this branch should include a Record-Route header 
	 * for this servlet engine or not.
	 *  
	 * Record-routing is used to specify that this servlet engine 
	 * must stay on the signaling path of subsequent requests.
	 * 
	 * @param includeRecordRoute - if true the engine will record-route, otherwise it won't
	 * 
	 * @throws IllegalStateException - if the proxy has already been started
	 * 
	 */
	void setRecordRoute (boolean includeRecordRoute);
	    

    /**
     * Specifies whether the servlet engine will automatically recurse or not.
     * If recursion is enabled the servlet engine will automatically attempt
     * to proxy to contact addresses received in redirect (3xx) responses.
     * If recursion is disabled and no better response is received, a redirect
     * response will be passed to the application and will be passed upstream
     * towards the client.
     *
     * @param recurse   if true enables recursion, otherwise disables it
     * 
     * @since 1.1
     */
    void setRecurse(boolean recurse);
    
}
