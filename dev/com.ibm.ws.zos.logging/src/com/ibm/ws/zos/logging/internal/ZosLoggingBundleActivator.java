/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.jni.NativeMethodUtils;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 * BundleActivator for com.ibm.ws.zos.logging bundle.
 *
 * Manages the registration / deregistration of z-specific LogHandlers,
 * based on the runtime env (e.g. STARTED TASK vs shell) and server config.
 *
 */
public class ZosLoggingBundleActivator implements BundleActivator, ServiceTrackerCustomizer<WsLocationAdmin, WsLocationAdmin> {
    private final static TraceComponent tc = Tr.register(ZosLoggingBundleActivator.class);
    /**
     * ServiceTracker listens for NativeMethodManager and forwards registrations/deregistrations
     * to ZosLoggingBundleActivator.
     */
    private volatile NativeMethodManagerServiceTracker nativeMethodManagerServiceTracker = new NativeMethodManagerServiceTracker(this);

    /**
     * native method manager reference.
     */
    private volatile NativeMethodManager nativeMethodManager;

    /**
     * For WTO logging.
     */
    private final LoggingWtoLogHandler wtoLogHandler = new LoggingWtoLogHandler(this);

    /**
     * For Hardcopy logging.
     */
    private final LoggingHardcopyLogHandler hardcopyLogHandler = new LoggingHardcopyLogHandler(this);

    /**
     * For MSGLOG logging.
     */
    private final MsgLogLogHandler msgLogLogHandler = new MsgLogLogHandler(this);

    /**
     * ManagedService that listens for updates to the <zosLogging> element.
     */
    private final ZosLoggingConfigListener configListener = new ZosLoggingConfigListener(this);

    /**
     * Used to determine whether WTO is required
     * for this launch context, making the config irrelevant
     */
    private volatile boolean isWTORequired = false;

    /**
     * If MSGLOG DD is defined we register the MsgLogLogHandler.
     */
    private volatile boolean isMsgLogDDDefined = false;

    /**
     * The name of the DD to write the messages.log file to
     */
    private volatile String ddName = "";
    /**
     * Used to determine whether WTO has been enabled in the config
     */
    private volatile boolean disableWtoMessages = false;

    /**
     * Used to determine whether WTO has been enabled in the config
     */
    private volatile boolean disableHardcopyMessages = false;

    /**
     * Used to determine whether WTO has been enabled in the config
     */
    private volatile boolean enableLogToMVS = false;

    /**
     * Indicates the bundle has been stopped
     */
    private volatile boolean stopped = false;

    /**
     * We've read the configuration at least once.
     */
    private volatile boolean configIsSet = false;

    /**
     * Stand alone thread used to "own" the msglog DD open
     */
    private static volatile OpenFileThread openFileThread = null;

    /**
     * For registering our LogHandler service(s).
     */
    private BundleContext bContext;

    /**
     * The server's name, for printing the shutdown message.
     */
    private String serverName = "*UNKNOWN*";
    private ServiceTracker<WsLocationAdmin, WsLocationAdmin> wsLocationAdmin;

    /**
     * Bundle activator: start
     */
    @Override
    public void start(BundleContext bundleContext) {
        bContext = bundleContext;
        wsLocationAdmin = new ServiceTracker<WsLocationAdmin, WsLocationAdmin>(bContext, WsLocationAdmin.class, this);
        wsLocationAdmin.open();

        // Register a ManagedService to be notified when the com.ibm.ws.zos.logging.config PID is updated.
        // When the PID is updated the configLIstener calls configUpdated() in this class.
        configListener.register(bundleContext);

        // ServiceTracker for NativeMethodManager.
        // When NativeMethodManager becomes available, native methods are registered and
        // the code immediately checks the native runtime env.  If we're a STARTED TASK,
        // then we register the LoggingWtoLogHandler.  If we're a STARTED TASK and the JCL
        // has the MSGLOG DD defined, then we register the MsgLogLogHandler.
        nativeMethodManagerServiceTracker.open(bundleContext);
    }

    /**
     * Bundle activator: stop
     */
    @Override
    public void stop(BundleContext ctx) {
        logStopMessage();
        stopped = true;

        nativeMethodManagerServiceTracker.close();

        toggleRegistration();

        isWTORequired = false;
        enableLogToMVS = false;
        isMsgLogDDDefined = false;
        disableWtoMessages = false;
        disableHardcopyMessages = false;
        wsLocationAdmin.close();
    }

    /**
     * Called by ZosLoggingConfigListener when <zosLogging> is updated.
     *
     * Register/deregister LogHandlers according to the updated config.
     */
    protected void configUpdated(Dictionary config) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "current values, enableLogToMVS:" + enableLogToMVS
                         + "\ndisableWtoMessages:" + disableWtoMessages
                         + "\ndisableHardcopyMessages:" + disableHardcopyMessages
                         + "\nmessageLogDD:" + ddName);
        }

        // Process config
        enableLogToMVS = (Boolean) config.get("enableLogToMVS");
        disableWtoMessages = (Boolean) config.get("disableWtoMessages");
        disableHardcopyMessages = (Boolean) config.get("disableHardcopyMessages");

        // Get the specified DDName from the server.xml file
        // TODO: Need to handle config updating the messageLogDD value.   Need to cause existing MSGLOG DD to close and then open
        //       new DD.  Currently, the old won't be closed as long as the "current" setting of ddName exists.
        ddName = (String) config.get("messageLogDD");

        // Validate the specified DDName
        if ((ddName.length() < 1) || ddName.length() > 8) {
            Tr.info(tc, "DDNAME_TOO_LONG");
            // Change to using the default value
            ddName = "MSGLOG";
        }

        configIsSet = true;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "updated values, enableLogToMVS:" + enableLogToMVS
                         + "\ndisableWtoMessages:" + disableWtoMessages
                         + "\ndisableHardcopyMessages:" + disableHardcopyMessages
                         + "\nmessageLogDD:" + ddName);
        }
        toggleRegistration();
    }

    /**
     * Injected by the NativeMethodManagerServiceTracker.
     */
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {

        this.nativeMethodManager = nativeMethodManager;

        // Attempt to load native code via the method manager.
        nativeMethodManager.registerNatives(ZosLoggingBundleActivator.class);

        // go native to determine the launch context
        isWTORequired = !isLaunchContextShell();

        toggleRegistration();
    }

    /**
     * Un-Injected by the NativeMethodManagerServiceTracker.
     */
    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        if (this.nativeMethodManager == nativeMethodManager) {
            this.nativeMethodManager = null;
        }
    }

    /**
     * Determine whether or not to enable WTOs for spawned JVMs.
     */
    private synchronized void toggleRegistration() {

        if (stopped || nativeMethodManager == null) {
            // force off
            wtoLogHandler.unregister();
            hardcopyLogHandler.unregister();
            msgLogLogHandler.unregister();
            return;
        }

        // If we haven't set the config at least once, just exit.  When we
        // register a log handler, we're handed all (or some) of the messages
        // that were issued in the past.  If we register the handler before we
        // read the config, we will effectively throw all those messages away
        // before being given an opportunity to issue them to WTO or hardcopy.
        if (configIsSet == false) {
            return;
        }

        // determine if MSGLOG DD is defined.
        // TODO: if ddName changed with a config update...we need to close old, then open new....configUpdated sets new value before calling here.
        isMsgLogDDDefined = isMsgLogDDDefined(ddName);

        if (isWTORequired) {
            // We were started by the native launcher,
            // WTO is required
            if (!disableWtoMessages) {
                wtoLogHandler.register(bContext);
            } else {
                wtoLogHandler.unregister();
            }

            if (!disableHardcopyMessages) {
                hardcopyLogHandler.register(bContext);
            } else {
                hardcopyLogHandler.unregister();
            }
        } else {
            // We were started from USS. Use the value obtained from
            // server.xml's <zosLogging enableLogToMVS="true|false"/> property
            // in updated(). False is the default when enableLogToMVS is missing
            // or has a value other than "true" or "false" (case-insensitive).
            if (enableLogToMVS) {
                // We were switched on in the config
                if (!disableWtoMessages) {
                    wtoLogHandler.register(bContext);
                } else {
                    wtoLogHandler.unregister();
                }

                if (!disableHardcopyMessages) {
                    hardcopyLogHandler.register(bContext);
                } else {
                    hardcopyLogHandler.unregister();
                }
            } else {
                // We were switched off in the config
                wtoLogHandler.unregister(); // disabled by enableLogToMVS

                if (!disableHardcopyMessages) { // specified by disableMessageHardcopy
                    hardcopyLogHandler.register(bContext);
                } else {
                    hardcopyLogHandler.unregister();
                }
            }
        }

        if (isMsgLogDDDefined) {
            msgLogLogHandler.register(bContext);

        } else {
            msgLogLogHandler.unregister();
        }
    }

    /**
     * Helper method so auto entry/exit can be injected if necessary.
     *
     * @return true if server was launched from the shell; false if launched as a STARTED TASK.
     */
    private boolean isLaunchContextShell() {
        return ntv_isLaunchContextShell();
    }

    /**
     * Helper method so auto entry/exit can be injected if necessary.
     *
     * @return true if MSGLOG DD is defined; false otherwise.
     */
    private boolean isMsgLogDDDefined(String ddname) {

        // Defensive check before calling native method
        if (ddName != null && ddName.length() > 0 && ddName.length() < 9) {
            return ntv_isMsgLogDDDefined(ddname);
        } else {
            return false;
        }
    }

    /**
     * Inner class for spawning and managing a separate thread to own the opening
     * of a file. The thread is not from the common pool and is not allowed to
     * terminate with an open file. The z/OS native file support associates recovery
     * against the opening thread and would close the file if it terminated.
     */
    protected class OpenFileThread extends Thread {
        private boolean done = false;
        private long file = 0;
        public int openErrno = 0;
        public int openErrno2 = 0;
        private CountDownLatch openLatch = null;

        OpenFileThread() {

            openLatch = new CountDownLatch(1);
        }

        //@FFDCIgnore(value = { InterruptedException.class })
        @Override
        public void run() {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "OpenFileThread.run() entry");
            }

            ByteBuffer errorCodes = ByteBuffer.allocate(8);
            String fileName = "DD:" + ddName;
            file = ntv_openFile(NativeMethodUtils.convertToEBCDIC(fileName, false), errorCodes.array());

            if (file == 0) {
                // Save failure errno's for caller.
                openErrno = errorCodes.asIntBuffer().get(0);
                openErrno2 = errorCodes.asIntBuffer().get(1);

                openLatch.countDown();
            }

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "OpenFileThread:" + fileName + ", ntv_openfile rc:" + file);
            }

            if (file != 0) {
                try {
                    synchronized (this) {
                        // Wake up caller.
                        openLatch.countDown();

                        while (!done) {
                            this.wait();
                        }
                    }
                } catch (InterruptedException e) {
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "OpenThread.run() exit");
            }
        }

        public void end() {
            synchronized (this) {
                done = true;

                // Wake up this thread to terminate.
                this.notify();
            }
        }

        public long getFilePtr() {
            return file;
        }

        public void waitForOpen() {
            try {
                openLatch.await();
            } catch (InterruptedException e) {
            }
        }

    }

    private void startOpenFileThread() {
        openFileThread = AccessController.doPrivileged(new PrivilegedAction<OpenFileThread>() {
            @Override
            public OpenFileThread run() {
                OpenFileThread thread = new OpenFileThread();
                return thread;
            }
        });

        openFileThread.setName("zOS open file Thread--" + this.ddName);
        openFileThread.setDaemon(true);
        openFileThread.start();

        // Wait for open to complete
        openFileThread.waitForOpen();
    }

    private static void stopOpenFileThread() {
        if (openFileThread != null) {
            openFileThread.end();
            openFileThread = null;
        }
    }

    /**
     *
     * @return the native FILE *
     *
     * @throws IOException if DD:MSGLOG could not be opened.
     */
    protected long openFile() throws IOException {
        // Issue open on its own threads that will not terminate until close().
        startOpenFileThread();

        // Retrieve the open DD reference
        long retMe = openFileThread.getFilePtr();

        // Check for error
        if (retMe == 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The server could not open FILE: " + ddName);
            }

            // Open failed...Terminate open thread.
            int errno = openFileThread.openErrno;
            int errno2 = openFileThread.openErrno2;
            stopOpenFileThread();

            // Tell caller that open failed.
            throw new IOException("The server could not open file "
                                  + ddName + "."
                                  + " errno: " + errno
                                  + " errno2: " + errno2);
        }

        return retMe;
    }

    /**
     * Call to native code to write the message to the MSGLOG DD card
     *
     * @throws IOException on error
     */
    protected void writeFile(long filePtr, String msg) throws IOException {

        ByteBuffer errorCodes = ByteBuffer.allocate(8);
        int rc = ntv_writeFile(filePtr, NativeMethodUtils.convertToEBCDIC(msg, false), errorCodes.array());

        int errno = errorCodes.asIntBuffer().get(0);

        if (errno != 0) {
            throw new IOException("The server could not write to FILE ptr x" + Long.toHexString(filePtr) + "."
                                  + " rc: " + rc
                                  + " errno: " + errno
                                  + " errno2: " + errorCodes.asIntBuffer().get(1)
                                  + " msg: " + msg);
        }
    }

    /**
     * Call to native code to close the MSGLOG DD file
     *
     */
    protected void closeFile(long filePtr) {

        int rc = ntv_closeFile(filePtr);

        if (rc != 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The server could not close FILE " + ddName + " with file ptr x" + Long.toHexString(filePtr)
                             + " rc: " + rc);
            }
        }

        // Terminate the spawned thread from the open.
        stopOpenFileThread();
    }

    /**
     * Issuing server shut down messages to WTO log handler based on server configuration.
     * This is the last point before the logging handler is deregistered.
     */
    private void logStopMessage() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Server stoping, issuing stoping message for the kernel shut down.");
        }

        // disableWtoMessages can override when isWTORequired is true
        boolean routeToWTO = false;
        if (isWTORequired) {
            routeToWTO = true;
            if (disableWtoMessages) {
                routeToWTO = false;
            }
        } else {
            if (enableLogToMVS) {
                routeToWTO = true;
                if (disableWtoMessages) {
                    routeToWTO = false;
                }
            } else {
                routeToWTO = false;
            }
        }
        if (routeToWTO) {
            Locale localeUS = new Locale.Builder().setLanguage("en").setRegion("US").build();
            List<Locale> localeList = new ArrayList<Locale>();
            localeList.add(localeUS);

            String shutDownMsgEN = Tr.formatMessage(tc, localeList, "KERNEL_SHUTDOWN_PRINT", serverName);
            wtoLogHandler.publish_eng(shutDownMsgEN);
        }
    }

    /**
     * Call to native code to write the message to the operator console.
     *
     * @return 0 on success; non-zero on error
     */
    protected native int ntv_WriteToOperatorConsole(byte[] msg);

    /**
     * Call to native code to write the message to the programmer and hardcopy.
     *
     */
    protected native int ntv_WriteToOperatorProgrammerAndHardcopy(byte[] msg);

    /**
     * @return true if server was launched from the shell; false if launched as a STARTED TASK.
     */
    protected native boolean ntv_isLaunchContextShell();

    /**
     * @param the name of the ddName to check for
     * @return true if MSGLOG DD is defined; false otherwise.
     */
    protected native boolean ntv_isMsgLogDDDefined(String ddName);

    /**
     * Call to native code to open the given file.
     *
     * @return the FILE *, or 0 if an error occurred (errorCodes set)
     */
    protected native long ntv_openFile(byte[] fileName, byte[] errorCodes);

    /**
     * Write the given msg to the given file.
     *
     * @return 0 if all is well; non-zero for error (errorCodes set)
     */
    protected native int ntv_writeFile(long filePtr, byte[] msg, byte[] errorCodes);

    /**
     * Close the given file.
     *
     * @return 0 if all is well; non-zero for error
     */
    protected native int ntv_closeFile(long filePtr);

    @Override
    public WsLocationAdmin addingService(ServiceReference<WsLocationAdmin> ref) {
        WsLocationAdmin wsLocAdmin = bContext.getService(ref);
        if (wsLocAdmin != null) {
            serverName = wsLocAdmin.getServerName();
        }
        return wsLocAdmin;
    }

    @Override
    public void modifiedService(ServiceReference<WsLocationAdmin> arg0, WsLocationAdmin arg1) {
        // do nothing
    }

    @Override
    public void removedService(ServiceReference<WsLocationAdmin> arg0, WsLocationAdmin arg1) {
        // do nothing
    }
}
