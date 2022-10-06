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

import java.util.stream.Stream;

import jakarta.data.Delete;
import jakarta.data.Select;
import jakarta.data.Update;
import jakarta.data.Where;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Repository;

/**
 *
 */
@Repository
public interface Shipments {
    @Update("o.status='CANCELED', o.canceledAt=CURRENT_TIMESTAMP")
    @Where("o.id=:shipmentId AND o.status IN ('PREPARING', 'READY_FOR_PICKUP')")
    boolean cancel(@Param("shipmentId") long id);

    @Update("o.status='IN_TRANSIT', o.location=?2, o.shippedAt=CURRENT_TIMESTAMP")
    @Where("o.id=?1 AND o.status = 'READY_FOR_PICKUP'")
    boolean dispatch(long id, String location);

    @Where("o.id=?1")
    Shipment find(long id);

    @Where("o.status=?1")
    @OrderBy("destination")
    Stream<Shipment> find(String status);

    @OrderBy("status")
    @OrderBy(value = "orderedAt", descending = true)
    Shipment[] getAll();

    @Select("status")
    @Where("o.id=?1")
    String getStatus(long id);

    @Delete
    @Where("o.status = 'CANCELED'")
    int removeCanceled();

    @Delete
    int removeEverything();

    void save(Shipment s);

    @Update("o.location = TRIM(o.location)")
    void trim();

    @Update("o.location=?3")
    @Where("o.id=?1 AND o.location=?2")
    boolean updateLocation(long id, String prevLocation, String newLocation);
}
