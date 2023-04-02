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

package com.ibm.ws.jpa.olgh17837.model;

import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(OLGH17837Entity.class)
public class OLGH17837Entity_ {
    public static volatile SingularAttribute<OLGH17837Entity, Long> id;
    public static volatile SingularAttribute<OLGH17837Entity, String> strVal1;
    public static volatile SingularAttribute<OLGH17837Entity, String> strVal2;
    public static volatile SingularAttribute<OLGH17837Entity, Integer> intVal1;
    public static volatile SingularAttribute<OLGH17837Entity, Integer> intVal2;
    public static volatile CollectionAttribute<OLGH17837Entity, String> colVal1;
}
