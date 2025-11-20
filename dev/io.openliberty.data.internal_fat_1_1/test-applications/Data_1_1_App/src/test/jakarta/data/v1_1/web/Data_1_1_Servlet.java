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

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.constraint.AtMost;
import jakarta.data.constraint.Between;
import jakarta.data.constraint.In;
import jakarta.data.constraint.Like;
import jakarta.data.constraint.NotBetween;
import jakarta.data.constraint.NotNull;
import jakarta.data.expression.TextExpression;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Cursor;
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
     * Tests that the Between and NotBetween constraint types can be assigned to
     * repository method parameters to enforce that matching entity attributes
     * are either within or not within a range.
     */
    @Test
    public void testBetweenAndNotBetweenConstraints() {

        assertEquals(List.of("One Fourteenth",
                             "Eleven Fourteenths",
                             "One Fifteenth",
                             "Four Fifteenths",
                             "Eleven Fifteenths",
                             "Fourteen Fifteenths",
                             "One Sixteenth",
                             "Eleven Sixteenths",
                             "Fifteen Sixteenths"),
                     fractions.withDenominatorBetweenNamedBeforeAndNumeratorNotBetween //
                     (Between.bounds(14, 16),
                      "Thirteen",
                      NotBetween.bounds(5, 10),
                      true)
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
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
     * Tests that the Is annotation can be applied to repository method parameters
     * to enforce matching a pattern. Use cursor-based pagination to obtain results.
     */
    @Test
    public void testIsAnnoLike() {

        Order<Fraction> descendingNumerator = //
                        Order.by(Sort.desc(_Fraction.NUMERATOR));

        Cursor threeFifths = Cursor.forKey(5, 3);

        PageRequest page2Req = PageRequest.ofSize(5)
                        .afterCursor(threeFifths)
                        .page(2)
                        .withoutTotal();

        CursoredPage<Fraction> page2 = fractions.namedLike("%fths",
                                                           descendingNumerator,
                                                           page2Req);
        assertEquals(List.of("Two Fifths",
                             "Eleven Twelfths",
                             "Ten Twelfths",
                             "Nine Twelfths",
                             "Eight Twelfths"),
                     page2.stream()
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));

        assertEquals(2L, page2.pageRequest().page());
        assertEquals(5L, page2.numberOfElements());
        assertEquals(true, page2.hasPrevious());
        assertEquals(true, page2.hasNext());

        CursoredPage<Fraction> page1 = fractions.namedLike("%fths",
                                                           descendingNumerator,
                                                           page2.previousPageRequest());

        assertEquals(List.of("Four Fifths",
                             "Three Fifths"),
                     page1.stream()
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));

        assertEquals(1L, page1.pageRequest().page());
        assertEquals(2L, page1.numberOfElements());
        assertEquals(false, page1.hasPrevious());
        assertEquals(true, page1.hasNext());

        CursoredPage<Fraction> page3 = fractions.namedLike("%fths",
                                                           descendingNumerator,
                                                           page2.nextPageRequest());

        assertEquals(List.of("Seven Twelfths",
                             "Six Twelfths",
                             "Five Twelfths",
                             "Four Twelfths",
                             "Three Twelfths"),
                     page3.stream()
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));

        assertEquals(3L, page3.pageRequest().page());
        assertEquals(5L, page3.numberOfElements());
        assertEquals(true, page3.hasPrevious());
        assertEquals(true, page3.hasNext());

        CursoredPage<Fraction> page4 = fractions.namedLike("%fths",
                                                           descendingNumerator,
                                                           page3.nextPageRequest());

        assertEquals(List.of("Two Twelfths"),
                     page4.stream()
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));

        assertEquals(4L, page4.pageRequest().page());
        assertEquals(1L, page4.numberOfElements());
        assertEquals(true, page4.hasPrevious());
        assertEquals(false, page4.hasNext());
    }

    /**
     * Tests that the Like constraint types can be assigned to a repository
     * method parameter to enforce that an entity attributes is matched
     * according to a literal value.
     */
    @Test
    public void testLikeConstraintWithLiteral() {

        assertEquals(List.of("Seven Eighths"),
                     fractions.named(Like.literal("Seven Eighths"),
                                     Order.by(),
                                     Limit.of(7)));

        assertEquals(List.of(),
                     fractions.named(Like.literal("Seven %"),
                                     Order.by(),
                                     Limit.of(8)));
    }

    /**
     * Tests that the Like constraint types can be assigned to a repository
     * method parameter to enforce that an entity attributes is matched
     * according to an escaped pattern.
     */
    @Test
    public void testLikeConstraintWithCustomEscapedPattern() {

        assertEquals(List.of("Five Sixteenths",
                             "Five Sixths",
                             "Four Sixteenths",
                             "Four Sixths"),
                     fractions.named(Like.pattern("Fccc Sixxsthxs",
                                                  'c', // char wildcard, normally _
                                                  's', // string wildcard, normally %
                                                  'x'), // escape char, normally \
                                     Order.by(Sort.asc(_Fraction.NAME)),
                                     Limit.of(6)));
    }

    /**
     * Tests that the Like constraint types can be assigned to a repository
     * method parameter to enforce that an entity attributes is matched
     * according to a simple pattern with wildcards. Also use a Limit parameter
     * to restrict the number of results returned.
     */
    @Test
    public void testLikeConstraintWithPatternAndLimit() {

        assertEquals(List.of("Three Fifths",
                             "Three Ninths",
                             "Three Sixths",
                             "Two Fifths",
                             "Two Ninths",
                             "Two Sixths"),
                     fractions.named(Like.pattern("T% _i_ths"),
                                     Order.by(Sort.asc(_Fraction.NAME)),
                                     Limit.of(10)));

        // reversing the order and limiting to 4 results:
        assertEquals(List.of("Two Sixths",
                             "Two Ninths",
                             "Two Fifths",
                             "Three Sixths"),
                     fractions.named(Like.pattern("T% _i_ths"),
                                     Order.by(Sort.desc(_Fraction.NAME)),
                                     Limit.of(4)));
    }

    /**
     * Tests that a Constraint parameter and Is annotation parameter can be
     * intermixed on a single repository method.
     */
    @Test
    public void testMixConstraintAndIsAnno() {
        Sort<Fraction> alphabetizedByName = Sort.asc(_Fraction.NAME);

        assertEquals(List.of("Eight Ninths",
                             "Five Ninths",
                             "Three Ninths"),
                     fractions.withNumeratorsAndDenominator(In.values(3, 5, 8, -12),
                                                            9,
                                                            alphabetizedByName));
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

    /**
     * Tests that the NotNull and AtMost constraint types can be assigned to
     * repository method parameters to enforce that matching entity attributes
     * are not null and less than or equal to a provided value.
     */
    @Test
    public void testNotNullAndAtMostConstraints() {

        assertEquals(List.of("One Fourth",
                             "Two Fourths",
                             "Three Fourths",
                             "One Third",
                             "Two Thirds",
                             "One Half"),
                     fractions.denominatoredUpTo(NotNull.instance(),
                                                 AtMost.max(4),
                                                 Sort.desc(_Fraction.DENOMINATOR),
                                                 Sort.asc(_Fraction.NUMERATOR)) //
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a repository method that performs a Query consisting of a SELECT
     * clause that uses the NEW keyword to specify the constructor for a
     * Java record.
     */
    @Test
    public void testSelectNewQuery() {

        assertEquals(new Ratio(5, 3),
                     fractions.singleRatio(5, 8).orElseThrow());
    }

    /**
     * Use a repository method that performs a Query with SELECT and ORDER BY
     * clauses only, retrieving a subset of entity attributes as a Java record.
     */
    @Test
    public void testSelectOrderByQueryReturnsPageOfRecords() {

        Page<Ratio> page1 = fractions.pageOfRatios(PageRequest.ofSize(12));

        assertEquals(List.of("1:19",
                             "1:18",
                             "2:18",
                             "1:17",
                             "2:17",
                             "3:17",
                             "1:16",
                             "2:16",
                             "3:16",
                             "4:16",
                             "1:15",
                             "2:15"),
                     page1.stream()
                                     .map(Ratio::toString)
                                     .collect(Collectors.toList()));

        Page<Ratio> page2 = fractions.pageOfRatios(page1.nextPageRequest());

        assertEquals(List.of("3:15",
                             "4:15",
                             "5:15",
                             "1:14",
                             "2:14",
                             "3:14",
                             "4:14",
                             "5:14",
                             "6:14",
                             "1:13",
                             "2:13",
                             "3:13"),
                     page2.stream()
                                     .map(Ratio::toString)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a repository method that performs a Query consisting of a SELECT
     * clause only, retrieving a subset of entity attributes as a Java record.
     */
    @Test
    public void testSelectQueryReturnsStreamOfRecords() {

        assertEquals(List.of("10:1",
                             "10:10",
                             "10:2",
                             "10:3",
                             "10:4",
                             "10:5",
                             "10:6",
                             "10:7",
                             "10:8",
                             "10:9",
                             "11:1",
                             "11:2",
                             "11:3",
                             "11:4",
                             "11:5"),
                     fractions.streamOfRatios()
                                     .map(Ratio::toString)
                                     .sorted()
                                     .limit(15)
                                     .collect(Collectors.toList()));
    }

}
