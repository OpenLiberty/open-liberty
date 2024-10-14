/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;

import java.io.Serializable;

/**
 * Recreate from Jakarta Data TCK
 */
@jakarta.persistence.Entity
public class NaturalNumber implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum NumberType {
        ONE, PRIME, COMPOSITE
    }

    @jakarta.persistence.Id
    private long id; //AKA the value

    private boolean isOdd;

    private Short numBitsRequired;

    private NumberType numType; // enum of ONE | PRIME | COMPOSITE

    private long floorOfSquareRoot;

    public static NaturalNumber of(int value) {
        boolean isOne = value == 1;
        boolean isOdd = value % 2 == 1;
        long sqrRoot = squareRoot(value);
        boolean isPrime = isOdd ? isPrime(value, sqrRoot) : (value == 2);

        NaturalNumber inst = new NaturalNumber();
        inst.id = value;
        inst.isOdd = isOdd;
        inst.numBitsRequired = bitsRequired(value);
        inst.numType = isOne ? NumberType.ONE : isPrime ? NumberType.PRIME : NumberType.COMPOSITE;
        inst.floorOfSquareRoot = sqrRoot;

        return inst;
    }

    private static Short bitsRequired(int value) {
        return (short) (Math.floor(Math.log(value) / Math.log(2)) + 1);
    }

    private static long squareRoot(int value) {
        return (long) Math.floor(Math.sqrt(value));
    }

    private static boolean isPrime(int value, long largestPossibleFactor) {
        if (value == 1)
            return false;

        for (int i = 2; i <= largestPossibleFactor; i++) {
            if (value % i == 0)
                return false;
        }
        return true;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isOdd() {
        return isOdd;
    }

    public void setOdd(boolean isOdd) {
        this.isOdd = isOdd;
    }

    public Short getNumBitsRequired() {
        return numBitsRequired;
    }

    public void setNumBitsRequired(Short numBitsRequired) {
        this.numBitsRequired = numBitsRequired;
    }

    public NumberType getNumType() {
        return numType;
    }

    public void setNumType(NumberType numType) {
        this.numType = numType;
    }

    public long getFloorOfSquareRoot() {
        return floorOfSquareRoot;
    }

    public void setFloorOfSquareRoot(long floorOfSquareRoot) {
        this.floorOfSquareRoot = floorOfSquareRoot;
    }
}
