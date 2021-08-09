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
 * This interface supports multiple storage policies for commands.
 * The default policy caches the command in serialized form, and
 * makes a copy of the command when putting it in the cache and
 * when giving it out during a cache hit.
 * A name of a class implementing this interface can be provided in the
 * dynacache.xml configuration file.
 */
public interface CommandStoragePolicy extends Serializable {

    /**
     * This converts the executed command into the cached representation.
     *
     * @param cacheableCommand The command to put in the cache.
     * @return The cached representation of the command.
     */
    public Serializable prepareForCache(CacheableCommand cacheableCommand);

    /**
     * This converts the cached representation of the command into something
     * that can be given out during a cache hit.
     *
     * @param object The cached representation of the command.
     * @return The command that is given out during a cache hit.
     */
    public CacheableCommand prepareForCacheAccess(Serializable object, DCache cache, EntryInfo ei);

}
