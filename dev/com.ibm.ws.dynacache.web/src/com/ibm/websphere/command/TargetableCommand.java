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
 * The TargetableCommand interface describes a command that can be run in a
 * remote JVM. It extends the Command interface and declares methods that
 * support remote execution of commands. Some of the methods are implemented
 * in the TargetableCommandImpl class, and some must be implemented by the
 * application programmer.</p>
 * <p>
 * A targetable command must have a target, which represents the server
 * that will actually run the command. The target object is an instance of the
 * CommandTarget interface, and it is responsible for ensuring that the
 * command runs in the desired server environment. For each server, there
 * is at least one class that implements the CommandTarget interface.</p>
 * <p>
 * The TargetableCommand interface provides two ways for a client to specify
 * the target of a command:
 * <ul>
 * <li>The target object can be set directly on the command by using
 *     the setCommandTarget() method.</li>
 * <li>The name of the target object can be set on the command by
 *     using the setCommandTargetName() method.</li>
 * </ul></p>
 * 
 * @ibm-api
 */
public interface TargetableCommand 
extends Command
{
	/**
         * Returns the target object for the command. The target
         * object locates the server in which the command will run.</p>
         * <p>
         * This method is implemented in the TargetableCommandImpl class.</p>
         *
         * @return The target object for the command.
	 */
	public CommandTarget 
	getCommandTarget();
	/**
         * Returns the name of the target object for the command. The target
         * object locates the server in which the command will run.</p>
         * <p>
         * This abstract is implemented in the TargetableCommandImpl class.</p>
         *
         * @return The name of the target object for the command. The name
         *         is a fully qualified name for a Java class, for example,
         *         mypkg.bp.MyBusinessCmdTarget.
	 */
	public String
	getCommandTargetName();
	/** 
         * Indicates if the command has any output properties that must
         * be returned to the client. If there is nothing to
         * return to the client, this method can return <code>false</code> to
         * eliminate unecessary copying and remote invocations.</p>
         * <p>
         * This method is implemented in the TargetableCommandImpl class.</p>
         *
         * @return The value <code>true</code> if the command has output properties.
	 */
	public boolean
	hasOutputProperties();
	/**
         * Runs the business logic that makes up the command. The application
         * programmer implements the performExecute() method for a command.
         * The executeCommand() method in the target server invokes
         * the performExecute() method.</p>
         * <p>
         * This method must be implemented by the application programmer.</p>
         *
         * @exception Exception Any exception that occurs in the method will be thrown
         *            as an Exception.
	 */
	public abstract void
	performExecute()
		 throws Exception;
	/** 
         * Sets the target object on the command. The target object locates
         * the server in which the command will run.</p>
         * <p>
         * This method is implemented in the TargetableCommandImpl class.</p>
         *
         * @param commandTarget The target object for the command.
	 */
	public void 
	setCommandTarget(CommandTarget commandTarget);
	/**
         * Sets the name of the target object on the command. The target
         * object locates the server in which the command will run.</p>
         * <p>
         * This method is implemented in the TargetableCommandImpl class.</p>
         *
         * @param commandTargetName The name of the target object for the command.
         *                          The name is a fully qualified name for a Java
         *                          class, for example, mypkg.bp.MyBusinessCmdTarget.
	 */
	public void
	setCommandTargetName(String commandTargetName);
	/**
         * Sets the return values on the command. If the command is executed
         * in a remote JVM, the returned command is a different instance
         * than the client's command. In this case, the execute() method
         * in the TargetableCommand interface calls the setOutputProperties()
         * method to copy properties from the returned instance of the
         * command to the client's instance. If the hasOutputProperties()
         * method returns <code>false</code>, there is no need to invoke this method.</p>
         * <p>
         * This method is implemented in the TargetableCommandImpl class.</p>
         *
         * @param fromCommand The command from which the output properties are copied.
	 */
	public void
	setOutputProperties(TargetableCommand fromCommand);
}
