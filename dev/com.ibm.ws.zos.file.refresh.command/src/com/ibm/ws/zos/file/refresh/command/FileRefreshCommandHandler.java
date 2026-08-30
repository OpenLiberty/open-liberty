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
package com.ibm.ws.zos.file.refresh.command;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.kernel.filemonitor.FileNotificationMBean;
import com.ibm.wsspi.zos.command.processing.CommandHandler;
import com.ibm.wsspi.zos.command.processing.CommandResponses;
import com.ibm.wsspi.zos.command.processing.ModifyResults;

/**
 * An implementation of an MVS console command handler that allows a
 * an update to either configuration or application. The help text is formatted with
 * the usage and description blocks. For ex:
 * __________________________________________________________________
 * CWWKB0012I:Usage:
 * CWWKB0012I:
 * CWWKB0012I: MODIFY <jobname>.identifier,refresh,<param>
 * CWWKB0012I:
 * CWWKB0012I:Description:
 * CWWKB0012I: This modify command process dynamic changes
 * CWWKB0012I: to the server configuration, dynamic updates
 * CWWKB0012I: to applications and security keystore updates
 * CWWKB0012I: when the updateTrigger attribute under the
 * CWWKB0012I: config, applicationMonitor and keystore elements
 * CWWKB0012I: are set to mbean respectively.
 * CWWKB0012I:
 * CWWKB0012I: Parameters:
 * CWWKB0012I: config: Processes configuration changes.
 * CWWKB0012I: apps: Processes application updates.
 * CWWKB0012I: keystore: Processes security updates.
 * CWWKB0012I:
 * CWWKB0005I: COMMAND RESPONSES COMPLETED SUCCESSFULLY OR UNSUCCESSFULLY FROM File Refresh Command Handler.
 * CWWKB0002I: MODIFY COMMAND REFRESH,<param> COMPLETED SUCCESSFULLY OR UNSUCCESSFULLY.
 * __________________________________________________________________
 * 
 * To provide consistency, make sure the appropriate message ID is added to
 * each message printed on the console from the handleModify method.
 * (See #handleModify for messaged ID descriptions)
 * (See #addHelpTextToResponse for implementation)
 */

@Component(service = { CommandHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "modify.filter.regex=((?i)(refresh).*)",
                        "service.vendor=IBM" })
public class FileRefreshCommandHandler implements CommandHandler {

    /**
     * A human readable name for this handler.
     */
    protected static final String NAME = "File Refresh Command Handler";
    /**
     * Help text.
     */
    protected static final List<String> HELP_TEXT_LONG = new ArrayList<String>();
    static {
        HELP_TEXT_LONG.add("Usage:");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add(" MODIFY <jobname>.identifier,refresh,<param>");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add("Description:");
        HELP_TEXT_LONG.add(" This modify command process dynamic changes");
        HELP_TEXT_LONG.add(" to the server configuration or dynamic updates");
        HELP_TEXT_LONG.add(" to applications and security keystore updates ");
        HELP_TEXT_LONG.add(" when the updateTrigger attribute under the");
        HELP_TEXT_LONG.add(" config, applicationMonitor and keystore elements");
        HELP_TEXT_LONG.add(" are set to mbean respectively.");
        HELP_TEXT_LONG.add(" ");
        HELP_TEXT_LONG.add(" Parameters:");
        HELP_TEXT_LONG.add(" config: Processes configuration changes.");
        HELP_TEXT_LONG.add(" apps: Processes application updates.");
        HELP_TEXT_LONG.add(" keystore: Processes security updates.");
        HELP_TEXT_LONG.add(" ");
    }

    protected static final List<String> HELP_TEXT_SHORT = new ArrayList<String>();
    static {
        HELP_TEXT_SHORT.add("To process changes to a server config, application or keystores");
        HELP_TEXT_SHORT.add("  MODIFY <jobname>.identifier,refresh,<param>");
        HELP_TEXT_SHORT.add("  <param> can be 'config', 'apps' or 'keystore'");
    }
    
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
    protected void deactivate() {}

    /**
     * Mbean Server instance to invoke the service
     */
    private static final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

    /** OSGi object variable for handling command response */
    private CommandResponses commandResponses;

    /**
     * Set the CommandResponses reference.
     *
     * @param commandResponses the CommandResponses to set
     *
     *            Note: We want to this CommandHandler to be activated even if the CommandResponses is not
     *            around to perform all the functions.
     */
    @Reference(service = CommandResponses.class, target = "(type=fileRefresh)", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL,
               policyOption = ReferencePolicyOption.GREEDY)
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
    public void handleModify(String commandparams, ModifyResults results) {
        String[] modify_param = commandparams.split(",");
        List<String> response = new ArrayList<String>();
        results.setProperty(ModifyResults.HELP_RESPONSE_KEY, false);

        try {
            FileNotificationMBean fn = JMX.newMBeanProxy(mbs, new ObjectName("WebSphere:service=com.ibm.ws.kernel.filemonitor.FileNotificationMBean"), FileNotificationMBean.class);
            // Check for invalid param and set status to error
            if (modify_param.length <= 1 || modify_param[1] == null || modify_param.length > 3) {
                response.add("An invalid number of parameters were found.\n");
                addHelpTextToResponse(response, results);
            } else {
                if (modify_param[1].equalsIgnoreCase("config")) {
                    fn.processConfigurationChanges();
                    results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
                    results.setResponsesContainMSGIDs(false);
                }
                else if (modify_param[1].equalsIgnoreCase("apps")) {
                    fn.processApplicationChanges();
                    results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
                    results.setResponsesContainMSGIDs(false);
                } 
                else if (modify_param[1].equalsIgnoreCase("keystore")) {
                    checkKeystoreRefresh(commandparams, response, results);
                }
                else if (modify_param[1].equalsIgnoreCase("help")) {
                    addHelpTextToResponse(response, results);
                    results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
                }
                else {
                    response.add("An invalid parameter of " + modify_param[1] + " was found.\n");
                    addHelpTextToResponse(response, results);
                }
            }
        } catch (Exception e) {
            // Relying on the FFDC to debug the exception
            response.add("Unable to process display command. See server logs for details.");
            results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
        }
        results.setResponses(response);
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
    public List<String> getHelp() {
        return HELP_TEXT_SHORT;
    }

    /**
     * This method prints out the help text with the message id below:
     * CWWKB0012I : Help information from the indicated command handler.
     * 
     * @param response
     * @param results
     */
    public void addHelpTextToResponse(List<String> response, ModifyResults results) {
        for (String textLine : HELP_TEXT_LONG) {
            response.add(textLine);
        }
        
        results.setProperty(ModifyResults.HELP_RESPONSE_KEY, true);
        results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
    }

    /**
     * This method calls the commandResponse to refresh the keystores. This checks if the
     * commandResponse is available and invokes the getCommandResponses method. The
     * getCommandResponses method will return non-zero value to indicate error
     * 
     * @param commandparams
     * @param response
     * @param results
     */
    public void checkKeystoreRefresh(String commandparams, List<String> response, ModifyResults results) {
        // if null, osgi object for CommandResponses not activated
        if (commandResponses == null) {
            response.add("Unable to refresh keystores. Validate that the");
            response.add("ssl-1.0 feature is configured.");
            results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
        } else if (commandResponses.getCommandResponses(commandparams, response) != 0) {
            results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND); // If RC non-zero, set error flag
            addHelpTextToResponse(response, results);
        } else {
            results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
            results.setResponsesContainMSGIDs(false);
        }
    }

}
