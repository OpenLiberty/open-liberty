/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.install.internal.asset.UninstallAsset;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.product.utility.extension.ifix.xml.Problem;
import com.ibm.ws.product.utility.extension.ifix.xml.UpdatedFile;

/**
 *
 */
public class FixDependencyChecker {

    private static final String S_DISABLE = "disable.fix.dependency.check";

    public FixDependencyChecker() {

    }

    /**
     * Return true if fixToBeUninstalled can be uninstalled. fixToBeUninstalled can only be uninstalled if there are no file conflicts with other fixes in the installedFixes Set
     * that supersedes fixToBeUninstalled
     *
     * @param installedFixes The set of fixes that is already installed, the set does not include the fixToBeUninstalled
     * @param fixToBeUninstalled The fix to be uninstalled
     * @return Return true if the fixToBeUninstalled can be uninstalled. False otherwise.
     */
    public static boolean isUninstallable(Set<IFixInfo> installedFixes, IFixInfo fixToBeUninstalled) {

        if (Boolean.valueOf(System.getenv(S_DISABLE)).booleanValue()) {
            return true;
        }

        if (fixToBeUninstalled != null) {
            for (IFixInfo fix : installedFixes) {
                if (!(fixToBeUninstalled.getId().equals(fix.getId())) && !confirmNoFileConflicts(fixToBeUninstalled.getUpdates().getFiles(), fix.getUpdates().getFiles())) {
                    if (!isSupersededBy(fix.getResolves().getProblems(), fixToBeUninstalled.getResolves().getProblems())) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Verfiy whether the fix is uninstallable and there is no other installed
     * fix still require this feature.
     *
     * @param uninstallAsset fix to be uninstalled
     * @param installedFixes installed fixes
     * @param uninstallAssets the list of fixes that is to be uninstalled
     * @return the true if there is no fix that depends on the uninstall asset. return false otherwise
     */
    public boolean isUninstallable(UninstallAsset uninstallAsset, Set<IFixInfo> installedFixes, List<UninstallAsset> uninstallAssets) {

        if (Boolean.valueOf(System.getenv(S_DISABLE)).booleanValue()) {
            return true;
        }

        IFixInfo fixToBeUninstalled = uninstallAsset.getIFixInfo();
        for (IFixInfo fix : installedFixes) {
            if (!(fixToBeUninstalled.getId().equals(fix.getId()))) {
                if ((!confirmNoFileConflicts(fixToBeUninstalled.getUpdates().getFiles(), fix.getUpdates().getFiles())) &&
                    (!isSupersededBy(fix.getResolves().getProblems(), fixToBeUninstalled.getResolves().getProblems())))
                    if (!isToBeUninstalled(fix.getId(), uninstallAssets))
                        return false;
            }
        }
        return true;
    }

    /**
     * Verify the name is on the uninstall list
     *
     * @param name symbolic name of the fix
     * @param list list of the uninstalling fix
     * @return true if the feature is going to be uninstalled, otherwise, return false.
     */
    public boolean isToBeUninstalled(String name, List<UninstallAsset> list) {
        for (UninstallAsset asset : list) {
            String fixName = asset.getIFixInfo().getId();
            if (fixName != null && fixName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine the fix apar is required by one of the installed features
     *
     * @param fixApar fix apar
     * @param installedFeatures installed features
     * @return true if the fix apar is required by one of the installed features, otherwise, return false.
     */
    public static ArrayList<String> fixRequiredByFeature(String fixApar, Map<String, ProvisioningFeatureDefinition> installedFeatures) {
        ArrayList<String> dependencies = new ArrayList<String>();
        for (ProvisioningFeatureDefinition fd : installedFeatures.values()) {
            String requireFixes = fd.getHeader("IBM-Require-Fix");
            if (requireFixes != null && requireFixes.length() > 0) {
                String[] apars = requireFixes.split(";");
                for (String apar : apars) {
                    if (apar.trim().equals(fixApar.trim())) {
                        dependencies.add(apar);
                    }
                }
            }
        }
        if (dependencies.isEmpty())
            return null;
        return dependencies;
    }

    /**
     * Determine the order of the fixes according to their dependency
     *
     * @param list list of the fixes
     * @return the sorted fixes list according to the fix dependency
     */
    public List<UninstallAsset> determineOrder(List<UninstallAsset> list) {
        if (list != null) {
            List<FixDependencyComparator> fixCompareList = new ArrayList<FixDependencyComparator>();
            // Initialize the feature comparator
            for (UninstallAsset asset : list) {
                fixCompareList.add(new FixDependencyComparator(asset.getIFixInfo()));
            }
            // Sort the feature list
            Collections.sort(fixCompareList, new FixDependencyComparator());
            List<UninstallAsset> newList = new ArrayList<UninstallAsset>();
            for (FixDependencyComparator f : fixCompareList) {
                newList.add(new UninstallAsset(f.getIfixInfo()));
            }
            return newList;
        }
        return list;
    }

    /**
     * Returns if the apars list apars1 is superseded by apars2. Apars1 is superseded by apars2 if all the apars in apars1 is also included in apars2
     *
     * @param apars1 Fix to check
     * @param apars2
     * @return Returns true if apars list apars1 is superseded by apars2. Else return false.
     */
    private static boolean isSupersededBy(List<Problem> apars1, List<Problem> apars2) {

        boolean result = true;

        // Now iterate over the current list of problems, and see if the incoming IFixInfo contains all of the problems from this IfixInfo.
        // If it does then return true, to indicate that this IFixInfo object has been superseded.
        for (Iterator<Problem> iter1 = apars1.iterator(); iter1.hasNext();) {
            boolean currAparMatch = false;
            Problem currApar1 = iter1.next();
            for (Iterator<Problem> iter2 = apars2.iterator(); iter2.hasNext();) {
                Problem currApar2 = iter2.next();
                if (currApar1.getDisplayId().equals(currApar2.getDisplayId())) {
                    currAparMatch = true;
                }
            }
            if (!currAparMatch)
                result = false;
        }
        return result;
    }

    /**
     * Confirms that UpdatedFile lists does not contain any common files
     *
     * @param updatedFiles1
     * @param updatedFiles2
     * @return Return true if there are no conflicts and return false otherwise
     */
    private static boolean confirmNoFileConflicts(Set<UpdatedFile> updatedFiles1, Set<UpdatedFile> updatedFiles2) {

        for (Iterator<UpdatedFile> iter1 = updatedFiles1.iterator(); iter1.hasNext();) {
            UpdatedFile currFile1 = iter1.next();
            for (Iterator<UpdatedFile> iter2 = updatedFiles2.iterator(); iter2.hasNext();) {
                UpdatedFile currFile2 = iter2.next();
                if (currFile1.getId().equals(currFile2.getId())) {
                    return false;
                }
            }
        }
        return true;
    }

    static class FixDependencyComparator implements Comparator<FixDependencyComparator> {

        private IFixInfo fix;

        public FixDependencyComparator() {}

        public FixDependencyComparator(IFixInfo fix) {
            this.fix = fix;
        }

        /** {@inheritDoc} */
        @Override
        public int compare(FixDependencyComparator fix1, FixDependencyComparator fix2) {
            if (fix1.getIfixInfo().getId().equals(fix2.getIfixInfo().getId())) {
                return 0;
            }
            if (checkFixDependency(fix1.getIfixInfo(), fix2.getIfixInfo())) {
                return -1;
            }
            return 1;
        }

        public IFixInfo getIfixInfo() {
            return fix;
        }

        /**
         * Return if the fix1 depends on fix 2. Fix1 depends on fix2 if there are common file changes and if fix1 does not supersede fix2
         * does not supersede fix1
         *
         * @param fix1
         * @param requiresFix2
         * @return return true if fix1 does not depend on fix2 and false otherwise
         */
        private boolean checkFixDependency(IFixInfo fix1, IFixInfo requiresFix2) {
            if (!(confirmNoFileConflicts(fix1.getUpdates().getFiles(), requiresFix2.getUpdates().getFiles())) &&
                (!isSupersededBy(requiresFix2.getResolves().getProblems(), fix1.getResolves().getProblems())))
                return false;
            else
                return true;
        }
    }

}
