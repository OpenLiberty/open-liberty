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
package com.ibm.ws.diagnostics.zos.javadump;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.zos.command.processing.extension.CommandHandlerExtension;
import com.ibm.wsspi.zos.command.processing.ModifyResults;

/**
 * An implementation of an MVS console command handler that allows a
 * system operator to request a jvm heapdump.
 */
public class HeapdumpCommandHandler implements CommandHandlerExtension {

    /**
     * A human readable name for this handler.
     */
    protected static final String NAME = "Heapdump Command Handler";

    /** Supported commands */
    private static final String COMMAND_NAME = "HEAPDUMP";
    private static final String COMMAND_HELP = "HEAPDUMP,HELP";

    /**
     * Full Help text. Displays when command with help parameter is called and when
     * command with invalid parameter is called (Example: "HEAPDUMP,badParam")
     */
    protected static final List<String> HELP_TEXT_LONG = new ArrayList<String>();
    static {
        HELP_TEXT_LONG.add("Usage:");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add(" MODIFY <jobname.>identifier,heapdump");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add("Description:");
        HELP_TEXT_LONG.add(" This Modify command is used to request a Java");
        HELP_TEXT_LONG.add(" virtual machine (JVM) heap dump from a specific");
        HELP_TEXT_LONG.add(" Liberty server.");
    }

    /**
     * Short help text. Displays when command is not recognized and all command
     * handler help text is WTO.
     */
    protected static final List<String> HELP_TEXT_SHORT = new ArrayList<String>();
    static {
        HELP_TEXT_SHORT.add("To request a JVM heap dump from a Liberty server:");
        HELP_TEXT_SHORT.add("  MODIFY <jobname.>identifier,heapdump");
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

        if (command != null && command.toUpperCase().startsWith(COMMAND_NAME)) {
            if (command.equalsIgnoreCase(COMMAND_NAME)) {
                Set<String> dumps = new LinkedHashSet<String>();
                dumps.add("heap");
                process.createJavaDump(dumps);
                results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
            } else {
                // Set property so modify knows to print help MSGid
                results.setProperty(ModifyResults.HELP_RESPONSE_KEY, true);

                // Modify should complete successfully when HELP param used, otherwise
                // it completes unsuccessfully
                if (command.equalsIgnoreCase(COMMAND_HELP)) {
                    results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
                } else {
                    results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
                    responses.add("Could not parse command: \"" + command + "\"");
                }
                addHelpTextToResponse(responses);
            }
        } else {
            results.setCompletionStatus(ModifyResults.UNKNOWN_COMMAND);
        }
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