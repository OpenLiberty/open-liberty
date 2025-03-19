/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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
package test.jakarta.data.experimental.web;

import static io.openliberty.data.repository.Is.Op.GreaterThanEqual;
import static io.openliberty.data.repository.Is.Op.Substringed;
import static io.openliberty.data.repository.function.Rounded.Direction.DOWN;
import static io.openliberty.data.repository.function.Rounded.Direction.UP;
import static jakarta.data.repository.By.ID;

import java.util.List;
import java.util.UUID;

import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Select;
import jakarta.data.repository.Update;

import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Is;
import io.openliberty.data.repository.function.Rounded;
import io.openliberty.data.repository.update.Add;
import io.openliberty.data.repository.update.Divide;
import io.openliberty.data.repository.update.Multiply;
import io.openliberty.data.repository.update.SubtractFrom;

/**
 * Repository interface for the unannotated Item entity, which has a UUID as the Id.
 * This corresponds to the Products repository in io.openliberty.data.internal_fat
 * and is duplicated here as a place to put some experimental function that
 * hasn't made it into Jakarta Data.
 */
@Repository
public interface Items {

    @Delete
    void destroy();

    @Query("SELECT DISTINCT name FROM Item WHERE name LIKE :namePattern")
    @OrderBy("name")
    List<String> findByNameLike(String namePattern);

    @Find
    Item get(@By("pk") UUID id);

    @Query("SELECT MAX(price) FROM Item")
    float highestPrice();

    @Update
    long inflateAllPrices(@Multiply("price") float rateOfIncrease);

    @Update
    long inflatePrices(@By("name") @Is(Substringed) String nameSubstring,
                       @Multiply("price") float rateOfIncrease);

    @Exists
    boolean isNotEmpty();

    @Query("SELECT MIN(price) FROM Item")
    float lowestPrice();

    @Query("SELECT AVG(price) FROM Item")
    double meanPrice();

    @Update
    int reduceBy(@By(ID) UUID id,
                 @Divide("price") int priceDivisor,
                 @Add("description") String additionalDescription);

    @Save
    void save(Item item);

    @Update
    boolean shorten(UUID pk,
                    @SubtractFrom float price,
                    @Add String description);

    @Update
    void shortenBy(@SubtractFrom("price") int reduction,
                   @Add("description") String moreDescription,
                   UUID pk);

    @Query("SELECT NEW test.jakarta.data.experimental.web.ItemCount(COUNT(name), COUNT(description), COUNT(price)) FROM Item")
    ItemCount stats();

    @Count
    int total();

    @Query("SELECT SUM(DISTINCT price) FROM Item")
    double totalOfDistinctPrices();

    @Query("UPDATE Item SET price=price/?2, version=version-1 WHERE (pk IN ?1)")
    // TODO switch to annotated parameters once available for conditions
    //@Filter(by = "pk", op = Compare.In)
    //@Update(attr = "price", op = Operation.Divide)
    //@Update(attr = "version", op = Operation.Subtract, value = "1")
    long undoPriceIncrease(Iterable<UUID> productIds, float divisor);

    @Find
    @OrderBy("price")
    Item[] versionedAtOrAbove(@By("version") @Is(GreaterThanEqual) long minVersion);

    @Find
    @OrderBy("name")
    @Select("name")
    List<String> withPriceAbout(@Rounded float price);

    @Find
    @OrderBy("name")
    @Select("name")
    List<String> withPriceFloored(@Rounded(DOWN) float price);

    @Find
    @OrderBy("name")
    @Select("name")
    List<String> withPriceCeiling(@Rounded(UP) float price);
}
