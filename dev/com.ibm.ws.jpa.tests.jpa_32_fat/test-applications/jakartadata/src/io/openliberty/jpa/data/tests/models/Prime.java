/**
 *
 */
package io.openliberty.jpa.data.tests.models;

import java.util.ArrayList;

import jakarta.persistence.Entity;
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
