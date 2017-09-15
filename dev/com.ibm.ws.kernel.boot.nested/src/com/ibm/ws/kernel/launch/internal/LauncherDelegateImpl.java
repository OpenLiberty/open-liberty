/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.internal.LauncherDelegate;
import com.ibm.wsspi.logprovider.LogProvider;

/**
 * The outer {@link com.ibm.ws.kernel.boot.Launcher} class constructs a custom
 * classloader with the selected framework jar, this bootstrap jar, and
 * configured ras jars on the class path. It then invokes the {@link LauncherDelegateImpl#launchFramework(BootstrapConfig)} method on this
 * class (loaded via a child-first classloader) via reflection to continue
 * platform/framework initialization.
 * <p>
 * The BootstrapChildFirstClassLoader always loads from the child classloader
 * first, _unless_ the class to be loaded starts with com.ibm.ws.kernel.boot:
 * This allows classes in the com.ibm.ws.kernel.launch.* packages to reference the
 * already loaded/ initialized bootstrap classes while finalizing framework
 * initialization.
 */
public class LauncherDelegateImpl implements LauncherDelegate {
    private final BootstrapConfig config;
    private FrameworkManager manager = null;
    private final CountDownLatch managerLatch = new CountDownLatch(1);
    private final com.ibm.wsspi.logging.TextFileOutputStreamFactory fileStreamFactory;

    public LauncherDelegateImpl(BootstrapConfig config) {
        this.config = config;

        // This simple factory bridges the actual logging API that the rest of the OSGi 
        // system can see (via exported System-Packages) to the bootstrap API that
        // is not exported. Confine logging-related API used by the framework at large to
        // classes and SPI contained within the logging bundle (i.e. do not propagate
        // use of kernel.boot resources where we can)
        fileStreamFactory = new com.ibm.wsspi.logging.TextFileOutputStreamFactory() {
            @Override
            @Trivial
            public FileOutputStream createOutputStream(File file) throws IOException {
                return com.ibm.ws.kernel.boot.logging.TextFileOutputStreamFactory.createOutputStream(file);
            }

            @Override
            @Trivial
            public FileOutputStream createOutputStream(File file, boolean append) throws IOException {
                return com.ibm.ws.kernel.boot.logging.TextFileOutputStreamFactory.createOutputStream(file, append);
            }

            @Override
            @Trivial
            public FileOutputStream createOutputStream(String name) throws IOException {
                return com.ibm.ws.kernel.boot.logging.TextFileOutputStreamFactory.createOutputStream(name);
            }

            @Override
            @Trivial
            public FileOutputStream createOutputStream(String name, boolean append) throws IOException {
                return com.ibm.ws.kernel.boot.logging.TextFileOutputStreamFactory.createOutputStream(name, append);
            }

        };
    }

    /** {@inheritDoc} */
    @Override
    public void launchFramework() {
        ClassLoader loader = config.getFrameworkClassloader();
        if (loader == null)
            loader = this.getClass().getClassLoader();

        try {
            // initialize RAS
            LogProvider provider = getLogProviderImpl(loader, config);

            // get framework/platform manager
            manager = new FrameworkManager();
            managerLatch.countDown();

            // Update framework configuration
            FrameworkConfigurator.configure(config);

            doFrameworkLaunch(provider);
        } catch (LaunchException le) {
            throw le;
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            // In case an error occurs before launching.
            managerLatch.countDown();
        }
    }

    protected void doFrameworkLaunch(LogProvider provider) {
        manager.launchFramework(config, provider);
    }

    /** {@inheritDoc} */
    @Override
    public boolean waitForReady() throws InterruptedException {
        managerLatch.await();
        return manager == null ? false : manager.waitForReady();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws InterruptedException
     */
    @Override
    public boolean shutdown() throws InterruptedException {
        // Prevent shutdown before the server has properly started
        managerLatch.await();

        if (manager == null)
            return false;

        manager.shutdownFramework();
        manager.waitForShutdown();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> queryFeatureInformation(String osRequest) throws IOException {
        if (manager != null) {
            return manager.queryFeatureInformation(osRequest);
        } else {
            throw new IllegalStateException("Framework is not launched.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> queryFeatureNames() {
        if (manager != null) {
            return manager.queryFeatureNames();
        } else {
            throw new IllegalStateException("Framework is not launched.");
        }
    }

    /**
     * Initialize RAS/FFDC LogProviders
     * 
     * @param loader Classloader to use when locating the log provider impl
     * @param config BootstrapConfig containing log provider class name.
     * @return LogProvider instance
     *         initial properties containing install directory locations
     * @throws RuntimeException
     *             If a RuntimeException is thrown during the initialization of
     *             log providers, it is propagated to the caller unchanged.
     *             Other Exceptions are caught and re-wrapped as
     *             RuntimeExceptions
     */
    protected LogProvider getLogProviderImpl(ClassLoader loader, BootstrapConfig config) {
        // consume/remove ras provider from the map
        String providerClassName = config.getKernelResolver().getLogProvider();
        LogProvider p = null;

        try {
            Class<?> providerClass = loader.loadClass(providerClassName);
            if (providerClass != null) {
                p = (LogProvider) providerClass.newInstance();

                p.configure(new ReadOnlyFrameworkProperties(config),
                            config.getLogDirectory(),
                            fileStreamFactory);
            }
        } catch (RuntimeException e) {
            // unexpected NPE, etc. -- no need to re-wrap a runtime exception
            throw e;
        } catch (Exception e) {
            // InstantiationException, ClassNotFoundException, IllegalAccessException
            throw new RuntimeException("Could not create framework configurator", e);
        }

        return p;
    }

    /**
     * This provides a (limited) map that wraps around the bootstrap properties
     * provided/held by BootstrapConfig. The OSGi framework automatically backs
     * framework properties with system properties. BootstrapConfig will also
     * check for system properties if the property is not present in bootstrap.properties.
     * This wrapper provides that property lookup pattern to LogProviders
     */
    static final class ReadOnlyFrameworkProperties implements Map<String, String> {
        final BootstrapConfig wrappedCfg;

        ReadOnlyFrameworkProperties(BootstrapConfig cfg) {
            wrappedCfg = cfg;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            if (key instanceof String) {
                return wrappedCfg.get((String) key) != null;
            }
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String get(Object key) {
            if (key instanceof String) {
                return wrappedCfg.get((String) key);
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Set<String> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String put(String key, String value) {
            return wrappedCfg.put(key, value);
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<String> values() {
            throw new UnsupportedOperationException();
        }
    }
}