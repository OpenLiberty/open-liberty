/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import jakarta.data.model.Attribute;
import jakarta.data.model.StaticMetamodel;

/**
 * Static metamodel for the City entity.
 */
@StaticMetamodel(City.class)
public class CityAttrNames1 {
    public static final Attribute name = Attribute.get();
    public static final Attribute stateName = Attribute.get();
    public static final Attribute population = Attribute.get();
}