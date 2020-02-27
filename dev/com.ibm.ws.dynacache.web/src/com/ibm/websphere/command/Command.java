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

import java.io.*;

/**
 * The Command interface defines the client side for fundamental command
 * functionality. A command encapsulates the set of operations that
 * makes up a business task. Commands provide the following advantages
 * over conventional client-server interaction:
 * <ul>
 * <li>They provide a simple, uniform way to call business logic, regardless
 *     of the programming style used for the business logic (for
 *     example, enterprise beans, JDBC, stored procedures, connectors,
 *     file access).</li>
 * <li>They reduce the number of messages used when a client interacts with a
 *     remote server, often replacing several messages that do small pieces
 *     of work with a single message that does all of them.</li>
 * <li>They provide local-remote transparency; the client-side command code
 *     is independent of whether the command is executed in the client's JVM or
 *     a remote JVM.</li>
 * </ul></p> 
 * <p>
 * The Command interface is extended by both the TargetableCommand and
 * CompensableCommand interfaces, which offer additional features.
 * The TargetableCommandImpl abstract class, which implements many of
 * the features of the TargetableCommand interface, provides a runtime for
 * command execution.</p>
 *
 * @ibm-api
 */
public interface Command 
extends Serializable 
{
	static final long serialVersionUID = 4736842876656373688L;
	
	/** 
	 * Executes the task encapsulated by this command. 
	 * This method calls the isReadyToCallExecute() method and
         * throws UnsetInputPropertiesException if the
         * isReadyToCallExecute() method returns <code>false</code>.</p>
         * <p>
         * This method is implemented in the TargetableCommandImpl class.</p>
	 * 
	 * @exception CommandException The superclass for all command exceptions. 
	 *            Specifically, UnsetInputPropertiesException is thrown if the
         *            isReadyToCallExecute() method returns <code>false</code>.
	 */
	public void 
	execute() 
		 throws CommandException;
	/** 
         * Indicates if all required input properties have been set.
         * The isReadyToCall() method is called in the client-side JVM
         * by the execute() before the command is given to the target
         * server to run, but an application programmer can also
         * call this method.</p>
         * <p> 
         * The programmer must determine the conditions under which
         * a command is considered ready to run and implement this
         * abstract method appropriately.</p>
	 * 
	 * @return The value <code>true</code> if the command is ready to execute. 
	 */
	public boolean
	isReadyToCallExecute();
	/** 
         * Sets the output properties to the values they had before the
         * the execute() method was run. After calling the reset() method, the
         * methods to get output properties no longer work, but the
         * isReadyToCallExecute() method returns true. The reset() method
         * provides a convenient and efficient way to reuse a command
         * instance after changing some input properties or the target.</p>
         * <p> 
         * The programmer must determine how to reset the input
         * properties and implement this abstract method accordingly.</p>
	 */
	public void 
	reset();
}
