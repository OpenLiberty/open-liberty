/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.v1_1.web;

import java.math.BigDecimal;
import java.util.Arrays;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity for representing fractions less than 1, such as 3/4 or 5/6.
 * This includes an embeddable with the nonrepeating and repeating digits,
 * and various other attribute types that we will want test coverage for.
 */
@Entity
public class Fraction {

    @Column(nullable = false)
    BigDecimal ceiling; // ceiling 4 digits past the decimal point

    @Column(nullable = false)
    int denominator;

    @Embedded
    Digits digits;

    @Column(nullable = false)
    double inverse;

    @Column(nullable = false)
    @Id
    String name;

    @Column(nullable = false)
    int numerator;

    @Column(nullable = false)
    boolean reduced;

    @Column(nullable = false)
    BigDecimal truncated; // truncated to 4 digits past the decimal point

    @Column(nullable = false)
    DecimalType type;

    @Column(nullable = false)
    double value;

    public static enum DecimalType {
        REPEATING,
        TERMINATING
    }

    @Embeddable
    public static record Digits(
                    // TODO switch to false once #29460 is fixed
                    @Column(nullable = true, table = "Fraction") //
                    String nonrepeating,

                    // TODO switch to false once #29460 is fixed
                    @Column(nullable = true, table = "Fraction") //
                    String repeating) {

        static Digits of(long[] digitValues, int nonRepeating, int total) {
            StringBuilder nonrep = new StringBuilder();
            StringBuilder rep = new StringBuilder();
            for (int i = 1; i <= nonRepeating; i++)
                nonrep.append(digitValues[i]);
            for (int i = nonRepeating + 1; i <= total; i++)
                rep.append(digitValues[i]);
            return new Digits(nonrep.toString(), rep.toString());
        }

        @Override
        public String toString() {
            return repeating.length() > 0 //
                            ? nonrepeating + repeating + repeating + "..." //
                            : nonrepeating;
        }
    }

    static final int MAX_DIGITS = 50;

    static final String[] DENOMINATOR_NAMES = new String[] {
                                                             null,
                                                             null,
                                                             "Half",
                                                             "Third",
                                                             "Fourth",
                                                             "Fifth",
                                                             "Sixth",
                                                             "Seventh",
                                                             "Eighth",
                                                             "Ninth",
                                                             "Tenth",
                                                             "Eleventh",
                                                             "Twelfth",
                                                             "Thirteenth",
                                                             "Fourteenth",
                                                             "Fifteenth",
                                                             "Sixteenth",
                                                             "Seventeenth",
                                                             "Eighteenth",
                                                             "Nineteenth",
                                                             "Twentieth"
    };

    static final String[] NUMERATOR_NAMES = new String[] {
                                                           "Zero",
                                                           "One",
                                                           "Two",
                                                           "Three",
                                                           "Four",
                                                           "Five",
                                                           "Six",
                                                           "Seven",
                                                           "Eight",
                                                           "Nine",
                                                           "Ten",
                                                           "Eleven",
                                                           "Twelve",
                                                           "Thirteen",
                                                           "Fourteen",
                                                           "Fifteen",
                                                           "Sixteen",
                                                           "Seventeen",
                                                           "Eighteen",
                                                           "Nineteen",
                                                           "Twenty"
    };

    public static Fraction of(int numerator, int denominator) {
        if (numerator < 0 || numerator >= NUMERATOR_NAMES.length)
            throw new IllegalArgumentException("numerator: " + numerator);

        if (denominator <= 0 || denominator >= DENOMINATOR_NAMES.length)
            throw new IllegalArgumentException("denominator: " + denominator);

        if (numerator >= denominator)
            throw new IllegalArgumentException(numerator + " / " + denominator +
                                               " >= 1");

        Fraction f = new Fraction();

        f.name = NUMERATOR_NAMES[numerator] + ' ' +
                 DENOMINATOR_NAMES[denominator] +
                 (numerator == 1 ? "" : "s");

        f.numerator = numerator;
        f.denominator = denominator;
        f.truncated = BigDecimal.valueOf(numerator * 10000
                                         / denominator,
                                         4);
        f.ceiling = BigDecimal.valueOf((numerator * 10000 + denominator - 1)
                                       / denominator,
                                       4);
        f.value = (double) numerator / (double) denominator;
        f.inverse = (double) denominator / (double) numerator;

        f.reduced = true;

        for (int i = 2; f.reduced && i <= numerator; i++)
            if (numerator % i == 0 &&
                denominator % i == 0)
                f.reduced = false;

        long[] digits = new long[MAX_DIGITS];
        long[] rem = new long[MAX_DIGITS];
        rem[0] = numerator;
        for (int i = 1; f.type == null && i <= MAX_DIGITS; i++) {
            long n = rem[i - 1] * 10;
            digits[i] = n / denominator;
            rem[i] = n % denominator;
            if (rem[i] == 0) {
                f.digits = Digits.of(digits, i, i);
                f.type = DecimalType.TERMINATING;
            } else {
                // find out if remaining amount is already found in list
                for (int prev = i - 1; prev >= 0; prev--)
                    if (rem[i] == rem[prev]) {
                        f.digits = Digits.of(digits, prev, i);
                        f.type = DecimalType.REPEATING;
                    }
            }
        }

        if (f.type == null)
            throw new IllegalArgumentException(numerator + " / " + denominator +
                                               " has too many fractional digits: " +
                                               Arrays.toString(digits) + "...");

        // TODO remove once #29460 is fixed
        f.digits = null;

        return f;
    }

    @Override
    public String toString() {
        return new StringBuilder("Fraction ")
                        .append(numerator)
                        .append('/')
                        .append(denominator)
                        .append(" ~0.⌊")
                        .append(String.format("%04d",
                                              truncated.movePointRight(4).intValue()))
                        .append("⌋ =0.")
                        //.append(digits)
                        .append(' ')
                        .append(type)
                        .append(' ')
                        .append(name)
                        .toString();
    }
}
