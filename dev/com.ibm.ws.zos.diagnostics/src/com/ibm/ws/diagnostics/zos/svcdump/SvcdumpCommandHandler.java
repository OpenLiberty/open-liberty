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
package com.ibm.ws.diagnostics.zos.svcdump;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.zos.command.processing.extension.CommandHandlerExtension;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.wsspi.zos.command.processing.ModifyResults;

/**
 * An implementation of an MVS console command handler that allows a
 * system operator to request a svcdump.
 */
public class SvcdumpCommandHandler implements CommandHandlerExtension {

    /**
     * A human readable name for this handler.
     */
    final static String NAME = "SVCDUMP Command Handler";

    /**
     * string used when SDUMPX returned a non zero.
     */
    public final String SDUMPX_ERROR = "SDUMPX returned return code ";

    /**
     * string used when some other error occurred when attempting to take a sdump.
     */
    public final String INTERNAL_ERROR = "Internal error occurred. Error code is ";

    /** Supported commands */
    private static final String COMMAND_NAME = "SVCDUMP";
    private static final String COMMAND_HELP = "SVCDUMP,HELP";

    /**
     * Full help text. Displays when command with help parameter is called and when
     * command with invalid parameter is called (Example: "SVCDUMP,badParam")
     */
    protected static final List<String> HELP_TEXT_LONG = new ArrayList<String>();
    static {
        HELP_TEXT_LONG.add("Usage:");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add(" MODIFY <jobname.>identifier,svcdump");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add("Description:");
        HELP_TEXT_LONG.add(" This Modify command is used to request an SVC");
        HELP_TEXT_LONG.add(" from a specific Liberty server.");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add(" To start an SVC dump, the server must be authorized");
        HELP_TEXT_LONG.add(" with read access to the BBG.AUTHMOD.BBGZSAFM.ZOSDUMP");
        HELP_TEXT_LONG.add(" resource profile in the SERVER class.");
    }

    /**
     * Short help text. Displays when command is not recognized and all command
     * handler help text is WTO.
     */
    protected static final List<String> HELP_TEXT_SHORT = new ArrayList<String>();
    static {
        HELP_TEXT_SHORT.add("To request an SVC dump from a Liberty server:");
        HELP_TEXT_SHORT.add("  MODIFY <jobname.>identifier,svcdump");
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
        nativeMethodManager.registerNatives(SvcdumpCommandHandler.class);
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
        results.setResponsesContainMSGIDs(false);
        results.setProperty(ModifyResults.HELP_RESPONSE_KEY, false);

        if (command != null && command.toUpperCase().startsWith(COMMAND_NAME)) {
            // If command is correct, handle the SVC dump
            if (command.equalsIgnoreCase(COMMAND_NAME)) {
                int dumpReturnCode = ntv_takeSvcDump("MODIFY SERVER,SVCDUMP COMMAND");
                if (dumpReturnCode != 0) {
                    if (dumpReturnCode > 0) {
                        responses.add(SDUMPX_ERROR + String.valueOf(dumpReturnCode));
                    } else {
                        responses.add(INTERNAL_ERROR + String.valueOf(dumpReturnCode));
                    }
                    results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
                } else {
                    results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
                }
            } else {
                // Starts with COMMAND_NAME but incorrect, print long help text to console
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
     * Call to native code to request a svcdump.
     *
     */
    protected native int ntv_takeSvcDump(String id);

}
