/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package jakarta.data.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a repository method to request sorting of results.<p>
 *
 * If multiple <code>@OrderBy</code> annotations are specified on a
 * repository method, the precedence for sorting follows the order
 * in which the <code>@OrderBy</code> annotations are specified.<p>
 *
 * For example, the following sorts by the <code>lastName</code>
 * attribute and then by the <code>firstName</code> attribute
 * for entities that have the same <code>lastName</code>,<p>
 *
 * <pre>
 * &#64;Where("o.zipCode = ?1")
 * &#64;OrderBy("lastName")
 * &#64;OrderBy("firstName")
 * Person[] getEligibleVoters(int zipCode);
 * </pre>
 */
@Repeatable(OrderBy.List.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OrderBy {
    /**
     * Indicate whether to use descending (vs ascending) order when sorting
     * by this attribute.<p>
     *
     * The default value of <code>false</code> means ascending sort.<p>
     *
     * @return whether to use descending (vs ascending) order.
     */
    boolean descending() default false;

    /**
     * Indicate whether or not to request case insensitive comparison when
     * sorting by this attribute.<p>
     *
     * The default value of <code>false</code> means that case insensitive
     * comparison is not requested.<p>
     *
     * @return whether or not to request case insensitive comparison.
     */
    boolean ignoreCase() default false;

    /**
     * Entity attribute name to sort by.<p>
     *
     * For example, with JPQL for relational databases,<p>
     *
     * <pre>
     * &#64;Where("o.lastName = ?1")
     * &#64;OrderBy("age")
     * Stream&#60;Person&#62; familyMembersByAge(String lastName);
     * </pre>
     */
    String value();

    /**
     * Enables multiple <code>OrderBy</code>
     * annotations on the same type.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface List {
        OrderBy[] value();
    }
}
