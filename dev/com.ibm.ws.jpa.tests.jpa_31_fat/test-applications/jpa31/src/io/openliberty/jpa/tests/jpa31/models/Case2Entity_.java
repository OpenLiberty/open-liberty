/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.jpa.tests.jpa31.models;

import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Case2Entity.class)
public class Case2Entity_ {
    public static volatile SingularAttribute<Case2Entity, Long> id;
    public static volatile SingularAttribute<Case2Entity, String> strVal1;
    public static volatile SingularAttribute<Case2Entity, String> strVal2;
    public static volatile SingularAttribute<Case2Entity, Integer> intVal1;
    public static volatile SingularAttribute<Case2Entity, Integer> intVal2;
    public static volatile CollectionAttribute<Case2Entity, String> colVal1;
}
