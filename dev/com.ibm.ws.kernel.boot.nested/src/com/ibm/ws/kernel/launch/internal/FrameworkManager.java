/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.instrument.Instrumentation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.ClientRunnerException;
import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.FileUtils;
import com.ibm.ws.kernel.boot.internal.KernelStartLevel;
import com.ibm.ws.kernel.boot.internal.commands.JavaDumpAction;
import com.ibm.ws.kernel.boot.internal.commands.JavaDumper;
import com.ibm.ws.kernel.boot.internal.commands.ServerDumpUtil;
import com.ibm.ws.kernel.boot.jmx.internal.PlatformMBeanServerBuilder;
import com.ibm.ws.kernel.boot.jmx.internal.PlatformMBeanServerBuilderListener;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerPipeline;
import com.ibm.ws.kernel.launch.internal.Provisioner.InvalidBundleContextException;
import com.ibm.ws.kernel.launch.service.ClientRunner;
import com.ibm.ws.kernel.launch.service.ForcedServerStop;
import com.ibm.ws.kernel.launch.service.FrameworkReady;
import com.ibm.ws.kernel.launch.service.PauseableComponentController;
import com.ibm.ws.kernel.launch.service.PauseableComponentControllerRequestFailedException;
import com.ibm.ws.kernel.launch.service.ServerContent;
import com.ibm.ws.kernel.launch.service.ServerFeatures;
import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.wsspi.logging.Introspector;
import com.ibm.wsspi.logprovider.LogProvider;

/**
 * Implementation of FrameworkManager. There are several important threads:
 *
 * <dl>
 * <dt>main
 * <dd>starts framework, waits for the framework stop, starts/closes the
 * command listener, and notifies the kernel launched/shutdown latches.
 *
 * <dt>shutdown hook
 * <dd>started by System.exit (if called); waits for kernel shutdown latch
 *
 * <dt>framework start level
 * <dd>starts and stops bundles; the thread waited upon by the main thread
 *
 * <dt>command listener
 * <dd>listens for commands on the server command port; requests framework stop
 *
 * <dt>command response status
 * <dd>responds to status requests for "starting" (waits for kernel launched)
 * and "stop" (waits for command listener to be closed)
 * </dl>
 */
public class FrameworkManager {
    private static final TraceComponent tc = Tr.register(FrameworkManager.class);

    private static final List<String> licLangs = Arrays.asList(new String[] { "cs", "de", "el", "es", "fr", "in", "it", "ja",
                                                                              "ko", "lt", "pl", "pt", "ru", "sl", "tr", "zh", "zh_TW" });
    /**
     * Command listener waiting for stop commands.
     */
    protected ServerCommandListener sc = null;

    /**
     * JVM Shutdown hook: this gets called when the JVM shuts down and attempts
     * to stop the framework (a no-op if the framework is already stopping).
     * This ensures that a framework stop is at least attempted when the JVM is
     * stopped.
     */
    protected ShutdownHook shutdownHook;

    /**
     * the bootstrap config
     */
    protected BootstrapConfig config = null;

    /**
     * The Framework classloader is retrieved from BootstrapConfig as the
     * framework instance is being created.
     */
    protected ClassLoader fwkClassloader;

    /**
     * Reference to framework instance. This field will be set to non-null if
     * the framework started successfully (and will therefore prevent the JVM
     * from exiting due to a running background framework thread).
     *
     * @see #waitForFramework
     */
    protected Framework framework = null;

    /**
     * Latch that is notified when the framework has started or a fatal error
     * has occurred. If Framework.start was successfully called, then
     * both {@link #framework} and {@link #systemBundleCtx} will be non-null.
     * Otherwise, framework will be null.
     *
     * @see #waitForFramework
     */
    protected final CountDownLatch frameworkLatch = new CountDownLatch(1);

    /**
     * True if the framework was launched successfully. The value of this
     * variable is only valid if {@link #frameworkLaunched} has been notified.
     */
    protected boolean frameworkLaunchSuccess;

    /**
     * Latch that is notified when the framework is ready. This latch will only
     * be notified if {@link #waitForFramework} returns non-null.
     */
    protected final CountDownLatch frameworkLaunched = new CountDownLatch(1);

    /**
     * Latch that is notified when the framework has stopped.
     */
    protected final CountDownLatch frameworkShutdownLatch = new CountDownLatch(1);

    /** Reference to OSGi bundle context */
    protected BundleContext systemBundleCtx;

    /** Start time is retrieved from BootstrapConfig: set by Launcher */
    protected long startTime;

    /** ServiceTracker for injecting ThreadIdentityService impls into ThreadIdentityManager. */
    private ThreadIdentityManagerConfigurator threadIdentityManagerTracker;

    /* A registered service for ClientRunner and only used in a client process. */
    private ClientRunner clientRunner;

    /**
     * Create and launch the OSGi framework
     *
     * @param config
     *            BootstrapConfig object encapsulating active initial framework
     *            properties
     * @param logProvider
     *            The initialized/active log provider that must be included in
     *            framework management activities (start/stop/.. ), or null
     * @param callback
     */
    public void launchFramework(BootstrapConfig config, LogProvider logProvider) {
        if (config == null)
            throw new IllegalArgumentException("bootstrap config must not be null");
        boolean isClient = config.getProcessType().equals(BootstrapConstants.LOC_PROCESS_TYPE_CLIENT);
        try {
            String nTime = config.remove(BootstrapConstants.LAUNCH_TIME);
            startTime = nTime == null ? System.nanoTime() : Long.parseLong(nTime);
            if (isClient) {
                Tr.audit(tc, "audit.launchTime.client", config.getProcessName());
            } else {
                Tr.audit(tc, "audit.launchTime", config.getProcessName());
            }

            outputLicenseRestrictionMessage();

            outputEmbeddedProductExtensions();

            outputEnvironmentVariableProductExtensions();

            // Save the bootstrap config locally
            this.config = config;

            boolean j2secManager = false;
            if (config.get(BootstrapConstants.JAVA_2_SECURITY_PROPERTY) != null) {
                j2secManager = true;
            }

            String j2secNoRethrow = config.get(BootstrapConstants.JAVA_2_SECURITY_NORETHROW);

            if (j2secManager) {

                if (j2secNoRethrow == null || j2secNoRethrow.equals("false")) {
                    try {
                        AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Void>() {
                            @Override
                            public Void run() throws Exception {
                                System.setSecurityManager(new SecurityManager());
                                return null;
                            }
                        });
                    } catch (Exception ex) {

                        Tr.error(tc, "error.set.securitymanager", ex.getMessage());
                    }
                } else {
                    if ("true".equals(config.get(BootstrapConstants.JAVA_2_SECURITY_UNIQUE)))
                        MissingDoPrivDetectionSecurityManager.setUniqueOnly(true);
                    try {
                        AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Void>() {
                            @Override
                            public Void run() throws Exception {
                                System.setSecurityManager(new MissingDoPrivDetectionSecurityManager());
                                return null;
                            }
                        });
                    } catch (Exception ex) {

                        Tr.error(tc, "error.set.trace.securitymanager", ex.getMessage());
                    }

                }
                Tr.info(tc, "info.java2security.started", config.getProcessName());

            }

            // Start the framework.
            Framework fwk = startFramework(config);

            if (fwk == null) {
                Tr.error(tc, "error.unableToLaunch");
                return;
            }
            // Set the framework variables only if everything succeeded.
            systemBundleCtx = fwk.getBundleContext();
            framework = fwk;
        } catch (BundleException e) {
            throw new RuntimeException(e);
        } finally {
            // If an error occurred, notify anyone that was waiting for the
            // framework so they know it's not coming.
            frameworkLatch.countDown();

            try {
                if (framework != null) {
                    try {
                        addShutdownHook(isClient);
                        startServerCommandListener();
                        innerLaunchFramework(isClient);

                        // Indicate that kernel has been started
                        Tr.info(tc, "audit.kernelStartTime", getElapsedTime(false));
                        frameworkLaunchSuccess = true;
                    } finally {
                        // If an error occurred, notify anyone that was waiting
                        // for launch so they know we're done.
                        frameworkLaunched.countDown();
                        try {
                            if (!frameworkLaunchSuccess) {
                                stopFramework();
                            } else if (isClient) {
                                try {
                                    if (waitForReady()) {
                                        launchClient();
                                    }
                                } catch (InterruptedException e) {
                                    // Ignore
                                } catch (Throwable t) {
                                    throw new ClientRunnerException("Error while executing running the application", BootstrapConstants.messages.getString("error.client.runner"), t);

                                } finally {
                                    stopFramework();
                                }
                            }
                        } finally {
                            // Run the server: wait indefinitely until framework stop.
                            // (It might have been stopped above if an error occurred.)
                            waitForFrameworkStop();

                            // Remove the shutdown hook in case someone stopped the OSGi
                            // framework without calling our shutdownFramework() method.
                            removeShutdownHook();

                            // Close the command listener port, and stop any of its threads.
                            if (sc != null) {
                                sc.close();
                            }

                            if (frameworkLaunchSuccess) {
                                if (isClient) {
                                    Tr.audit(tc, "audit.kernelUpTime.client", config.getProcessName(), getElapsedTime(true));
                                } else {
                                    Tr.audit(tc, "audit.kernelUpTime", config.getProcessName(), getElapsedTime(true));
                                }
                            }
                        }
                    }
                }
            } finally {
                // Stop the log provider.
                if (logProvider != null) {
                    logProvider.stop();
                }

                // Finally, notify any waiters that the kernel has been shutdown.
                // This is done after stopping the log provider so that logs are
                // flushed in case the shutdown hook will immediately exit the JVM.
                frameworkShutdownLatch.countDown();
            }
        }
    }

    /**
     * @return
     */
    private boolean isJava6() {
        return "1.6".equals(System.getProperty("java.specification.version"));
    }

    private void launchClient() {
        ServiceReference<ClientRunner> reference = systemBundleCtx.getServiceReference(ClientRunner.class);
        clientRunner = reference == null ? null : systemBundleCtx.getService(reference);
        if (clientRunner == null) {
            Tr.error(tc, "error.client.runner.missing");
            return;
        }
        clientRunner.run();
    }

    /**
     *
     */
    private void outputLicenseRestrictionMessage() {
        try {
            // TODO need to work out how to strip the IBM branding knowledge out of here.
            ProductInfo pi = ProductInfo.getAllProductInfo().get("com.ibm.websphere.appserver");
            if (pi != null && pi.getReplacedBy() == null) {
                String edition = String.valueOf(pi.getEdition()).toLowerCase();
                String licenseType = String.valueOf(pi.getProperty("com.ibm.websphere.productLicenseType")).toLowerCase();
                String key = "audit.licenseRestriction." + edition + '.' + licenseType;
                ResourceBundle rb = TraceNLS.getBaseResourceBundle(FrameworkManager.class, tc.getResourceBundleName());
                if (rb.containsKey(key)) {
                    String file = getLang(Locale.getDefault()) + ".html";
                    Tr.audit(tc, key, "https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/license/" +
                                      edition + '/' + licenseType + '/' + pi.getVersion() + "/lafiles/" + file);
                }
            }
        } catch (ProductInfoParseException e) {
        } catch (DuplicateProductInfoException e) {
        } catch (ProductInfoReplaceException e) {
        }
    }

    private void outputEmbeddedProductExtensions() {
        String embededData = System.getProperty(BootstrapConstants.ENV_PRODUCT_EXTENSIONS_ADDED_BY_EMBEDDER);

        if (embededData != null) {
            String[] extensions = embededData.split("\n");
            for (int i = 0; (i < extensions.length) && ((i + 3) <= extensions.length); i = i + 3) {

                Tr.info(tc, "info.addProductExtension", extensions[i], extensions[i + 1], extensions[i + 2]);

            }
        }
    }

    private void outputEnvironmentVariableProductExtensions() {
        String envData = System.getProperty(BootstrapConstants.ENV_PRODUCT_EXTENSIONS_ADDED_BY_ENV);

        if (envData != null) {
            String[] extensions = envData.split("\n");
            for (int i = 0; (i < extensions.length) && ((i + 3) <= extensions.length); i = i + 3) {

                Tr.info(tc, "info.envProductExtension", extensions[i], extensions[i + 1], extensions[i + 2]);

            }
        }
    }

    /**
     * @param default1
     * @return
     */
    private static String getLang(Locale l) {
        String lang = "en";

        String option = l.getLanguage() + '_' + l.getCountry();
        if (licLangs.contains(option)) {
            lang = option;
        } else if (licLangs.contains(l.getLanguage())) {
            lang = l.getLanguage();
        }

        return lang;
    }

    /**
     * Register core services and perform initial provisioning.
     * Separate method to allow test to avoid/swap out the behavior.
     */
    @FFDCIgnore({ InvalidBundleContextException.class, LaunchException.class })
    protected void innerLaunchFramework(boolean isClient) {
        try {
            // Register services
            registerInstrumentationService(systemBundleCtx);
            registerLibertyProcessService(systemBundleCtx, config);
            preRegisterMBeanServerPipelineService(systemBundleCtx);
            openThreadIdentityTracker(systemBundleCtx);
            registerPauseableComponentController(systemBundleCtx);

            // Initialize the repo registry: we can use caches AND logging for messages
            BundleRepositoryRegistry.initializeDefaults(config.getProcessName(), true, isClient);

            Provisioner provisioner = new ProvisionerImpl();
            provisioner.initialProvisioning(systemBundleCtx, config);

            // All of our kernel provisioning activities are complete!
            config.getKernelResolver().dispose();
        } catch (LaunchException le) {
            // this is one of ours, no need to wrap again, just rethrow
            throw le;
        } catch (InvalidBundleContextException e) {
            // no-op: the framework was shutdown while bundles were being
            // installed -- things should stop gracefully w/ no ugly error
            // messages.
        }
    }

    /**
     * Register the command line service with the OSGi framework service
     * registry. This gives service consumers access to the command line used to
     * launch the platform/runtime.
     *
     * @param systemBundleCtx
     *            The framework system bundle context
     * @param config
     *            The active bootstrap config
     */
    private void registerLibertyProcessService(BundleContext systemBundleCtx, BootstrapConfig config) {
        List<String> cmds = config.getCmdArgs();
        if (cmds == null)
            cmds = new ArrayList<String>();

        LibertyProcessImpl processImpl = new LibertyProcessImpl(cmds, this);
        systemBundleCtx.registerService(LibertyProcess.class.getName(), processImpl, processImpl.getServiceProps());
    }

    /**
     * Register the instrumentation class as a service in the OSGi registry
     *
     * @param systemBundleCtx
     *            The framework system bundle context
     */
    protected void registerInstrumentationService(BundleContext systemContext) {
        Instrumentation inst = config.getInstrumentation();
        if (inst != null) {
            // Register a wrapper so we can trace callers.
            inst = new TraceInstrumentation(inst);
            Hashtable<String, String> svcProps = new Hashtable<String, String>();
            systemContext.registerService(Instrumentation.class.getName(), inst, svcProps);
        }
    }

    /**
     * Register the PauseableComponentController class as a service in the OSGi registry
     *
     * @param systemBundleCtx
     *            The framework system bundle context
     */
    protected void registerPauseableComponentController(BundleContext systemContext) {
        PauseableComponentControllerImpl pauseableComponentController = new PauseableComponentControllerImpl(systemContext);
        if (pauseableComponentController != null) {
            Hashtable<String, String> svcProps = new Hashtable<String, String>();
            systemContext.registerService(PauseableComponentController.class.getName(), pauseableComponentController, svcProps);
        }
    }

    /**
     * Delayed registration of the platform MBeanServerPipeline in the OSGi registry.
     *
     * @param systemContext The framework system bundle context
     */
    private void preRegisterMBeanServerPipelineService(final BundleContext systemContext) {
        PlatformMBeanServerBuilder.addPlatformMBeanServerBuilderListener(new PlatformMBeanServerBuilderListener() {
            @Override
            @FFDCIgnore(IllegalStateException.class)
            public void platformMBeanServerCreated(final MBeanServerPipeline pipeline) {
                if (pipeline != null) {
                    final Hashtable<String, String> svcProps = new Hashtable<String, String>();
                    try {
                        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                            @Override
                            public Void run() {
                                systemContext.registerService(MBeanServerPipeline.class.getName(), pipeline, svcProps);
                                return null;
                            }
                        });
                    } catch (IllegalStateException ise) { /* This instance of the system bundle is no longer valid. Ignore it. */
                    }
                }
            }
        });
    }

    /**
     * Create and start a new instance of an OSGi framework using the provided
     * properties as framework properties.
     */
    protected Framework startFramework(BootstrapConfig config) throws BundleException {
        // Set the default startlevel of the framework. We want the framework to
        // start at our bootstrap level (i.e. Framework bundle itself will start, and
        // it will pre-load and re-start any previously known bundles in the
        // bootstrap start level).
        config.put(org.osgi.framework.Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
                   Integer.toString(KernelStartLevel.OSGI_INIT.getLevel()));

        fwkClassloader = config.getFrameworkClassloader();

        FrameworkFactory fwkFactory = FrameworkConfigurator.getFrameworkFactory(fwkClassloader);
        // Initialize the framework to create a valid system bundle context
        // Start the shutdown monitor (before we start any bundles)
        // This exception will have a translated message stating that an unknown exception occurred.
        // This is so bizarre a case that it should never happen.
        try {
            Framework fwk = fwkFactory.newFramework(config.getFrameworkProperties());
            if (fwk == null)
                return null;
            fwk.start();
            return fwk;
        } catch (BundleException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // Try to diagnose this exception. If it's something we know about, we will log an error and
            // return null here (which will result in a general "Failed to start the framework" error message
            // higher up.) Otherwise, just throw the exception
            if (!handleEquinoxRuntimeException(ex))
                throw ex;
            return null;
        }
    }

    private static final String MANAGER_DIR_NAME = ".manager";
    private static final String OSGI_DIR_NAME = "org.eclipse.osgi";

    /**
     * Attempt to diagnose Equinox exceptions and issue a sane error message rather than a massive equinox stack
     *
     * @param ex The RuntimeException thrown by Equinox
     * @return true if we understand the exception and issued an error message, false otherwise
     */
    private boolean handleEquinoxRuntimeException(RuntimeException ex) {

        Throwable cause = ex.getCause();
        if (cause != null) {
            if (cause instanceof IOException) {
                // Check common causes for IOExceptions
                File osgiDir = config.getWorkareaFile(OSGI_DIR_NAME);
                if (!osgiDir.exists() || !osgiDir.isDirectory() || !osgiDir.canWrite()) {
                    Tr.error(tc, "error.serverDirPermission", osgiDir.getAbsolutePath());
                    return true;
                }
                File managerDir = new File(osgiDir, MANAGER_DIR_NAME);
                if (!managerDir.exists() || !managerDir.isDirectory() || !managerDir.canWrite()) {
                    Tr.error(tc, "error.serverDirPermission", managerDir.getAbsolutePath());
                    return true;
                }

            }

        }
        return false;
    }

    private Framework waitForFramework() throws InterruptedException {
        // wait for the framework to start
        frameworkLatch.await();
        return framework;
    }

    /**
     * Waits for the server to become ready.
     *
     * @return true if the server is ready, or false if an error occurred
     * @throws InterruptedException
     */
    public boolean waitForReady() throws InterruptedException {
        // wait for the framework to be set.
        if (waitForFramework() == null) {
            return false;
        }

        frameworkLaunched.await();
        if (!frameworkLaunchSuccess) {
            return false;
        }

        // Now look for the FrameworkReady service in the service registry
        Collection<ServiceReference<FrameworkReady>> readyServiceRefs;
        try {
            readyServiceRefs = systemBundleCtx.getServiceReferences(FrameworkReady.class, null);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e); // unlikely.
        } catch (IllegalStateException ex) {
            // The framework might have been stopped before we finished starting
            if (framework.getState() != Bundle.ACTIVE) {
                waitForFrameworkStop();
                return false;
            } else {
                throw ex;
            }
        }

        // If we have any, we will wait for them...
        if (readyServiceRefs != null) {
            for (ServiceReference<FrameworkReady> readyServiceRef : readyServiceRefs) {
                FrameworkReady ready = systemBundleCtx.getService(readyServiceRef);
                if (ready != null) {
                    ready.waitForFrameworkReady();
                }
            }
        }
        // Check if some component declared a fatal start error by initiating
        // framework stop before we were fully started.  If so, wait for the
        // framework to finish stopping, then report an error.
        if (framework.getState() != Bundle.ACTIVE) {
            waitForFrameworkStop();
            return false;
        }

        return true;
    }

    /**
     * Stop as relayed from the server command utility
     *
     * @param force true if shutdown was invoked with --force
     */
    void shutdownCommand(boolean force) {
        if (force) {
            systemBundleCtx.registerService(ForcedServerStop.class, new ForcedServerStop(), null);
        }
        shutdownFramework();
    }

    /**
     * Shutdown the framework. Shutdown is asynchronous so this
     * method will return after initiating shutdown.
     *
     * @see {@link #waitForShutdown}
     */
    public void shutdownFramework() {
        try {
            if (waitForFramework() != null) {
                stopFramework();
            }
        } catch (InterruptedException e) {
        }
    }

    /**
     * Wait for the framework to shutdown.
     */
    public void waitForShutdown() throws InterruptedException {
        frameworkShutdownLatch.await();
    }

    /**
     * Stops the framework, ignoring exceptions.
     */
    @FFDCIgnore(Exception.class)
    protected void stopFramework() {
        try {
            framework.stop();
        } catch (Exception e) {
            // do nothing.
        }
    }

    @FFDCIgnore(InterruptedException.class)
    protected void waitForFrameworkStop() {
        try {
            FrameworkEvent rc;
            do {
                rc = framework.waitForStop(0);
            } while (rc.getType() == FrameworkEvent.STOPPED_UPDATE);
        } catch (InterruptedException e) {
            // do nothing.
        }
    }

    /**
     * Attach a shutdown hook to make sure that the framework is shutdown nicely
     * in most cases. There are ways of shutting a JVM down that don't use the
     * shutdown hook, but this catches most cases.
     */
    private void addShutdownHook(boolean isClient) {
        if (shutdownHook == null) {
            shutdownHook = new ShutdownHook(isClient);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    /**
     * Remove the shutdown hook previously added by {@link #addShutdownHook}.
     */
    @FFDCIgnore(IllegalStateException.class)
    private void removeShutdownHook() {
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // do nothing.
            }
        }
    }

    /**
     * Create a server command listener with the given config and uuid.
     * Made a method to allow test to avoid/swap out the listen() behavior
     */
    protected void startServerCommandListener() {
        String uuid = systemBundleCtx.getProperty("org.osgi.framework.uuid");
        sc = new ServerCommandListener(config, uuid, this);
        sc.startListening();
    }

    protected class ShutdownHook extends Thread {
        private final boolean isClient;

        ShutdownHook(boolean isClient) {
            super("WS-ShutdownHook");
            setDaemon(false);
            this.isClient = isClient;
        }

        /**
         * When the shutdown hook is run (at JVM-shutdown time, in
         * arbitrary/unknown order with other shutdown hooks), if the framework
         * var hasn't been cleared, we want to stop the framework, and then wait
         * for the stop operation to complete.
         */
        @Override
        public void run() {
            issueMessage();
            stopFramework();

            try {
                if (isClient) {
                    // the client main runs on the main thread - which also executes the shutdown hook
                    // so we have to wait for the framework to end here, rather than waiting for the
                    // framework to end in the launchFramework method.
                    framework.waitForStop(0);
                } else {
                    waitForShutdown();
                }
            } catch (InterruptedException ex) {
            }
        }

        private void issueMessage() {
            ThreadGroup systemGroup = getSystemThreadGroup();
            // This is inherently racy, but give space for extra threads to start.
            Thread[] threads = new Thread[systemGroup.activeCount() * 2];
            int numThreads = systemGroup.enumerate(threads);

            for (int i = 0; i < numThreads; i++) {
                Thread thread = threads[i];
                StackTraceElement[] stack = thread.getStackTrace();

                // System.exit usually calls Runtime.exit, so check it first.
                if (issueMessageForStackMethod(thread, stack, "java.lang.System", "exit") ||
                    issueMessageForStackMethod(thread, stack, "java.lang.Runtime", "exit")) {
                    return;
                }
            }

            Tr.audit(tc, "audit.jvm.shutdown", config.getProcessName());
        }

        private ThreadGroup getSystemThreadGroup() {
            ThreadGroup group = Thread.currentThread().getThreadGroup();
            for (ThreadGroup parent;; group = parent) {
                parent = group.getParent();
                if (parent == null) {
                    return group;
                }
            }
        }

        private boolean issueMessageForStackMethod(Thread thread, StackTraceElement[] stack, String className, String methodName) {
            for (int i = 0; i < stack.length; i++) {
                StackTraceElement element = stack[i];
                if (element.getClassName().equals(className) && element.getMethodName().equals(methodName)) {
                    // Use a Throwable for RAS stack truncation.  Skip frames
                    // leading up to the relevant call since they only show the
                    // thread blocked on monitors in java.lang.* internals.
                    StackTraceElement[] callerStack = new StackTraceElement[stack.length - i];
                    System.arraycopy(stack, i, callerStack, 0, stack.length - i);
                    Throwable callerStackFormattable = new CallerStack(callerStack);

                    Tr.audit(tc, "audit.system.exit",
                             config.getProcessName(),
                             thread.getName(),
                             DataFormatHelper.getThreadId(thread),
                             className + '.' + methodName,
                             callerStackFormattable);
                    return true;
                }
            }

            return false;
        }

        @SuppressWarnings("serial")
        private class CallerStack extends Throwable {
            CallerStack(StackTraceElement[] stack) {
                setStackTrace(stack);
            }

            @Override
            public String toString() {
                // Remove the "java.lang.Throwable" string from the message.
                return "";
            }

            @Override
            public Throwable fillInStackTrace() {
                // We replace the stack in the constructor, so avoid the
                // overhead of super.fillInStackTrace().
                return this;
            }
        }
    }

    /**
     * Format the elapsed time: from JVM launch to current time.
     * Current behavior:
     * 1- If factor is true, elapsed time will be from the time the server is launched to current time
     * in detailed units, else if false, elapsed time will be in seconds.
     * 2- If two parameters were passed, then requester is a testing class and the second parameter specifies
     * the elapsed time, in milliseconds, to format
     *
     * @param factor
     *            If true, the elapsed time will be factored into more detailed
     *            units: days/hours/minutes/seconds
     *            The decimal format of the seconds is #.### or #.## or #.# or # or 0
     *            If false it will be returned as the total of seconds
     *            The decimal format of the seconds is #.### or #.## or #.# or # or 0
     *
     * @return A String containing the formatted elapsed time.
     *         Examples when the English language 'en' is the 'Locale':
     *         23 days, 5 hours, 33 minutes, 28.124 seconds
     *         23 days, 33 minutes, 28.124 seconds
     *         5 hours, 28.124 seconds
     *         28.1 seconds
     */
    protected String getElapsedTime(boolean factor, long... testMilliseconds) {
        long elapsedTime;
        String info_days = BootstrapConstants.messages.getString("info.days");
        String info_hours = BootstrapConstants.messages.getString("info.hours");
        String info_minutes = BootstrapConstants.messages.getString("info.minutes");
        String info_seconds = BootstrapConstants.messages.getString("info.seconds");

        if (testMilliseconds.length == 0) {
            // Grab elapsed time in millis
            elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        } else {
            // This mean this function is used in a test case where the value of elapsed time is
            // being assigned by the testing class
            elapsedTime = testMilliseconds[0];
        }
        StringBuilder timeString = new StringBuilder(30);
        if (elapsedTime <= 0) {
            long scndsN = 0L;
            timeString.append(MessageFormat.format(info_seconds, scndsN));
            return timeString.toString();
        }

        final double secMillis = 1000.0;
        final long minuteMillis = 60 * (long) secMillis;
        final long hourMillis = 60 * minuteMillis;
        final long dayMillis = 24 * hourMillis;
        long mod = elapsedTime;

        if (factor) {
            // Issue 01: Hard coding the "," or "." result in bad looking translated messages
            // Example in pl language: CWWKE0036I: Serwer defaultServer zosta? zatrzymany po 4 dn., 5 godz., 50 min., 34,231 sek..
            // we see 2 issues: 1- Some languages might not recognize the ","
            //                  2- When having to write an abbreviation at the end of the statement you will end up with 2 dots
            // Issue 02: Introducing the unit Milliseconds is a suggested solution to get more accurate numbers. Currently we are
            // losing some accuracy when we convert between 'Long' & 'Double'.
            long days = mod / dayMillis;
            mod = mod % dayMillis;
            if (days > 0)
                timeString.append(MessageFormat.format(info_days, days)).append(", ");
            long hours = mod / hourMillis;
            mod = mod % hourMillis;
            if (hours > 0)
                timeString.append(MessageFormat.format(info_hours, hours)).append(", ");
            long minutes = mod / minuteMillis;
            mod = mod % minuteMillis;
            if (minutes > 0)
                timeString.append(MessageFormat.format(info_minutes, minutes)).append(", ");
            double seconds = mod / secMillis;
            mod = mod % (long) secMillis;
            if (mod == 0)
                timeString.append(MessageFormat.format(info_seconds, String.format("%,.0f", seconds)));
            else if (Long.toString(mod).endsWith("00"))
                timeString.append(MessageFormat.format(info_seconds, String.format("%,.1f", seconds)));
            else if (Long.toString(mod).endsWith("0"))
                timeString.append(MessageFormat.format(info_seconds, String.format("%,.2f", seconds)));
            else
                timeString.append(MessageFormat.format(info_seconds, String.format("%,.3f", seconds)));
            return timeString.toString();
        } else {
            double seconds = elapsedTime / secMillis;
            mod = elapsedTime % (long) secMillis;
            // mod is not correct for really large numbers so just drop the fraction
            if (seconds >= 0xFFFFFFFFL)
                timeString.append(MessageFormat.format(info_seconds, String.format("%,.0f", seconds)));
            else if (mod == 0)
                timeString.append(MessageFormat.format(info_seconds, String.format("%,.0f", seconds)));
            else if (Long.toString(mod).endsWith("00"))
                timeString.append(MessageFormat.format(info_seconds, String.format("%,.1f", seconds)));
            else if (Long.toString(mod).endsWith("0"))
                timeString.append(MessageFormat.format(info_seconds, String.format("%,.2f", seconds)));
            else
                timeString.append(MessageFormat.format(info_seconds, String.format("%,.3f", seconds)));
            return timeString.toString();
        }

    }

    /**
     * Query feature information
     *
     * @param osRequest contains any os filtering request information.
     * @return A set containing the absolute paths for all files comprising the currently configured features
     */
    public Set<String> queryFeatureInformation(String osRequest) throws IOException {
        Set<String> result = null;
        try {
            //if you change the 'null' arg, don't forget to implement the exception handler block!
            ServiceReference<?>[] refs = this.systemBundleCtx.getAllServiceReferences(ServerContent.class.getName(), null);
            if (refs != null && refs.length > 0) {
                result = new HashSet<String>();
                //if we somehow had more than one eligible service, we will use them all.
                //result will then be the aggregate of all data.
                for (ServiceReference<?> sr : refs) {
                    ServerContent sc = (ServerContent) this.systemBundleCtx.getService(sr);
                    String infos[] = sc.getServerContentPaths(osRequest);
                    for (String info : infos) {
                        result.add(info);
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            //tough for the filter 'null' to be invalid..
        }

        //sort the list alpha.. just makes it neater to debug missing files..
        ArrayList<String> sorted = new ArrayList<String>();
        if (result != null)
            sorted.addAll(result);
        Collections.sort(sorted);
        result = new LinkedHashSet<String>(); //linked set will preserve order

        //maybe addAll will work here, it's not clear, the intent is to preserve the sorted order.
        for (String s : sorted) {
            result.add(s);
        }
        return result;
    }

    /**
     * Query feature information
     *
     * @param osRequest contains any os filtering request information.
     * @return A set containing the absolute paths for all files comprising the currently configured features
     */
    public Set<String> queryFeatureNames() {
        Set<String> result = null;
        try {
            //if you change the 'null' arg, don't forget to implement the exception handler block!
            ServiceReference<?>[] refs = this.systemBundleCtx.getAllServiceReferences(ServerFeatures.class.getName(), null);
            if (refs != null && refs.length > 0) {
                result = new HashSet<String>();
                //if we somehow had more than one eligible service, we will use them all.
                //result will then be the aggregate of all data.
                for (ServiceReference<?> sr : refs) {
                    ServerFeatures sf = (ServerFeatures) this.systemBundleCtx.getService(sr);
                    String infos[] = sf.getServerFeatureNames();
                    for (String info : infos) {
                        result.add(info);
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            //tough for the filter 'null' to be invalid..
        }

        //sort the list alpha.. just makes it neater to debug missing files..
        ArrayList<String> sorted = new ArrayList<String>();
        if (result != null)
            sorted.addAll(result);
        Collections.sort(sorted);
        result = new LinkedHashSet<String>(); //linked set will preserve order

        //maybe addAll will work here, it's not clear, the intent is to preserve the sorted order.
        for (String s : sorted) {
            result.add(s);
        }
        return result;
    }

    /**
     * Introspect the framework
     * Get all IntrospectableService from OSGi bundle context, and dump a running
     * server status from them.
     *
     * @param timestamp
     *            Create a unique dump folder based on the time stamp string.
     * @param javaDumpActions
     *            The java dumps to create, or null for the default set.
     */
    public void introspectFramework(String timestamp, Set<JavaDumpAction> javaDumpActions) {
        Tr.audit(tc, "info.introspect.request.received");

        File dumpDir = config.getOutputFile(BootstrapConstants.SERVER_DUMP_FOLDER_PREFIX + timestamp + "/");
        if (!dumpDir.exists()) {
            throw new IllegalStateException("dump directory does not exist.");
        }

        // generate java dumps if needed, and move them to the dump directory.
        if (javaDumpActions != null) {
            File javaDumpLocations = new File(dumpDir, BootstrapConstants.SERVER_DUMPED_FILE_LOCATIONS);
            dumpJava(javaDumpActions, javaDumpLocations);
        }

        IntrospectionContext introspectionCtx = new IntrospectionContext(systemBundleCtx, dumpDir);
        introspectionCtx.introspectAll();

        // create dumped flag file
        File dumpedFlag = new File(dumpDir, BootstrapConstants.SERVER_DUMPED_FLAG_FILE_NAME);
        try {
            dumpedFlag.createNewFile();
        } catch (IOException e) {
            Tr.warning(tc, "warn.unableWriteFile", dumpedFlag, e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private static class IntrospectionContext {
        private final BundleContext systemBundleCtx;
        private final File dumpDir;
        private int unnamedCount;

        IntrospectionContext(BundleContext systemBundleCtx, File dumpDir) {
            this.systemBundleCtx = systemBundleCtx;
            this.dumpDir = dumpDir;
        }

        public void introspectAll() {
            // create introspection dir in the dump dir which was created in the server's output directory
            File introspectionDir = new File(dumpDir, BootstrapConstants.SERVER_INTROSPECTION_FOLDER_NAME);
            if (!FileUtils.createDir(introspectionDir)) {
                throw new IllegalStateException("introspections directory could not be created.");
            }

            try {
                introspectIntrospectors(introspectionDir);
                introspectIntrospectableServices(introspectionDir);
            } catch (InvalidSyntaxException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception occured when get IntrospectableService refs: {0}", e);
                }
            }
        }

        private void introspectIntrospectors(File introspectionDir) throws InvalidSyntaxException {
            Collection<ServiceReference<Introspector>> refs = this.systemBundleCtx.getServiceReferences(Introspector.class, null);
            if (refs != null && !refs.isEmpty()) {
                for (ServiceReference<Introspector> ref : refs) {
                    Introspector introspector = this.systemBundleCtx.getService(ref);
                    if (introspector != null) {
                        try {
                            String name = introspector.getIntrospectorName();
                            String desc = introspector.getIntrospectorDescription();
                            introspect(introspectionDir, name, desc, introspector, null);
                        } finally {
                            this.systemBundleCtx.ungetService(ref);
                        }
                    }
                }
            }
        }

        private void introspectIntrospectableServices(File introspectionDir) throws InvalidSyntaxException {
            Collection<ServiceReference<com.ibm.wsspi.logging.IntrospectableService>> legacyRefs = systemBundleCtx.getServiceReferences(com.ibm.wsspi.logging.IntrospectableService.class,
                                                                                                                                        null);
            if (legacyRefs != null && !legacyRefs.isEmpty()) {
                for (ServiceReference<com.ibm.wsspi.logging.IntrospectableService> ref : legacyRefs) {
                    com.ibm.wsspi.logging.IntrospectableService serv = systemBundleCtx.getService(ref);
                    if (serv != null) {
                        try {
                            String name = serv.getName();
                            String desc = serv.getDescription();
                            introspect(introspectionDir, name, desc, null, serv);
                        } finally {
                            systemBundleCtx.ungetService(ref);
                        }
                    }
                }
            }
        }

        private void introspect(File introspectionDir,
                                String introspectionName,
                                String introspectionDesc,
                                Introspector introspector,
                                com.ibm.wsspi.logging.IntrospectableService introspectable) {
            if (introspectionName == null || introspectionName.isEmpty()) {
                introspectionName = Introspector.class.getSimpleName() + '.' + unnamedCount++;
            }

            File introspectionFile = new File(introspectionDir, introspectionName + ".txt");

            OutputStream out = null;
            PrintWriter pw = null;
            try {
                out = new FileOutputStream(introspectionFile);
                pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));

                // write header
                if (introspectionDesc != null && !introspectionDesc.isEmpty()) {
                    pw.println("The description of this introspector:");
                    pw.println(introspectionDesc);
                    pw.println();
                    pw.flush();
                }

                // write body
                if (introspectable != null) {
                    introspectable.introspect(out);
                } else {
                    introspector.introspect(pw);
                }
            } catch (FileNotFoundException e) {
                e.getCause(); // findbugs
                Tr.error(tc, "error.fileNotFound", introspectionFile);
            } catch (Throwable t) {
                Tr.warning(tc, "warn.unableWriteFile", introspectionFile, t.getMessage());
                if (out != null) {
                    t.printStackTrace(pw);
                }
            } finally {
                Utils.tryToClose(pw);
                Utils.tryToClose(out);
            }
        }
    }

    public void dumpJava(Set<JavaDumpAction> javaDumpActions) {
        Tr.audit(tc, "info.javadump.request.received");

        if (javaDumpActions == null) {
            javaDumpActions = new LinkedHashSet<JavaDumpAction>();
            javaDumpActions.add(JavaDumpAction.THREAD);
        }

        File serverOutputDir = config.getOutputFile(null);
        File dumpedFlag = new File(serverOutputDir, BootstrapConstants.SERVER_DUMPED_FLAG_FILE_NAME);

        dumpJava(javaDumpActions, dumpedFlag);
    }

    private void dumpJava(Set<JavaDumpAction> javaDumpActions, File statusFile) {

        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(statusFile), "UTF-8");
            for (JavaDumpAction javaDumpAction : javaDumpActions) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Start javadump action " + javaDumpAction);
                }
                String dumpFileName;
                try {
                    File dumpFile = JavaDumper.getInstance().dump(javaDumpAction, config.getOutputFile(null));
                    if (dumpFile == null) {
                        if (ServerDumpUtil.isZos() && (JavaDumpAction.SYSTEM == javaDumpAction)) {
                            // We get null returned because the dump goes to a dataset
                            Tr.audit(tc, "info.javadump.zos.system.created");
                            dumpFileName = "";
                        } else {
                            Tr.warning(tc, "warn.javadump.unsupported", javaDumpAction.displayName());
                            dumpFileName = "";
                        }
                    } else {
                        Tr.audit(tc, "info.javadump.created", dumpFile.getAbsolutePath());
                        dumpFileName = dumpFile.getAbsolutePath();
                    }
                } catch (RuntimeException e) {
                    dumpFileName = "ERROR: " + e.toString();
                }

                writer.write(javaDumpAction.name());
                writer.write('=');
                writer.write(dumpFileName);
                writer.write('\n');
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "End javadump action " + javaDumpAction + ", fileName = " + dumpFileName);
                }
            }
        } catch (IOException e) {
            Tr.warning(tc, "warn.unableWriteFile", statusFile, e.getMessage());
            Utils.tryToClose(writer);
            if (!statusFile.delete()) {
                // Avoid FindBugs warning.  We delete as a best effort.
                return;
            }
        } finally {
            Utils.tryToClose(writer);
        }
    }

    /**
     * Create the ThreadIdentityManagerConfigurator (a ServiceTracker) to track
     * ThreadIdentityService references and inject them into ThreadIdentityManager
     * (which sits on the non-OSGI side of logging).
     */
    private void openThreadIdentityTracker(BundleContext systemContext) {
        threadIdentityManagerTracker = new ThreadIdentityManagerConfigurator(systemContext);
        threadIdentityManagerTracker.open();
    }

    @FFDCIgnore(PauseableComponentControllerRequestFailedException.class)
    public ReturnCode pauseListeners(String args) {

        // A registered service for delivering pause/resume requests to interested Components.
        PauseableComponentController pauseableComponentController;

        ServiceReference<PauseableComponentController> reference = systemBundleCtx.getServiceReference(PauseableComponentController.class);
        pauseableComponentController = reference == null ? null : systemBundleCtx.getService(reference);
        if (pauseableComponentController == null) {
            Tr.error(tc, "error.pause.request.failed");
            return ReturnCode.ERROR_SERVER_PAUSE;
        }

        try {
            if (args == null) {

                pauseableComponentController.pause();

            } else {
                String targetList = args.substring(args.indexOf("=") + 1);
                pauseableComponentController.pause(targetList);
            }

        } catch (PauseableComponentControllerRequestFailedException e) {
            // The pause request did not complete successfully.

            return ReturnCode.ERROR_SERVER_PAUSE;
        }

        return ReturnCode.OK;
    }

    @FFDCIgnore(PauseableComponentControllerRequestFailedException.class)
    public ReturnCode resumeListeners(String args) {

        // A registered service for delivering pause/resume requests to interested Components.
        PauseableComponentController pauseableComponentController;

        ServiceReference<PauseableComponentController> reference = systemBundleCtx.getServiceReference(PauseableComponentController.class);
        pauseableComponentController = reference == null ? null : systemBundleCtx.getService(reference);
        if (pauseableComponentController == null) {
            Tr.warning(tc, "error.resume.request.failed");
            return ReturnCode.ERROR_SERVER_RESUME;
        }

        try {
            if (args == null) {
                pauseableComponentController.resume();

            } else {
                String targetList = args.substring(args.indexOf("=") + 1);
                pauseableComponentController.resume(targetList);
            }

        } catch (PauseableComponentControllerRequestFailedException e) {
            // Resume request did not complete successfully

            return ReturnCode.ERROR_SERVER_RESUME;
        }

        return ReturnCode.OK;
    }
}
