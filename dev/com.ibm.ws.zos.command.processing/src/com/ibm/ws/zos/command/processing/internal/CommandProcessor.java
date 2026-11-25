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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.zos.command.processing.CommandHandler;
import com.ibm.wsspi.zos.command.processing.ModifyResults;

/**
 * z/OS Command Processing
 *
 * Provide services to receive and process z/OS console commands
 * directed at this Server.
 */
public class CommandProcessor {
    private static final TraceComponent tc = Tr.register(CommandProcessor.class);

    protected final static String WS_COMMAND_HANDLER_REFERENCE_NAME = "wsCommandHandler";

    private String FAILED_TO_TRANSLATE_TO_EBCDIC = null;

    private CommandListenerThread thread = null;

    protected LibertyProcess cmdLine = null;

    protected NativeMethodManager nativeMethodManager = null;

    /**
     * The reference to the WSLocationAdmin service.
     */
    private WsLocationAdmin locationAdmin = null;

    /**
     * Declarative Service component context.
     */
    private ComponentContext componentContext;

    /**
     * The <code>BundleContext</code> of our host bundle.
     */
    private BundleContext bundleContext;

    /**
     * Map of ServiceReference to holders. This map is used to locate wrappers
     * when <code>CommandHandler</code>s are bound and unbound.
     */
    final Map<ServiceReference<CommandHandler>, CommandHandlerHolder> serviceReferenceMap = new HashMap<ServiceReference<CommandHandler>, CommandHandlerHolder>();

    /*
     * Native struct CommandInfoArea constants
     *
     * struct CommandInfoArea {
     * int cia_commandType
     * int cia_errorCode
     * unsigned char cia_commandCART[8]
     * int cia_commandRestOfCommandLength
     * char cia_commandRestOfCommand[CRA_MAX_COMMAND_PARMS]
     * }
     */
    public final static int CIA_COMMANDTYPE_STOP = 0x00000001;
    public final static int CIA_COMMANDTYPE_MODIFY = 0x00000002;
    public final static int CIA_COMMANDTYPE_UNKNOWN = 0x00000003;
    public final static int CIA_COMMANDTYPE_ENDING = 0x00000004;
    public final static int CIA_COMMANDTYPE_ERROR = 0x00000005;

    /**
     * DS method to activate this component.
     */
    protected void activate(ComponentContext componentContext) {

        ResourceBundle resBundle = TraceNLS.getBaseResourceBundle(this.getClass(), tc.getResourceBundleName());

        this.FAILED_TO_TRANSLATE_TO_EBCDIC = TraceNLS.getFormattedMessage(this.getClass(),
                                                                          resBundle,
                                                                          tc.getResourceBundleName(),
                                                                          "MESSAGE_TRANSLATE_ERROR",
                                                                          (Object[]) null,
                                                                          "CWWKB0008E: CWWKB0009E: A COMMAND RESPONSE COULD NOT BE TRANSLATED TO NATIVE ENCODING",
                                                                          Locale.ENGLISH,
                                                                          false); // No, don't be quiet about it.

        this.componentContext = componentContext;

        // Hold a reference to the bundle context
        this.bundleContext = componentContext.getBundleContext();

        // Attempt to load native code via the method manager.
        nativeMethodManager.registerNatives(CommandProcessor.class);

        final CommandProcessor temp = this;
        thread = AccessController.doPrivileged(new PrivilegedAction<CommandListenerThread>() {
            @Override
            public CommandListenerThread run() {
                CommandListenerThread thread = new CommandListenerThread(temp);
                return thread;
            }
        });

        thread.setName("zOS Command Listener Thread");
        thread.start();
        /*
         * ExecutorService executor = Executors.newSingleThreadExecutor();
         * executor.execute(new Runnable() {
         * public void run() {}
         * });
         */
    }

    /**
     * DS method to deactivate this component.
     *
     */
    void deactivate(ComponentContext componentContext) {
        thread.end();
        this.bundleContext = null;
        this.componentContext = null;
    }

    protected void setKernelCommandLine(LibertyProcess cmdLine) {
        this.cmdLine = cmdLine;
    }

    protected void unsetKernelCommandLine(LibertyProcess cmdLine) {
        if (this.cmdLine == cmdLine) {
            this.cmdLine = null;
        }
    }

    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
    }

    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        if (this.nativeMethodManager == nativeMethodManager) {
            this.nativeMethodManager = null;
        }
    }

    protected synchronized void setWsCommandHandler(ServiceReference<CommandHandler> handlerReference) {
        CommandHandlerHolder holder = new CommandHandlerHolder(this, handlerReference);
        serviceReferenceMap.put(handlerReference, holder);
    }

    protected synchronized void unsetWsCommandHandler(ServiceReference<CommandHandler> handlerReference) {
        serviceReferenceMap.remove(handlerReference);
    }

    /**
     * Sets the WsLocationAdmin reference.
     */
    protected void setLocationAdmin(WsLocationAdmin locationAdmin) {
        this.locationAdmin = locationAdmin;
    }

    /**
     * Clears the WsLocationAdmin reference.
     */
    protected void unsetLocationAdmin(WsLocationAdmin locationAdmin) {
        this.locationAdmin = null;
    }

    /**
     * Get the <code>BundleContext</code> for the host bundle.
     *
     * @return the <code>BundleContext</code>
     */
    BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * Get the <code>ComponentContext</code> for this component.
     *
     * @return the <code>ComponentContext</code>
     */
    ComponentContext getComponentContext() {
        return componentContext;
    }

    /**
     * Get the name of the WebSphere event handler references called out in
     * the service declaration. This is required to resolve injected <code>ServiceReference</code> objects.
     *
     * @return the eventHandler reference name
     */
    protected String getWsHandlerReferenceName() {
        return WS_COMMAND_HANDLER_REFERENCE_NAME;
    }

    protected void processCommand(byte[] inCmd) {
        int commandType = 0;
        ConsoleCommand consoleCmd;

        try {
            consoleCmd = new ConsoleCommand(inCmd);
        } catch (UnsupportedEncodingException uee) {
            Tr.error(tc, "COMMAND_TRANSLATE_ERROR");
            return;
        }

        commandType = consoleCmd.getCommandType();
        switch (commandType) {
            case CIA_COMMANDTYPE_STOP:
                // Stop processing
                Tr.audit(tc, "STOP_COMMAND_RECEIVED", locationAdmin.getServerName());

                // Make sure our CommandLine reference isn't "unset" while we're processing
                // a command by saving a temporary reference across the shutdown
                if (cmdLine != null) {
                    // delete the PID_FILE
                    String pidenv = System.getenv("X_PID_FILE");
                    if (pidenv != null) {
                        File pidfile = new File(pidenv);
                        if (fileExists(pidfile)) {
                            if (!fileDelete(pidfile)) {
                                Tr.error(tc, "FILE_DELETE_UNSUCCESSFUL", pidfile.getAbsolutePath());
                            }
                        }
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "X_PID_FILE is NULL");
                    }
                    cmdLine.shutdown();
                }

                break;
            case CIA_COMMANDTYPE_MODIFY:

                // Give the modify command to the CommandHandlers
                deliverCommandToHandlers(consoleCmd);

                break;
            case CIA_COMMANDTYPE_UNKNOWN:
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "CommandProcessor::processCommand, UNKNOWN case path,  commandType=" + commandType);

                break;
            case CIA_COMMANDTYPE_ENDING:
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "CommandProcessor::processCommand, ENDING case path,  commandType=" + commandType);

                break;
            case CIA_COMMANDTYPE_ERROR:
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "CommandProcessor::processCommand, ERROR case path,  commandType=" + commandType);

                Tr.error(tc, "NATIVE_ERROR_COMMAND_RECEIVED", consoleCmd.getErrorCode());
                break;
            default:
                // TODO: How do I tell OSGI to shut me down?
                // throw ?
                Tr.error(tc, "NATIVE_ERROR_COMMAND_RECEIVED", consoleCmd.getErrorCode());

                thread.end(); // For now, disable the listening loop
                // Perhaps something like the following:
                //cc.disableComponent("com.ibm.wsspi.zos.command.processing");
        }
    }

    /*
     * Method to deliver a native command to a CommandHandler
     *
     * @param consoleCommand Modify command to process
     *
     * @return 0 indicates successfully handled command, non-zero otherwise.
     */
    protected synchronized int deliverCommandToHandlers(ConsoleCommand consoleCommand) {

        int rc = ModifyResults.UNKNOWN_COMMAND;
        ModifyResultsImpl commandResults = null;

        String currentHandlerName = "UNKNOWN";
        String completionStatus;

        try {
            for (CommandHandlerHolder commandHandlerHolder : serviceReferenceMap.values()) {
                commandResults = (ModifyResultsImpl) commandHandlerHolder.deliverCommand(consoleCommand.getCommandString());
                currentHandlerName = commandHandlerHolder.getCommandHandlerName();

                if (commandResults != null) {
                    int handlerRC = commandResults.getCompletionStatus();

                    List<String> responseMsgs = commandResults.getResponses();
                    if ((responseMsgs != null) && !responseMsgs.isEmpty()) {
                        // Issue command responses
                        Iterator<String> it = responseMsgs.iterator();
                        while (it.hasNext()) {
                            String currentMsg = it.next();
                            // Issue WTO for each, prepend a MSGId if needed
                            // Get property for help text response, check if its null in case it was not set
                            Object helpResponseProperty = commandResults.getProperty(ModifyResults.HELP_RESPONSE_KEY);
                            // Prepend help text MSGid
                            if ((helpResponseProperty != null) && ((Boolean) helpResponseProperty == true)) {
                                buildAndIssueCommandResponse(consoleCommand,
                                                             "MODIFY_COMMAND_HANDLER_HELP_TXT",
                                                             new Object[] { currentMsg },
                                                             "CWWKB0008E: " + currentMsg + ".");
                            } else if (!commandResults.responsesContainMSGIDs()) {
                                buildAndIssueCommandResponse(consoleCommand,
                                                             "MODIFY_COMMAND_HANDLER_MSG",
                                                             new Object[] { currentMsg },
                                                             "CWWKB0008E: " + currentMsg);
                            } else {
                                issueCommandResponse(consoleCommand, currentMsg);
                            }
                        }
                    }

                    if (handlerRC == ModifyResults.PROCESSED_COMMAND) {
                        rc = handlerRC;
                        completionStatus = "SUCCESSFULLY";
                    } else {
                        completionStatus = "UNSUCCESSFULLY";
                        if (handlerRC == ModifyResults.ERROR_PROCESSING_COMMAND) {
                            rc = handlerRC;
                        }
                    }

                    if (handlerRC != ModifyResults.UNKNOWN_COMMAND) {
                        // Issue Handler done message
                        buildAndIssueCommandResponse(consoleCommand,
                                                     "MODIFY_COMMAND_HANDLER_DONE",
                                                     new Object[] { completionStatus, currentHandlerName },
                                                     "CWWKB0008E: CWWKB0005I: COMMAND RESPONSES COMPLETED " + completionStatus + " FROM " + currentHandlerName + ".");
                    }
                }
            }
        } catch (Throwable t) {
            rc = ModifyResults.ERROR_PROCESSING_COMMAND;
            buildAndIssueCommandResponse(consoleCommand,
                                         "MODIFY_HANDLER_ERROR",
                                         new Object[] { currentHandlerName },
                                         "CWWKB0008E: CWWKB0007E: ERROR RECEIVED FROM COMMAND HANDLER " + currentHandlerName + ".");
        }

        // Issue "help" if no handlers handled
        if (rc == ModifyResults.UNKNOWN_COMMAND) {
            if (consoleCommand.getCommandString().equalsIgnoreCase("HELP") == false) {
                buildAndIssueCommandResponse(consoleCommand,
                                             "MODIFY_COMMAND_UNKNOWN",
                                             new Object[] { consoleCommand.getCommandString() },
                                             "CWWKB0008E: CWWKB0006W: MODIFY COMMAND " + consoleCommand.getCommandString() + " WAS NOT KNOWN TO ANY ACTIVE COMMAND HANDLER.");
            }

            issueHelpFromRegisteredHandlers(consoleCommand);
        }

        // Issue Command complete message
        if (rc == ModifyResults.ERROR_PROCESSING_COMMAND) {
            completionStatus = "UNSUCCESSFULLY";
        } else if ((rc == ModifyResults.UNKNOWN_COMMAND) &&
                   (consoleCommand.getCommandString().equalsIgnoreCase("HELP") == false)) {
            completionStatus = "UNSUCCESSFULLY";
        } else {
            completionStatus = "SUCCESSFULLY";
        }

        buildAndIssueCommandResponse(consoleCommand,
                                     "MODIFY_COMMAND_COMPLETED",
                                     new Object[] { consoleCommand.getCommandString(), completionStatus },
                                     "CWWKB0008E: MODIFY COMMAND" + consoleCommand.getCommandString() + " COMPLETED " + completionStatus + ".");
        return rc;
    }

    /**
     * Issue "Help" information for each registered <code>CommandHandler</code>
     *
     * @param consoleCommand
     *            object representing the command
     */
    private void issueHelpFromRegisteredHandlers(ConsoleCommand consoleCommand) {
        try {
            for (CommandHandlerHolder commandHandlerHolder : serviceReferenceMap.values()) {

                if (commandHandlerHolder.displayHelp() == false)
                    continue;

                List<String> helpMsgs = commandHandlerHolder.getHelp();

                if ((helpMsgs != null) && !helpMsgs.isEmpty()) {
                    String handlerName = commandHandlerHolder.getCommandHandlerName();
                    // Issue help response header
                    buildAndIssueCommandResponse(consoleCommand,
                                                 "MODIFY_COMMAND_HANDLER_HELP_HDR",
                                                 new Object[] { handlerName },
                                                 "CWWKB0008E: CWWKB0011I: START OF HELP INFORMATION FROM COMMAND HANDLER " + handlerName + ".");

                    Iterator<String> it = helpMsgs.iterator();
                    while (it.hasNext()) {
                        String currentMsg = it.next();

                        buildAndIssueCommandResponse(consoleCommand,
                                                     "MODIFY_COMMAND_HANDLER_HELP_TXT",
                                                     new Object[] { currentMsg },
                                                     "CWWKB0008E: " + currentMsg);
                    }

                    // Issue help response header
                    buildAndIssueCommandResponse(consoleCommand,
                                                 "MODIFY_COMMAND_HANDLER_HELP_END",
                                                 new Object[] { handlerName },
                                                 "CWWKB0008E: CWWKB0013I END OF HELP INFORMATION FROM COMMAND HANDLER " + handlerName + ".");
                }
            }
        } catch (Throwable t) {
        }
    }

    /**
     * Build a command response message and drive it back to the command originator.
     *
     * @param consoleCommand
     * @param NLS_msgID
     * @param msgArgs
     * @param defaultMsg
     */
    protected void buildAndIssueCommandResponse(ConsoleCommand consoleCommand, String NLS_msgID, Object[] msgArgs, String defaultMsg) {

        ResourceBundle resBundle = TraceNLS.getBaseResourceBundle(this.getClass(), tc.getResourceBundleName());

        String msg = TraceNLS.getFormattedMessage(this.getClass(),
                                                  resBundle,
                                                  tc.getResourceBundleName(),
                                                  NLS_msgID,
                                                  msgArgs,
                                                  defaultMsg,
                                                  Locale.ENGLISH,
                                                  false); // No, don't be quiet about it.
        issueCommandResponse(consoleCommand, msg);
    }

    /**
     * Issue command response back to command originator
     *
     * @param consoleCommand
     *            <code>ConsoleCommand</code> containing response information
     * @param msg
     *            <code>String</code> message to issue as a command response
     */
    protected void issueCommandResponse(ConsoleCommand consoleCommand, String msg) {

        // Issue command responses
        long cart = consoleCommand.getCart();
        int consid = consoleCommand.getConsoleID();
        byte[] temp = null;

        // Note: Write a WTO message. If the message has embedded line-separators, a
        // multi-line WTO should be issued with the line-separators determining the
        // individual lines.
        //
        // If a single line is longer than the allowed maximum, it should be
        // formatted to fit on multiple lines.
        try {
            // Convert to EBCDIC for native.
            temp = msg.getBytes("Cp1047");

        } catch (UnsupportedEncodingException uee) {
            try {
                temp = FAILED_TO_TRANSLATE_TO_EBCDIC.getBytes("Cp1047");
            } catch (UnsupportedEncodingException uee2) {
                Tr.error(tc, "MESSAGE_TRANSLATE_ERROR");
            }
        }

        ntv_issueCommandResponse(temp, cart, consid);
    }

    /**
     * Inner class for spawning and managing a separate thread to
     * interact with the native code to receive operator commands.
     */
    static public class CommandListenerThread extends Thread {
        private final CommandProcessor cp;
        private boolean keepGoing = true;
        private long iezcom_area;

        CommandListenerThread(CommandProcessor cp) {
            this.cp = cp;
        }

        @Override
        public void run() {

            this.iezcom_area = cp.ntv_getIEZCOMReference();

            while (keepGoing && (this.iezcom_area != 0)) {
                try {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "CommandListenerThread, waiting for command");

                    byte[] nativeCommandInfoArea = cp.ntv_getCommand(iezcom_area);

                    if (nativeCommandInfoArea.length != 0) {
                        // Process new command
                        cp.processCommand(nativeCommandInfoArea);
                    } else {
                        keepGoing = false;
                    }
                } catch (Exception e) {
                }
            }

        }

        public void end() {
            // Need to wake this thread up from a Native wait for another Command.
            keepGoing = false;
            cp.ntv_stopListeningForCommands();
        }
    }

    /**
     * Test if the specified file exists using this class's security context.
     *
     * @param file the file to test
     *
     * @return true if the file exists
     */
    boolean fileExists(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.exists();
            }
        });
    }

    /**
     * Test if the specified file is deleted using this class's security context.
     *
     * @param file the file to test
     *
     * @return true if the file is deleted
     */
    boolean fileDelete(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.delete();
            }
        });
    }

//
// Native methods
//

    /**
     * Call to native code Console Area (IEZCOM).
     *
     */
    protected native long ntv_getIEZCOMReference();

    /**
     * Call to native code to wait for a Command.
     *
     */
    protected native byte[] ntv_getCommand(long iezcomm);

    /**
     * Call to native code to stop listening for operator commands
     *
     */
    protected native void ntv_stopListeningForCommands();

    /**
     * Issue WTO with CART and console Identifier
     */
    protected native int ntv_issueCommandResponse(byte[] response, long cart, int consid);
}
