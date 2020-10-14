/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.diagnostics.zos.dump;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.zos.command.processing.extension.CommandHandlerExtension;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.zos.command.processing.ModifyResults;

/**
 * An implementation of an MVS console command handler that allows a
 * system operator to request a server dump.
 */
public class ServerDumpCommandHandler implements CommandHandlerExtension {

    /**
     * A human readable name for this handler.
     */
    protected static final String NAME = "Server Dump Command Handler";

    /**
     * Full help text. Displays when command with help parameter is called and when
     * command with invalid parameter is called (Example: "DUMP,badParam")
     */
    protected static final List<String> HELP_TEXT_LONG = new ArrayList<String>();
    static {
        HELP_TEXT_LONG.add("Usage:");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add(" MODIFY <jobname.>identifier,dump,include=javadump1,...");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add("Description:");
        HELP_TEXT_LONG.add(" This Modify command is used to request a server dump");
        HELP_TEXT_LONG.add(" from a specific Liberty server. The INCLUDE parameter");
        HELP_TEXT_LONG.add(" is optional.");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add("Valid Java dump types for INCLUDE parameter:");
        HELP_TEXT_LONG.add(" THREAD for a Java core dump");
        HELP_TEXT_LONG.add(" HEAP for a Java heap dump");
    }

    /**
     * Short help text. Displays when command is not recognized and all command
     * handler help text is WTO.
     */
    protected static final List<String> HELP_TEXT_SHORT = new ArrayList<String>();
    static {
        HELP_TEXT_SHORT.add("To request a server dump from a Liberty server:");
        HELP_TEXT_SHORT.add("  MODIFY <jobname.>identifier,dump,include=javadump1,...");
    }

    /** Supported commands */
    private static final String DUMP_COMMAND = "dump";
    private static final String DUMP_COMMAND_WITH_INCLUDE = "dump,include=";
    private static final String DUMP_COMMAND_HELP = "dump,help";
    private static final int DUMP_COMMAND_WITH_INCLUDE_LENGTH = DUMP_COMMAND_WITH_INCLUDE.length();

    protected static final Set<String> VALID_DUMPS = new LinkedHashSet<String>();
    static {
        VALID_DUMPS.addAll(Arrays.asList("thread", "heap"));
    }

    protected LibertyProcess process = null;

    protected void setLibertyProcess(LibertyProcess process) {
        this.process = process;
    }

    protected void unsetLibertyProcess(LibertyProcess process) {
        if (this.process == process) {
            this.process = null;
        }
    }

    protected WsLocationAdmin locationAdmin = null;

    protected void setLocationAdmin(WsLocationAdmin locationAdmin) {
        this.locationAdmin = locationAdmin;
    }

    protected void unsetLocationAdmin(WsLocationAdmin locationAdmin) {
        if (this.locationAdmin == locationAdmin) {
            this.locationAdmin = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getHelp() {
        return HELP_TEXT_SHORT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleModify(String command, ModifyResults results) {
        List<String> responses = new ArrayList<String>();
        results.setResponsesContainMSGIDs(false);
        results.setResponses(responses);
        results.setProperty(ModifyResults.HELP_RESPONSE_KEY, false);

        Set<String> dumps = getOptionalDumpSet(command);

        // Handles invalid input
        if (dumps == null) {
            // Print long help text to console when HELP param used and when invalid command
            // prefixed with 'dump' used
            if (command != null && command.toLowerCase().startsWith(DUMP_COMMAND)) {
                // Set property so modify knows to print help MSGid
                results.setProperty(ModifyResults.HELP_RESPONSE_KEY, true);

                // Modify should complete successfully when HELP param used, otherwise
                // it completes unsuccessfully
                if (command.equalsIgnoreCase(DUMP_COMMAND_HELP)) {
                    results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
                } else {
                    results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
                    responses.add("Could not parse command: \"" + command + "\"");
                }
                addHelpTextToResponse(responses);
            } else {
                // Set to UNKNOWN if command is unrecognized or null
                results.setCompletionStatus(ModifyResults.UNKNOWN_COMMAND);
            }
        } else {
            String dumpFileName = process.createServerDump(dumps);
            if (dumpFileName != null) {
                responses.add("Server " + locationAdmin.getServerName() + " dump complete in " + dumpFileName + ".");
                results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
            } else {
                responses.add("Error creating server dump, see server log for details.");
                results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
            }
        }
    }

    /**
     * Parses the command string to create a set of dump names to be included
     * in the server dump.
     *
     * @param command the command string
     * @return a set of dump names to be included in the server dump, or null if the command
     *         string contains a syntax error
     */
    private Set<String> getOptionalDumpSet(String command) {
        Set<String> dumps = new LinkedHashSet<String>();

        if (command == null) {
            return null;
        }

        command = command.toLowerCase();

        if (command.equals(DUMP_COMMAND)) {
            // no optional args were specified
            return dumps;
        }

        if (!command.startsWith(DUMP_COMMAND_WITH_INCLUDE)) {
            // command has an invalid syntax
            return null;
        }

        // to be consistent with server script behavior, unrecognized dump types are NOT considered a syntax
        // error and are simply ignored
        String[] requestedDumps = command.substring(DUMP_COMMAND_WITH_INCLUDE_LENGTH).split(",");
        for (String requestedDump : requestedDumps) {
            if (VALID_DUMPS.contains(requestedDump)) {
                dumps.add(requestedDump);
            }
        }
        return dumps;
    }

    /**
     * Adds the help text to the response array
     *
     * @param response
     */
    private void addHelpTextToResponse(List<String> responses) {
        for (String helpText : HELP_TEXT_LONG) {
            responses.add(helpText);
        }
    }
}