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

package com.ibm.ws.jpa.jpa31.model;

import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(JPA31Entity.class)
public class JPA31Entity_ {
    public static volatile SingularAttribute<JPA31Entity, Long> id;
    public static volatile SingularAttribute<JPA31Entity, String> strVal1;
    public static volatile SingularAttribute<JPA31Entity, String> strVal2;
    public static volatile SingularAttribute<JPA31Entity, Integer> intVal1;
    public static volatile SingularAttribute<JPA31Entity, Integer> intVal2;
    public static volatile CollectionAttribute<JPA31Entity, String> colVal1;
}
