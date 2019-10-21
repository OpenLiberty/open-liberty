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
package com.ibm.aries.buildtasks.semantic.versioning.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Set of features built with compatible singleton choices (constraints)
 * 
 * we allow compatible choices to aggregate..
 * selection of a subset of any given singleton acts as a constraint.
 * if this featureset is already constrained by the same subsets, for the same or fewer singleton sets, then it is compatible.
 * selection of entire set of any given singleton set is not a constraint.
 * if this featureset is already constrained by a subset, then selection of entire set is incompatible.
 * selection of a non singleton is not a constraint.
 */
public final class ConstrainedFeatureSet {
    public Set<ResolvedConstrainedFeature> features = new HashSet<ResolvedConstrainedFeature>();
    public Map<SingletonSetId, SingletonChoice> chosenSingletons = new TreeMap<SingletonSetId, SingletonChoice>();

    /** public constructor to allow test cases to drive this.. */
    public ConstrainedFeatureSet() {}

    /**
     * Checks if we can add the set to this set.. allowed if the sets constraints do not conflict.
     */
    private boolean isConstrainedFeatureSetCompatible(ConstrainedFeatureSet newFeature) {
        boolean isOk = true;
        //only singleton choices are stored in choices, so we don't need to worry about non singleton selections.
        for (Map.Entry<SingletonSetId, SingletonChoice> e : newFeature.chosenSingletons.entrySet()) {

            SingletonSetId newFeatureChoiceSetId = e.getKey();
            //System.out.println("Checking constraint id "+newFeatureChoiceSetId);
            //does this set know about the current constraint ?
            if (chosenSingletons.containsKey(newFeatureChoiceSetId)) {
                SingletonChoice existingOption = chosenSingletons.get(newFeatureChoiceSetId);
                SingletonChoice newChoiceValue = e.getValue();
                //yes, compare if the chosen options are compatible.. 
                if (existingOption.equals(newChoiceValue)) {
                    //System.out.println(" merge check ok "+chosenSingletons.get(newFeatureChoiceSetId)+"\n  "+e.getValue());						
                } else {
                    //System.out.println(" merge denied \n  "+chosenSingletons.get(newFeatureChoiceSetId)+"\n  "+e.getValue());
                    isOk = false;
                    break;
                }
            } else {
                //no, this constraint is not known to the current set, so it's ok to merge it
                //UNLESS it's a private choice, and the option being offered is not the default/preferred one.
                if (newFeatureChoiceSetId.isPrivate()) {
                    if (!e.getValue().isPreferred()) {
                        isOk = false;
                        break;
                    }
                }
            }
        }
        //if(isOk)
        //	System.out.println(" merge approved.");
        return isOk;
    }

    /**
     * Adds the set to this set, if possible, returns true if it worked, and false if it wasnt possible.
     */
    public boolean addConstrainedFeatureSet(ConstrainedFeatureSet newFeatureSet) {
        if (!isConstrainedFeatureSetCompatible(newFeatureSet)) {
            //System.out.println("unable to merge feature sets, not compatible.");
            return false;
        }
        for (Map.Entry<SingletonSetId, SingletonChoice> e : newFeatureSet.chosenSingletons.entrySet()) {
            SingletonSetId newFeatureChoiceSetId = e.getKey();
            //does this set know about the current constraint ?
            if (chosenSingletons.containsKey(newFeatureChoiceSetId)) {
                //sets are already ok, else we would have returned false.
            } else {
                //no, this constraint is not known to the current set, so it's ok to merge it.	
                chosenSingletons.put(newFeatureChoiceSetId, e.getValue());
            }
        }
        for (ResolvedConstrainedFeature cf : newFeatureSet.features) {
            boolean found = false;
            for (ResolvedConstrainedFeature existing : features) {
                if (existing.feature.equals(cf.feature)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                features.add(cf);
            }
        }
        return true;
    }

    /**
     * Checks if an individual feature can be added to this set, if the constraints are not conflicting.
     */
    public boolean isConstrainedFeatureCompatible(ResolvedConstrainedFeature newFeature) {
        boolean isOk = true;
        //only singleton choices are stored in choices, so we don't need to worry about non singleton selections.
        for (Map.Entry<SingletonSetId, SingletonChoice> e : newFeature.choices.entrySet()) {
            SingletonSetId newFeatureChoiceSetId = e.getKey();

            //System.out.println("testing choice id "+newFeatureChoiceSetId+" for cfs "+this);

            //need to process private & non-private choices differently.. 
            if (newFeatureChoiceSetId.isPrivate()) {
                //private choices.

                //we only add when
                // - we don't know the choice id and the value offered is the preferred/default choice
                // - we do know the choice id, and the value offered is same, default or not. 
                if (!chosenSingletons.containsKey(newFeatureChoiceSetId)) {
                    //if the set id isnt known to this featureset at all, then if the set value is a ##default, it's ok.
                    SingletonChoice newChoiceValue = e.getValue();
                    if (newChoiceValue.isPreferred()) {
                        //isOk remains true.
                    } else {
                        //System.out.println("denied as value not default");
                        isOk = false;
                        //exit the loop, doesn't matter if the rest is compatible.
                        break;
                    }
                } else {
                    //   if the set id is known to this featureset, then the choice must match, with or without a ## prefix 
                    //    (because to be known to the set means we've already accepted a default).
                    SingletonChoice newChoiceValue = e.getValue();
                    SingletonChoice currentChoiceValue = chosenSingletons.get(newFeatureChoiceSetId);

                    SingletonChoice existingOption = currentChoiceValue;
                    if (newChoiceValue.equals(existingOption)) {
                        //isOk remains true.
                    } else {
                        //System.out.println("denied as not a value match");
                        isOk = false;
                        //exit the loop, doesn't matter if the rest is compatible.
                        break;
                    }
                }

            } else {
                //non-private choices. 

                //the choice for the singleton is already resolved, because we expanded choices up front. 

                //if this sets selection doesn't contain this new choice, it's compatible
                //if this sets selection does contain the set for the choice, and the values match, it's compatible.
                if (chosenSingletons.containsKey(newFeatureChoiceSetId) && !chosenSingletons.get(newFeatureChoiceSetId).equals(e.getValue())) {
                    //System.out.println("deneid as public value no match");
                    isOk = false;
                    break;
                }
            }

        }
        return isOk;
    }

    /**
     * Adds a feature to this set, if possible, returns false if feature conflicted with this set, true if it added ok.
     */
    public boolean addConstrainedFeature(ResolvedConstrainedFeature newFeature) {
        if (!isConstrainedFeatureCompatible(newFeature)) {
            return false;
        }

        for (Map.Entry<SingletonSetId, SingletonChoice> e : newFeature.choices.entrySet()) {
            SingletonSetId newFeatureChoiceSetId = e.getKey();
            //does this set know about the current constraint ?
            if (chosenSingletons.containsKey(newFeatureChoiceSetId)) {
                //sets are already ok, else we would have returned false.
                //do nothing.
            } else {
                //no, this constraint is not known to the current set, so it's ok to merge it.	
                chosenSingletons.put(newFeatureChoiceSetId, e.getValue());
            }
        }
        features.add(newFeature);
        return true;
    }

    /**
     * Handy debug ;p
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Set:\n");
        sb.append("  Features: \n");
        //spin 3 times to produce nice looking output grouped by visibility.
        for (ResolvedConstrainedFeature f : features) {
            if ("public".equals(f.feature.getVisibility())) {
                sb.append("   feature:" + f.feature.getName() + "\n");
            }
        }
        for (ResolvedConstrainedFeature f : features) {
            if ("protected".equals(f.feature.getVisibility())) {
                sb.append("   [protected] feature:" + f.feature.getName() + "\n");
            }
        }
        for (ResolvedConstrainedFeature f : features) {
            if ("private".equals(f.feature.getVisibility())) {
                sb.append("   [private] feature" + f.feature.getName() + "\n");
            }
        }
        sb.append("  Constraints: \n");
        for (Map.Entry<SingletonSetId, SingletonChoice> e : chosenSingletons.entrySet()) {
            sb.append("   " + e.getKey() + " :: " + e.getValue() + "\n");
        }
        return sb.toString();
    }
}