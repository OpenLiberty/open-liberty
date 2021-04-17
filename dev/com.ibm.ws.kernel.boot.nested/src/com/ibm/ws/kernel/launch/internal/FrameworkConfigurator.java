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
package com.ibm.ws.kernel.launch.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.launch.FrameworkFactory;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.KernelUtils;

/**
 * The framework configurator takes care of property customizations required to
 * launch a framework.
 * <p>
 * The framework factory is located according to spec, using the ServiceLoader
 * pattern: "META-INF/services/org.osgi.framework.launch.FrameworkFactory"
 */
public class FrameworkConfigurator {

    /**
     * Configure the OSGi framework: perform common property management tasks
     * like establishing the appropriate properties for the osgi remote console,
     * and call the abstract {@link #customize(BootstrapConfig)} method, to
     * allow sub-classes to further massage framework initialization properties.
     *
     * @param config
     *            BootstrapConfig object containing the active set of properties
     *            that will be used to initialize the framework.
     */
    public static void configure(BootstrapConfig config) {
        extraBootDelegationPackages(config);

        customize(config);
    }

    /**
     * Add equinox-specific properties to the framework initialization
     * properties:
     * <ul>
     * <li>org.osgi.framework.storage -- osgi standard property, set to the
     * workarea absolute path. Equinox creates a subdirectory in this workarea
     * for it's cache: org.eclipse.osgi
     * <li>osgi.configuration.area -- set to the workarea absolute path
     * <li>osgi.instance.area -- set to the workarea absolute path
     * <li>osgi.user.area -- set to the server directory (parent of workarea)
     * <li>osgi.framework.activeThreadType -- none; prevent equinox from
     * spawning a thread to prevent framework from stopping
     * <li>osgi.checkConfiguration -- true; ensure equinox checks for updates to
     * referenced jars
     * <li>osgi.compatibility.eagerStart.LazyActivation -- false; do not
     * auto-start bundles with Bundle-ActivationPolicy: lazy
     * </ul>
     */
    protected static void customize(BootstrapConfig config) {
        File workarea = config.getWorkareaFile(null);

        // Enable logic in aries jndi-core to reset static JNDI NamingManager fields
        // when the liberty jndi feature is activated/deactivated.
        config.putIfAbsent("org.apache.aries.jndi.force.builder", "true");

        // Specify the storage area that this framework should use:
        // because information about installed/started bundles is contained in
        // that storage area, we use space specific to the server.
        config.put(org.osgi.framework.Constants.FRAMEWORK_STORAGE, workarea.getAbsolutePath());

        config.put("osgi.logfile", new File(workarea, "equinox.log").getAbsolutePath());

        // Use an equinox property to identify the location of the equinox
        // user area (we'll treat the server directory as the user area)
        config.put("osgi.user.area", workarea.getParent());

        // Use an equinox property to identify the location of the equinox
        // instance area.
        config.put("osgi.instance.area", workarea.getAbsolutePath());

        // Without this equinox property set, Equinox will create a non-daemon
        // thread to ensure that the framework doesn't just exit when it's idle.
        // Since we drive waitForStop with the main thread, we don't need
        // equinox to do that.
        config.put("osgi.framework.activeThreadType", "none");

        // Use an equinox property to ensure that equinox checks whether or not
        // the jar files we're installing (for file:// URLs) have changed if
        // osgi is not starting clean. If the bundle jar has changed, associated
        // cached data is tossed.
        config.putIfAbsent("osgi.checkConfiguration", "true");

        // We do not want Bundle-ActivationPolicy: lazy to cause bundle
        // resolution to automatically start bundles.
        config.put("osgi.compatibility.eagerStart.LazyActivation", "false");

        // We need to allow badly formed bundles to provide osgi.ee and osgi.native
        // capabilities in order to keep zero migration.
        config.put("osgi.equinox.allow.restricted.provides", "true");

        // It would be good if we could not set this, but it really craters the build image if we don't
        // this is because build image installs loads of bundles together that introduce enormous uses constraint issues.
        config.put("equinox.resolver.revision.batch.size", "1");

        // It is not clear that multi-threading really helps with performance of the resolver.
        config.putIfAbsent("equinox.resolver.thread.count", "1");

        // Disable the capture of log locations since we do not use location in logging of our entries
        // when tracing is enabled for the logservice
        config.putIfAbsent("equinox.log.capture.entry.location", "false");

        // By default use multiple threads for activating bundles from start-level
        // Set to the min of 4 or Runtime.getRuntime().availableProcessors().
        // Testing shows that going with more than 4 threads does not help
        // with startup performance of Liberty
        config.putIfAbsent("equinox.start.level.thread.count",
                           Integer.toString(Math.min(4, Runtime.getRuntime().availableProcessors())));
        config.putIfAbsent("equinox.start.level.restrict.parallel", "true");

        // default module.lock.timeout value in seconds.
        config.putIfAbsent("osgi.module.lock.timeout", "5");

        // The IBM shared class adapter issues a message if it's loaded on a
        // non-IBM JVM.  Since it does no harm, we'll suppress the message.
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                System.setProperty("ibm.cds.suppresserrors", "true");
                return null;
            }
        });

        // Want to bind to the port specified but set the address to be localhost
        // unless it is explicitly overridden.  To listen on all addresses, '*'
        // should be specified.
        String port = config.get("osgi.console");
        if (port != null && !"".equals(port)) {
            int colon = port.indexOf(':');
            if (colon == -1) {
                port = "localhost:" + port;
            } else if (port.startsWith("*:")) {
                port = port.substring(colon + 1);
            }
            config.put("osgi.console", port);
        }

        // We do not want unnecessary write checks to happen in the install area
        // This causes grief for SE Linux enforcement of read-only install areas (see RTC defect 93452)
        config.put("eclipse.home.location.readOnly", "true");

        // Disabling implicit boot delegation altogether.  This causes performance issues:
        // See RTC defect 95505.
        config.put("osgi.context.bootdelegation", "false");

        // Disable starting auto-start bundles during resolution.  This can cause deadlock if enabled
        config.put("osgi.module.auto.start.on.resolve", "false");

        // Disabling Equinox region MBean registration
        // See RTC defect 119657
        config.put("org.eclipse.equinox.region.register.mbeans", "false");

        // some blueprint configuration
        config.put("org.apache.aries.blueprint.use.system.context", "true");
        config.put("org.apache.aries.blueprint.parser.service.ignore.unknown.namespace.handlers", "true");

        config.put("ds.global.extender", "true");
        config.putIfAbsent("ds.cache.metadata", "true");
    }

    /**
     * Add {@code com.ibm.ws.boot.delegated.*} to the list of boot delegation
     * packages. Packages that meet that specification will only be loaded by
     * the boot class loader. The {@code java.lang.instrument.Instrumentation} hook
     * can be used to add a jar containing these classes at runtime.
     * <p>
     * The monitoring code depends on this delegation for proxy generation.
     * </p>
     *
     * @param config the framework properties
     */
    private static void extraBootDelegationPackages(BootstrapConfig config) {
        // adding org.apache.xml for defect 53421; hope to remove when strategic fix is available
        // adding org.apache.xerces for defect 94476
        final String defaultDelegation = "com.ibm.ws.kernel.boot.jmx.internal," +
                                         "sun.*,com.sun.*,com.ibm.lang.management,com.ibm.ws.boot.delegated.*," +
                                         "org.apache.xml.*,org.apache.xerces.*,com.ibm.xylem.*,com.ibm.xml.*," +
                                         "com.ibm.xtq.*,com.ibm.net.ssl.*,com.ibm.crypto.*,com.ibm.security.*,jdk.*";

        String osgiDelegationPackages = config.get(org.osgi.framework.Constants.FRAMEWORK_BOOTDELEGATION);
        if (osgiDelegationPackages == null)
            osgiDelegationPackages = defaultDelegation;
        else
            osgiDelegationPackages = defaultDelegation + "," + osgiDelegationPackages;

        config.put(org.osgi.framework.Constants.FRAMEWORK_BUNDLE_PARENT, "framework");
        config.put(org.osgi.framework.Constants.FRAMEWORK_BOOTDELEGATION, osgiDelegationPackages);

        // If the config does not contain osgi.resolver.preferSystemPackages, set to "false" by default
        config.putIfAbsent(BootstrapConstants.CONFIG_OSGI_PREFER_SYSTEM_PACKAGES, "false");
    }

    /**
     * Use the provided classloader to find the target FrameworkFactory via the
     * ServiceLoader pattern. If the
     * "META-INF/services/org.osgi.framework.launch.FrameworkFactory" resource
     * is found on the classpath, it is read for the first non-comment line
     * containing a classname. That factory is then used as the service class
     * for creating a FrameworkFactory instance.
     *
     * @return non-null instance of the framework factory.
     * @throws LaunchException
     *             if Factory can not be found or instantiated.
     * @see {@link KernelUtils#getServiceClass(BufferedReader)}
     */
    public static FrameworkFactory getFrameworkFactory(ClassLoader loader) {
        FrameworkFactory factory = null;
        Class<?> factoryClass = null;

        final String factoryResource = "META-INF/services/org.osgi.framework.launch.FrameworkFactory";

        java.io.InputStream inputstream = loader.getResourceAsStream(factoryResource);
        if (inputstream == null)
            throw new IllegalStateException("Could not find " + factoryResource + " on classpath.");

        String factoryClassName = null;

        BufferedReader bufferedreader;
        try {
            bufferedreader = new BufferedReader(new InputStreamReader(inputstream, StandardCharsets.UTF_8));

            factoryClassName = KernelUtils.getServiceClass(bufferedreader);
            bufferedreader.close();
        } catch (Exception e) {
            throw new IllegalStateException("Could not read FrameworkFactory service: " + factoryClassName + "; exception=" + e);
        }

        if (factoryClassName == null)
            throw new IllegalStateException("Could not find FrameworkFactory service: " + factoryResource);

        try {
            factoryClass = loader.loadClass(factoryClassName);

            Constructor<?> ctor = factoryClass.getConstructor();
            factory = (FrameworkFactory) ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Could not load/instantiate framework factory (" + factoryClassName + ")", e);
        } catch (Error e) {
            throw new IllegalStateException("Could not load/instantiate framework factory (" + factoryClassName + ")", e);
        }

        return factory;
    }
}
