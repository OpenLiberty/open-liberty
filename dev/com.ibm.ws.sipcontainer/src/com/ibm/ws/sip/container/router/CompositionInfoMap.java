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
package com.ibm.ws.sip.container.router;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The class is a singelton class
 * contains a concurrent hash map that holds a <compositionId, compositionData> as <Key, Value>
 * we use this map to save the request's composition info before passing it to the next application
 * and then retrieves it back when the request returns to the container
 * 
 * @author Chaya Berezin-Chaimson
 */
public class CompositionInfoMap {
	
	private static final CompositionInfoMap _instance = new CompositionInfoMap();
	private ConcurrentHashMap<String, CompositionData> _compositionInfoMap = new ConcurrentHashMap<String, CompositionData>();
	
	/**
	 * Private constructor
	 */
	private CompositionInfoMap() {
		
	}
	
	/**
	 * This method will return the instance of the CompositionInfoMap class: _instance
	 * 
	 * @return _instance
	 */
	public static CompositionInfoMap getInstance() {
		return _instance;
	}
	
	/**
     * This method will add a new pair of <Key, Value> to the hash map
     * @param key, value
     */
	public void addCompositionInfo (String key, CompositionData value) {
		_compositionInfoMap.put(key, value);
	}
	
	/**
     * This method will remove a pair of <Key, Value> from the hash map according to a given key
     * and will return the CompositionData value of the removed pair
     * @param key
     * @return CompositionData
     */
	public CompositionData removeCompositionInfo (String key) {
		return _compositionInfoMap.remove(key);
	}
	
	/**
     * This method will return the CompositionData value of a pair of <Key, Value> from the hash map according to a given key
     * @param key
     * @return CompositionData
     */
	public CompositionData getCompositionInfoByKey (String key) {
		return _compositionInfoMap.get(key);
	}

}
