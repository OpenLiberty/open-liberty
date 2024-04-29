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
import jakarta.data.metamodel.impl.AttributeRecord;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

/**
 * Metamodel for the House entity.
 */
@StaticMetamodel(House.class)
public interface _House {
    SortableAttribute<House> AREA = new SortableAttributeRecord<>("area");

    Attribute<House> garage = new AttributeRecord<>("garage");

    SortableAttribute<House> GARAGE_AREA = new SortableAttributeRecord<>("garage.area");

    SortableAttribute<House> garage_door_height = new SortableAttributeRecord<>("garage.door.height");

    SortableAttribute<House> Garage_Door_Width = new SortableAttributeRecord<>("garage.door.width");

    SortableAttribute<House> garage_type = new SortableAttributeRecord<>("garage.type");

    Attribute<House> kitchen = new AttributeRecord<>("kitchen");

    SortableAttribute<House> kitchen_length = new SortableAttributeRecord<>("kitchen.length");

    SortableAttribute<House> kitchen_width = new SortableAttributeRecord<>("kitchen.width");

    SortableAttribute<House> LotSize = new SortableAttributeRecord<>("lotSize");

    String NUM_BEDROOMS = "numBedrooms";

    SortableAttribute<House> numBedrooms = new SortableAttributeRecord<>(NUM_BEDROOMS);

    TextAttribute<House> parcelid = new TextAttributeRecord<>("parcelId");
}
