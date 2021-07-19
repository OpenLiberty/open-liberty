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
package com.ibm.ws.diagnostics.zos.tdump;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.zos.command.processing.extension.CommandHandlerExtension;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.wsspi.zos.command.processing.ModifyResults;

/**
 * An implementation of an MVS console command handler that allows a
 * system operator to request a transaction dump.
 */
public class TdumpCommandHandler implements CommandHandlerExtension {

    /**
     * A human readable name for this handler.
     */
    final static String NAME = "TDUMP Command Handler";

    /**
     * string used when InitiateSystemDump returned a non zero return code.
     */
    public final String DUMP_ERROR = "InitiateSystemDump returned return code ";

    /**
     * string used when some other error occurred when attempting to take a transaction dump.
     */
    public final String INTERNAL_ERROR = "Internal error occurred. Error code is ";

    /** Supported commands */
    private static final String COMMAND_NAME = "TDUMP";
    private static final String COMMAND_HELP = "TDUMP,HELP";

    /**
     * Full help text. Displays when command with help parameter is called and when
     * command with invalid parameter is called (Example: "TDUMP,badParam")
     */
    protected static final List<String> HELP_TEXT_LONG = new ArrayList<String>();
    static {
        HELP_TEXT_LONG.add("Usage:");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add(" MODIFY <jobname.>identifier,tdump");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add("Description:");
        HELP_TEXT_LONG.add(" This Modify command is used to request a");
        HELP_TEXT_LONG.add(" transaction dump from a specific Liberty server.");
    }

    /**
     * Short help text. Displays when command is not recognized and all command
     * handler help text is WTO.
     */
    protected static final List<String> HELP_TEXT_SHORT = new ArrayList<String>();
    static {
        HELP_TEXT_SHORT.add("To request JVM to initiate a transaction dump:");
        HELP_TEXT_SHORT.add("  MODIFY <jobname.>identifier,tdump");
    }
    /**
     * native method manager reference.
     */
    protected NativeMethodManager nativeMethodManager = null;

    /**
     * DS method to activate this component.
     */
    protected void activate() {
        // Attempt to load native code via the method manager.
        nativeMethodManager.registerNatives(TdumpCommandHandler.class);
    }

    /**
     * DS method to deactivate this component.
     */
    protected void deactivate() {
    }

    /**
     * Method to set the native method manager.
     */
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
    }

    /**
     * Method to unset the native method manager.
     */
    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        if (this.nativeMethodManager == nativeMethodManager) {
            this.nativeMethodManager = null;
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
     * Get the name of this console command handler.
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
        results.setProperty(ModifyResults.HELP_RESPONSE_KEY, false);

        if (command != null && command.toUpperCase().startsWith(COMMAND_NAME)) {
            if (command.equalsIgnoreCase(COMMAND_NAME)) {
                int dumpReturnCode = ntv_takeTDump();
                if (dumpReturnCode != 0) {
                    if (dumpReturnCode > 0) {
                        responses.add(INTERNAL_ERROR + String.valueOf(dumpReturnCode));
                    } else {
                        responses.add(DUMP_ERROR + String.valueOf(dumpReturnCode));
                    }
                    results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
                } else {
                    results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
                }
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

        results.setResponsesContainMSGIDs(false);
        results.setResponses(responses);
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

    /**
     * Call to native code to request a transaction dump.
     *
     */
    protected native int ntv_takeTDump();

}
