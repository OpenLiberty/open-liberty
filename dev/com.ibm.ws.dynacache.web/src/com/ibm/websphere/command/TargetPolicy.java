/*******************************************************************************
 * Copyright (c) 2000 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.command;

/**
 * The TargetPolicy interface declares one method, getCommandTarget(), which
 * implements the routine used to associate commands and targets. The
 * TargetPolicyDefault class provides an implementation of this interface,
 * but application programmers can override it to suit their needs, for
 * example, to allow mapping commands to targets through the use of
 * properties files or administrative applications.
 * 
 * @ibm-api
 */
public interface TargetPolicy 
{
	/**
	 * Retrieves the target associated with the command, as
         * determined by the target policy.
	 * 
	 * @param command The command whose target is requested.
	 * @return The target for the command. 
	 */
	public CommandTarget
	getCommandTarget(TargetableCommand command);
}
