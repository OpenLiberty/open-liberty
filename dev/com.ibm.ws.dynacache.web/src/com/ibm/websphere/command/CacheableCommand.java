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
 * The CacheableCommand interface provides the contract between the
 * client and the command implementation for those
 * commands that participate in the command caching framework.
 * It allows caching metadata to be supplied.
 * @ibm-api 
 */
public interface CacheableCommand extends TargetableCommand, CommandCaller
{
    /**
     * This gets the cache id for the command.
     * It typically has a simple relationship to the
     * command class and input properties.
     * However, it can involve any logic that can be invoked in the
     * command client's JVM.
     *
     * @return The cache id.
     * @ibm-api 
     */
    public String getId();

    /**
     * This gets the sharing policy that dictates how distributed caching
     * is managed.
     *
     * @return Either EntryInfo.NOT_SHARED, EntryInfo.SHARED_PULL
     * or EntryInfo.SHARED_PUSH.
     * @ibm-api 
     */
    public int getSharingPolicy();

    /**
     * This gets the EntryInfo object for this command, which
     * holds caching metadata.
     *
     * @return The EntryInfo object.
     * @ibm-api 
     */
    public EntryInfo getEntryInfo();

    /**
     * Allows the command writer to perform actions on the cache
     * prior to the execution, and potential caching, of the command.
     *
     * @return True implies that the command's execution should be
     * terminated, so the performExecute, postExecute and setCommand
     * will not happen.
     * @ibm-api 
     */
    public boolean preExecute();

    /**
     * Allows the command writer to perform actions on the cache
     * after the execution, and potential caching, of the command.
     * Such actions might include invalidation of commands
     * made invalid by execution of this command.
     *
     * @ibm-api 
     */
    public void postExecute();

    /**
     * This sets the caller object for a caller who is cached.
     * The caller can be another command or a JSP.
     * The caller must support the CommandCaller interface.
     * It allows invalidation dependencies to be added to the caller.
     * <p>
     * The caller of a command must call this method on the
     * command some time after creating the new command instance,
     * but before calling the execute() method.
     *
     * @param caller The command that called this command.
     * @ibm-api 
     */
    public void setCaller(CommandCaller caller);

    /**
     * Returns the object that called this command instance.
     * The caller can be another command or a JSP.
     * The caller must support the CommandCaller interface.
     * It is used by this command to add invalidation dependencies
     * to the caller.
     * The caller must be explicitly set using the setCaller method.
     *
     * @return The caller of the command.
     * @ibm-api 
     */
    public CommandCaller getCaller();

    /**
     * executeFromCache This method will check the cache to see if the
     * given command is present.  If so then the command is populated with the
     * cached results and true is returned.  If the command is not cached, then
     * false is returned and no change is made to the state of the command.
     *
     * @return true if the command was retrieved from cache
     * @ibm-api 
     */
    public boolean executeFromCache() throws CommandException;

    /**
     * This method will cause the current command to be placed into the cache.
     * Any existing entry with the same cache id will be replaced.
     *
     * @ibm-api 
     */
    public void updateCache();
}
