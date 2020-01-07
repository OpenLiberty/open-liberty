/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.web.command;

import java.io.*;

import com.ibm.websphere.command.CacheableCommand;
import com.ibm.websphere.command.CacheableCommandImpl;
import com.ibm.ws.cache.command.CommandStoragePolicy;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.util.ObjectSizer;
import com.ibm.ws.cache.util.SerializationUtility;
import com.ibm.ws.cache.EntryInfo;

/**
 * This class provides the default implementation of the CommandStoragePolicy
 * interface.
 * It caches the command in serialized form, and
 * makes a copy of the command when putting it in the cache and
 * when giving it out during a cache hit.
 */
public class SerializedCommandStorage implements CommandStoragePolicy {
    
    private static final long serialVersionUID = 3990461887543683654L;
    
	/**
	 * This implements the method in the CommandStoragePolicy interface.
	 *
	 * @param cacheableCommand The command to put in the cache.
	 * @return The cached representation of the command.
	 */
	public Serializable prepareForCache(CacheableCommand command) {
		try {
			byte[] b = SerializationUtility.serialize(command);
			setObjectSize(command, b);
			return b;
		} catch (Exception ex) {
			com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.command.SerializedCommandStorage.prepareForCache", "42", this);
			throw new IllegalStateException("serialization exception");
		}
	}

	/**
	 * This implements the method in the CommandStoragePolicy interface.
	 *
	 * @param object The cached representation of the command.
	 * @return The command that is given out during a cache hit.
	 */
	public CacheableCommand prepareForCacheAccess(Serializable inputObject, DCache cache, EntryInfo ei) {
		if (inputObject == null) {
			return null;
		}
		if (!(inputObject instanceof byte[])) {
			throw new IllegalStateException("inputObject is of type: " + inputObject.getClass());
		}
		byte[] array = (byte[]) inputObject;
		Object outputObject = null;
		try {
			outputObject = SerializationUtility.deserialize(array, cache.getCacheName());
			CacheableCommand cc = (CacheableCommand) outputObject;
			setObjectSize(cc, array);
			return cc;

		} catch (ClassCastException ex) {
			com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.command.SerializedCommandStorage.prepareForCacheAccess", "80", this);
			throw new IllegalStateException("deserialized object is of type " + outputObject.getClass());
		} catch (Exception ex) {
			com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.command.SerializedCommandStorage.prepareForCacheAccess", "84", this);

			if (cache.getCacheConfig().isIgnoreCacheableCommandDeserializationException())
				return null;
			
			throw new IllegalStateException("deserialization exception");
		}
	}
	
	private void setObjectSize(CacheableCommand command, byte[] b) {
		if (command instanceof CacheableCommandImpl){
			((CacheableCommandImpl)command).setObjectSize(ObjectSizer.getSize(b));
		}
	}

}
