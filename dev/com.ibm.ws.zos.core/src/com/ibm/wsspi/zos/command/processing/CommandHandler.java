/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.zos.command.processing;

import java.util.List;

/**
 * A <code>CommandHandler</code> provides the ability to receive MVS console modify
 * commands, process them, and provide response messages for the command.
 * <p>
 * A <code>CommandHandler</code> is delivered MVS console commands which match
 * its filter. The filter value is defined via the property "modify.filter.regex".
 * <p>
 * A MVS modify command may be issued as "f <jobname.>identifier,command". The "command"
 * may consist of a command verb and optional parameters. Also, the entire "command"
 * may be issued within quotes to preserve mixed cased characters.
 * <p>
 * The following is an example of a MVS modify command for a <code>CommandHandler</code>
 * which interacts with the OSGi ConsoleSession support:
 * <code>f bbgzsrv,'osgi,scr info 51'</code>
 * <p>
 * The "command" delivered to the <code>CommandHandler</code> is "osgi,scr info 51".
 * Note that the single quotes have been removed.
 * <p>
 * The entire "command" portion of the MVS modify command, without the enclosing quotes,
 * if any, are passed to the <code>CommandHandler</code>'s <code>handleModify</code>
 * method.
 *
 * @see com.ibm.wsspi.zos.command.processing.ModifyResults For more information on providing results
 *      of processing a MVS modify command.
 */
public interface CommandHandler {

    /**
     * Configuration property identifying the regular expression used to match
     * the command string.
     * <p>
     * If this property is not set, the handler will be a target for all modify commands.
     * <p>
     * The following is an example of a filter property for a <code>CommandHandler</code>
     * receiving OSGi Console-like commands:
     * <code>"modify.filter.regex=(\(?i\)\(osgi\).*)"</code>
     * <p>
     * The filter above ignores case and matches any "command" that begins with "osgi".
     */
    public final String MODIFY_FILTER = "modify.filter.regex";

    /**
     * Configuration property indicating if help information should be displayed when a
     * "general" request for help information is requested. This expresses a desire
     * to "hide" this command from general use.
     */
    public final String DISPLAY_HELP = "display.command.help";

    /**
     * This method is driven to process a MVS command which matched the
     * <code>modify.filter.regex</code>.
     *
     * @param modifyCommmand modify command string
     *
     * @param results        object that can hold the result of processing the command
     */
    public void handleModify(String modifyCommmand, ModifyResults results);

    /**
     * <code>CommandHandler</code> identity (used in command responses)
     *
     * @return identity string for <code>CommandHandler</code>
     */
    public String getName();

    /**
     * Return help information for this <code>CommandHandler</code>.
     *
     * @return a list of strings that provide information about how to use this
     *         command handler. Each entry in the list issued as a line in an operator
     *         reply.
     */
    public List<String> getHelp();

}
