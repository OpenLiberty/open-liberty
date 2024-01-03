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
    public static volatile SortableAttribute AREA;

    public static volatile Attribute garage;

    public static volatile SortableAttribute GARAGE_AREA;

    public static volatile SortableAttribute garage_door_height;

    public static volatile SortableAttribute Garage_Door_Width;

    public static volatile SortableAttribute garage_type;

    public static volatile Attribute kitchen;

    public static volatile SortableAttribute kitchen_length;

    public static volatile SortableAttribute kitchen_width;

    public static volatile TextAttribute id;

    public static volatile SortableAttribute LotSize;

    public static final String NUM_BEDROOMS = "numBedrooms";

    public static volatile SortableAttribute numBedrooms;

    public static volatile TextAttribute parcelid;
}
