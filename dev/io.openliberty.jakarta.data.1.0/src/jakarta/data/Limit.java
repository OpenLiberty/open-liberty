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
package jakarta.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a repository finder method to restrict how many results are
 * returned.<p>
 *
 * For example, the following returns the 10 largest cities in a country,<p>
 *
 * <pre>
 * &#64;Where("o.country = ?1")
 * &#64;OrderBy("population")
 * &#64;Limit(10)
 * City[] findMostPopulousIn(String country);
 * </pre>
 *
 * This annotation can also be used to indicate a single result for a
 * {@link java.util.concurrent.CompletionStage CompletionStage} or
 * {@link java.util.concurrent.CompletableFuture CompletableFuture}.<p>
 *
 * For example,<p>
 *
 * <pre>
 * &#64;Asynchronous
 * &#64;Limit(1)
 * CompletableFuture&#60;City&#62; findByCityAndStateAndCountry(String cityName,
 *                                                      String stateName,
 *                                                      String countryName);
 * </pre>
 *
 * Do not combine on a method with {@link Paginated &#64;Paginated} or
 * {@link Pageable}, which limits results per page instead of per query.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Limit {
    /**
     * Maximum number of results for a query invocation.
     * Must be a positive number.
     *
     * @return maximum number of results for a query invocation.
     */
    int value();
}
