/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.webcontainer.async;

import java.util.List;


/**
 * AsyncRequestDispatcherConfig is used to pass custom config for a particular request dispatcher 
 * including error messages, timeouts, etc. If various encodings are not specified, 
 * the default encoding value of the top level response's getCharacterEncoding() method is used.
 * 
 * @ibm-api
 *
 */
public interface AsyncRequestDispatcherConfig {
	/**
	 * Maximum amount of time results can be in the results store where it is still retrievable by the client.
	 * 
	 * @param includeTimeout timeout value in ms
	 */
	public void setExpirationTimeoutOverride(int includeTimeout);
	/**
	 *  Retrieve the maximum amount of time results can be in the results store where it is still retrievable by the client.
	 * 
	 * @return timeout value in ms
	 */
	public int getExpirationTimeoutOverride() ;

	/**
	 * Set the message displayed for any error that causes the output to be 
	 * unretrievable, such as the result expiring from the results store.
	 * 
	 * @param message custom failure message
	 * @param encoding
	 */
	public void setOutputRetrievalFailureMessage(String message, String encoding) ;
	/**
	 * Return the message displayed for any error that causes the output to be 
	 * unretrievable. This include expiration of content.
	 * 
	 * @return failure message
	 */
	public String getOutputRetrievalFailureMessage();
	/**
	 * Return the encoding used for the message displayed for any error that causes
	 * the output to be unretrievable.
	 * 
	 * @return encoding
	 */
	public String getOutputRetrievalFailureEncoding();

	/**
	 * Set the maximum length of time allowed for the execution of an include.
	 * Does not apply to insertFragmentBlocking calls.
	 * 
	 * @param ms timeout in ms
	 */
	public void setExecutionTimeoutOverride(int ms) ;
	/**
	 * Set the message displayed if the maximum length of time for the execution
	 * of an include is reached
	 * 
	 * @param message custom timeout message
	 * @param encoding 
	 */
	public void setExecutionTimeoutMessage(String message, String encoding);
	/**
	 * Return the message displayed if the maximum length of time for the execution
	 * of an include is reached. The default value is 60000 ms (or one minute). 
	 * If this execution timeout is reached, but the actual include results are ready 
	 * once the data can be flushed, preference is given to the actual results to be sent.
	 * 
	 * @return timeout message
	 */
	public String getExecutionTimeoutMessage();
	/**
	 * Return the maximum length of time allowed for the execution of an include
	 * 
	 * @return timeout in ms
	 */
	public int getExecutionTimeoutOverride();
	/**
	 * Return the encoding used for the message displayed if the maximum length of 
	 * time for the execution of an include has been reached
	 * 
	 * @return encoding
	 */
	public String getExecutionTimeoutEncoding();
	
	/**
	 * Set the message displayed when an include is rejected by the work manager
	 * 
	 * @param rejectedMessage custom error message
	 * @param rejectedEncoding encoding
	 */
	public void setRejectedMessage(String rejectedMessage,String rejectedEncoding);
	/**
	 * Return the message displayed when an include is rejected by the work manager
	 * 
	 * @return error message
	 */
	public String getRejectedMessage();
	/**
	 * Returns the encoding used to display the message when work is rejected
	 * 
	 * @return encdoding
	 */
	public String getRejectedEncoding();

	/**
	 * If isRetriable is true, a rejected include will attempt to be executed again. 
	 * Default is false.
	 * 
	 * @return true/false
	 */
	public boolean isRetriable() ;
	
	/**
	 * Set whether or not we will retry the include if we've received a rejected work item.
	 * 
	 * @param retriable
	 */
	public void setRetriable(boolean retriable) ;
	
	/**
	 * This must be set if the developer needs request attributes and internal headers 
	 * returned to the top level request. This only works for insertFragmentBlocking 
	 * since that is the only insertion method that guarantees the top level request 
	 * still exists.
	 * 
	 * @return true/false
	 */
	public boolean isTransferState();
	
	/**
	 * Set the value of transfer state.
	 * 
	 * @param state
	 */
	public void setTransferState(boolean state);
	
     /**
      * Returns true when ARD sends the javascript to aggregate the content on the client side.
      * Returns false when the customer must implement their own code to aggregate content
      * on the client side. They should retrieve the request attribute 
      * "com.ibm.websphere.webcontainer.ard.endpointURI" to get this endpoint and then make XMLHttpRequests
      * to retrieve the results
	 * @return true/false
	 */	
	public boolean isUseDefaultJavascript();
	
     /**
      * sets whether to allow ARD to insert javascript into customer code to retrieve
      * results for client-side aggregation.
	 * @param useDefaultJavascript
	 */
	public void setUseDefaultJavascript(boolean useDefaultJavascript);
	
	/**
     * Since the AsynchBeans executes the includes on a non-child thread,
     * InheritableThreadLocals will not work so a mechanism is provided
     * to propagate them
	 * @param threadLocals
	 */
	@SuppressWarnings("unchecked")
	public void addThreadLocalToPropagate(ThreadLocal threadLocals);
	
	/**
     * Since the AsynchBeans executes the includes on a non-child thread,
     * InheritableThreadLocals will not work. This method allows the retrieval
     * of the current set that need to be propagated.
	 * @param a list of ThreadLocals
	 */
	@SuppressWarnings("unchecked")
	public List<ThreadLocal> getThreadLocalsToPropagate();
	
	/**
     * Since the AsynchBeans executes the includes on a non-child thread,
     * InheritableThreadLocals will not work. This method clears the list
     * of ThreadLocals to propagate if any were set on a previous request.
	 */
	public void initThreadLocals();
}
