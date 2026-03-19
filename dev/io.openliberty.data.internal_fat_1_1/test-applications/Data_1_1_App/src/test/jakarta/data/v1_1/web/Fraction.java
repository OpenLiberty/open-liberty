/*******************************************************************************
 * Copyright (c) 2025,2026 IBM Corporation and others.
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

    @Embedded
    Decimal decimal;

    @Column(nullable = false)
    int denominator;

    @Column(nullable = false)
    @Id
    String name;

    @Column(nullable = false)
    int numerator;

    @Column(nullable = false)
    boolean reduced;

    @Embeddable
    public static record Decimal(
                    @Column(nullable = false, table = "Fraction") //
                    double value,

                    @Column(nullable = false, table = "Fraction") //
                    Type type,

                    @Column(nullable = false, table = "Fraction") //
                    double inverse,

                    @Column(nullable = false, precision = 12, scale = 4, table = "Fraction") //
                    BigDecimal ceiling, // rounded up 4 digits past the decimal point

                    @Column(nullable = false, precision = 10, scale = 4, table = "Fraction") //
                    BigDecimal truncated, // truncated 4 digits past the decimal point

                    @Embedded Digits digits) {

        public static enum Type {
            REPEATING,
            TERMINATING
        }

        public static Decimal of(int numerator, int denominator) {
            long[] dec = new long[MAX_DIGITS];
            long[] rem = new long[MAX_DIGITS];
            rem[0] = numerator;
            Digits digits = null;
            Type type = null;
            for (int i = 1; type == null && i <= MAX_DIGITS; i++) {
                long n = rem[i - 1] * 10;
                dec[i] = n / denominator;
                rem[i] = n % denominator;
                if (rem[i] == 0) {
                    digits = Digits.of(dec, i, i);
                    type = Type.TERMINATING;
                } else {
                    // find out if remaining amount is already found in list
                    for (int prev = i - 1; prev >= 0; prev--)
                        if (rem[i] == rem[prev]) {
                            digits = Digits.of(dec, prev, i);
                            type = Type.REPEATING;
                        }
                }
            }

            if (type == null)
                throw new IllegalArgumentException(numerator + " / " + denominator +
                                                   " has too many fractional digits: " +
                                                   Arrays.toString(dec) + "...");

            BigDecimal truncated = BigDecimal
                            .valueOf(numerator * 10000
                                     / denominator,
                                     4);
            BigDecimal ceiling = BigDecimal
                            .valueOf((numerator * 10000 + denominator - 1)
                                     / denominator,
                                     4);
            double value = (double) numerator / (double) denominator;
            double inverse = (double) denominator / (double) numerator;

            return new Decimal(value, type, inverse, ceiling, truncated, digits);
        }
    }

    @Embeddable
    public static record Digits(
                    @Column(nullable = false, table = "Fraction") //
                    String nonrepeating,

                    @Column(nullable = false, table = "Fraction") //
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
                                                             "Twentieth",
                                                             "Twenty-first",
                                                             "Twenty-second"
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
                                                           "Twenty",
                                                           "Twenty-one",
                                                           "Twenty-two"
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
        f.decimal = Decimal.of(numerator, denominator);

        f.reduced = true;

        for (int i = 2; f.reduced && i <= numerator; i++)
            if (numerator % i == 0 &&
                denominator % i == 0)
                f.reduced = false;

        return f;
    }

    @Override
    public String toString() {
        int tenThousandths = decimal.truncated.movePointRight(4).intValue();
        return new StringBuilder("Fraction ")
                        .append(numerator)
                        .append('/')
                        .append(denominator)
                        .append(" ~0.⌊")
                        .append(String.format("%04d", tenThousandths))
                        .append("⌋ =0.")
                        .append(decimal.digits)
                        .append(' ')
                        .append(decimal.type)
                        .append(' ')
                        .append(name)
                        .toString();
    }
}
