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

import jakarta.data.metamodel.BooleanAttribute;
import jakarta.data.metamodel.ComparableAttribute;
import jakarta.data.metamodel.NavigableAttribute;
import jakarta.data.metamodel.NumericAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;

import test.jakarta.data.v1_1.web.Fraction.Decimal;
import test.jakarta.data.v1_1.web.Fraction.Digits;

/**
 * Static metamodel for the Fraction entity.
 */
@StaticMetamodel(Fraction.class)
public interface _Fraction {

    String DECIMAL = "decimal";
    String DECIMAL_CEILING = "decimal.ceiling";
    String DECIMAL_DIGITS = "decimal.digits";
    String DECIMAL_DIGITS_NONREPEATING = "decimal.digits.nonrepeating";
    String DECIMAL_DIGITS_REPEATING = "decimal.digits.repeating";
    String DECIMAL_INVERSE = "decimal.inverse";
    String DECIMAL_TRUNCATED = "decimal.truncated";
    String DECIMAL_TYPE = "decimal.type";
    String DECIMAL_VALUE = "decimal.value";
    String DENOMINATOR = "denominator";
    String NAME = "name";
    String NUMERATOR = "numerator";
    String REDUCED = "reduced";

    NavigableAttribute<Fraction, Decimal> decimal = //
                    NavigableAttribute.of(Fraction.class, DECIMAL, Decimal.class);

    NumericAttribute<Fraction, BigDecimal> decimal_ceiling = //
                    NumericAttribute.of(Fraction.class, DECIMAL_CEILING, BigDecimal.class);

    NavigableAttribute<Fraction, Digits> decimal_digits = //
                    NavigableAttribute.of(Fraction.class, DECIMAL_DIGITS, Digits.class);

    TextAttribute<Fraction> decimal_digits_nonrepeating = //
                    TextAttribute.of(Fraction.class, DECIMAL_DIGITS_NONREPEATING);

    TextAttribute<Fraction> decimal_digits_repeating = //
                    TextAttribute.of(Fraction.class, DECIMAL_DIGITS_REPEATING);

    NumericAttribute<Fraction, Double> decimal_inverse = //
                    NumericAttribute.of(Fraction.class, DECIMAL_INVERSE, double.class);

    NumericAttribute<Fraction, BigDecimal> decimal_truncated = //
                    NumericAttribute.of(Fraction.class, DECIMAL_TRUNCATED, BigDecimal.class);

    ComparableAttribute<Fraction, Decimal.Type> decimal_type = //
                    ComparableAttribute.of(Fraction.class, DECIMAL_TYPE, Decimal.Type.class);

    NumericAttribute<Fraction, Double> decimal_value = //
                    NumericAttribute.of(Fraction.class, DECIMAL_VALUE, double.class);

    NumericAttribute<Fraction, Integer> denominator = //
                    NumericAttribute.of(Fraction.class, DENOMINATOR, int.class);

    TextAttribute<Fraction> name = //
                    TextAttribute.of(Fraction.class, NAME);

    NumericAttribute<Fraction, Integer> numerator = //
                    NumericAttribute.of(Fraction.class, NUMERATOR, int.class);

    BooleanAttribute<Fraction> reduced = //
                    BooleanAttribute.of(Fraction.class, REDUCED, boolean.class);
}
