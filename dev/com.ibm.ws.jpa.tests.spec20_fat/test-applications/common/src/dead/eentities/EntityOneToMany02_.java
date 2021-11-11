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

@javax.persistence.metamodel.StaticMetamodel(value = dead.eentities.EntityOneToMany02.class)
public class EntityOneToMany02_ {
    public static volatile CollectionAttribute<EntityOneToMany02, Object> entityOneToMany02_collection01;
    public static volatile CollectionAttribute<EntityOneToMany02, Object> entityOneToMany02_collection02;
    public static volatile CollectionAttribute<EntityOneToMany02, Object> entityOneToMany02_collection03;
    public static volatile CollectionAttribute<EntityOneToMany02, EntityOneToMany01> entityOneToMany02_collection04;
    public static volatile CollectionAttribute<EntityOneToMany02, EntityOneToMany01> entityOneToMany02_collection05;
    public static volatile CollectionAttribute<EntityOneToMany02, dead.eentities.EntityOneToMany02> entityOneToMany02_collection06;
    public static volatile CollectionAttribute<EntityOneToMany02, dead.eentities.EntityOneToMany02> entityOneToMany02_collection07;
    public static volatile SetAttribute<EntityOneToMany02, Object> entityOneToMany02_hashset01;
    public static volatile SetAttribute<EntityOneToMany02, Object> entityOneToMany02_hashset02;
    public static volatile SetAttribute<EntityOneToMany02, Object> entityOneToMany02_hashset03;
    public static volatile SetAttribute<EntityOneToMany02, EntityOneToMany01> entityOneToMany02_hashset04;
    public static volatile SetAttribute<EntityOneToMany02, EntityOneToMany01> entityOneToMany02_hashset05;
    public static volatile SetAttribute<EntityOneToMany02, dead.eentities.EntityOneToMany02> entityOneToMany02_hashset06;
    public static volatile SetAttribute<EntityOneToMany02, dead.eentities.EntityOneToMany02> entityOneToMany02_hashset07;
    public static volatile SingularAttribute<EntityOneToMany02, Integer> entityOneToMany02_id;
    public static volatile ListAttribute<EntityOneToMany02, Object> entityOneToMany02_list01;
    public static volatile ListAttribute<EntityOneToMany02, Object> entityOneToMany02_list02;
    public static volatile ListAttribute<EntityOneToMany02, Object> entityOneToMany02_list03;
    public static volatile ListAttribute<EntityOneToMany02, EntityOneToMany01> entityOneToMany02_list04;
    public static volatile ListAttribute<EntityOneToMany02, EntityOneToMany01> entityOneToMany02_list05;
    public static volatile ListAttribute<EntityOneToMany02, dead.eentities.EntityOneToMany02> entityOneToMany02_list06;
    public static volatile ListAttribute<EntityOneToMany02, dead.eentities.EntityOneToMany02> entityOneToMany02_list07;
    public static volatile MapAttribute<EntityOneToMany02, Object, Object> entityOneToMany02_map01;
    public static volatile MapAttribute<EntityOneToMany02, Object, Object> entityOneToMany02_map02;
    public static volatile MapAttribute<EntityOneToMany02, Object, Object> entityOneToMany02_map03;
    public static volatile MapAttribute<EntityOneToMany02, EntityOneToMany01, EntityOneToMany01> entityOneToMany02_map05;
    public static volatile MapAttribute<EntityOneToMany02, dead.eentities.EntityOneToMany02, dead.eentities.EntityOneToMany02> entityOneToMany02_map06;
    public static volatile MapAttribute<EntityOneToMany02, dead.eentities.EntityOneToMany02, dead.eentities.EntityOneToMany02> entityOneToMany02_map07;
    public static volatile SetAttribute<EntityOneToMany02, Object> entityOneToMany02_set01;
    public static volatile SetAttribute<EntityOneToMany02, Object> entityOneToMany02_set02;
    public static volatile SetAttribute<EntityOneToMany02, Object> entityOneToMany02_set03;
    public static volatile SetAttribute<EntityOneToMany02, EntityOneToMany01> entityOneToMany02_set04;
    public static volatile SetAttribute<EntityOneToMany02, EntityOneToMany01> entityOneToMany02_set05;
    public static volatile SetAttribute<EntityOneToMany02, dead.eentities.EntityOneToMany02> entityOneToMany02_set06;
    public static volatile SetAttribute<EntityOneToMany02, dead.eentities.EntityOneToMany02> entityOneToMany02_set07;
    public static volatile SingularAttribute<EntityOneToMany02, Integer> entityOneToMany02_version;
}
