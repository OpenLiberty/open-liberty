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
package com.ibm.ws.anno.util.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import com.ibm.wsspi.anno.util.Util_InternMap;

/**
 * Exported identity set.  This is a read-only collection of strings which is
 * used to expose the identity sets of bidirectional mappings.
 * 
 * This intermediary changes containment to use {@link Object#equals}, which is
 * necessary for any export of the held and holder sets of a bidirectional mapping.
 * 
 * Usually, external containment tests use a non-interned string.  Containment
 * tests would usually fail for directly exposed held and holder sets.
 */
public class UtilImpl_ExportedIdentitySet implements Set<String> {
    protected UtilImpl_ExportedIdentitySet(UtilImpl_InternMap identityMap, Set<String> identitySet) {
        this.identityMap = identityMap;
        this.identitySet = identitySet;
    }

    private final UtilImpl_InternMap identityMap;
    private final Set<String> identitySet;

    // Operations which redirect to the base identity set.
    
    @Override
    public int size() {
        return identitySet.size();
    }

    @Override
    public boolean isEmpty() {
        return identitySet.isEmpty();
    }

    @Override
    public Iterator<String> iterator() {
        return identitySet.iterator();
    }

    @Override
    public Object[] toArray() {
        return identitySet.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return identitySet.toArray(a);
    }

    // Operations which change containment testing to use Object.equals.

    @Override
    public boolean contains(Object o) {
        if ( o == null ) {
            return false;
        } else if ( !(o instanceof String) ) {
            return false;
        }

        String i_o = identityMap.intern((String) o, Util_InternMap.DO_NOT_FORCE);
        if ( i_o == null ) {
            return false;
        }
        return identitySet.contains(i_o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for ( Object o : c ) {
            if ( !contains(o) ) {
                return false;
            }
        }
        return true;
    }

    // Unsupported options since this is a read-only collection.

    @Override
    public boolean add(String e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
