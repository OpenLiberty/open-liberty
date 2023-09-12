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

import jakarta.data.repository.StaticMetamodel;

/**
 * Metamodel for the House entity.
 */
@StaticMetamodel(House.class)
public class House_ {
    public static volatile String AREA;

    public static volatile String garage;

    public static volatile String GARAGE_AREA;

    public static volatile String garage_door_height;

    public static volatile String Garage_Door_Width;

    public static volatile String garage_type;

    public static volatile String kitchen;

    public static volatile String kitchen_length;

    public static volatile String kitchen_width;

    public static volatile String id;

    public static volatile String LotSize;

    public static volatile String numBedrooms;

    public static volatile String parcelid;
}
