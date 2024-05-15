/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

/**
 * Repository for the DemographicInfo entity, which covers the basic types
 * Instant, BigDecimal, and BigInteger.
 */
@Repository
public interface Demographics {

    @Find
    Stream<DebtPerWorker> debtPerFullTimeWorker();

    @OrderBy("publicDebt")
    List<DemographicInfo> findByPublicDebtBetween(BigDecimal min, BigDecimal max);

    // TODO support for Instant requires JPA 3.2, after which the following can be tried out:

    //@Query("SELECT publicDebt / numFullTimeWorkers FROM DemographicInfo WHERE EXTRACT (YEAR FROM collectedOn) = ?1")
    //Optional<BigDecimal> publicDebtPerFullTimeWorker(int year);

    //@Find
    //Optional<DemographicInfo> read(Instant collectedOn);

    @OrderBy("numFullTimeWorkers")
    @Query("WHERE numFullTimeWorkers >= :min AND numFullTimeWorkers <= :max")
    List<Instant> whenFullTimeEmploymentWithin(BigInteger min, BigInteger max);

    @Insert
    void write(DemographicInfo info);
}
