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

import java.util.List;

/**
 * Represents the operation of proxying a SIP request.
 * 
 * <p>A number of parameters control how proxying is carried out:
 * <dl>
 * <dt><b>addToPath</b>:
 * <dd> addToPath: Whether the application adds a Path header to the REGISTER request. The default is false.
 * <dt><b>recurse</b>: 
 * <dd>Whether to autmotically recurse or not. The default is true.
 * 
 * <dt><b>recordRoute</b>: 
 * <dd>Whether to record-route or not. The default is false.
 * 
 * <dt><b>parallel</b>: 
 * <dd>Whether to proxy in parallel or sequentially. The default is true.
 * 
 * <dt><b>stateful</b>: 
 * <dd>Whether to remain transaction stateful for the duration of the
 *     proxying operation. The default is true.
 * 
 * <dt><b>supervised</b>: 
 * <dd>Whether the application will be invoked on incoming responses related to this proxying.
 * 
 * <dt><b>proxyTimeout</b>: 
 * <dd>The timeout for the proxy in general. In case the proxy is a sequential proxy
 * then this value behaves like the sequential-search-timeout
 * which is deprecated since v1.1. In case the proxy is a parallel proxy then this timeout acts as 
 * the timeout for the entire proxy i.e each of its parallel branches before it starts
 * to send out CANCELs waiting for final responses on all INVITE branches and sends the 
 * best final response upstream.  
 * 
 * <dt><b>sequentialSearchTimeout</b>:
 * <dd>The time the container waits for a final response before it CANCELs
 * the branch and proxies to the next destination in the target set. The usage of 
 * this explicit sequential timeout setting is <b>deprecated</b> and replaced by a general 
 * <code>proxyTimeout</code> parameter. 
 * </dl>
 * 
 * The effect of the various parameters is explained further below.
 */
public interface Proxy {

	/**
     * Cancels this proxy transaction and any of its child branches if recursion 
     * was enabled. 
     * 
     * @throws IllegalStateException if the transaction has completed
     */
    void cancel() throws IllegalStateException;

    /**
     * This overloaded method of cancel() provides a way to specify the 
     * reason for cancelling this Proxy by including the appropriate 
     * Reason headers [RFC 3326].
     * 
     * @param protocol - describes the source of the 'cause' 
     * 			field in the Reason header field.
     * @param reasonCode - corresponds to the 'cause' field. 
     * 			For eg, if protocol is SIP, the reasonCode 
     * 			would be the status code of the response which 
     * 			caused the cancel
     * @param reasonText - describes the reason for cancelling the Proxy.
     */
    void cancel(java.lang.String[] protocol,
            int[] reasonCode,
            java.lang.String[] reasonText);
    
    /**
     * Returns the list of {@link ProxyBranch} objects given 
     * a set of targets. The resulting branches will not 
     * have associated client transactions until 
     * startProxy() is invoked.
     * 
     * Implementations are required to support SipURI arguments 
     * and may support other types of URIs. 
     * 
     * @param targets - a list of {@link URI} objects to proxy to
     * 
     * @throws IllegalArgumentException - if any of the destination 
     * 			URIs contains a scheme that is not supported for proxying
     * 
     * @return list of created proxy branches.
     */
    List<ProxyBranch> createProxyBranches(List<? extends URI> targets) throws IllegalArgumentException;
    
    /**
     * Returns true if subsequent invocations of {@link Proxy#proxyTo(URI)} or 
     * {@link Proxy#startProxy()} will add a Path header to 
     * the proxied request, false otherwise. 
     * 
     * @return value of the "addToPath" flag 
     */
    boolean getAddToPath();
    
    
    /**
     * 
     * Returns true if the proxy will not cancel outstanding branches 
     * upon receiving the first 2xx INVITE response as in RFC 3841
     *
     * @return - true if the proxy will not cancel outstanding 
     * 	branches upon receiving the first 2xx response, 
     *  false otherwise      
     */
    boolean getNoCancel();

    /**
     * Returns the request received from the upstream caller.
     * 
     * @return object representing the incoming request that was proxied
     */
    SipServletRequest getOriginalRequest();

    /**
     * Returns true if this proxy object is set to proxy in parallel, or
     * false if it is set to proxy sequentially.
     * 
     * @return value of the "parallel" flag
     */
    boolean getParallel();
    
    
    /**
     * Returns a SipURI that the application can use to add parameters 
     * to the Path header. This may be used by Path header adding proxy 
     * applications in order to push state to the Registrar and have it 
     * returned in subsequent requests for the registered UA.
     * 
     * Parameters added through a URI returned by this method can be 
     * retrieved from a subsequent request in the same dialog by calling 
     * {@link javax.servlet.ServletRequest#getParameter(java.lang.String)}.
     * 
     * Note that the URI returned is good only for specifying a set of 
     * parameters that the application can retrieve when invoked to handle 
     * subsequent requests Other components of the URI are irrelevant 
     * and cannot be trusted to reflect the actual values that the container 
     * will be using when inserting a Path header into proxied request.      
     *
     * @return ProxyBranch associated with the uri
     * 
     * @throws IllegalStateException
     * 
     */
    SipURI getPathURI() throws IllegalStateException;
    
    
    /**
     * Any branch has a primary URI associated with it, using which it 
     * was created. The ProxyBranch may have been created using 
     * {@link Proxy#createProxyBranches(List)} method, implicitly 
     * when proxyTo() is called or when any of the proxy 
     * branch recurses as a result of a redirect response. A URI uniquely 
     * identifies a branch.      
     *  
     * @param uri - URI using which the ProxyBranch may have been created 
     * @return ProxyBranch associated with the uri
     */
    ProxyBranch getProxyBranch(URI uri);
    
    /**
     * 
     * More than one branches are associated with a proxy 
     * when {@link Proxy#proxyTo(List)} or {@link Proxy#createProxyBranches(List)}
     * is used. This method returns the top level branches thus created. 
     * If recursion is enabled on proxy or on any of its branches 
     * then on receipt of a 3xx class response on that branch, 
     * the branch may recurse into sub-branches. This method returns 
     * just the top level branches started.      
     *  
     * @return all the the top level branches associated with this proxy
     */ 
    List<ProxyBranch> getProxyBranches();
    
    /**
     * 
     * The current value of the overall proxy timeout value. 
     * This is measured in seconds. 
     * 
     * @return current value of proxy timeout in seconds.
     *      
     */
    int getProxyTimeout();
    
    /**
     * Returns true if subsequent invocations of {@link Proxy#proxyTo(URI)}
     * will add a Record-Route header to the proxied request, false otherwise.
     * 
     * @return value of the "recordroute" flag
     */
    boolean getRecordRoute();

    /**
     * 
     * Returns a <code>SipURI</code> that the application can use to
     * add parameters to the Record-Route header. This is used by
     * record-routing proxy applications in order to push state to the
     * endpoints and have it returned in subsequent requests belonging
     * to the same dialog.
     * 
     * <p>Parameters added through a URI returned by this method can
     * be retrieved from a subsequent request in the same dialog by
     * calling {@link javax.servlet.ServletRequest#getParameter(java.lang.String)}.
     * 
     * <p>Note that the URI returned is good <em>only</em> for
     * specifying a set of parameters that the application can
     * retrieve when invoked to handle subsequent requests in the same
     * dialog. Other components of the URI are irrelevant and cannot
     * be trusted to reflect the actual values that the container will
     * be using when inserting a Record-Route header into proxied
     * request.
     * 
     * @return SIP URI whose parameters can be modified and then retrieved
     *     by this application when processing subsequent requests in the
     *     same SIP dialog
     *     
     * @throws IllegalStateException if record-routing is not enabled
     */
    SipURI getRecordRouteURI() throws IllegalStateException;
    
    /**
     * Returns true if this proxy object is set to recurse, or false otherwise.
     *
     * @return true if proxying is enabled, false otherwise
     */
    boolean getRecurse();
    
    /**
     * @deprecated use a more general purpose {@link Proxy#getProxyTimeout()}
     *  
     * Returns the current value of the sequential search timeout parameter.
     * This is measured in seconds.
     * 
     * 
     * @return current value of the sequential search timeout parameter
     * 
     */
    int getSequentialSearchTimeout();
    
    /**
     * @deprecated stateless proxy is no longer supported
     * 
     * Returns true if this proxy operation is transaction stateful
     * (the default), or false if it is stateless.
     * 
     * @return value of the "stateful" flag
     */
    boolean getStateful();

    /**
     * 
     * Returns true if the controlling servlet will be invoked on incoming
     * responses for this proxying operation, and false otherwise.
     * 
     * @return true if the application will be invoked for responses,
     *      and false if not
     */
    boolean getSupervised();
    
    /**
     * Proxies a SIP request to the specified destination.
     * 
     * <p>Implementations are required to support <code>SipURI</code>
     * arguments and may support other types of URIs.
     * 
     * @param uri  specifies the destination to proxy to
     * 
     * @throws     java.lang.IllegalStateException    - if the 
     * 								transaction has already completed 
     * @throws     java.lang.IllegalArgumentException - if any of 
     * 								the destination URIs contains a 
     * 								scheme that is not supported for proxying 
     * @throws     java.lang.NullPointerException     - 
     * 								if any of the URI in the List is null.
     */
    void proxyTo(URI uri)throws IllegalStateException, 
	   						IllegalArgumentException, 
	   						NullPointerException;
    
    /**
     * Proxies a SIP request to the specified set of destinations.
     * 
     * @param uris  a list of {@link URI} objects to proxy to
     * 
     * @throws     java.lang.IllegalStateException    - if the 
     * 								transaction has already completed 
     * @throws     java.lang.IllegalArgumentException - if any of 
     * 								the destination URIs contains a 
     * 								scheme that is not supported for proxying 
     * @throws     java.lang.NullPointerException     - 
     * 								if any of the URI in the List is null.
     *      
     */
    void proxyTo(List<? extends URI> uris) throws IllegalStateException, 
    							   IllegalArgumentException, 
    							   NullPointerException;
    
    /**
     *
     * Specifies whether branches initiated in this proxy 
     * operation should include a Path header for the REGISTER 
     * request for this servlet container or not. The Path header 
     * field for the container should be on top of any application 
     * pushed Path header fields (pushed using the 
     * {@link SipServletRequest#pushPath(Address)} API).
     *
     * Path header is used to specify that this Proxy must stay 
     * on the signaling path of subsequent requests sent to the 
     * Registered UA from the Home Proxy in the network. 
     * As a best practice, before calling this method a proxy 
     * should check if the UA has indicated support for the Path 
     * extension by checking the Supported header field value in the 
     * request being proxied. The detailed procedure of Path header 
     * handling is defined in RFC 3327. 
     * 
     * @param addToPath - if true the container will add Path header
     */
    void 	setAddToPath(boolean addToPath);
    
    /**
     * Specifies whether the proxy should, or should not cancel 
     * outstanding branches upon receiving the first 2xx INVITE 
     * response as defined in RFC 3841.
     * 
     * The default proxy behavior, as per RFC 3261 section 16.7 
     * point 10, is to cancel outstanding branches upon receiving 
     * the first 2xx response; this method allows configuring the 
     * proxy to keep the branches and forward all 2xx responses upstream.
     * 
     * Default is false.
     *  
     * @param noCancel - when true, the proxy will not cancel outstanding 
     *                   branches upon receiving the first 2xx response
     */
    void 	setNoCancel(boolean noCancel);
    
    /**
     * In multi-homed environment this method can be used to select 
     * the outbound interface to use when sending requests for proxy 
     * branches. The specified address must be the address of one of 
     * the configured outbound interfaces. The set of SipURI objects 
     * which represent the supported outbound interfaces can be obtained 
     * from the servlet context attribute named javax.servlet.sip.outboundInterfaces.
     *      
     * Invocation of this method also impacts the system headers generated 
     * by the container for the branches, such as the Record-Route header 
     * {@link Proxy#getRecordRouteURI()} and the Via. The specified IP address 
     * is used to
     *  
     * construct these system headers.
     *  
     * @param address - the address which represents the outbound interface
     *
     * @throws     java.lang.IllegalStateException    - if this method is 
     * 										called on an invalidated session  
     * @throws     java.lang.IllegalArgumentException - if the address does 
     * 										not represent one of the container's 
     * 										outbound interfaces  
     * @throws     java.lang.NullPointerException     - on null address 
     * 
     */
    void 	setOutboundInterface(java.net.InetAddress address) throws IllegalStateException, 
																	IllegalArgumentException, 
																	NullPointerException;
    
    /**
     * In multi-homed environment this method can be used to select 
     * the outbound interface to use when sending requests for proxy 
     * branches. The specified address must be the address of one of 
     * the configured outbound interfaces. The set of SipURI objects 
     * which represent the supported outbound interfaces can be obtained 
     * from the servlet context attribute named javax.servlet.sip.outboundInterfaces.
     *      
     * Invocation of this method also impacts the system headers generated 
     * by the container for the branches, such as the Record-Route header 
     * {@link Proxy#getRecordRouteURI()} and the Via. The specified IP address 
     * is used to
     *  
     * construct these system headers.
     *  
     * @param address - the address which represents the outbound interface
     *
     * @throws     java.lang.IllegalStateException    - if this method is 
     * 										called on an invalidated session  
     * @throws     java.lang.IllegalArgumentException - if the address does 
     * 										not represent one of the container's 
     * 										outbound interfaces  
     * @throws     java.lang.NullPointerException     - on null address 
     * 
     */
    void 	setOutboundInterface(java.net.InetSocketAddress address) throws IllegalStateException, 
																	IllegalArgumentException, 
																	NullPointerException;
    
    /**
     * Specifies whether to proxy in parallel or sequentially.
     *
     * @param parallel  if true the servlet engine will proxy to all
     *      destinations in parallel, otherwise it will proxy to one at a time
     */
    void setParallel(boolean parallel);

    /**
     * Sets the overall proxy timeout. If this proxy is a sequential 
     * proxy then the behavior is same as the erstwhile 
     * {@link Proxy#setSequentialSearchTimeout(int)}. 
     * 
     * Further the value set through this method shall override any explicit 
     * sequential value set through deprecated {@link Proxy#setSequentialSearchTimeout(int)}.
     *  
     * On the other hand if the proxy is parallel then this acts as the upper 
     * limit for the entire proxy operation resulting in equivalent of invoking cancel() 
     * if the the proxy did not complete during this time, which means that a 
     * final response was not sent upstream.
     *       
     * @param seconds - seconds waited for each branch in case proxy is sequential 
     * 					and overall wait for parallel proxy.  
     * 
     * @throws IllegalArgumentException - if the container cannot set the value as 
     * 					requested because it is too high, too low or negative
     * 
     */
    void setProxyTimeout(int seconds) throws IllegalArgumentException;

    /**
     * Specifies whether branches initiated in this proxy operation should
     * include a Record-Route header for this servlet engine or not.
     * 
     * <p>Record-routing is used to specify that this servlet engine must
     * stay on the signaling path of subsequent requests. 
     *
     * @param recordRoute - if true the engine will record-route, otherwise it won't
     * 
     * @throws IllegalArgumentException - if the proxy has already been started
     */
    void setRecordRoute(boolean recordRoute);

    /**
     * Specifies whether the servlet engine will automatically recurse or not.
     * If recursion is enabled the servlet engine will automatically attempt
     * to proxy to contact addresses received in redirect (3xx) responses.
     * If recursion is disabled and no better response is received, a redirect
     * response will be passed to the application and will be passed upstream
     * towards the client.
     *
     * @param recurse   if true enables recursion, otherwise disables it
     */
    void setRecurse(boolean recurse);
    
    /**
     * @deprecated - use a more general purpose {@link Proxy#setProxyTimeout(int)}
     *                   
     * Sets the sequential search timeout value for this
     * <code>Proxy</code> object. This is the amount of time the
     * container waits for a final response when proxying
     * sequentially. When the timer expires the container CANCELs the
     * current branch and proxies to the next element in the target
     * set.
     * 
     * <p>The container is free to ignore this parameter.
     * 
     * @param seconds seconds waited for a final responses when proxying
     *     sequentially
     *     
     */
    void setSequentialSearchTimeout(int seconds);
	
	
	/**
     * @deprecated - stateless proxy is no longer supported
     * 
     * Specifies whether the server should proxy statelessly or not,
     * that is whether it should maintain transaction state whilst the
     * proxying operation is in progress.
     * 
     * <p>This proxy parameter is a hint only. Implementations may
     * choose to maintain transaction state regardless of the value of
     * this flag, but if so the application will not be invoked again
     * for this transaction.
     *
     * @param stateful  if true the proxy operation will be stateful
     * 
     */
    void setStateful(boolean stateful);
    
	/**
     * Specifies whether the controlling servlet is to be invoked for
     * subsequent events relating to this proxying transaction, i.e. for
     * incoming responses, CANCELs, and ACKs.
     *
     * @param supervised    if true, the servlet invoked to handle the request
     *      originally received will be notified when the "best" response
     *      is received.
     */
    void setSupervised(boolean supervised);
    
    /**
     * 
     * Proxies a SIP request to the set of destinations previously 
     * specified in {@link Proxy#createProxyBranches(java.util.List)}. 
     * 
     * This method will actually start the proxy branches and their 
     * associated client transactions. For example,
     * 
     * List branches = proxy.createProxyBranches(targets);
     * proxy.startProxy();
     *  
     * 
     * is essentially equivalent to Proxy.proxyTo(targets), 
     * with the former giving the application finer control over the 
     * individual proxy branches through the {@link ProxyBranch} class. 
     * 
     * Since the {@link Proxy#createProxyBranches(List)} can be invoked 
     * multiple times before the startProxy method the effect of startProxy is 
     * to start all the branches added in the target set. 
     * 
     * @throws java.lang.IllegalStateException
     */
	void startProxy() throws java.lang.IllegalStateException;

}
