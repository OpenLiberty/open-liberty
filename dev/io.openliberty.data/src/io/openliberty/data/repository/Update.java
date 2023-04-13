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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Annotates a repository method to specify an update operation.</p>
 *
 * <p>Multiple {@code Update} annotations can be placed on a repository method.
 * When positional parameters are used, method parameters corresponding to
 * {@link Filter} annotations are specified first,
 * followed by the method parameters for {@code Update} annotations
 * in the same order as the {@code Update} annotations appear on the method.</p>
 *
 * <p>Example query:</p>
 *
 * <pre>
 * {@literal @Filter}(by = "amountSold", op = Compare.LessThan)
 * {@literal @Update}(attr = "price", op = Operation.Multiply)
 * {@literal @Update}(attr = "timesDiscounted", op = Operation.Add, value = "1")
 * long discountUnpopularItems(int numSoldThreshold, float discountRate);
 * </pre>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * numDiscounted = products.discountUnpopularItems(20, 0.9f);
 * </pre>
 *
 * <p>Do not use in combination with the {@link jakarta.data.repository.Query Query},
 * {@link Count}, {@link Delete}, or {@link Exists} annotation.</p>
 */
@Repeatable(Update.List.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Update {

    /**
     * <p>Entity attribute name. For hierarchical data, this can include
     * the {@code .} character. For example, {@code address.zipCode} on a
     * {@code Customer} entity with a field {@code address} of type
     * {@code Address} with a field {@code zipCode} of type {@code int}.</p>
     *
     * @return entity attribute name.
     */
    String attr();

    /**
     * <p>The type of update {@link Operation} to apply to the entity attribute.
     * The default value is assignment of a new value.</p>
     *
     * @return the type of update operation.
     */
    Operation op() default Operation.Assign;

    /**
     * <p>Optionally specifies a named parameter for the update {@link Operation}.
     * When an update operation requires 2 parameters, the first parameter is the
     * current entity attribute value and the second parameter is either a
     * {@link #value() hard-coded value}, a {@link #param() named parameter},
     * or a parameter to the method.</p>
     *
     * <p>The default value is the empty string, which indicates that a named
     * parameter is not provided.</p>
     *
     * @return a named parameter. Empty string indicates none is provided.
     */
    String param() default "";

    /**
     * <p>Optionally specifies a hard-coded parameter value for the update {@link Operation}.
     * Single quotes are automatically added to the beginning and end of the
     * hard-coded value unless the value starts with a number or single quote.
     * When an update operation requires 2 parameters, the first parameter is the
     * current entity attribute value and the second parameter is either a
     * {@link #value() hard-coded value}, a {@link #param() named parameter},
     * or a parameter to the method.</p>
     *
     * <p>The default value is the empty array, which indicates that a hard-coded
     * parameter is not provided.</p>
     *
     * @return a hard-coded parameter value. Empty indicates none is provided.
     */
    String[] value() default {};

    /**
     * Enables multiple {@link Update}
     * annotations on the same type.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface List {
        Update[] value();
    }
}
