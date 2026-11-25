/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.jni.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

import com.ibm.ws.kernel.boot.delegated.zos.NativeMethodHelper;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * Component responsible for driving native code related to JNI methods.
 * In particular, this code will track the classes for which native methods
 * are registered and for driving activation and deactivation hooks to
 * enable native code to more effectively manage non-java state.
 */
public class NativeMethodManagerImpl implements BundleActivator, BundleListener, NativeMethodManager {

    /**
     * Map of bundle to associated {@code Class}. This map can only be
     * used by synchronized instance methods.
     */
    protected Map<Bundle, Set<Class<?>>> bundleData = new HashMap<Bundle, Set<Class<?>>>();

    /**
     * Map of {@code Class} instance to {@code NativeMethodInfo}. This map
     * can only be used by synchronized instance methods.
     */
    protected Map<Class<?>, NativeMethodInfo> nativeInfo = new HashMap<Class<?>, NativeMethodInfo>();

    /**
     * Service registration for the native method manager.
     */
    protected ServiceRegistration<NativeMethodManager> serviceRegistration;

    /**
     * The hosting bundle's context.
     */
    protected final BundleContext bundleContext;

    /**
     * Constructor.
     *
     * @param the owning bundle's context
     */
    public NativeMethodManagerImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Component activation callback.
     *
     * @param bundleContext the host bundle's context
     */
    @Override
    public synchronized void start(BundleContext bundleContext) {
        bundleContext.addBundleListener(this);
        Dictionary<String, String> properties = new Hashtable<String, String>();

        properties.put(Constants.SERVICE_VENDOR, "IBM");

        serviceRegistration = bundleContext.registerService(NativeMethodManager.class, this, properties);

        // Expose the NativeMethodManger functions to non-Zos Components
        AngelUtilsImpl.INSTANCE.start(bundleContext);
    }

    /**
     * Component passivation callback.
     */
    @Override
    public synchronized void stop(BundleContext bundleContext) {
        AngelUtilsImpl.INSTANCE.stop();

        bundleContext.removeBundleListener(this);
        serviceRegistration.unregister();
        serviceRegistration = null;

        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.addAll(nativeInfo.keySet());

        for (Class<?> clazz : classes) {
            removeRegistration(clazz);
        }

        bundleData.clear();
        nativeInfo.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerNatives(Class<?> clazz) {
        registerNatives(clazz, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    synchronized public void registerNatives(Class<?> clazz, Object[] extraInfo) {
        Bundle bundle = getBundle(clazz);
        Version bundleVersion = bundle != null ? bundle.getVersion() : null;

        // Skip processing if we've already processed this class
        if (nativeInfo.containsKey(clazz)) {
            return;
        }

        for (String candidate : getStructureCandidates(clazz, bundleVersion)) {
            long dllHandle = invokeHelperRegisterNatives(clazz, candidate, extraInfo);
            if (dllHandle > 0) {
                addRegistration(clazz, bundle, extraInfo, candidate, dllHandle);
                return;
            }
        }

        // Failed to resolve the native method descriptor
        throw new UnsatisfiedLinkError("Native method descriptor for " + clazz.getCanonicalName() + " not found");
    }

    /**
     * {@inheritDoc} <p>
     * This callback is used to handle bundle {@code STOPPED} events to cleanup
     * server state related to native code associated with the stopped bundle.
     */
    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STOPPED) {
            cleanupRegistrations(event.getBundle());
        }
    }

    /**
     * Cleanup any native code state associated with classes loaded
     * from the specified bundle.
     *
     * @param bundle the bundle that is stopping
     */
    synchronized void cleanupRegistrations(Bundle bundle) {
        Set<Class<?>> classes = bundleData.remove(bundle);
        if (classes != null) {
            for (Class<?> clazz : classes) {
                removeRegistration(clazz);
            }
        }
    }

    /**
     * Remove a native method registration and drive the necessary cleanup
     * functions associated with the registration.
     *
     * @param clazz the class that should no longer be referencable
     */
    private synchronized void removeRegistration(Class<?> clazz) {
        NativeMethodInfo info = nativeInfo.remove(clazz);
        if (info != null) {
            invokeHelperDeregisterNatives(info.getDllHandle(),
                                          info.getClazz(),
                                          info.getNativeDescriptorName(),
                                          info.getExtraInfo());
        }
    }

    /**
     * Get the ordered list of names we'll look for when resolving
     * the {@code NativeMethodDescriptor} symbol in native.
     *
     * @param clazz         the class to link native methods with
     * @param bundleVersion the version information from the bundle
     *                          associated with {@code clazz} or {@code null}
     */
    List<String> getStructureCandidates(Class<?> clazz, Version bundleVersion) {
        List<String> candidates = new ArrayList<String>();

        final String prefix = "zJNI_";
        final String stem = prefix + clazz.getCanonicalName().replaceAll("\\.", "_");
        if (bundleVersion != null) {
            candidates.add(stem + "__" + bundleVersion.getMajor() + "_" + bundleVersion.getMinor() + "_" + bundleVersion.getMicro());
            candidates.add(stem + "__" + bundleVersion.getMajor() + "_" + bundleVersion.getMinor());
            candidates.add(stem + "__" + bundleVersion.getMajor());
        }
        candidates.add(stem);
        candidates.add(prefix + clazz.getSimpleName());

        return candidates;
    }

    protected synchronized void addRegistration(Class<?> clazz, Bundle bundle, Object[] extraInfo, String descriptorName, long dllHandle) {
        // Bundle --> Class
        if (bundle != null) {
            Set<Class<?>> bundleClasses = bundleData.get(bundle);
            if (bundleClasses == null) {
                bundleClasses = new HashSet<Class<?>>();
                bundleData.put(bundle, bundleClasses);
            }
            bundleClasses.add(clazz);
        }

        // Class<?> --> NativeMethodInfo
        nativeInfo.put(clazz, new NativeMethodInfo(clazz, descriptorName, extraInfo, dllHandle));
    }

    protected long invokeHelperRegisterNatives(Class<?> clazz, String structureName, Object[] extraInfo) {
        return NativeMethodHelper.registerNatives(clazz, structureName, extraInfo);
    }

    protected long invokeHelperDeregisterNatives(long dllHandle, Class<?> clazz, String structureName, Object[] extraInfo) {
        return NativeMethodHelper.deregisterNatives(dllHandle, clazz, structureName, extraInfo);
    }

    protected Bundle getBundle(Class<?> clazz) {
        return FrameworkUtil.getBundle(clazz);
    }
}
