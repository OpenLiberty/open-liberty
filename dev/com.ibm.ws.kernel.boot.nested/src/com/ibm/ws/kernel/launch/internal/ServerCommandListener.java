/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.FileUtils;
import com.ibm.ws.kernel.boot.internal.ServerCommand;
import com.ibm.ws.kernel.boot.internal.commands.JavaDumpAction;

import io.openliberty.checkpoint.spi.CheckpointHook;

/**
 * A trivial server command listener: opens a socket listening for commands.
 */
public class ServerCommandListener extends ServerCommand implements CheckpointHook {
    private static final TraceComponent tc = Tr.register(ServerCommandListener.class);

    private final FrameworkManager frameworkManager;

    /**
     * The next authorization challenge ID.
     */
    private int nextCommandAuthID;

    /**
     * True if {@link #close} has been called.
     */
    private boolean closed;

    /**
     * The server socket channel used to listen for requests
     */
    private ServerSocketChannel serverSocketChannel;

    /**
     * The lock used to synchronize response writes.
     */
    private final Object responseLock = new Object() {
    };

    private final AtomicReference<Thread> responseThread = new AtomicReference<Thread>();

    // ServerCommandID contains info used in handshake between ServerCommandListener and a ServerCommandClient
    // it's persisted on file system in the .sCommand file
    private final ServerCommandID sci;
    private final File serverWorkArea;
    private volatile boolean listenForCommands = false;
    private final Thread listeningThread;

    /**
     * The server name is read from BootstrapConfig and used when issuing messages.
     */
    private final String serverName;

    private static final char DELIM = '#';
    
    /**
     * Debug log file for detailed socket/IO tracing
     */
    private static PrintWriter debugLog = null;
    private static final Object debugLogLock = new Object();

    /**
     * Constructor for use by the server. It establishes a server socket listener and writes the
     * port and UUID value to the server command file.
     *
     * @param bootProps
     * @param uuid
     */
    public ServerCommandListener(BootstrapConfig bootProps, String uuid, FrameworkManager frameworkManager,
                                 Thread listeningThread, BundleContext bndCtxt) {

        super(bootProps);
        this.frameworkManager = frameworkManager;
        this.serverUUID = uuid;
        this.serverName = bootProps.getProcessName();
        this.listeningThread = listeningThread;

        Tr.info(tc, "Initializing ServerCommandListener for server: " + bootProps.getProcessName() + ", UUID: " + uuid);

        File serverDir = bootProps.getConfigFile(null);
        serverWorkArea = bootProps.getWorkareaFile(null);
        
        // Initialize debug log file in logs directory
        initDebugLog(bootProps);

        // set the default command port to:
        //   -- disabled (-1) if we are running as a z/OS started task
        //   -- ephemeral (0) otherwise
        int commandPort = Boolean.parseBoolean(bootProps.get(BootstrapConstants.DEFAULT_COMMAND_PORT_DISABLED_PROPERTY)) ? -1 : 0;

        String commandPortString = bootProps.get(BootstrapConstants.S_COMMAND_PORT_PROPERTY);
        if (commandPortString != null) {
            try {
                commandPort = Integer.parseInt(commandPortString);
            } catch (NumberFormatException nfe) {
            }
        }

        // If the server directory doesn't exist we have bigger problems
        if (!serverDir.exists()) {
            throw new LaunchException("Can not initialize server command listener - Invalid server directory", MessageFormat.format(BootstrapConstants.messages.getString("error.invalid.directory"),
                                                                                                                                    serverDir));
        }

        File commandFileTmp = createCommandFileTmp(serverWorkArea);

        try {
            sci = init(commandPort, commandFileTmp);

            createCommandFile(commandFileTmp);
            
            Tr.info(tc, "ServerCommandListener initialized successfully on port " + sci.getPort() + " for server: " + serverName);
        } catch (IOException ex) {
            Tr.error(tc, "Failed to initialize server command listener", ex);
            throw new LaunchException("Failed to initialize server command listener", MessageFormat.format(BootstrapConstants.messages.getString("error.serverCommand.init"), ex));
        }

        bndCtxt.registerService(CheckpointHook.class, this, null);
    }

    @Override
    public void restore() {
        //For checkpoint the the .sCommand file needs restored
        File commandFileTmp = createCommandFileTmp(serverWorkArea);
        createCommandFile(commandFileTmp);
    }

    /**
     *
     */
    private void createCommandFile(File sCommandTmp) {
        debugLog("createCommandFile() called with temp file: " + sCommandTmp.getAbsolutePath());
        debugLog("Target command file: " + commandFile.getAbsolutePath());
        
        FileOutputStream fos = null;
        try {
            String idString = sci.getIDString();
            debugLog("Writing command file content: " + idString);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Writing command file with content: " + idString);
            }
            fos = new FileOutputStream(sCommandTmp);
            fos.write(idString.getBytes());
            fos.close();
            debugLog("Command file written successfully to temp location");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Command file written to temp location: " + sCommandTmp.getAbsolutePath());
            }
        } catch (IOException ioex) {
            debugLog("Failed to write command file", ioex);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to write command file", ioex);
            }
            throw new LaunchException("Failed to initialize server command listener", MessageFormat.format(BootstrapConstants.messages.getString("error.serverCommand.init"),
                                                                                                           ioex));
        } finally {
            Utils.tryToClose(fos);
        }
        // Now that the command file is fully written
        debugLog("Attempting to rename " + sCommandTmp.getName() + " to " + commandFile.getName());
        if (!sCommandTmp.renameTo(commandFile)) {
            debugLog("Failed to rename command file!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to rename command file from " + sCommandTmp.getAbsolutePath() + " to " + commandFile.getAbsolutePath());
            }
            throw securityError(serverWorkArea, null);
        }
        debugLog("Command file renamed successfully - .sCommand file is now available for clients");
        debugLog("createCommandFile() completed successfully");
    }

    /**
     * setup workarea temp command file (.sCommand.tmp) and clear .sCommandAuth/. Make best effort to restrict .sCommand access to user only.
     *
     * @param serverWorkArea
     * @return File handle to newly created .sCommand.tmp
     * @throws LaunchException if we fail to create/remove or set appropriate access permissions to a file
     */
    private File createCommandFileTmp(File serverWorkArea) throws LaunchException {

        boolean writable = true;
        if (!serverWorkArea.exists())
            writable = serverWorkArea.mkdirs();

        if (!writable || !serverWorkArea.canWrite()) {
            throw securityError(serverWorkArea, null);
        }

        writable = true;
        if (!this.commandFile.delete() && this.commandFile.exists()) {
            throw securityError(serverWorkArea, null);
        }

        File commandFileTmp = new File(commandFile.getParentFile(), commandFile.getName() + ".tmp");
        if (!commandFileTmp.delete() && commandFileTmp.exists()) {
            throw securityError(serverWorkArea, null);
        }

        try {
            writable = commandFileTmp.createNewFile();
        } catch (IOException ex) {
            writable = false;
        }

        if (writable) {
            // Set the command file to writable for the owner only
            if (!commandFileTmp.setWritable(false))
                writable = false;
            if (!commandFileTmp.setWritable(true, true))
                writable = false;
        }

        if (!writable || !commandFileTmp.canWrite()) {
            throw securityError(serverWorkArea, null);
        }

        // Set the command file to readable for the owner only
        commandFileTmp.setReadable(false);
        commandFileTmp.setReadable(true, true);

        // Delete the contents of the directory.
        if (!FileUtils.recursiveClean(this.commandAuthDir) || !this.commandAuthDir.mkdir()) {
            throw securityError(serverWorkArea, null);
        }
        return commandFileTmp;
    }

    private LaunchException securityError(File file, Throwable cause) {

        return new LaunchException("Can not create or write to command directory, check server directory permissions", MessageFormat.format(BootstrapConstants.messages.getString("error.serverDirPermission"),
                                                                                                                                            file.getAbsolutePath()), cause);

    }

    /**
     * Initialize the server socket and write the UUID,port value to the server command file
     *
     * @throws IOException
     */
    private ServerCommandID init(int port, File commandFileTmp) throws IOException {
        debugLog("init() called with port: " + port + ", commandFileTmp: " + commandFileTmp.getAbsolutePath());
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Initializing server command listener with port: " + port);
        }
        
        if (port != -1) {
            debugLog("Opening ServerSocketChannel...");
            serverSocketChannel = SelectorProvider.provider().openServerSocketChannel();
            debugLog("ServerSocketChannel opened successfully");

            // Open the socket for loopback only..
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(null), port);
            debugLog("Binding to address: " + address + " (loopback, port=" + port + ")");

            IOException bindError = null;

            boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");

            try {
                //Attempt bind with reuseAddress set to false.
                debugLog("Setting SO_REUSEADDR=false");
                serverSocketChannel.socket().setReuseAddress(false);
                debugLog("Attempting bind to " + address);
                serverSocketChannel.socket().bind(address);
                debugLog("Bind successful on first attempt");

                //If we are not on Windows and the bind succeeded, we should set reuseAddr=true
                //for future binds.
                if (!isWindows) {
                    serverSocketChannel.socket().setReuseAddress(true);
                    debugLog("Set SO_REUSEADDR=true (non-Windows platform)");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "ServerSocket reuse set to true to allow for later override");
                    }
                }
            } catch (IOException ioe) {
                // see if we got the error because port is in waiting to be cleaned up.
                // If so, no one should be accepting connections on it, and open should fail.
                // If that's the case, we can set ReuseAddr to expedite the bind process.
                debugLog("Bind failed on first attempt", ioe);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ServerSocket bind failed on first attempt with IOException: " + ioe.getMessage());
                }
                bindError = ioe;
                try {
                    debugLog("Testing if port is actually in use by attempting connection...");
                    InetSocketAddress testAddr = new InetSocketAddress(InetAddress.getByName(null), port);
                    if (!testAddr.isUnresolved()) {
                        SocketChannel testChannel = SocketChannel.open(testAddr);
                        // if we get here, socket opened successfully, which means someone is really listening
                        // so close connection and don't bother trying to bind again
                        debugLog("Test connection succeeded - port is actually in use!");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "attempt to connect to command port to check listen status worked, someone else is using the port!");
                        }
                        testChannel.close();
                    } else {
                        debugLog("Test address is unresolvable: " + testAddr);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Test connection addr is unresolvable; " + testAddr);
                        }
                    }
                } catch (IOException testioe) {
                    debugLog("Test connection failed (port not in use) - will retry bind with SO_REUSEADDR=true", testioe);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "attempt to connect to command port to check listen status failed with IOException: " + testioe.getMessage());
                    }
                    try {
                        // open (or close) got IOException, retry with reuseAddress set to true
                        debugLog("Setting SO_REUSEADDR=true and retrying bind");
                        serverSocketChannel.socket().setReuseAddress(true);
                        serverSocketChannel.socket().bind(address);
                        debugLog("Bind successful on second attempt with SO_REUSEADDR=true");
                        bindError = null;

                    } catch (IOException newioe) {
                        debugLog("Bind failed on second attempt", newioe);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "ServerSocket bind failed on second attempt with IOException: " + newioe.getMessage());
                        }
                        bindError = newioe;
                    }
                }
            }

            if (bindError == null) {
                // if we requested an ephemeral port, find out what port we ended up with
                if (port == 0) {
                    port = serverSocketChannel.socket().getLocalPort();
                    debugLog("Ephemeral port assigned: " + port);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Ephemeral port assigned: " + port);
                    }
                }
                listenForCommands = true;
                debugLog("Server socket bound successfully on port: " + port);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Server socket bound successfully on port: " + port);
                }
            } else {
                debugLog("Failed to bind server socket - throwing exception", bindError);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to bind server socket", bindError);
                }
                throw bindError;
            }
        } else {
            debugLog("Command port disabled (port=-1)");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Command port disabled (port=-1)");
            }
        }

        ServerCommandID result = new ServerCommandID(port, serverUUID);
        debugLog("ServerCommandID created - UUID: " + serverUUID + ", Port: " + port);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ServerCommandID created - UUID: " + serverUUID + ", Port: " + port);
        }
        debugLog("init() completed successfully");
        return result;
    }

    /**
     * Finish any outstanding asynchronous responses, and close the server
     * socket to prevent new command requests.
     */
    public void close() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Closing server command listener");
        }
        Thread responseThread = null;
        synchronized (this) {
            if (!closed) {
                closed = true;
                notifyAll();

                if (listenForCommands) {
                    listenForCommands = false;
                    if (listeningThread != null) {
                        listeningThread.interrupt();
                    }
                }

                Utils.tryToClose(serverSocketChannel);
                commandFile.delete();

                responseThread = this.responseThread.getAndSet(null);
            }
        }

        if (responseThread != null) {
            try {
                responseThread.join();
            } catch (InterruptedException ex) {
            }
        }
    }
    
    /**
     * Initialize the debug log file in the server's logs directory
     */
    private static void initDebugLog(BootstrapConfig bootProps) {
        synchronized (debugLogLock) {
            if (debugLog == null) {
                try {
                    File logsDir = bootProps.getOutputFile("logs");
                    if (!logsDir.exists()) {
                        logsDir.mkdirs();
                    }
                    File debugFile = new File(logsDir, "servercommand.debug.log");
                    debugLog = new PrintWriter(new FileWriter(debugFile, true), true); // auto-flush
                    String separator = "================================================================================";
                    debugLog.println(separator);
                    debugLog.println("ServerCommandListener Debug Log - " + new Date());
                    debugLog.println("Server: " + bootProps.getProcessName());
                    debugLog.println(separator);
                } catch (Exception e) {
                    // If we can't create debug log, just continue without it
                    System.err.println("Failed to create servercommand.debug.log: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Write a debug message to the debug log file
     */
    private static void debugLog(String message) {
        synchronized (debugLogLock) {
            if (debugLog != null) {
                debugLog.println("[" + new Date() + "] " + message);
            }
        }
    }
    
    /**
     * Write a debug message with exception to the debug log file
     */
    private static void debugLog(String message, Throwable t) {
        synchronized (debugLogLock) {
            if (debugLog != null) {
                debugLog.println("[" + new Date() + "] " + message);
                if (t != null) {
                    t.printStackTrace(debugLog);
                }
            }
        }
    }

    /**
     * Start listening for incoming commands.
     * read() (accept()/read()) are blocking, not waiting, operations.
     * Locks are not suspended while waiting for input
     */
    public void startListening() {

        Tr.info(tc, "Server command listener started and accepting connections on port " + sci.getPort());
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Server command listener started on port " + sci.getPort());
        }
        while (listenForCommands && acceptAndExecuteCommand()) {
            //loop intentionally empty
        }
        
        Tr.info(tc, "Server command listener stopped");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Server command listener stopped");
        }
    }

    /**
     * Read and execute a command from the server socket
     *
     * @throws IOException
     */
    @FFDCIgnore({ IOException.class })
    private boolean acceptAndExecuteCommand() {
        boolean socketValid = false;
        try {
            //exceptions thrown by accept are assumed to mean the channel is no longer usable.
            SocketChannel sc = serverSocketChannel.accept();
            socketValid = true;
            if (sc != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "accepted socket", sc);
                }

                try {
                    String uuidAndCommand = read(sc);
                    ServerCommandID commandID = new ServerCommandID(uuidAndCommand);
                    String command = commandID.getOperation();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        // Don't trace uuidAndCommand to avoid putting it in logs
                        // (not as secure as the workarea).
                        Tr.debug(tc, "read UUID and command", command);
                    }

                    // As a first level of security, require that the user has read access to the .sCommand file.
                    if (!commandID.validate()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "invalid UUID", uuidAndCommand);
                        }
                    } else {
                        // As a second level of security, ensure that the client has write access to the workarea. We do
                        // this because by default, many umasks create world-readable files, which means everyone on a
                        // system can read the .sCommand file.
                        //
                        // Create a .sCommandAuth and request that the client delete the named file.
                        String authID;
                        File authFile;
                        do {
                            authID = Integer.toString(nextCommandAuthID++);
                            authFile = new File(commandAuthDir, authID);
                        } while (authFile.exists());

                        // The server is going to create the file (to ensure server ownership bits are preserved),
                        authFile.createNewFile();

                        // write the authId: the client will delete the file to prove write-access
                        write(sc, authID);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "wrote authorization challenge", authID, authFile);
                        }

                        // Read the response: the caller should have deleted the file.
                        String authIDResponse = read(sc);
                        if (!authIDResponse.equals(authID) || authFile.exists()) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "authorization failed");
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "authorization succeeded");
                            }

                            sc = executeCommand(sc, command);
                        }
                    }
                } finally {
                    // Close the socket: one command per connection.
                    Utils.tryToClose(sc);
                }
            }
        } catch (IOException ex) {
            // FFDCIgnore of IOExceptions: some expected due to async close
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "IOException while accepting/processing command (may be expected during shutdown)", ex);
            }
        } catch (Throwable t) {
            // Don't allow an exception from a single command to
            // break the entire command listener.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected exception while processing command", t);
            }
        }
        return socketValid;
    }

    private SocketChannel executeCommand(SocketChannel sc, String command) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Executing command: " + command);
        }
        
        if (STATUS_START_COMMAND.equals(command)) {
            asyncResponse(command, sc);
            sc = null;
        } else if (STOP_COMMAND.equals(command) || FORCE_STOP_COMMAND.equals(command)) {
            Tr.audit(tc, "info.stop.request.received", new Date(), serverName);
            asyncResponse(command, sc);
            sc = null;
            frameworkManager.shutdownCommand(FORCE_STOP_COMMAND.equals(command));
        } else if (command.startsWith(INTROSPECT_COMMAND)) {
            String arg = command.substring(command.indexOf('#') + 1);

            String timestamp;
            Set<JavaDumpAction> javaDumpActions;
            if (command.startsWith(INTROSPECT_JAVADUMP_COMMAND)) {
                String[] args = arg.split(",");
                timestamp = args[0];
                javaDumpActions = parseJavaDumpActions(args, 1);
            } else {
                timestamp = arg;
                javaDumpActions = null;
            }

            frameworkManager.introspectFramework(timestamp, javaDumpActions);
            writeResponse(sc);
        } else if (command.startsWith(JAVADUMP_COMMAND)) {
            int index = command.indexOf('#');
            Set<JavaDumpAction> javaDumpActions = index == -1 ? null : parseJavaDumpActions(command.substring(index + 1).split(","), 0);
            frameworkManager.dumpJava(javaDumpActions);
            writeResponse(sc);
        } else if (command.startsWith(PAUSE_COMMAND)) {
            String args = null;
            int index = command.indexOf("#");
            if (index > 0 && (command.length() > (index + 1))) {
                args = command.substring(index + 1);
            }

            ReturnCode rc = frameworkManager.pauseListeners(args);

            writeResponse(sc, rc.getValue());
        } else if (command.startsWith(RESUME_COMMAND)) {
            String args = null;
            int index = command.indexOf("#");
            if (index > 0 && (command.length() > (index + 1))) {
                args = command.substring(index + 1);
            }

            ReturnCode rc = frameworkManager.resumeListeners(args);

            writeResponse(sc, rc.getValue());
        } else {
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "warning.unrecognized.command", command);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Command executed: " + command);
        }
        return sc;
    }

    /**
     * Creates a single thread to wait for
     * the command to complete then respond.
     */
    private synchronized void asyncResponse(String command, SocketChannel sc) {
        if (closed) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Command listener closed, not creating async response for: " + command);
            }
            Utils.tryToClose(sc);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating async response thread for command: " + command);
            }
            Thread thread = new Thread(new ResponseThread(command, sc), "kernel-" + command + "-command-response");

            // We allow a maximum of one outstanding status start or stop command
            Thread oldThread = responseThread.getAndSet(thread);
            if (oldThread != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Interrupting previous response thread");
                }
                oldThread.interrupt();
            }

            thread.start();
        }
    }

    /**
     * Write a response on a socket channel.
     */
    private void writeResponse(SocketChannel sc) throws IOException {
        synchronized (responseLock) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Writing response (UUID only)");
            }
            write(sc, serverUUID);
        }
    }

    /**
     * Write a response on a socket channel with response code.
     */
    private void writeResponse(SocketChannel sc, int rc) throws IOException {
        synchronized (responseLock) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Writing response with return code: " + rc);
            }
            write(sc, serverUUID + DELIM + rc);
        }
    }

    private Set<JavaDumpAction> parseJavaDumpActions(String[] args, int index) {
        if (index == args.length) {
            return null;
        }

        Set<JavaDumpAction> javaDumpActions = new LinkedHashSet<JavaDumpAction>();
        for (; index < args.length; index++) {
            javaDumpActions.add(JavaDumpAction.valueOf(args[index]));
        }
        return javaDumpActions;
    }

    /**
     * This class is used to allow server start.status and stop
     * commands to be asynchronous.
     * This allows a dump to be taken even if the server is
     * still starting or stopping.
     */
    private class ResponseThread implements Runnable {
        private final SocketChannel sc;
        private final String command;

        ResponseThread(String command, SocketChannel sc) {
            this.command = command;
            this.sc = sc;
        }

        @Override
        public void run() {
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ResponseThread starting for command: " + command);
                }
                
                boolean success = true;
                if (STATUS_START_COMMAND.equals(command)) {   
                    Tr.info(tc, "Waiting for framework to be ready (status:start command received)...");
                    
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Waiting for framework to be ready...");
                    }
                    long startTime = System.currentTimeMillis();
                    success = frameworkManager.waitForReady();
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    
                    if (success) {
                        Tr.info(tc, "Framework is ready (waited " + elapsedTime + "ms)");
                    } else {
                        Tr.warning(tc, "Framework failed to become ready after " + elapsedTime + "ms");
                    }
                    
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Framework ready status: " + success + " (waited " + elapsedTime + "ms)");
                    }
                } else if (STOP_COMMAND.equals(command) || FORCE_STOP_COMMAND.equals(command)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Waiting for framework to stop...");
                    }
                    frameworkManager.waitForFrameworkStop();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Framework stopped");
                    }
                } else {
                    if (tc.isWarningEnabled()) {
                        Tr.warning(tc, "warning.unrecognized.command", command);
                    }
                }
                if (success) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Sending response for command: " + command);
                    }
                    writeResponse(sc);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Not sending response - success=false for command: " + command);
                    }
                }
            } catch (InterruptedException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ResponseThread interrupted for command: " + command);
                }
                // Close the socket without a status.
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "IOException in ResponseThread for command: " + command, e);
                }
            } finally {
                Utils.tryToClose(sc);
                responseThread.compareAndSet(Thread.currentThread(), null);
            }
        }
    }

}
