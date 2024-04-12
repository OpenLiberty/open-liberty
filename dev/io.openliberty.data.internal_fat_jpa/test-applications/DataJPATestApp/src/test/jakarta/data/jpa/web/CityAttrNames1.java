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

import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

/**
 * Static metamodel for the City entity.
 */
@StaticMetamodel(City.class)
public interface CityAttrNames1 {
    TextAttribute<City> name = new TextAttributeRecord<>("name");
    TextAttribute<City> stateName = new TextAttributeRecord<>("stateName");
    SortableAttribute<City> population = new SortableAttributeRecord<>("population");
}