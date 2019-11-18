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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.aries.buildtasks.semantic.versioning.model.ConstrainedFeatureSet;
import com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo;
import com.ibm.aries.buildtasks.semantic.versioning.model.PkgInfo;
import com.ibm.aries.buildtasks.semantic.versioning.model.ResolvedConstrainedFeature;
import com.ibm.ws.featureverifier.internal.FilterUtils.ErrorInfo;
import com.ibm.ws.featureverifier.internal.FilterUtils.ParseError;
import com.ibm.ws.featureverifier.internal.XmlErrorCollator.ReportType;

public class NoCompareFeatureValidator {

    public static void testFeaturesWithoutBaselineCompare(Collection<FeatureInfo> featuresToTest, Collection<FeatureInfo> allFeaturesFromDisk) {
        System.out.println("There are " + featuresToTest.size() + " features to check in the set");

        List<ResolvedConstrainedFeature> allPermutations = FeatureSetGenerator.getPermutationsForFeatureSet(featuresToTest);

        for (FeatureInfo feature : featuresToTest) {
            XmlErrorCollator.setCurrentFeature(feature.getName());
            XmlErrorCollator.setNestedFeature(feature.getName());

            System.out.println("No compare mode evaluating " + feature.getName());

            //report any issues found with the manifest.. 
            if (!feature.getIssuesFoundParsingManifest().isEmpty()) {
                for (FeatureInfo.ErrorInfo ei : feature.getIssuesFoundParsingManifest()) {
                    System.out.println(ei.shortText + " " + ei.detail);
                    XmlErrorCollator.addReport(feature.getName(), "", ei.type, ei.summary, ei.shortText, ei.detail);
                }
            }

            //check subsystem name prefix..
            if (!feature.getName().startsWith("com.ibm.websphere.appserver.")) {
                System.out.println("[INCORRECT_FEATURE_SYMBOLICNAME " + feature.getName() + "]");
                XmlErrorCollator.addReport(feature.getName(), "", ReportType.ERROR, "Feature Symbolic name does not start with correct prefix", "[INCORRECT_FEATURE_SYMBOLICNAME "
                                                                                                                                                + feature.getName() + "]",
                                           "Liberty Features are expected to start with com.ibm.websphere.appserver.");
            }

            //validate autofeature singleton tolerance.
            if (feature.getAutoFeature()) {
                try {
                    checkAutoFeatureSingletons(feature, featuresToTest, allPermutations);
                } catch (ParseError e) {
                    for (FilterUtils.ErrorInfo ei : e.eis) {
                        System.out.println(ei.shortText + " " + ei.detail);
                        XmlErrorCollator.addReport(feature.getName(), "", ei.type, ei.summary, ei.shortText, ei.detail);
                    }
                }
            }

            Set<String> apiTypes = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] { "api", "ibm-api", "internal", "spec", "spec:osgi", "third-party" })));
            Set<String> spiTypes = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] { "spi", "ibm-spi", "spec", "third-party" })));
            //new.. validate api/spi package prefixes, validate spi usage of type attribute.
            Map<PkgInfo, Set<Map<String, String>>> api = feature.getLocalAPI();
            for (Entry<PkgInfo, Set<Map<String, String>>> apiDeclaration : api.entrySet()) {
                boolean ok = true;
                //we only want to report failures for type=ibm-api
                for (Map<String, String> declarationAttribs : apiDeclaration.getValue()) {
                    String type = declarationAttribs.containsKey("type") ? declarationAttribs.get("type") : null;
                    if (type != null && type.equals("ibm-api")) {
                        if (!apiDeclaration.getKey().getName().startsWith("com.ibm.websphere.")) {
                            ok = false;
                        }
                    } else {
                        //if it had a type and it wasnt ibm-api .. lets validate we know the type.
                        if (type != null && !apiTypes.contains(type)) {
                            XmlErrorCollator.addReport(feature.getName(),
                                                       "",
                                                       ReportType.ERROR,
                                                       "API Package declared with unknown type.",
                                                       "[UNKNOWN_IBM_API_TYPE_VALUE "
                                                                       + apiDeclaration.getKey() + "]",
                                                       "Liberty API Packages may only be declared with types " + apiTypes + " The type encountered '"
                                                                       + type
                                                                       + "' is not recognised");
                        }
                    }
                }
                if (!ok) {
                    XmlErrorCollator.addReport(feature.getName(), "", ReportType.ERROR, "API Package name does not start with correct prefix",
                                               "[INCORRECT_API_PACKAGE_PREFIX "
                                                               + apiDeclaration.getKey() + "]",
                                               "Liberty API Packages are expected to start with 'com.ibm.websphere.' unless they have been granted an exception.");
                }
            }
            Map<PkgInfo, Set<Map<String, String>>> spi = feature.getLocalSPI();
            for (Entry<PkgInfo, Set<Map<String, String>>> spiDeclaration : spi.entrySet()) {
                boolean ok = true;
                for (Map<String, String> declarationAttribs : spiDeclaration.getValue()) {
                    String type = declarationAttribs.containsKey("type") ? declarationAttribs.get("type") : null;
                    if (type == null || (type.equals("ibm-spi") || type.equals("spi"))) {
                        if (!spiDeclaration.getKey().getName().startsWith("com.ibm.wsspi.")) {
                            ok = false;
                        }
                    } else {
                        //if the type isn't set, or it's unknown.. 
                        if (type != null && !spiTypes.contains(type)) {
                            XmlErrorCollator.addReport(feature.getName(),
                                                       "",
                                                       ReportType.ERROR,
                                                       "SPI Package declared with unknown type.",
                                                       "[UNKNOWN_IBM_SPI_TYPE_VALUE "
                                                                       + spiDeclaration.getKey() + "]",
                                                       "Liberty API Packages may only be declared with types " + apiTypes + " The type encountered '"
                                                                       + type
                                                                       + "' is not recognised");
                        }
                    }
                }
                if (!ok) {
                    XmlErrorCollator.addReport(feature.getName(), "", ReportType.ERROR, "SPI Package name does not start with correct prefix",
                                               "[INCORRECT_SPI_PACKAGE_PREFIX "
                                                               + spiDeclaration.getKey() + "]",
                                               "Liberty SPI Packages are expected to start with 'com.ibm.wsspi.' unless they have been granted an exception.");
                }
            }

            //new.. validate that private features do not declare api/spi (result of design discussion, to be enforced for all new features)
            if ("private".equals(feature.getVisibility())) {
                if (api.size() > 0 || spi.size() > 0) {
                    XmlErrorCollator.addReport(feature.getName(),
                                               "",
                                               ReportType.ERROR,
                                               "Private features should not declare api/spi packages",
                                               "[PRIVATE_API_SPI_DECLARATION "
                                                               + feature.getName() + "]",
                                               "Features with private visibility should not use IBM-API-Package or IBM-SPI-Package to declare api/spi. They may supply the jars holding the classes, but the actual api/spi declarations should be made via public/protected features. Api: "
                                                               + api.keySet() + " Spi: " + spi.keySet());
                }
            }
        }
    }

    private static void checkAutoFeatureSingletons(FeatureInfo autofeature,
                                                   Collection<FeatureInfo> featuresToTest, Collection<ResolvedConstrainedFeature> allPermutations) throws ParseError {
        //if feature is an autofeature.. 
        //  "every autofeature should for each singleton choice present from their trigger features
        //     either 
        //         tolerate all versions of the singleton choice, 
        //     or 
        //         bind the singleton choice within the provision-capability header"
        //extract the features this autofeature is depending on..
        Set<String> filters = AutoFeatureUtils.getFiltersForAutoFeatureHeader(autofeature);

        //build a quick map of feature name to feature info..
        Map<String, FeatureInfo> nameToFeatureMap = FeatureInfoUtils.createNameToFeatureMap(featuresToTest);

        //obtain the permutations for this autofeature.. 
        //note this only expands possible permutations based on the set 'featuresToTest'
        //so if the autofeature supports additional permutations possible only with features not in the 
        //featuresToTest list, then those permutations will not be returned. 
        //this allows autofeatures to reference unreleased features, without failing the expansion.
        //if we wish to warn of references outside the set, this can be done as a new test.
        List<ResolvedConstrainedFeature> autoFeaturePermutations = FeatureSetGenerator.getPermutationsForFeature(autofeature, featuresToTest);

        Set<Set<String>> triggerSets = FilterUtils.parseFilters(filters);

        //now check each possible trigger feature set, and make sure there is a compatible auto feature permutation for it.
        List<ErrorInfo> errors = new ArrayList<ErrorInfo>();
        for (Set<String> triggerSet : triggerSets) {
            List<List<ResolvedConstrainedFeature>> expandedTriggerPermutations = new ArrayList<List<ResolvedConstrainedFeature>>();
            for (String triggerFeatureName : triggerSet) {
                FeatureInfo triggerFeature = nameToFeatureMap.get(triggerFeatureName);
                if (triggerFeature != null) {
                    List<ResolvedConstrainedFeature> triggerExpansions = FeatureSetGenerator.getPermutationsForFeature(triggerFeature, featuresToTest);
                    expandedTriggerPermutations.add(triggerExpansions);
                }
            }

            //find the viable combinations of triggerFeature permutations.. 
            List<ConstrainedFeatureSet> viableCombinations = getViableCombinationsOfRCFs(expandedTriggerPermutations, 0);

            //test if any of the autofeature permutations is compatible with each viable combination of triggers.
            for (ConstrainedFeatureSet triggerCombination : viableCombinations) {
                boolean foundViableMatch = false;
                for (ResolvedConstrainedFeature autoFeaturePermutation : autoFeaturePermutations) {
                    if (triggerCombination.isConstrainedFeatureCompatible(autoFeaturePermutation)) {
                        foundViableMatch = true;
                        break;
                    }
                }
                if (!foundViableMatch) {
                    String autofeaturePerms = "";
                    for (ResolvedConstrainedFeature autoFeaturePermutation : autoFeaturePermutations) {
                        autofeaturePerms += " autofeature singleton permutation : " + autoFeaturePermutation + "\r\n";
                    }
                    ErrorInfo ei = new ErrorInfo(ReportType.ERROR, "[UNSATISFIED_AUTOFEATURE_TRIGGER " + autofeature.getName() + " ]",
                                    "Unsatisfied trigger combination " + triggerSet + " for autofeature " + autofeature.getName(),
                                    "Checker found that the combination of "
                                                    + triggerSet
                                                    + " produces a combination of singleton choices that the autofeature is incompatible with "
                                                    + triggerCombination
                                                    + "\r\n  Compare these autofeature permutations against the above trigger set choices, to see which singleton choice(s) are the issue.\r\n"
                                                    + autofeaturePerms);
                    errors.add(ei);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ParseError(errors.toArray(new ErrorInfo[] {}));
        }

    }

    private static List<ConstrainedFeatureSet> getViableCombinationsOfRCFs(List<List<ResolvedConstrainedFeature>> expandedTriggerPermutations, int depth) {
        List<ConstrainedFeatureSet> results = new ArrayList<ConstrainedFeatureSet>();
        if (expandedTriggerPermutations.isEmpty()) {
            return results;
        }
        System.out.println("Checking " + depth + " against " + (expandedTriggerPermutations.size() - 1) + " total sets " + expandedTriggerPermutations.size());
        if (depth == (expandedTriggerPermutations.size() - 1)) {
            //terminal case.. return our set of rcf's back.
            for (ResolvedConstrainedFeature rcf : expandedTriggerPermutations.get(depth)) {
                ConstrainedFeatureSet cfs = new ConstrainedFeatureSet();
                if (cfs.addConstrainedFeature(rcf)) {
                    results.add(cfs);
                }
            }
        } else {
            int newDepth = depth + 1;
            List<ConstrainedFeatureSet> fromOtherRCFs = getViableCombinationsOfRCFs(expandedTriggerPermutations, newDepth);
            //now merge our rcf's to the cfs from below us by depth.. and create new cfs to return back..
            for (ResolvedConstrainedFeature rcf : expandedTriggerPermutations.get(depth)) {
                for (ConstrainedFeatureSet cfe : fromOtherRCFs) {
                    //clone this cfe.. 
                    ConstrainedFeatureSet cfeclone = new ConstrainedFeatureSet();
                    cfeclone.features.addAll(cfe.features);
                    cfeclone.chosenSingletons.putAll(cfe.chosenSingletons);
                    //attempt to add this rcf to this clone.. 
                    if (cfeclone.addConstrainedFeature(rcf)) {
                        results.add(cfeclone);
                    }
                }
            }
        }
        return results;
    }
}
