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
package com.ibm.ws.sip.container.asynch;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletsFactoryImpl;

/**
 * A singleton AsynchronousWorkTasksManager is used to keep the asynch work tasks.
 * The task are stored in a map using ASYNWORK Call-Id as a key of the task objects.
 * 
 * A sip container puts the tasks into the map when a new SIP ASYNWORK received and 
 * removes the task when it is completed.
 * 
 * @author galina
 *
 */
public class AsynchronousWorkTasksManager {
	
	// The singleton instance
	private static final AsynchronousWorkTasksManager _instance = new AsynchronousWorkTasksManager();
	
	// Map callId of SIP request to AsynchronousWorkTask
	private Map<String, AsynchronousWorkTask> _asynchWorkTasks;
	
	//the SipApplicationSession that will be used for all outgoing async messages from this server
	private SipApplicationSessionImpl _asynchSipApp = null;

	/**
	 * Constructor
	 */
	private AsynchronousWorkTasksManager() {
		_asynchWorkTasks = new ConcurrentHashMap<String, AsynchronousWorkTask>();
		_asynchSipApp = (SipApplicationSessionImpl) SipServletsFactoryImpl.getInstance().createApplicationSession();
		
		//set the asynch SAS to never expire
		_asynchSipApp.setExpires(0);
		//set the asynch SAS to not go thru the invalidate when ready mechanism
		_asynchSipApp.setInvalidateWhenReady(false);
	}

	/**
	 * AsynchronousWorkTasksManager is singleton.
	 * The method returns its only instance.
	 * 
	 * @return
	 */
	public static AsynchronousWorkTasksManager instance() {
		return _instance;
	}
	
	/**
	 * Get AsynchronousWorkTask by call-id
	 * 
	 * @param key
	 * @return
	 */
	public AsynchronousWorkTask getAsynchronousWorkTask(String key) {
		return _asynchWorkTasks.get(key);
	}
	
	/**
	 * Get all AsynchronousWorkTasks
	 * 
	 * @return Collection of AsynchronousWorkTask
	 */
	public Collection<AsynchronousWorkTask> getAllAsynchronousWorkTasks() {
		Collection<AsynchronousWorkTask> allAsynchronousWorkTasks = 
			new LinkedList<AsynchronousWorkTask>(_asynchWorkTasks.values());
		
		return allAsynchronousWorkTasks;
	}
	
	/**
	 * Put AsynchronousWorkTask 
	 * 
	 * @param call-id
	 * @return
	 */
	public void putAsynchronousWorkTask(String key, AsynchronousWorkTask value) {
		if(getAsynchronousWorkTask(key) == null) {
			_asynchWorkTasks.put(key, value);
		}
	}
	
	/**
	 * Remove AsynchronousWorkTask 
	 * 
	 * @param call-id
	 * @return
	 */
	public void removeAsynchronousWorkTask(String key) {
		if (key != null) {
			_asynchWorkTasks.remove(key);
		}
	}	
	
	/**
	 * @return the Asynch SAS
	 */
	public SipApplicationSessionImpl getAsynchSipApp() {
		return _asynchSipApp;
	}	
}