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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo;
import com.ibm.aries.buildtasks.semantic.versioning.model.ResolvedConstrainedFeature;
import com.ibm.aries.buildtasks.semantic.versioning.model.SingletonChoice;
import com.ibm.aries.buildtasks.semantic.versioning.model.SingletonSetId;
import com.ibm.aries.buildtasks.semantic.versioning.model.UnresolvedConstrainedFeature;

public class FeatureExpander {

    /**
     * Called by the top level recursion routine, with each permutation of the singleton choices
     * from the root feature.
     * 
     * The ResolvedConstrainedFeature contains the root feature with the singleton choices in effect for this permutation.
     * 
     * This function has to take the root feature, and expand it, with the singleton choices acting as constraints.
     * 
     * This expansion may result in additional singletons becoming bound.
     * 
     * The results are returned as new instances of ResolvedConstrainedFeature, each representing a valid expansion
     * of this permutation, that complies with the constraints.
     * 
     * @param permutation
     * @param results
     * @param nameToFeatureMap
     */
    private static void validateAndExtendPermutation(
                                                     ResolvedConstrainedFeature permutation,
                                                     Collection<ResolvedConstrainedFeature> results,
                                                     Map<String, FeatureInfo> nameToFeatureMap) {
        //if the root feature is a singleton itself, we must test if it was a preferred choice 
        //by checking the choices within the permutation.
        boolean isPreferred = false;
        if (permutation.feature.getSingleton()) {
            SingletonSetId setId = new SingletonSetId(permutation.feature);
            if (permutation.choices.containsKey(setId) && permutation.choices.get(setId).isPreferred()) {
                isPreferred = true;
            }
        }
        List<ResolvedConstrainedFeature> expandedFeature = expandBranchApplyingConstraints(permutation, nameToFeatureMap, permutation.feature, isPreferred, 0);

        results.addAll(expandedFeature);
    }

    /**
     * Simple recursive routine that walks through every combination of singleton choices supported by the UnresolvedConstrainedFeature.
     * Each permutation is then passed to validateAndExtendPermutation for processing.
     * 
     * @param root
     * @param nameToFeatureMap
     * @param results
     */
    private static void generatePermutations(UnresolvedConstrainedFeature root, Map<String, FeatureInfo> nameToFeatureMap, Collection<ResolvedConstrainedFeature> results) {
        if (root.noChoicesRemainInThisUCF()) {
            Map<SingletonSetId, SingletonChoice> choices = new HashMap<SingletonSetId, SingletonChoice>();
            for (Entry<SingletonSetId, Set<SingletonChoice>> entry : root.choices.entrySet()) {
                choices.put(entry.getKey(), entry.getValue().iterator().next());
            }
            ResolvedConstrainedFeature rcf = new ResolvedConstrainedFeature(root.feature, choices);
            //all choices are bound.. invoke validateAndExtendPermutation to fluff up this permutation into viable results.
            validateAndExtendPermutation(rcf, results, nameToFeatureMap);
        } else {
            //choices still remain.. we find the first choice with multiple options, and iterate it, calling ourselves again
            //with each option that is possible.
            SingletonSetId key = null;
            for (Map.Entry<SingletonSetId, Set<SingletonChoice>> e : root.choices.entrySet()) {
                //find the first choice in the set with more than one selection available, or that we have not bound yet..
                if (e.getValue().size() > 1) {
                    key = e.getKey();
                    break;
                }
            }
            //clone the choices except the one we plan to iterate.
            Map<SingletonSetId, Set<SingletonChoice>> childChoices = new HashMap<SingletonSetId, Set<SingletonChoice>>();
            for (Map.Entry<SingletonSetId, Set<SingletonChoice>> e : root.choices.entrySet()) {
                if (!e.getKey().equals(key)) {
                    childChoices.put(e.getKey(), e.getValue());
                }
            }
            //iterate the selected singleton.
            for (SingletonChoice choice : root.choices.get(key)) {
                String featureName = choice.getChoiceFeatureName();
                //only process choices that exist in our feature set.
                if (nameToFeatureMap.containsKey(featureName)) {
                    Set<SingletonChoice> chosenSingleton = new HashSet<SingletonChoice>();
                    chosenSingleton.add(choice);
                    childChoices.put(key, chosenSingleton);
                    UnresolvedConstrainedFeature ucf = new UnresolvedConstrainedFeature(childChoices, root.feature);
                    generatePermutations(ucf, nameToFeatureMap, results);
                }
            }

        }
    }

    /**
     * Small routine to check a current set of singleton choices, against those possible in a child feature.
     * 
     * Singletons that have no statement made in the child feature, are removed, which will force preferred only
     * processing.
     * 
     * @param existingChoices The map to evaluate AND remove choices from
     * @param forFeature The child feature against which to evaluate
     * @param nameToFeatureMap The name to feature map, required for processing the child feature.
     */
    private static void refineSingletonChoices(Map<SingletonSetId, SingletonChoice> existingChoices, FeatureInfo forFeature, Map<String, FeatureInfo> nameToFeatureMap) {
        Set<SingletonSetId> keysToRemove = new HashSet<SingletonSetId>();
        UnresolvedConstrainedFeature ucf = new UnresolvedConstrainedFeature(forFeature, nameToFeatureMap);
        for (Entry<SingletonSetId, SingletonChoice> c : existingChoices.entrySet()) {
            if (ucf.choices.containsKey(c.getKey())) {
                //new feature knows of this key.. this is fine
                //it will either support the chosen value, or not.. that will be determined when we process forFeature 
            } else {
                //new feature does not know of this key
                //we shall remove the key from the set.. 
                //if children of the new feature then attempt to use this key, they will be restricted to their 
                //preferred version.
                keysToRemove.add(c.getKey());
            }
        }
        for (SingletonSetId id : keysToRemove) {
            existingChoices.remove(id);
        }
    }

    /**
     * Recursive routine that treats this permutation as a node in a tree,
     * and treats each nested feature within this permutation as a child node.
     * 
     * Each child node is expanded in turn, producing a set of results from each branch.
     * 
     * Then the results from each branch, are merged together to create the results for this node.
     * 
     * When considering child nodes that are singletons, if the singleton is constrained by the permutation
     * then only allow the version from the permutation. If the version from the permutation is unavailable
     * for the singleton at this node, then return an empty set indicating no viable permutations are possible
     * for this node.
     * 
     * For all other singletons not constrained by the permutation, if they are private, we aggregate results
     * for all preferred/tolerated versions of the singleton. If they are not private, we process only the
     * preferred version.
     * 
     * @param permutation
     * @param nameToFeatureMap
     * @param feature
     * @param isPreferred
     * @param expansionDepth
     * @return
     */
    private static List<ResolvedConstrainedFeature> expandBranchApplyingConstraints(
                                                                                    ResolvedConstrainedFeature permutation, Map<String, FeatureInfo> nameToFeatureMap,
                                                                                    FeatureInfo feature,
                                                                                    boolean isPreferred, int expansionDepth) {
        List<List<ResolvedConstrainedFeature>> branchResults = new ArrayList<List<ResolvedConstrainedFeature>>();
        //collect the branch permutations
        for (Map.Entry<String, String> featureBranch : feature.getContentFeatures().entrySet()) {
            String featureName = featureBranch.getKey();
            //is this featureName one of the singletons in the permutation?
            //we guess if this feature is private or not..  messy, but faster 
            //than attempting to resolve the feature to discover if it really is or not.
            SingletonSetId setId = new SingletonSetId(featureName, true);
            boolean singletonConstrainedByPermutation = false;
            boolean isAPreferredChoice = false;
            SingletonChoice choice = null;
            if (permutation.choices.containsKey(setId)) {
                choice = permutation.choices.get(setId);
                singletonConstrainedByPermutation = true;
            }
            //if choice is non null, then we recognised this nested feature as one of the 
            //singletons constrained by the permutation. 
            if (choice != null) {
                //have to check if the choice is a supported one for this branch
                //it could be we are constrained to use a version this branch does not offer.
                if (choice.isPreferred()) {
                    isAPreferredChoice = true;
                }
                boolean ok = false;
                //check the preferred.. 
                if (featureName.equals(choice.getChoiceFeatureName())) {
                    ok = true;
                    isAPreferredChoice = true;
                } else {
                    //check the tolerated
                    String tolerated = featureBranch.getValue();
                    if (tolerated != null) {
                        String toleratedVersions[] = tolerated.split(",");
                        for (String toleratedVersion : toleratedVersions) {
                            String toleratedFeatureName = setId.getSetFeatureNamePrefix() + "-" + toleratedVersion;
                            if (nameToFeatureMap.containsKey(toleratedFeatureName)) {
                                if (toleratedFeatureName.equals(choice.getChoiceFeatureName())) {
                                    ok = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!ok) {
                    //add an empty list of options for this branch, and skip the rest of the processing.
                    branchResults.add(new ArrayList<ResolvedConstrainedFeature>());
                    break;
                } else {
                    //the constrained singleton choice is known for this feature, so we must use it.
                    featureName = choice.getChoiceFeatureName();
                    isAPreferredChoice = choice.isPreferred();
                }
            }
            List<ResolvedConstrainedFeature> thisBranchResults = new ArrayList<ResolvedConstrainedFeature>();
            if (nameToFeatureMap.containsKey(featureName)) {
                FeatureInfo nestedfeature = nameToFeatureMap.get(featureName);
                //we now have a feature to process.. 
                //
                //feature might be a singleton, with tolerated alternatives.. if so, we need to walk 
                //the alternatives in some cases. 
                //this happens when.. 
                //  - the child feature is a singleton (else it won't have any alternatives to process at all)
                //  - and it's not already bound by the permutation (otherwise it's other options would just conflict anyway)
                //  - and it's visibility is private (if it's not private, we can only look at the other options if they are in the permutation)
                if (nestedfeature.getSingleton() && !singletonConstrainedByPermutation && "private".equals(nestedfeature.getVisibility())) {
                    //expand options and aggregate into thisBranchResults
                    //preferred only.. if the singleton isn't already constrained, we can only use the preferred option
                    Map<SingletonSetId, SingletonChoice> nestedChoice = new HashMap<SingletonSetId, SingletonChoice>();
                    nestedChoice.putAll(permutation.choices);
                    SingletonChoice preferredChoice = new SingletonChoice(featureName, true);
                    nestedChoice.put(setId, preferredChoice);
                    refineSingletonChoices(nestedChoice, nestedfeature, nameToFeatureMap);
                    ResolvedConstrainedFeature nestedpermutation = new ResolvedConstrainedFeature(nestedfeature, nestedChoice);
                    List<ResolvedConstrainedFeature> branchResult = expandBranchApplyingConstraints(nestedpermutation, nameToFeatureMap, nestedfeature, true, expansionDepth + 1);
                    thisBranchResults.addAll(branchResult);
                    //then any tolerated.. (only for private singletons!!)
                    String tolerated = featureBranch.getValue();
                    if (tolerated != null) {
                        String toleratedVersions[] = tolerated.split(",");
                        for (String toleratedVersion : toleratedVersions) {
                            String toleratedFeatureName = setId.getSetFeatureNamePrefix() + "-" + toleratedVersion;
                            if (nameToFeatureMap.containsKey(toleratedFeatureName)) {
                                FeatureInfo toleratedFeature = nameToFeatureMap.get(toleratedFeatureName);
                                Map<SingletonSetId, SingletonChoice> toleratedChoices = new HashMap<SingletonSetId, SingletonChoice>();
                                toleratedChoices.putAll(permutation.choices);
                                SingletonChoice toleratedChoice = new SingletonChoice(toleratedFeatureName, false);
                                toleratedChoices.put(setId, toleratedChoice);
                                refineSingletonChoices(toleratedChoices, toleratedFeature, nameToFeatureMap);
                                ResolvedConstrainedFeature toleratedPermutation = new ResolvedConstrainedFeature(toleratedFeature, toleratedChoices);
                                List<ResolvedConstrainedFeature> toleratedResult = expandBranchApplyingConstraints(toleratedPermutation, nameToFeatureMap, toleratedFeature,
                                                                                                                   false,
                                                                                                                   expansionDepth + 1);
                                thisBranchResults.addAll(toleratedResult);
                            }
                        }
                    }
                } else {
                    //just recurse down the single branch.
                    //this might be a bound public/protected singleton, or a non singleton. 
                    Map<SingletonSetId, SingletonChoice> nestedChoice = new HashMap<SingletonSetId, SingletonChoice>();
                    nestedChoice.putAll(permutation.choices);
                    refineSingletonChoices(nestedChoice, nestedfeature, nameToFeatureMap);
                    ResolvedConstrainedFeature nestedpermutation = new ResolvedConstrainedFeature(nestedfeature, nestedChoice);
                    boolean nestedIsPreferred = singletonConstrainedByPermutation ? isAPreferredChoice : true;
                    List<ResolvedConstrainedFeature> branchResult = expandBranchApplyingConstraints(nestedpermutation, nameToFeatureMap, nestedfeature, nestedIsPreferred,
                                                                                                    expansionDepth + 1);
                    thisBranchResults.addAll(branchResult);
                }
            }
            //after processing the branch, store the results.
            branchResults.add(thisBranchResults);
        }

        if (feature.getSingleton()) {
            //if this feature itself is a singleton, we cheat slightly, and add ourselves as a branch to the results.
            //this will cause us to reject any results from other branches that bound us to alternate values than ourself.
            //needed when expanding singletons that are not constrained by the permutation.
            List<ResolvedConstrainedFeature> thisSingleton = new ArrayList<ResolvedConstrainedFeature>();
            Map<SingletonSetId, SingletonChoice> thisChoice = new HashMap<SingletonSetId, SingletonChoice>();
            SingletonSetId thisSetId = new SingletonSetId(feature);
            SingletonChoice thisSingletonChoice = new SingletonChoice(feature.getName(), isPreferred);
            thisChoice.put(thisSetId, thisSingletonChoice);
            ResolvedConstrainedFeature rcf = new ResolvedConstrainedFeature(feature, thisChoice);
            thisSingleton.add(rcf);
            branchResults.add(thisSingleton);
        }

        //finally..  
        //if the sub branches exist
        // - merge together all the sub branches..
        //if they do not, then there were no branches to evaluate from this node.
        //(eg a node with no children at all)
        // - return the rcf as supplied.
        List<ResolvedConstrainedFeature> results;
        if (branchResults.size() > 0) {
            //merge the branch permutations
            results = mergeBranches(permutation, branchResults, 0, "");
        } else {
            //no children, return ourselves.
            results = new ArrayList<ResolvedConstrainedFeature>();
            results.add(permutation);
        }
        return results;
    }

    private static boolean rcfChoicesAreCompatibleWithPermutation(ResolvedConstrainedFeature permutation, ResolvedConstrainedFeature candidate) {
        //for each candidate choice, 
        //if the candidate singleton set id is known to the permutation, then the values must match
        //if the candidate singleton set id is NOT known to the permutation, then ONLY the default value is allowed
        //  unless the set is private, in which case all values are fine.
        boolean ok = true;
        for (Map.Entry<SingletonSetId, SingletonChoice> candidateSingleton : candidate.choices.entrySet()) {
            if (permutation.choices.containsKey(candidateSingleton.getKey())) {
                SingletonChoice permvalue = permutation.choices.get(candidateSingleton.getKey());
                SingletonChoice candvalue = candidateSingleton.getValue();
                if (!permvalue.equals(candvalue)) {
                    ok = false;
                    break;
                }
            } else {
                if (!(candidateSingleton.getValue().isPreferred() || candidateSingleton.getKey().isPrivate())) {
                    ok = false;
                    break;
                }
            }
        }
        return ok;
    }

    private static boolean rcfChoicesAreCompatibleWithEachOther(ResolvedConstrainedFeature candidate1, ResolvedConstrainedFeature candidate2) {
        //Overlapping values must match. otherwise not an issue.
        boolean ok = true;
        for (Map.Entry<SingletonSetId, SingletonChoice> candidate1Singleton : candidate1.choices.entrySet()) {
            if (candidate2.choices.containsKey(candidate1Singleton.getKey())) {
                SingletonChoice cand1value = candidate1Singleton.getValue();
                SingletonChoice cand2value = candidate2.choices.get(candidate1Singleton.getKey());
                if (!cand1value.equals(cand2value)) {
                    ok = false;
                    break;
                }
            }
        }
        return ok;
    }

    /**
     * The branch merge routine...
     * 
     * Takes a list of possible combinations per branch..
     * Each combination represents a valid permutation for that branch alone.
     * 
     * Combines the permutations from each branch in compatible ways to produce a single set of permutations that
     * represent the permutations for the node that held the branches.
     * 
     * Uses recursion.. dives down along the branch list, until it reaches the last branch, where permutations
     * that are compatible with the constraints for the current node are returned.
     * 
     * Then for non terminal nodes, takes each permutation from the current branch, and combines it with each permutation
     * passed back from the recursion.. building a new list of permutations that were the compatible product of this branch
     * plus the results from below. This new list is then passed back to the next layer of the recursion.
     * 
     * The net effect is the production of a set of permutations representing the valid singleton combinations for the node.
     * 
     * @param permutation
     * @param branchResults
     * @param depth
     * @param debugprefix
     * @return
     */
    private static List<ResolvedConstrainedFeature> mergeBranches(
                                                                  ResolvedConstrainedFeature permutation,
                                                                  List<List<ResolvedConstrainedFeature>> branchResults,
                                                                  int depth,
                                                                  String debugprefix) {
        List<ResolvedConstrainedFeature> results = new ArrayList<ResolvedConstrainedFeature>();
        if (branchResults.isEmpty()) {
            return results;
        }
        if (depth == (branchResults.size() - 1)) {
            //terminal case.. return our set of rcf's back.
            for (ResolvedConstrainedFeature candidate : branchResults.get(depth)) {
                if (rcfChoicesAreCompatibleWithPermutation(permutation, candidate)) {
                    Map<SingletonSetId, SingletonChoice> choices = new HashMap<SingletonSetId, SingletonChoice>();
                    choices.putAll(candidate.choices);
                    ResolvedConstrainedFeature aggregate = new ResolvedConstrainedFeature(permutation.feature, choices);
                    results.add(aggregate);
                }
            }
        } else {
            List<ResolvedConstrainedFeature> fromOtherRCFs = mergeBranches(permutation, branchResults, depth + 1, debugprefix);
            //now merge our rcf's
            for (ResolvedConstrainedFeature rcf : branchResults.get(depth)) {
                if (rcfChoicesAreCompatibleWithPermutation(permutation, rcf)) {
                    for (ResolvedConstrainedFeature cfe : fromOtherRCFs) {
                        //so we know rcf is compat with the permutation because we just tested it
                        //and we know cfe is compat with the permutation because it came from our if block above in a lower recursion
                        //but we need to test if rcf & cfe are compatible with each other.. 
                        //we just need to know if the intersecting singleton setids have the same value
                        if (rcfChoicesAreCompatibleWithEachOther(cfe, rcf)) {
                            //clone, but upgrade from non-preferred choices to preferred.
                            Map<SingletonSetId, SingletonChoice> choices = new HashMap<SingletonSetId, SingletonChoice>();
                            choices.putAll(rcf.choices);
                            for (Map.Entry<SingletonSetId, SingletonChoice> cfeChoice : cfe.choices.entrySet()) {
                                if (!choices.containsKey(cfeChoice.getKey())) {
                                    choices.put(cfeChoice.getKey(), cfeChoice.getValue());
                                } else {
                                    //only add the new value if it's preferred over this one.
                                    if (cfeChoice.getValue().isPreferred()) {
                                        choices.put(cfeChoice.getKey(), cfeChoice.getValue());
                                    }
                                }
                            }
                            ResolvedConstrainedFeature aggregate = new ResolvedConstrainedFeature(permutation.feature, choices);
                            results.add(aggregate);
                        }
                    }
                }
            }
        }
        return results;

    }

    /**
     * The public entry method for this util, takes a feature and returns the combinations of singletons possible for the feature.
     * 
     * @param feature
     * @param nameToFeatureMap
     * @return
     */
    public static Collection<ResolvedConstrainedFeature> expandFeature(FeatureInfo feature, Map<String, FeatureInfo> nameToFeatureMap) {
        System.out.println("Expanding " + feature.getName());
        Collection<ResolvedConstrainedFeature> results = new ArrayList<ResolvedConstrainedFeature>();

        //build the basic ucf.. this will contain the singletons from this level of feature only.
        //(ucf holds a map of setid->(set of possible choices) for this level of the feature only)
        UnresolvedConstrainedFeature ucf = new UnresolvedConstrainedFeature(feature, nameToFeatureMap);

        generatePermutations(ucf, nameToFeatureMap, results);

        System.out.println("Done Expanding " + feature.getName());
        return results;
    }
}
