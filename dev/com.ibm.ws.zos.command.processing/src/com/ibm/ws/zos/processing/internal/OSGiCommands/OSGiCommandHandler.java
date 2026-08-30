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
package com.ibm.ws.zos.processing.internal.OSGiCommands;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.osgi.framework.console.ConsoleSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.zos.command.processing.CommandHandler;
import com.ibm.wsspi.zos.command.processing.ModifyResults;

/**
 *
 */
public class OSGiCommandHandler implements CommandHandler {

    final static String OSGICOMMANDHANDLERNAME = "OSGiCommandHandler";

    protected BundleContext bundleContext = null;

    /**
     * Constructor.
     */
    public OSGiCommandHandler() {

    }

    /**
     * DS method to activate this component.
     */
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * DS method to deactivate this component.
     *
     */
    void deactivate(BundleContext bundleContext) {
        this.bundleContext = null;
    }

    /*
     * @see
     * com.ibm.wsspi.zos.command.processing.CommandHandler#handleModify(
     * java.lang.String, com.ibm.wsspi.zos.command.processing.ModifyResults)
     */
    @Override
    public void handleModify(java.lang.String commandString, ModifyResults results) {
        List<String> responses = null;

        String[] osgiCmd = commandString.split("((?i)(osgi,))", 2);
        if (osgiCmd.length > 1 && osgiCmd[1] != null) {
            try {
                // System.out.println("osgiCmd[0]=" + osgiCmd[0] + ",osgiCmd[1]=" + osgiCmd[1]);
                OSGiConsole oConsole = this.getNewConsole(osgiCmd[1]);

                // Since the "disconnect..." above is causing OSGi to drive ConsoleSession.doClose() I
                // no longer need to drive the unregister(), and thus, dont need a reference to
                // drive in on.
                //ServiceRegistration<?> servReg =
                this.registerConsole(oConsole);

                responses = oConsole.getResults();

                // Strip out the "disconnect" response from the responses
                // If it contains "osgi> Disconnect from console?" then punt it
                Iterator<String> it = responses.iterator();
                List<String> messagesToRemove = new ArrayList<String>();
                while (it.hasNext()) {
                    String currentMsg = it.next();

                    // Issue WTO for each, prepend a MSGId if needed
                    if (currentMsg.contains("osgi> Disconnect from console?")) {
                        messagesToRemove.add(currentMsg);
                    }
                }

                responses.removeAll(messagesToRemove);

                // I'm getting doClose() driven with the addition of the last disconnect\ny\n above.
                // it caused the doClose() to get driven on the OSGiConsole.  With this, it must
                // have performed the unregister().  I was getting an IllegalState exception with it.
                //servReg.unregister();

                results.setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
            } catch (UnsupportedEncodingException e) {
                results.setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
            }
        } else {
            results.setCompletionStatus(ModifyResults.UNKNOWN_COMMAND);
        }

        results.setResponsesContainMSGIDs(false);
        results.setResponses(responses);
    }

    protected OSGiConsole getNewConsole(String osgiCmd) throws UnsupportedEncodingException {
        String osgiCmdModified = osgiCmd + "\ndisconnect\ny\n";
        return new OSGiConsole(osgiCmdModified);
    }

    protected ServiceRegistration<?> registerConsole(OSGiConsole oConsole) {
        return this.bundleContext.registerService(ConsoleSession.class.getName(), oConsole, null);
    }

    /*
     * @see
     * com.ibm.wsspi.zos.command.processing.CommandHandler#getName()
     */
    @Override
    public String getName() {
        return OSGICOMMANDHANDLERNAME;
    }

    /**
     * @see com.ibm.wsspi.zos.command.processing.CommandHandler#getHelp()
     */
    @Override
    public List<String> getHelp() {
        List<String> responses = new ArrayList<String>();

        responses.add("Issue \"MODIFY <jobname.>identifier,\'osgi,osgicmd\'\"");
        responses.add("  where osgicmd is a valid OSGI console command");

        return responses;
    }
}
