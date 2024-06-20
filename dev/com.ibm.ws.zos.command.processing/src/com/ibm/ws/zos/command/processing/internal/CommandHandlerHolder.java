/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.command.processing.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.zos.command.processing.CommandHandler;
import com.ibm.wsspi.zos.command.processing.ModifyResults;

/**
 * Encapsulation of data related to a registered <code>CommandHandlerHandler</code>.
 */
public class CommandHandlerHolder implements Comparable<CommandHandlerHolder> {

    /**
     * Reference to the owning <code>CommandProcessor</code> implementation.
     */
    final CommandProcessor commandProcessor;

    /**
     * Dynamic <code>ServiceReference</code> to the target handler. We hold this
     * reference to allow for late activation of the handler and for tracking
     * purposes.
     */
    final ServiceReference<CommandHandler> serviceReference;

    /**
     * Filter specification provided by the target handler.
     */
    final String filterSpec;

    /**
     * Resolved WebSphere handler instance.
     */
    CommandHandler target = null;

    /*
     * Resolved WebSphere handler instance name for display in responses.
     */
    String targetHandlerName = null;

    /**
     * Compiled <code>Pattern</code>. May be null.
     */
    Pattern filterPattern = null;

    /**
     * Id of the target handler (used to sort services w/ equivalent weight/rank
     */
    final long serviceId;

    /**
     * Id of the target handler (used to sort services w/ equivalent weight/rank
     */
    final int serviceRanking;

    /**
     * Indication if help information should be displayed when a "general" request for
     * help information is requested. This expresses a desire to "hide" this command from
     * general use.
     */
    final Boolean displayHelp;

    /**
     * Create a new <code>HandlerHolder</code> instance that wrappers the target <code>CommandHandler</code>.
     *
     * @param CommandProcessor
     *                             the owning implementation of <code>EventAdmin</code>
     * @param serviceReference
     *                             the unresolved reference to the target handler
     */
    CommandHandlerHolder(CommandProcessor commandProcessor, ServiceReference<CommandHandler> serviceReference) {
        this.commandProcessor = commandProcessor;
        this.serviceReference = serviceReference;

        // Hold on to the filter
        filterSpec = (String) serviceReference.getProperty(CommandHandler.MODIFY_FILTER);
        serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);

        // Service ranking is optional. If not specified, the default is 0
        Object tmp = serviceReference.getProperty(Constants.SERVICE_RANKING);
        serviceRanking = (tmp == null) ? 0 : (Integer) tmp;

        String displayHelpStr = (String) serviceReference.getProperty(CommandHandler.DISPLAY_HELP);
        displayHelp = Boolean.valueOf(((displayHelpStr != null) && displayHelpStr.compareToIgnoreCase("FALSE") == 0) ? false : true);
    }

    /**
     * Get the <code>ServiceReference</code> representing the target <code>CommandHandler</code> service.
     *
     * @return the <code>ServiceReference</code> of the target handler
     */
    ServiceReference<CommandHandler> getServiceReference() {
        return serviceReference;
    }

    /**
     * Get the resolved <code>CommandHandler</code> service instance.
     *
     * @return the resolved <code>CommandHandler</code> service instance
     */
    CommandHandler getService() {
        if (target != null) {
            return target;
        }

        // Look for the handler
        ComponentContext context = commandProcessor.getComponentContext();
        String referenceName = commandProcessor.getWsHandlerReferenceName();

        target = (CommandHandler) context.locateService(referenceName, serviceReference);
        targetHandlerName = target.getName();

        return target;
    }

    /**
     * Get the <code>Pattern</code> representing the target handler's filter
     * specification.
     *
     * @return the compiled <code>Pattern</code> instance or <code>null</code> if
     *         no filter specification was declared
     */
    Pattern getFilter() {
        if (filterPattern == null && filterSpec != null) {
            filterPattern = Pattern.compile(filterSpec);
        }

        return filterPattern;
    }

    /**
     * Compare two handlers: if there are somehow two handlerholders for the
     * same service (by instance of holder, or by service id), 0 will be
     * returned.
     *
     * Holders are otherwise sorted by service ranking (higher .. lower), and
     * then by service id (lower .. higher).
     *
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(CommandHandlerHolder holderToCompare) {
        if (this.equals(holderToCompare))
            return 0;

        // service ranking is higher to lower
        int compare = holderToCompare.serviceRanking - this.serviceRanking;
        if (compare == 0) {
            // service id is lower to higher
            return holderToCompare.serviceId > this.serviceId ? -1 : 1;
        }

        // service ranking is higher to lower
        // Can't get here with equal ranking, falls into compare block w/
        // non-equal service ids.
        return holderToCompare.serviceRanking > this.serviceRanking ? 1 : -1;
    }

    @Override
    public boolean equals(Object holderToCompare) {
        if (this == holderToCompare) {
            return true;
        }

        if (holderToCompare == null || !(holderToCompare instanceof CommandHandlerHolder)) {
            return false;
        }

        return this.serviceId == ((CommandHandlerHolder) holderToCompare).serviceId;
    }

    @Override
    public int hashCode() {
        return (int) this.serviceId;
    }

    public ModifyResults deliverCommand(java.lang.String modifyCommmand) {

        ModifyResultsImpl results = new ModifyResultsImpl();
        results.setCompletionStatus(ModifyResults.UNKNOWN_COMMAND);
        try {
            Pattern filter = getFilter();
            if ((filter == null) || filter.matcher(modifyCommmand).matches()) {
                CommandHandler handler = getService();

                if (handler != null) {
                    handler.handleModify(modifyCommmand, results);
                }
            }
        } catch (Throwable t) {
            // This needs work.
            results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
            List<String> response = new ArrayList<String>();
            response.add("CommandHandlerHolder:deliverCommand Caught \"" + t.getClass().getName() + "\", " + t.getMessage());
            results.setResponses(response);
            results.setResponsesContainMSGIDs(false);
        }

        return results;
    }

    /**
     * @return String the name of the CommandHandler
     */
    String getCommandHandlerName() {
        return targetHandlerName;
    }

    /**
     * Return indication if configuration desires to hide the help information.
     *
     * @return Boolean indicating to display help
     */
    boolean displayHelp() {
        return (displayHelp != null) ? displayHelp.booleanValue() : true;
    }

    /**
     * Retrieve help information from the <code>CommandHandler</code>.
     *
     * @return List<String>
     *         <code>List<String></code> containing help information.
     */
    List<String> getHelp() {
        List<String> helpMsgs = null;
        CommandHandler handler = getService();

        if (handler != null) {
            helpMsgs = handler.getHelp();
        }

        return helpMsgs;
    }

    /**
     * Simple diagnostic aid that presents a human readable representation of
     * the object instance.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";target=").append(target);
        sb.append(",serviceReference=").append(serviceReference);
        return sb.toString();
    }
}
