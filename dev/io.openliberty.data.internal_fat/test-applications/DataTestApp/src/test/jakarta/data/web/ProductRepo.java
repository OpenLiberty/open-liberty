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
package test.jakarta.data.web;

import java.util.List;
import java.util.Set;

import io.openliberty.data.Data;
import io.openliberty.data.OrderBy;
import io.openliberty.data.Param;
import io.openliberty.data.Query;
import io.openliberty.data.Select;
import io.openliberty.data.Select.Aggregate;
import io.openliberty.data.Update;
import io.openliberty.data.Where;

/**
 *
 */
@Data
public interface ProductRepo {
    void addOrModify(Product p);

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

    @Select(function = Aggregate.MINIMUM, value = "price")
    float lowestPrice();

    @Select(function = Aggregate.AVERAGE, value = "price")
    float meanPrice();

    @Query("UPDATE Product o SET o.price = o.price - (?2 * o.price) WHERE o.name LIKE CONCAT('%', ?1, '%')")
    long putOnSale(String nameContains, float discount);

    @Update("o.price=?3")
    @Where("o.id=?1 AND o.version=?2")
    boolean setPrice(String id, long currentVersion, float newPrice);

    @Select(function = Aggregate.COUNT, distinct = false, value = { "name", "description", "price" })
    ProductCount stats();

    @Select(function = Aggregate.SUM, distinct = true, value = "price")
    float totalOfDistinctPrices();
}
