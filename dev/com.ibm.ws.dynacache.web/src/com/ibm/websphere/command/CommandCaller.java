/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.command;

import com.ibm.websphere.cache.EntryInfo;

/**
 * This interface allows the caller of a command to have additional
 * invalidation dependencies added by this command.
 * If the caller is a cacheable JSP or command, then it sets an object
 * that supports this interface on the command that is called
 * using the CacheableCommand.setCaller method.
 * Every CacheableCommand is a CommandCaller.
 * When the client is a command, it would call the following:
 * <pre>     command.setCaller(this);</pre>
 * The request object for a JSP/Servlet is a CommandCaller.
 * When the client is a JSP/Servlet, it would call the following:
 * <pre>     command.setCaller(request);</pre>
 * The called command gets the caller via the CacheableCommand.getCaller
 * method, and uses this interface to add the dependencies.
 * 
 * @ibm-api 
 */
public interface CommandCaller
{
    /**
     * This interface allows the caller of a command to have additional
     * invalidation dependencies added by the command.
     * It is called by the CacheableCommandImpl class.
     *
     * @param entryInfo The cache entry's EntryInfo object.
     * @ibm-api 
     */
    public void unionDependencies(EntryInfo entryInfo);
}
