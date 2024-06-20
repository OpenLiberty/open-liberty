/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.cache.clear.command;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.zos.command.processing.extension.CommandHandlerExtension;
import com.ibm.wsspi.zos.command.processing.CommandHandler;
import com.ibm.wsspi.zos.command.processing.CommandResponses;
import com.ibm.wsspi.zos.command.processing.ModifyResults;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Command Handler for clearing the Liberty Authentication Cache.
 * Use command 'f, bbgzsrv,cache,clear,auth'
 */
@Component(service = { CommandHandler.class }, 
		   configurationPolicy = ConfigurationPolicy.IGNORE, 
		   property = { "modify.filter.regex=((?i)(cache,clear).*)", "display.command.help=TRUE", "service.vendor=IBM" }) 
public class CacheClearCommandHandler implements CommandHandlerExtension {

	/** trace variable */
	private static final TraceComponent tc = Tr.register(CacheClearCommandHandler.class);

	/** A human readable name for this handler. */
	final static String NAME = "Cache Clear Command Handler";

	/** Command string and length of command string*/
	private static final String CACHE_CLEAR_COMMAND = "cache,clear,auth";
	private static final String CACHE_CLEAR_COMMAND_HELP = "cache,clear,help";

	/** OSGi object variable for handling command response */
	private CommandResponses commandResponses;

	/** Long Help text. */
	private final static List<String> HELP_TEXT_LONG = new ArrayList<String>();
	static {
		HELP_TEXT_LONG.add("Usage:");
		HELP_TEXT_LONG.add(" ");
		HELP_TEXT_LONG.add(" MODIFY <jobname.>identifier,cache,clear,auth");
		HELP_TEXT_LONG.add(" ");
		HELP_TEXT_LONG.add("Description:");
		HELP_TEXT_LONG.add(" This modify command clears the user authentication");
		HELP_TEXT_LONG.add(" cache for the Liberty server.");
		HELP_TEXT_LONG.add(" The security service must be active for this ");
		HELP_TEXT_LONG.add(" modify command to complete successfully.");

	}
	
	/** short help text */
	private final static List<String> HELP_TEXT_SHORT = new ArrayList<String>();
	static {
		HELP_TEXT_SHORT.add("To clear the authentication cache for a Liberty server");
		HELP_TEXT_SHORT.add("  MODIFY <jobname.>identifier,cache,clear,auth");
	}
	
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
	@Reference(service = CommandResponses.class, target="(type=cacheClear)", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
	protected void setCommandResponses(CommandResponses reference) {
		commandResponses = reference;
	}

	protected void unsetCommandResponses(CommandResponses reference) {
		commandResponses = null;
	}


	@Override
	public List<String> getHelp() {
		return HELP_TEXT_SHORT;
	}

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

			// Osgi sets commandResponses if security service is active
			// if commandResponses null, issue message that security service is not active
			if (commandResponses == null) {
				responses.add("Server security service is not active.");
				responses.add("Could not complete CACHE,CLEAR,AUTH command.");
				results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
				results.setResponses(responses);
				return;
			}
			
			// Validate command syntax
			if (!command.equalsIgnoreCase(CACHE_CLEAR_COMMAND)) {
				results.setProperty(ModifyResults.HELP_RESPONSE_KEY, true);
		        
				// Invalid syntax will cause error message and completion status
				// Help command will cause successful completion status
				if (!command.equalsIgnoreCase(CACHE_CLEAR_COMMAND_HELP)) {
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
			responses.add("Unable to process cache clear command due to an error");
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
