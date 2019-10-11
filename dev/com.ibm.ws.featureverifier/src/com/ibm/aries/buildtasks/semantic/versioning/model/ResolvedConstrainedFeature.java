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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Wrapper class for feature that represents a feature with its singletons all bound to choices.
 */
public final class ResolvedConstrainedFeature {
    public FeatureInfo feature;
    public Map<SingletonSetId, SingletonChoice> choices = new TreeMap<SingletonSetId, SingletonChoice>();

    public ResolvedConstrainedFeature(FeatureInfo base, Map<SingletonSetId, SingletonChoice> choices) {
        this.feature = base;
        this.choices = Collections.unmodifiableMap(choices);
    }

    //delegate hashcode/equals to the underlying feature.
    @Override
    public int hashCode() {
        return feature.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResolvedConstrainedFeature) {
            ResolvedConstrainedFeature other = (ResolvedConstrainedFeature) obj;
            return feature.equals(other.feature);
        }
        return false;
    }

    @Override
    public String toString() {
        String v = "[" + super.hashCode() + "] " + feature.getName() + " " + feature.getVisibility() + " { ";
        for (Map.Entry<SingletonSetId, SingletonChoice> e : choices.entrySet()) {
            v += "{" + e.getKey() + " " + e.getValue() + "} ";
        }
        v += "}";
        return v;
    }
}