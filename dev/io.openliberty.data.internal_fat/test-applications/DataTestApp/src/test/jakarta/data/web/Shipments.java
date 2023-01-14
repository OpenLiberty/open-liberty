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

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import jakarta.data.repository.Condition;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Filter;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;
import jakarta.data.repository.Update;

/**
 *
 */
@Repository
public interface Shipments {
    @Filter(by = "id", param = "shipmentId")
    @Filter(by = "status", op = Condition.In, value = { "PREPARING", "READY_FOR_PICKUP" })
    @Update(attr = "status", value = "CANCELED")
    @Update(attr = "canceledAt", param = "time")
    boolean cancel(@Param("shipmentId") long id,
                   @Param("time") OffsetDateTime timeOfCancellation);

    @Filter(by = "id")
    @Filter(by = "status", value = "'READY_FOR_PICKUP'")
    @Update(attr = "status", value = "'IN_TRANSIT'")
    @Update(attr = "location")
    @Update(attr = "shippedAt")
    boolean dispatch(long id, String location, OffsetDateTime timeOfDispatch);

    @Filter(by = "id")
    Shipment find(long id);

    @Filter(by = "status")
    @OrderBy("destination")
    Stream<Shipment> find(String status);

    @OrderBy("status")
    @OrderBy(value = "orderedAt", descending = true)
    Shipment[] getAll();

    @Filter(by = "id")
    @Select("status")
    String getStatus(long id);

    @Filter(by = "status", value = "CANCELED")
    @Delete
    int removeCanceled();

    @Delete
    int removeEverything();

    void save(Shipment s);

    // TODO @Update on its own instead of this
    @Query("UPDATE Shipment o SET o.location = TRIM(o.location)")
    void trim();

    @Filter(by = "id")
    @Filter(by = "location")
    @Update(attr = "location")
    boolean updateLocation(long id, String prevLocation, String newLocation);
}
