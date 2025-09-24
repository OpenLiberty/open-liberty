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

import jakarta.data.metamodel.ComparableAttribute;
import jakarta.data.metamodel.NavigableAttribute;
import jakarta.data.metamodel.NumericAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;

import test.jakarta.data.v1_1.web.Fraction.DecimalType;
import test.jakarta.data.v1_1.web.Fraction.Digits;

/**
 * Static metamodel for the Fraction entity.
 */
@StaticMetamodel(Fraction.class)
public interface _Fraction {

    String CEILING = "ceiling";
    String DENOMINATOR = "denominator";
    String DIGITS = "digits";
    String DIGITS_NONREPEATING = "digits.nonrepeating";
    String DIGITS_REPEATING = "digits.repeating";
    String INVERSE = "inverse";
    String NAME = "name";
    String NUMERATOR = "numerator";
    String REDUCED = "reduced";
    String TRUNCATED = "truncated";
    String TYPE = "type";
    String VALUE = "value";

    NumericAttribute<Fraction, BigDecimal> ceiling = //
                    NumericAttribute.of(Fraction.class, CEILING, BigDecimal.class);

    NumericAttribute<Fraction, Integer> denominator = //
                    NumericAttribute.of(Fraction.class, DENOMINATOR, int.class);

    NavigableAttribute<Fraction, Digits> digits = //
                    NavigableAttribute.of(Fraction.class, DIGITS, Digits.class);

    TextAttribute<Fraction> digits_nonrepeating = //
                    TextAttribute.of(Fraction.class, DIGITS_NONREPEATING);

    TextAttribute<Fraction> digits_repeating = //
                    TextAttribute.of(Fraction.class, DIGITS_REPEATING);

    NumericAttribute<Fraction, Double> inverse = //
                    NumericAttribute.of(Fraction.class, INVERSE, double.class);

    TextAttribute<Fraction> name = //
                    TextAttribute.of(Fraction.class, NAME);

    NumericAttribute<Fraction, Integer> numerator = //
                    NumericAttribute.of(Fraction.class, NUMERATOR, int.class);

    ComparableAttribute<Fraction, Boolean> reduced = //
                    ComparableAttribute.of(Fraction.class, REDUCED, boolean.class);

    NumericAttribute<Fraction, BigDecimal> truncated = //
                    NumericAttribute.of(Fraction.class, TRUNCATED, BigDecimal.class);

    ComparableAttribute<Fraction, DecimalType> type = //
                    ComparableAttribute.of(Fraction.class, TYPE, DecimalType.class);

    NumericAttribute<Fraction, Double> value = //
                    NumericAttribute.of(Fraction.class, VALUE, double.class);
}
