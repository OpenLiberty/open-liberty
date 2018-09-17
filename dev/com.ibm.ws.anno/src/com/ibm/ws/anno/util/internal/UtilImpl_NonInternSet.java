/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2014, 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.anno.util.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;

import com.ibm.wsspi.anno.util.Util_InternMap;

/**
 * <p>Utility set for removing identity semantics.</p>
 *
 * <p>Necessary because external callers to the annotations
 * framework must be given sets which implement containment
 * using usual set equality.</p>
 */
public class UtilImpl_NonInternSet implements Set<String> {

    public UtilImpl_NonInternSet(Util_InternMap internMap, Set<String> base) {
        this.internMap = internMap;
        this.base = base;
    }

    //

    protected final Util_InternMap internMap;
    protected final Set<String> base;

    @Trivial
    public Util_InternMap getInternMap() {
        return internMap;
    }

    @Trivial
    public Set<String> getBase() {
        return base;
    }

    //

    @Override
    public int size() {
        return base.size();
    }

    @Override
    public boolean contains(Object candidateObject) {
        if ( candidateObject == null ) {
//            System.out.println("NonIntern: Candidate [ " + candidateObject + " ] [ false (null) ]");
            return false;
        } else if ( !(candidateObject instanceof String) ) {
//            System.out.println("NonIntern: Candidate [ " + candidateObject + " ] [ false (non-string) ]");
            return false;
        }

        String candidateString = (String) candidateObject;

        String i_candidateString = internMap.intern(candidateString, Util_InternMap.DO_NOT_FORCE);
        if ( i_candidateString == null ) {
//            System.out.println("NonIntern: Candidate [ " + candidateObject + " ] [ false (not interned) ]");
            return false;
        }

        boolean result = base.contains(i_candidateString);
//        System.out.println("NonIntern: Candidate [ " + candidateObject + " ] [ " + result + " (of " + base.size() + " elements) ]");
        return result;
    }

    @Override
    public boolean containsAll(Collection<?> candidateCollection) {
        for ( Object candidateObject : candidateCollection ) {
            if ( !contains(candidateObject) ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object object) {
        return base.equals(object);
    }

    @Override
    public int hashCode() {
        return base.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty();
    }

    @Override
    public Iterator<String> iterator() {
        return base.iterator();
    }

    @Override
    public Object[] toArray() {
        return base.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return base.toArray(array);
    }

    // Unsupported

    @Override
    public boolean add(String object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends String> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
