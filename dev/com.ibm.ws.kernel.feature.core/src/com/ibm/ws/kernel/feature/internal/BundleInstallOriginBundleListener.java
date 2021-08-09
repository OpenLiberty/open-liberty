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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

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
public class BundleInstallOriginBundleListener implements BundleListener {

    private static final TraceComponent tc = Tr.register(BundleInstallOriginBundleListener.class);

    private static final String storagePath = "bundle.origin.cache";

    private final BundleContext ctx;
    private final File bundleOriginCache;

    //stores the ID of a bundle that has installed other bundles as a key to
    //the set of the install IDs of the bundles it has installed
    private final Map<Long, Set<Long>> bundleOrigins = Collections.synchronizedMap(new HashMap<Long, Set<Long>>());
    private final Set<Long> allTracked = new HashSet<>();

    /**
     * This constructor reads the persisted bundleOrigins data from disk and tidies it
     * to account for any removed installer bundles before completing.
     */
    BundleInstallOriginBundleListener(BundleContext ctx) {
        bundleOriginCache = ctx.getDataFile(storagePath);
        // use system bundle context so we can lookup using bundle ID without worrying
        // about hooks filtering them out.
        this.ctx = ctx.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
        loadCacheFromDisk();
    }

    private void loadCacheFromDisk() {
        if (bundleOriginCache != null && bundleOriginCache.exists()) {
            debug("Found existing bundle origin cache", bundleOriginCache.toString(), bundleOriginCache);
            //read the contents of the cache file, which is a map of bundle locations
            readCacheFile();

            debug("Repopulated origins cache from disk", bundleOriginCache);

            // we need to clean up bundles that may have been
            // removed while we weren't listening
            Set<Long> installerBundlesToRemove = new HashSet<Long>();
            synchronized (bundleOrigins) {
                //remove from the loaded cache any bundles that have been removed while the server was down
                for (Map.Entry<Long, Set<Long>> bundleOriginEntry : bundleOrigins.entrySet()) {
                    Long installerBundleId = bundleOriginEntry.getKey();
                    Bundle installerBundle = ctx.getBundle(installerBundleId);
                    if (installerBundle == null) {
                        debug("Installer bundle has been removed", installerBundleId);
                        installerBundlesToRemove.add(installerBundleId);
                    }
                }
            }
            for (Long installerBundleId : installerBundlesToRemove) {
                processUninstalledInstallerBundle(installerBundleId);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void bundleChanged(BundleEvent event) {
        boolean store = false;
        switch (event.getType()) {
            case BundleEvent.INSTALLED:
                Bundle installedBundle = event.getBundle();
                if (!installedBundle.getLocation().startsWith(Provisioner.BUNDLE_LOC_FEATURE_TAG)) {
                    Bundle installerBundle = event.getOrigin();
                    //the bundle was installed by something other than the feature manager
                    //we should associate it with its installer
                    debug("Tracking bundle {0} at location {1} installed by {2}.", installedBundle, installedBundle.getLocation(), installerBundle);
                    synchronized (bundleOrigins) {
                        Set<Long> existingInstalleeBundles = bundleOrigins.get(installerBundle.getBundleId());
                        if (existingInstalleeBundles == null) {
                            existingInstalleeBundles = new HashSet<>();
                            bundleOrigins.put(installerBundle.getBundleId(), existingInstalleeBundles);
                        }
                        //add the ID of the bundle being installed to the set for the origin bundle
                        existingInstalleeBundles.add(installedBundle.getBundleId());
                        allTracked.add(installedBundle.getBundleId());
                    }
                    //flag to store the data
                    store = true;
                }
                break;
            case BundleEvent.UNINSTALLED:
                Bundle uninstalledBundle = event.getBundle();
                //set the flag to store data to disk if the uninstall was one of our known installer bundles
                store = processUninstalledInstallerBundle(uninstalledBundle.getBundleId());
                break;
            default:
                break;
        }
        //if we had a installed or uninstalled event and the data changed
        //then we need to persist it to the workarea ready for a subsequent warm start
        if (store == true) {
            //persist the data after each event is processed
            writeCacheFile();
        }
    }

    /**
     * Determines if the uninstalled bundle ID was an installer bundle and calls for the
     * removal of any installees associated with it if it was.
     *
     * @param installerBundleId
     * @return true if the parameter was an installer bundle
     */
    private boolean processUninstalledInstallerBundle(long installerBundleId) {
        //find out if the uninstalled bundle ID was an installer bundle and remove its installees as well
        Set<Long> bundleIdsToUninstall;
        boolean tracked;
        synchronized (bundleOrigins) {
            bundleIdsToUninstall = bundleOrigins.remove(installerBundleId);
            // use snapshot to avoid concurrent modification while iterating below
            if (bundleIdsToUninstall != null) {
                bundleIdsToUninstall = new HashSet<>(bundleIdsToUninstall);
            }
            tracked = allTracked.remove(installerBundleId);
        }

        boolean uninstalledTracked = bundleIdsToUninstall != null;
        if (uninstalledTracked) {
            debug("Installer bundle {0} had installees that need to be uninstalled", installerBundleId);
            //we recognized the bundle being uninstalled as an installer, try to remove its installees
            Set<Long> unsuccessfulUninstallLocations = uninstallInstalleeBundles(bundleIdsToUninstall);
            //check if we successfully uninstalled everything
            if (!unsuccessfulUninstallLocations.isEmpty()) {
                //we weren't able to uninstall every installee
                //we should update the map with the set of remaining bundle locations
                //that way we will try to uninstall them again another time when it might work
                bundleOrigins.put(installerBundleId, unsuccessfulUninstallLocations);
                debug("Not all installees were removed", unsuccessfulUninstallLocations);
            }
        }
        if (tracked) {
            // may need to clean up the bundle from the sets
            synchronized (bundleOrigins) {
                for (Map.Entry<Long, Set<Long>> entry : bundleOrigins.entrySet()) {
                    uninstalledTracked |= entry.getValue().remove(installerBundleId);
                }
            }
        }
        return uninstalledTracked;
    }

    /**
     * Iterates a set of installee bundles attempting to uninstall them.
     *
     * @param installeeBundleIdsToUninstall
     * @return Set of bundle IDs that could not be uninstalled
     */
    private Set<Long> uninstallInstalleeBundles(Set<Long> installeeBundleIdsToUninstall) {
        Set<Long> unsuccessfulUninstallLocations = new HashSet<>();
        for (Long installeeBundleId : installeeBundleIdsToUninstall) {
            Bundle installeeBundleToUninstall = ctx.getBundle(installeeBundleId);
            if (installeeBundleToUninstall != null) {
                try {
                    installeeBundleToUninstall.uninstall();
                } catch (BundleException e) {
                    //this will auto FFDC
                    //uninstall failed, track it
                    unsuccessfulUninstallLocations.add(installeeBundleId);
                    //we couldn't uninstall the bundle, but we want to stop it starting
                    //set the bundle start level to MAX_INT
                    BundleStartLevel bsl = installeeBundleToUninstall.adapt(BundleStartLevel.class);
                    bsl.setStartLevel(Integer.MAX_VALUE);
                }
            }
        }
        return unsuccessfulUninstallLocations;
    }

    @Trivial
    private void debug(String msg, Object... objs) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, msg, objs);
        }
    }

    private void writeCacheFile() {
        // note that cache version is not stored here because we assume
        // the impl bundle is re-installed if a new version is used which means we
        // will be starting clean anyway
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(bundleOriginCache)))) {
            // cache format is as follows:
            // Int - number of origin bundles being tracked
            // for each origin bundle ->
            //   Long - the origin bundle ID
            //   Int - number of bundles installed by origin bundle
            //   for each bundle installed ->
            //      Long - the installed bundle ID
            synchronized (bundleOrigins) {
                out.writeInt(bundleOrigins.size());
                for (Entry<Long, Set<Long>> origin : bundleOrigins.entrySet()) {
                    out.writeLong(origin.getKey());
                    out.writeInt(origin.getValue().size());
                    for (Long installed : origin.getValue()) {
                        out.writeLong(installed);
                    }
                }
            }
        } catch (IOException e) {
            // auto FFDC is fine here
        }
    }

    private void readCacheFile() {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(bundleOriginCache)))) {
            synchronized (bundleOrigins) {
                bundleOrigins.clear();
                int numOrigins = in.readInt();
                for (int i = 0; i < numOrigins; i++) {
                    long installeeId = in.readLong();
                    int numInstalled = in.readInt();
                    Set<Long> installed = new HashSet<>(numInstalled);
                    for (int j = 0; j < numInstalled; j++) {
                        long id = in.readLong();
                        installed.add(id);
                        allTracked.add(id);
                    }
                    bundleOrigins.put(installeeId, installed);
                }
            }
        } catch (IOException e) {
            // auto FFDC is fine here
        }
    }
}
