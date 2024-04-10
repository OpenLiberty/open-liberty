/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.EmbeddedServerImpl;
import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.Launcher;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.security.WLPDynamicPolicy;
import com.ibm.ws.kernel.boot.utils.SequenceNumber;
import com.ibm.ws.kernel.internal.classloader.BootstrapChildFirstJarClassloader;
import com.ibm.ws.kernel.internal.classloader.BootstrapChildFirstURLClassloader;
import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;
import com.ibm.ws.kernel.provisioning.NameBasedLocalBundleRepository;
import com.ibm.ws.kernel.provisioning.ServiceFingerprint;
import com.ibm.ws.kernel.provisioning.VersionUtility;

import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Bootstrap the runtime: Resolve the jar files required by the framework classloader,
 * locate the launcher delegate using reflection, then invoke the launcher delegate
 * using the framework classloader.
 *
 * The bootstrap API is used by both the {@link Launcher} for normal command-line
 * invocation, and the {@link EmbeddedServerImpl} for embedded server launch.
 */
public class KernelBootstrap {
    /**
     * Standard constructor.
     *
     * @param bootProps The parameters of the launch.
     */
    public KernelBootstrap(BootstrapConfig bootProps) {
        this.bootProps = bootProps;

        this.libertyBoot = Boolean.parseBoolean(bootProps.get(BootstrapConstants.LIBERTY_BOOT_PROPERTY));

        // Use initialized bootstrap configuration to create the server lock.
        // This ensures the server and nested workarea directory exist and are writable.
        this.serverLock = ServerLock.createServerLock(bootProps);
    }

    /** Initial configuration and location manager. */
    protected final BootstrapConfig bootProps;

    /**
     * Control parameter: Is liberty boot mode enabled.
     * See {@link KernelResolver} for liberty boot mode details.
     */
    protected final boolean libertyBoot;

    /** Lock ensuring only one VM is using the server directory/workarea (as a server). */
    protected final ServerLock serverLock;

    //

    protected final CountDownLatch delegateCreated = new CountDownLatch(1);
    protected LauncherDelegate launcherDelegate;
    protected File serverRunning;

    /**
     * Start the kernel.
     *
     * <ul>
     * <li>establish appropriate mechanisms to mark the server as started,
     * <li>identify kernel resources and log providers
     * <li>create a special bootstrap classlaoder
     * <li>Create an appropriate LauncherDelegate
     * <li>Launch the OSGi framework via the LauncherDelegate
     * </ul>
     *
     * @return The launch return code. This method only ever returns
     *         {@link ReturnCode#OK}. Failed launches throw exceptions.
     */
    public ReturnCode go() {
        try {
            serverLock.obtainServerLock(); // This can fail with a timeout.

            clearServerStateDir();

            setupJMXOverride();
            setupHttpRetryPost();
            setupLogManager();

            // Create the server running marker file.  This is automatically
            // deleted when JVM terminates normally.
            // If marker file already exists, a clean start will be performed.
            ServerLock.createServerRunningMarkerFile(bootProps);

            cleanStart(); // Refresh the service fingerprint.
                          // Optionally, clear the workarea (perform a clean start).

            BootstrapManifest bootManifest;
            try {
                bootManifest = BootstrapManifest.readBootstrapManifest(libertyBoot);
            } catch (IOException e) {
                throw KernelUtils.launchException(e, "Failed to read bootstrap manifest", "error.unknown.kernel.version");
            }

            BootstrapDefaults bootDefaults;
            try {
                bootDefaults = new BootstrapDefaults(bootProps);
            } catch (IOException e) {
                throw KernelUtils.launchException(e, "Failed to read bootstrap defaults", "error.unknown.kernel.version");
            }

            bootManifest.prepSystemPackages(bootProps);

            String kernelVersion = BootstrapConstants.SERVER_NAME_PREFIX + bootManifest.getBundleVersion();
            String productInfo = getProductInfoDisplayName();

            KernelResolver resolver = newResolver(bootProps, bootDefaults, libertyBoot);

            // ISSUE LAUNCH FEEDBACK TO THE CONSOLE -- we've done the cursory validation at least.
            String logLevel = bootProps.get("com.ibm.ws.logging.console.log.level");
            boolean logVersionInfo = (logLevel == null || !logLevel.equalsIgnoreCase("off"));

            processVersion(bootProps, "info.serverLaunch", kernelVersion, productInfo, logVersionInfo);

            if (logVersionInfo) {
                logSerialFilterMessage();
                List<String> cmdArgs = bootProps.getCmdArgs();
                if ((cmdArgs != null) && !cmdArgs.isEmpty()) {
                    System.out.println("\t" + KernelUtils.format("info.cmdArgs", cmdArgs));
                }
            }

            // Now that we have a resolver, check if a clean start is being forced.
            // If we already cleaned once because of the cmd line, osgi property, or
            // service changes then the resolver won't force us to clean again.
            if (resolver.getForceCleanStart()) {
                KernelUtils.cleanStart(bootProps.getWorkareaFile(null));
            }

            ServiceFingerprint.putInstallDir(null, bootProps.getInstallRoot());

            String packages = bootProps.get(BootstrapConstants.INITPROP_OSGI_EXTRA_PACKAGE);
            packages = resolver.appendExtraSystemPackages(packages);
            if (packages != null) {
                bootProps.put(BootstrapConstants.INITPROP_OSGI_EXTRA_PACKAGE, packages);
            }

            // Obtain a launcher delegate.
            //
            // When liberty boot mode is enabled, the current class loader is
            // used.  Boot jar processing is needed only when boot mode is not
            // enabled, in which case the boot jars are used to configure the
            // launch delegate's class loader.

            List<URL> urlList = new ArrayList<URL>();

            if (!libertyBoot) {
                bootProps.addBootstrapJarURLs(urlList);
                resolver.addBootJars(urlList); // OSGi, log provider, os extension jars.
            }

            ClassLoader delegateLoader = buildClassLoader(urlList, bootProps.get("verifyJarSignature"));

            try {
                Class<? extends LauncherDelegate> launcherDelegateClass = getLauncherDelegateClass(delegateLoader);
                launcherDelegate = launcherDelegateClass.getConstructor(BootstrapConfig.class).newInstance(bootProps);
            } catch (Exception e) {
                throw KernelUtils.unwindException("Failed to create launcher delegate " + e, e);
            }

            delegateCreated.countDown();

            bootProps.setFrameworkClassloader(delegateLoader);
            bootProps.setKernelResolver(resolver);
            bootProps.setInstrumentation(getInstrumentation());

            if (!Boolean.parseBoolean(bootProps.get(BootstrapConstants.INTERNAL_START_SIMULATION))) {
                // GO!!! We won't come back from this call until the framework has stopped
                launcherDelegate.launchFramework();
            }

        } catch (LaunchException le) {
            throw le; // Already packaged correctly; rethrow.
        } catch (Throwable e) {
            throw KernelUtils.unwindException("Unexpected exception", e);

        } finally {
            delegateCreated.countDown();
            serverLock.releaseServerLock();
        }

        return ReturnCode.OK;
    }

    private static KernelResolver newResolver(BootstrapConfig bootProps,
                                              BootstrapDefaults bootDefaults,
                                              boolean libertyBoot) {

        return new KernelResolver(bootProps.getInstallRoot(), bootProps.getWorkareaFile(KernelResolver.CACHE_FILE), bootDefaults.getKernelDefinition(bootProps), bootDefaults.getLogProviderDefinition(bootProps), bootDefaults.getOSExtensionDefinition(bootProps), libertyBoot);
    }

    private void clearServerStateDir() {
        File stateDir = bootProps.getOutputFile("logs/state");
        KernelUtils.cleanDirectory(stateDir, "state");
    }

    public Set<String> getServerContent(String osRequest) throws FileNotFoundException, IOException, InterruptedException {
        delegateCreated.await();
        if (launcherDelegate != null)
            return launcherDelegate.queryFeatureInformation(osRequest);

        return Collections.emptySet();
    }

    public Set<String> getServerFeatures() throws InterruptedException {
        delegateCreated.await();
        if (launcherDelegate != null)
            return launcherDelegate.queryFeatureNames();

        return Collections.emptySet();
    }

    public boolean waitForStarted() throws InterruptedException {
        delegateCreated.await();
        if (launcherDelegate != null)
            return launcherDelegate.waitForReady();

        return false;
    }

    /**
     * Stop the server. This emulates the path that the server stop action takes:
     * it calls shutdown on the LauncherDelegate, and then waits until it can obtain
     * the server lock to ensure the server has stopped.
     */
    public ReturnCode shutdown() throws InterruptedException {
        return shutdown(!DO_FORCE);
    }

    public static final boolean DO_FORCE = true;

    /**
     * Force stop the server. This emulates the path that the server stop action takes:
     * it calls shutdown on the LauncherDelegate, and then waits until it can obtain
     * the server lock to ensure the server has stopped.
     */
    public ReturnCode shutdown(boolean force) throws InterruptedException {
        delegateCreated.await();

        if ((launcherDelegate != null) && launcherDelegate.shutdown(force)) {
            return serverLock.waitForStop();
        } else {
            return ReturnCode.OK;
        }
    }

    public static final String JMX_BUILDER_PROPERTY_NAME = "javax.management.builder.initial";
    public static final String JMX_BUILDER_PROPERTY_VALUE = "com.ibm.ws.kernel.boot.jmx.internal.PlatformMBeanServerBuilder";

    /**
     * Set the JMX builder property, {@link #JMX_BUILDER_PROPERTY_NAME}.
     * Always set {@link #JMX_BUILDER_PROPERTY_VALUE}.
     */
    protected void setupJMXOverride() {
        System.setProperty(JMX_BUILDER_PROPERTY_NAME, JMX_BUILDER_PROPERTY_VALUE);
    }

    public static final String LOG_MANAGER_PROPERTY_NAME = "java.util.logging.manager";
    public static final String LOG_MANAGER_DEFAULT = "com.ibm.ws.kernel.boot.logging.WsLogManager";

    /**
     * Set the logging manager system property, {@link #LOG_MANAGER_PROPERTY_NAME}.
     *
     * This is carried from the bootstrap properties, if possible.
     * A default of {@link #LOG_MANAGER_DEFAULT} is used if the bootstrap property
     * is not set.
     *
     * Usually, the log manager property is set by the java agent.
     */
    private void setupLogManager() {
        String logManager = bootProps.get(LOG_MANAGER_PROPERTY_NAME);
        if (logManager == null) {
            logManager = LOG_MANAGER_DEFAULT;
        }

        System.setProperty(LOG_MANAGER_PROPERTY_NAME, logManager);
    }

    public static final String HTTP_RETRY_PROPERTY_NAME = "sun.net.http.retryPost";
    public static final String HTTP_RETRY_DEFAULT = "false";

    /**
     * Set the HTTP retry system property, {@link #HTTP_RETRY_PROPERTY_NAME}.
     *
     * This is carried from the bootstrap properties, if possible.
     * A default of {@link #HTTP_RETRY_DEFAULT} is used if the bootstrap property
     * is not set.
     *
     * This system property must be set per java bug "JDK-6382788" :
     * "URLConnection is silently retrying POST request".
     */
    private void setupHttpRetryPost() {
        String httpRetryPost = bootProps.get(HTTP_RETRY_PROPERTY_NAME);
        if (httpRetryPost == null) {
            httpRetryPost = HTTP_RETRY_DEFAULT;
        }
        System.setProperty(HTTP_RETRY_PROPERTY_NAME, httpRetryPost);
    }

    /**
     * Conditionally perform clean start steps.
     *
     * Perform cleanup if either service has been applied, or
     * if a clean start has been requested through the bootstrap
     * properties.
     *
     * Cleanup means clearing the service fingerprint and emptying
     * the server workarea.
     *
     * Cleanup also removes the clean paramaters from the bootstrap
     * properties.
     */
    protected void cleanStart() {
        File workareaFile = bootProps.getWorkareaFile(null);

        // *ALWAYS* invoke 'hasServiceBeenApplied' first: The call has side
        // effects which must always occur.

        if (ServiceFingerprint.hasServiceBeenApplied(bootProps.getInstallRoot(), workareaFile) || bootProps.checkCleanStart()) {
            ServiceFingerprint.clear(); // Force the fingerprint to be recalculated.
            KernelUtils.cleanStart(workareaFile); // Clear the work directory.

            // Clean parameters which trigger a clean up.
            bootProps.remove(BootstrapConstants.INITPROP_OSGI_CLEAN);
            bootProps.remove(BootstrapConstants.OSGI_CLEAN);
        }
    }

    private static final String LAUNCHER_DELEGATE_CLASS_NAME = "com.ibm.ws.kernel.launch.internal.LauncherDelegateImpl";

    /**
     * Load the launcher delegate class, {@link #LAUNCHER_DELEGATE_CLASS_NAME}.
     *
     * This method must be retained, so that it can be overridden by test subclasses.
     *
     * @return The launcher delegate class.
     */
    protected Class<? extends LauncherDelegate> getLauncherDelegateClass(ClassLoader ldClassloader) throws ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Class<? extends LauncherDelegate> ldClass = (Class<? extends LauncherDelegate>) ldClassloader.loadClass(LAUNCHER_DELEGATE_CLASS_NAME);
        if (ldClass == null) {
            throw new ClassNotFoundException(LAUNCHER_DELEGATE_CLASS_NAME);
        }
        return ldClass;
    }

    /**
     * Obtain a class loader to be used to load the launcher delegate.
     *
     * When in liberty boot mode, use the class loader which loaded this class.
     *
     * Otherwise, construct a class loader using the supplied jar URLs.
     *
     * When verification is enabled, a failure to create the class loader
     * results in a thrown exception.
     *
     * @param jarUrls           The URLs of the class path of the class loader.
     * @param verifyJarProperty Property used to force verification of the JAR URLS.
     *
     * @return A class loader for the launcher delegate.
     */
    protected ClassLoader buildClassLoader(List<URL> jarUrls, String verifyJarProperty) {
        if (libertyBoot) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return getClass().getClassLoader();
                }
            });
        }

        // Verify the classloader JARS ...
        // Only if explicitly enabled when there is no security manager.
        // Always if there is a security manager.

        boolean verifyJar;
        if (System.getSecurityManager() == null) {
            verifyJar = "true".equalsIgnoreCase(verifyJarProperty);
        } else {
            verifyJar = true;
        }

        enableJava2SecurityIfSet(bootProps, jarUrls);

        ClassLoader loader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                ClassLoader parent = getClass().getClassLoader();
                URL[] urls = jarUrls.toArray(new URL[jarUrls.size()]);
                if (verifyJar) {
                    return new BootstrapChildFirstURLClassloader(urls, parent);
                } else {
                    try {
                        return new BootstrapChildFirstJarClassloader(urls, parent);
                    } catch (RuntimeException e) {
                        // fall back to URLClassLoader in case something went wrong
                        return new BootstrapChildFirstURLClassloader(urls, parent);
                    }
                }
            }

        });

        return loader;
    }

    public static void enableJava2SecurityIfSet(BootstrapConfig bootProps, List<URL> urlList) {
        if (bootProps.get(BootstrapConstants.JAVA_2_SECURITY_PROPERTY) != null) {

            NameBasedLocalBundleRepository repo = new NameBasedLocalBundleRepository(bootProps.getInstallRoot());
            File bestMatchFile = repo.selectBundle("com.ibm.ws.org.eclipse.equinox.region",
                                                   VersionUtility.stringToVersionRange("[1.0,1.0.100)"));
            if (bestMatchFile == null) {
                throw new LaunchException("Could not find bundle for " + "com.ibm.ws.org.eclipse.equinox.region"
                                          + ".", BootstrapConstants.messages.getString("error.missingBundleException"));
            } else {
                // Add to the list of boot jars...
                try {
                    urlList.add(bestMatchFile.toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new LaunchException("Failure to set the default Security Manager due to exception ", BootstrapConstants.messages.getString("error.set.securitymanager"), e);
                }
            }
            Policy wlpPolicy = new WLPDynamicPolicy(Policy.getPolicy(), urlList);
            Policy.setPolicy(wlpPolicy);
        }

    }

    /**
     * Get the runtime build version based on the information in the manifest of the
     * kernel jar: this is not authoritative, as it won't include information about
     * applied iFixes, etc.
     */
    public static void showVersion(BootstrapConfig bootProps) {
        BootstrapManifest bootManifest;
        try {
            bootManifest = BootstrapManifest.readBootstrapManifest(Boolean.parseBoolean(bootProps.get(BootstrapConstants.LIBERTY_BOOT_PROPERTY)));
            String kernelVersion = bootManifest.getBundleVersion();
            String productDisplayName = getProductInfoDisplayName();

            processVersion(bootProps, "info.serverVersion", kernelVersion, productDisplayName, true);
        } catch (IOException e) {
            throw new LaunchException("Could not read the jar manifest", BootstrapConstants.messages.getString("error.unknown.kernel.version"), e);
        }
    }

    private static void processVersion(BootstrapConfig bootProps, String msgKey, String kernelVersion, String productDisplayName, boolean printVersion) {
        // Two keys, mostly the same parameters (3rd is ignored in one case):
        // info.serverLaunch=Launching {3} ({0}) on {1}, version {2}
        // info.serverVersion={0} on {1}, version {2}
        // 0 : productInfo/wlp-version OR productInfo (wlp-version)
        // 1 : java.vm.name
        // 2 : java.runtime.version (maybe with locale)
        // 3 : serverName

        final String launchString;
        final String versionString;

        String bsConsoleFormat = bootProps.get("com.ibm.ws.logging.console.format");
        String envConsoleFormat = System.getenv("WLP_LOGGING_CONSOLE_FORMAT");

        //boostrap format should take precedence
        String consoleFormat = bsConsoleFormat != null ? bsConsoleFormat : envConsoleFormat;

        if (productDisplayName == null) {
            // RARE/CORNER-CASE: All bets are off, we don't have product info anyway... :(
            launchString = "WebSphere Application Server/" + kernelVersion;
            versionString = "WebSphere Application Server (" + kernelVersion + ")";
        } else {
            launchString = productDisplayName + "/" + kernelVersion;
            versionString = productDisplayName + " (" + kernelVersion + ")";
        }
        String consoleLogHeader = MessageFormat.format(BootstrapConstants.messages.getString(msgKey),
                                                       "info.serverLaunch".equals(msgKey) ? launchString : versionString,
                                                       System.getProperty("java.vm.name"),
                                                       System.getProperty("java.runtime.version") + " (" + Locale.getDefault() + ")",
                                                       bootProps.getProcessName());
        if (printVersion) {
            if ("json".equals(consoleFormat)) {
                String jsonConsoleHeader = constructJSONHeader(consoleLogHeader, bootProps);
                System.out.println(jsonConsoleHeader);
            } else {
                System.out.println(consoleLogHeader);
            }
            if (!CheckpointPhase.getPhase().restored()) {
                bootProps.put(BootstrapConstants.BOOTPROP_CONSOLE_LOG_HEADER, consoleLogHeader);
            }
        }

        displayWarningIfBeta(bootProps, consoleFormat);

        bootProps.put(BootstrapConstants.BOOTPROP_PRODUCT_INFO, versionString);
    }

    /**
     * Display a warning in the console.log for each product that is early access.
     *
     * These are determined from properties files in "lib/versions", using property
     * "com.ibm.websphere.productEdition=EARLY_ACCESS".
     */
    private static void displayWarningIfBeta(BootstrapConfig bootProps, String consoleFormat) {
        try {
            Map<String, ProductInfo> productInfos = ProductInfo.getAllProductInfo();
            for (ProductInfo info : productInfos.values()) {
                if (!info.isBeta()) {
                    continue;
                }
                String message = KernelUtils.format("warning.earlyRelease", info.getName());
                if ("json".equals(consoleFormat)) {
                    message = constructJSONHeader(message, bootProps);
                }
                System.out.println(message);
            }
        } catch (Exception e) {
            // FFDC
        }
    }

    private static String constructJSONHeader(String consoleLogHeader, BootstrapConfig bootProps) {
        //retrieve information for header
        String serverName = bootProps.get(BootstrapConstants.INTERNAL_SERVER_NAME);
        String wlpUserDir = System.getProperty("wlp.user.dir");
        String serverHostName = getServerHostName();
        String datetime = getDatetime();
        String sequenceNumber = getSequenceNumber();

        List<String> headerFieldNames = new ArrayList<>(Arrays.asList("type", "host", "ibm_userDir", "ibm_serverName", "message", "ibm_datetime", "ibm_sequence"));
        String[] headerFieldValues = { "liberty_message", serverHostName, wlpUserDir, serverName, consoleLogHeader, datetime, sequenceNumber };
        String OMIT_FIELDS_STRING = "@@@OMIT@@@";

        //bootstrap fieldMappings should take precedence
        String bsFieldMappings = bootProps.get("com.ibm.ws.logging.json.field.mappings");
        String envFieldMappings = System.getenv("WLP_LOGGING_JSON_FIELD_MAPPINGS");
        String fieldMappings = bsFieldMappings != null ? bsFieldMappings : envFieldMappings;

        if (fieldMappings != null && !fieldMappings.isEmpty() && fieldMappings != "") {
            String[] keyValuePairs = fieldMappings.split(",");
            for (String pair : keyValuePairs) {
                pair = pair.trim();
                if (pair.endsWith(":"))
                    pair = pair + OMIT_FIELDS_STRING;

                String[] entry = pair.split(":");
                entry[0] = entry[0].trim();

                if (entry.length == 2) {
                    entry[1] = entry[1].trim();
                    if (headerFieldNames.contains(entry[0]))
                        headerFieldNames.set(headerFieldNames.indexOf(entry[0]), entry[1]);

                } else if (entry.length == 3 && entry[0].equals("message")) {
                    entry[1] = entry[1].trim();
                    entry[2] = entry[2].trim();
                    if (headerFieldNames.contains(entry[1]))
                        headerFieldNames.set(headerFieldNames.indexOf(entry[1]), entry[2]);
                }
            }
        }

        StringBuilder jsonHeader = new StringBuilder("{");
        int currentValue = 0;
        Boolean isFirstField = true;

        for (String name : headerFieldNames) {
            if (!name.equals(OMIT_FIELDS_STRING)) {
                if (!isFirstField)
                    jsonHeader.append(",");
                jsonHeader.append("\"" + name + "\":\"");
                jsonHeader = jsonEscape(jsonHeader, headerFieldValues[currentValue]);
                jsonHeader.append("\"");
                isFirstField = false;
            }
            currentValue++;
        }

        return jsonHeader.append("}").toString();
    }

    /**
     * Answer a sequence number
     *
     * @return
     */
    private static String getSequenceNumber() {
        return SequenceNumber.formatSequenceNumber(System.currentTimeMillis(), 0);
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static String getDatetime() {
        return dateFormat.format(System.currentTimeMillis());
    }

    private static String getServerHostName() {
        String containerHost = System.getenv("CONTAINER_HOST");
        if ((containerHost != null) && !containerHost.isEmpty()) {
            return containerHost;
        }

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws UnknownHostException {
                    return InetAddress.getLocalHost().getCanonicalHostName();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Escape \b, \f, \n, \r, \t, ", \, / characters and appends to a string builder
     *
     * @param sb String builder to append to
     * @param s  String to escape
     *
     * @return The string builder.
     */
    private static StringBuilder jsonEscape(StringBuilder sb, String s) {
        if (s == null) {
            return sb.append(s);
        }

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;

                case '\\':
                case '\"':
                case '/':
                    sb.append("\\");
                    sb.append(c);
                    break;

                default:
                    sb.append(c);
            }
        }
        return sb;
    }

    /**
     * Return a display name for the currently running server.
     */
    protected static String getProductInfoDisplayName() {
        String result = null;
        try {
            Map<String, ProductInfo> products = ProductInfo.getAllProductInfo();
            StringBuilder builder = new StringBuilder();
            for (ProductInfo productInfo : products.values()) {
                ProductInfo replaced = productInfo.getReplacedBy();
                if (productInfo.getReplacedBy() == null || replaced.isReplacedProductLogged()) {
                    if (builder.length() != 0) {
                        builder.append(", ");
                    }
                    builder.append(productInfo.getDisplayName());
                }
            }
            result = builder.toString();
        } catch (ProductInfoParseException e) {
            // ignore exceptions-- best effort to get a pretty string
        } catch (DuplicateProductInfoException e) {
            // ignore exceptions-- best effort to get a pretty string
        } catch (ProductInfoReplaceException e) {
            // ignore exceptions-- best effort to get a pretty string
        }
        return result;
    }

    public static final String KERNEL_AGENT = "com.ibm.ws.kernel.instrument.BootstrapAgent";
    public static final String WLP_LIB_AGENT = "wlp.lib.extract.agent.BootstrapAgent";

    /**
     * Retrieve an instrumentation instance from the agent class.
     *
     * Try {@link #KERNEL_AGENT} then try {@link #WLP_LIB_AGENT}.
     *
     * Answer null if neither agent class is available, or neither has
     * an instrumentation instance.
     *
     * Locate the agent classes uses the system class loader.
     *
     * @return An instrumentation instance. Null may be returned.
     */
    protected Instrumentation getInstrumentation() {
        ClassLoader agentClassLoader = ClassLoader.getSystemClassLoader();
        Instrumentation instrumentation = findInstrumentation(agentClassLoader, KERNEL_AGENT);
        if (instrumentation == null) {
            instrumentation = findInstrumentation(agentClassLoader, WLP_LIB_AGENT);
        }
        return instrumentation;
    }

    /**
     * Attempt to retrieve instrumentation from an agent class. Load the agent
     * class using the supplied class loader.
     *
     * Invoke <code>getInstrumentation</code> on the agent class.
     *
     * Answer null if the agent class cannot be found.
     *
     * @param agentClassLoader The class loader to use to load the agent class.
     * @param agentClassName   The name of the agent class.
     *
     * @return The instrumentation from the agent class. Null if the agent
     *         class is not available or does not have an instrumentation instance.
     */
    private Instrumentation findInstrumentation(ClassLoader agentClassLoader, String agentClassName) {
        try {
            Class<?> agentClass = agentClassLoader.loadClass(agentClassName);
            Method getInstrumentation = agentClass.getMethod("getInstrumentation");
            return (Instrumentation) getInstrumentation.invoke(null);
        } catch (Exception t) {
            // Eat the issue and rely on users to report.
        }
        return null;
    }

    //

    private void logSerialFilterMessage() {
        if (isSerialFilterLoaded()) {
            System.out.println(BootstrapConstants.messages.getString("info.serialFilterLoaded"));
        }
    }

    private boolean isSerialFilterLoaded() {
        String activeSerialFilter = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("com.ibm.websphere.serialfilter.active");
            }
        });
        return ("true".equalsIgnoreCase(activeSerialFilter));
    }
}
