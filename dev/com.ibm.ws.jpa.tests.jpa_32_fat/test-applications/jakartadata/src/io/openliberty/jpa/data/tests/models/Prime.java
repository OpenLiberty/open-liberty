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

import java.util.ArrayList;

import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

/**
 * Recreate from io.openliberty.data.internal_fat
 */
@Entity
public class Prime {
    public String binaryDigits;

    public boolean even;

    public String hex;

    public String name;

    @Id
    public long numberId;

    public String romanNumeral;

    @ElementCollection(fetch = FetchType.EAGER)
    public ArrayList<String> romanNumeralSymbols;
    
    public int sumOfBits;

    public static Prime of(long number, String romanNumeral, String name) {
        Prime inst = new Prime();
        inst.binaryDigits = Long.toBinaryString(number);
        inst.even = number % 2 == 0;
        inst.hex = Long.toHexString(number);
        inst.name = name;
        inst.romanNumeral = romanNumeral;
        inst.numberId = number;
        inst.sumOfBits = Long.bitCount(number);
        if (romanNumeral != null) {
            inst.romanNumeralSymbols = new ArrayList<>(romanNumeral.length());
            for (int i = 0; i < romanNumeral.length(); i++)
                inst.romanNumeralSymbols.add(romanNumeral.substring(i, i + 1));
        }
        return inst;
    }
}
