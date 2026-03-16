/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import test.jakarta.data.v1_1.web.Fraction.Decimal;
import test.jakarta.data.v1_1.web.Fraction.Digits;

/**
 * Static metamodel for the Fraction.Decimal embeddable.
 */
@StaticMetamodel(Decimal.class)
public interface _Decimal {

    String CEILING = "ceiling";
    String DIGITS = "digits";
    String DIGITS_NONREPEATING = "digits.nonrepeating";
    String DIGITS_REPEATING = "digits.repeating";
    String INVERSE = "inverse";
    String TRUNCATED = "truncated";
    String TYPE = "type";
    String VALUE = "value";

    NumericAttribute<Decimal, BigDecimal> ceiling = //
                    NumericAttribute.of(Decimal.class, CEILING, BigDecimal.class);

    NavigableAttribute<Decimal, Digits> digits = //
                    NavigableAttribute.of(Decimal.class, DIGITS, Digits.class);

    TextAttribute<Decimal> digits_nonrepeating = //
                    TextAttribute.of(Decimal.class, DIGITS_NONREPEATING);

    TextAttribute<Decimal> digits_repeating = //
                    TextAttribute.of(Decimal.class, DIGITS_REPEATING);

    NumericAttribute<Decimal, Double> decimal_inverse = //
                    NumericAttribute.of(Decimal.class, INVERSE, double.class);

    NumericAttribute<Decimal, BigDecimal> truncated = //
                    NumericAttribute.of(Decimal.class, TRUNCATED, BigDecimal.class);

    ComparableAttribute<Decimal, Decimal.Type> type = //
                    ComparableAttribute.of(Decimal.class, TYPE, Decimal.Type.class);

    NumericAttribute<Decimal, Double> value = //
                    NumericAttribute.of(Decimal.class, VALUE, double.class);
}
