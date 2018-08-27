/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * This class listens for {@link Bundle}.INSTALLED and {@link Bundle}.UNINSTALLED events.
 * If a INSTALLED event has an origin that was not the {@link FeatureManager}'s bundle then
 * it adds the location of that installer bundle to a map serving as a key to track the
 * locations of all installee bundles the installer installs.
 * <BR>
 * If a installer bundle is uninstalled, then it also uninstalls any installee bundles that
 * were associated with it.
 * <BR>
 * Additionally the data is persisted to the workarea so that the data can be used on a warm start.
 * When the data is read from disk the set of installer bundles is checked to verify that the installer
 * bundles are still present, because they could have been removed by a feature config change while
 * the server was down. If an installer bundle has been removed, then this the associated installees
 * are also uninstalled.
 * <BR>
 * On a clean start all bundles are installed again so the data can be rebuilt from the events.
 * <BR>
 * If there is ever an issue uninstalling one of the installee bundles the map is preserved so as not to
 * lose track of the faulty installee bundle. However, in this scenario the {@link BundleStartLevel} will
 * be adjusted so as to avoid starting the installee bundle that should no longer be present. Later server
 * starts will attempt to uninstall the bundle again.
 */
public class BundleInstallOriginBundleListener implements BundleListener, Callable<Void> {

    private static final TraceComponent tc = Tr.register(BundleInstallOriginBundleListener.class);

    private static final String storagePath = "bundle.origin.cache";
    private final Bundle featureManager;
    private final BundleContext ctx;
    private final File bundleOriginCache;

    //stores the location of a bundle that has installed other bundles as a key to
    //the set of the install locations of the bundles it has installed
    private volatile ConcurrentHashMap<String, Set<String>> bundleOrigins = new ConcurrentHashMap<String, Set<String>>();

    /**
     * This constructor reads the persisted bundleOrigins data from disk and tidies it
     * to account for any removed installer bundles before completing.
     */

    BundleInstallOriginBundleListener(BundleContext ctx) {
        this.ctx = ctx;
        //this class is constructed in the feature manager, so get bundle from the ctx
        featureManager = ctx.getBundle();
        bundleOriginCache = featureManager.getDataFile(storagePath);
        bundleOrigins = loadCacheFromDisk(true);
        //set a purge up in case there aren't actually any more bundle events
        delayPurge();
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore(IOException.class)
    private synchronized ConcurrentHashMap<String, Set<String>> loadCacheFromDisk(boolean newInstance) {
        ConcurrentHashMap<String, Set<String>> loadedOrigins = new ConcurrentHashMap<String, Set<String>>();
        if (bundleOriginCache != null && bundleOriginCache.exists()) {
            debug("Found existing bundle origin cache", bundleOriginCache.toString(), bundleOriginCache);
            //read the contents of the cache file, which is a map of bundle locations
            FileInputStream fis = null;
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream((fis = new FileInputStream(bundleOriginCache)));
                loadedOrigins = (ConcurrentHashMap<String, Set<String>>) ois.readObject();

                debug("Repopulated origins cache from disk", loadedOrigins);

                //if we are a new instance we need to clean up bundles that may have been
                //removed while we weren't listening
                if (newInstance) {
                    Set<String> installerBundlesToRemove = new HashSet<String>();
                    //remove from the loaded cache any bundles that have been removed while the server was down
                    for (Map.Entry<String, Set<String>> bundleOriginEntry : loadedOrigins.entrySet()) {
                        String installerBundleLocation = bundleOriginEntry.getKey();
                        Bundle installerBundle = ctx.getBundle(installerBundleLocation);
                        if (installerBundle == null) {
                            debug("Installer bundle has been removed", installerBundleLocation);
                            installerBundlesToRemove.add(installerBundleLocation);
                        }
                    }
                    for (String installerBundleLocation : installerBundlesToRemove) {
                        processUninstalledInstallerBundle(installerBundleLocation);
                    }
                }

            } catch (FileNotFoundException e) {
                //autoFFDC
            } catch (IOException e) {
                //manually FFDC here because we are preventing autoFFDC instrumentation for the finally block
                FFDCFilter.processException(e, getClass().getName(), "118", new Object[] { bundleOriginCache });
            } catch (ClassNotFoundException e) {
                //autoFFDC
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException e) {
                        //suppress FFDC
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        //suppress FFDC
                    }
                }
            }
        }
        return loadedOrigins;
    }

    private void loadCacheIfPurged() {
        if (bundleOrigins == null) {
            synchronized (this) {
                //check still null
                if (bundleOrigins == null)
                    bundleOrigins = loadCacheFromDisk(false);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(IOException.class)
    public void bundleChanged(BundleEvent event) {
        boolean store = false;
        switch (event.getType()) {
            case BundleEvent.INSTALLED:
                Bundle installerBundle = event.getOrigin();
                if (!featureManager.equals(installerBundle)) {
                    loadCacheIfPurged();
                    //the bundle was installed by something other than the feature manager
                    //we should associate it with its installer
                    String installingBundleSN = installerBundle.getLocation();
                    Set<String> installeeBundles = Collections.synchronizedSet(new HashSet<String>());
                    Set<String> existingInstalleeBundles;
                    existingInstalleeBundles = bundleOrigins.putIfAbsent(installingBundleSN, installeeBundles);
                    if (existingInstalleeBundles == null)
                        existingInstalleeBundles = installeeBundles;
                    //add the location of the bundle being installed to the set for the origin bundle
                    existingInstalleeBundles.add(event.getBundle().getLocation());
                    //flag to store the data
                    store = true;
                    //delay the purge of the origins cache
                    delayPurge();
                }
                break;
            case BundleEvent.UNINSTALLED:
                loadCacheIfPurged();
                Bundle uninstalledBundle = event.getBundle();
                //set the flag to store data to disk if the uninstall was one of our known installer bundles
                store = processUninstalledInstallerBundle(uninstalledBundle.getLocation());
                //delay the purge of the origins cache
                delayPurge();
                break;
            default:
                break;
        }
        //if we had a installed or uninstalled event and the data changed
        //then we need to persist it to the workarea ready for a subsequent warm start
        if (store == true) {
            //persist the data after each event is processed
            synchronized (bundleOriginCache) {
                FileOutputStream fos = null;
                ObjectOutputStream oos = null;
                try {
                    //create if it doesn't exist yet
                    boolean created = bundleOriginCache.createNewFile();
                    if (created || bundleOriginCache.exists()) {
                        oos = new ObjectOutputStream((fos = new FileOutputStream(bundleOriginCache)));
                        oos.writeObject(bundleOrigins);
                    }
                } catch (FileNotFoundException e) {
                    //autoFFDC
                } catch (IOException e) {
                    //manually FFDC here because we are preventing autoFFDC instrumentation for the finally block
                    FFDCFilter.processException(e, getClass().getName(), "201", new Object[] { bundleOrigins, bundleOriginCache });
                } finally {
                    if (oos != null) {
                        try {
                            oos.close();
                        } catch (IOException e) {
                            //suppress FFDC
                        }
                    }
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            //suppress FFDC
                        }
                    }
                }

            }
        }
    }

    /**
     * Determines if the uninstalled bundle location was an installer bundle and calls for the
     * removal of any installees associated with it if it was.
     *
     * @param installerBundleLocation
     * @return true if the parameter was an installer bundle
     */
    private boolean processUninstalledInstallerBundle(String installerBundleLocation) {
        //find out if the uninstalled bundle location was an installer bundle and remove its installees as well
        Set<String> bundleLocationsToUninstall = bundleOrigins.remove(installerBundleLocation);
        if (bundleLocationsToUninstall != null) {
            debug("Installer bundle at location {0} had installees that need to be uninstalled", installerBundleLocation);
            //we recognized the bundle being uninstalled as an installer, try to remove its installees
            Set<String> unsuccessfulUninstallLocations = uninstallInstalleeBundles(bundleLocationsToUninstall);
            //check if we successfully uninstalled everything
            if (!unsuccessfulUninstallLocations.isEmpty()) {
                //we weren't able to uninstall every installee
                //we should update the map with the set of remaining bundle locations
                //that way we will try to uninstall them again another time when it might work
                bundleOrigins.put(installerBundleLocation, unsuccessfulUninstallLocations);
                debug("Not all installees were removed", unsuccessfulUninstallLocations);
            }
            return true;
        }
        return false;
    }

    /**
     * Iterates a set of installee bundles attempting to uninstall them.
     *
     * @param installeeBundleLocationsToUninstall
     * @return Set of bundle locations that could not be uninstalled
     */
    private Set<String> uninstallInstalleeBundles(Set<String> installeeBundleLocationsToUninstall) {
        Set<String> unsuccessfulUninstallLocations = new HashSet<String>();
        for (String installeeBundleLocation : installeeBundleLocationsToUninstall) {
            Bundle installeeBundleToUninstall = ctx.getBundle(installeeBundleLocation);
            if (installeeBundleToUninstall != null) {
                try {
                    installeeBundleToUninstall.uninstall();
                } catch (BundleException e) {
                    //this will auto FFDC
                    //uninstall failed, track it
                    unsuccessfulUninstallLocations.add(installeeBundleLocation);
                    //we couldn't uninstall the bundle, but we want to stop it starting
                    //set the bundle start level to MAX_INT
                    BundleStartLevel bsl = installeeBundleToUninstall.adapt(BundleStartLevel.class);
                    bsl.setStartLevel(Integer.MAX_VALUE);
                }
            }
        }
        return unsuccessfulUninstallLocations;
    }

    private void debug(String msg, Object... objs) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, msg, objs);
        }
    }

    private ScheduledFuture<?> futurePurge = null;
    //if there have been no bundle events for 5 minutes, purge the map from memory
    //it will be reloaded from disk if necessary
    private final int purgeDelay = 5;

    @FFDCIgnore(IllegalStateException.class)
    private synchronized void delayPurge() {

        // if the framework is stopping, dont create the delayed purge
        if (FrameworkState.isStopping()) {
            return;
        }

        //try to cancel any existing task if we can and it isn't running
        if (futurePurge != null) {
            futurePurge.cancel(false);
        }

        //any time this method is called we want to schedule a new purge for the future
        ServiceReference<ScheduledExecutorService> sesRef = ctx.getServiceReference(ScheduledExecutorService.class);
        if (sesRef != null) {
            try {
                ScheduledExecutorService executorService = ctx.getService(sesRef);
                futurePurge = executorService.schedule(this, purgeDelay, TimeUnit.MINUTES);
            } finally {
                try {
                    ctx.ungetService(sesRef);
                } catch (IllegalStateException e) {
                    // This is highly unlikely, but can happen.
                    // Rather than do a boolean check, its more efficient in the 99.99% case
                    // to just handle the exception if it occurs (which is unlikely)
                    if (tc.isEventEnabled()) {
                        Tr.event(tc,
                                 "IllegalStateException while releasing ServiceReference<ScheduledExecutorService> sesRef - the bundle is stopped or in an otherwise invalid so we shouldn't care",
                                 e);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public synchronized Void call() throws Exception {
        //this gets called on schedule to clear the map for garbage collection
        futurePurge = null;
        bundleOrigins = null;
        return null;
    }
}
