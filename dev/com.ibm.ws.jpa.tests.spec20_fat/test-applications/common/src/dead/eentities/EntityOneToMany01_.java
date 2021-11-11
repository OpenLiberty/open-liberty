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

import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

@javax.persistence.metamodel.StaticMetamodel(value = dead.eentities.EntityOneToMany01.class)
public class EntityOneToMany01_ {
    public static volatile CollectionAttribute<EntityOneToMany01, Object> entityOneToMany01_collection01;
    public static volatile CollectionAttribute<EntityOneToMany01, Object> entityOneToMany01_collection02;
    public static volatile CollectionAttribute<EntityOneToMany01, Object> entityOneToMany01_collection03;
    public static volatile CollectionAttribute<EntityOneToMany01, dead.eentities.EntityOneToMany01> entityOneToMany01_collection04;
    public static volatile CollectionAttribute<EntityOneToMany01, dead.eentities.EntityOneToMany01> entityOneToMany01_collection05;
    public static volatile CollectionAttribute<EntityOneToMany01, EntityOneToMany02> entityOneToMany01_collection06;
    public static volatile CollectionAttribute<EntityOneToMany01, EntityOneToMany02> entityOneToMany01_collection07;
    public static volatile SetAttribute<EntityOneToMany01, Object> entityOneToMany01_hashset01;
    public static volatile SetAttribute<EntityOneToMany01, Object> entityOneToMany01_hashset02;
    public static volatile SetAttribute<EntityOneToMany01, dead.eentities.EntityOneToMany01> entityOneToMany01_hashset04;
    public static volatile SetAttribute<EntityOneToMany01, dead.eentities.EntityOneToMany01> entityOneToMany01_hashset05;
    public static volatile SetAttribute<EntityOneToMany01, EntityOneToMany02> entityOneToMany01_hashset06;
    public static volatile SingularAttribute<EntityOneToMany01, Integer> entityOneToMany01_id;
    public static volatile ListAttribute<EntityOneToMany01, Object> entityOneToMany01_list01;
    public static volatile ListAttribute<EntityOneToMany01, Object> entityOneToMany01_list02;
    public static volatile ListAttribute<EntityOneToMany01, Object> entityOneToMany01_list03;
    public static volatile ListAttribute<EntityOneToMany01, dead.eentities.EntityOneToMany01> entityOneToMany01_list04;
    public static volatile ListAttribute<EntityOneToMany01, dead.eentities.EntityOneToMany01> entityOneToMany01_list05;
    public static volatile ListAttribute<EntityOneToMany01, EntityOneToMany02> entityOneToMany01_list06;
    public static volatile ListAttribute<EntityOneToMany01, EntityOneToMany02> entityOneToMany01_list07;
    public static volatile MapAttribute<EntityOneToMany01, Object, Object> entityOneToMany01_map01;
    public static volatile MapAttribute<EntityOneToMany01, Object, Object> entityOneToMany01_map02;
    public static volatile MapAttribute<EntityOneToMany01, dead.eentities.EntityOneToMany01, dead.eentities.EntityOneToMany01> entityOneToMany01_map04;
    public static volatile MapAttribute<EntityOneToMany01, dead.eentities.EntityOneToMany01, dead.eentities.EntityOneToMany01> entityOneToMany01_map05;
    public static volatile MapAttribute<EntityOneToMany01, EntityOneToMany02, EntityOneToMany02> entityOneToMany01_map06;
    public static volatile SetAttribute<EntityOneToMany01, Object> entityOneToMany01_set01;
    public static volatile SetAttribute<EntityOneToMany01, Object> entityOneToMany01_set02;
    public static volatile SetAttribute<EntityOneToMany01, Object> entityOneToMany01_set03;
    public static volatile SetAttribute<EntityOneToMany01, dead.eentities.EntityOneToMany01> entityOneToMany01_set04;
    public static volatile SetAttribute<EntityOneToMany01, dead.eentities.EntityOneToMany01> entityOneToMany01_set05;
    public static volatile SetAttribute<EntityOneToMany01, EntityOneToMany02> entityOneToMany01_set06;
    public static volatile SetAttribute<EntityOneToMany01, EntityOneToMany02> entityOneToMany01_set07;
    public static volatile SingularAttribute<EntityOneToMany01, Integer> entityOneToMany01_version;
}
