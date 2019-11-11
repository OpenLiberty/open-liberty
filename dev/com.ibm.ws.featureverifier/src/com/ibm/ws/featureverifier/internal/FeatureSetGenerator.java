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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;

import com.ibm.aries.buildtasks.semantic.versioning.model.ConstrainedFeatureSet;
import com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo;
import com.ibm.aries.buildtasks.semantic.versioning.model.ResolvedConstrainedFeature;
import com.ibm.aries.buildtasks.semantic.versioning.model.SingletonChoice;
import com.ibm.aries.buildtasks.semantic.versioning.model.SingletonSetId;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

/**
 * This class is still incomplete, it is essentially one large utility invoked via the generateFeatureSets method.
 * 
 * It's intent is to..
 * 
 * - expand each feature given into each supported variant of itself.
 * Eg, a feature supporting servlet-3.0 and servlet-3.1 will have 2 supported variants of itself.
 * each usage of a singleton within a features hierarchy leads to more supported variants of the top feature,
 * however it's not a straight 2^N expansion, as nested branches can further restrict singletons higher up.
 * 
 * - combine the expansions into 'buckets' to create server.xml configurations that will load the contained
 * expansions in their variants so the api/spi can be evaluated.
 * 
 * - create server.xmls and protectedN.mfs for each bucket
 * 
 * This is as far as it goes today.
 * 
 * The next stage is to cope with change.
 * 
 * - each feature will still be expanded
 * - the existing server.xmls / protectedN.mf's will be read and processed using the new features read to
 * understand which expansions are already tested.
 * - the untested expansions will then be aggregated to new buckets
 * - new configs will be written to test the new buckets.
 * 
 */
public class FeatureSetGenerator {

    /**
     * attempt to add each bucket in the list to the bucket passed as 1st arg.
     */
    private static List<ConstrainedFeatureSet> mergeBuckets(ConstrainedFeatureSet bucketToMergeTo, List<ConstrainedFeatureSet> originalBuckets) {
        List<ConstrainedFeatureSet> merged = new ArrayList<ConstrainedFeatureSet>();
        for (ConstrainedFeatureSet cfs : originalBuckets) {
            if (bucketToMergeTo.addConstrainedFeatureSet(cfs)) {
                merged.add(cfs);
            }
        }
        return merged;
    }

    /**
     * Checks if the test feature will help to bind the choices in target.
     * Although all choices in target are made, they may include 'non preferred' choices
     * Other features can upgrade those non-preferred choices to be preferred if they have the choice as preferred.
     */
    private static boolean willHelpToResolve(ResolvedConstrainedFeature target, ResolvedConstrainedFeature test) {
        //if this feature offers no overlap for the private choices, ignore it.
        boolean found = false;
        boolean helpful = false;
        boolean compatible = true;
        for (Map.Entry<SingletonSetId, SingletonChoice> choice : target.choices.entrySet()) {
            if (test.choices.containsKey(choice.getKey())) {
                //the test offers resolution for some of the choice in the target.. 
                //to be compatible, the options must not clash.
                found = true;
                //check the values match.. 
                SingletonChoice targetChoice = choice.getValue();
                SingletonChoice testChoice = test.choices.get(choice.getKey());
                if (targetChoice.isPreferred() && testChoice.isPreferred()) {
                    compatible &= targetChoice.equals(testChoice);
                } else if (targetChoice.isPreferred() && !testChoice.isPreferred()) {
                    //new option is not default, but list was default.. 
                    //don't add.. if the aggregate already has a default.. adding a non-default risks adding a conflicting default..
                    compatible = false;
                } else if (!targetChoice.isPreferred() && testChoice.isPreferred()) {
                    compatible &= targetChoice.equals(testChoice);
                    helpful |= compatible;
                } else if (!targetChoice.isPreferred() && !testChoice.isPreferred()) {
                    compatible &= targetChoice.equals(testChoice);
                }
            }
            if (!compatible)
                break;
        }
        //if no options overlapped.. the test rcf will not help to resolve this target.
        if (!found) {
            //System.out.println("no overlap, no help.");
            return false;
        } else {
            //System.out.println("compatible? "+compatible+" helpful? "+helpful);
            return compatible && helpful;
        }
    }

    /**
     * tests if the given feature is compatible with the singleton choices already made in the aggregate choice set.
     */
    private static boolean isCompatibleWithAggregateChoices(ResolvedConstrainedFeature target, Map<SingletonSetId, SingletonChoice> aggregate) {
        boolean compatible = true;
        //check if the target will conflict.. 
        for (Map.Entry<SingletonSetId, SingletonChoice> aggregateChoice : aggregate.entrySet()) {
            //check each choice in the aggregate set against those known to the feature.
            if (target.choices.containsKey(aggregateChoice.getKey())) {
                SingletonChoice targetChoiceValue = target.choices.get(aggregateChoice.getKey());
                SingletonChoice aggregateChoiceValue = aggregateChoice.getValue();
                if (targetChoiceValue.isPreferred() && aggregateChoiceValue.isPreferred()) {
                    //both defaults? must match
                    compatible &= targetChoiceValue.equals(aggregateChoiceValue);
                } else if (targetChoiceValue.isPreferred() && !aggregateChoiceValue.isPreferred()) {
                    //new option is default? then it must be the non-default for the one from the list.
                    compatible &= targetChoiceValue.equals(aggregateChoiceValue);
                } else if (!targetChoiceValue.isPreferred() && aggregateChoiceValue.isPreferred()) {
                    //new option is not default, but list was default.. 
                    //don't add.. if the aggregate already has a default.. adding a non-default risks adding a conflicting default..
                    compatible = false;
                } else if (!targetChoiceValue.isPreferred() && !aggregateChoiceValue.isPreferred()) {
                    //neither choice is a default.. still must match.. 
                    compatible &= targetChoiceValue.equals(aggregateChoiceValue);
                }
            } else {
                //the constraint isn't known to the feature, so it can't conflict.
            }
            if (!compatible)
                break;
        }
        return compatible;
    }

    /**
     * takes a list of features with their choices already bound, and gives back a list of buckets containing the features
     * aggregated in compatible ways.
     * 
     * protected so test can invoke
     */
    protected static List<ConstrainedFeatureSet> mergeFeaturesIntoSets(List<ResolvedConstrainedFeature> expanded) {
        //ok now we have a list containing all permutations of choices for features.
        //keep a copy of it for after the set creation..
        List<ResolvedConstrainedFeature> originalExpandedList = new ArrayList<ResolvedConstrainedFeature>(expanded);

        //sort the list by complexity.. 
        Collections.sort(originalExpandedList, new Comparator<ResolvedConstrainedFeature>() {
            @Override
            public int compare(ResolvedConstrainedFeature o1,
                               ResolvedConstrainedFeature o2) {
                if (o1.choices.size() > o2.choices.size()) {
                    return 1;
                } else if (o1.choices.size() < o2.choices.size()) {
                    return -1;
                } else {
                    return o1.feature.getName().compareTo(o2.feature.getName());
                }
            }

        });

        //we'll now group those into buckets, where each bucket holds compatible choices. 
        List<ConstrainedFeatureSet> buckets = new ArrayList<ConstrainedFeatureSet>();
        List<ResolvedConstrainedFeature> unableToAdd = new ArrayList<ResolvedConstrainedFeature>();
        boolean didSomething = true;
        int xx = 0;
        while (didSomething) {
            didSomething = false;
            xx = 0;
            for (ResolvedConstrainedFeature cf : expanded) {
                //System.out.println("Attempting to merge expanded config "+(xx++)+"/"+expanded.size());

                //we DONT! allow private features, as although we can set them via the protected.mf's
                //if we do, we end up breaking assumptions made by the feature authors. 
                if ("private".equals(cf.feature.getVisibility())) {
                    continue;
                }

                if (!cf.feature.getAutoFeature()) {
                    //System.out.println("Evaluating rcf "+cf.feature.getName());
                    boolean added = false;
                    for (ConstrainedFeatureSet s : buckets) {
                        //if this is a singleton,, does this set already contain this one?
                        boolean alreadyAdded = false;
                        if (cf.feature.getSingleton()) {
                            for (ResolvedConstrainedFeature f : s.features) {
                                if (f.feature.getName().equals(cf.feature.getName())) {
                                    alreadyAdded = true;
                                    break;
                                }
                            }
                        }

                        if (!alreadyAdded && s.addConstrainedFeature(cf)) {
                            didSomething = true;
                            added = true;
                            //System.out.println("added "+cf.feature.getName()+" .. ");
                            //allow trying to add singletons to everything.. 
                            //but stop after the 1st successful add for everything else.
                            if (!cf.feature.getSingleton()) {
                                break;
                            }
                        }
                    }
                    if (cf.feature.getSingleton() && cf.feature.getVisibility().equals("private")) {
                        unableToAdd.add(cf);
                    } else {
                        if (!added) {
                            ConstrainedFeatureSet empty = new ConstrainedFeatureSet();
                            if (!empty.addConstrainedFeature(cf)) {
                                //if we're denied adding this to an empty bucket.. then the feature requires private features that 
                                //haven't been set yet by other features..

                                //remember the feature, so we can try again in a mo.
                                unableToAdd.add(cf);
                            } else {
                                buckets.add(empty);
                                didSomething = true;
                            }
                        }
                    }
                }
            }
            //System.out.println("Finished pass of expandeds, didSomething?"+didSomething+" unableToAdd.size "+unableToAdd.size() );
            if (unableToAdd.size() > 0) {
                //did anything change?
                if (didSomething) {
                    //are all the remaining rcf's private ?
                    boolean allSingletons = true;
                    for (ResolvedConstrainedFeature rcf : unableToAdd) {
                        if (!(rcf.feature.getVisibility().equals("private"))) {
                            allSingletons = false;
                            break;
                        }
                    }
                    if (!allSingletons) {
                        //System.out.println("Going round again with "+unableToAdd.size());
                        //for(ResolvedConstrainedFeature x:unableToAdd){
                        //	System.out.println("   ["+(x.feature.getSingleton()?"s]":" ]")+x.toString());
                        //}

                        //allow us to go round again.. 
                        expanded = unableToAdd;
                        unableToAdd = new ArrayList<ResolvedConstrainedFeature>();
                    } else {
                        //System.out.println("All remaining rcf's are singletons.. sort them out next..");
                        didSomething = false;
                    }
                } else {
                    //we end here, as nothing altered
                    //didSomething is already false.
                }
            } else {
                //no more buckets to process.. exit now.
                didSomething = false;
            }
            //System.out.println("are we going round again? "+didSomething);
        }

        //finally.. trim the singletons out of the unableToAdd set
        List<ResolvedConstrainedFeature> keepList = new ArrayList<ResolvedConstrainedFeature>();
        for (ResolvedConstrainedFeature rcf : unableToAdd) {
            if (!rcf.feature.getSingleton() || rcf.feature.getVisibility().equals("private")) {
                keepList.add(rcf);
            }
        }
        unableToAdd = keepList;

        System.out.println("Initial pass complete.. we now have " + buckets.size() + " buckets.. and " + unableToAdd.size() + " remaining");

        List<ResolvedConstrainedFeature> unsolved = new ArrayList<ResolvedConstrainedFeature>();
        if (unableToAdd.size() > 0) {

            System.out.println("Beginning extended merge pass.. we have " + unableToAdd.size() + " to try.");

            //ok.. our simple approach to jamming things into buckets required that the features would aggregate ok
            //in the order we discovered them in.. which isn't guaranteed.. since we only assign features to buckets 
            //once, that means we may exhaust the selections required to satisfy the constraints of later features
            //before we reach them. And if we exhausted them into buckets that were not compatible with the later features
            //then the later features will end up stuck there..

            //so now, we'll loop through the left over feature permutations, and attempt to satisfy them with chocices
            //from the original list. 

            //it's still possible for permutations to be impossible to satisfy, 
            //  eg, f-A uses f-B-1.0 or f-B-1.1 and f-A uses f-C-1.0 which uses f-B-1.1 only.
            //      in this circumstance, f-A(B1.0+C1.0) is impossible to satisfy.
            //this may become even harder to visualise if the dependencies are moved out a few layers, and 
            //the blocking dependencies become combinations rather than simple instances of a single f-version.

            //still, we hope that for each remaining permutation, we can find other permutations to satisfy it, 
            //without needing to resort to a recursive combinatorial search that would soon become expensive.
            List<ResolvedConstrainedFeature> toTryAgain = new ArrayList<ResolvedConstrainedFeature>(unableToAdd);
            xx = 0;
            boolean resolvedThisRCF = false;
            for (ResolvedConstrainedFeature rcf : toTryAgain) {
                System.out.println("Attempting to resolve unsatisfied feature combination (" + (xx++) + "/" + toTryAgain.size() + ") - " + rcf);

                //we don't want to allow aggregation of private features.. user's can't do that, so we shouldn't either.
                if (rcf.feature.getVisibility().equals("private")) {
                    continue;
                }
                if (rcf.feature.getAutoFeature()) {
                    continue;
                }

                //loop over originalExpandedList
                //see if can find feature(s) to satisfy rcf.

                //can't use the normal set merge, as it only uses aggregation of constraint
                //and here we are now actively seeking compatible ones..
                List<ResolvedConstrainedFeature> builtForThisRCF = new ArrayList<ResolvedConstrainedFeature>();
                Map<SingletonSetId, SingletonChoice> aggregateConstraints = new TreeMap<SingletonSetId, SingletonChoice>();
                aggregateConstraints.putAll(rcf.choices);

                //System.out.println("Initial constraints");
                //for(Map.Entry<String,String> e : aggregateConstraints.entrySet()){
                //	System.out.println(" "+e.getKey()+" -> "+e.getValue());
                //}

                builtForThisRCF.add(rcf);
                boolean addedSomething = true;
                while (addedSomething) {
                    addedSomething = false;
                    for (ResolvedConstrainedFeature test : originalExpandedList) {

                        //avoid aggregating privates.. 
                        if ("private".equals(test.feature.getVisibility())) {
                            //System.out.println("- not evaluating private feature "+test.feature.getName());
                            continue;
                        }

                        //System.out.print("- checking if I can add "+test);						
                        if (willHelpToResolve(rcf, test)) {
                            //System.out.println(" : yes, will help resolve constraints for this one.");
                            //System.out.print("- is it compatible? ");
                            //System.out.println(" aggregateConstraints: ");
                            //for(Map.Entry<String,String> e : aggregateConstraints.entrySet()){
                            //	System.out.println("  "+e.getKey()+" -> "+e.getValue());
                            //}
                            //System.out.println(" rcf: "+rcf);
                            //System.out.println("test: "+test);
                            if (isCompatibleWithAggregateChoices(test, aggregateConstraints)) {
                                //System.out.println(":yes");								
                                //System.out.print("-have we already added it?");
                                boolean alreadyAdded = false;
                                for (ResolvedConstrainedFeature alreadyDone : builtForThisRCF) {
                                    //System.out.println(" checking "+test.feature.getName()+" against "+alreadyDone.feature.getName());
                                    if (test.feature.getName().equals(alreadyDone.feature.getName())) {
                                        alreadyAdded = true;
                                        break;
                                    }
                                }
                                if (!alreadyAdded) {
                                    //System.out.println(" :no, not added yet.");
                                    addedSomething = true;
                                    //System.out.println(" Merging.. "+test);
                                    builtForThisRCF.add(test);
                                    //merge the constraints.
                                    for (Map.Entry<SingletonSetId, SingletonChoice> newChoice : test.choices.entrySet()) {
                                        if (aggregateConstraints.containsKey(newChoice.getKey())) {
                                            //key is known to set, and was compatible.. upgrade to the default if applicable.
                                            if (newChoice.getValue().isPreferred()) {
                                                aggregateConstraints.put(newChoice.getKey(), newChoice.getValue());
                                            }
                                        } else {
                                            //key not known, was compatible, so we just add it.
                                            aggregateConstraints.put(newChoice.getKey(), newChoice.getValue());
                                        }
                                    }
                                    //System.out.println("After merge constraints are now ");
                                    //for(Map.Entry<String,String> e : aggregateConstraints.entrySet()){
                                    //	System.out.println("  "+e.getKey()+" -> "+e.getValue());
                                    //}
                                    //Are all the constraints satisfied for this rcf now ?
                                    boolean allResolved = true;
                                    Set<SingletonChoice> why = new TreeSet<SingletonChoice>();
                                    for (Map.Entry<SingletonSetId, SingletonChoice> e : aggregateConstraints.entrySet()) {
                                        if (!e.getValue().isPreferred()) {
                                            allResolved = false;
                                            why.add(e.getValue());
                                        }
                                    }
                                    if (allResolved) {
                                        break;
                                    } else {
                                        //System.out.println(" - still missing "+why);
                                    }
                                } else {
                                    //System.out.println(" :yes, already added");
                                }
                            } else {
                                //System.out.println(":no, is incompatible");
                            }
                        } else {
                            //System.out.println(": will not help to resolve. ");
                        }
                    }

                    //System.out.println("finished trying to add original expansions to rcf.. scanning to see if all constraints are resolved.. ");

                    //optimistic ;p assume we just resolved it, and attempt to disprove that.
                    resolvedThisRCF = true;
                    Set<SingletonChoice> why = new TreeSet<SingletonChoice>();
                    for (Map.Entry<SingletonSetId, SingletonChoice> e : aggregateConstraints.entrySet()) {
                        if (!e.getValue().isPreferred()) {
                            resolvedThisRCF = false;
                            why.add(e.getValue());
                        }
                    }
                    //System.out.println("constraints "+why+" are still unsatisfied "+why);

                    //System.out.println("state: allResolved:"+resolvedThisRCF+" addedSomething:"+addedSomething);

                    if (!resolvedThisRCF && !addedSomething) {
                        System.out.println("ERROR: Unable to find compatible features to resolve feature permuation " + rcf + " unresolved constraints: " + why);
                    } else {
                        if (resolvedThisRCF) {
                            System.out.println("set resolved. ");
                            System.out.println("For unresolved rcf " + rcf.feature.getName() + " built following set");
                            System.out.println(" agg constraints:");
                            for (Map.Entry<SingletonSetId, SingletonChoice> e : aggregateConstraints.entrySet()) {
                                System.out.println("  " + e.getKey() + " : " + e.getValue());
                            }
                            for (ResolvedConstrainedFeature l : builtForThisRCF) {
                                System.out.println("  " + l);
                            }
                            //clear while loop flag
                            addedSomething = false;

                            ConstrainedFeatureSet cfs = new ConstrainedFeatureSet();
                            cfs.features.addAll(builtForThisRCF);
                            cfs.chosenSingletons.putAll(aggregateConstraints);
                            buckets.add(cfs);
                        } else if (!resolvedThisRCF) {
                            //System.out.println("Going round again.. ");
                        }
                    }
                }
                //if we didn't solve this feaure.. and we've run out of things to compare against in the while loop
                //remember this rcf so we can report it as missing.
                if (!resolvedThisRCF) {
                    unsolved.add(rcf);
                }
            }
        }

        if (unsolved.size() > 0) {
            for (ResolvedConstrainedFeature cf : unsolved) {
                System.out.println("ERROR: Unsatisfied feature combination - " + cf);
            }
        }

        System.out.println("Bucket merge beginning.");

        //now run thru and try to merge the buckets.. this will save us time by reducing the number of configs to test.
        //can merge a bucket if all the constraints are for different singleton sets, or if the constraints for overlapping sets
        //have the same values.
        List<ConstrainedFeatureSet> mergedBuckets = new ArrayList<ConstrainedFeatureSet>();
        System.out.println("master bucket list starting at size " + buckets.size());
        while (buckets.size() > 0) {
            //System.out.println("creating new target bucket.. ");
            ConstrainedFeatureSet target = new ConstrainedFeatureSet();
            mergedBuckets.add(target);
            //System.out.println("Attempting merge into target : currently holding  "+target.chosenSingletons.keySet());				
            List<ConstrainedFeatureSet> merged = mergeBuckets(target, buckets);
            //System.out.println(" - that merged "+merged.size()+" into target.");
            if (merged.size() > 0) {
                buckets.removeAll(merged);
                //System.out.println("master bucket list now at size "+buckets.size());
            }
        }

        //Finally, due to the way we handle singletons, we may now have buckets that are totally satisfied, and constrained,
        //but are missing the public singletons that bind the final constraints. 
        for (ConstrainedFeatureSet c : mergedBuckets) {
            //iterate a clone of the chosen options, because we'll be modifying it during the loop
            Map<SingletonSetId, SingletonChoice> choicesClone = new HashMap<SingletonSetId, SingletonChoice>(c.chosenSingletons);
            for (Map.Entry<SingletonSetId, SingletonChoice> e : choicesClone.entrySet()) {
                SingletonSetId singletonId = e.getKey();
                SingletonChoice featureBoundToSingleton = e.getValue();
                if (!featureBoundToSingleton.isPreferred()) {
                    c.chosenSingletons.remove(singletonId);
                    if (singletonId.isPrivate()) {
                        throw new IllegalStateException("Unbound private singleton " + singletonId + " within bucket " + c);
                    }
                    //ok, this is an unbound public singleton.. let's find it in the expanded list, and merge it in..
                    List<ResolvedConstrainedFeature> candidates = new ArrayList<ResolvedConstrainedFeature>();
                    for (ResolvedConstrainedFeature rcf : originalExpandedList) {
                        if (rcf.feature.getName().equals(featureBoundToSingleton.getChoiceFeatureName())) {
                            candidates.add(rcf);
                        }
                    }
                    System.out.println("have found " + candidates.size() + " matches for singleton " + featureBoundToSingleton + " to try to add.");
                    //if the public singletons themselves have permutations, we may need to find a compatible one.
                    boolean foundAMatch = false;
                    for (ResolvedConstrainedFeature candidate : candidates) {
                        System.out.println("testing " + candidate);
                        if (c.addConstrainedFeature(candidate)) {
                            foundAMatch = true;
                            break;
                        }
                    }
                    if (!foundAMatch) {
                        throw new IllegalStateException("Unable to find a compatible expansion of singleton " + featureBoundToSingleton + " to add to " + c);
                    }
                }
            }

            //ok.. now all the singletons should be bound.. 
            for (Map.Entry<SingletonSetId, SingletonChoice> e : c.chosenSingletons.entrySet()) {
                if (!e.getValue().isPreferred()) {
                    throw new IllegalStateException("Bucket still has unresolved singletons " + e.getKey() + " " + c);
                }
            }
        }

        //wipe the list as we don't need it anymore.
        originalExpandedList.clear();

        System.out.println("Merged Buckets " + mergedBuckets.size());
        xx = 0;
        for (ConstrainedFeatureSet c : mergedBuckets) {
            System.out.println("Merged Bucket " + (xx++));
            System.out.println(c);
        }
        System.out.println("");

        //temp.
        return mergedBuckets;
    }

    static String json = "";

    public static List<ResolvedConstrainedFeature> getPermutationsForFeatureSet(Collection<FeatureInfo> allInstalledFeatures) {
        return getPermutationsForFeatureSet(allInstalledFeatures, allInstalledFeatures);
    }

    public static List<ResolvedConstrainedFeature> getPermutationsForFeature(FeatureInfo expandOnly, Collection<FeatureInfo> allInstalledFeatures) {
        List<FeatureInfo> expandedList = new ArrayList<FeatureInfo>();
        expandedList.add(expandOnly);
        return getPermutationsForFeatureSet(expandedList, allInstalledFeatures);
    }

    public static List<ResolvedConstrainedFeature> getPermutationsForFeatureSet(Collection<FeatureInfo> expandOnly, Collection<FeatureInfo> allInstalledFeatures) {
        //we have to build our own name to feature map, as the one built by the main path code
        //will only have the features in the current config in it.
        Map<String, FeatureInfo> nameToFeatureMap = FeatureInfoUtils.createNameToFeatureMap(allInstalledFeatures);

        //create a json string for the feature visualiser
        //buildJsonString(allInstalledFeatures, nameToFeatureMap);

        List<ResolvedConstrainedFeature> result = new ArrayList<ResolvedConstrainedFeature>();

        Set<String> broken = new HashSet<String>();
        for (FeatureInfo f : expandOnly) {

            Collection<ResolvedConstrainedFeature> aggregate = FeatureExpander.expandFeature(f, nameToFeatureMap);

            String debug = " (" + aggregate.size() + " permutations) [";
            for (ResolvedConstrainedFeature d : aggregate) {
                debug += d + ",";
            }
            debug += "]";
            System.out.println("Expanded " + f.getName() + " to " + debug);
            result.addAll(aggregate);
        }

        System.out.println("Total set size was " + result.size());

        //easy to get these if you try writing recursive routines without enough sleep!!
        if (!broken.isEmpty())
            System.out.println("BORKEN:!! " + broken);

        return result;
    }

    /**
     * Top level entry for this class..
     * 
     * generates the permutations, aggregates them, then writes them out as server.xmls / protectedN.mfs
     */
    public static void generateFeatureSets(List<FeatureInfo> allInstalledFeatures, BundleContext context) throws IOException {

        List<ResolvedConstrainedFeature> sortedByComplexity = getPermutationsForFeatureSet(allInstalledFeatures);

        List<ConstrainedFeatureSet> mergedBuckets = mergeFeaturesIntoSets(sortedByComplexity);

        int c = 0;
        System.out.println("MERGED BUCKETS:: ");
        for (ConstrainedFeatureSet cfs : mergedBuckets) {
            System.out.println("Bucket " + c);
            c++;
            System.out.println(cfs);
        }

        WsLocationAdmin wla = context.getService(context.getServiceReference(WsLocationAdmin.class));
        String outputDirString = wla.resolveString(WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR);
        File outputDir = new File(outputDirString);

//		json = FeatureJSONCreator.buildJsonString(allInstalledFeatures, nameToFeatureMap);
//		File jsonF = new File(outputDir,"data.json");
//		FileWriter jw = new FileWriter(jsonF);
//		jw.append(json);
//		jw.close();

        File configsDir = new File(outputDir, "configs");
        if (!configsDir.exists()) {
            configsDir.mkdirs();
        }
        File protectedDir = new File(configsDir, "protected");
        if (!protectedDir.exists()) {
            protectedDir.mkdirs();
        }
        Map<String, FeatureInfo> nameToFeatureMap = FeatureInfoUtils.createNameToFeatureMap(allInstalledFeatures);
        int config = 0;
        for (ConstrainedFeatureSet cfs : mergedBuckets) {
            String prefix = "";
            if (GlobalConfig.getSetGenerateConfigPrefix() != null && GlobalConfig.getSetGenerateConfigPrefix().length() > 0)
                prefix = GlobalConfig.getSetGenerateConfigPrefix();
            File thisConfig = new File(configsDir, prefix + "config" + (++config));
            if (!thisConfig.exists()) {
                thisConfig.mkdirs();
            }
            boolean needsProtected = false;
            File serverxml = new File(thisConfig, "server.xml");
            FileWriter fw = new FileWriter(serverxml);
            try {
                fw.append("<server><featureManager>\n");
                fw.append("<!-- Constraints : -->\n");
                for (Map.Entry<SingletonSetId, SingletonChoice> e : cfs.chosenSingletons.entrySet()) {
                    fw.append("<!--  " + e.getKey() + " bound to " + e.getValue() + " -->\n");
                }

                for (ResolvedConstrainedFeature rcf : cfs.features) {
                    if (rcf.feature.getVisibility().equals("public")) {
                        fw.append(" <feature>" + rcf.feature.getName() + "</feature> <!-- " + rcf.choices + "--> \n");
                    } else {
                        fw.append(" <!-- " + rcf.feature.getVisibility() + " feature " + rcf.feature.getName() + " will be supplied by the protected feature 'protected" + config
                                  + "' -->\n");
                    }
                    if (rcf.feature.getVisibility().equals("protected")) {
                        needsProtected = true;
                    }
                }
                //the server.xml also needs all the protected/public singletons to meet the tolerated chain rule.
                for (Map.Entry<SingletonSetId, SingletonChoice> e : cfs.chosenSingletons.entrySet()) {
                    FeatureInfo singleton = nameToFeatureMap.get(e.getValue().getChoiceFeatureName());
                    if (singleton != null && "public".equals(singleton.getVisibility())) {
                        fw.append(" <feature>" + singleton.getName() + "</feature> <!-- added to help force singleton choice--> \n");
                    }
                }
                if (needsProtected) {
                    fw.append(" <feature>protected." + prefix + "." + config + "</feature>\n");
                }
                //don't forget to include our checker!!
                fw.append("  <feature>featureverifier-1.0</feature>\n");
                fw.append("</featureManager></server>\n");
                fw.flush();
            } finally {
                fw.close();
            }
            if (needsProtected) {
                File protectedxml = new File(protectedDir, "protected." + prefix + "." + config + ".mf");
                FileWriter pw = new FileWriter(protectedxml);
                try {
                    pw.append("Subsystem-ManifestVersion: 1\n");
                    pw.append("Subsystem-SymbolicName: protected." + prefix + "." + config + "; visibility:=public\n");
                    pw.append("Subsystem-Version: 1.0.0\n");
                    pw.append("Subsystem-Content: ");
                    boolean needsComma = false;
                    //the protected feature needs ALL the public features, to ensure the singletons are 
                    //tolerated in the same configurations.
                    for (ResolvedConstrainedFeature rcf : cfs.features) {
                        if (needsComma)
                            pw.append(" ,");
                        pw.append(rcf.feature.getName() + "; type=\"osgi.subsystem.feature\"\n");
                        needsComma = true;
                    }
                    //the protected feature also needs all the protected/public singletons to meet the tolerated chain rule.
                    for (Map.Entry<SingletonSetId, SingletonChoice> e : cfs.chosenSingletons.entrySet()) {
                        FeatureInfo singleton = nameToFeatureMap.get(e.getValue().getChoiceFeatureName());
                        if (singleton != null && !"private".equals(singleton.getVisibility())) {
                            if (needsComma)
                                pw.append(" ,");
                            pw.append(singleton.getName() + "; type=\"osgi.subsystem.feature\"\n");
                            needsComma = true;
                        }
                    }

                    // com.ibm.websphere.appserver.anno-1.0; type="osgi.subsystem.feature",
                    pw.append("Subsystem-Type: osgi.subsystem.feature\n");
                    pw.append("IBM-Feature-Version: 2\n");
                    pw.append("IBM-ProductID: com.ibm.websphere.appserver\n");
                } finally {
                    pw.close();
                }

            }
        }

    }
}
