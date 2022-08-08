/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the Java class to use when returning query results
 * or the result of an update or delete from a repository method.<p>
 *
 * Usage of this annotation is unnecessary when the return type
 * can be inferred from the repository method return type
 * or if the type that is chosen by default based on the entity type,
 * entity attribute type, or update count is acceptable.<p>
 *
 * When the repository method uses parameterized types (such as
 * with <code>List&#60;String&#62;</code> and
 * <code>CompletableFuture&#60;MyEntity&#62;</code>)
 * the type information is not available at run time in order
 * to infer a return type. In this case, the default behavior is to
 * use the entity class if entity instance(s) are returned,
 * or the attribute class if a single attribute is returned,
 * or Long.class if an update or delete.<p>
 *
 * This annotation explicitly specifies the return type for
 * results, overriding the aforementioned behavior.<p>
 *
 * For example, if the entity type is <code>Employee</code>,
 * but you want to instead receive query results as a
 * <code>List&#60;Customer&#62;</code>,
 * and the following constructor is present,
 * <code>public Customer(String firstName, String surname, String homeAddress)</code>,
 * you can write a repository method,
 *
 * <pre>
 * &#64;Result(Customer.class)
 * &#64;Select({ "firstName", "lastName", "address" }) // attributes of Employee.class
 * List&#60;Customer&#62; findBySalaryGreaterThan(float salaryAbove);
 * </pre>
 *
 * To get a <code>CompletableFuture</code> to return a
 * <code>Boolean</code> instead of the default of <code>Long</code>,
 * you can write a repository method,
 *
 * <pre>
 * &#64;Asynchronous
 * &#64;Query("UPDATE Products o SET o.available=o.available-?1 WHERE o.id=?2 AND o.available>=?1")
 * &#64;Result(Boolean.class)
 * CompletableFuture&#60;Boolean&#62; reduceInventoryAsync(int amount, String productId);
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Result {
    /**
     * Specifies a return type for query results or an update or delete result.
     * For custom types, instances of are constructed with the entity attributes
     * selected in the order that is specified by {@link Select#value}.
     *
     * @return the return type for results.
     */
    Class<?> value();
}
