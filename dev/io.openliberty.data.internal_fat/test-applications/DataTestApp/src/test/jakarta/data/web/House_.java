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

import jakarta.data.model.Attribute;
import jakarta.data.model.StaticMetamodel;

/**
 * Metamodel for the House entity.
 */
@StaticMetamodel(House.class)
public class House_ {
    public static final Attribute AREA = Attribute.get();

    public static final Attribute garage = Attribute.get();

    public static final Attribute GARAGE_AREA = Attribute.get();

    public static final Attribute garage_door_height = Attribute.get();

    public static final Attribute Garage_Door_Width = Attribute.get();

    public static final Attribute garage_type = Attribute.get();

    public static final Attribute kitchen = Attribute.get();

    public static final Attribute kitchen_length = Attribute.get();

    public static final Attribute kitchen_width = Attribute.get();

    public static final Attribute id = Attribute.get();

    public static final Attribute LotSize = Attribute.get();

    public static final Attribute numBedrooms = Attribute.get();

    public static final Attribute parcelid = Attribute.get();
}
