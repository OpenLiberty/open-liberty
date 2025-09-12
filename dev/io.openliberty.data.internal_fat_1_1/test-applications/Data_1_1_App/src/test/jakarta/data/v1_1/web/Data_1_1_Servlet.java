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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.expression.TextExpression;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class Data_1_1_Servlet extends FATServlet {

    @Inject
    Fractions fractions;

    /**
     * Initialize read-only data that is prepopulated for tests
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        // Fractions including 1/2, 1/3, 2/3, ... 19/20
        Set<Fraction> fractionsToAdd = new HashSet<Fraction>();
        for (int d = 2; d <= 20; d++)
            for (int n = 1; n < d; n++) {
                Fraction f = Fraction.of(n, d);
                System.out.println(f);
                fractionsToAdd.add(f);
            }
        fractions.supply(fractionsToAdd);
    }

    /**
     * Tests that the Is annotation can be applied to repository method parameters
     * to enforce constraints on minimum and maximum values.
     */
    @Test
    public void testIsAnnoAtLeastAndAtMost() {

        assertEquals(List.of("One Third",
                             "One Fourth",
                             "One Fifth",
                             "Two Thirds",
                             "Two Fourths",
                             "Two Fifths",
                             "Three Fourths",
                             "Three Fifths",
                             "Four Fifths"),
                     fractions.havingDenominatorWithin(3, 5)
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Tests that the Is annotation can be applied to repository method parameters
     * to enforce constraints on equality and inequality.
     */
    @Test
    public void testIsAnnoEqualityAndInequality() {
        Order<Fraction> order = Order.by(Sort.desc(_Fraction.VALUE));

        assertEquals(List.of("Four Fifths",
                             "Two Fifths",
                             "One Fifth"),
                     fractions.withDenominatorButNotNumerator(5,
                                                              3,
                                                              order)
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Attempt to use the static metamodel to create an expression for a
     * negative length prefix of an entity attribute value. Verify that
     * IllegalArgumentException is raised for the negative length and that
     * the message includes a message argument, which in this case is
     * "length", which is the name of the parameter that is received by the
     * left method.
     */
    @Test
    public void testMessageIncludesArg() {
        try {
            TextExpression<Fraction> negativeLengthPrefix = //
                            _Fraction.name.left(-2);

            fail("Created an expression for a negative length prefix: " +
                 negativeLengthPrefix);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() != null &&
                x.getMessage().contains("length"))
                ; // pass
            else
                throw x;
        }
    }
}
