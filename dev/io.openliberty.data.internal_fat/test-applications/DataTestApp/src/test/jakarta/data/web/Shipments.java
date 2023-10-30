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

import jakarta.data.repository.Delete;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

import io.openliberty.data.repository.Filter;
import io.openliberty.data.repository.Select;
import io.openliberty.data.repository.update.Assign;

/**
 *
 */
@Repository
public interface Shipments {
    @Query("UPDATE Shipment o SET o.status='CANCELED', o.canceledAt=:time WHERE (o.id=:shipmentId And o.status IN ('PREPARING', 'READY_FOR_PICKUP'))")
    // TODO switch to parameter annotations once annotations for conditions area added
    //@Filter(by = "id", param = "shipmentId")
    //@Filter(by = "status", op = Compare.In, value = { "PREPARING", "READY_FOR_PICKUP" })
    //@Update(attr = "status", value = "CANCELED")
    //@Update(attr = "canceledAt", param = "time")
    boolean cancel(@Param("shipmentId") long id,
                   @Param("time") OffsetDateTime timeOfCancellation);

    boolean dispatch(long id,
                     String status,
                     @Assign("status") String newStatus,
                     @Assign("location") String location,
                     @Assign("shippedAt") OffsetDateTime timeOfDispatch);

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

    @Save
    void save(Shipment s);

    boolean updateLocation(long id,
                           String location,
                           @Assign("location") String newLocation);
}
