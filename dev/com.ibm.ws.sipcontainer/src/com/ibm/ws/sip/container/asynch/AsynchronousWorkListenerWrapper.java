/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.asynch;

import java.io.Serializable;

import com.ibm.websphere.sip.AsynchronousWorkListener;
import com.ibm.ws.jain.protocol.ip.sip.message.SipResponseCodes;
import com.ibm.ws.sip.container.router.tasks.RoutedTask;

/**
 * A wrapper class that is used for running async listener tasks
 * in the sip container queues.
 * 
 * it can be set to run on completed or on failure according to the 
 * given parameters
 * 
 * @author asafz
 *
 */
public class AsynchronousWorkListenerWrapper extends RoutedTask{
	//action modes
	public static final int ON_COMPLETE = 1;
	public static final int ON_FAIL = 2;
	
	//the real listener
	private AsynchronousWorkListener _appAsynchWorkListener;
	
	//the result object in case of on complete
	private Serializable _result;
	
	//the reason number in case of on fail
	private int _reason;
	
	//the wrapper mode
	private int _mode = ON_COMPLETE; 
	
	/**
	 * 
	 * @return
	 */
	public int getReason() {
		return _reason;
	}

	/**
	 * 
	 * @param reason
	 */
	public void setReason(int reason) {
		this._reason = reason;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getMode() {
		return _mode;
	}

	/**
	 * 
	 * @param mode
	 */
	public void setMode(int mode) {
		_mode = mode;
	}

	/**
	 * 
	 * @return
	 */
	public Serializable getResult() {
		return _result;
	}

	/**
	 * 
	 * @param result
	 */
	public void setResult(Serializable result) {
		_result = result;
	}

	/**
	 * 
	 * @param asynchWorkListener
	 */
	public AsynchronousWorkListenerWrapper(AsynchronousWorkListener asynchWorkListener, long queueIndex) {
		_appAsynchWorkListener = asynchWorkListener;
		_index = queueIndex;
	}

	@Override
	protected void doTask() {
		//run on fail or on complete according to the mode
		switch (_mode) {
		case ON_COMPLETE:
			_appAsynchWorkListener.onCompleted(_result);
			break;

		case ON_FAIL:
			_appAsynchWorkListener.onFailed(_reason, SipResponseCodes.getResponseCodeText(_reason));
			break;	
		default:
			break;
		}
	}

	@Override
	public String getMethod() {
		return "Asynch Work Listener Wrapper work";
	}
}
