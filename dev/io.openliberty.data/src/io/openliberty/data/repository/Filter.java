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

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Annotates a repository method to specify a find operation
 * or provide conditions for a {@link Count}, {@link Delete}, {@link Exists},
 * or {@link Update} operation.</p>
 *
 * <p>Multiple {@code Filter} annotations can be placed on a repository method.
 * When positional parameters are used, method parameters corresponding to
 * {@code Filter} annotations are specified in the same order as the
 * {@code Filter} annotations appear on the method,
 * followed by the method parameters for any {@link Update} annotations,
 * followed by any additional parameters with special meaning (such as
 * {@link jakarta.data.repository.Sort sorting},
 * {@link jakarta.data.repository.Limit limits}, and
 * {@link jakarta.data.repository.Pageable pagination}.</p>
 *
 * <p>Example query:</p>
 *
 * <pre>
 * {@literal @Filter}(by = "price", op = Compare.Between)
 * {@literal @Filter}(by = "name", ignoreCase = true, op = Compare.Contains)
 * Page{@literal <Product>} pricedWithin(float minPrice, float maxPrice, String pattern, Pageable pagination);
 * </pre>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * pagination = Pageable.size(10).sortBy(Sort.desc("price"), Sort.asc("name"), Sort.asc("id"));
 * page1 = products.pricedWithin(20.0f, 40.0f, "trackball%mouse", pagination);
 * </pre>
 *
 * <p>Do not use in combination with the {@link jakarta.data.repository.Query Query} annotation.</p>
 */
@Repeatable(Filter.List.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Filter {
    /**
     * <p>Specifies the filter type, either {@link Type#And} or {@link Type#Or}.
     * The type is useful when there are multiple filter conditions to indicate whether
     * both need need to be satisfied or at least one of them.</p>
     *
     * <p>When filters on method have a combination of {@link Type#And} and {@link Type#Or},
     * precedence is determine by the database. Many relational databases give precedence
     * to {@code And}, evaluating it prior to {@code Or}.</p>
     *
     * <p>The default value is {@link Type#And}, meaning that the prior filter condition
     * (if any) and this filter condition must be satisfied.</p>
     *
     * @return the filter type.
     */
    Type as() default Type.And;

    /**
     * Entity attribute name to which the filter applies.
     * For hierarchical data, this can include the {@code .} character.
     * For example, {@code address.zipCode} on a {@code Customer} entity
     * with a field {@code address} of type {@code Address}
     * with a field {@code zipCode} of type {@code int}.</p>
     *
     * @return entity attribute name.
     */
    String by();

    /**
     * <p>Optionally specifies one or more functions to apply to the
     * value in the database when comparing. The first function
     * in the list is applied to the value found in the database.
     * If a second function is listed, that function is applied to the
     * result of the first function. For example, if applying both
     * {@link Function#Trimmed} and {@link Function#CharCount},
     * list the {@link Function#Trimmed} function first so that the
     * value is trimmed before counting the number of characters.
     * The opposite order would mean attempting to trim a numeric value,
     * which is not valid.</p>
     *
     * @return functions to apply.
     */
    Function[] fn() default {};

    /**
     * <p>Specifies the filter {@link Compare comparison operation}.</p>
     *
     * <p>The default value is {@link Compare#Equal} which is an equality comparison
     * on the value.</p>
     *
     * @return comparison operation for the filter.
     */
    Compare op() default Compare.Equal;

    /**
     * <p>Optionally specifies names of named parameter(s) for the filter
     * {@link Compare comparison operation}. Many comparison operations
     * require 1 parameter. Some require 0 or 2 parameters.
     * The name of each named parameter must correspond to the value of a
     * {@link jakarta.data.repository.Param#value() Param} annotation
     * on a method parameter. Named parameters cannot be combined with
     * positional parameters (specified as method parameters without the
     * {@link jakarta.data.repository.Param Param} annotation).</p>
     *
     * <p>The default value is the empty array, which indicates that named
     * parameters are not provided.</p>
     *
     * @return named parameter name(s). Empty array indicates none are provided.
     */
    String[] param() default {};

    /**
     * <p>Optionally specifies hard-coded parameter values for the filter
     * {@link Compare comparison operation}. Many comparison operations
     * require 1 parameter. Some require 0 or 2 parameters.
     * Single quotes are automatically added to the beginning and end of
     * hard-coded values unless the value starts with a number or single quote
     * or is a collection. If the entity attribute being filtered is a collection
     * and the operation requires a single parameter, then all of the
     * elements of {@code value} define the collection value.</p>
     *
     * <p>The default value is the empty array, which indicates that a hard-coded
     * parameter is not provided.</p>
     *
     * @return a hard-coded parameter value. Empty indicates none is provided.
     */
    String[] value() default {};

    /**
     * Enables multiple {@link Filter}
     * annotations on the same type.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface List {
        Filter[] value();
    }

    /**
     * <p>Type of filter.</p>
     */
    public enum Type {
        /**
         * <p>Indicates that the current filter condition and the previous
         * must both be satisfied.</p>
         */
        And,

        /**
         * <p>Indicates that the current filter condition or the previous
         * must be satisfied.</p>
         */
        Or
    }
}
