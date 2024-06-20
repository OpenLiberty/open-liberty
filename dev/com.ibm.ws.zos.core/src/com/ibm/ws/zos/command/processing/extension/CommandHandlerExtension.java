/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.command.processing.extension;

import com.ibm.wsspi.zos.command.processing.CommandHandler;

/**
 * This interface is used to document behaviors associated with CommandHandler
 * help text on the MVS Console.
 *
 * Any unrecognized command or calling the base HELP command (F BBGZSRV,HELP)
 * should result in the shorthand help text of all the command handlers.
 * Each command handler should display two lines of help text which includes a
 * short description and the modify command syntax.
 *
 * Any valid command called with 'HELP' as a parameter will result in the specific
 * command handler being invoked and a longer help text being returned as the command response.
 * It will give a description of the commands help text along
 * with parameters that can be used and features that may are needed for the command
 * handler. Finally, the command response will complete successfully.
 *
 * Any valid command called with an unrecognized command will result in the same
 * behavior as a valid command called with 'HELP' as a parameter. However,
 * command responses will complete unsuccessfully.
 */
public interface CommandHandlerExtension extends CommandHandler {
}
