/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Contains a set of ConfigSources, sorted by ordinal, highest value first
 */
public class SortedSources implements Iterable<ConfigSource> {

    private SortedSet<ConfigSource> sources;

    public SortedSources() {
        sources = new TreeSet<ConfigSource>(ConfigSourceComparator.INSTANCE);
    }

    public SortedSources(SortedSet<ConfigSource> initialSources) {
        this();
        sources.addAll(initialSources);
    }

    @Trivial
    public SortedSources unmodifiable() {
        sources = Collections.unmodifiableSortedSet(sources);
        return this;
    }

    /**
     * @param toAdd
     */
    @Trivial
    public void addAll(Collection<ConfigSource> toAdd) {
        sources.addAll(toAdd);
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public Iterator<ConfigSource> iterator() {
        return sources.iterator();
    }

    /**
     * @return
     */
    @Trivial
    public int size() {
        return sources.size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Sorted Sources: ");
        for (ConfigSource source : this) {
            builder.append("\n\t");
            builder.append(source.getOrdinal());
            builder.append(" = ");
            builder.append(source.getName());
        }
        return builder.toString();
    }

    /**
     * CURRENTLY ONLY USED BY UNIT TEST
     */
    @Trivial
    public void add(ConfigSource source) {
        sources.add(source);
    }

}
