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
package com.ibm.ws.zos.display.work.command;

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

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.zos.command.processing.extension.CommandHandlerExtension;
import com.ibm.wsspi.zos.command.processing.CommandHandler;
import com.ibm.wsspi.zos.command.processing.CommandResponses;
import com.ibm.wsspi.zos.command.processing.ModifyResults;

/**
 * An implementation of a MVS console command handler that allows a system operator to display request work counts.
 * The requests types being processed are: servlet requests.
 * This command handler processed commands with the following syntax: DISPLAY,WORK
 */
@Component(name = "com.ibm.ws.zos.display.work.command.DisplayWorkCommandHandler", service = { CommandHandler.class }, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "modify.filter.regex=((?i)(display,work).*)",
		"display.command.help=TRUE",
		"service.vendor=IBM" })
public class DisplayWorkCommandHandler implements CommandHandlerExtension {

	/** trace variable */
	private static final TraceComponent tc = Tr.register(DisplayWorkCommandHandler.class);

	/** A human readable name for this handler. */
	final static String NAME = "Display Work Command Handler";

	/** Command string and length of command string*/
	private static final String DISPLAY_COMMAND = "display,work";
	private static final String DISPLAY_COMMAND_HELP = "display,work,help";

	/** Long Help text. */
	protected final static List<String> HELP_TEXT_LONG = new ArrayList<String>();
	static {
		HELP_TEXT_LONG.add("Usage:");
		HELP_TEXT_LONG.add(" ");
		HELP_TEXT_LONG.add(" MODIFY <jobname.>identifier,display,work");
		HELP_TEXT_LONG.add(" ");
		HELP_TEXT_LONG.add("Description:");
		HELP_TEXT_LONG.add(" This modify command displays request activity counts");
		HELP_TEXT_LONG.add(" for the Liberty server.");
		HELP_TEXT_LONG.add(" ");
		HELP_TEXT_LONG.add(" The following information is provided:");
		HELP_TEXT_LONG.add(" Total Requests: The total requests since the server ");
		HELP_TEXT_LONG.add(" started.");
		HELP_TEXT_LONG.add(" Total Active Requests: The total request currently in");
		HELP_TEXT_LONG.add(" flight.");
		HELP_TEXT_LONG.add(" Total Slow Requests: The total requests in flight that");
		HELP_TEXT_LONG.add(" are deemed slow.");
		HELP_TEXT_LONG.add(" Total Hung Requests: The total requests in flight that");
		HELP_TEXT_LONG.add(" are deemed hung.");
		HELP_TEXT_LONG.add(" ");
		HELP_TEXT_LONG.add(" The Command also provides a DELTA count that shows the");
		HELP_TEXT_LONG.add(" count difference between the last time the command is");
		HELP_TEXT_LONG.add(" executed and the current command execution.");
		HELP_TEXT_LONG.add(" ");
		HELP_TEXT_LONG.add(" This command requires the requestTiming feature to be");
		HELP_TEXT_LONG.add(" configured.");
	}
	
	protected final static List<String> HELP_TEXT_SHORT = new ArrayList<String>();
	static {
		HELP_TEXT_SHORT.add("To display request activity counts for a Liberty server");
		HELP_TEXT_SHORT.add("  MODIFY <jobname.>identifier,display,work");
	}

	/** OSGi object variable for handling command response */
	private CommandResponses commandResponses;

	/**
	 * DS method to activate this component.
	 */
	@Activate
	protected void activate() {}

	/**
	 * DS method to deactivate this component.
	 */
	@Deactivate
	protected void deactivate() {}

	/**
	 * Set the CommandResponses reference.
	 *
	 * @param commandResponses the CommandResponses to set
	 *
	 *            Note: We want to this CommandHandler to be activated even if the CommandResponses is not
	 *            around to perform all the functions.
	 */
	@Reference(service = CommandResponses.class, target="(type=displayWork)", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
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
		// create list that will hold command responses as Strings
		List<String> responses = new ArrayList<String>();

		try {
			results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
			results.setResponsesContainMSGIDs(false);
	        results.setProperty(ModifyResults.HELP_RESPONSE_KEY, false);

			// if null, osgi object for CommandResponses not activated
			if (commandResponses == null) {
				responses.add("Unable to display work counts. Validate that the");
				responses.add("requestTiming feature is configured.");
				results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
				results.setResponses(responses);
				return;
			}
			
			// Validate command syntax
			if (!command.toLowerCase().equals(DISPLAY_COMMAND)) {
		        results.setProperty(ModifyResults.HELP_RESPONSE_KEY, true);
		        
		        // Invalid syntax will cause error message and completion status
		        // Help command will cause successful completion status
		        if (!command.equalsIgnoreCase(DISPLAY_COMMAND_HELP)) {
					results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
                    responses.add("Could not parse command: '" + command + "'");
		        }
				addHelpTextToResponse(responses);
				results.setResponses(responses);
				return;
			}
			
			// Process command
			int rc = commandResponses.getCommandResponses(command, responses);
			
			// If RC non-zero, set error flag
			if (rc != 0) {
				results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
			}

		} catch (Exception e){
			// FFDC will do for debugging.
			responses.add("Unable to process display command due to an error");
			responses.add("Check logs for details");
	        results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
		}
		results.setResponses(responses);
	}
	
	/**
	 * Adds the help text to the response array
	 * @param response
	 */
	public void addHelpTextToResponse(List<String> response) {
		for (String textLine : HELP_TEXT_LONG) {
			response.add(textLine);
		}
	}
}
