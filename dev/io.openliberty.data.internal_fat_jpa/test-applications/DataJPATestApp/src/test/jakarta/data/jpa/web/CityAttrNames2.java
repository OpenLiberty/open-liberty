/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import jakarta.data.metamodel.Attribute;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;

/**
 * Static metamodel for the City entity.
 */
@StaticMetamodel(City.class)
public class CityAttrNames2 {
    public static volatile Attribute<City> areaCodes;
    public static volatile SortableAttribute<City> changeCount;
    public static volatile Attribute<City> id;
    public static volatile SortableAttribute<City> ignore; // ignored because the entity has no attribute with this name
    public static volatile long population; // ignored due to data type
    public static final TextAttribute<City> name = null; // ignored due to final
}