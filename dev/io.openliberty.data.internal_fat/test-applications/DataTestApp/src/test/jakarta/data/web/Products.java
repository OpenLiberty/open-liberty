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
package test.jakarta.data.web;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;

import io.openliberty.data.repository.Compare;
import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Delete;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Filter;
import io.openliberty.data.repository.Operation;
import io.openliberty.data.repository.Select;
import io.openliberty.data.repository.Select.Aggregate;
import io.openliberty.data.repository.Update;

/**
 * Repository interface for the unannotated Product entity, which has a UUID as the Id.
 */
@Repository
public interface Products {
    @Delete
    void clear();

    @Query("DELETE FROM Product o WHERE o.pk IN ?1")
    int discontinueProducts(Set<UUID> ids);

    @Select(value = "name", distinct = true)
    @OrderBy("name")
    List<String> findByNameLike(String namePattern);

    Product[] findByVersionGreaterThanEqualOrderByPrice(long minVersion);

    @Query("SELECT o FROM Product o WHERE o.pk=:productId")
    Product findItem(@Param("productId") UUID id);

    Optional<Product> findById(UUID id);

    @Select(function = Aggregate.MAXIMUM, value = "price")
    float highestPrice();

    @Update(attr = "price", op = Operation.Multiply)
    @Update(attr = "version", op = Operation.Add, value = "1")
    long inflateAllPrices(float rateOfIncrease);

    @Filter(by = "name", op = Compare.Contains)
    @Update(attr = "price", op = Operation.Multiply)
    @Update(attr = "version", op = Operation.Add, value = "1")
    long inflatePrices(String nameContains, float rateOfIncrease);

    @Exists
    boolean isNotEmpty();

    @Select(function = Aggregate.MINIMUM, value = "price")
    float lowestPrice();

    @Select(function = Aggregate.AVERAGE, value = "price")
    float meanPrice();

    @Query("UPDATE Product o SET o.price = o.price - (?2 * o.price) WHERE o.name LIKE CONCAT('%', ?1, '%')")
    long putOnSale(String nameContains, float discount);

    // Custom repository method that combines multiple operations into a single transaction
    @Transactional
    default Product remove(UUID id) {
        for (Optional<Product> product; (product = findById(id)).isPresent();)
            if (discontinueProducts(Set.of(id)) == 1)
                return product.get();
        return null;
    }

    void save(Product p);

    @Filter(by = "pk")
    @Filter(by = "version")
    @Update(attr = "price")
    boolean setPrice(UUID id, long currentVersion, float newPrice);

    @Select(function = Aggregate.COUNT, distinct = false, value = { "name", "description", "price" })
    ProductCount stats();

    @Count
    int total();

    @Select(function = Aggregate.SUM, distinct = true, value = "price")
    float totalOfDistinctPrices();

    @Filter(by = "pk", op = Compare.In)
    @Update(attr = "price", op = Operation.Divide)
    @Update(attr = "version", op = Operation.Subtract, value = "1")
    long undoPriceIncrease(Iterable<UUID> productIds, float divisor);
}
