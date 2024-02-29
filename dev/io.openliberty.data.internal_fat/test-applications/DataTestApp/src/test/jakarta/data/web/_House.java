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

import jakarta.data.metamodel.Attribute;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;

/**
 * Metamodel for the House entity.
 */
@StaticMetamodel(House.class)
public class _House {
    public static volatile SortableAttribute<House> AREA;

    public static volatile Attribute<House> garage;

    public static volatile SortableAttribute<House> GARAGE_AREA;

    public static volatile SortableAttribute<House> garage_door_height;

    public static volatile SortableAttribute<House> Garage_Door_Width;

    public static volatile SortableAttribute<House> garage_type;

    public static volatile Attribute<House> kitchen;

    public static volatile SortableAttribute<House> kitchen_length;

    public static volatile SortableAttribute<House> kitchen_width;

    public static volatile TextAttribute<House> id;

    public static volatile SortableAttribute<House> LotSize;

    public static final String NUM_BEDROOMS = "numBedrooms";

    public static volatile SortableAttribute<House> numBedrooms;

    public static volatile TextAttribute<House> parcelid;
}
