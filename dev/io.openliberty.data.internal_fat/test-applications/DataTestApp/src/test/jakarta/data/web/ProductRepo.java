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
import java.util.Set;

import jakarta.data.repository.Condition;
import jakarta.data.repository.Count;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Exists;
import jakarta.data.repository.Filter;
import jakarta.data.repository.Operation;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;
import jakarta.data.repository.Select.Aggregate;
import jakarta.data.repository.Update;

/**
 *
 */
@Repository
public interface ProductRepo {
    @Delete
    void clear();

    @Query("DELETE FROM Product o WHERE o.id IN ?1")
    int discontinueProducts(Set<String> ids);

    @Select(value = "name", distinct = true)
    @OrderBy("name")
    List<String> findByNameLike(String namePattern);

    Product[] findByVersionGreaterThanEqualOrderByPrice(long minVersion);

    @Query("SELECT o FROM Product o WHERE o.id=:productId")
    Product findItem(@Param("productId") String id);

    @Select(function = Aggregate.MAXIMUM, value = "price")
    float highestPrice();

    @Update(attr = "price", op = Operation.Multiply)
    @Update(attr = "version", op = Operation.Add, value = "1")
    long inflateAllPrices(float rateOfIncrease);

    @Filter(by = "name", op = Condition.Contains)
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

    void save(Product p);

    @Filter(by = "id")
    @Filter(by = "version")
    @Update(attr = "price")
    boolean setPrice(String id, long currentVersion, float newPrice);

    @Select(function = Aggregate.COUNT, distinct = false, value = { "name", "description", "price" })
    ProductCount stats();

    @Count
    int total();

    @Select(function = Aggregate.SUM, distinct = true, value = "price")
    float totalOfDistinctPrices();

    @Filter(by = "id", op = Condition.In)
    @Update(attr = "price", op = Operation.Divide)
    @Update(attr = "version", op = Operation.Subtract, value = "1")
    long undoPriceIncrease(Iterable<String> productIds, float divisor);
}
