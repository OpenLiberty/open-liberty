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
 * The CompensableCommand interface allows you to make a command reversible.
 * When you make a command compensable, you associate a second command
 * with it. The second command compensates for the effects of the first
 * command by undoing its work as completely as possible.</p>
 * <p>
 * The CompensableCommand interface defines one method, getCompensatingCommand(),
 * which returns the compensating command associated with a command. The
 * application programmer must implement both this method and the compensating
 * command itself as part of implementing a compensable command.</p>
 * <p>
 * A client that wants to reverse a compensable command calls the compensating
 * command like this:
 * <pre>
 *       myCommand.getCompensatingCommand().performExecute();
 * </pre></p>
 * 
 * @ibm-api
 */
public interface CompensableCommand 
extends Command 
{
        /**
         * Retrieves the compensating command associated with the command.
         * Call this method only after the associated compensable command has
         * been run.</p>
         * <p>
         * The application programmer implements the getCompensatingCommand
         * method as part of writing a compensable command. For a compensating
         * command whose input properties are the output properties of the
         * original command, the following implementation is sufficient:
         * <pre>
         *      Command command = new MyCompensatingCommand();
         *      command.setInputPropertyX(outputPropertyP);
         *      return command;
         * </pre></p>
         *
         * @return The compensating command associated with the command.
         * @exception CommandException
         * The superclass for all command exceptions. Specifically,
         * UnavailableCompensatingCommandException is thrown if there is no
         * compensating command associated with the command.
	 */
	public Command
	getCompensatingCommand() throws CommandException;
}
