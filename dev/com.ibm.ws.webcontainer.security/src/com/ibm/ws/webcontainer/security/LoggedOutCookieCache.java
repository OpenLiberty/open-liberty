/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.security;

/**
 * Common interface to access a {@link LoggedOutCookieCache}.
 */
public interface LoggedOutCookieCache {
	/**
	 * Does the cache contain an entry for the specified key.
	 * 
	 * @param key The key to check.
	 * @return True if the cache contains the key; otherwise, false.
	 */
	public boolean contains(String key);

	/**
	 * Put the specified key / value pair into the cache.
	 * 
	 * @param key   The key to store the value for.
	 * @param value The value to store.
	 */
	public void put(String key, Object value);
}
