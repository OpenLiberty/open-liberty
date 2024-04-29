/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.experimental.web;

import static jakarta.data.repository.By.ID;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;

import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Or;
import io.openliberty.data.repository.comparison.GreaterThan;
import io.openliberty.data.repository.comparison.GreaterThanEqual;
import io.openliberty.data.repository.comparison.LessThanEqual;
import io.openliberty.data.repository.comparison.StartsWith;
import io.openliberty.data.repository.function.IgnoreCase;
import io.openliberty.data.repository.function.Not;
import io.openliberty.data.repository.update.Assign;

/**
 *
 */
@Repository
public interface Towns {
    @Insert
    Town add(Town c);

    @Exists
    boolean areFoundIn(@By("stateName") String state);

    @Count
    long countByStateButNotTown_Or_NotTownButWithTownName(@By("stateName") String state, @By(ID) @Not TownId exceptForInState,
                                                          @Or @By(ID) @Not TownId exceptForTown, @By("name") String town);

    @Delete
    TownId[] deleteWithinPopulationRange(@By("population") @GreaterThanEqual int min,
                                         @By("population") @LessThanEqual int max);

    @Exists
    boolean existsById(@By(ID) TownId id);

    @Find
    Optional<Town> findById(@By(ID) TownId id);

    @Find
    @OrderBy("name")
    Stream<Town> findByIdIsOneOf(@By(ID) TownId id1,
                                 @Or @By(ID) @IgnoreCase TownId id2,
                                 @Or @By(ID) TownId id3);

    @Find
    @OrderBy("stateName")
    Stream<Town> findByNameButNotId(@By("name") String townName,
                                    @By(ID) @Not TownId exceptFor);

    @Exists
    boolean isBiggerThan(@By("population") @GreaterThan int minPopulation,
                         TownId id);

    @Find
    @OrderBy("stateName")
    @OrderBy("name")
    Stream<Town> largerThan(@By("population") @GreaterThan int minPopulation,
                            @By(ID) @IgnoreCase @Not TownId exceptFor,
                            @By("stateName") @StartsWith String statePattern);

    @Update
    int replace(@By(ID) TownId id,
                @Assign("name") String newTownName,
                @Assign("stateName") String newStateName,
                // TODO switch the above to the following once IdClass is supported for updates
                //@Assign(ID) TownId newId,
                @Assign("population") int newPopulation,
                @Assign("areaCodes") Set<Integer> newAreaCodes);

    @Update
    int replace(String name,
                String stateName,
                @Assign("name") String newTownName,
                @Assign("stateName") String newStateName,
                @Assign("areaCodes") Set<Integer> newAreaCodes,
                @Assign("population") int newPopulation);

    @Find
    @OrderBy(value = ID, descending = true)
    CursoredPage<Town> sizedWithin(@By("population") @GreaterThanEqual int minPopulation,
                                   @By("population") @LessThanEqual int maxPopulation,
                                   PageRequest pagination);
}
