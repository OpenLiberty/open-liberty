/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache;

import javax.cache.Cache;

public interface JCacheService {
	/**
	 * Get the JCache {@link Cache} instance that will be used to store objects.
	 * 
	 * @return The JCache {@link Cache} instance.
	 */
	public Cache<Object, byte[]> getCache();

	/**
	 * Deserialize a previously serialized object.
	 * 
	 * @param bytes The bytes of the serialized object.
	 * @return The object instance.
	 */
	public Object deserialize(byte[] bytes);

	/**
	 * Serialize an object into a byte array.
	 * 
	 * @param o The object to serialize.
	 * @return The bytes of the serialized object.
	 */
	public byte[] serialize(Object o);
}
