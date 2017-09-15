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
package com.ibm.ws.jca.utils.metagen;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.ws.jca.utils.xml.metatype.MetatypeOcd;

/**
 * Tracks preferences for child aliases and assigns them.
 */
class ChildAliasSelector {
    /**
     * Rankings that each OCD makes for child alias suffixes.
     */
    private final Map<MetatypeOcd, List<String>> rankings = new HashMap<MetatypeOcd, List<String>>();

    /**
     * Names that are unavailable for child alias suffixes,
     * either because they correspond to the aliasSuffix override in wlp-ra.xml,
     * or because an OCD has indicated a preference for the name.
     */
    private final Map<String, MetatypeOcd> unavailableSuffixes = new HashMap<String, MetatypeOcd>();

    /**
     * Assign available child aliases to OCDs based on previously established rankings.
     * Precondition: This should only be invoked after all OCDs have provided rankings for child alias names.
     * 
     * @param raIdentifier identifier for the resource adapter
     * @param baseChildAlias properties.<raIdentifier> portion of the child alias name
     */
    void assign(String raIdentifier, String baseChildAliasName) {
        for (Map.Entry<MetatypeOcd, List<String>> entry : rankings.entrySet()) {
            MetatypeOcd ocd = entry.getKey();
            String suffix = null;
            // Can run out of entries if there are two items with the exact same list of possibilities,
            // which is usually an error in configuration that should be caught
            if (entry.getValue() != null && !entry.getValue().isEmpty())
                suffix = entry.getValue().get(0);
            if (suffix == null || suffix.length() == 0)
                ocd.setExtendsAlias(baseChildAliasName);
            else
                ocd.setExtendsAlias(baseChildAliasName + '.' + suffix);

            // Also assign the OCD name if not overridden by wlp-ra.xml
            if (ocd.getName() == null)
                if (suffix == null || suffix.length() == 0)
                    ocd.setName(raIdentifier + " Properties");
                else
                    ocd.setName(raIdentifier + ' ' + suffix + " Properties");
        }
    }

    /**
     * Specifies rankings for child alias suffixes.
     * 
     * @param ocd represents an object class definition (OCD) element.
     * @param rankedSuffixes ordered list that indicates the rankings. Elements at the beginning of the list are considered to have the highest preference.
     */
    void rank(MetatypeOcd ocd, List<String> rankedSuffixes) {
        List<String> childAliasSuffixes = new LinkedList<String>();
        for (String suffix : rankedSuffixes)
            if (!childAliasSuffixes.contains(suffix)) {
                MetatypeOcd collision = unavailableSuffixes.put(suffix, ocd);
                if (collision == null)
                    childAliasSuffixes.add(suffix);
                else {
                    List<String> suffixesForCollisionOCD = rankings.get(collision);
                    if (suffixesForCollisionOCD != null)
                        suffixesForCollisionOCD.remove(suffix);
                }
            }
        rankings.put(ocd, childAliasSuffixes);
    }

    /**
     * Reserve a child alias suffix so that no one else can claim it.
     * This method should only be used when an aliasSuffix override is specified in wlp-ra.xml.
     * 
     * @param suffix name of the child alias.
     * @param ocd OCD element that is claiming it.
     * @throws IllegalArgumentException if another OCD has already reserved the child alias suffix.
     */
    void reserve(String suffix, MetatypeOcd ocd) {
        MetatypeOcd previous = unavailableSuffixes.put(suffix, ocd);
        if (previous != null)
            throw new IllegalArgumentException("aliasSuffix: " + suffix);
    }
}
