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
package test.jakarta.data.web;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.data.repository.Delete;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.transaction.Transactional;

/**
 * Repository interface for the unannotated Product entity, which has a UUID as the Id.
 */
@Repository
public interface Products {
    @Delete
    void clear();

    @Query("DELETE FROM Product WHERE pk IN ?1")
    int discontinueProducts(Set<UUID> ids);

    @OrderBy("name")
    List<Product> findByNameLike(String namePattern);

    Product[] findByVersionGreaterThanEqualOrderByPrice(long minVersion);

    @Query("WHERE pk=:productId")
    Product findItem(@Param("productId") UUID id);

    Optional<Product> findByPK(UUID id);

    @Query("UPDATE Product SET price = price - (?2 * price) WHERE name LIKE CONCAT('%', ?1, '%')")
    long putOnSale(String nameContains, float discount);

    // Custom repository method that combines multiple operations into a single transaction
    @Transactional
    default Product remove(UUID id) {
        for (Optional<Product> product; (product = findByPK(id)).isPresent();)
            if (discontinueProducts(Set.of(id)) == 1)
                return product.get();
        return null;
    }

    @Save
    void save(Product p);

    @Save
    Product[] saveMultiple(Product... p);

    @Query("UPDATE Product SET price=?3 WHERE pk=?1 AND version=?2")
    boolean setPrice(UUID pk,
                     long version,
                     float newPrice);

    @Update
    Boolean update(Product product);

    @Update
    Long update(Stream<Product> products);

    @Save
    Product upsert(Product p);
}
