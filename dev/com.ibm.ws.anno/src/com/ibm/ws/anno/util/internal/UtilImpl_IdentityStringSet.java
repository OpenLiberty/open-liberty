/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.util.internal;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

// Should the iterator handle concurrent modifications?
//  For now, for simplicity, it doesn't.

/**
 * <p>An set for strings based on string identity.</p>
 *
 * <p>This implements storage as a simple array, using linear time
 * operations to find and remove elements.</p>
 */
public class UtilImpl_IdentityStringSet implements Set<String> {

    // Default parameters:

    // Default Factor is used as a multiplier on expansion. So array will
    // grow from 0 , 1, 2, 4, 8, 16, etc. for a factor of two. Two is fairly
    // standard for growing arrays.  This is only the INITIAL factor. After
    // a size of 64, we instead grow by a 20% fixed amount.
    protected static final int DOUBLING_FACTOR = 2;

    // The largest size at below which we still double on every grow operation.
    protected static final int MAX_DOUBLING_SIZE = 64;

    // Indicates a percentage to grow the size of the set. For example,
    // "5" would indicate "1/5", a 20% growing factor.
    protected static final int PERCENTAGE_FACTOR = 5;

    // Storage for the fixed empty string array; used when the set is empty.
    protected static final String[] EMPTY_STRING_ARRAY = new String[0];

    //

    public UtilImpl_IdentityStringSet() {
        super();

        this.factor = DOUBLING_FACTOR;

        this.size = 0;
        this.storage = EMPTY_STRING_ARRAY;
    }

    public UtilImpl_IdentityStringSet(int factor) {
        super();

        this.factor = factor;

        this.size = 0;
        this.storage = EMPTY_STRING_ARRAY;
    }

    public UtilImpl_IdentityStringSet(int initialStorage, int factor) {
        super();

        this.factor = factor;

        this.size = 0;
        this.storage = ((initialStorage == 0) ? EMPTY_STRING_ARRAY : new String[initialStorage]);
    }

    //

    protected final int factor;

    public int getFactor() {
        return factor;
    }

    //

    protected int size;
    protected String[] storage;

    @Override
    public int size() {
        return size;
    }

    protected String[] getStorage() {
        return storage;
    }

    protected String[] growStorage(String[] oldStorage) {
        int oldLength = oldStorage.length;

        int newLength;
        if ( oldLength < MAX_DOUBLING_SIZE ) {
            newLength = ((oldLength == 0) ? 1 : (oldLength * factor));
        } else {
            newLength = oldLength + (oldLength / PERCENTAGE_FACTOR);
        }

        String[] newStorage = new String[newLength];

        System.arraycopy(oldStorage, 0,
                         newStorage, 0,
                         oldLength);

        return newStorage;
    }

    public static final int TRIM_LIMIT = 4;

    public void trimStorage() {
        if ( (storage == null) || size == storage.length ) {
            return;
        }

        // e.g., if the difference is less than 1/4'th of the size, don't bother
        // reallocating.

        if ( ((storage.length - size) * TRIM_LIMIT) < size ) {
            return;
        }

        String[] newStorage;

        if ( size == 0 ) {
            newStorage = EMPTY_STRING_ARRAY;

        } else {
            newStorage = new String[size];
            System.arraycopy(storage, 0, newStorage, 0, size);
        }

        storage = newStorage;
    }

    //

    @Override
    public boolean add(String i_newElement) {
        for ( int offset = 0; offset < size; offset++ ) {
            // Note that this LOOKS like improper string comparison, but since we are
            // using interned strings (i_strings) it's OK. The entire point of this data
            // structure is that passed in strings are object-equal in addition to
            // String equal.

            // This has been added to findbugs ignore.

            if ( storage[offset] == i_newElement ) {
                return false;
            }
        }

        if ( size == storage.length ) {
            storage = growStorage(storage);
        }

        storage[size++] = i_newElement;

        return true;
    }

    @Override
    public boolean addAll(Collection<? extends String> newElements) {
        boolean didAdd = false;
        for ( String newElement : newElements ) {
            if ( add(newElement) ) {
                didAdd = true;
            }
        }
        return didAdd;
    }

    @Override
    public void clear() {
        size = 0;
        storage = EMPTY_STRING_ARRAY;
    }

    //

    @Override
    public boolean contains(Object candidateElement) {
        if ( size == 0 ) {
            return false;
        }

        for ( String currentElement : storage ) {
            if ( currentElement == candidateElement ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        for ( Object nextCandidateElement : collection ) {
            if ( !contains(nextCandidateElement) ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        return ( size == 0 );
    }

    @Override
    public boolean remove(Object object) {
        for ( int offset = 0; offset < size; offset++ ) {
            if ( storage[offset] == object ) {
                System.arraycopy(storage, offset + 1,
                                 storage, offset,
                                 size - (offset + 1));
                storage[--size] = null;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean removeAll(Collection<?> candidateElements) {
        boolean didRemoveAny = false;
        for ( Object nextCandidate : candidateElements ) {
            if ( remove(nextCandidate) ) {
                didRemoveAny = true;
            }
        }
        return didRemoveAny;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        boolean didOmitAny = false;
        for (int offset = 0; offset < size; offset++) {
            if ( !collection.contains(storage[offset]) ) {
                System.arraycopy(storage, offset + 1,
                                 storage, offset,
                                 size - (offset + 1));
                storage[--size] = null;
                didOmitAny = true;
            }
        }
        return didOmitAny;
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[size];
        System.arraycopy(storage, 0, result, 0, size);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> S[] toArray(S[] targetArray) {
        if ( size > targetArray.length ) {
            Class<?> componentType = storage.getClass().getComponentType();
            targetArray = (S[]) Array.newInstance(componentType, size);
        }

        System.arraycopy(storage, 0, targetArray, 0, size);

        if ( size < targetArray.length ) {
            for ( int offset = size; offset < targetArray.length; offset++ ) {
                targetArray[offset] = null;
            }
        }

        return targetArray;
    }

    //

    @Override
    public Iterator<String> iterator() {
        return new SimpleStringIterator();
    }

    protected class SimpleStringIterator implements Iterator<String> {

        protected int offset;

        protected SimpleStringIterator() {
            super();

            this.offset = 0;
        }

        @Override
        public boolean hasNext() {
            return ( this.offset < size );
        }

        @Override
        public String next() {
            if ( this.offset < size ) {
                return storage[this.offset++];
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            if ( this.offset < size ) {
                System.arraycopy(storage, offset + 1,
                                 storage, offset,
                                 size - (offset + 1));
                storage[--size] = null;
            } else {
                throw new NoSuchElementException();
            }
        }
    }

    public boolean i_equals(UtilImpl_IdentityStringSet otherSet) {
        if ( otherSet == null ) {
            return false;
        } else if ( otherSet == this ) {
            return true;
        } else if ( otherSet.size() != size() ) {
            return false;
        } else {
            return containsAll(otherSet);
        }
    }
}
