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
package com.ibm.ws.zos.logging.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.zos.command.processing.extension.CommandHandlerExtension;
import com.ibm.wsspi.zos.command.processing.CommandHandler;
import com.ibm.wsspi.zos.command.processing.ModifyResults;

/**
 * An implementation of an MVS console command handler that allows a
 * system operator to change the trace and logging configuration.
 */
@Component(name = "com.ibm.ws.zos.command.processing.logging.LoggingCommandHandler",
           service = { CommandHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "modify.filter.regex=((?i)(logging).*)",
                        "display.command.help=TRUE",
                        "service.vendor=IBM" })
public class LoggingCommandHandler implements CommandHandlerExtension {

    /**
     * A human readable name for this handler.
     */
    final static String NAME = "Logging Command Handler";

    /**
     * Full help text. Displays when command with help parameter is called and when
     * command with invalid parameter is called (Example: "LOGGING,badtext")
     */
    protected static final List<String> HELP_TEXT_LONG = new ArrayList<String>();
    static {
        HELP_TEXT_LONG.add("Usage:");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add(" MODIFY <jobname.>identifier,logging=\'tracespec\'");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add("Description:");
        HELP_TEXT_LONG.add(" This Modify command is used to enable traces or");
        HELP_TEXT_LONG.add(" change the trace specification of a Liberty server");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add(" The tracespec value is a valid trace specification");
        HELP_TEXT_LONG.add(" or the value reset. The tracespec is case sensitive");
        HELP_TEXT_LONG.add(" and must be enclosed in single quotes. Specifying");
        HELP_TEXT_LONG.add(" the value 'reset' will return the server to the ");
        HELP_TEXT_LONG.add(" settings specified in the configuration");
    }

    /**
     * Short help text. Displays when command is not recognized and all command
     * handler help text is WTO.
     */
    protected static final List<String> HELP_TEXT_SHORT = new ArrayList<String>();
    static {
        HELP_TEXT_SHORT.add("To enable tracing for a specified Liberty server:");
        HELP_TEXT_SHORT.add("  MODIFY <jobname.>identifier,logging=\'tracespec\'");
    }

    /** Help command string */
    private static final String COMMAND_HELP = "logging,help";

    /**
     * The persistent identifier for the logging configuration object.
     */
    final static String LOGGING_PID = "com.ibm.ws.logging";

    /**
     * The configuration key for the trace specification.
     */
    final static String TRACE_SPEC_KEY = "traceSpecification";

    /**
     * The injected reference to the OSGi configuration admin service.
     */
    protected ConfigurationAdmin configAdmin;

    /**
     * The initial configured trace specification.
     */
    protected String configuredTraceSpec = null;

    /**
     * DS method to inject the ConfigurationAdmin and capture the initial trace specification
     */
    @Reference
    protected void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
        this.setConfiguredTraceSpec();
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
        List<String> responses = null;
        results.setProperty(ModifyResults.HELP_RESPONSE_KEY, false);
        String[] loggingCmd = command.split("=", 2);

        if (loggingCmd.length > 1 && loggingCmd[1] != null && !command.equalsIgnoreCase(COMMAND_HELP)) {
            try {
                //Assumption is the command is entered with mixed case enclosed in single quotes
                String traceSpec = loggingCmd[1];
                traceSpec = this.trimQuotes(traceSpec);

                if (traceSpec.isEmpty()) {
                    responses = new ArrayList<String>(HELP_TEXT_LONG);
                    responses.add(0, "Could not parse command: \"" + command + "\"");
                    results.setProperty(ModifyResults.HELP_RESPONSE_KEY, true);
                    results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);

                } else {
                    Configuration config = configAdmin.getConfiguration(LOGGING_PID, null);
                    Dictionary<String, Object> props = config.getProperties();
                    if (traceSpec.equalsIgnoreCase("reset")) {
                        if (configuredTraceSpec != null) {
                            props.put(TRACE_SPEC_KEY, configuredTraceSpec);
                            config.update(props);
                        } else {
                            //The configuredTraceSpec should never be null. If a traceSpecification
                            //was not explicitly set the runtime sets a default value of '*=info=enabled'.
                            //This is just to catch the case where something unexpected has occurred.
                            responses = new ArrayList<String>();
                            responses.add("Unexpected null found for configuredTraceSpec");
                            results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
                            results.setResponsesContainMSGIDs(false);
                            results.setResponses(responses);
                            return;
                        }
                    } else {
                        props.put(TRACE_SPEC_KEY, traceSpec);
                        config.update(props);
                    }
                    results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
                }
            } catch (IOException ioe) {
                results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
                ioe = null; // Avoid FindBugs DLS
            }
        } else {
            responses = new ArrayList<String>(HELP_TEXT_LONG);
            results.setProperty(ModifyResults.HELP_RESPONSE_KEY, true);
            if (command.equalsIgnoreCase(COMMAND_HELP)) {
                results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
            } else {
                responses.add(0, "Could not parse command: \"" + command + "\"");
                results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
            }
        }
        results.setResponsesContainMSGIDs(false);
        results.setResponses(responses);
    }

    /**
     * Strip off leading and trailing single quotes, if string is null or any quotes are missing then
     * return an empty string.
     *
     * @param aString
     * @return aString
     */
    String trimQuotes(String aString) {
        if (aString == null) {
            return "";
        }

        if (!aString.startsWith("\'") && !aString.endsWith("\'")) {
            return aString;
        }

        if (aString.startsWith("\'")) {
            aString = aString.substring(1, aString.length());
        } else {
            return "";
        }

        if (aString.endsWith("\'")) {
            aString = aString.substring(0, aString.length() - 1);
        } else {
            return "";
        }

        return aString;
    }

    /**
     * Capture the configured trace specification
     */
    protected void setConfiguredTraceSpec() {

        if (configuredTraceSpec != null)
            return;

        try {
            Configuration config = configAdmin.getConfiguration(LOGGING_PID, null);
            Dictionary<String, Object> props = config.getProperties();
            configuredTraceSpec = (String) props.get(TRACE_SPEC_KEY);
        } catch (IOException e) {
            configuredTraceSpec = null;
            e = null; //satisfy findbugs
        }
    }

}
