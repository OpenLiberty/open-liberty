/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.featureverifier.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.osgi.framework.Version;

import com.ibm.aries.buildtasks.semantic.versioning.BinaryCompatibilityStatus;
import com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo;
import com.ibm.aries.buildtasks.semantic.versioning.model.PackageContent;
import com.ibm.aries.buildtasks.semantic.versioning.model.PkgInfo;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.ClassDeclaration;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.FieldDeclaration;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.MethodDeclaration;
import com.ibm.aries.buildtasks.utils.SemanticVersioningUtils;
import com.ibm.ws.featureverifier.internal.PackageComparator.VersionResult.Change;
import com.ibm.ws.featureverifier.internal.XmlErrorCollator.ReportType;

public class PackageComparator {

    private final static String oneLineBreak = "\n";
    private final static String twoLineBreaks = "\n";

    public PackageComparator() {}

    public static class VersionResult {
        public enum Change {
            NO_CHANGE, MAJOR, MINOR
        };

        public VersionChange majorChange;
        public VersionChange minorChange;
        public Set<String> oldFeatureDeclarers;
        public Set<String> newFeatureDeclarers;
        public PkgInfo pkg;
        public Map<String, Change> affectedResources;
    }

    private String makeNice(String in) {
        if (in == null)
            return "";
        else
            return in;
    }

    public List<VersionResult> compareSplitDupePackage(PackageContent oldPkg, PackageContent newPkg, Set<String> oldTypes, Set<String> newTypes, Set<FeatureInfo> features,
                                                       String apiOrSpi) {
        boolean good = true;

        List<VersionResult> results = new ArrayList<VersionResult>();

        VersionResult vr = comparePackage(oldPkg, newPkg, oldTypes, newTypes, true, new PrintWriter(new StringWriter()), features, apiOrSpi);
        if (vr.minorChange.isChange()) {
            good = false;
            System.out.println("Compare of split/dupe package found minor differences bundle1: " + oldPkg.getPackage().getFromBundle() + "@"
                               + oldPkg.getPackage().getFromBundleVersion() + " bundle2: " + newPkg.getPackage().getFromBundle() + "@"
                               + newPkg.getPackage().getFromBundleVersion());
            System.out.println(" Class: " + vr.minorChange.changeClass + "\n Reason: " + makeNice(vr.minorChange.getReason()) + "\n Remarks: "
                               + makeNice(vr.minorChange.getSpecialRemarks()));
        }
        if (vr.majorChange.isChange()) {
            good = false;
            System.out.println("Compare of split/dupe package found major differences bundle1: " + oldPkg.getPackage().getFromBundle() + "@"
                               + oldPkg.getPackage().getFromBundleVersion() + " bundle2: " + newPkg.getPackage().getFromBundle() + "@"
                               + newPkg.getPackage().getFromBundleVersion());
            System.out.println(" Class: " + vr.majorChange.changeClass + "\n Reason: " + makeNice(vr.majorChange.getReason()) + "\n Remarks: "
                               + makeNice(vr.majorChange.getSpecialRemarks()));
        }
        if (!good) {
            results.add(vr);
            good = true;
        }

        //for split/dupes.. compare BOTH directions..
        vr = comparePackage(newPkg, oldPkg, oldTypes, newTypes, true, new PrintWriter(new StringWriter()), features, apiOrSpi);
        if (vr.minorChange.isChange()) {
            good = false;
            System.out.println("Compare of split/dupe package found minor differences bundle1: " + oldPkg.getPackage().getFromBundle() + "@"
                               + oldPkg.getPackage().getFromBundleVersion() + " bundle2: " + newPkg.getPackage().getFromBundle() + "@"
                               + newPkg.getPackage().getFromBundleVersion());
            System.out.println(" Class: " + vr.minorChange.changeClass + "\n Reason: " + makeNice(vr.minorChange.getReason()) + "\n Remarks: "
                               + makeNice(vr.minorChange.getSpecialRemarks()));
        }
        if (vr.majorChange.isChange()) {
            good = false;
            System.out.println("Compare of split/dupe package found major differences bundle1: " + oldPkg.getPackage().getFromBundle() + "@"
                               + oldPkg.getPackage().getFromBundleVersion() + " bundle2: " + newPkg.getPackage().getFromBundle() + "@"
                               + newPkg.getPackage().getFromBundleVersion());
            System.out.println(" Class: " + vr.majorChange.changeClass + "\n Reason: " + makeNice(vr.majorChange.getReason()) + "\n Remarks: "
                               + makeNice(vr.majorChange.getSpecialRemarks()));
        }
        if (!good) {
            results.add(vr);
            good = true;
        }

        if (results.isEmpty()) {
            return null;
        } else {
            return results;
        }
    }

    private enum VersionCheckResult {
        CORRECT, NEEDS_CHANGE, INCREMENTED_TOO_MUCH, REGRESSED
    }

    private class VersionCheck {

        private static final String CORRECT_MESSAGE = "The version is correct. ";
        private static final String REGRESSED_MESSAGE = "The version has been reduced incorrectly. ";
        private static final String INCREMENTED_TOO_MUCH_MESSAGE = "The version was increased more than it should have been. ";
        private static final String NEEDS_MAJOR_CHANGE_MESSAGE = "The major version should have increased but did not. ";
        private static final String NEEDS_MINOR_CHANGE_MESSAGE = "The minor version should have increased but did not. ";

        private final Version oldVersion;
        private final Version newVersion;
        private final VERSION_CHANGE_TYPE changeType;
        private VersionCheckResult result = null;

        /**
         * @param changeTypeToCheck
         * @param oldVersion
         * @param newVersion
         */
        public VersionCheck(VERSION_CHANGE_TYPE changeTypeToCheck, String oldVersion, String newVersion) {
            this.oldVersion = Version.parseVersion(oldVersion);
            this.newVersion = Version.parseVersion(newVersion);

            //we don't want to allow version changes for packages in the ifix stream.
            if (GlobalConfig.getReleaseMode().equals("IFIX")) {
                if (changeTypeToCheck == VERSION_CHANGE_TYPE.MAJOR_CHANGE || changeTypeToCheck == VERSION_CHANGE_TYPE.MINOR_CHANGE)
                    changeType = VERSION_CHANGE_TYPE.NO_CHANGE;
                else
                    changeType = changeTypeToCheck;
            } else {
                this.changeType = changeTypeToCheck;
            }
        }

        @Override
        public String toString() {
            if (this.result == null)
                this.result = computeResult();

            StringBuffer message = new StringBuffer();
            switch (result) {
                case CORRECT:
                    return CORRECT_MESSAGE;
                case NEEDS_CHANGE:
                    if (changeType == VERSION_CHANGE_TYPE.MAJOR_CHANGE)
                        message.append(NEEDS_MAJOR_CHANGE_MESSAGE);
                    else
                        message.append(NEEDS_MINOR_CHANGE_MESSAGE);
                    break;
                case INCREMENTED_TOO_MUCH:
                    message.append(INCREMENTED_TOO_MUCH_MESSAGE);
                    break;
                case REGRESSED:
                    message.append(REGRESSED_MESSAGE);
                    break;

            }

            message.append("The previous version was ");
            message.append(oldVersion.toString());
            message.append(". The current version is ");
            message.append(newVersion.toString());

            return message.toString();
        }

        private VersionCheckResult validateVersionDifference(int diff) {
            if (diff == 1) {
                return VersionCheckResult.CORRECT;
            } else if (diff == 0) {
                return VersionCheckResult.NEEDS_CHANGE;
            } else if (diff > 0) {
                return VersionCheckResult.INCREMENTED_TOO_MUCH;
            } else {
                return VersionCheckResult.REGRESSED;
            }
        }

        public VersionCheckResult getMajorChangeResult() {
            int majorDiff = newVersion.getMajor() - oldVersion.getMajor();
            return validateVersionDifference(majorDiff);
        }

        public VersionCheckResult getMinorChangeResult() {
            int minorDiff = newVersion.getMinor() - oldVersion.getMinor();
            // I'm not sure why we're accepting a major version change here.. possibly because minor changes still show up when we have a major change?
            if (getMajorChangeResult() == VersionCheckResult.CORRECT) {
                return VersionCheckResult.CORRECT;
            } else if (getMajorChangeResult() == VersionCheckResult.NEEDS_CHANGE) {
                // "NEEDS_CHANGE" in this case just means that major version wasn't updated, as we would expect here
                return validateVersionDifference(minorDiff);
            } else {
                // we have bigger problems...
                return getMajorChangeResult();
            }

        }

        public boolean isCorrect() {
            if (this.result == null) {
                this.result = computeResult();
            }
            return this.result == VersionCheckResult.CORRECT;
        }

        public VersionCheckResult computeResult() {
            VersionCheckResult versionCorrect = VersionCheckResult.NEEDS_CHANGE;

            if (changeType == VERSION_CHANGE_TYPE.MAJOR_CHANGE) {
                return getMajorChangeResult();
            } else if (changeType == VERSION_CHANGE_TYPE.MINOR_CHANGE) {
                return getMinorChangeResult();
            } else if (changeType == VERSION_CHANGE_TYPE.NO_CHANGE) {
                // If no change, it is not acceptable to move up major or minor versions.
                // However, will enforce a change from 0.0.0 to anything else, as it is bad practice to export
                // packages as 0.0.0, and allow anything for the new version, not just 1.0.0, as we may be
                // importing and exporting a (possibly third party) package with an existing version number.
                if (oldVersion.equals(Version.emptyVersion) && !newVersion.equals(Version.emptyVersion)) {
                    versionCorrect = VersionCheckResult.CORRECT;
                }
                if ((newVersion.getMajor() == oldVersion.getMajor()) && (newVersion.getMinor() == oldVersion.getMinor()) && (newVersion.getMicro() >= oldVersion.getMicro())) {
                    versionCorrect = VersionCheckResult.CORRECT;
                }
            } else if (changeType == VERSION_CHANGE_TYPE.NEW_PACKAGE) {
                if (newVersion.equals(Version.parseVersion(SemanticVersioningUtils.WAS_PACKAGE_INITIAL_VERSION))) {
                    versionCorrect = VersionCheckResult.CORRECT;
                }
            }
            return versionCorrect;
        }

    }

    /**
     * Compares 2 package contents, to see if they are compatible.
     * <p>
     * Used in two modes<br>
     * <ul>
     * <li>Split/Dupe Check mode. To compare 2 instances of the same package found in the same build to see if they are copies of each other.
     * <li>Build/Baseline mode. To compare the same package found in 2 different builds.
     * </ul>
     *
     * @param oldPkg the baseline package (or first instance of split package)
     * @param newPkg the new build package (or 2nd instance of split package)
     * @param splitCompare true, if this is being performed as a split/dupe comparison, false if this is a baseline compare
     * @param pkgElements
     * @param features the features declaring the new package as api/spi. Used for report messages, and dev jar validation.
     * @return VersionResult structure containing the comparison results & messages.
     */
    public VersionResult comparePackage(PackageContent oldPkg, PackageContent newPkg, Set<String> oldTypes, Set<String> newTypes, boolean splitCompare, PrintWriter pkgElements,
                                        Set<FeatureInfo> features, String apiOrSpi) {
        PkgInfo fatal_package = null;
        VersionResult vr = new VersionResult();
        VersionChange majorChange = new VersionChange();
        VersionChange minorChange = new VersionChange();
        vr.majorChange = majorChange;
        vr.minorChange = minorChange;
        vr.affectedResources = new HashMap<String, Change>();

        String pkgName = oldPkg.getPackage().getName();

        Set<FeatureInfo> aggregateFeatures = new HashSet<FeatureInfo>();
        for (FeatureInfo f : features) {
            aggregateFeatures.addAll(f.getAggregateFeatureSet());
        }

        if (aggregateFeatures.isEmpty()) {
            System.out.println("Aggregation set empty in pc.. ");
            for (FeatureInfo f : features) {
                System.out.println("Original set " + f.getName());
            }
        }

        //copy the maps as visitPackage will delete classes it processes, and we need to preserve the original
        //maps for future comparisons this run.
        Map<String, ClassDeclaration> oldclasses = new TreeMap<String, ClassDeclaration>(oldPkg.getClasses());
        Map<String, ClassDeclaration> newclasses = new TreeMap<String, ClassDeclaration>(newPkg.getClasses());

        //visit will visit the two packages, and remove classes processed from the newclasses maps.
        visitPackage(aggregateFeatures, pkgName,
                     oldclasses,
                     newclasses,
                     oldTypes,
                     newTypes,
                     apiOrSpi,
                     vr);

        // If there is no binary compatibility changes, check whether xsd files have been added, changed or deleted
        if (!!!majorChange.isChange()) {
            Map<String, String> oldXsds = new TreeMap<String, String>(oldPkg.getXsds());
            Map<String, String> newXsds = new TreeMap<String, String>(newPkg.getXsds());

            //check will remove xsds processed from the newxsds map
            checkXsdChangesInPkg(aggregateFeatures, pkgName,
                                 oldXsds,
                                 newXsds,
                                 apiOrSpi,
                                 vr);

            // If everything is ok with the existing classes. Need to find out whether there are more API (abstract classes) in the current bundle.
            // loop through curClazz and visit it and find out whether one of them is abstract.
            // check whether there are more xsd or abstract classes added
            if (!!!(majorChange.isChange() || minorChange.isChange())) {

                //newclasses & newxsds will now only contain 'extra' content beyond the oldPkg content.
                checkAdditionalClassOrXsds(aggregateFeatures, pkgName,
                                           newclasses,
                                           newXsds,
                                           oldTypes,
                                           newTypes,
                                           apiOrSpi,
                                           vr);
            }
            // We have scanned the whole packages, check to see whether we need to increase minor version
            String oldVersion = oldPkg.getPackage().getVersion();
            String newVersion = newPkg.getPackage().getVersion();
            //TODO: support version overrides.
            if (majorChange.isChange()) {
                fatal_package = newPkg.getPackage();

                VersionCheck vc = new VersionCheck(VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion, newVersion);

                if (vc.isCorrect()) {
                    majorChange.setChange(false);
                    if (!splitCompare) {
                        XmlErrorCollator.addReport(fatal_package.toString(),
                                                   "",
                                                   ReportType.INFO,
                                                   "Updated package version for package " + fatal_package,
                                                   "[MAJOR_PACKAGE_CHANGE " + fatal_package + "]",
                                                   "The package "
                                                                                                   + fatal_package
                                                                                                   + " has had major changes made within it, and its version has been updated correctly. The baseline knew the package at version "
                                                                                                   + oldVersion
                                                                                                   + " and the package is now known at version " + newVersion + ". "
                                                                                                   + vr.majorChange.getReason());
                    }
                } else {
                    //otherwise
                    if (!splitCompare) {
                        if (GlobalConfig.getReleaseMode().equals("IFIX")) {
                            XmlErrorCollator.addReport(fatal_package.toString(),
                                                       "",
                                                       ReportType.ERROR,
                                                       "Incorrect package version for package " + fatal_package,
                                                       "[BAD_PACKAGE_VERSION " + fatal_package + "]",
                                                       "The package "
                                                                                                      + fatal_package
                                                                                                      + " has changed version. Package version changes are not allowed in the ifix stream. The baseline knows the package at version "
                                                                                                      + oldVersion
                                                                                                      + " Revert the version of the package from its current version " + newVersion
                                                                                                      + " to "
                                                                                                      + oldVersion);
                        } else {
                            XmlErrorCollator.addReport(fatal_package.toString(),
                                                       "",
                                                       ReportType.ERROR,
                                                       "Incorrect package version for package " + fatal_package,
                                                       "[BAD_PACKAGE_VERSION " + fatal_package + "]",
                                                       "Changes were detected in package "
                                                                                                      + fatal_package
                                                                                                      + " which mean the package version is now incorrect. The baseline knows the package at version "
                                                                                                      + oldVersion
                                                                                                      + " Change the version of the package from its current version "
                                                                                                      + newVersion
                                                                                                      + " to "
                                                                                                      + getRecommendedVersion(VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion)
                                                                                                      + " Remember!! Major Changes in CD Open stream require POC Approval. If you do not have this, do not proceed with the changes that lead to this error.");

                        }
                    }
                }

                if (GlobalConfig.ApiSpiReviewMode) {
                    if (!splitCompare) {
                        XmlErrorCollator.addReport(majorChange.changeClass,
                                                   "",
                                                   ReportType.ERROR,
                                                   //summary
                                                   "(Major) Package Compare detail for " + fatal_package,
                                                   //shortText
                                                   "[PACKAGE_API_SPI_REVIEW_DETAIL " + fatal_package + "]",
                                                   //reason
                                                   "" + vr.majorChange.getReason() + "\n" + vc.toString());
                    }
                }

            } else if (minorChange.isChange()) { //&& (!pkgName.startsWith(thirdpartyPkgsPrefix))) { //we filter 3rd party when selecting packages
                if (fatal_package == null)
                    fatal_package = newPkg.getPackage();

                //original here used to have option to
                // listAllChanges - will always addReport
                VersionCheck vc = new VersionCheck(VERSION_CHANGE_TYPE.MINOR_CHANGE, oldVersion, newVersion);
                if (vc.isCorrect()) {
                    minorChange.setChange(false);
                    if (!splitCompare) {
                        XmlErrorCollator.addReport(fatal_package.toString(),
                                                   "",
                                                   ReportType.INFO,
                                                   "Updated package version for package " + fatal_package,
                                                   "[MINOR_PACKAGE_CHANGE " + fatal_package + "]",
                                                   "The package "
                                                                                                   + fatal_package
                                                                                                   + " has had minor changes made within it, and it's version has been updated correctly. The baseline knew the package at version "
                                                                                                   + oldVersion
                                                                                                   + " and the package is now known at version " + newVersion + ". "
                                                                                                   + vr.minorChange.getReason());
                    }
                } else {
                    if (!splitCompare) {
                        if (GlobalConfig.getReleaseMode().equals("IFIX")) {
                            //otherwise
                            XmlErrorCollator.addReport(fatal_package.toString(),
                                                       "",
                                                       ReportType.ERROR,
                                                       "Incorrect package version for package " + fatal_package,
                                                       "[BAD_PACKAGE_VERSION " + fatal_package + "]",
                                                       "The package "
                                                                                                      + fatal_package
                                                                                                      + " whas changed version. Package version changes are not allowed in the ifix stream. The baseline knows the package at version "
                                                                                                      + oldVersion
                                                                                                      + " Revert the version of the package from its current version " + newVersion
                                                                                                      + " to "
                                                                                                      + oldVersion);
                        } else {
                            //otherwise
                            XmlErrorCollator.addReport(fatal_package.toString(),
                                                       "",
                                                       ReportType.ERROR,
                                                       "Incorrect package version for package " + fatal_package,
                                                       "[BAD_PACKAGE_VERSION " + fatal_package + "]",
                                                       "Changes were detected in package " + fatal_package
                                                                                                      + " which mean the package version is now incorrect. The baseline knows the package at version "
                                                                                                      + oldVersion
                                                                                                      + " Change the version of the package from its current version " + newVersion
                                                                                                      + " to "
                                                                                                      + getRecommendedVersion(VERSION_CHANGE_TYPE.MINOR_CHANGE, oldVersion));

                        }
                    }
                }

                if (GlobalConfig.ApiSpiReviewMode) {
                    if (!splitCompare) {
                        XmlErrorCollator.addReport(majorChange.changeClass,
                                                   "",
                                                   ReportType.ERROR,
                                                   //summary
                                                   "(Minor) Package Compare detail for " + fatal_package,
                                                   //shortText
                                                   "[PACKAGE_API_SPI_REVIEW_DETAIL " + fatal_package + "]",
                                                   //reason
                                                   "" + vr.minorChange.getReason() + "\n" + vc.toString());
                    }
                }

            } else {
                VersionCheck vc = new VersionCheck(VERSION_CHANGE_TYPE.NO_CHANGE, oldVersion, newVersion);
                if (!vc.isCorrect()) {
                    if (fatal_package == null)
                        fatal_package = newPkg.getPackage();
                    if (!splitCompare) {
                        XmlErrorCollator.addReport(pkgName, "", ReportType.ERROR, "Incorrect package version for package " + fatal_package, "[BAD_PACKAGE_VERSION " + fatal_package
                                                                                                                                            + "]",
                                                   "No major or minor version changes are made to the package " + pkgName
                                                                                                                                                   + ". The package version should remain at "
                                                                                                                                                   + oldVersion + ".");
                    }
                }
            }
        } else {
            //we didn't get as far as comparing the xsd's since we already had a major package version change identified to report..
            String oldVersion = oldPkg.getPackage().getVersion();
            String newVersion = newPkg.getPackage().getVersion();
            fatal_package = newPkg.getPackage();

            //original here used to have option to
            // listAllChanges - will always addReport
            // release!=main  - will always addReport

            VersionCheck vc = new VersionCheck(VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion, newVersion);
            if (vc.isCorrect()) {
                majorChange.setChange(false);
                if (!splitCompare) {
                    XmlErrorCollator.addReport(fatal_package.toString(),
                                               "",
                                               ReportType.INFO,
                                               "Updated package version for package " + fatal_package,
                                               "[MAJOR_PACKAGE_CHANGE " + fatal_package + "]",
                                               "The package "
                                                                                               + fatal_package
                                                                                               + " has had major changes made within it, and it's version has been updated correctly. The baseline knew the package at version "
                                                                                               + oldVersion
                                                                                               + " and the package is now known at version " + newVersion + ". "
                                                                                               + vr.majorChange.getReason());
                }

            } else {
                if (!splitCompare) {
                    //otherwise
                    XmlErrorCollator.addReport(fatal_package.toString(),
                                               "",
                                               ReportType.ERROR,
                                               "Incorrect package version for package " + fatal_package,
                                               "[BAD_PACKAGE_VERSION " + fatal_package + "]",
                                               "Changes were detected in package "
                                                                                              + fatal_package
                                                                                              + " which mean the package version is now incorrect. The baseline knows the package at version "
                                                                                              + oldVersion
                                                                                              + " Change the version of the package from its current version "
                                                                                              + newVersion
                                                                                              + " to "
                                                                                              + getRecommendedVersion(VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion)
                                                                                              + " Remember!! Major Changes in CD Open stream require POC Approval. If you do not have this, do not proceed with the changes that lead to this error.");
                }
            }

            if (GlobalConfig.ApiSpiReviewMode) {
                if (!splitCompare) {
                    XmlErrorCollator.addReport(majorChange.changeClass,
                                               "",
                                               ReportType.ERROR,
                                               //summary
                                               "(Major) Package Compare detail for " + fatal_package,
                                               //shortText
                                               "[PACKAGE_API_SPI_REVIEW_DETAIL " + fatal_package + "]",
                                               //reason
                                               "" + vr.majorChange.getReason() + "\n" + vc.toString());
                }
            }

        }

        return vr;
    }

    /**
     * Original code from api checker.
     *
     * @param status
     * @param oldVersionStr
     * @return
     */
    private Version getRecommendedVersion(VERSION_CHANGE_TYPE status, String oldVersionStr) {
        Version oldVersion = Version.parseVersion(oldVersionStr);
        Version recommendedVersion = Version.parseVersion(oldVersionStr);
        if (status == VERSION_CHANGE_TYPE.MAJOR_CHANGE) {
            recommendedVersion = new Version(oldVersion.getMajor() + 1, 0, 0);
        } else if (status == VERSION_CHANGE_TYPE.MINOR_CHANGE) {
            recommendedVersion = new Version(oldVersion.getMajor(), oldVersion.getMinor() + 1, 0);
        } else if (status == VERSION_CHANGE_TYPE.NO_CHANGE) {
            recommendedVersion = oldVersion;
        } else if (status == VERSION_CHANGE_TYPE.NEW_PACKAGE) {
            recommendedVersion = Version.parseVersion(SemanticVersioningUtils.WAS_PACKAGE_INITIAL_VERSION);
        }
        if (recommendedVersion.getMajor() == 0) {
            recommendedVersion = Version.parseVersion(SemanticVersioningUtils.WAS_PACKAGE_INITIAL_VERSION);
        }
        return recommendedVersion;
    }

    /**
     * Visit the whole package to scan each class to see whether we need to log minor or major changes.
     *
     * @param pkgName
     * @param baseClazz
     * @param newClazz
     * @param majorChange
     * @param minorChange
     */
    private void visitPackage(Set<FeatureInfo> features, String pkgName,
                              Map<String, ClassDeclaration> baseClazz,
                              Map<String, ClassDeclaration> newClazz,
                              Set<String> oldTypes,
                              Set<String> newTypes,
                              String apiOrSpi,
                              VersionResult result) {
        StringBuilder major_reason = new StringBuilder();
        StringBuilder minor_reason = new StringBuilder();
        boolean is_major_change = false;
        boolean is_minor_change = false;
        String fatal_class = null;
        boolean foundNewAbstract = false;

        if (features.isEmpty()) {
            System.err.println("ASKED TO COMPARE " + pkgName + " for empty feature set!!");
            Exception e = new Exception();
            e.printStackTrace();

            return;
        }

        //iterate the base classes..
        for (Map.Entry<String, ClassDeclaration> baseEntry : baseClazz.entrySet()) {

            // we are iterating the old classes, so we don't want to invoke isExcluded on classes that have been removed, because
            // doing so will lead to errors about not finding the api/spi jar for the class.
            if (newClazz.containsKey(baseEntry.getKey())) {
                // skip this class if it is excluded (and ignore the inner class if the outer class is excluded).
                if (isExcluded(resourceNameToExclusionTestName(baseEntry.getKey() + ".class", true), features, newTypes, apiOrSpi, "")) {
                    continue;
                }
            } else {
                //base class is not in the new classes set.. we'll handle this as a major change below.
            }

            String fullyQualifiedNameOfClassBeingEvaluated = baseEntry.getValue().getName();

            // skip the property files as they are compiled as class file as well
            ClassDeclaration baseClassDeclaration = baseEntry.getValue();
            ClassDeclaration newClassDeclaration = newClazz.get(baseEntry.getKey());
            if ((baseClassDeclaration != null) && SemanticVersioningUtils.isPropertyFile(baseClassDeclaration)) {
                //the file is base was a property file, we'll skip it and perform no action.
            } else if (newClassDeclaration == null) {
                // the class we are scanning does not exist in the new build of the code.
                // because we only scan api/spi, this represents a breaking change.
                // (note that changing visibility so the scanner cannot see it results in same overall effect as deletion).
                // This should be a major increase
                major_reason.append(twoLineBreaks + "The class/interface " + getClassName(fullyQualifiedNameOfClassBeingEvaluated) +
                                    " has been deleted," +
                                    " or has been changed to private or package default visibility");
                is_major_change = true;
                // only replace the fatal class if not set as the class won't be found in cmvc due to the fact it has been deleted.
                if (fatal_class == null) {
                    fatal_class = fullyQualifiedNameOfClassBeingEvaluated;
                }

                System.out.println("Adding affected resource with Major change " + resourceNameToExclusionTestName(baseEntry.getKey() + ".class", true) + " : " + major_reason);
                result.affectedResources.put(resourceNameToExclusionTestName(baseEntry.getKey() + ".class", true), Change.MAJOR);
            } else {
                // The class exists in both old and new, so we shall check for binary compatibility
                // remove the class from the newClazz collection as we use it to track if extra classes have been added.
                newClazz.remove(baseEntry.getKey());

                // check for binary compatibility
                // If the class has been changed to private or pkg default access.
                BinaryCompatibilityStatus bcs = newClassDeclaration.getBinaryCompatibleStatus(baseClassDeclaration);

                if (!bcs.isCompatible()) {
                    major_reason.append(twoLineBreaks + "In the " + getClassName(fullyQualifiedNameOfClassBeingEvaluated)
                                        + " class or its supers, there are the following changes.");
                    // break binary compatibility
                    major_reason.append(bcs.getReason());
                    is_major_change = true;
                    fatal_class = fullyQualifiedNameOfClassBeingEvaluated;
                    System.out.println("Adding affected resource with Major change " + resourceNameToExclusionTestName(baseEntry.getKey() + ".class", true) + " : " + major_reason);
                    result.affectedResources.put(resourceNameToExclusionTestName(baseEntry.getKey() + ".class", true), Change.MAJOR);
                } else {
                    //check to see whether more methods are added
                    ClassDeclaration oldcd = baseClassDeclaration;
                    Collection<MethodDeclaration> extraMethods = newClassDeclaration.getExtraMethods(oldcd);

                    boolean containsConcrete = false;
                    boolean containsAbstract = false;

                    boolean abstractClass = newClassDeclaration.isAbstract();

                    StringBuilder subRemarks = new StringBuilder();
                    String concreteSubRemarks = null;
                    for (MethodDeclaration extraMethod : extraMethods) {
                        //only interested in the visible methods not the system generated ones
                        if (!extraMethod.getName().contains("$")) {
                            if (abstractClass) {
                                if (extraMethod.isAbstract()) {
                                    foundNewAbstract = true;
                                    containsAbstract = true;
                                    subRemarks.append(oneLineBreak + SemanticVersioningUtils.getReadableMethodSignature(extraMethod.getName(), extraMethod.getDesc()));
                                } else {
                                    //only list one abstract method, no need to list all
                                    containsConcrete = true;
                                    concreteSubRemarks = oneLineBreak
                                                         + SemanticVersioningUtils.getReadableMethodSignature(extraMethod.getName(), extraMethod.getDesc());
                                }
                            } else {
                                containsConcrete = true;
                                concreteSubRemarks = oneLineBreak + SemanticVersioningUtils.getReadableMethodSignature(extraMethod.getName(), extraMethod.getDesc());
                                break;
                            }
                        }
                    }

                    if (containsConcrete || containsAbstract) {
                        is_minor_change = true;
                        result.affectedResources.put(resourceNameToExclusionTestName(baseEntry.getKey() + ".class", true), Change.MINOR);
                        if (!is_major_change) {
                            fatal_class = fullyQualifiedNameOfClassBeingEvaluated;
                        }
                        if (containsAbstract) {

                            minor_reason.append(twoLineBreaks + "In the " + getClassName(fullyQualifiedNameOfClassBeingEvaluated)
                                                + " class or its supers, the following abstract methods have been added.");
                            minor_reason.append(subRemarks);
                        } else {
                            minor_reason.append(twoLineBreaks + "In the " + getClassName(fullyQualifiedNameOfClassBeingEvaluated)
                                                + " class or its supers, the following method has been added.");
                            minor_reason.append(concreteSubRemarks);
                        }
                        System.out.println("Adding affected resource with minor change " + resourceNameToExclusionTestName(baseEntry.getKey() + ".class", true) + " : "
                                           + minor_reason);
                    }
                    //check to see whether there are extra public/protected fields if there are no additional methods
                    if (!is_minor_change) {
                        for (FieldDeclaration field : newClassDeclaration.getExtraFields(oldcd)) {
                            if (field.isPublic() || field.isProtected()) {
                                is_minor_change = true;

                                result.affectedResources.put(resourceNameToExclusionTestName(baseEntry.getKey() + ".class", true), Change.MINOR);
                                String extraFieldRemarks = oneLineBreak + " " + SemanticVersioningUtils.transform(field.getDesc()) + " " + field.getName();
                                if (!is_major_change) {
                                    fatal_class = fullyQualifiedNameOfClassBeingEvaluated;
                                }
                                minor_reason.append(twoLineBreaks + "In the " + getClassName(fullyQualifiedNameOfClassBeingEvaluated)
                                                    + " class or its supers, the following fields have been added.");
                                minor_reason.append(extraFieldRemarks);

                                System.out.println("Adding affected resource with minor change " + resourceNameToExclusionTestName(baseEntry.getKey() + ".class", true) + " : "
                                                   + minor_reason);
                                break;
                            }
                        }

                    }

                }
            }

        }
        if (is_major_change) {
            result.majorChange.update(major_reason.toString(), fatal_class, null);
        }
        if (is_minor_change) {
            result.minorChange.update(minor_reason.toString(), fatal_class, (foundNewAbstract ? "true" : null));
        }
    }

    /**
     * Enum to represent the change type.
     * Originally from api checker.
     */
    enum VERSION_CHANGE_TYPE {
        MAJOR_CHANGE(SemanticVersioningUtils.MAJOR_CHANGE),
        MINOR_CHANGE(SemanticVersioningUtils.MINOR_CHANGE),
        NO_CHANGE(SemanticVersioningUtils.NO_CHANGE),
        NEW_PACKAGE(SemanticVersioningUtils.NEW_PACKAGE);
        private final String text;

        VERSION_CHANGE_TYPE(String text) {
            this.text = text;
        }

        public String text() {
            return this.text;
        }
    };

    /**
     * Class to encapsulate version change information
     * Originally from api checker.
     */
    public static class VersionChange {
        boolean change = false;
        String reason = null;
        String changeClass = null;
        String specialRemarks = null;

        public String getSpecialRemarks() {
            return specialRemarks;
        }

        public boolean isChange() {
            return change;
        }

        public void setChange(boolean change) {
            this.change = change;
        }

        public String getReason() {
            return reason;
        }

        public String getChangeClass() {
            return changeClass;
        }

        public void update(String reason, String changeClass, String specialRemarks) {
            this.change = true;
            this.reason = reason;
            this.changeClass = changeClass;
            this.specialRemarks = specialRemarks;
        }
    }

    /**
     * Check whether the package has gained additional class or xsd files. If yes, log a minor change.
     *
     * @param pkgName
     * @param curClazz
     * @param curXsds
     * @param minorChange
     */
    private void checkAdditionalClassOrXsds(Set<FeatureInfo> features,
                                            String pkgName,
                                            Map<String, ClassDeclaration> curClazz,
                                            Map<String, String> curXsds,
                                            Set<String> oldTypes,
                                            Set<String> newTypes,
                                            String apiOrSpi,
                                            VersionResult result) {
        String reason = null;
        StringBuilder abstractReason = new StringBuilder();
        Collection<ClassDeclaration> ifiles = curClazz.values();
        Iterator<ClassDeclaration> iterator = ifiles.iterator();
        String abstract_class = "";
        String concrete_class = "";
        while (iterator.hasNext()) {
            ClassDeclaration cd = iterator.next();
            String changeClass = cd.getName();
            String exclusionName = resourceNameToExclusionTestName(changeClass + ".class", true);
            // skip this class if it is excluded (and ignore the inner class if the outer class is excluded).
            if (isExcluded(exclusionName, features, newTypes, apiOrSpi, "")) {
                continue;
            }
            if ((!SemanticVersioningUtils.isPropertyFile(cd))) {
                // If this is a public/protected class, it will need to increase the minor version of the package.
                result.minorChange.setChange(true);
                result.affectedResources.put(exclusionName, Change.MINOR);
                if (cd.isAbstract() || cd.isInterface()) {
                    // list all abstract classes
                    abstract_class = changeClass;
                    String type = "abstract";
                    if (cd.isInterface()) {
                        type = "interface";
                    }
                    abstractReason.append(twoLineBreaks + "The " + type + " class " + getClassName(changeClass)
                                          + " has been added to the package.");
                } else {
                    // just display one class is enough.
                    concrete_class = changeClass;
                    reason = twoLineBreaks + "The class " + getClassName(changeClass) + " has been added to the package.";
                }
            }
        }
        if (result.minorChange.isChange()) {
            if (abstractReason.length() > 0) {
                result.minorChange.update(abstractReason.toString(), abstract_class, "true");
            } else {
                result.minorChange.update(reason, concrete_class, null);
            }
        }
        if (!!!(result.minorChange.isChange() || curXsds.isEmpty())) {
            /// a new xsd file was added, it is a minor change
            Entry<String, String> firstXsd = null;
            Iterator<Entry<String, String>> xsdIterator = curXsds.entrySet().iterator();
            firstXsd = xsdIterator.next();

            reason = twoLineBreaks + "The schema file(s) " + curXsds.keySet() + " have been added to the package.";
            result.minorChange.update(reason, firstXsd.getKey(), null);
            for (String xsdPath : curXsds.keySet()) {
                result.affectedResources.put(resourceNameToExclusionTestName(xsdPath, false), Change.MINOR);
            }
        }
    }

    /**
     * Convert a path to a .class file into a path to a .java file.
     *
     * @param fullClassPath
     * @return
     */
    private String getClassName(String fullClassPath) {
        String[] chunks = fullClassPath.split("/");
        String className = chunks[chunks.length - 1];
        className = className.replace(SemanticVersioningUtils.classExt, SemanticVersioningUtils.javaExt);
        return className;
    }

    /**
     * if resource is a .class, removes any innerclass portion of the name.
     * converts path seperated resource into package seperated resource.
     *
     * @param resourceName
     * @param removeInnerClassPart
     * @return
     */
    public static String resourceNameToExclusionTestName(String resourceName, boolean removeInnerClassPart) {
        boolean endsWithClass = resourceName.endsWith(".class");
        String className = resourceName.replace(".class", "").replaceAll("/", ".");
        if (removeInnerClassPart && className.contains("$")) {
            className = className.substring(0, className.lastIndexOf("$"));
        }
        String result = className + (endsWithClass ? ".class" : "");
        //System.out.println("  "+resourceName+" -> "+result);
        return result;
    }

    /**
     * Check whether the package has xsd file changes or deleted. If yes, log a minor change.
     *
     * @param pkgName
     * @param baseXsds
     * @param curXsds
     * @param majorChange
     * @throws IOException
     */
    private void checkXsdChangesInPkg(Set<FeatureInfo> features, String pkgName, Map<String, String> baseXsds,
                                      Map<String, String> curXsds, String apiOrSpi, VersionResult result) {
        String reason;
        for (Map.Entry<String, String> file : baseXsds.entrySet()) {
            // scan the latest version of the class
            String curXsdHash = curXsds.get(file.getKey());
            String changeClass = file.getKey();
            // check whether the xsd have been deleted or changed or added
            if (curXsdHash == null) {
                reason = twoLineBreaks + "The schema file " + file.getKey() + " has been deleted.";
                result.majorChange.update(reason, changeClass, null);
                result.affectedResources.put(resourceNameToExclusionTestName(changeClass, false), Change.MAJOR);
                break;
            } else {
                // check whether it is the same
                //read the current xsd file
                curXsds.remove(file.getKey());
                if (isExcluded(resourceNameToExclusionTestName(changeClass, false), features, null, apiOrSpi, "")) {
                    continue;
                }
                String oldXsdHash = file.getValue();
                if (!!!(curXsdHash.equals(oldXsdHash))) {
                    reason = twoLineBreaks + "The schema file " + file.getKey() + " has been updated.";
                    result.majorChange.update(reason, changeClass, null);
                    result.affectedResources.put(resourceNameToExclusionTestName(changeClass, false), Change.MINOR);
                    break;
                }
            }
        }
    }

    /**
     * Originally checked if a class was excluded from scanning by config, now tests if the class is known to be in a dev jar
     * for the features being scanned.
     *
     * If the class is not found in any associated dev jar, the class is excluded from scanning.
     *
     * @param classOrPackageName
     * @param features
     * @param newTypes
     * @param apiOrSpi
     * @param logStringHeader
     * @return
     */
    private boolean isExcluded(String classOrPackageName, Set<FeatureInfo> features, Set<String> newTypes, String apiOrSpi, String logStringHeader) {
        //still seeing empty features in the log.. hmm
        if (features.isEmpty()) {
            Exception e = new Exception("WHY NO FEATURES??");
            e.printStackTrace(System.out);
        }

        boolean debug = false; //causes isResourceKnown.. to dump lots of info..

        boolean isExcluded = true;
        Set<String> all = new HashSet<String>();
        for (FeatureInfo f : features) {
            all.add(f.getName());
            if (!f.isResourceKnownToAssociatedDevJar(classOrPackageName, debug).isEmpty()) {
                isExcluded = false;
                break;
            }
        }

        if (isExcluded) {
            //report excluded classes that were defined as api or spi or spec.
            //do not report excluded classes that were internal or third-party
            if (newTypes != null
                && (newTypes.isEmpty() || newTypes.toString().contains("ibm-api") || newTypes.toString().contains("ibm-spi") || newTypes.toString().contains("spec"))) {
                //log(logStringHeader + "the class or package " + classOrPackageName + " is excluded from scanning, is not known to any api/spi jar for features "+all);
                XmlErrorCollator.addReport(classOrPackageName,
                                           "",
                                           ReportType.WARNING,
                                           //summary
                                           "Unable to locate API/SPI jar for " + classOrPackageName,
                                           //shortText
                                           "[CANNOT_LOCATE_APISPI_JAR " + classOrPackageName + "]",
                                           //reason
                                           "The class or package " + classOrPackageName + " (declared as type " + apiOrSpi + ""
                                                                                                    + (apiOrSpi.contains("api") ? " with types[" + newTypes + "]" : "")
                                                                                                    + ") is excluded from scanning, is not known to any api/spi jar for aggregate feature set "
                                                                                                    + all);
            } else {
                XmlErrorCollator.addReport(classOrPackageName,
                                           "",
                                           ReportType.WARNING,
                                           //summary
                                           "Unable to locate API/SPI jar for " + classOrPackageName,
                                           //shortText
                                           "[CANNOT_LOCATE_APISPI_INTERNAL3RDPARTY_JAR " + classOrPackageName + "]",
                                           //reason
                                           "The class or package " + classOrPackageName + " (declared as type " + apiOrSpi + ""
                                                                                                                     + (apiOrSpi.contains("api") ? " with types[" + newTypes
                                                                                                                                                   + "]" : "")
                                                                                                                     + ") is excluded from scanning, is not known to any api/spi jar for aggregate feature set "
                                                                                                                     + all);

            }
        }

        return isExcluded;
    }

}
