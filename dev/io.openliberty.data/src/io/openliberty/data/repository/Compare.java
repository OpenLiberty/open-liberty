/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.data.repository;

/**
 * Comparison operations for {@link Filter#op()}.
 */
public enum Compare {
    /**
     * <p>Inclusive between. A {@link Filter} with this
     * {@link Filter#op() comparison operation} requires 2 parameters,
     * the first indicating the lower bound and the second indicating the
     * upper bound of the range that the value must be within
     * in order to match.</p>
     *
     * <p>Applies to: numeric, strings, time.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "age", op = Compare.Between)
     * List{@literal <Person>} aged(int minAge, int maxAge);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * teens = people.aged(13, 19);
     * </pre>
     */
    Between(null),

    /**
     * <p>The {@code Contains} {@link Filter#op() operation} requires 1 parameter.
     * If the entity's attribute value is a collection, the {@code Contains} operation
     * checks whether the parameter is found within the collection value.
     * If the entity's attribute value is a string, the {@code Contains} operation
     * checks whether the parameter is a substring of the string value.
     * The characters {@code %} and {@code _} within the parameter are considered
     * wildcard characters by Jakarta Persistence-backed providers when comparing
     * string values.</p>
     *
     * <p>Applies to: collections, strings.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "name", ignoreCase=true, op = Compare.Contains)
     * {@literal @Filter}(by = "availableSizes", op = Compare.Contains)
     * {@literal @OrderBy}("productId")
     * Page{@literal <Product>} match(String namePattern, Size size, Pageable pagination);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * page1 = products.match("t%shirt", Size.SMALL, Pageable.size(10));
     * </pre>
     */
    Contains(null),

    /**
     * <p>The {@code Empty} {@link Filter#op() comparison operation}
     * matches empty collection values. It requires no parameters.
     * For non-collection types, it matches {@code null} values.</p>
     *
     * <p>Applies to: collections, nullable types.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "emailAddresses", op = Compare.Empty)
     * List{@literal <Customer>} withMissingContactInfo();
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * list = customers.withMissingContactInfo();
     * </pre>
     */
    Empty(null),

    /**
     * <p>The {@code EndsWith} {@link Filter#op() operation}
     * matches the ending characters of the entity attribute value
     * against the parameter that is required by this operation.
     * The characters {@code %} and {@code _} within the parameter are
     * considered wildcards by Jakarta Persistence-backed providers.</p>
     *
     * <p>Applies to: strings.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "email", op = Compare.EndsWith)
     * List{@literal <Student>} atDomain(String ending);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * cyclones = students.atDomain("@iastate.edu");
     * </pre>
     */
    EndsWith(null),

    /**
     * <p>The {@code Equal} {@link Filter#op() comparison operation}
     * matches the entity attribute value against the parameter that is
     * required by this operation.</p>
     *
     * <p>Applies to: collections, numeric, strings, time.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "productId", op = Compare.Equal)
     * Optional{@literal <Product>} get(String productId);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * found = products.get("PRD0012-W6-2100L");
     * </pre>
     */
    Equal(null),

    /**
     * <p>The {@code False} {@link Filter#op() comparison operation}
     * matches boolean values of {@code false}. It requires no parameters.</p>
     *
     * <p>Applies to: booleans.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Count}
     * {@literal @Filter}(by = "fullTime", op = Compare.False)
     * int numPartTime();
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * total = employees.numPartTime();
     * </pre>
     */
    False(null),

    /**
     * <p>The {@code GreaterThan} {@link Filter#op() operation}
     * matches if the entity attribute value is larger than the
     * parameter for this operation.</p>
     *
     * <p>Applies to: numeric, strings, time.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "balanceDue", op = Compare.GreaterThan)
     * {@literal @Update}(attr = "balanceDue", op = Operation.Multiply)
     * long chargeHighBalanceFee(float threshold, float feeRate);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * numCharged = creditAccounts.chargeHighBalanceFee(2000.0f, 1.00625);
     * </pre>
     */
    GreaterThan(null),

    /**
     * <p>The {@code GreaterThanEqual} {@link Filter#op() operation}
     * matches if the entity attribute value is larger or equal to the
     * parameter for this operation.</p>
     *
     * <p>Applies to: numeric, strings, time.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "discount", op = Compare.GreaterThanEqual)
     * Optional{@literal <Product>} onSale(float minDiscount);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * saleItems = products.onSale(10.0f);
     * </pre>
     */
    GreaterThanEqual(null),

    /**
     * <p>The {@code In} {@link Filter#op() operation}
     * matches if the entity attribute value is equal to one of the
     * elements within the parameter, which is a collection,
     * for this operation.</p>
     *
     * <p>Applies to: numeric, strings, time.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "grade", op = Compare.In, value = { "A+", "A" })
     * {@literal @Filter}(by = "homeAddress.stateCode", op = Compare.In)
     * List{@literal <Student>} topPerformers(Set<String> stateCodes);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * regionalTopPerformers = students.topPeformers(Set.of("MN", "IA", "WI"));
     * </pre>
     */
    In(null),

    /**
     * <p>The {@code LessThan} {@link Filter#op() operation}
     * matches if the entity attribute value is smaller than the
     * parameter for this operation.</p>
     *
     * <p>Applies to: numeric, strings, time.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Exists}
     * {@literal @Filter}(by = "weight", op = Compare.LessThan)
     * {@literal @Filter}(by = "id")
     * boolean eligibleForFreeShipping(String trackingNum, float weightLimit);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * freeShipping = packages.eligibleForFreeShipping(package.num, 10.0f);
     * </pre>
     */
    LessThan(null),

    /**
     * <p>The {@code LessThanEqual} {@link Filter#op() operation}
     * matches if the entity attribute value is less or equal to the
     * parameter for this operation.</p>
     *
     * <p>Applies to: numeric, strings, time.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Count}
     * {@literal @Filter}(by = "age", op = Compare.LessThanEqual)
     * {@literal @Filter}(by = "groupId")
     * int upToAge(int maxAgeFree, String groupId);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * numFreeAdmissions = attendees.upToAge(5, groupId);
     * </pre>
     */
    LessThanEqual(null),

    /**
     * <p>The {@code Like} {@link Filter#op() operation} matches the
     * entity attribute value against the pattern parameter for this operation.
     * The characters {@code %} and {@code _} within the parameter are considered
     * wildcard characters by Jakarta Persistence-backed providers when comparing
     * string values.</p>
     *
     * <p>Applies to: strings.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "name", ignoreCase=true, op = Compare.Like)
     * Page{@literal <Product>} namedLike(String namePattern, Pageable pagination);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * page1request = Pageable.size(20).sortBy(Sort.desc("price"), Sort.asc("id"));
     * page1 = products.namedLike("%phone%", page1request);
     * </pre>
     */
    Like(null),

    /**
     * <p>The {@code Null} {@link Filter#op() comparison operation}
     * matches matches {@code null} values. It requires no parameters.</p>
     *
     * <p>Applies to: nullable types.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Exists}
     * {@literal @Filter}(by = "userName", op = Compare.Null)
     * {@literal @Filter}(by = "customerNumber")
     * boolean isNewUser(long customerNum);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * needsRegistration = customers.isNewUser(customer.num);
     * </pre>
     */
    Null(null),

    /**
     * <p>The {@code StartsWith} {@link Filter#op() operation}
     * matches the starting characters of the entity attribute value
     * against the parameter that is required by this operation.
     * The characters {@code %} and {@code _} within the parameter are
     * considered wildcards by Jakarta Persistence-backed providers.</p>
     *
     * <p>Applies to: strings.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "incidentCode", op = Compare.StartsWith, value = "TH")
     * {@literal @Filter}(by = "year")
     * List{@literal <Incident>} theftsIn(int year);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * thefts = incidents.theftsIn(2022);
     * </pre>
     */
    StartsWith(null),

    /**
     * <p>The {@code True} {@link Filter#op() comparison operation}
     * matches boolean values of {@code true}. It requires no parameters.</p>
     *
     * <p>Applies to: booleans.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "clearance", op = Compare.True)
     * {@literal @Filter}(by = "price", op = Compare.LessThan)
     * {@literal @OrderBy}(value = "price", descending = true)
     * List{@literal <Product>} onClearanceUnder(float maxPrice);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * clearanceItems = products.onClearanceUnder(20.0f);
     * </pre>
     */
    True(null),

    /**
     * <p>The {@code Not} {@link Filter#op() comparison operation}
     * requires that the entity attribute value not match the parameter
     * for this operation.</p>
     *
     * <p>Applies to: collections, numeric, strings, time.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "status", op = Compare.Not, value = Status.OnLeaveOfAbsence)
     * {@literal @Filter}(by = "status", op = Compare.Not, value = Status.Retired)
     * List{@literal <Employee>} allActive();
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * empList = employees.allActive();
     * </pre>
     */
    Not(Equal),

    /**
     * <p>Matches values exclusive of a range. A {@link Filter} with this
     * {@link Filter#op() comparison operation} requires 2 parameters,
     * the first indicating the lower bound and the second indicating the
     * upper bound of the range that the value must <i>not</i> be within
     * in order to match.</p>
     *
     * <p>Applies to: numeric, strings, time.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "score", op = Compare.NotBetween)
     * List{@literal <Result>} outliers(float rangeMin, float rangeMax);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * outliers = results.outliers(75.0, 88.0);
     * </pre>
     */
    NotBetween(Between),

    /**
     * <p>The {@code NotContains} {@link Filter#op() operation} requires 1 parameter.
     * If the entity's attribute value is a collection, the {@code Contains} operation
     * checks whether the parameter is <i>not</i> found within the collection value.
     * If the entity's attribute value is a string, the {@code Contains} operation
     * checks whether the parameter is <i>not</i> a substring of the string value.
     * The characters {@code %} and {@code _} within the parameter are considered
     * wildcard characters by Jakarta Persistence-backed providers when comparing
     * string values.</p>
     *
     * <p>Applies to: collections, strings.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Exists}
     * {@literal @Filter}(by = "id")
     * {@literal @Filter}(by = "grades", op = Compare.NotContains, value = "F")
     * boolean isGraduating(long studentIdNum);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * boolean moveToNextGradeLevel = students.isGraduating(student.id);
     * </pre>
     */
    NotContains(Contains),

    /**
     * <p>The {@code NotEmpty} {@link Filter#op() comparison operation}
     * matches non-empty collection values. It requires no parameters.
     * For non-collection types, it matches non-{@code null} values.</p>
     *
     * <p>Applies to: collections, nullable types.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "phoneNumbers", op = Compare.NotEmpty)
     * {@literal @Filter}(by = "totalDonated", op = Compare.GreaterThanEqual)
     * List{@literal <Donor>} createCallingList(float donationThreshold);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * callingList = donors.createCallingList(1000.0f);
     * </pre>
     */
    NotEmpty(Empty),

    /**
     * <p>The {@code EndsWith} {@link Filter#op() operation}
     * matches the ending characters of the entity attribute value
     * against the parameter for this operation, indicating when they
     * do <i>not</i> match.
     * The characters {@code %} and {@code _} within the parameter are
     * considered wildcards by Jakarta Persistence-backed providers.</p>
     *
     * <p>Applies to: strings.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Count}
     * {@literal @Filter}(by = "vehicleReg.address.line2", op = Compare.NotEndsWith)
     * List<TrafficViolation> vehicleNotFrom(String state);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * outOfState = trafficViolations.vehicleNotFrom("MN");
     * </pre>
     */
    NotEndsWith(EndsWith),

    /**
     * <p>The {@code NotIn} {@link Filter#op() operation}
     * matches if the entity attribute value is not equal to any of the
     * elements within the parameter, which is a collection,
     * for this operation.</p>
     *
     * <p>Applies to: numeric, strings, time.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "price", op = Compare.Between)
     * {@literal @Filter}(by = "type", op = Compare.NotIn)
     * List{@literal <Vehicle>} inPriceRangeExcept(float min, float max, Set<Type> excludes);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * list = vehicles.inPriceRangeExcept(30000.0f, 40000.0f, Set.of(Type.BUS, Type.LIMO));
     * </pre>
     */
    NotIn(In),

    /**
     * <p>The {@code NotLike} {@link Filter#op() operation} matches the
     * entity attribute value against the pattern parameter for this operation,
     * indicating when they do <i>not</i> match.
     * The characters {@code %} and {@code _} within the parameter are considered
     * wildcard characters by Jakarta Persistence-backed providers when comparing
     * string values.</p>
     *
     * <p>Applies to: strings.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "notes", op = Compare.NotLike, value = "%ship%delay%")
     * {@literal @Filter}(by = "price", op = Compare.LessThanEqual)
     * {@literal @Filter}(by = "category")
     * Page{@literal <Product>} pricedUpTo(float maxPrice, String category, Pageable pagination);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * page1request = Pageable.size(25).sortBy(Sort.desc("price"), Sort.asc("id"));
     * page1 = products.pricedUpTo(30.0f, "Apparel|Mens|Shirts", page1request);
     * </pre>
     */
    NotLike(Like),

    /**
     * <p>The {@code NotNull} {@link Filter#op() comparison operation}
     * matches non-{@code null} values. It requires no parameters.</p>
     *
     * <p>Applies to: nullable types.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Count}
     * {@literal @Filter}(by = "expiredOn", op = Compare.NotNull)
     * List{@literal <Membership>} numExpired();
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * count = memberships.numExpired();
     * </pre>
     */
    NotNull(Null),

    /**
     * <p>The {@code NotStartsWith} {@link Filter#op() operation}
     * matches the starting characters of the entity attribute value
     * against the parameter for this operation, indicating when they
     * do <i>not</i> match.
     * The characters {@code %} and {@code _} within the parameter are
     * considered wildcards by Jakarta Persistence-backed providers.</p>
     *
     * <p>Applies to: strings.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Count}
     * {@literal @Filter}(by = "grade", op = Compare.NotStartsWith, value = "A")
     * {@literal @Filter}(by = "grade", op = Compare.Not, value = "B+")
     * int gradeBOrLower();
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * count = testScores.gradeBOrLower();
     * </pre>
     */
    NotStartsWith(StartsWith);

    private Compare negated;

    private Compare(Compare negated) {
        this.negated = negated;
    }

    /**
     * For comparisons that begin with {@code Not}, returns the opposite comparison
     * enumeration value that was negated to form this {@code Compare} value.
     *
     * @return the comparison that was negated to form this comparison.
     *         Null if this comparison begins with something other than {@code Not}.
     */
    public final Compare negated() {
        return negated;
    }
}
