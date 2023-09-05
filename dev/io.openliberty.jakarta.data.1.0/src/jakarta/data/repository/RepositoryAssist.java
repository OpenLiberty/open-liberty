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
package jakarta.data.repository;

import java.util.Optional;

/**
 * <p>A mix-in interface for repositories that provides access to additional information
 * and other function from the Jakarta Data provider.</p>
 *
 * <p>This interface is implemented by the Jakarta Data provider when the application adds
 * it to a repository, as follows:</p>
 *
 * <pre>
 * &#64;Repository
 * public interface Products extends CrudRepository&lt;Product, String&gt;, RepositoryAssist {
 *     ...
 *
 *     default Collection&lt;Product&gt; lowPricedOrHighlyDiscounted(float maxPrice, float minDiscount) {
 *         if (supportsKeyword("Or")) {
 *             return findByPriceLessThanOrDiscountGreaterThan(maxPrice, minDiscount);
 *         } else {
 *             Set&lt;Product&gt merged = new HashSet&lt;&gt();
 *             merged.addAll(findByPriceLessThan(maxPrice));
 *             merged.addAll(findByDiscountGreaterThan(minDiscount));
 *             return merged;
 *         }
 *     }
 * }
 * </pre>
 */
public interface RepositoryAssist {

    // TODO are there any resource types for NoSQL that would be useful?
    /**
     * <p>Requests that the Jakarta Data provider supply a type of resource, such as
     * {@code jakarta.persisence.EntityManager} (if backed by Jakarta Persistence) or
     * {@code javax.sql.DataSource} or {@code java.sql.Connection} (if backed by JDBC),
     * to a repository default method.</p>
     *
     * <p>If the resource type implements {@code AutoCloseable}, the repository
     * default method is expected to close the resource instance before returning.
     * For example,</p>
     *
     * <pre>
     * &#64;Repository
     * public interface Cars extends CrudRepository&lt;Car, Long&gt;, RepositoryAssist {
     *     default Car[] advancedSearch(SearchOptions filter) {
     *         try (EntityManager em = getResource(EntityManager.class).orElseThrow()) {
     *             ... use entity manager
     *             return results;
     *         }
     *     }
     * }
     * </pre>
     *
     * <p>If the repository default method does not close the resource instance,
     * then the Jakarta Data provider invokes the {@code AutoCloseable#close()}
     * method on the resource instance once the repository default method ends.</p>
     *
     * @param type type of resource requested.
     * @param <T>  type of resource requested.
     * @return {@link Optional} that contains an instance of the specified resource type
     *         if the Jakarta Data provider is backed by this type of resource and
     *         permits returning an instance of the resource via this method.
     *         Otherwise, this method returns {@link Optional#empty()}.
     * @throws IllegalStateException if invoked from outside the scope of a
     *                                   repository default method.
     */
    <T> Optional<T> getResource(Class<T> type);

    /**
     * The major version number of the Jakarta Data provider that supplies the
     * implementation of this repository.
     *
     * @return the major version number of the Jakarta Data provider.
     */
    int providerMajorVersion();

    /**
     * The minor version number of the Jakarta Data provider that supplies the
     * implementation of this repository.
     *
     * @return the minor version number of the Jakarta Data provider.
     */
    int providerMinorVersion();

    /**
     * Name of the Jakarta Data provider that supplies the implementation of this
     * repository. You can specify this provider name or the name of a different
     * Jakarta Data provider on {@link Repository#provider()} when there is a
     * need to disambiguate between multiple Jakarta Data providers.
     *
     * @return the name of the Jakarta Data provider.
     */
    String providerName();

    /**
     * Indicates whether or not a keyword is supported by the Jakarta Data provider.
     *
     * @param keyword a keyword that is defined by the Jakarta Data specification such as
     *                    {@code LessThan}, {@code Not}, or {@code IgnoreCase},
     *                    or another String that might be a custom keyword from a
     *                    Jakarta Data provider. The keyword value must be in mixed case
     *                    with the first letter of each word capitalized,
     *                    just as it would appear in a repository method.
     *                    Combination of multiple keywords, such as {@code IgnoreCaseLike}
     *                    and {@code NotStartsWith} must not be supplied to this method.
     *                    Instead, invoke {@code supportsKeyword} separately for each
     *                    individual keyword.
     *
     * @return true if the keyword is supported and properly formed, otherwise false.
     */
    boolean supportsKeyword(String keyword);
}