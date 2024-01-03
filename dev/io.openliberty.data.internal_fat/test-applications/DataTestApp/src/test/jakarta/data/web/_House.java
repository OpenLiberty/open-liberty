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

import jakarta.data.metamodel.Attribute;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;

/**
 * Metamodel for the House entity.
 */
@StaticMetamodel(House.class)
public interface House_ {
    SortableAttribute AREA = SortableAttribute.get();

    Attribute garage = Attribute.get();

    SortableAttribute GARAGE_AREA = SortableAttribute.get();

    SortableAttribute garage_door_height = SortableAttribute.get();

    SortableAttribute Garage_Door_Width = SortableAttribute.get();

    SortableAttribute garage_type = SortableAttribute.get();

    Attribute kitchen = Attribute.get();

    SortableAttribute kitchen_length = SortableAttribute.get();

    SortableAttribute kitchen_width = SortableAttribute.get();

    TextAttribute id = TextAttribute.get();

    SortableAttribute LotSize = SortableAttribute.get();

    SortableAttribute numBedrooms = SortableAttribute.get();

    TextAttribute parcelid = TextAttribute.get();
}
