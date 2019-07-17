/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.sip;

import java.io.Serializable;

import com.ibm.ws.sip.container.osgi.AsynchronousWorkDispatcher;
import com.ibm.ws.sip.container.osgi.AsynchronousWorkHolder;

/**
 * The <code>AsynchronousWork</code> is an abstract class that should be overridden by an application that 
 * is interested in using asynchronous invocation of code.
 * The propose of using such invocation would be for 2 reasons
 * 1. The application code is executed on a certain server in a cluster while the SIP Application Session location 
 * 	  it is referring to is unknown and might be residing on a different server in the cluster. Using this API will make sure 
 * 	  the code is executed on the correct server.	
 * 2. SIP Container APIs are being accessed from a non-SIP container thread. This can happen if the code is not executed from
 *    a SIP servlet or any other SIP 1.0 or 1.1 API listener methods. In this case WebSphere Application Server 
 *    will need this code to be executed using an extension to this AsynchronousWork, so that the code will be executed from a  
 *    SIP container thread and by that eliminate the need for synchronization or other locking mechanisms on the related SIP Application Session elements.    
 * <p>
 *  
 * An application that extends this class should implement <code>doAsyncTask</code> abstract method that 
 * returns a <code>Serializable</code> object.
 * When an application needs to run an asynchronous work, it creates a <code>AsynchronousWork</code> object 
 * providing the appSessionId to which the work relates, 
 * and calls <code>dispatch</code> method with a <code>AsynchronousWorkListener</code> (optional).
 * This listener will receive a response when the work is completed. 
 * @see {@link com.ibm.websphere.sip.AsynchronousWorkListener} interface for details.
 * In case the application is not interested in a response, it should pass null in 
 * the <code>AsynchronousWorkListener</code> parameter.
 * <p>
 * A sip container that receives the message is responsible either to execute the work locally or to send it to 
 * another server in the cluster which manages the session.
 * After the message reaches the right server, or the right thread within the same server, 
 * the SIP container on this server invokes the <code>doAsyncTask</code> method 
 * where an application performs its work.
 *
 * <p>
 * Finally, after the work is done, a response (either success or failure) returns to the SIP container on the originating server 
 * and it invokes the appropriate <code>AsynchronousWorkListener</code> method to return a response to the application.
 * 
 * <p>
 * An example of using the asynchronous invocation is shown here: 
 * <pre>
 * 
 * public class MyListener implements AsynchronousWorkListener {
 *		public void onCompleted(Serializeable myResponse){
 *
 *		}
 *		public void onFailed(int reasonCode, String reason)	{
 *		}
 *	}
 *
 * public class MyClass extends AsynchronousWork {  
 * 	SipApplicationSession _sessionId;
 * 
 * 	public MyClass(String sessionId) { 
 *		super(sessionId); 
 *		_sessionId = sessionId; 
 *	} 
 * 
 *	// This is the code that will be invoked on the target machine/thread 
 *	public Serializeable doAsyncTask() { 
 *	  // Application code goes here...., For instance:
 *	  appSession = sessionUtils.getApplicationSession(_sessionId);
 *	  appSession.createRequest().....
 *
 *	  Serializeable myResponse = new MyResponse();
 *	  myResponse.setStatus(200);
 *	  return (myResponse);
 *	}
 * }
 * 
 * // When need to invoke the asynchronous call (e.g., when you receive the proprietary message): 
 *
 *	public void onMyMessage() {
 *		// Obtain the session ID from the message or somewhere else
 * 		String sessionId = obtainIdFromMessage();
 *
 *		MyClass myClass = new MyClass(sessionId);
 *
 *		// Create the listener
 *		MyListener myListener = new MyListener();
 *
 *		// Dispatch it
 *	 	myClass.dispatch(myListener);
 *	}
 * </pre>
 * 
 * @author Galina Rubinshtein, Dec 2008
 * @ibm-api
 */
public abstract class AsynchronousWork implements Serializable {
	
	private String _appSessionId;
	
	private transient AsynchronousWorkDispatcher _dispatcher;
	
	/**
	 * Constructor 
	 * 
	 * @param appSessionId id of the SipApplicationSession that the SIP signaling code to be invoked asynchronously
	 * is related to.  
	 */
	public AsynchronousWork(String appSessionId){
		_appSessionId = appSessionId;
    }
	
	/**
	 * The application should call this method to dispatch the message 
	 * to the right server or thread that will execute the asynchronous work task.
	 * 
	 * @param listener AsynchronousWorkListener object to receive notification when the work is done.
	 */
	public final void dispatch(AsynchronousWorkListener listener) {	
		if (_dispatcher == null){
			_dispatcher = AsynchronousWorkHolder.getAsynchWorkInstance().getAsynchWorkTaskObject(_appSessionId);
		}
		
		_dispatcher.dispatch(this, listener);
	}
	
	/**
	 * Wait until the response is received, this method will cause the current thread to be locked
	 * until the async work task is finished
	 * 
	 * The application should use it if it wishes to wait until the async work is finished
	 * 
	 * @param time - the maximum time to wait in milliseconds, 0 is for waiting forever
	 * @return Object - the result of the async task or null if it was not finished yet 
	 */
	public final Object waitForResponse(long time) {	
		Object result = null;
		
		//we only need to wait if the _dispatcher is not null -> the task is already dispatched
		if (_dispatcher != null){
			result = _dispatcher.waitForResponse(time);
		}
		
		return result;
	}
	
	/**
	 * This abstract method should be overridden by an application with 
	 * the work it wishes to invoke asynchorounously.
	 * Note: The SIP signaling code that will be implemented in this method must relate only to the SipApplicationSession
	 * which ID was passed to the constructor and not to any other SipApplicationSession. 
	 * Meaning that only the state of the related SIP dialogs (represented by that SipApplicationSession SipSessions) 
	 * and their timers or attributes can be modified here.
	 * 
	 * @return Serializable object that will pass to the AsynchronousWorkListener. 
	 * If the return value is null, then the listener, if exists, will not be invoked.
	 */
	public abstract Serializable doAsyncTask();
}
