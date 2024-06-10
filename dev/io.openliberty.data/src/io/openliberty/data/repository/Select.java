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
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

// This annotation will not be needed at all and can be removed if the Find
// annotation is enhanced to accept the entity attribute names, or if some
// other alternative approach is added to Jakarta Data.
/**
 * <p>Specifies which entity attribute values are retrieved by a repository
 * {@link Find} method. For a single entity attribute, the method return type
 * can be the type of the entity attribute or an array, {@link List},
 * {@link Stream}, or {@link Optional} of that type
 * For one or more entity attributes, the method return type is a Java class
 * or record with a public constructor that accepts the entity attributes in
 * the same order specified or an array, {@link List}, {@link Stream}, or
 * {@link Optional} of that type.</p>
 *
 * <p>Example query for single attribute value:</p>
 *
 * <pre>
 * {@code @Find}
 * {@code @Select}("price")
 * Optional{@code <Float>} priceOf({@code @By(ID)} long productId);
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
 * {@code @Find}
 * {@code @Select}({"firstName", "lastName"})
 * {@code @OrderBy}("lastName")
 * {@code @OrderBy}("firstName")
 * List{@code <Person>} ofAge({@code @By("age")} int yearsOld);
 * </pre>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * List{@code <Person>} thirtyYearOlds = employees.ofAge(30);
 * </pre>
 *
 * <p>Do not use in combination with the {@link jakarta.data.repository.Query Query},
 * {@link Count}, {@link Delete}, {@link Exists}, {@link Insert}, {@link Save}, or {@link Update} annotation,
 * or with any annotation in the {@link io.openliberty.data.repository.update} package.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Select {

    /**
     * Limits the entity attributes to the names listed.
     * The order in which the attributes are listed is preserved,
     * enabling them to be supplied as constructor arguments for an instance
     * of the identified type.<p>
     *
     * An example of returning a single attribute,
     *
     * <pre>
     * {@code @Find}
     * {@code @Select("price")}
     * List&#60;Float&#62; pricesOf({@code @By("productId") @In} Collection<String> ids);
     * </pre>
     *
     * An example of returning multiple attributes as a different type,
     *
     * <pre>
     * {@code @Find}
     * {@code @Select}({ "productId", "available" })
     * List&#60;SurplusItem&#62; overstocked({@code @By("available") @GreaterThan} int targetInventoryLevel);
     * </pre>
     *
     * @return names of entity attributes to select.
     */
    String[] value();
}
