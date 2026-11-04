/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.request.interrupt.command;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.zos.command.processing.extension.CommandHandlerExtension;
import com.ibm.wsspi.zos.command.processing.CommandHandler;
import com.ibm.wsspi.zos.command.processing.CommandResponses;
import com.ibm.wsspi.zos.command.processing.ModifyResults;

/**
 * An implementation of an MVS console command handler that allows a
 * system operator to display status of the registered interrupt objects.
 */
@Component(name = "com.ibm.ws.zos.request.interrupt.command.RequestInterruptCommandHandler", service = { CommandHandler.class }, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "modify.filter.regex=((?i)(display,interrupts).*)",
                                                                                                                                                                                                                  "display.command.help=TRUE",
                                                                                                                                                                                                                  "service.vendor=IBM" })
public class RequestInterruptCommandHandler implements CommandHandlerExtension {
    /**
     * trace variable
     */
    //private static final TraceComponent tc = Tr.register(RequestInterruptCommandHandler.class);

    /**
     * A human readable name for this handler.
     */
    final static String NAME = "Request Interrupt Command Handler";

    /**
     * Full help text. Displays when command with help parameter is called and when
     * command with invalid parameter is called (Example: "display,interrupts,badtext")
     */
    protected static final List<String> HELP_TEXT_LONG = new ArrayList<String>();
    static {
        HELP_TEXT_LONG.add("Usage:");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add(" MODIFY <jobname.>identifier,display,interrupts,<param>");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add("Description:");
        HELP_TEXT_LONG.add(" This Modify command is used to display summary or");
        HELP_TEXT_LONG.add(" detailed information about all interrupted requests");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add("Valid Parameters are:");
        HELP_TEXT_LONG.add(" details: to display detailed information about");
        HELP_TEXT_LONG.add("   interrupted requests.");
        HELP_TEXT_LONG.add(" age=<value> : to display summary information about");
        HELP_TEXT_LONG.add("   requests that are older than the value specified");
        HELP_TEXT_LONG.add(" age=<value>,details : to display detailed information");
        HELP_TEXT_LONG.add("   on requests that are older than the value specified");
        HELP_TEXT_LONG.add(" timedout: to display summary information about requests");
        HELP_TEXT_LONG.add("   that have timed out.");
        HELP_TEXT_LONG.add(" timedout,details: to display detailed information about");
        HELP_TEXT_LONG.add("   requests that have timed out.");
        HELP_TEXT_LONG.add(" request=<value> : to display detailed information about");
        HELP_TEXT_LONG.add("   the specified request.");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add(" This command requires the requestTiming feature to be");
        HELP_TEXT_LONG.add(" configured.");
    }

    /**
     * Short help text. Displays when command is not recognized and all command
     * handler help text is WTO.
     */
    protected static final List<String> HELP_TEXT_SHORT = new ArrayList<String>();
    static {
        HELP_TEXT_SHORT.add("To display information on interrupted requests");
        HELP_TEXT_SHORT.add("  MODIFY <jobname.>identifier,display,interrupts,<param>");
    }

    private static final String COMMAND_HELP = "display,interrupts,help";

    private CommandResponses commandResponses = null;

    /**
     * DS method to activate this component.
     */
    @Activate
    protected void activate() {
    }

    /**
     * DS method to deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
    }

    /**
     * Set the CommandResponses reference.
     *
     * @param commandResponses the CommandResponses to set
     *
     *                             Note: We want to this CommandHandler to be activated even if the CommandResponses is not
     *                             around to perform all the functions.
     */

    @Reference(service = CommandResponses.class, target = "(type=requestInterrupt)", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    protected void setCommandResponses(CommandResponses commandResponses) {
        this.commandResponses = commandResponses;
    }

    protected void unsetCommandResponses(CommandResponses commandResponses) {
        this.commandResponses = null;
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
        results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);

        if (command.equalsIgnoreCase(COMMAND_HELP)) {
            addHelpTextToResponse(responses);
            results.setProperty(ModifyResults.HELP_RESPONSE_KEY, true);
            results.setResponses(responses);
            results.setResponsesContainMSGIDs(false);
            return;
        }

        if (command.toLowerCase().startsWith("display,interrupts")) {
            if (commandResponses != null) {
                int rc = commandResponses.getCommandResponses(command, responses);
                if (rc != 0) {
                    results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
                    results.setProperty(ModifyResults.HELP_RESPONSE_KEY, true);
                    responses.add("Could not parse command: \"" + command + "\"");
                    addHelpTextToResponse(responses);
                }
            } else {
                responses.add("requestTiming-1.0 feature is not installed ");
            }
            results.setResponses(responses);
        } else {
            results.setCompletionStatus(ModifyResults.UNKNOWN_COMMAND);
        }

        results.setResponsesContainMSGIDs(false);
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
