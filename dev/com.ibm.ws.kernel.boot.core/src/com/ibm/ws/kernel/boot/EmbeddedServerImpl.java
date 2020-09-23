/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants.VerifyServer;
import com.ibm.ws.kernel.boot.internal.KernelBootstrap;
import com.ibm.ws.kernel.boot.internal.ServerLock;
import com.ibm.ws.kernel.provisioning.ProductExtension;
import com.ibm.wsspi.kernel.embeddable.Server;
import com.ibm.wsspi.kernel.embeddable.ServerEventListener;
import com.ibm.wsspi.kernel.embeddable.ServerEventListener.ServerEvent;
import com.ibm.wsspi.kernel.embeddable.ServerEventListener.ServerEvent.Type;
import com.ibm.wsspi.kernel.embeddable.ServerException;

/**
 *
 */
public class EmbeddedServerImpl implements Server {

    /** Location manager and initial configuration */
    protected final BootstrapConfig bootProps = new BootstrapConfig();

    /** Lock ensuring only one VM is using the server directory/workarea (as a server) */
    protected final ServerLock testServerLock;

    /** Registered server event listener */
    protected final ServerEventListener externalListener;

    /** Last state we knew for the server... */
    protected volatile ServerEvent lastEvent = null;

    protected final AtomicReference<StopOperation> pendingStop = new AtomicReference<StopOperation>();
    protected final AtomicReference<StartOperation> pendingStart = new AtomicReference<StartOperation>();
    protected final AtomicReference<ServerTask> runningServer = new AtomicReference<ServerTask>();

    /**
     * Our own private executor for queueing operations. By default this thread pool is cached, but unbounded.
     * Since we allow only one pendingStop, and one pendingStart, any operation requested by the
     * user that doesn't snag one of those two spots, it will return quickly as a no-op/redundant
     * operation. An alternative executor can be specified at construction.
     */
    protected final ExecutorService opQueue;

    /**
     * This is an abridged version of what the Launcher does when invoked from the
     * command line. The constructor just verifies the configuration of the server--
     * reading of bootstrap properties and detailed processing of configuration is
     * deferred until server start.
     *
     * <p>Note this constructor cannot be modified since it is being used by
     * a client which could be in maven central, removing this will break
     * compatibility with older versions of the embeddable launch API. New
     * enhancements need new constructors.</p>
     *
     * @param serverName ServerName: defaultServer will be used if this is null
     * @param userDir    WLP_USER_DIR equivalent, may be null
     * @param outputDir  WLP_OUTPUT_DIR equivalent, may be null
     * @param listener   ServerEventListener that should receive notifications of Server lifecycle changes, may be null.
     *
     * @see {@link ServerEventListener}
     */
    public EmbeddedServerImpl(String serverName, File userDir, File outputDir, ServerEventListener listener) {
        this(serverName, userDir, outputDir, null, listener, null, null, null);
    }

    /**
     * This is an abridged version of what the Launcher does when invoked from the
     * command line. The constructor just verifies the configuration of the server--
     * reading of bootstrap properties and detailed processing of configuration is
     * deferred until server start.
     *
     * <p>Note this is a new constructor with new enhancement to support the LOG_DIR
     * property.</p>
     *
     * @param serverName ServerName: defaultServer will be used if this is null
     * @param userDir    WLP_USER_DIR equivalent, may be null
     * @param outputDir  WLP_OUTPUT_DIR equivalent, may be null
     * @param listener   ServerEventListener that should receive notifications of Server lifecycle changes, may be null.
     *
     * @see {@link ServerEventListener}
     */
    public EmbeddedServerImpl(String serverName, File userDir, File outputDir, File logDir, ServerEventListener listener) {
        this(serverName, userDir, outputDir, logDir, listener, null, null, null);
    }

    /**
     * This is an abridged version of what the Launcher does when invoked from the
     * command line. The constructor just verifies the configuration of the server--
     * reading of bootstrap properties and detailed processing of configuration is
     * deferred until server start.
     *
     * <p>Note this is a new constructor with new enhancement to support
     * extraProductExtensions.</p>
     *
     * @param serverName             ServerName: defaultServer will be used if this is null
     * @param userDir                WLP_USER_DIR equivalent, may be null
     * @param outputDir              WLP_OUTPUT_DIR equivalent, may be null
     * @param listener               ServerEventListener that should receive notifications of Server lifecycle changes, may be null.
     * @param extraProductExtensions HashMap of Properties, may be null
     *
     * @see {@link ServerEventListener}
     */
    public EmbeddedServerImpl(String serverName, File userDir, File outputDir, File logDir, ServerEventListener listener, HashMap<String, Properties> extraProductExtensions) {
        this(serverName, userDir, outputDir, logDir, listener, extraProductExtensions, null, null);
    }

    /**
     * This is an abridged version of what the Launcher does when invoked from the
     * command line. The constructor just verifies the configuration of the server--
     * reading of bootstrap properties and detailed processing of configuration is
     * deferred until server start.
     *
     * <p>Note this is a new constructor with new enhancement to support
     * extraProductExtensions and uses the specified ExecutorService</p>
     *
     * @param serverName             ServerName: defaultServer will be used if this is null
     * @param userDir                WLP_USER_DIR equivalent, may be null
     * @param outputDir              WLP_OUTPUT_DIR equivalent, may be null
     * @param listener               ServerEventListener that should receive notifications of Server lifecycle changes, may be null.
     * @param extraProductExtensions HashMap of Properties, may be null
     * @param executor               the executor service to use for start and stop operations
     *
     * @see {@link ServerEventListener}
     */
    public EmbeddedServerImpl(String serverName, File userDir, File outputDir, File logDir, ServerEventListener listener, HashMap<String, Properties> extraProductExtensions,
                              ExecutorService executor) {
        this(serverName, userDir, outputDir, logDir, listener, extraProductExtensions, executor, null);
    }

    /**
     * This is an abridged version of what the Launcher does when invoked from the
     * command line. The constructor just verifies the configuration of the server--
     * reading of bootstrap properties and detailed processing of configuration is
     * deferred until server start.
     *
     * <p>Note this is a new constructor with new enhancement to support an
     * alternate workarea location for embedded servers launched by utilities.</p>
     *
     * @param serverName             ServerName: defaultServer will be used if this is null
     * @param userDir                WLP_USER_DIR equivalent, may be null
     * @param outputDir              WLP_OUTPUT_DIR equivalent, may be null
     * @param logDir                 Server log file directory, may be null
     * @param listener               ServerEventListener that should receive notifications of Server lifecycle changes, may be null.
     * @param extraProductExtensions HashMap of Properties, may be null
     * @param executor               The executor service to use for start and stop operations
     * @param workareaDirStr         Embedded workarea location, null for default
     *
     * @see {@link ServerEventListener}
     */
    public EmbeddedServerImpl(String serverName, File userDir, File outputDir, File logDir, ServerEventListener listener, HashMap<String, Properties> extraProductExtensions,
                              ExecutorService executor, String workareaDirStr) {
        if (executor == null) {
            opQueue = Executors.newCachedThreadPool(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    t.setName("EmbeddedLibertyOperation-" + t.getName());
                    return t;
                }
            });
        } else {
            opQueue = executor;
        }
        // find locations using the absolute paths of the files provided, which will be normalized
        String userDirPath = userDir == null ? null : userDir.getAbsolutePath();
        String outputDirPath = outputDir == null ? null : outputDir.getAbsolutePath();
        String logDirPath = logDir == null ? null : logDir.getAbsolutePath();

        if (serverName == null) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.defaultServer"),
                                                    BootstrapConstants.DEFAULT_SERVER_NAME));
        }

        externalListener = listener;

        // Find location will throw standard exceptions w/ translated messages
        // for bad serverName or bad directories.  Workareas for a server and
        // embedded server instances launched by admin utilities must be separate.
        BootstrapLocations locations = new BootstrapLocations();
        locations.setProcessName(serverName);
        locations.setUserDir(userDirPath);
        locations.setUserDir(outputDirPath);
        locations.setLogDir(logDirPath);
        locations.setWorkAreaDir(workareaDirStr);

        bootProps.findLocations(locations);

        // PI20344 - 2014/06/16:  Setting a couple of java properties that are needed by the
        // com.ibm.ws.kernel.boot.cmdline.Utils class, which expects to have been launched from the
        // command line with WLP_USER_DIR & WLP_OUTPUT_DIR set as environment variables.  Since
        // we're embedded, those environment variables never got set from the command line.
        // Here I am setting these variables as Java properties, and the Utils class is updated to
        // check for these Java properties if the environment variables are not set.
        if (userDirPath != null) {
            System.setProperty(BootstrapConstants.ENV_WLP_USER_DIR, userDirPath);
        }
        if (outputDirPath != null) {
            System.setProperty(BootstrapConstants.ENV_WLP_OUTPUT_DIR, outputDirPath);
        }
        if (logDirPath != null) {
            System.setProperty(BootstrapConstants.ENV_LOG_DIR, logDirPath);
        }

        System.clearProperty(BootstrapConstants.ENV_PRODUCT_EXTENSIONS_ADDED_BY_EMBEDDER);
        if (extraProductExtensions != null) {
            StringBuffer buf = new StringBuffer();
            for (Entry<String, Properties> entry : extraProductExtensions.entrySet()) {
                String name = entry.getKey();
                Properties featureProperties = entry.getValue();
                String installLocation = featureProperties.getProperty(ProductExtension.PRODUCT_EXTENSIONS_INSTALL);
                String productId = featureProperties.getProperty(ProductExtension.PRODUCT_EXTENSIONS_ID);
                buf.append(name + "\n" + productId + "\n" + installLocation + "\n");
            }
            String embededData = buf.toString();
            System.setProperty(BootstrapConstants.ENV_PRODUCT_EXTENSIONS_ADDED_BY_EMBEDDER, embededData);
        }

        // Create a testServerLock -- the real lock will be owned/managed by
        // KernelBootstrap: we don't want to interfere!
        testServerLock = ServerLock.createTestLock(bootProps);
    }

    @Override
    public boolean isRunning() {
        return testServerLock.testServerRunning();
    }

    @Override
    public Future<Result> start(String... arguments) {
        return start(null, arguments);
    }

    @Override
    public Future<Result> start(Map<String, String> props, String... arguments) {
        return opQueue.submit(new StartOperation(props, arguments));
    }

    @Override
    public Future<Result> stop(String... arguments) {
        return opQueue.submit(new StopOperation(arguments));
    }

    /**
     * This supports the package command: the minify operation needs to launch the server
     * far enough for it to read config and figure out all of the features that would be
     * loaded. It doesn't actually start any of those features, but it needs to get far
     * enough to evaluate the full feature set, including features and auto-features
     * that are part of Liberty or are provided by product extensions.
     *
     * @param osRequest Super-secret internal runtime handshake
     * @return Set of strings describing the required contents for the server. Will not return null.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public Set<String> getServerContent(String osRequest) throws FileNotFoundException, IOException {
        ServerTask serverTask = runningServer.get();
        try {
            if (serverTask != null)
                return serverTask.getServerContent(osRequest);
        } catch (InterruptedException e) {
            // nothing to do here, we couldn't get the results from the server
        }

        return Collections.emptySet();
    }

    /**
     * The feature gather operation needs to launch the server far enough for it to read
     * config and figure out all of the features that would be loaded. It doesn't actually
     * start any of those features, but it needs to get far enough to evaluate the full
     * feature set, including features and auto-features that are part of Liberty or are
     * provided by product extensions.
     *
     * @return Set of strings describing the required features for the server. Will not return null.
     */
    public Set<String> getServerFeatures() {
        ServerTask serverTask = runningServer.get();
        try {
            if (serverTask != null)
                return serverTask.getServerFeatures();
        } catch (InterruptedException e) {
            // nothing to do here, we couldn't get the results from the server
        }

        return Collections.emptySet();
    }

    /**
     * Queued operation for starting the server. This does the work
     * of trying to start the server.
     */
    private class StartOperation implements Callable<Result> {
        final Map<String, String> props;
        final String[] args;

        StartOperation(Map<String, String> props, String... arguments) {
            this.props = props;
            this.args = arguments;
        }

        @Override
        public Result call() {
            if (runningServer.get() != null || !pendingStart.compareAndSet(null, this)) {
                // either the server has already been started, or another operation is
                // in the process of starting it...
                return new ServerResult(false, ReturnCode.REDUNDANT_ACTION_STATUS, null);
            }

            try {
                Map<String, String> initProps = new HashMap<String, String>(20);

                // Add passed-in properties to the map
                if (props != null && !props.isEmpty())
                    initProps.putAll(props);

                try {
                    // LaunchArguments will throw on bad/unrecognized parameters
                    List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
                    LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);
                    if (ReturnCode.OK.equals(launchArgs.getRc())) {
                        bootProps.verifyProcess(VerifyServer.EXISTS, null);
                    } else {
                        // Some OTHER action was attempted, which we don't support...
                        LaunchException le = new LaunchException("Invalid argument passed to embedded start: "
                                                                 + launchArgs.getAction(), MessageFormat.format(BootstrapConstants.messages.getString("warning.unrecognized.command"),
                                                                                                                launchArgs.getAction()), null, // no exception
                                        ReturnCode.BAD_ARGUMENT);
                        fireEvent(Type.FAILED, le);
                        return new ServerResult(false, le);
                    }
                    // configure will throw if it can't read bootstrap.properties
                    bootProps.configure(initProps);

                } catch (LocationException le) {
                    // Something is wrong w/ where paths are. We need to show that somewhere.
                    System.err.println(bootProps.printLocations(true));
                    fireEvent(Type.FAILED, le);
                    return new ServerResult(false, ReturnCode.LOCATION_EXCEPTION, le);
                } catch (LaunchException le) {
                    // Something is wrong w/ where paths are. We need to show that somewhere.
                    System.err.println(bootProps.printLocations(true));
                    fireEvent(Type.FAILED, le);
                    return new ServerResult(false, le.getReturnCode(), le);
                }

                // Create kernel bootstrap and the server task
                KernelBootstrap bootstrap = new KernelBootstrap(bootProps);
                ServerTask serverTask = new ServerTask(bootstrap);

                // Set the server task as current
                if (runningServer.compareAndSet(null, serverTask)) {
                    // Dig it. This is the server task.
                    Thread t = createServerThread(serverTask);

                    // START THE SERVER TASK....
                    t.start();
                } else {
                    // RETURN: Something else started a running task (unlikely)..
                    return new ServerResult(true, ReturnCode.REDUNDANT_ACTION_STATUS, null);
                }

                // WAIT FOR THE SERVER TO FINISH STARTING
                try {
                    if (serverTask.waitForStarted()) {
                        fireEvent(Type.STARTED, null);
                        return new ServerResult(true, ReturnCode.OK, null);
                    } else {
                        fireEvent(Type.FAILED, null);
                        return new ServerResult(false, ReturnCode.ERROR_SERVER_START, null);
                    }
                } catch (InterruptedException e) {
                    return new ServerResult(false, ReturnCode.SERVER_UNKNOWN_STATUS, null);
                }
            } finally {
                // I'm done!
                pendingStart.set(null);
            }
        }
    }

    /**
     * Queued operation for stopping the server. This does the work
     * of trying to stop the server.
     */
    private class StopOperation implements Callable<Result> {

        private boolean isForceShutdown = false;

        /**
         * Handles argument parsing for stop command.
         *
         * @param arguments
         */
        StopOperation(String... arguments) {
            List<String> cmdArgs = Arrays.asList(arguments);

            if (0 < cmdArgs.size()) {
                Iterator<String> i = cmdArgs.listIterator();
                while (i.hasNext()) {
                    String arg = i.next();

                    if ((arg != null) && (arg.equalsIgnoreCase("--force"))) {
                        isForceShutdown = true;
                    } else {
                        // Ignore unknown arguments so we do not break anyone
                        // anyone who is currently passing unknown arguments
                        continue;
                    }
                }
            }
        }

        @Override
        public Result call() {
            if (runningServer.get() == null || !pendingStop.compareAndSet(null, this)) {
                // either the server has already been stopped, or another operation is
                // in the process of stopping it...
                return new ServerResult(false, ReturnCode.REDUNDANT_ACTION_STATUS, null);
            }

            try {
                ServerTask server = runningServer.get();
                if (server != null) {
                    // SHUTDOWN, AND WAIT FOR STOP
                    ReturnCode rc;
                    if (isForceShutdown) {
                        rc = server.shutdown(isForceShutdown);
                    } else {
                        rc = server.shutdown();
                    }
                    return new ServerResult(rc == ReturnCode.OK, rc, null);
                } else {
                    // Server was already stopped
                    return new ServerResult(false, ReturnCode.REDUNDANT_ACTION_STATUS, null);
                }
            } catch (InterruptedException e) {
                return new ServerResult(false, ReturnCode.SERVER_UNKNOWN_STATUS, null);
            } finally {
                // done
                pendingStop.set(null);
            }
        }
    }

    private void fireEvent(Type type, ServerException ex) {
        // Set the new event
        ServerEvent event = lastEvent = new ServerEventImpl(type, ex);

        if (externalListener != null) {
            try {
                externalListener.serverEvent(event);
            } catch (Throwable t) {
                // do nothing. Just prevent harm
            }
        }
    }

    /**
     * This is the long-running server task. Once started (via the runningServer executor), it
     * will keep running until the server stops.
     */
    protected class ServerTask implements Runnable {
        /** KernelBootstrap, kept to enable minify to obtain server content */
        protected final KernelBootstrap kernelBootstrap;

        ServerTask(KernelBootstrap bootstrap) {
            this.kernelBootstrap = bootstrap;
        }

        public boolean waitForStarted() throws InterruptedException {
            return kernelBootstrap.waitForStarted();
        }

        public ReturnCode shutdown() throws InterruptedException {
            return kernelBootstrap.shutdown();
        }

        public ReturnCode shutdown(boolean force) throws InterruptedException {
            return kernelBootstrap.shutdown(force);
        }

        public Set<String> getServerContent(String osRequest) throws FileNotFoundException, IOException, InterruptedException {
            return kernelBootstrap.getServerContent(osRequest);
        }

        public Set<String> getServerFeatures() throws InterruptedException {
            return kernelBootstrap.getServerFeatures();
        }

        @Override
        public void run() {
            // This task was queued!
            fireEvent(Type.STARTING, null);

            ServerException ex = null;
            // GO will not return until the server has stopped
            try {
                kernelBootstrap.go();
            } catch (LaunchException e) {
                ex = e;
            } finally {
                // This server is done!
                fireEvent(Type.STOPPED, ex);
                runningServer.compareAndSet(this, null);
            }
        }
    };

    class ServerEventImpl implements ServerEvent {
        final Type t;
        final ServerException ex;

        ServerEventImpl(Type t, ServerException ex) {
            this.t = t;
            this.ex = ex;
        }

        @Override
        public Server getServer() {
            return EmbeddedServerImpl.this;
        }

        @Override
        public Type getType() {
            return t;
        }

        @Override
        public ServerException getException() {
            return ex;
        }

        @Override
        public String toString() {
            return "ServerEvent[" + t
                   + (ex == null ? "" : ", ex=" + ex.toString())
                   + "]";
        }
    }

    static class ServerResult implements Server.Result {

        final boolean successful;
        final ReturnCode rc;
        final ServerException srvEx;

        ServerResult(boolean successful, ReturnCode rc, ServerException srvEx) {
            this.successful = successful;
            this.rc = rc;
            this.srvEx = srvEx;
        }

        ServerResult(boolean successful, LaunchException srvEx) {
            this.successful = successful;
            this.rc = srvEx.getReturnCode();
            this.srvEx = srvEx;
        }

        ServerResult(boolean successful, ServerEvent event) {
            this.successful = successful;
            this.srvEx = event.getException();
            if (srvEx != null) {
                if (srvEx instanceof LaunchException)
                    this.rc = ((LaunchException) srvEx).getReturnCode();
                else
                    this.rc = ReturnCode.UNKNOWN_EXCEPTION;
            } else {
                this.rc = ReturnCode.OK;
            }
        }

        @Override
        public boolean successful() {
            return successful;
        }

        @Override
        public int getReturnCode() {
            return rc.val;
        }

        @Override
        public ServerException getException() {
            return srvEx;
        }
    }

    /**
     * The EmbeddedLibertyServer is not a daemon thread: if the server
     * or framework is running, we want this process to stay awake. There
     * are a lot of ways to get it unstuck... (or diagnose a hung
     * server or whatever). But as soon as the framework does stop, then
     * this thread will go away.
     *
     * @param r
     * @return
     */
    private Thread createServerThread(ServerTask r) {
        Thread t = new Thread(r);
        t.setName("EmbeddedLibertyServer-" + t.getName());
        return t;
    }
}
