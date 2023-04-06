/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

import javax.management.Query;

/**
 * Provides the ability to control which columns are returned by
 * repository method find operations.<p>
 *
 * Do not combine on a single method with {@link Query @Query}, which is a more advanced way of providing this information.
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
