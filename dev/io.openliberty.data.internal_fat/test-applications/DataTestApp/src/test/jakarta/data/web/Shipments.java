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

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

import io.openliberty.data.repository.Select;
import io.openliberty.data.repository.comparison.In;
import io.openliberty.data.repository.update.Assign;

/**
 *
 */
@Repository
public interface Shipments {
    @Update
    boolean cancel(long id,
                   @By("status") @In Set<String> currentStatus,
                   @Assign("status") String newStatus,
                   @Assign("canceledAt") OffsetDateTime timeOfCancellation);

    @Update
    boolean dispatch(long id,
                     String status,
                     @Assign("status") String newStatus,
                     @Assign("location") String location,
                     @Assign("shippedAt") OffsetDateTime timeOfDispatch);

    @Find
    Shipment find(@By("id") long shipmentId);

    @Find
    @OrderBy("destination")
    Stream<Shipment> find(@By("status") String shipmentStatus);

    @Find
    @OrderBy("status")
    @OrderBy(value = "orderedAt", descending = true)
    Shipment[] getAll();

    @Find
    @Select("status")
    String getStatus(long id);

    @Delete
    int removeEverything();

    @Save
    void save(Shipment s);

    @Delete
    int statusBasedRemoval(@By("status") String s);

    @Update
    boolean updateLocation(long id,
                           String location,
                           @Assign("location") String newLocation);
}
