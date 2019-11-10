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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;

import com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo;
import com.ibm.ws.featureverifier.internal.FilterUtils.ParseError;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

public class FeatureInfoUtils {

    public static Map<String, FeatureInfo> createNameToFeatureMap(
                                                                  Collection<FeatureInfo> allInstalledFeatures) {
        Map<String, FeatureInfo> nameToFeatureMap = new HashMap<String, FeatureInfo>();

        for (FeatureInfo f : allInstalledFeatures) {
            nameToFeatureMap.put(f.getName(), f);
            if (f.getShortName() != null) {
                nameToFeatureMap.put(f.getShortName(), f);
            }
        }
        return nameToFeatureMap;
    }

    public static Collection<FeatureInfo> filterFeaturesByEdition(String editionName, List<FeatureInfo> allFeatures, BundleContext context) {
        return filterFeaturesByEdition(editionName, allFeatures, context, true);
    }

    public static Collection<FeatureInfo> filterFeaturesByEdition(String editionName, List<FeatureInfo> allFeatures, BundleContext context, boolean errorOnMissing) {
        //build a map of feature symbname/short name to feature info
        Map<String, FeatureInfo> nameToFeatureMap = new HashMap<String, FeatureInfo>();
        for (FeatureInfo fi : allFeatures) {
            nameToFeatureMap.put(fi.getName(), fi);
            if (fi.getShortName() != null) {
                nameToFeatureMap.put(fi.getShortName(), fi);
            }
        }

        //read the edition xml, and process the includes
        //resulting in a set of String, that are the specified features to include.
        WsLocationAdmin wla = context.getService(context.getServiceReference(WsLocationAdmin.class));
        String outputDirString = wla.resolveString(WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR);
        File outputDir = new File(outputDirString);
        File editionsDir = new File(outputDir, "editions");
        File chosenEditionDir = new File(editionsDir, editionName);
        System.out.println("Attempting to load edition info from " + chosenEditionDir.getAbsolutePath() + " exists? " + chosenEditionDir.exists());
        Set<String> editionFeatureNames = new HashSet<String>();
        editionFeatureNames.addAll(Arrays.asList(FeatureVerifier.KERNEL_FEATURES));
        try {
            File runtimexml = new File(chosenEditionDir, "runtime.xml");
            if (runtimexml.exists()) {
                Set<String> runtime = EditionXmlUtils.obtainFeatureNamesFromXml(runtimexml);
                editionFeatureNames.addAll(runtime);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse runtime edition ", e);
        }
        try {
            File extendedxml = new File(chosenEditionDir, "extended.xml");
            if (extendedxml.exists()) {
                Set<String> runtime = EditionXmlUtils.obtainFeatureNamesFromXml(extendedxml);
                editionFeatureNames.addAll(runtime);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse runtime edition ", e);
        }
        try {
            File repoxml = new File(chosenEditionDir, "repo.xml");
            if (repoxml.exists()) {
                Set<String> repo = EditionXmlUtils.obtainFeatureNamesFromXml(repoxml);
                editionFeatureNames.addAll(repo);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse repo edition ", e);
        }

        System.out.println("##Features from xml agg for edition '" + editionName + "' " + editionFeatureNames);

        Set<FeatureInfo> results = new HashSet<FeatureInfo>();
        Set<FeatureInfo> alreadyDone = new HashSet<FeatureInfo>();
        //query the map to obtain each feature specified
        for (String editionFeature : editionFeatureNames) {
            FeatureInfo fi = nameToFeatureMap.get(editionFeature);

            if (fi == null) {
                //editionFeatureNames can come in with the 'wrong' casing.
                for (FeatureInfo test : nameToFeatureMap.values()) {
                    if (test.getName().equalsIgnoreCase(editionFeature) || (test.getShortName() != null && test.getShortName().equalsIgnoreCase(editionFeature))) {
                        fi = test;
                        break;
                    }
                }
                if (fi == null && errorOnMissing) {
                    throw new IllegalStateException("Unable to locate feature " + editionFeature + " for edition " + editionName);
                }
            }

            if (fi != null) {
                //obtain the aggregate feature set for each feature obtained from the map
                //(can't use fi.getAggregateFeatureSet() here, because the thats the 
                Set<FeatureInfo> agg = expandFeature(fi, nameToFeatureMap, alreadyDone, false);

                Set<String> atotalSet = new HashSet<String>();
                for (FeatureInfo afi : agg) {
                    atotalSet.add(afi.getName());
                }
                System.out.println("##Expanded '" + editionFeature + "' to set " + atotalSet);

                //add the aggregates to the overall set of FeatureInfo being returned. 
                results.addAll(agg);
            } else {
                if (!errorOnMissing) {
                    System.out.println("##Missing '" + editionFeature + "' from edition " + editionName);
                    GlobalConfig.addFeatureToIgnore(editionFeature);
                }
            }
        }

        //ok.. now we have the set of features available.. we need to go back through, and evaluate which autofeatures
        //may be required.. 
        for (FeatureInfo feature : allFeatures) {
            if (feature.getAutoFeature()) {
                Set<String> filters = AutoFeatureUtils.getFiltersForAutoFeatureHeader(feature);
                try {
                    Set<Set<String>> features = FilterUtils.parseFilters(filters);

                    for (Set<String> featureSet : features) {
                        boolean allKnown = true;
                        for (String featureToTest : featureSet) {
                            FeatureInfo fi = nameToFeatureMap.get(featureToTest);
                            if (fi != null) {
                                if (!results.contains(fi) && !fi.getAutoFeature()) {
                                    allKnown = false;
                                }
                            }
                        }
                        if (allKnown) {
                            System.out.println("Expanding and adding autofeature " + feature.getName() + " to set because trigger features " + featureSet + " are known.");
                            Set<FeatureInfo> agg = expandFeature(feature, nameToFeatureMap, alreadyDone, false);
                            results.addAll(agg);
                            break;
                        }
                    }

                } catch (ParseError e) {
                    //ignore the errors here.. we'll have another chance to handle them later.
                }
            }
        }

        //a little trace for diag later.. 
        Set<String> totalSet = new HashSet<String>();
        for (FeatureInfo fi : results) {
            totalSet.add(fi.getName());
        }
        System.out.println("##Features for edition '" + editionName + "' " + totalSet);

        return results;
    }

    private static Set<FeatureInfo> expandFeature(FeatureInfo feature, Map<String, FeatureInfo> nameToFeatureMap, Set<FeatureInfo> alreadyDone, boolean processTolerates) {
        //we don't need to expand the tolerates, because they'll be in the list of features we have to expand.. 
        //and we assume each feature expands the same regardless of where we expand it. 
        Set<FeatureInfo> results = new HashSet<FeatureInfo>();
        //another feature may already have processed this one, if so, return empty.
        if (alreadyDone.contains(feature)) {
            //System.out.println(" - skipping "+feature.getName()+" as already processed once.");
            return results;
        }

        //System.out.println(" - processing "+feature.getName());
        results.add(feature);

        alreadyDone.add(feature);
        Map<String, String> nestedFeatureMap = feature.getContentFeatures();
        for (String nestedFeatureName : nestedFeatureMap.keySet()) {
            FeatureInfo nestedFeature = nameToFeatureMap.get(nestedFeatureName);
            if (nestedFeature == null) {
                throw new IllegalStateException("Unable to locate feature info for feature named " + nestedFeatureName);
            }
            //skip stuff we already processed.
            if (alreadyDone.contains(nestedFeature)) {
                //System.out.println("  - already met "+nestedFeatureName);
                continue;
            } else {
                //System.out.println("  - collecting nested "+nestedFeatureName);
                Set<FeatureInfo> nestedAggregation = expandFeature(nestedFeature, nameToFeatureMap, alreadyDone, processTolerates);
                //Set<String> atotalSet = new HashSet<String>();
                //for(FeatureInfo afi : nestedAggregation){
                //	atotalSet.add(afi.getName());
                //}
                //System.out.println("  - Expanded '"+nestedFeatureName+"' to set "+atotalSet);

                results.addAll(nestedAggregation);

                if (processTolerates) {
                    if (nestedFeature.getSingleton()) {
                        String tolerates = nestedFeatureMap.get(nestedFeatureName);
                        if (tolerates != null && tolerates.length() > 0) {
                            String name = nestedFeature.getName();
                            String setId = name.substring(0, name.lastIndexOf('-'));
                            String[] versions = tolerates.split(",");
                            for (String version : versions) {
                                String toleratedName = setId + "-" + version;
                                FeatureInfo tolerated = nameToFeatureMap.get(toleratedName);
                                if (tolerated != null) {
                                    Set<FeatureInfo> toleratedAggregation = expandFeature(nestedFeature, nameToFeatureMap, alreadyDone, processTolerates);
                                    results.addAll(toleratedAggregation);
                                }
                            }
                        }
                    }
                }
            }
        }

        //Set<String> atotalSet = new HashSet<String>();
        //for(FeatureInfo afi : results){
        //	atotalSet.add(afi.getName());
        //}
        //System.out.println(" - expanded "+feature.getName()+" to "+atotalSet);
        return results;
    }

}
