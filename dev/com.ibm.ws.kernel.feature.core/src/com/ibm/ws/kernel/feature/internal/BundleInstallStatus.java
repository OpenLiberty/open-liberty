/*******************************************************************************
 * Copyright (c) 2009-2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;

/**
 * Container for the results of installing a set of bundles:
 * A) A Collection of Bundles that were successfully installed (sorted by startLevel),
 * B) A list of Strings naming bundles that could not be located, and
 * C) A map of String-Exception pairs for bundles that caused exceptions
 * when being loaded.
 *
 * <p>
 * It is up to the caller to use this information to provide appropriate diagnostics.
 */
public class BundleInstallStatus {
    private List<Throwable> otherExceptions = null;
    private Collection<Bundle> bundlesToStart = null;
    private List<FeatureResource> missingBundles = null;
    private List<String> missingFeatures = null;
    private List<String> conflictFeatures = null;

    protected volatile IllegalStateException invalidContextEx = null;

    private Map<String, Throwable> installExceptions = null;
    private Collection<Bundle> bundlesDelta = null;
    private Collection<Bundle> bundlesRemovedDelta = null;

    @Trivial
    public boolean contextIsValid() {
        return invalidContextEx == null;
    }

    public void markContextInvalid(IllegalStateException ise) {
        invalidContextEx = ise;
    }

    public void rethrowInvalidContextException() {
        if (invalidContextEx != null)
            throw invalidContextEx;
    }

    public boolean canContinue(boolean continueOnError) {
        if (!continueOnError) {
            if (otherExceptions() || bundlesMissing() ||
                featuresMissing() || installExceptions() ||
                featuresConflict()) {
                return false;
            }
        }
        return true;
    }

    @Trivial
    public boolean otherExceptions() {
        return otherExceptions != null;
    }

    @Trivial
    public boolean bundlesMissing() {
        return missingBundles != null;
    }

    @Trivial
    public boolean featuresMissing() {
        return missingFeatures != null;
    }

    @Trivial
    public boolean bundlesToStart() {
        return bundlesToStart != null;
    }

    @Trivial
    public boolean installExceptions() {
        return installExceptions != null;
    }

    @Trivial
    public boolean featuresConflict() {
        return conflictFeatures != null;
    }

    public List<Throwable> getOtherExceptions() {
        return otherExceptions;
    }

    public List<FeatureResource> getMissingBundles() {
        return missingBundles;
    }

    public List<String> getMissingFeatures() {
        return missingFeatures;
    }

    public Collection<Bundle> getBundlesToStart() {
        return bundlesToStart;
    }

    public Map<String, Throwable> getInstallExceptions() {
        return installExceptions;
    }

    public Collection<String> getConflictFeatures() {
        return conflictFeatures;
    }

    public void addMissingBundle(FeatureResource fr) {
        if (missingBundles == null)
            missingBundles = new ArrayList<FeatureResource>();

        missingBundles.add(fr);
    }

    public void addMissingFeature(String bundleKey) {
        if (missingFeatures == null)
            missingFeatures = new ArrayList<String>();

        missingFeatures.add(bundleKey);
    }

    public void addBundleAddedDelta(Bundle bundle) {
        if (bundlesDelta == null)
            bundlesDelta = new TreeSet<Bundle>(sortByStartLevel);

        bundlesDelta.add(bundle);
    }

    public void addBundleRemovedDelta(Bundle bundle) {
        if (bundlesRemovedDelta == null)
            bundlesRemovedDelta = new HashSet<Bundle>();

        bundlesRemovedDelta.add(bundle);
    }

    public void addBundleToStart(Bundle bundle) {
        if (bundlesToStart == null)
            bundlesToStart = new TreeSet<Bundle>(sortByStartLevel);

        bundlesToStart.add(bundle);
    }

    static final Comparator<Bundle> sortByStartLevel = new Comparator<Bundle>() {
        //sort by startLevel (and then bundle id)
        //this preserves the start level start order for feature bundles
        //even when they are added dynamically (i.e. the OSGi framework
        //has already been started and is at maximum start level)
        @Override
        public int compare(Bundle b1, Bundle b2) {
            int sl1 = b1.adapt(BundleStartLevel.class).getStartLevel();
            int sl2 = b2.adapt(BundleStartLevel.class).getStartLevel();
            //compare the start levels
            int result = Integer.signum(sl1 - sl2);
            if (result == 0) {
                //start level was the same, use bundleId
                int idResult = Long.signum(b1.getBundleId() - b2.getBundleId());
                //if bundle id was the same, they are equal, otherwise order by bundle id
                return (idResult == 0) ? 0 : idResult;
            } else {
                return result;
            }
        }
    };

    public void addInstallException(String bundleKey, Throwable e) {
        if (installExceptions == null)
            installExceptions = new HashMap<String, Throwable>();

        installExceptions.put(bundleKey, e);
    }

    public void addOtherException(Throwable t) {
        if (otherExceptions == null)
            otherExceptions = new ArrayList<Throwable>();

        otherExceptions.add(t);
    }

    public void addConflictFeature(String feature) {
        if (conflictFeatures == null)
            conflictFeatures = new ArrayList<String>();

        conflictFeatures.add(feature);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[missingBundles=" + missingBundles
               + ",missingFeatures=" + missingFeatures
               + ",bundlesToStart=" + bundlesToStart
               + ",otherExceptions=" + otherExceptions
               + ",installExceptions=" + installExceptions
               + ",conflictingFeatures=" + conflictFeatures
               + "]";
    }

    /**
     * @return
     */
    public Collection<Bundle> getBundlesAddedDelta() {
        return this.bundlesDelta;
    }

    public Collection<Bundle> getBundlesRemovedDelta() {
        return this.bundlesRemovedDelta;
    }
}