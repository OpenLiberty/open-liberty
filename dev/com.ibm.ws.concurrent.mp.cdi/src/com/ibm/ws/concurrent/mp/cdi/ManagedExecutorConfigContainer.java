/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.cdi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;
import org.eclipse.microprofile.concurrent.ThreadContext;

/**
 * The purpose of this class is to wrap {@link ManagedExecutorConfig} annotations such that
 * instances are comparable via .equals() and .hashCode(). Comparing raw annotation instances
 * is insufficient because we need annotaitons like {@code @ManagedExecutorConfig(propogated = {"A","B"})}
 * and {@code @ManagedExecutorConfig(propogated = {"B","A"})} to be equal
 */
public class ManagedExecutorConfigContainer {

    private static final ManagedExecutorConfigContainer DEFAULT = new ManagedExecutorConfigContainer();

    public final Set<String> cleared = new HashSet<String>();
    public final Set<String> propogated = new HashSet<String>();
    public final int maxAsync;
    public final int maxQueued;

    public static ManagedExecutorConfigContainer get(ManagedExecutorConfig anno) {
        return anno == null ? DEFAULT : new ManagedExecutorConfigContainer(anno);
    }

    private ManagedExecutorConfigContainer() {
        // init default values manually if no config annotation present
        cleared.add(ThreadContext.TRANSACTION);
        propogated.add(ThreadContext.ALL_REMAINING);
        maxAsync = -1;
        maxQueued = -1;
    }

    private ManagedExecutorConfigContainer(ManagedExecutorConfig anno) {
        Objects.requireNonNull(anno);
        cleared.addAll(Arrays.asList(anno.cleared()));
        propogated.addAll(Arrays.asList(anno.propagated()));
        maxAsync = anno.maxAsync();
        maxQueued = anno.maxQueued();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ManagedExecutorConfigContainer))
            return false;
        ManagedExecutorConfigContainer other = (ManagedExecutorConfigContainer) obj;
        return maxAsync == other.maxAsync &&
               maxQueued == other.maxQueued &&
               Objects.equals(propogated, other.propogated) &&
               Objects.equals(cleared, other.cleared);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxAsync, maxQueued, propogated, cleared);
    }

    @Override
    public String toString() {
        return new StringBuilder("ManagedExecutorConfigContainer@")
                        .append(Integer.toHexString(hashCode()))
                        .append("(maxAsync=")
                        .append(maxAsync)
                        .append(", maxQueued=")
                        .append(maxQueued)
                        .append(", propogated=")
                        .append(propogated)
                        .append(", cleared=")
                        .append(cleared)
                        .toString();
    }
}
