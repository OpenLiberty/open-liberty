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

import java.rmi.*;

/**
 * The CommandTarget interface describes the object that handles the
 * execution of a targetable command. This object represents the server that
 * runs the command.</p>
 * <p>
 * The CommandTarget interface declares one method, executeCommand().
 * The command package provides an implementation of this interface in the
 * com.ibm.websphere.command.LocalTarget class. This class can be used when
 * the client and server reside in the same JVM. For convenience, this
 * target's name is aliased to the global (public static) variable LOCAL
 * in the TargetableCommandImpl class.</p>
 * <p>
 * Applications that need to run commands remotely must override this
 * local implementation of the CommandTarget interface. An implementation
 * of the executeCommand() method has three tasks:
 * <ul>
 * <li>If the target server is in a remote JVM, the target must copy the
 *     command to the server-side JVM using whatever protocol the server
 *     requires.</li>
 * <li>The target object must ensure that the command is executed in the
 *     server environment. It must call the methods from the TargetableCommand
 *     interface to perform appropriate actions. In most cases, this means
 *     calling the performExecute() method.</li>
 * <li>If the hasOutputProperties method on the command returns <code>true</code>,
 *     the target must return a copy of the command to the client with the
 *     get accessor methods loaded.</li>
 * </ul></p>
 * <p>
 * The CommandTarget interface can be implemented in a variety of ways.
 * One technique makes the target object a client-side adapter that deals
 * with the server's protocol. A technique that offers more transparent
 * local/remote communication makes the target a server-side object with
 * a client-side stub. An example of this technique uses an enterprise
 * bean's remote interface as the target. In this case, the target can be
 * used to run the command in the same server as another object.</p>
 * 
 * @ibm-api
 */
public interface CommandTarget 
{
	/**
	 * Submits the command for execution.
         * This method must copy the command to the server, invoke the
         * command on the server, and return any output properties to the client.
	 * 
	 * @param command The targetable command to be run. 
	 * @return The command after it has been run.
	 * If the hasOutputProperties() method on the command returns
         * <code>false</code>, this method can return null as a performance
         * optimization. 
	 * @exception RemoteException The superclass for all remote exceptions.
	 * This type is thrown in case a remote server (like an EJB server)
         * throws an RMI remote exception.
	 * @exception CommandException The superclass for all command exceptions.
	 */ 
	public TargetableCommand
	executeCommand(TargetableCommand command)
		 throws RemoteException, CommandException; 
}
