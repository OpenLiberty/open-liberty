/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package dead.eentities;

import javax.persistence.metamodel.SingularAttribute;

@javax.persistence.metamodel.StaticMetamodel(value = dead.eentities.EntityManyToOne01.class)
public class EntityManyToOne01_ {
    public static volatile SingularAttribute<EntityManyToOne01, Integer> entityManyToOne01_id;
    public static volatile SingularAttribute<EntityManyToOne01, Integer> entityManyToOne01_version;
}
