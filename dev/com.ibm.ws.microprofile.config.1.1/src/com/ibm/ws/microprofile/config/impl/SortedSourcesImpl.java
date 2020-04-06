/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.impl;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.interfaces.SortedSources;

/**
 * Contains a set of ConfigSources, sorted by ordinal, highest value first
 */
public class SortedSourcesImpl extends AbstractSet<ConfigSource> implements SortedSources {

    private SortedSet<ConfigSource> sources = new TreeSet<>(ConfigSourceComparator.INSTANCE);

    public SortedSourcesImpl() {
        super();
    }

    public SortedSourcesImpl(SortedSet<ConfigSource> initialSources) {
        this();
        this.sources.addAll(initialSources);
    }

    @Override
    @Trivial
    public SortedSourcesImpl unmodifiable() {
        this.sources = Collections.unmodifiableSortedSet(this.sources);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public Iterator<ConfigSource> iterator() {
        return this.sources.iterator();
    }

    /**
     * @return
     */
    @Override
    @Trivial
    public int size() {
        return this.sources.size();
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

    @Override
    public boolean add(ConfigSource configSource) {
        return this.sources.add(configSource);
    }
}
