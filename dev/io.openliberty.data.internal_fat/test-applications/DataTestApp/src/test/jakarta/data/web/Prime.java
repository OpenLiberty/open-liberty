/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import java.util.ArrayList;

import jakarta.data.Entity;
import jakarta.data.Id;

/**
 *
 */
@Entity
public class Prime {
    public String binary;

    public boolean even;

    public String hex;

    public String name;

    @Id("NUMBER")
    public long number;

    public String romanNumeral;

    public ArrayList<String> romanNumeralSymbols;

    public int sumOfBits;

    public Prime() {
    }

    public Prime(long number, String hexadecimal, String binary, int sumOfBits, String romanNumeral, String name) {
        this.binary = binary;
        this.even = number % 2 == 0;
        this.hex = hexadecimal;
        this.name = name;
        this.romanNumeral = romanNumeral;
        this.number = number;
        this.sumOfBits = sumOfBits;
        if (romanNumeral != null) {
            this.romanNumeralSymbols = new ArrayList<>(romanNumeral.length());
            for (int i = 0; i < romanNumeral.length(); i++)
                romanNumeralSymbols.add(romanNumeral.substring(i, i + 1));
        }
    }

    @Override
    public String toString() {
        return "Prime@" + hex + " #" + number;
    }
}
