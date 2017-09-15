/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.util.internal;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

//TODO: Should the iterator handle concurrent modifications?
//    For now, for simplicity, it doesn't.

/**
 * <p>An set for strings based on string identity.</p>
 */
public class UtilImpl_IdentityStringSet implements Set<String> {
    public UtilImpl_IdentityStringSet() {
        super();

        this.size = 0;
        this.storage = new String[size];
        this.factor = DOUBLING_FACTOR;
    }

    public UtilImpl_IdentityStringSet(int factor) {
        super();

        this.size = 0;
        this.storage = new String[size];
        this.factor = factor;
    }

    //

    //Default Factor is used as a multiplier on expansion. So array will grow from..
    //0 , 1, 2, 4, 8, 16, etc. for a factor of two. Two is fairly standard for growing arrays.
    //This is only the INITIAL factor. After a size of 64, we instead grow by a 20% fixed amount.
    protected static final int DOUBLING_FACTOR = 2;

    //The largest size at below which we still double on every grow operation.
    protected static final int MAX_DOUBLING_SIZE = 64;
    protected final int factor;

    //indicates a percentage to grow the size of the set. For example, "5" would indicate "1/5" 
    //a 20% growing factor.
    protected static final int PERCENTAGE_FACTOR = 5;

    public int getFactor() {
        return factor;
    }

    //

    protected int size;
    protected String[] storage;

    protected String[] getStorage() {
        return storage;
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return size;
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        this.size = 0;
        this.storage = new String[size];
    }

    protected String[] growStorage() {
        int priorLength = this.storage.length;
        //String[] newStorage = (String[]) new Object[priorLength + this.factor];

        int newLength = 0;
        if (priorLength < MAX_DOUBLING_SIZE)
            newLength = (priorLength == 0 ? 1 : priorLength * this.factor);
        else
            newLength = priorLength + (priorLength / PERCENTAGE_FACTOR);

        String[] newStorage = new String[newLength];

        System.arraycopy(this.storage, 0, newStorage, 0, priorLength);
        return newStorage;
    }

    //

    /** {@inheritDoc} */
    @Override
    public boolean add(String i_newElement) {
        for (int offset = 0; offset < this.size; offset++) {
            //Note that this LOOKS like improper string comparison, but since we are using interned strings
            //(i_strings) it's OK. The entire point of this data structure is that passed in strings are
            //object-equal in addition to String equal.

            //This has been added to findbugs ignore.
            if (this.storage[offset] == i_newElement) {
                return false;
            }
        }

        if (this.size == this.storage.length) {
            this.storage = growStorage();
        }

        this.storage[this.size++] = i_newElement;

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean addAll(Collection<? extends String> newElements) {
        boolean didAdd = false;
        for (String newElement : newElements) {
            if (add(newElement)) {
                didAdd = true;
            }
        }
        return didAdd;
    }

    /** {@inheritDoc} */
    @Override
    public boolean contains(Object candidateElement) {
        if (this.size == 0) {
            return false;
        }
        for (String currentElement : this.storage) {
            if (currentElement == candidateElement) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsAll(Collection<?> collection) {
        for (Object nextCandidateElement : collection) {
            if (!contains(nextCandidateElement)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return (this.size == 0);
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(Object object) {
        for (int offset = 0; offset < this.size; offset++) {
            if (this.storage[offset] == object) {
                System.arraycopy(this.storage, offset + 1, this.storage, offset, this.size - (offset + 1));
                this.storage[--this.size] = null;
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeAll(Collection<?> candidateElements) {
        boolean didRemoveAny = false;
        for (Object nextCandidate : candidateElements) {
            if (remove(nextCandidate)) {
                didRemoveAny = true;
            }
        }
        return didRemoveAny;
    }

    /** {@inheritDoc} */
    @Override
    public boolean retainAll(Collection<?> collection) {
        boolean didOmitAny = false;
        for (int offset = 0; offset < this.size; offset++) {
            if (!collection.contains(this.storage[offset])) {
                System.arraycopy(this.storage, offset + 1, this.storage, offset, this.size - (offset + 1));
                this.storage[--this.size] = null;
                didOmitAny = true;
            }
        }
        return didOmitAny;
    }

    /** {@inheritDoc} */
    @Override
    public Object[] toArray() {
        Object[] result = new Object[this.size];
        System.arraycopy(this.storage, 0, result, 0, this.size);
        return result;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <S> S[] toArray(S[] targetArray) {
        if (this.size > targetArray.length) {
            Class<?> componentType = this.storage.getClass().getComponentType();
            targetArray = (S[]) Array.newInstance(componentType, this.size);
        }

        System.arraycopy(this.storage, 0, targetArray, 0, this.size);

        if (this.size < targetArray.length) {
            for (int offset = this.size; offset < targetArray.length; offset++) {
                targetArray[offset] = null;
            }
        }

        return targetArray;
    }

    //

    /** {@inheritDoc} */
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

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return (this.offset < size);
        }

        /** {@inheritDoc} */
        @Override
        public String next() {
            if (this.offset < size) {
                return storage[this.offset++];
            } else {
                throw new NoSuchElementException();
            }
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            if (this.offset < size) {
                System.arraycopy(storage, offset + 1, storage, offset, size - (offset + 1));
                storage[--size] = null;
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
