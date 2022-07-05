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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an interface as a data access repository.<p>
 *
 * For example,
 *
 * <pre>
 * &#64;Data(Product.class)
 * public interface Products {
 *     Product[] findByProductNameLikeOrderByPrice(String nameContains);
 *
 *     &#64;Query("UPDATE Product o SET o.price = o.price * (1 - ?2) WHERE o.id = ?1")
 *     boolean putOnSale(long productId, float discountRate);
 *
 *     ...
 * </pre>
 *
 * This class is a CDI bean-defining annotation.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Data {
    /**
     * Entity class. By default, detect automatically.
     */
    Class<?> value() default void.class;

    /**
     * Returns the name of the provider of backend data access for
     * the entity.
     *
     * @return provider name.
     */
    String provider() default "DefaultDataStore";
}
