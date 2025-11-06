/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
import java.util.List;

/**
 * Entity class for which data is pre-populated.
 * This should be treated as read-only to avoid interference between tests.
 */
public class Prime {
    public String binaryDigits;

    public boolean even;

    public String hex;

    public String name;

    public long numberId;

    public String romanNumeral;

    public List<String> romanNumeralSymbols; //TODO revert to ArrayList once https://github.com/OpenLiberty/open-liberty/issues/33205 is closed

    public int sumOfBits;

    public Prime() {
    }

    public Prime(long number, String hexadecimal, String binary, int sumOfBits, String romanNumeral, String name) {
        this.binaryDigits = binary;
        this.even = number % 2 == 0;
        this.hex = hexadecimal;
        this.name = name;
        this.romanNumeral = romanNumeral;
        this.numberId = number;
        this.sumOfBits = sumOfBits;
        if (romanNumeral != null) {
            this.romanNumeralSymbols = new ArrayList<>(romanNumeral.length());
            for (int i = 0; i < romanNumeral.length(); i++)
                romanNumeralSymbols.add(romanNumeral.substring(i, i + 1));
        }
    }

    @Override
    public String toString() {
        return "Prime@" + hex + " #" + numberId;
    }
}
