/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.command;

import java.io.*;
import com.ibm.websphere.command.*;
import com.ibm.ws.cache.EntryInfo;
import com.ibm.ws.cache.intf.DCache;

/**
 * This class provides the default implementation of the CommandStoragePolicy
 * interface.
 * It caches the command in serialized form, and
 * makes a copy of the command when putting it in the cache and
 * when giving it out during a cache hit.
 */
public class CommandStoragePolicyImpl implements CommandStoragePolicy
{
    private static final long serialVersionUID = 1275064778046836019L;
    
    /**
     * This implements the method in the CommandStoragePolicy interface.
     *
     * @param cacheableCommand The command to put in the cache.
     * @return The cached representation of the command.
     */
    public Serializable prepareForCache(CacheableCommand command)
    {
       return command;
    }

    /**
     * This implements the method in the CommandStoragePolicy interface.
     *
     * @param object The cached representation of the command.
     * @return The command that is given out during a cache hit.
     */
    public CacheableCommand prepareForCacheAccess(Serializable inputObject, DCache cache, EntryInfo ei)
    {
        return (CacheableCommand) inputObject;
    }
}
