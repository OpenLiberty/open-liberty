/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.web.eclipselink;

import static jakarta.data.repository.By.ID;

import java.util.stream.Stream;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

/**
 * Repository interface for the DirectedSegement entity, which is
 * a Java record with multiple of the same type of embeddable.
 */
@Repository(dataStore = "java:module/env/jdbc/DerbyDataSourceRef")
public interface Cylinders {

    @Find
    @OrderBy("side.b.y")
    @OrderBy("side_b_x")
    @OrderBy(ID)
    Stream<Cylinder> centeredAt(int centerX, int center_y);

    @Query("SELECT COUNT(THIS) " +
           " WHERE (side.a.y - side.b.y) * (side.a.y + side.b.y - 2 * center.y)" +
           "     = (side.b.x - side.a.x) * (side.a.x + side.b.x - 2 * center.x)")
    int countValid();

    @Delete
    Long eraseAll();

    Stream<Cylinder> findBySideAXOrSideBXOrderBySideBYDesc(int x1, int x2);

    @Save
    void upsert(Cylinder... s);
}
