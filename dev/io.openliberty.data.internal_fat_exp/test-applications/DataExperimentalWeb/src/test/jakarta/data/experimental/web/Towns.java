/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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

import jakarta.data.constraint.AtLeast;
import jakarta.data.constraint.AtMost;
import jakarta.data.constraint.GreaterThan;
import jakarta.data.constraint.Like;
import jakarta.data.constraint.NotEqualTo;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Is;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;

import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.IgnoreCase;
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
    long countByStateButNotTown(@By("stateName") String state,
                                @By(ID) @Is(NotEqualTo.class) TownId exceptForInState);

    @Delete
    TownId[] deleteWithinPopulationRange(@By("population") @Is(AtLeast.class) int min,
                                         @By("population") @Is(AtMost.class) int max);

    @Exists
    boolean existsById(@By(ID) TownId id);

    @Find
    Optional<Town> findById(@By(ID) TownId id);

    @Exists
    boolean isBiggerThan(@By("population") @Is(GreaterThan.class) int minPopulation,
                         @By(ID) TownId id);

    @Find
    @OrderBy("stateName")
    @OrderBy("name")
    Stream<Town> largerThan(@By("population") @Is(GreaterThan.class) int minPopulation,
                            @By("name") @IgnoreCase @Is(NotEqualTo.class) String cityToExclude,
                            @By("stateName") Like statePattern);

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

    // The assignment is intentionally between the other two query parameters
    // to cover a scenario of intermixing them.
    @Update
    boolean setPopulation(@By(ID) TownId id,
                          @Assign("population") int newPopulation,
                          @By("population") int oldPopulation);

    @Find
    @OrderBy(value = ID, descending = true)
    CursoredPage<Town> sizedWithin(@By("population") @Is(AtLeast.class) int minPopulation,
                                   @By("population") @Is(AtMost.class) int maxPopulation,
                                   PageRequest pagination);
}
