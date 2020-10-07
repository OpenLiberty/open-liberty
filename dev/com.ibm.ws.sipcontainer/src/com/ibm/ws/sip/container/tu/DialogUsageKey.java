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
package com.ibm.ws.sip.container.tu;

import java.io.Serializable;
import java.util.HashMap;

/**
 * This class represents the key of usages. 
 * The usage key is used to differentiate between the usages.
 * 
 * Dialog Usage definition
 * =======================
 * 
 * Several methods in SIP can establish a dialog.  
 * When they do so, they also establish an association between 
 * the endpoints within that dialog.  
 * This association is known as a "dialog usage".  
 *  
 * @author nogat
 */
public class DialogUsageKey implements Serializable{
	
	/** method part of the key */
	private final String _method;

	/** additional key information */
	private final String _secondaryKey;

	/** cache of DialogUsageKey instances with _secondaryKey == null */
	private static final HashMap<String, DialogUsageKey> s_cache =
		new HashMap<String, DialogUsageKey>();

	/**
	 * gets an instance. if secondaryKey is null, the instance is cached.
	 * @param method method part of the key
	 * @param secondaryKey additional key information
	 * @return the DialogUsageKey instance
	 */
	public static DialogUsageKey instance(String method, String secondaryKey) {
		DialogUsageKey dialogUsageKey;
		if (secondaryKey == null) {
			dialogUsageKey = s_cache.get(method);
			if (dialogUsageKey == null) {
				// add to cache
				synchronized (s_cache) {
					dialogUsageKey = s_cache.get(method);
					if (dialogUsageKey == null) {
						dialogUsageKey = new DialogUsageKey(method, null);
						s_cache.put(method, dialogUsageKey);
					}
				}
			}
		}
		else {
			dialogUsageKey = new DialogUsageKey(method, secondaryKey);
		}
		return dialogUsageKey;
	}

	/**
	 * private constructor
	 */
	private DialogUsageKey(String method, String secondaryKey) {
		_method = method;
		_secondaryKey = secondaryKey;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (_method == null ? 0 : _method.hashCode())
			^ (_secondaryKey == null ? 0 : _secondaryKey.hashCode());
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DialogUsageKey)) {
			return false;
		}
		DialogUsageKey other = (DialogUsageKey)obj;

		// compare method
		if (_method == null) {
			if (other._method != null) {
				return false;
			}
		}
		else {
			if (other._method == null) {
				return false;
			}
			if (!_method.equals(other._method)) {
				return false;
			}
		}

		// compare additional key information
		if (_secondaryKey == null) {
			if (other._secondaryKey != null) {
				return false;
			}
		}
		else {
			if (other._secondaryKey == null) {
				return false;
			}
			if (!_secondaryKey.equals(other._secondaryKey)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (_method == null) {
			if (_secondaryKey == null) {
				return null;
			}
			return _secondaryKey;
		}
		else {
			if (_secondaryKey == null) {
				return _method;
			}
			return _method + _secondaryKey;
		}
	}

	/**
	 * @return method part of the key
	 */
	public String getMethod() {
		return _method;
	}

	/**
	 * @return additional key information
	 */
	public String getSecondaryKey() {
		return _secondaryKey;
	}
}
