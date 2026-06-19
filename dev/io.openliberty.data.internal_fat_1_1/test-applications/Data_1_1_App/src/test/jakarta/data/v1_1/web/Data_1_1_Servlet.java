/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Resource;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.constraint.AtLeast;
import jakarta.data.constraint.AtMost;
import jakarta.data.constraint.Between;
import jakarta.data.constraint.In;
import jakarta.data.constraint.Like;
import jakarta.data.constraint.NotBetween;
import jakarta.data.constraint.NotNull;
import jakarta.data.exceptions.DataException;
import jakarta.data.expression.TextExpression;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Cursor;
import jakarta.data.restrict.Restrict;
import jakarta.data.restrict.Restriction;
import jakarta.data.spi.expression.literal.NumericLiteral;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;
import test.jakarta.data.v1_1.web.Fraction.Decimal;
import test.jakarta.data.v1_1.web.Fraction.Decimal.Type;

@SuppressWarnings("serial")
@WebServlet("/*")
public class Data_1_1_Servlet extends FATServlet {

    /**
     * Maximum number of seconds that a test waits for an asynchronous
     * operation to complete.
     */
    static final long TIMEOUT_S = TimeUnit.MINUTES.toSeconds(2);

    @Inject
    Advertisements ads;

    @Inject
    Fractions fractions;

    @Inject
    StatefulFractionRepository statefulFractionRepo;

    @Inject
    StatefulFractions statefulFractions;

    @Resource
    UserTransaction tx;

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
     * Indicates if testing with the Derby database.
     *
     * @return true if testing with the Derby database.
     */
    static final boolean isDerby() {
        String jdbcJarName = System.getenv().getOrDefault("DB_DRIVER", "UNKNOWN");
        return jdbcJarName.startsWith("derby");
    }

    /**
     * Indicates if testing with the Hibernate Persistence provider
     * rather than EclipseLink.
     *
     * @return true if testing with the Hibernate Persistence provider.
     */
    static final boolean isHibernatePersistence() {
        return Boolean.valueOf(System.getenv("TEST_HIBERNATE"));
    }

    /**
     * Tests that the Between and NotBetween constraint types can be assigned to
     * repository method parameters to enforce that matching entity attributes
     * are either within or not within a range of non-Literal expressions.
     */
    @Test
    public void testBetweenAndNotBetweenConstraintsWithExpressions() {
        Between<Integer> denominator2to10LessThanNumerator = //
                        Between.bounds(_Fraction.numerator.plus(2),
                                       _Fraction.numerator.plus(10));

        NotBetween<Integer> numeratorNot8to12LessThanDenominator = //
                        NotBetween.bounds(_Fraction.denominator.minus(12),
                                          _Fraction.denominator.minus(8));

        assertEquals(List.of("One Fifth",
                             "Four Sevenths",
                             "Five Sevenths",
                             "One Eighth",
                             "Five Eighths",
                             "Four Ninths",
                             "Five Ninths",
                             "Four Elevenths",
                             "Five Elevenths",
                             "Eight Elevenths",
                             "Nine Elevenths",
                             "Five Twelfths",
                             "Eight Thirteenths",
                             "Nine Thirteenths",
                             "Eleven Thirteenths",
                             "Nine Fourteenths",
                             "Eleven Fourteenths",
                             "Eight Fifteenths",
                             "Eleven Fifteenths",
                             "Nine Sixteenths",
                             "Eleven Sixteenths",
                             "Eleven Seventeenths",
                             "Fourteen Seventeenths",
                             "Fifteen Seventeenths",
                             "Eleven Eighteenths",
                             "Fourteen Nineteenths",
                             "Fifteen Nineteenths"),
                     fractions.withDenominatorBetweenNamedBeforeAndNumeratorNotBetween //
                     (denominator2to10LessThanNumerator,
                      "One Fourth",
                      numeratorNot8to12LessThanDenominator,
                      true) // must be reduced
                                     .map(f -> f.name)
                                     .toList());
    }

    /**
     * Tests that the Between and NotBetween constraint types can be assigned to
     * repository method parameters to enforce that matching entity attributes
     * are either within or not within a range of Literal values.
     */
    @Test
    public void testBetweenAndNotBetweenConstraintsWithLiterals() {

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
     * Tests that the Between and NotBetween constraint types can be assigned to
     * repository method parameters to enforce that matching entity attributes
     * are either within or not within a range of mostly Literals, but with one
     * non-Literal expression.
     */
    @Test
    public void testBetweenAndNotBetweenConstraintsWithOneExpression() {

        assertEquals(List.of("One Sixth",
                             "One Seventh",
                             "One Eighth",
                             "One Ninth",
                             "Two Ninths",
                             "Eight Ninths",
                             "One Tenth",
                             "Nine Tenths",
                             "One Eleventh",
                             "Two Elevenths",
                             "Ten Elevenths",
                             "One Twelfth",
                             "Eleven Twelfths"),
                     fractions.withDenominatorBetweenNamedBeforeAndNumeratorNotBetween //
                     (Between.bounds(6, 12),
                      "Two Sevenths",
                      NotBetween.bounds(3, _Fraction.name.length().minus(5)),
                      true) // must be reduced
                                     .map(f -> f.name)
                                     .toList());
    }

    /**
     * Supply restrictions to a repository method where one of the restrictions
     * is a restriction on a boolean attribute.
     */
    @Test
    public void testBooleanAttributeRestrictions() {

        assertEquals(List.of(11, 7, 5, 1),
                     fractions.where(Restrict.all(_Fraction.denominator.equalTo(12),
                                                  _Fraction.reduced.isTrue()))
                                     .map(f -> f.numerator)
                                     .toList());

        assertEquals(List.of(2, 3, 4),
                     fractions.withNameLike("%ixths",
                                            _Fraction.reduced.isFalse(),
                                            Order.by(_Fraction.numerator.asc()))
                                     .map(f -> f.numerator)
                                     .toList());
    }

    /**
     * Supply restrictions to a repository method where one of the restrictions
     * casts BigDecimal values to BigInteger in order to compare as whole numbers.
     */
    @Test
    public void testCastBigDecimalToBigInteger() {

        // EclipseLink does not have
        // CAST (value AS BIGINTEGER)
        if (!isHibernatePersistence())
            return;

        Restriction<Fraction> roundsUpTo2223 = _Fraction.decimal_ceiling
                        .times(BigDecimal.valueOf(10000L))
                        .asBigInteger()
                        .equalTo(BigInteger.valueOf(2223));

        assertEquals(List.of("2/9",
                             "4/18"),
                     fractions.where(roundsUpTo2223)
                                     .map(f -> f.numerator + "/" + f.denominator)
                                     .toList());
    }

    /**
     * Supply restrictions to a repository method where one of the restrictions
     * casts BigDecimal values to Long in order to compare as whole numbers.
     */
    @Test
    public void testCastBigDecimalToLong() {

        // EclipseLink does not have
        // CAST (value AS LONG)
        if (!isHibernatePersistence())
            return;

        Restriction<Fraction> roundsDownTo5555 = _Fraction.decimal_truncated
                        .times(BigDecimal.valueOf(10000L))
                        .asLong()
                        .equalTo(5555L);

        assertEquals(List.of("5/9",
                             "10/18"),
                     fractions.where(roundsDownTo5555)
                                     .map(f -> f.numerator + "/" + f.denominator)
                                     .toList());
    }

    /**
     * Supply restrictions to a repository method where one of the restrictions
     * casts double values to BigDecimal to compare with another BigDecimal value.
     */
    @Test
    public void testCastDoubleToBigDecimal() {
        // EclipseLink does not have
        // CAST (value AS BIGDECIMAL)
        if (!isHibernatePersistence())
            return;

        Restriction<Fraction> value_x_4_is_1pt5 = _Fraction.decimal_value
                        .times(4.0)
                        .asBigDecimal()
                        .equalTo(BigDecimal.valueOf(15, 1));

        assertEquals(List.of("3/8",
                             "6/16"),
                     fractions.where(value_x_4_is_1pt5)
                                     .map(f -> f.numerator + "/" + f.denominator)
                                     .toList());

    }

    /**
     * Supply restrictions to a repository method where one of the restrictions
     * casts integer values to double in order to perform division that results
     * in a decimal value.
     */
    @Test
    public void testCastIntegerToDouble() {

        Restriction<Fraction> within22to34Hundreths = _Fraction.numerator
                        .asDouble()
                        .dividedBy(_Fraction.denominator.asDouble())
                        .between(0.22, 0.34);

        assertEquals(List.of("1/3",
                             "1/4",
                             "2/6",
                             "2/7",
                             "2/8",
                             "3/9", "2/9",
                             "3/10",
                             "3/11",
                             "4/12", "3/12",
                             "4/13", "3/13",
                             "4/14",
                             "5/15", "4/15",
                             "5/16", "4/16",
                             "5/17", "4/17",
                             "6/18", "5/18", "4/18",
                             "6/19", "5/19",
                             "6/20", "5/20"),
                     fractions.where(within22to34Hundreths)
                                     .map(f -> f.numerator + "/" + f.denominator)
                                     .toList());
    }

    /**
     * Use a repository method that has the Is annotation on one method
     * argument and another method argument is a composite All restriction.
     */
    @Test
    public void testCompositeAllRestrictionAndIsAnno() {

        assertEquals(List.of("Three Twelfths",
                             "Five Twelfths",
                             "Six Twelfths",
                             "Ten Twelfths"),
                     fractions.withNameLike("% Twelfths",
                                            Restrict.all(_Fraction.numerator.greaterThan(2),
                                                         _Fraction.name.notBetween("Four",
                                                                                   "Several"),
                                                         _Fraction.name.notStartsWith("E")),
                                            Order.by(_Fraction.numerator.asc())) //
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a repository method that has the Is annotation on one method
     * argument and another method argument is a composite Any restriction.
     */
    @Test
    public void testCompositeAnyRestrictionAndIsAnno() {

        assertEquals(List.of("Five Elevenths",
                             "Five Sevenths",
                             "Nine Elevenths",
                             "Ten Elevenths",
                             "Two Elevenths",
                             "Two Sevenths"),
                     fractions.withNameLike("%evenths",
                                            Restrict.any(_Fraction.name.like("T__ %"),
                                                         _Fraction.name.like("_i_e %")),
                                            Order.by(_Fraction.name.asc())) //
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a repository method that has the Is annotation on one method
     * argument and another method argument is a composite restriction
     * that has another composite restriction within it.
     */
    @Test
    public void testCompositeRestrictionsDepth2() {

        Restriction<Fraction> restriction = //
                        Restrict.all(_Fraction.numerator.notEqualTo(6),
                                     Restrict.any(_Fraction.numerator.lessThan(4),
                                                  _Fraction.numerator.greaterThanEqual(5)));

        assertEquals(List.of("Eight Ninths",
                             "Seven Ninths",
                             "Five Ninths",
                             "Three Ninths",
                             "Two Ninths"),
                     fractions.withNameLike("% Ninths",
                                            restriction,
                                            Order.by(_Fraction.numerator.desc())) //
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Supply a concatenation expression to a repository method.
     */
    @Test
    public void testConcatExpression() {

        assertEquals(List.of("One Seventh",
                             "One Ninth",
                             "One Tenth",
                             "One Eleventh",
                             "One Thirteenth",
                             "One Fourteenth",
                             "One Fifteenth",
                             "One Sixteenth",
                             "One Seventeenth",
                             "One Eighteenth",
                             "One Nineteenth"),
                     fractions.withNameLike("___ %",
                                            _Fraction.name.append("s").endsWith("nths"),
                                            Order.by(_Fraction.denominator.asc()))
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a stateful repository to find an entity. Use a detach operation to
     * make the entity unmanaged. Make updates to the entity. After committing,
     * verify that updates made after the detach operation are not written to
     * the database.
     */
    @Test
    public void testDetach() throws Exception {

        boolean removed = false;
        try {
            // Populate with 5/23.
            // Ensure deletion in the finally block.
            tx.begin();
            statefulFractionRepo.write(Fraction.of(5, 23));
            tx.commit();

            System.out.println("Fetch 5/23 to detach, modify, and commit");

            tx.begin();
            Fraction f = statefulFractions.fetch(5, 23).orElseThrow();
            statefulFractions.detach(f);
            f.reduced = false;
            f.decimal = Decimal.of(4, 23);
            tx.commit();

            f = statefulFractions.fetch(5, 23).orElseThrow();

            // modifications from after detach should not be persisted
            assertEquals(true,
                         f.reduced);

            assertEquals(BigDecimal.valueOf(2173, 4), // first 4 decimals of 5/23
                         f.decimal.truncated());

            statefulFractionRepo.remove(f);
            removed = true;
        } finally {
            if (tx.getStatus() != Status.STATUS_NO_TRANSACTION)
                tx.rollback();

            // Ensure no fractions with denominator of 23 or more are left around
            if (!removed)
                fractions.discard(AtLeast.min(23),
                                  AtMost.max(Integer.MAX_VALUE),
                                  Restrict.unrestricted());
        }
    }

    /**
     * Use the QueryOptions annotation on a Find method to supply a load graph
     * that overrides loading of an ElementCollection to make it eager rather
     * than lazily loaded.
     */
    @Test
    public void testEntityGraphAsQueryOption() {
        assertEquals(List.of(BigDecimal.valueOf(300, 3), // nearest tenth
                             BigDecimal.valueOf(310, 3), // nearest hundreth
                             BigDecimal.valueOf(313, 3)), // nearest thousandth
                     fractions.of(5, 16).orElseThrow().rounded);

        assertEquals(List.of(BigDecimal.valueOf(900, 3), // nearest tenth
                             BigDecimal.valueOf(860, 3), // nearest hundreth
                             BigDecimal.valueOf(857, 3)), // nearest thousandth
                     fractions.of(6, 7).orElseThrow().rounded);

        assertEquals(List.of(BigDecimal.valueOf(600, 3), // nearest tenth
                             BigDecimal.valueOf(620, 3), // nearest hundreth
                             BigDecimal.valueOf(615, 3)), // nearest thousandth
                     fractions.of(8, 13).orElseThrow().rounded);
    }

    /**
     * Tests a Find method with the First annotation specifying a value
     * larger than 1.
     */
    @Test
    public void testFindFirst10() {

        Restriction<Fraction> notReduced = _Fraction.reduced.isFalse();

        Order<Fraction> alphabetized = Order.by(_Fraction.name.ascIgnoreCase());

        assertEquals(List.of(8, // Eight
                             18, // Eighteen
                             15, // Fifteen
                             5, // Five
                             4, // Four
                             14, // Fourteen
                             6, // Six
                             16, // Sixteen
                             10, // Ten
                             12), // Twelve
                     fractions.atMost10Numerators(20,
                                                  notReduced,
                                                  alphabetized));
    }

    /**
     * Tests a Find method with the First annotation specifying a value
     * but where there are less results available than requested.
     */
    @Test
    public void testFindFirstReturnsLesserAmount() {

        Restriction<Fraction> firstLetterIsF = _Fraction.name.startsWith("F");

        Order<Fraction> reverseAlphabetized = Order.by(_Fraction.name.desc());

        assertEquals(List.of(14, // Fourteen
                             4, // Four
                             5, // Five
                             15), // Fifteen
                     fractions.atMost10Numerators(18,
                                                  firstLetterIsF,
                                                  reverseAlphabetized));
    }

    @Test
    public void testInheritanceFromAbstractEntity() {
        ads.removeBySponsorIn(List.of("Open Liberty",
                                      "Eclipse Foundation",
                                      "IBM"));

        LocalDate today = LocalDate.now();

        Advertisement ad1 = ads
                        .add(Billboard.create(1,
                                              "IBM",
                                              today.plusDays(4),
                                              today.plusDays(8),
                                              "3605 Highway 52 North, Rochester, MN 55901"));
        assertEquals(ad1.toString(),
                     true,
                     ad1 instanceof Billboard);

        Advertisement ad2 = ads
                        .add(Billboard.create(2,
                                              "Open Liberty",
                                              today.plusDays(2),
                                              today.plusDays(3),
                                              "3605 Highway 52 North, Rochester, MN 55901"));
        assertEquals(ad2.toString(),
                     true,
                     ad2 instanceof Billboard);

        Advertisement ad3 = ads
                        .add(Billboard.create(3,
                                              "Open Liberty",
                                              today.plusDays(9),
                                              today.plusDays(12),
                                              // test case later switches this to
                                              // 111 Broadway Ave S, Rochester, MN 55904
                                              "3605 Highway 52 North, Rochester, MN 55901"));
        assertEquals(ad3.toString(),
                     true,
                     ad3 instanceof Billboard);

        Advertisement ad4 = ads
                        .add(Commercial.create(4,
                                               "Eclipse Foundation",
                                               "NBC",
                                               40,
                                               LocalDateTime.of(today,
                                                                LocalTime.of(16, 44, 4))));
        assertEquals(ad4.toString(),
                     true,
                     ad4 instanceof Commercial);

        Advertisement ad5 = ads
                        .add(Billboard.create(5,
                                              "Eclipse Foundation",
                                              today.plusDays(15),
                                              today.plusDays(25),
                                              "3605 Highway 52 North, Rochester, MN 55901"));
        assertEquals(ad5.toString(),
                     true,
                     ad5 instanceof Billboard);

        Advertisement ad6 = ads
                        .add(Commercial.create(6,
                                               "Open Liberty",
                                               "ABC",
                                               60,
                                               // test case later switches to
                                               // today @19:20:21
                                               LocalDateTime.of(today,
                                                                LocalTime.of(18, 6, 0))));
        assertEquals(ad6.toString(),
                     true,
                     ad6 instanceof Commercial);

        // @Find method

        List<Advertisement> list = ads.sponsoredBy("Eclipse Foundation");
        assertEquals(list.toString(),
                     2,
                     list.size());

        Advertisement ad;
        Commercial c;
        Billboard b;

        assertNotNull(ad = list.get(0));
        assertEquals(4,
                     ad.id);
        assertEquals("Eclipse Foundation",
                     ad.sponsor);
        assertEquals(ad.toString(),
                     true,
                     ad instanceof Commercial);
        c = (Commercial) ad;
        assertEquals("NBC",
                     c.network);
        assertEquals(40,
                     c.numSeconds);
        assertEquals(LocalDateTime.of(today,
                                      LocalTime.of(16, 44, 4)),
                     c.showAt);

        assertNotNull(ad = list.get(1));
        assertEquals(5,
                     ad.id);
        assertEquals("Eclipse Foundation",
                     ad.sponsor);
        assertEquals(ad.toString(),
                     true,
                     ad instanceof Billboard);
        b = (Billboard) ad;
        assertEquals(today.plusDays(15),
                     b.firstDay);
        assertEquals(today.plusDays(25),
                     b.lastDay);
        assertEquals("3605 Highway 52 North, Rochester, MN 55901",
                     b.location);

        // @Update method

        Billboard b3 = (Billboard) ad3;
        b3.location = "111 Broadway Ave S, Rochester, MN 55904";
        ads.redeploy(b3);

        // UPDATE query

        assertEquals(true,
                     ads.changeTimeShown(6,
                                         LocalDateTime.of(today,
                                                          LocalTime.of(19, 20, 21))));

        // @Find again to confirm updates

        list = ads.sponsoredBy("Open Liberty");
        assertEquals(list.toString(),
                     3,
                     list.size());

        assertNotNull(ad = list.get(0));
        assertEquals(2,
                     ad.id);
        assertEquals("Open Liberty",
                     ad.sponsor);
        assertEquals(ad.toString(),
                     true,
                     ad instanceof Billboard);
        b = (Billboard) ad;
        assertEquals(today.plusDays(2),
                     b.firstDay);
        assertEquals(today.plusDays(3),
                     b.lastDay);
        assertEquals("3605 Highway 52 North, Rochester, MN 55901",
                     b.location);

        assertNotNull(ad = list.get(1));
        assertEquals(3,
                     ad.id);
        assertEquals("Open Liberty",
                     ad.sponsor);
        assertEquals(ad.toString(),
                     true,
                     ad instanceof Billboard);
        b = (Billboard) ad;
        assertEquals(today.plusDays(9),
                     b.firstDay);
        assertEquals(today.plusDays(12),
                     b.lastDay);
        assertEquals("111 Broadway Ave S, Rochester, MN 55904", // changed
                     b.location);

        assertNotNull(ad = list.get(2));
        assertEquals(6,
                     ad.id);
        assertEquals("Open Liberty",
                     ad.sponsor);
        assertEquals(ad.toString(),
                     true,
                     ad instanceof Commercial);
        c = (Commercial) ad;
        assertEquals("ABC",
                     c.network);
        assertEquals(60,
                     c.numSeconds);
        assertEquals(LocalDateTime.of(today,
                                      LocalTime.of(19, 20, 21)), // changed
                     c.showAt);

        // delete method:

        assertEquals(6L,
                     ads.removeBySponsorIn(List.of("Open Liberty",
                                                   "Eclipse Foundation",
                                                   "IBM")));
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
        Order<Fraction> order = Order.by(Sort.desc(_Fraction.DECIMAL_VALUE));

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
     * Use a repository method that is annotated JakartaQuery that has
     * both Restriction and Order parameters.
     */
    @Test
    public void testJakartaQueryWithRestrictionAndOrder() {

        Restriction<Fraction> ninthsAndTenths = //
                        Restrict.any(_Fraction.denominator.equalTo(9),
                                     _Fraction.denominator.equalTo(10));

        assertEquals(List.of("One Ninth",
                             "Two Ninths",
                             "Two Tenths",
                             "Three Ninths",
                             "Three Tenths",
                             "Four Ninths",
                             "Four Tenths"),
                     fractions.squareRootBetween(0.33,
                                                 0.67,
                                                 ninthsAndTenths,
                                                 Order.by(_Fraction.numerator.asc(),
                                                          _Fraction.denominator.asc()))
                                     .stream()
                                     .map(f -> f.name)
                                     .toList());
    }

    /**
     * Supply LEFT and RIGHT expressions to a repository method.
     */
    @Test
    public void testLeftAndRightExpressions() {

        Restriction<Fraction> restriction = //
                        Restrict.all(_Fraction.name.left(4).right(1).equalTo(" "),
                                     Restrict.any(_Fraction.name.right(3).equalTo("fth"),
                                                  _Fraction.name.right(4).equalTo("fths")));

        assertEquals(List.of("Ten Twelfths",
                             "Six Twelfths",
                             "Two Twelfths",
                             "One Twelfth",
                             "Two Fifths",
                             "One Fifth"),
                     fractions.withNameLike("%",
                                            restriction,
                                            Order.by(_Fraction.denominator.desc(),
                                                     _Fraction.numerator.desc()))
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Supply a LENGTH expression to a repository method.
     */
    @Test
    public void testLengthExpression() {

        assertEquals(List.of("One Tenth",
                             "One Ninth",
                             "One Sixth",
                             "One Fifth",
                             "One Third",
                             "One Half"),
                     fractions.withNameLike("%",
                                            _Fraction.name.length().lessThan(10),
                                            Order.by(_Fraction.denominator.desc()))
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Tests that the Like constraint type can be assigned to a repository
     * method parameter to enforce that an entity attributes is matched
     * according to a computed expression.
     */
    @Test
    public void testLikeConstraintWithExpression() {

        Like first3CharsAlsoAppearLaterInName = //
                        Like.pattern(_Fraction.name
                                        .left(3)
                                        .append("%")
                                        .prepend("___%"),
                                     '^');

        assertEquals(List.of("Eight Eighteenths",
                             "Four Fourteenths",
                             "Nine Nineteenths",
                             "Seven Seventeenths",
                             "Six Sixteenths",
                             "Twelve Twentieths"),
                     fractions.named(first3CharsAlsoAppearLaterInName,
                                     Order.by(_Fraction.name.asc()),
                                     Limit.of(10)));

    }

    /**
     * Tests that the Like constraint type can be assigned to a repository
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
     * Use the QueryOptions annotation to establish pessimistic locking and
     * a query timeout on a repository method annotated JakartaQuery.
     */
    @AllowedFFDC("javax.transaction.xa.XAException") // due to query timeout
    @Test
    public void testLockModeAndQueryTimeoutAsQueryOptions() throws Exception {
        // Populate with 18/23.
        // Ensure deletion in the finally block.
        fractions.supply(List.of(Fraction.of(18, 23)));

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch blocked = new CountDownLatch(1);
        CompletableFuture<Fraction> lockDB = CompletableFuture.supplyAsync(() -> {
            try {
                tx.setTransactionTimeout((int) TIMEOUT_S * 3);
                tx.begin();
                Optional<Fraction> found = //
                                fractions.withWriteLock("Eighteen Twenty-thirds");
                System.out.println("Obtained lock on 18/23");
                locked.countDown();
                blocked.await(TIMEOUT_S * 2, TimeUnit.SECONDS);
                System.out.println("Release lock on 18/23");
                tx.commit();
                return found.orElseThrow();
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new CompletionException(x);
            }
        });
        try {
            assertEquals(true, locked.await(TIMEOUT_S, TimeUnit.SECONDS));
            // Derby ignores query timeout and the lock timeout ends up applying
            // instead. Work around this by also setting the lock timeout when
            // running on Derby:
            if (isDerby())
                if (isHibernatePersistence())
                    statefulFractions.setLockTimeout(5);
                else
                    fractions.setLockTimeout(5);
            tx.begin();
            try {
                Optional<Fraction> found = //
                                fractions.withWriteLock("Eighteen Twenty-thirds");
                fail("Query timeout on QueryOptions should have caused the" +
                     " query to time out. Instead found " + found);
            } catch (DataException x) {
                // expected for query timeout
            } finally {
                tx.rollback();
            }

            blocked.countDown();
            tx.begin();
            Fraction f18_23 = fractions.withWriteLock("Eighteen Twenty-thirds")
                            .orElseThrow();
            assertEquals(0.7826,
                         f18_23.decimal.value(),
                         0.0001);
            tx.commit();

            // allow error to be raised if any
            lockDB.get(TIMEOUT_S, TimeUnit.SECONDS);
        } finally {
            locked.countDown();
            blocked.countDown();
            // Ensure no fractions with denominator of 23 or more are left around
            fractions.discard(AtLeast.min(23),
                              AtMost.max(Integer.MAX_VALUE),
                              Restrict.unrestricted());
            if (isDerby())
                if (isHibernatePersistence())
                    statefulFractions.setLockTimeout(60);
                else
                    fractions.setLockTimeout(60);
        }
    }

    /**
     * Supply a LOWER expression to a repository method.
     */
    @Test
    public void testLowerExpression() {

        assertEquals(List.of("Two Eighteenths",
                             "Two Elevenths",
                             "Two Fifteenths",
                             "Two Fourteenths",
                             "Two Nineteenths",
                             "Two Seventeenths",
                             "Two Sevenths",
                             "Two Sixteenths",
                             "Two Tenths",
                             "Two Thirteenths"),
                     fractions.withNameLike("%enths",
                                            _Fraction.name.lower().startsWith("two "),
                                            Order.by(_Fraction.name.asc()))
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a merge operation on a stateful repository to make an unmanaged
     * entity into a managed entity.
     */
    @Test
    public void testMerge() throws Exception {

        // Populate with 15/23.
        // Ensure deletion in the finally block.
        statefulFractionRepo.write(Fraction.of(15, 23));

        boolean removed = false;
        try {
            System.out.println("Update an unmanaged instance and merge");
            Fraction f = Fraction.of(15, 23);
            f.denominator = 21;
            f.reduced = false;
            f = statefulFractionRepo.manage(f);

            assertEquals(21,
                         f.denominator);
            assertEquals(false,
                         f.reduced);
            assertEquals("Fifteen Twenty-thirds",
                         f.name);
            assertEquals(BigDecimal.valueOf(6521, 4), // first 4 decimals of 15/23
                         f.decimal.truncated());

            f.decimal = Decimal.of(15, 21);

            if (isHibernatePersistence()) {
                statefulFractions.manager().flush();
            } else {
                tx.begin();
                statefulFractions.manager().flush();
                tx.commit();
            }

            assertEquals(true,
                         statefulFractions.fetch(15, 23).isEmpty());

            tx.begin();
            f = statefulFractions.fetch(15, 21).orElseThrow();
            statefulFractions.detach(f);
            f.reduced = true;
            f = statefulFractionRepo.manage(f);
            f.denominator = 23;
            tx.commit();

            f = statefulFractions.fetch(15, 23).orElseThrow();

            assertEquals(23,
                         f.denominator);
            assertEquals(true,
                         f.reduced);
            assertEquals("Fifteen Twenty-thirds",
                         f.name);
            assertEquals(BigDecimal.valueOf(7142, 4), // first 4 decimals of 15/21
                         f.decimal.truncated());
            assertEquals(true,
                         statefulFractions.fetch(15, 21).isEmpty());

            statefulFractionRepo.remove(f);
            removed = true;
        } finally {
            if (tx.getStatus() != Status.STATUS_NO_TRANSACTION)
                tx.rollback();

            // Ensure no fractions with denominator of 21 or more are left around
            if (!removed)
                fractions.discard(AtLeast.min(21),
                                  AtMost.max(Integer.MAX_VALUE),
                                  Restrict.unrestricted());
        }
    }

    /**
     * Use a multiple merge operation on a stateful repository to make multiple
     * unmanaged enities into managed entities.
     */
    @Test
    public void testMergeMultiple() throws Exception {

        // Populate with 16/23 and 17/23.
        // Ensure deletion in the finally block.
        statefulFractionRepo.persistAll(List.of(Fraction.of(16, 23),
                                                Fraction.of(17, 23)));

        boolean removed = false;
        try {
            // unmanaged due to detach:
            Fraction f16 = statefulFractions.fetch(16, 23).orElseThrow();
            statefulFractions.detach(f16);
            f16.reduced = false;

            // unmanaged due to creating instance:
            Fraction f17 = Fraction.of(17, 23);
            f17.reduced = false;

            Fraction[] managed = statefulFractionRepo.multiMerge(f16, f17);

            assertEquals(16,
                         managed[0].numerator);
            assertEquals(23,
                         managed[0].denominator);
            assertEquals(false,
                         managed[0].reduced);
            assertEquals("Sixteen Twenty-thirds",
                         managed[0].name);
            assertEquals(BigDecimal.valueOf(6956, 4), // first 4 decimals of 16/23
                         managed[0].decimal.truncated());

            assertEquals(17,
                         managed[1].numerator);
            assertEquals(23,
                         managed[1].denominator);
            assertEquals(false,
                         managed[1].reduced);
            assertEquals("Seventeen Twenty-thirds",
                         managed[1].name);
            assertEquals(BigDecimal.valueOf(7391, 4), // first 4 decimals of 17/23
                         managed[1].decimal.truncated());

            if (isHibernatePersistence()) {
                statefulFractions.manager().flush();
            } else {
                tx.begin();
                statefulFractions.manager().flush();
                tx.commit();
            }

            f16 = managed[0];
            f17 = managed[1];
            statefulFractions.detach(f16);

            tx.begin();
            statefulFractions.detach(f17);

            f16.decimal = Decimal.of(16, 22);
            f17.reduced = true;

            managed = statefulFractionRepo.multiMerge(f16, f17);

            f17 = managed[1];
            f17.decimal = Decimal.of(17, 22);
            tx.commit();

            f16 = statefulFractions.fetch(16, 23).orElseThrow();

            assertEquals(16,
                         f16.numerator);
            assertEquals(23,
                         f16.denominator);
            assertEquals(false,
                         f16.reduced);
            assertEquals("Sixteen Twenty-thirds",
                         f16.name);
            assertEquals(BigDecimal.valueOf(7272, 4), // first 4 decimals of 16/22
                         f16.decimal.truncated());

            f17 = statefulFractions.fetch(17, 23).orElseThrow();

            assertEquals(17,
                         f17.numerator);
            assertEquals(23,
                         f17.denominator);
            assertEquals(true,
                         f17.reduced);
            assertEquals("Seventeen Twenty-thirds",
                         f17.name);
            assertEquals(BigDecimal.valueOf(7727, 4), // first 4 decimals of 17/22
                         f17.decimal.truncated());

            statefulFractionRepo.remove(f16, f17);
            removed = true;
        } finally {
            if (tx.getStatus() != Status.STATUS_NO_TRANSACTION)
                tx.rollback();

            // Ensure no fractions with denominator of 23 or more are left around
            if (!removed)
                fractions.discard(AtLeast.min(23),
                                  AtMost.max(Integer.MAX_VALUE),
                                  Restrict.unrestricted());
        }
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
     * Supply minus and times expressions to a restriction that is
     * supplied to a repository method.
     */
    @Test
    public void testMinusAndTimes() {

        Restriction<Fraction> restriction = //
                        _Fraction.numerator.times(3)
                                        .equalTo(_Fraction.denominator.minus(1));

        assertEquals(List.of("One Fourth",
                             "Two Sevenths",
                             "Three Tenths",
                             "Four Thirteenths",
                             "Five Sixteenths",
                             "Six Nineteenths"),
                     fractions.where(restriction)
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a repository method that has the Is annotation on one method
     * argument and another method argument is a Between restriction.
     */
    @Test
    public void testMixBetweenRestrictionAndIsAnno() {

        assertEquals(List.of("Three Tenths",
                             "Four Tenths",
                             "Five Tenths",
                             "Six Tenths"),
                     fractions.withNameLike("% Tenths",
                                            _Fraction.numerator.between(3, 6),
                                            Order.by(_Fraction.numerator.asc())) //
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Tests that a Constraint parameter and Is annotation parameter can be
     * intermixed on a single repository method. Both Literal and non-Literal
     * expressions are supplied to the Constraint.
     */
    @Test
    public void testMixConstraintAndIsAnno() {
        In<Integer> numeratorConstraint = In
                        .expressions(NumericLiteral.of(1),
                                     NumericLiteral.of(2),
                                     _Fraction.denominator.minus(_Fraction.numerator).times(3));

        Sort<Fraction> alphabetizedByName = Sort.asc(_Fraction.NAME);

        assertEquals(List.of("One Eighth",
                             "Six Eighths", // (8 - 6) * 3 = 6
                             "Two Eighths"),
                     fractions.withNumeratorsAndDenominator(numeratorConstraint,
                                                            8,
                                                            alphabetizedByName));
    }

    /**
     * Tests that a Constraint parameter and Is annotation parameter can be
     * intermixed on a single repository method. Only non-Literal expressions
     * are supplied to the Constraint.
     */
    @Test
    public void testMixExpressionConstraintAndIsAnno() {
        In<Integer> numeratorConstraint = In
                        .expressions(_Fraction.name.length().minus(_Fraction.numerator),
                                     _Fraction.denominator.minus(1),
                                     _Fraction.denominator.minus(_Fraction.numerator.times(2)));

        Sort<Fraction> reverseAlphabetizedByName = Sort.desc(_Fraction.NAME);

        assertEquals(List.of(// length(name) - numerator     = 12 - 6      = 6
                             "Six Twelfths",

                             // length(name) - numerator     = 14 - 7      = 7
                             "Seven Twelfths",

                             // denominator - numerator * 2  = 12 - 4 * 2  = 4
                             "Four Twelfths",

                             // denominator - 1              = 12 - 1      = 11
                             "Eleven Twelfths"),
                     fractions.withNumeratorsAndDenominator(numeratorConstraint,
                                                            12,
                                                            reverseAlphabetizedByName));
    }

    /**
     * Tests that a Constraint parameter and Is annotation parameter can be
     * intermixed on a single repository method. Only Literal expressions
     * are supplied to the Constraint.
     */
    @Test
    public void testMixLiteralConstraintAndIsAnno() {
        Sort<Fraction> alphabetizedByName = Sort.asc(_Fraction.NAME);

        assertEquals(List.of("Eight Ninths",
                             "Five Ninths",
                             "Three Ninths"),
                     fractions.withNumeratorsAndDenominator(In.values(3, 5, 8, -12),
                                                            9,
                                                            alphabetizedByName));
    }

    /**
     * Use a repository method that has the Is annotation on one method
     * argument and another method argument is a lessThanEqual restriction.
     */
    @Test
    public void testMixLTERestrictionAndIsAnno() {

        assertEquals(List.of("Five Eighths",
                             "Four Eighths",
                             "Three Eighths",
                             "Two Eighths"),
                     fractions.withNameLike("% Eighths",
                                            _Fraction.numerator.lessThanEqual(5),
                                            Order.by(_Fraction.numerator.desc())) //
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a repository method that has the Is annotation on one method
     * argument and another method argument is a notLike restriction.
     */
    @Test
    public void testMixNotLikeRestrictionAndIsAnno() {

        assertEquals(List.of("Five Sevenths",
                             "Four Sevenths",
                             "Three Sevenths"),
                     fractions.withNameLike("% Sevenths",
                                            _Fraction.name.notLike("--- *", '-', '*', '!'),
                                            Order.by(_Fraction.numerator.desc())) //
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a NativeQuery method that performs SQL INSERT, UPDATE, and DELETE
     * statements.
     */
    @Test
    public void testNativeQueryExecutesStatements() {
        // Populate with 14/23.
        // Ensure deletion in the finally block.
        fractions.create(14,
                         23,
                         "Fourteen Twenty-Thirds",
                         true,
                         14.0 / 23.0,
                         23.0 / 14.0,
                         BigDecimal.valueOf(6090L, 4),
                         BigDecimal.valueOf(6080L, 4),
                         Type.REPEATING.ordinal(),
                         "",
                         "60869565");
        try {
            assertEquals(BigDecimal.valueOf(6090L, 4),
                         fractions.roundedUp(14, 23)
                                         .orElseThrow());

            System.out.println("Update 14/23");

            assertEquals(true, fractions.change(BigDecimal.valueOf(6087L, 4),
                                                BigDecimal.valueOf(6086L, 4),
                                                14,
                                                23));

            assertEquals(BigDecimal.valueOf(6087L, 4),
                         fractions.roundedUp(14, 23)
                                         .orElseThrow());
        } finally {
            // Ensure no fractions with denominator of 23 or more are left around
            assertEquals(1L,
                         fractions.destroy(14, "Twenty-Thirds"));
        }
    }

    /**
     * Use a NativeQuery method that returns subsets of entity attributes
     * as an array of Java records
     */
    // @Test // TODO Java record results?
    public void testNativeQueryReturnsArrayOfRecord() {
        assertEquals(List.of("1:8",
                             "2:7",
                             "3:6",
                             "4:5",
                             "5:4",
                             "6:3",
                             "7:2",
                             "8:1"),
                     Stream.of(fractions.ratioArrayWithDenominator(9))
                                     .map(Ratio::toString)
                                     .toList());
    }

    /**
     * Use a NativeQuery method that selects the first matching entity.
     */
    @Test
    public void testNativeQueryReturnsFirstEntity() {
        assertEquals("Seven Twentieths",
                     fractions.firstValueWithin(0.334, 0.4)
                                     .orElseThrow().name);
    }

    /**
     * Use a NativeQuery method that selects multiple entities as a list.
     * Also covers a NativeQuery that has no parameters.
     */
    @Test
    public void testNativeQueryReturnsListOfEntities() {
        assertEquals(List.of("1/2",
                             "1/3",
                             "1/4", "2/4",
                             "1/5", "2/5",
                             "1/6", "2/6",
                             "1/7", "2/7",
                             "1/8", "2/8",
                             "1/9", "2/9", "3/9"),
                     fractions.numeratorLTESquareRootOfDenominator(Limit.of(15))
                                     .stream()
                                     .map(f -> f.numerator + "/" + f.denominator)
                                     .toList());
    }

    /**
     * Use a NativeQuery method that returns subsets of entity attributes
     * as a List of Java records
     */
    // @Test // TODO Java record results?
    public void testNativeQueryReturnsListOfRecord() {
        assertEquals(List.of("1:11",
                             "2:12",
                             "3:13",
                             "4:14",
                             "5:15",
                             "6:16",
                             "7:17",
                             "8:18",
                             "9:19"),
                     fractions.ratioListWithDifferenceOfTerms(10)
                                     .stream()
                                     .map(Ratio::toString)
                                     .toList());
    }

    /**
     * Use a NativeQuery method that returns subsets of entity attributes
     * as a Stream of Java records
     */
    // @Test // TODO Java record results?
    public void testNativeQueryReturnsStreamOfRecord() {
        assertEquals(List.of("1:7",
                             "2:6",
                             "3:5",
                             "4:4",
                             "5:3",
                             "6:2",
                             "7:1"),
                     fractions.ratioStreamWithSumOfTerms(8)
                                     .map(Ratio::toString)
                                     .toList());
    }

    /**
     * Use a NativeQuery method that selects the result of a count operation
     * as a single value.
     */
    @Test
    public void testNativeQuerySelectsCount() {
        assertEquals(6L, // 1/18, 5/18, 7/18, 11/18, 13/18, 17/18
                     fractions.numReducedWithDenominatorOf(18, true));
    }

    /**
     * Use a NativeQuery method that executes a SQL query with named parameters,
     * where the named parameter names are implied from the method parameter names.
     */
    @Test
    public void testNativeQueryWithImplicitNamedParameters() {
        // Relies on optional capability where Hibernate provides named parameters
        // for SQL queries
        if (isHibernatePersistence()) {
            Fraction fiveTwelfths = fractions.ifReduced(true, 5, 12)
                            .orElseThrow();
            assertEquals(5,
                         fiveTwelfths.numerator);
            assertEquals(12,
                         fiveTwelfths.denominator);
            assertEquals("Five Twelfths",
                         fiveTwelfths.name);
            assertEquals(true,
                         fiveTwelfths.reduced);
            // TODO would require Hibernate support for
            // @QueryOptions(entityGraph = "EagerlyLoadRoundedValues")
            // on native queries
            //assertEquals(List.of(BigDecimal.valueOf(400, 3),
            //                     BigDecimal.valueOf(420, 3),
            //                     BigDecimal.valueOf(417, 3)),
            //             fiveTwelfths.rounded);
            assertEquals(BigDecimal.valueOf(4167, 4),
                         fiveTwelfths.decimal.ceiling());
            assertEquals(BigDecimal.valueOf(4166, 4),
                         fiveTwelfths.decimal.truncated());
            assertEquals(5.0 / 12.0,
                         fiveTwelfths.decimal.value(),
                         0.0001);
            assertEquals(2.4,
                         fiveTwelfths.decimal.inverse(),
                         0.0001);
            assertEquals("41",
                         fiveTwelfths.decimal.digits().nonrepeating());
            assertEquals("6",
                         fiveTwelfths.decimal.digits().repeating());
        }
    }

    /**
     * Use a NativeQuery method that executes a SQL query with some method
     * parameters correponding to named parameters and another being a special
     * parameter of type Limit.
     */
    @Test
    public void testNativeQueryWithNamedAndSpecialParameters() {
        // Relies on optional capability where Hibernate provides named parameters
        // for SQL queries
        if (isHibernatePersistence())
            assertEquals(List.of("Eight Elevenths",
                                 "Eight Fifteenths",
                                 "Eight Nineteenths",
                                 "Eight Ninths",
                                 "Eight Seventeenths",
                                 "Eight Thirteenths",
                                 "Eighteen Nineteenths",
                                 "Eleven Eighteenths",
                                 "Eleven Fifteenths"),
                         fractions.alphabetized(true, Limit.of(9)));
    }

    /**
     * Use a NativeQuery method that executes a SQL query with named parameters,
     * where the named parameter names are indicted by the Param annotation of each
     * method parameter.
     */
    @Test
    public void testNativeQueryWithNamedParameters() {
        // Relies on optional capability where Hibernate provides named parameters
        // for SQL queries
        if (isHibernatePersistence())
            assertEquals(List.of("Nine Eighteenths",
                                 "Nine Fifteenths",
                                 "Nine Fourteenths",
                                 "Nine Nineteenths",
                                 "Nine Seventeenths",
                                 "Nine Sixteenths",
                                 "Nine Thirteenths"),
                         Stream.of(fractions.withNamePattern("Nine ",
                                                             "teenths"))
                                         .map(f -> f.name)
                                         .toList());
    }

    /**
     * Supply a Restriction to a repository method where the Restriction
     * requires navigating through 2 levels of embeddables to compute the
     * expressions that are used in its constraint.
     */
    @Test
    public void testNavigableAttribute() {
        Restriction<Fraction> twiceAsManyNonrepeatingVsRepeatingDigits = //
                        _Fraction.decimal
                                        .navigate(_Decimal.digits)
                                        .navigate(_Digits.nonrepeating)
                                        .length()
                                        .equalTo(_Fraction.decimal
                                                        .navigate(_Decimal.digits)
                                                        .navigate(_Digits.repeating)
                                                        .length()
                                                        .times(2));

        assertEquals(List.of("4166...",
                             "5833...",
                             "9166..."),
                     fractions.withNameLike("%ths",
                                            twiceAsManyNonrepeatingVsRepeatingDigits,
                                            Order.by(_Fraction.decimal_value.desc())) //
                                     .map(f -> f.decimal.digits().toString())
                                     .sorted()
                                     .collect(Collectors.toList()));
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
     * Use a stateless repository to find an entity. Modify the entity. Verify the
     * updates can be committed or rolled back.
     */
    @Test
    public void testPersistenceContext() throws Exception {

        // Populate with 2/23 and 3/23.
        // Ensure deletion in the finally block.
        statefulFractionRepo.persistAll(List.of(Fraction.of(2, 23),
                                                Fraction.of(3, 23)));
        boolean removed = false;
        try {
            System.out.println("Fetch 2/23 to modify and commit");

            tx.begin();
            Fraction f = statefulFractions.fetch(2, 23).orElseThrow();
            f.numerator = f.numerator * 2;
            f.decimal = Decimal.of(f.numerator, 23);
            tx.commit();

            assertEquals(BigDecimal.valueOf(1739, 4),
                         statefulFractions.fetch(4, 23).orElseThrow() //
                                         .decimal.truncated());

            assertEquals(true,
                         statefulFractions.fetch(2, 23).isEmpty());

            System.out.println("Fetch 3/23 to modify and roll back");

            tx.begin();
            f = statefulFractions.fetch(3, 23).orElseThrow();
            f.numerator = f.numerator - 2;
            f.decimal = Decimal.of(f.numerator, 23);
            tx.rollback();

            Fraction f3_23 = statefulFractions.fetch(3, 23).orElseThrow();
            assertEquals(BigDecimal.valueOf(1304, 4),
                         f3_23.decimal.truncated());

            assertEquals(true,
                         statefulFractions.fetch(1, 23).isEmpty());

            Fraction f4_23 = statefulFractions.fetch(4, 23).orElseThrow();

            statefulFractionRepo.remove(f3_23, f4_23);
            removed = true;
        } finally {
            if (tx.getStatus() != Status.STATUS_NO_TRANSACTION)
                tx.rollback();

            // Ensure no fractions with denominator of 23 or more are left around
            if (!removed)
                fractions.discard(AtLeast.min(23),
                                  AtMost.max(Integer.MAX_VALUE),
                                  Restrict.unrestricted());
        }
    }

    /**
     * Supply plus and divide expressions to a restriction that is
     * supplied to a repository method.
     */
    @Test
    public void testPlusAndDivide() {

        Restriction<Fraction> restriction = //
                        _Fraction.denominator
                                        .plus(1)
                                        .dividedBy(_Fraction.numerator)
                                        .equalTo(2);

        assertEquals(List.of("Two Thirds", //     (3+1)/2 = 2
                             "Two Fourths", //    (4+1)/2 floor is 2
                             "Three Fifths", //   (5+1)/3 = 2
                             "Three Sixths", //   (6+1)/3 floor is 2
                             "Four Sevenths", //  (7+1)/4 = 2
                             "Three Sevenths", // (7+1)/3 floor is 2
                             "Four Eighths", //   (8+1)/4 floor is 2
                             "Five Ninths", //    (9+1)/5 = 2
                             "Four Ninths", //    (9+1)/4 floor is 2
                             "Five Tenths", //   (10+1)/5 floor is 2
                             "Four Tenths", //   (10+1)/4 floor is 2
                             "Six Elevenths", // (11+1)/6 = 2
                             "Five Elevenths", // ...
                             "Six Twelfths",
                             "Five Twelfths",
                             "Seven Thirteenths",
                             "Six Thirteenths",
                             "Five Thirteenths",
                             "Seven Fourteenths",
                             "Six Fourteenths",
                             "Eight Fifteenths",
                             "Seven Fifteenths",
                             "Six Fifteenths",
                             "Eight Sixteenths",
                             "Seven Sixteenths",
                             "Six Sixteenths",
                             "Nine Seventeenths",
                             "Eight Seventeenths",
                             "Seven Seventeenths",
                             "Nine Eighteenths",
                             "Eight Eighteenths",
                             "Seven Eighteenths",
                             "Ten Nineteenths",
                             "Nine Nineteenths",
                             "Eight Nineteenths",
                             "Seven Nineteenths",
                             "Ten Twentieths",
                             "Nine Twentieths",
                             "Eight Twentieths"),
                     fractions.where(restriction)
                                     .map(f -> f.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Tests a Query method with the First annotation defaulting to a value
     * of 1.
     */
    @Test
    public void testQueryFirst1() {

        assertEquals("Fifteen Sixteenths",
                     fractions.greatestLessThan1(16).orElseThrow() //
                                     .name);

        assertEquals("Seven Eighths",
                     fractions.greatestLessThan1(8).orElseThrow() //
                                     .name);
    }

    /**
     * Tests a Query method with the First annotation defaulting to a value
     * of 1.
     */
    @Test
    public void testQueryFirstNoneFound() {

        assertEquals(false,
                     fractions.greatestLessThan1(0).isPresent());
    }

    /**
     * Use the QueryOptions annotation to establish a query timeout on a
     * repository method annotated NativeQuery.
     */
    @AllowedFFDC("javax.transaction.xa.XAException") // due to query timeout
    @Test
    public void testQueryTimeoutAsQueryOptionOnNativeQuery() throws Exception {
        // Derby ignores query timeout and the lock timeout ends up applying instead
        if (isDerby())
            return;

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch blocked = new CountDownLatch(1);

        CompletableFuture<Boolean> lockDB = CompletableFuture.supplyAsync(() -> {
            try {
                tx.setTransactionTimeout((int) TIMEOUT_S * 3);
                tx.begin();
                boolean found = fractions.change(BigDecimal.valueOf(1880, 4),
                                                 BigDecimal.valueOf(1870, 4),
                                                 3,
                                                 16);
                System.out.println("Obtained lock on 3/16");
                locked.countDown();
                blocked.await(TIMEOUT_S * 2, TimeUnit.SECONDS);
                System.out.println("Release lock on 3/16");
                tx.rollback();
                return found;
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new CompletionException(x);
            }
        });

        try {
            assertEquals(true, locked.await(TIMEOUT_S, TimeUnit.SECONDS));
            try {
                boolean found = fractions.change(BigDecimal.valueOf(1900, 4),
                                                 BigDecimal.valueOf(1800, 4),
                                                 3,
                                                 16);
                fail("Query timeout on QueryOptions should have caused the" +
                     " query to time out. Instead found entity? " + found);
            } catch (DataException x) {
                // expected for query timeout
            }

            blocked.countDown();

            // changes must be rolled back and the lock released
            Fraction f3_16 = fractions.of(3, 16)
                            .orElseThrow();
            assertEquals(BigDecimal.valueOf(1875, 4),
                         f3_16.decimal.ceiling());
            assertEquals(BigDecimal.valueOf(1875, 4),
                         f3_16.decimal.truncated());

            // allow error to be raised if any
            lockDB.get(TIMEOUT_S, TimeUnit.SECONDS);
        } finally {
            locked.countDown();
            blocked.countDown();
        }
    }

    /**
     * Use a stateful repository to find an entity. Modify the entity. Use a
     * repository Refresh method to restore the state of the entity from the
     * database and verify that the previously modified entity attributes
     * have been restored to their prior values.
     */
    @Test
    public void testRefresh() throws Exception {

        // Populate with 7/23 and 8/23.
        // Ensure deletion in the finally block.
        statefulFractionRepo.persistAll(List.of(Fraction.of(7, 23),
                                                Fraction.of(8, 23)));
        boolean removed = false;
        try {
            System.out.println("Fetch 7/23 to modify and refresh outside of tran");

            Fraction f7_23 = statefulFractions.fetch(7, 23).orElseThrow();
            f7_23.decimal = Decimal.of(7, 21);
            f7_23.reduced = false;
            statefulFractionRepo.restore(f7_23);
            assertEquals(true,
                         f7_23.reduced);
            assertEquals(BigDecimal.valueOf(3043, 4), // first 4 decimals of 7/23
                         f7_23.decimal.truncated());

            System.out.println("Fetch 8/23 to modify and refresh within tran");

            tx.begin();
            Fraction f8_23 = statefulFractions.fetch(8, 23).orElseThrow();
            f8_23.decimal = Decimal.of(8, 16);
            f8_23.reduced = false;
            statefulFractionRepo.restore(f8_23);
            assertEquals(true,
                         f8_23.reduced);
            assertEquals(BigDecimal.valueOf(3478, 4), // first 4 decimals of 8/23
                         f8_23.decimal.truncated());
            tx.commit();

            f8_23 = statefulFractions.fetch(8, 23).orElseThrow();

            statefulFractionRepo.remove(f7_23, f8_23);
            removed = true;
        } finally {
            if (tx.getStatus() != Status.STATUS_NO_TRANSACTION)
                tx.rollback();

            // Ensure no fractions with denominator of 23 or more are left around
            if (!removed)
                fractions.discard(AtLeast.min(23),
                                  AtMost.max(Integer.MAX_VALUE),
                                  Restrict.unrestricted());
        }
    }

    /**
     * Use a stateful repository to remove entities.
     */
    @Test
    public void testRemove() throws Exception {

        // Populate with 9/23, 10/23, 11/23, and 12/23.
        // Ensure deletion in the finally block.
        statefulFractionRepo.persistAll(List.of(Fraction.of(9, 23),
                                                Fraction.of(10, 23),
                                                Fraction.of(11, 23),
                                                Fraction.of(12, 23)));
        try {
            System.out.println("Remove 10/23 and 12/23 within the same tran");

            tx.begin();
            Fraction f10_23 = statefulFractions.fetch(10, 23).orElseThrow();
            Fraction f12_23 = statefulFractions.fetch(12, 23).orElseThrow();
            statefulFractionRepo.remove(f10_23, f12_23);
            tx.commit();

            Fraction f9_23 = statefulFractions.fetch(9, 23).orElseThrow();

            assertEquals(true,
                         statefulFractions.fetch(10, 23).isEmpty());

            Fraction f11_23 = statefulFractions.fetch(11, 23).orElseThrow();

            assertEquals(true,
                         statefulFractions.fetch(12, 23).isEmpty());

            System.out.println("Remove 11/23, fetched outside of tran");

            statefulFractionRepo.remove(f11_23);

            f9_23 = statefulFractions.fetch(9, 23).orElseThrow();

            assertEquals(true,
                         statefulFractions.fetch(11, 23).isEmpty());
        } finally {
            if (tx.getStatus() != Status.STATUS_NO_TRANSACTION)
                tx.rollback();

            // Ensure no fractions with denominator of 23 or more are left around
            fractions.discard(AtLeast.min(23),
                              AtMost.max(Integer.MAX_VALUE),
                              Restrict.unrestricted());
        }
    }

    /**
     * Use a repository method that imposes restrictions on a Query By Method Name
     * count method that has no constraints indicated by the method name.
     */
    @Test
    public void testRestrictedCount() {

        Restriction<Fraction> filter = _Fraction.denominator
                        .in(// 1/18, 2/19, 3/20:
                            _Fraction.numerator.plus(17),
                            // 2/3, 3/8, 4/15:
                            _Fraction.numerator.plus(1)
                                            .times(_Fraction.numerator.minus(1)),
                            // One Ninth
                            // Two Tenths
                            // Six Tenths
                            // Two Twelfths
                            // Six Twelfths
                            // Ten Twelfths
                            // One Fourteenth
                            // Four Fifteenths (duplicate of 4/15 from above)
                            // Five Fifteenths
                            // Nine Fifteenths
                            // Three Sixteenths
                            // Seven Sixteenths
                            // Eight Sixteenths
                            // Four Seventeenths
                            // Five Seventeenths
                            // Nine Seventeenths
                            // Eleven Eighteenths
                            // Twelve Eighteenths
                            // Fifteen Nineteenths
                            // Sixteen Nineteenths
                            // Seventeen Twentieths:
                            _Fraction.name.length());

        assertEquals(Long.valueOf(26),
                     fractions.count(filter));
    }

    /**
     * Use a repository method that imposes restrictions on a Query By Method Name
     * count method that has a Between constraint from its method name.
     */
    @Test
    public void testRestrictedCountBy() {

        Restriction<Fraction> filter = //
                        Restrict.all(_Fraction.reduced.isTrue(),
                                     _Fraction.denominator
                                                     .minus(_Fraction.numerator)
                                                     .lessThanEqual(3));

        assertEquals(8L, // 1/3, 2/3, 1/4, 3/4, 2/5, 3/5, 4/5, 5/6
                     fractions.countByDenominatorBetween(3, 6, filter));
    }

    /**
     * Use a repository method that imposes restrictions and constraints on
     * deletion.
     */
    @Test
    public void testRestrictedDeletion() {

        // Populate with fractions that have denominators of 21 and 22.
        // These will be deleted by the test case.
        List<Fraction> twentyFirstsAndTwentySeconds = new ArrayList<>(41);
        for (int n = 1; n < 21; n++)
            twentyFirstsAndTwentySeconds.add(Fraction.of(n, 21));
        for (int n = 1; n < 22; n++)
            twentyFirstsAndTwentySeconds.add(Fraction.of(n, 22));

        fractions.supply(twentyFirstsAndTwentySeconds);
        try {
            List<Fraction> removed;

            System.out.println("Deletion with constraint and composite restriction:");

            Restriction<Fraction> restriction = Restrict
                            .all(_Fraction.numerator
                                            .plus(_Fraction.denominator)
                                            .lessThan(30),
                                 _Fraction.denominator.greaterThan(20));
            removed = fractions.remove(Like.prefix("Tw"), restriction);
            assertEquals(List.of("Two Twenty-firsts",
                                 "Two Twenty-seconds"),
                         removed.stream()
                                         .map(f -> f.name)
                                         .sorted()
                                         .toList());

            System.out.println("Deletion with constraint and unrestricted restriction:");

            Like first6CharsRepeatedAtPosition8 = //
                            Like.pattern(_Fraction.name.left(6).append("%").prepend("______ "),
                                         '$');
            removed = fractions.remove(first6CharsRepeatedAtPosition8,
                                       Restrict.unrestricted());
            assertEquals(List.of("Twenty Twenty-firsts",
                                 "Twenty Twenty-seconds"),
                         removed.stream()
                                         .map(f -> f.name)
                                         .sorted()
                                         .toList());

            System.out.println("Deletion with constraint and single restriction:");

            removed = fractions.remove(Like.pattern("-i--teen *^-*", '-', '*', '^'),
                                       _Fraction.name.like("%e________n%"));
            assertEquals(List.of("Eighteen Twenty-seconds",
                                 "Nineteen Twenty-firsts",
                                 "Nineteen Twenty-seconds"),
                         removed.stream()
                                         .map(f -> f.name)
                                         .sorted()
                                         .toList());

            System.out.println("Deletion with 2 constraints and simple Like restriction:");

            assertEquals(4L, // 11/21, 12/21, 11/22, 12/22
                         fractions.discard(AtLeast.min(21),
                                           AtMost.max(22),
                                           _Fraction.name.like(":l:ve:", '.', ':')));

            System.out.println("Deletion with 2 constraints and arithmetic restriction:");

            restriction = _Fraction.numerator
                            .plus(_Fraction.name.length())
                            .minus(_Fraction.denominator)
                            .equalTo(6);
            assertEquals(6L, // 8/21, 9/21, 10/21, 8/22, 9/22, 10/22
                         fractions.discard(AtLeast.min(21),
                                           AtMost.max(22),
                                           restriction));

            System.out.println("Deletion with 2 constraints and composite restriction:");

            restriction = Restrict.any(_Fraction.numerator.in(21, 13),
                                       _Fraction.reduced.isFalse());
            assertEquals(13L,
                         // numerator in:  21/22, 13/23, 13/22
                         // not reduced:   3/21, 6/21, 7/21, 14/21, 15/21, 18/21,
                         //                4/22, 6/22, 14/22, 16/22,
                         fractions.discard(AtLeast.min(21),
                                           AtMost.max(22),
                                           restriction));

            System.out.println("Deletion by method name query with restriction:");

            restriction = Restrict.all(_Fraction.name.left(5).right(1).equalTo(" "),
                                       _Fraction.denominator.between(21, 30));

            assertEquals(3, // 4/21, 5/21, 5/22
                         fractions.deleteByNameStartsWith("F", restriction));

            // If the assertions above complete successfully, the following remain:
            //  1/21 One Twenty-first
            // 16/21 Sixteen Twenty-firsts
            // 17/21 Seventeen Twenty-firsts
            //  1/22 One Twenty-second
            //  3/22 Three Twenty-seconds
            //  7/22 Seven Twenty-seconds
            // 15/22 Fifteen Twenty-seconds
            // 17/22 Seventeen Twenty-seconds
        } finally {
            // Ensure no fractions with deninators above 20 are left around
            fractions.discard(AtLeast.min(21),
                              AtMost.max(Integer.MAX_VALUE),
                              Restrict.unrestricted());
        }
    }

    /**
     * Use a repository method that imposes restrictions on a Query By Method Name
     * exists method that has no constraints indicated by the method name.
     */
    @Test
    public void testRestrictedExists() {

        assertEquals(true,
                     fractions.exists(_Fraction.numerator
                                     .times(_Fraction.denominator)
                                     .equalTo(133))); // 7/19 is present in data

        assertEquals(false,
                     fractions.exists(_Fraction.numerator
                                     .times(_Fraction.denominator)
                                     .equalTo(134))); // 2/67 and 1/134 not present
    }

    /**
     * Use a repository method that imposes restrictions on a Query By Method Name
     * exists method that has additional constraints from its method name.
     */
    @Test
    public void testRestrictedExistsBy() {

        Restriction<Fraction> filter = //
                        Restrict.all(_Fraction.reduced.isTrue(),
                                     _Fraction.numerator.between(2, 4));

        assertEquals(Boolean.FALSE, // none of (2/6, 3/6, 4/6) are reduced
                     fractions.existsByDenominatorGreaterThanAndDenominatorLessThan //
                     (5,
                      7,
                      filter));

        assertEquals(Boolean.TRUE, // at least one of (2/8, 3/8, 4/8) is reduced
                     fractions.existsByDenominatorGreaterThanAndDenominatorLessThan //
                     (7,
                      9,
                      filter));
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

    /**
     * Invoke methods on a stateful repository while running on an unmanaged
     * thread that is not part of any request scope.
     */
    @Test
    public void testStatefulOnUnmanagedThread() throws Exception {

        CompletableFuture.supplyAsync(() -> {
            try {
                try {
                    tx.begin();
                    statefulFractionRepo.write(Fraction.of(13, 23));
                    tx.commit();

                    tx.begin();
                    Fraction f = statefulFractions.fetch(13, 23).orElseThrow();
                    f.decimal = Decimal.of(1, 23);
                    statefulFractionRepo.restore(f);
                    f.reduced = false;
                    statefulFractions.flush();
                    statefulFractions.detach(f);
                    f.numerator = 1;
                    tx.commit();

                    f.numerator = 13;
                    f.denominator = 26;
                    tx.begin();
                    f = statefulFractionRepo.manage(f); // merge operation
                    tx.commit();
                } finally {
                    if (tx.getStatus() != Status.STATUS_NO_TRANSACTION)
                        tx.rollback();
                }
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new CompletionException(x);
            }
            return "testStatefulOnUnmanagedThread async operations completed";
        }).get(TIMEOUT_S, TimeUnit.SECONDS);

        boolean removed = false;
        tx.begin();
        try {
            assertEquals(true,
                         statefulFractions.fetch(13, 23).isEmpty());

            Fraction f = statefulFractions.fetch(13, 26).orElseThrow();
            assertEquals(false,
                         f.reduced);
            assertEquals(13,
                         f.numerator);
            assertEquals(26,
                         f.denominator);
            assertEquals(BigDecimal.valueOf(5652, 4), // first 4 decimals of 13/23
                         f.decimal.truncated());
            statefulFractionRepo.remove(f);
            tx.commit();
            removed = true;
        } finally {
            if (!removed)
                fractions.discard(AtLeast.min(23),
                                  AtMost.max(Integer.MAX_VALUE),
                                  Restrict.unrestricted());
        }
    }

    /**
     * Use a stateful repository to find an entity. Make updates to the entity.
     * Obtain the EntityManager via a resource accessor method and use it to
     * flush changes to the database. Then use a repository detach operation to
     * make the entity unmanaged and make additional changes to the entity.
     * After committing, verify that only the updates from before the flush
     * are written to the database.
     */
    @Test
    public void testStatefulResourceAccessor() throws Exception {

        // Populate with 6/23.
        // Ensure deletion in the finally block.
        statefulFractionRepo.write(Fraction.of(6, 23));
        boolean removed = false;
        try {
            System.out.println("Fetch 6/23 to modify, flush, detach, modify," +
                               " and commit");

            tx.begin();
            Fraction f = statefulFractions.fetch(6, 23).orElseThrow();
            f.reduced = false;
            statefulFractions.flush(); // via resource accessor
            statefulFractions.detach(f);
            f.decimal = Decimal.of(3, 23);
            tx.commit();

            f = statefulFractions.fetch(6, 23).orElseThrow();

            // modifications from before flush should be persisted

            assertEquals(false,
                         f.reduced);

            // modifications from after detach should not be persisted

            assertEquals(BigDecimal.valueOf(2608, 4), // first 4 decimals of 6/23
                         f.decimal.truncated());

            statefulFractionRepo.remove(f);
            removed = true;
        } finally {
            if (tx.getStatus() != Status.STATUS_NO_TRANSACTION)
                tx.rollback();

            // Ensure no fractions with denominator of 23 or more are left around
            if (!removed)
                fractions.discard(AtLeast.min(23),
                                  AtMost.max(Integer.MAX_VALUE),
                                  Restrict.unrestricted());
        }
    }

}
