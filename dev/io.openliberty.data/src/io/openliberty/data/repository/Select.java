/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

/**
 * <p>Specifies which entity attribute values are retrieved by a repository method
 * find operation and, in the case of multiple entity attributes, also the ordering
 * of these entity attribute values to use when constructing results for the method.</p>
 *
 * <p>Example query for single attribute value:</p>
 *
 * <pre>
 * {@literal @Select}("price")
 * Optional{@literal <Float>} priceOf({@literal @By("id")} long productId);
 * </pre>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * price = products.priceOf(prodId);
 * </pre>
 *
 * <p>Example query of Employee entities converted to record type Person(firstName, surname):</p>
 *
 * <pre>
 * {@literal @Select}("firstName", "lastName")
 * {@literal @OrderBy}("lastName")
 * {@literal @OrderBy}("firstName")
 * List{@literal <Person>} ofAge({@literal @By("age")} int yearsOld);
 * </pre>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * List{@literal <Person>} thirtyYearOlds = employees.ofAge(30);
 * </pre>
 *
 * <p>Do not use in combination with the {@link jakarta.data.repository.Query Query},
 * {@link Count}, {@link Delete}, {@link Exists}, {@link Find}, {@link Insert}, {@link Save}, or {@link Update} annotation,
 * or with any annotation in the {@link io.openliberty.data.repository.update} package.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Select {
    // TODO should this include aggregates and distinct? These are not currently available in query-by-method
    /**
     * Aggregate functions.
     */
    public static enum Aggregate {
        AVERAGE,
        COUNT,
        MAXIMUM,
        MINIMUM,
        SUM,
        UNSPECIFIED
    }

    /**
     * Specifies an aggregate function to apply to the selected entity attribute.
     * When specifying an aggregate function, select a single entity attribute {@link #value}.
     * The default is {@link Aggregate#UNSPECIFIED}, which means no aggregate function is applied.<p>
     *
     * An example repository method that counts the distinct first names that start with
     * the specified letter,
     *
     * <pre>
     * &#64;Select(function = Aggregate.COUNT, distinct = true, value = "firstName")
     * int findByStartsWith(String firstLetter);
     * </pre>
     *
     * @return the aggregate function to apply to the selected entity attribute.
     */
    Aggregate function() default Aggregate.UNSPECIFIED;

    /**
     * Requests that only distinct values of an entity attribute be returned.
     * When specifying <code>distinct</code>, select a single entity attribute {@link #value}.
     * The default is <code>false</code>.
     *
     * @return whether only distinct values of an entity attribute should be returned.
     */
    boolean distinct() default false;

    /**
     * Limits the entity attributes to the names listed.
     * The order in which the attributes are listed is preserved,
     * enabling them to be supplied as constructor arguments for an instance
     * of the identified type.<p>
     *
     * An example of returning a single attribute,
     *
     * <pre>
     * &#64;Select("price")
     * List&#60;Float&#62; findByProductIdIn(Collection<String> productIds);
     * </pre>
     *
     * An example of returning multiple attributes as a different type,
     *
     * <pre>
     * &#64;Select({ "productId", "available" })
     * List&#60;SurplusItem&#62; findByAvailableGreaterThan(int targetInventoryLevel);
     * </pre>
     *
     * @return names of entity attributes to select.
     */
    String[] value();
}
