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
package com.ibm.ws.featureverifier.migrator;

import java.util.HashSet;
import java.util.Set;

class IgnoreBlock {
    final String forFeature;
    final Set<Ignore> ignores = new HashSet<Ignore>();

    public IgnoreBlock(String feature) {
        this.forFeature = feature;
    }

//    public void addMessageIgnore(String regex) {
//        Ignore i = new MessageIgnore(regex, forFeature);
//        ignores.add(i);
//    }
//
//    public void addComment(String value) {
//        Comment c = new Comment(value, forFeature);
//        ignores.add(c);
//    }
//
//    public void addPackageIgnore(String regex, boolean baseline, boolean runtime) {
//        PackageIgnore pi = new PackageIgnore(regex, forFeature, baseline, runtime);
//        ignores.add(pi);
//    }

    /**
     * @return
     */
    public int getSize() {
        return ignores.size();
    }

    /**
     * @param value
     * @return
     */
    public boolean appliesTo(String value) {
        return ((forFeature != null) && forFeature.equals(value));
    }

    /**
     * @param candidate
     */
    public void add(IgnoreBlock candidate) {
        if (!appliesTo(candidate.forFeature)) {
            // Make sure we're merging an IgnoreBlock for the same feature
            return;
        }

        ignores.addAll(candidate.ignores);

    }

}